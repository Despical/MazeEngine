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

package dev.despical.mazeengine.api.operation;

import dev.despical.mazeengine.api.model.MazeId;

import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Tracks exactly one accepted world-editing operation.
 * <p>
 * The handle's UUID identifies the operation independently of its maze name.
 * It retains its terminal state even if that name is later deleted or used by
 * another operation. Identity, kind, and completion can be retained; progress
 * and cancellation must be accessed on the server thread.
 * <p>
 * Use {@link #completion()} to observe the final result without blocking the
 * server thread. Cancelling a CompletableFuture obtained from that stage does
 * not stop world work; use {@link #cancel(CommandSender)} to request cancellation.
 * <p>
 * A failure or cancellation may leave partially changed blocks and a recoverable
 * failed record. A successful result means the world operation and final
 * persistence finished; it does not mean the actor was teleported.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public interface MazeOperation {

    /**
     * Returns the unique identity assigned to this accepted operation.
     * <p>
     * The identity remains unchanged after completion. Another operation targeting
     * the same maze name receives a different UUID.
     *
     * @return this operation's UUID
     */
    UUID id();

    /**
     * Returns the normalized maze name targeted by this operation.
     * <p>
     * During creation planning, a handle can target a name for which the registry
     * does not yet contain a maze record.
     *
     * @return the target maze identifier
     */
    MazeId mazeId();

    /**
     * Returns the action this operation was submitted to perform.
     * <p>
     * The kind distinguishes creation, full regeneration, structural repair,
     * and removal. It does not change as the operation advances through stages.
     *
     * @return the submitted operation kind
     */
    OperationKind kind();

    /**
     * Captures the current progress of this operation on the server thread.
     * <p>
     * The returned value is immutable and does not update itself. Once terminal,
     * the handle returns its retained final state even after the name is reused.
     * Use state for lifecycle decisions; stage text is intended for diagnostics.
     *
     * @return a point-in-time progress sample
     * @throws IllegalStateException if called off the server thread
     */
    OperationProgress progress();

    /**
     * Returns a read-only stage representing this operation's final outcome.
     * <p>
     * Success completes with the final maze description after persistence and
     * reservation release. Failure completes exceptionally with its underlying
     * cause; cancellation uses {@link java.util.concurrent.CancellationException}.
     * Never call blocking {@code join()} or {@code get()} on the server thread.
     * <p>
     * Completion is published on the server thread. A callback attached after
     * completion may run on the attaching thread, and asynchronous callbacks use
     * their executor. Schedule Bukkit work explicitly whenever needed.
     * <p>
     * Changing or cancelling a future derived from this stage does not cancel the
     * operation itself. Use {@link #cancel(CommandSender)} for world-work cancellation.
     *
     * @return the operation's read-only completion stage
     */
    CompletionStage<OperationResult> completion();

    /**
     * Requests cancellation of this exact operation on the server thread.
     * <p>
     * The actor must satisfy the plugin's cancellation permission and ownership
     * rules. A handle for an ended operation returns false and cannot cancel a
     * newer operation using the same maze name.
     * <p>
     * Cancellation is not a rollback guarantee. A maze whose blocks have already
     * changed may remain as a failed record requiring recovery or removal.
     *
     * @param actor the sender requesting cancellation
     * @return true when cancellation was requested, or false when this operation has ended
     * @throws IllegalArgumentException if the actor lacks cancellation permission or ownership rights
     * @throws IllegalStateException if called off the server thread or the plugin is disabled
     */
    boolean cancel(CommandSender actor);
}
