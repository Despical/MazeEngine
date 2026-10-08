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

import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.operation.MazeOperation;
import dev.despical.mazeengine.api.request.CreateMazeRequest;

import org.bukkit.command.CommandSender;

/**
 * Submits maze creation, regeneration, repair, and removal requests.
 * <p>
 * Call this service on the server thread while MazeEngine is enabled. The actor
 * receives ordinary plugin feedback and must have {@code mazeengine.use}, the
 * action permission, and any ownership or management rights required by that
 * action. Non-player administrative actors use the normal administrative rules.
 * <p>
 * Validation performed before submission throws directly and produces no
 * operation handle. Once accepted, planning, protection, storage, or block-work
 * failures complete the handle exceptionally. Completion does not imply that
 * the actor was teleported, and cancellation does not guarantee a rollback.
 * <p>
 * Use {@link MazeOperation#completion()} for the final result and
 * {@link MazeOperation#progress()} for the current state. Never block the server
 * thread waiting for an operation to finish.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public interface MazeOperations {

    /**
     * Starts creation of a new maze using a validated request.
     * <p>
     * Unspecified overrides inherit the selected current preset. An omitted seed
     * is generated when the request is submitted. A player's UUID becomes the
     * owner; a non-player actor produces an owner with the zero UUID.
     * <p>
     * The name is reserved before asynchronous topology planning starts. The
     * handle can therefore exist before the maze appears in the registry.
     *
     * @param actor the sender with {@code mazeengine.use} and {@code mazeengine.create}
     * @param request the maze identifier, origin, grid size, preset, and overrides
     * @return a handle for this accepted creation request
     * @throws NullPointerException if {@code actor} or {@code request} is null
     * @throws IllegalArgumentException if permissions, preset, world, geometry, limits, or conflicting reservations reject the request
     * @throws IllegalStateException if called off the server thread or the plugin is disabled
     */
    MazeOperation create(CommandSender actor, CreateMazeRequest request);

    /**
     * Starts a full rebuild of an existing maze with the supplied seed.
     * <p>
     * The rebuild uses the maze's frozen preset and grid dimensions. Its original
     * terrain snapshot is retained rather than replaced by a snapshot of the maze.
     * Using the saved seed reuses the saved topology; another seed generates a new
     * layout. The full structure can overwrite contents within its bounds.
     *
     * @param actor the sender with regeneration permission and ownership or management rights
     * @param id the existing maze identifier
     * @param seed the deterministic seed to use for the rebuilt maze
     * @return a handle for this accepted regeneration request
     * @throws IllegalArgumentException if the maze, world, permissions, safety checks,
     *     required original snapshot, or conflicting reservations reject regeneration
     * @throws IllegalStateException if called off the server thread or the plugin is disabled
     */
    MazeOperation regenerate(CommandSender actor, MazeId id, long seed);

    /**
     * Starts repair of the structure described by the saved maze plan.
     * <p>
     * Repair retains the current seed and topology and restores planned non-air
     * structure blocks. Planned air blocks are skipped, preserving passage
     * contents. It uses regeneration permission and the same ownership checks.
     *
     * @param actor the sender with regeneration permission and ownership or management rights
     * @param id the existing maze identifier
     * @return a handle for this accepted repair request
     * @throws IllegalArgumentException if the maze, world, permissions, safety checks,
     *     required original snapshot, or conflicting reservations reject repair
     * @throws IllegalStateException if called off the server thread or the plugin is disabled
     */
    MazeOperation repair(CommandSender actor, MazeId id);

    /**
     * Starts removal of a maze and its persisted record.
     * <p>
     * The removal mode determines whether the original terrain is restored or the
     * bounded volume is cleared to air. This API submits removal immediately;
     * integrations presenting a player-facing destructive action should provide
     * their own confirmation before calling it.
     * <p>
     * On success, the result describes the removed maze with
     * {@link dev.despical.mazeengine.api.model.MazeSnapshot.Status#DELETED}.
     * The identifier is then absent from the registry and can be used again.
     *
     * @param actor the sender with deletion permission and ownership or management rights
     * @param id the maze to remove
     * @param mode the terrain handling policy
     * @return a handle for this accepted removal request
     * @throws NullPointerException if {@code mode} is null
     * @throws IllegalArgumentException if the maze, world, permissions, player safety,
     *     required restore snapshot, or conflicting reservations reject removal
     * @throws IllegalStateException if called off the server thread or the plugin is disabled
     */
    MazeOperation delete(CommandSender actor, MazeId id, RemovalMode mode);

    /**
     * Determines what happens to the maze's block volume during removal.
     * <p>
     * The policy affects terrain handling only. Successful removal also deletes
     * the saved maze metadata and releases its identifier.
     */
    enum RemovalMode {

        /**
         * Restores original terrain when the maze has a recorded snapshot.
         * <p>
         * If no snapshot was recorded, removal clears the volume to air. A recorded
         * but missing snapshot is a restore error and does not fall back to clearing.
         */
        AUTO,

        /**
         * Requires restoration of the complete original terrain snapshot.
         * <p>
         * WorldEdit or FAWE and the saved snapshot file must be available. A maze
         * without a complete snapshot is rejected before removal is submitted.
         */
        RESTORE,

        /**
         * Clears the entire bounded volume to air.
         * <p>
         * This policy clears terrain even when an original snapshot exists. It does
         * not require WorldEdit or FAWE for snapshot restoration.
         */
        CLEAR
    }
}
