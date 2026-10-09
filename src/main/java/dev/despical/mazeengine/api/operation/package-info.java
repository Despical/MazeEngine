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

/**
 * Handles, progress samples, and results for accepted world-editing operations.
 * <p>
 * {@link dev.despical.mazeengine.api.MazeOperations} returns a
 * {@link dev.despical.mazeengine.api.operation.MazeOperation} after accepting
 * creation, regeneration, repair, or deletion. Each handle has a unique UUID
 * independent of the target maze name and retains its terminal state even when
 * that name is later reused. {@link dev.despical.mazeengine.api.operation.OperationKind}
 * identifies the requested action throughout its lifecycle.
 * <p>
 * Query progress and request cancellation on the server thread.
 * {@link dev.despical.mazeengine.api.operation.OperationProgress} is an immutable
 * sample; its state is the lifecycle contract, while stage text and percentage
 * describe the current internal step. Observe the completion stage without
 * blocking the server thread, and schedule Bukkit work explicitly when a
 * callback may run on another thread.
 * <p>
 * Successful completion publishes an immutable
 * {@link dev.despical.mazeengine.api.operation.OperationResult} after final
 * persistence or record removal and reservation release. Failure and cancellation
 * complete the stage exceptionally and may leave partially changed blocks.
 * Cancelling a future derived from the completion stage does not stop world
 * work; use the handle's cancellation method to request it.
 */
package dev.despical.mazeengine.api.operation;
