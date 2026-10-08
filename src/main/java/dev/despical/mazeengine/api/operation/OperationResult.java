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

import dev.despical.mazeengine.api.model.MazeSnapshot;

import java.time.Instant;
import java.util.UUID;

/**
 * Describes the successful final outcome of one world-editing operation.
 * <p>
 * The result is published after world work and final metadata persistence or
 * deletion have finished and the reservation has been released. Failed and
 * cancelled operations do not produce this result; their stages complete
 * exceptionally and their failure events carry the underlying cause.
 * <p>
 * The maze description is immutable. For removal, it carries
 * {@link MazeSnapshot.Status#DELETED} even though the record is now absent from
 * the registry. Retaining this result does not make it update with future work.
 *
 * @param operationId the unique UUID of the completed operation
 * @param kind the action that completed
 * @param maze the final immutable maze description
 * @param completedAt the timestamp at which this result was created
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record OperationResult(UUID operationId, OperationKind kind, MazeSnapshot maze, Instant completedAt) {
}
