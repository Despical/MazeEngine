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

import dev.despical.mazeengine.api.operation.OperationResult;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Called after a maze operation has completed successfully.
 * <p>
 * This event is fired synchronously on the server thread for both command and
 * API operations. By the time listeners run:
 * <ul>
 *     <li>The requested world work has finished</li>
 *     <li>Final metadata has been saved, or deleted for removal</li>
 *     <li>The operation's name reservation has been released</li>
 * </ul>
 * The event is not cancellable. It reports a finished action rather than offering
 * a preflight veto. Success does not imply that the actor has been teleported.
 * <p>
 * Typical use cases:
 * <ul>
 *     <li>Updating menus or statistics after maze creation and rebuilding</li>
 *     <li>Recording completed operation identities and final descriptions</li>
 *     <li>Reacting to removal using the retained DELETED maze snapshot</li>
 * </ul>
 * The result is immutable and can be retained after the event returns. For a
 * removed maze, querying the registry by its name will no longer return that record.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeOperationCompletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The successful operation result delivered to listeners.
     * <p>
     * The contained maze is a point-in-time description, including the DELETED
     * status when the completed operation removed the maze.
     */
    private final OperationResult result;

    /**
     * Constructs a completion event containing the successful result.
     * <p>
     * Constructing the event does not dispatch it or perform world work. The
     * plugin dispatches it after successful operation cleanup and persistence.
     *
     * @param result the completed operation result
     * @throws NullPointerException if {@code result} is null
     */
    public MazeOperationCompletedEvent(OperationResult result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    /**
     * Returns the final outcome reported by this event.
     * <p>
     * Use the operation kind to distinguish creation, regeneration, repair,
     * and removal. The result's maze description is safe to retain.
     *
     * @return the immutable successful operation result
     */
    public OperationResult result() {
        return result;
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
