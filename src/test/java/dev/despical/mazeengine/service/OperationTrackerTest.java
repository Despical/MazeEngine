/*
 * MazeEngine - Deterministic maze generation for Minecraft.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.despical.mazeengine.service;

import static org.junit.jupiter.api.Assertions.*;

import dev.despical.mazeengine.api.operation.OperationKind;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Verifies operation reservation identity and terminal publication ordering.
 * <p>
 * The tests exercise shared capacity limits, reject stale cancellation tokens,
 * and ensure observers receive an outcome before the result future completes.
 * Repeated settlement must not publish or complete another terminal outcome.
 * <p>
 * Shutdown cases check cancellation of pending tickets and rejection of new
 * work. The fixture isolates tracking behavior without planning a graph or
 * mutating world blocks.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class OperationTrackerTest {

    @Test
    void enforcesSharedLimitsAndRejectsStaleCancellationTokens() {
        var tracker = new OperationTracker(() -> 1, () -> false, Logger.getAnonymousLogger());
        tracker.reserve("garden", OperationKind.CREATE);
        var original = tracker.operation("GARDEN");
        assertThrows(IllegalArgumentException.class, () -> tracker.reserve("garden", OperationKind.CREATE));
        assertThrows(IllegalArgumentException.class, () -> tracker.reserve("second", OperationKind.CREATE));
        tracker.settle("garden", null, new CancellationException("Cancelled."));
        assertTrue(original.result().isCompletedExceptionally());
        tracker.reserve("garden", OperationKind.REGENERATE);
        assertFalse(tracker.currentOperation("garden", original.id()));
        assertTrue(tracker.currentOperation("garden", tracker.operation("garden").id()));
    }

    @Test
    void publishesBeforeCompletingAndSettlesAnOperationOnlyOnce() {
        var tracker = new OperationTracker(() -> 1, () -> false, Logger.getAnonymousLogger());
        tracker.reserve("garden", OperationKind.CREATE);
        var ticket = tracker.operation("garden");
        var outcome = new AtomicReference<MazeService.OperationOutcome>();
        tracker.operationListener(event -> {
            assertFalse(ticket.result().isDone());
            assertFalse(tracker.currentOperation("garden", ticket.id()));
            outcome.set(event);
        });
        var failure = new IllegalArgumentException("Occupied terrain.");
        tracker.settle("garden", null, failure);
        assertSame(failure, outcome.get().error());
        assertSame(ticket, outcome.get().ticket());
        tracker.settle("garden", null, null);
        assertSame(failure, outcome.get().error());
    }

    @Test
    void shutdownCancelsOutstandingOperationsAndRejectsNewRequests() {
        var stopping = new AtomicBoolean();
        var tracker = new OperationTracker(() -> 2, stopping::get, Logger.getAnonymousLogger());
        tracker.reserve("first", OperationKind.DELETE);
        tracker.reserve("second", OperationKind.REPAIR);
        var first = tracker.operation("first");
        var second = tracker.operation("second");
        stopping.set(true);
        tracker.close(Map.of());
        assertTrue(first.result().isCompletedExceptionally());
        assertTrue(second.result().isCompletedExceptionally());
        assertTrue(tracker.reserved.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> tracker.reserve("third", OperationKind.CREATE));
    }
}
