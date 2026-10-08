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

package dev.despical.mazeengine.api.event;

import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.model.MazeSnapshot;
import dev.despical.mazeengine.api.operation.OperationKind;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Called when an accepted maze operation ends with failure or cancellation.
 * <p>
 * This event is fired synchronously on the server thread after the operation's
 * reservation has been released. It is not cancellable and does not indicate
 * that world changes were rolled back. Blocks may already have been modified,
 * and a failed record may remain for recovery.
 * <p>
 * The last known maze description can be absent when creation failed during
 * topology planning, before a record existed. A description may also refer to
 * a failed preparation record that has already been removed from the registry.
 * Use {@link #maze()} to inspect the retained description when available.
 * <p>
 * Typical use cases:
 * <ul>
 *     <li>Logging the operation identity and underlying failure</li>
 *     <li>Distinguishing cancellation from a generation or persistence error</li>
 *     <li>Offering recovery actions for a retained failed maze</li>
 * </ul>
 * Requests rejected synchronously before submission do not fire this event.
 * Their validation exception is delivered directly to the API caller.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeOperationFailedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The unique identity of the failed operation.
     * <p>
     * A later operation using the same maze name has a different UUID.
     */
    private final UUID operationId;

    /**
     * The normalized maze name targeted by the operation.
     * <p>
     * The name is available even when planning failed before a record existed.
     */
    private final MazeId mazeId;

    /**
     * The action requested by the failed operation.
     * <p>
     * It describes the intended action, regardless of which internal stage failed.
     */
    private final OperationKind kind;

    /**
     * The last known maze description, or null before a record existed.
     * <p>
     * This retained description does not guarantee that a live record remains
     * in the registry or that any changed blocks have been restored.
     */
    private final MazeSnapshot maze;

    /**
     * The underlying terminal failure.
     * <p>
     * Cancellation, including plugin shutdown, is represented by
     * {@link java.util.concurrent.CancellationException}.
     */
    private final Throwable cause;

    /**
     * Constructs a terminal event using the last known maze directly.
     * <p>
     * Pass null for the maze when no record was produced. Callers do not need
     * to wrap constructor arguments in Optional; {@link #maze()} exposes absence
     * to event consumers. Constructing the event does not dispatch it.
     *
     * @param operationId the identity of the failed operation
     * @param mazeId the target maze name
     * @param kind the requested operation kind
     * @param maze the last known description, or null when no record existed
     * @param cause the underlying failure or cancellation
     * @throws NullPointerException if any required identity, kind, or cause is null
     */
    public MazeOperationFailedEvent(
            UUID operationId,
            MazeId mazeId,
            OperationKind kind,
            MazeSnapshot maze,
            Throwable cause
    ) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.mazeId = Objects.requireNonNull(mazeId, "mazeId");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.maze = maze;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /**
     * Returns the identity of the operation that ended.
     * <p>
     * Use this UUID to correlate failure with a handle or earlier progress
     * sample, even if the target name is reused.
     *
     * @return the failed operation UUID
     */
    public UUID operationId() {
        return operationId;
    }

    /**
     * Returns the name the operation targeted.
     * <p>
     * This identifier is present even when creation planning produced no record.
     * It does not indicate whether a maze currently exists under that name.
     *
     * @return the normalized target maze identifier
     */
    public MazeId mazeId() {
        return mazeId;
    }

    /**
     * Returns the action that was being attempted.
     * <p>
     * The kind remains the originally submitted action after failure or
     * cancellation; it does not identify the internal stage that failed.
     *
     * @return the requested operation kind
     */
    public OperationKind kind() {
        return kind;
    }

    /**
     * Returns the last known maze description, when available.
     * <p>
     * The description may belong to a removed preparation record. An empty
     * result means the operation ended before it had a record to describe.
     *
     * @return the retained description, or empty when no record existed
     */
    public Optional<MazeSnapshot> maze() {
        return Optional.ofNullable(maze);
    }

    /**
     * Returns the underlying error that ended the operation.
     * <p>
     * Completion and execution wrapper exceptions are unwrapped by the provider.
     * Cancellation and shutdown use CancellationException; other causes may
     * indicate planning, protection, storage, or block-work failures.
     *
     * @return the terminal failure or cancellation cause
     */
    public Throwable cause() {
        return cause;
    }

    /**
     * Returns the listener registry used to dispatch this event.
     * <p>
     * The same registry is shared by all instances of this event class.
     *
     * @return the Bukkit handler list for this event type
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static registry required for Bukkit listener registration.
     * <p>
     * Bukkit uses this method to associate registered listeners with this event
     * type before any event instance is dispatched.
     *
     * @return the Bukkit handler list shared by this event class
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
