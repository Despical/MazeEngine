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
 * Bukkit events reporting the terminal outcome of accepted maze operations.
 * <p>
 * {@link dev.despical.mazeengine.api.event.MazeOperationCompletedEvent} reports
 * successful world work after final persistence or record removal and release
 * of the operation's name reservation.
 * {@link dev.despical.mazeengine.api.event.MazeOperationFailedEvent} reports
 * failures and cancellation with the operation identity, underlying cause,
 * and last known maze description when one exists.
 * <p>
 * MazeEngine dispatches these events synchronously on the server thread for
 * both command and API operations. They are outcome notifications and cannot
 * be cancelled. Request validation that rejects submission before a handle is
 * accepted does not produce an operation outcome event.
 * <p>
 * Listeners may retain immutable results and maze descriptions for menus,
 * statistics, or audit records. A successful deletion retains a description
 * with {@link dev.despical.mazeengine.api.model.MazeSnapshot.Status#DELETED}
 * even though the registry no longer contains the record. Failure or
 * cancellation does not guarantee that changed world blocks were restored.
 */
package dev.despical.mazeengine.api.event;
