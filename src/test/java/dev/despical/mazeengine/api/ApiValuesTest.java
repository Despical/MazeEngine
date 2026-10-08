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

package dev.despical.mazeengine.api;

import static org.junit.jupiter.api.Assertions.*;

import dev.despical.mazeengine.api.event.MazeOperationFailedEvent;
import dev.despical.mazeengine.api.model.CellPosition;
import dev.despical.mazeengine.api.model.CellSize;
import dev.despical.mazeengine.api.model.MazeBounds;
import dev.despical.mazeengine.api.model.MazeGeometry;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.model.MazeLocation;
import dev.despical.mazeengine.api.model.MazeSnapshot;
import dev.despical.mazeengine.api.model.PresetSnapshot;
import dev.despical.mazeengine.api.model.TeleportDestination;
import dev.despical.mazeengine.api.operation.OperationKind;
import dev.despical.mazeengine.api.request.CreateMazeRequest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CancellationException;

/**
 * Verifies absence, explicit overrides, and retained value semantics in API values.
 *
 * @author Despical
 * <p>
 * Created at 08.10.2026
 */
class ApiValuesTest {

    private final MazeId id = new MazeId("Arena");
    private final MazeLocation origin = new MazeLocation(new UUID(0, 1), -20, 64, 10);
    private final CellSize cells = new CellSize(8, 6);

    @Test
    void absentOverridesInheritWhileZeroAndFalseRemainExplicit() {
        var inherited = request(null, null, null, null);
        assertTrue(inherited.seed().isEmpty());
        assertTrue(inherited.complexity().isEmpty());
        assertTrue(inherited.roof().isEmpty());
        assertTrue(inherited.snapshot().isEmpty());
        assertTrue(inherited.geometry().isEmpty());
        assertTrue(inherited.placement().isEmpty());
        assertEquals("default", inherited.preset());

        var explicit = request(0L, 0.0, false, false);
        assertEquals(0L, explicit.seed().orElseThrow());
        assertEquals(0.0, explicit.complexity().orElseThrow());
        assertFalse(explicit.roof().orElseThrow());
        assertFalse(explicit.snapshot().orElseThrow());
        assertNotEquals(inherited, explicit);
    }

    @Test
    void reusingBuilderDoesNotChangePreviouslyBuiltRequests() {
        var builder = CreateMazeRequest.builder("Arena", origin, cells);
        var inherited = builder.build();
        var geometry = new MazeGeometry(3, 2, 5);
        var overridden = builder.seed(-742).roof(false).snapshot(true)
                .geometry(geometry).placement(CreateMazeRequest.Placement.SAFE).build();

        assertEquals(request(null, null, null, null), inherited);
        assertTrue(inherited.seed().isEmpty());
        assertTrue(inherited.geometry().isEmpty());
        assertTrue(inherited.roof().isEmpty());
        assertEquals(-742L, overridden.seed().orElseThrow());
        assertEquals(geometry, overridden.geometry().orElseThrow());
        assertEquals(CreateMazeRequest.Placement.SAFE, overridden.placement().orElseThrow());
        assertTrue(overridden.snapshot().orElseThrow());
        assertEquals(overridden, builder.build());
        assertEquals(overridden.hashCode(), builder.build().hashCode());
    }

    @Test
    void directAndBuilderConstructionApplyTheSameComplexityValidation() {
        for (double invalid : new double[] {-0.01, 1.01, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> request(null, invalid, null, null));
            assertThrows(IllegalArgumentException.class,
                    () -> CreateMazeRequest.builder("arena", origin, cells).complexity(invalid).build());
        }
        assertEquals(1.0, request(null, 1.0, null, null).complexity().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> CreateMazeRequest.builder("arena", origin, cells).preset("bad name").build());
        assertThrows(NullPointerException.class,
                () -> CreateMazeRequest.builder("arena", null, cells));
    }

    @Test
    void snapshotAcceptsDirectDestinationAndRetainsContentEquality() {
        var defaultPoint = snapshot(null);
        assertTrue(defaultPoint.teleportDestination().isEmpty());
        assertEquals(defaultPoint, snapshot(null));
        assertEquals(defaultPoint.hashCode(), snapshot(null).hashCode());

        var destination = new TeleportDestination(new UUID(0, 2), 4.5, 65, -2.5, 90, 0);
        var customPoint = snapshot(destination);
        assertEquals(destination, customPoint.teleportDestination().orElseThrow());
        assertNotEquals(defaultPoint, customPoint);
        assertTrue(defaultPoint.teleportDestination().isEmpty());
    }

    @Test
    void failureEventAcceptsAbsentOrDirectLastKnownSnapshot() {
        var operationId = UUID.randomUUID();
        var cause = new CancellationException("Stopped while planning");
        var beforeRecord = new MazeOperationFailedEvent(operationId, id, OperationKind.CREATE, null, cause);
        assertTrue(beforeRecord.maze().isEmpty());
        assertSame(cause, beforeRecord.cause());
        assertEquals(operationId, beforeRecord.operationId());

        var record = snapshot(null);
        var afterRecord = new MazeOperationFailedEvent(operationId, id, OperationKind.CREATE, record, cause);
        assertSame(record, afterRecord.maze().orElseThrow());
        assertEquals(id, afterRecord.mazeId());
    }

    private CreateMazeRequest request(Long seed, Double complexity, Boolean roof, Boolean snapshot) {
        return new CreateMazeRequest(id, origin, cells, "DeFaUlT",
                seed, null, complexity, roof, snapshot, null);
    }

    private MazeSnapshot snapshot(TeleportDestination destination) {
        var preset = new PresetSnapshot("default", "Default", "A maze", "#FFFFFF",
                PresetSnapshot.Difficulty.EASY, new MazeGeometry(2, 1, 3), false, false,
                PresetSnapshot.Algorithm.PERFECT, 0.5);
        return new MazeSnapshot(id, "world", new UUID(0, 3),
                new MazeBounds(origin, 25, 4, 19), cells, 742, preset, MazeSnapshot.Status.READY,
                false, Instant.EPOCH, destination, new CellPosition(0, 0), new CellPosition(7, 5),
                12, 8, "");
    }
}
