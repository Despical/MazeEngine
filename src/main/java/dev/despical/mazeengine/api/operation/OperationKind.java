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

/**
 * Identifies the world-editing action associated with an operation.
 * <p>
 * The kind is assigned when a request is accepted and remains attached to its
 * handle, result, and events. It describes the action rather than the current
 * planning, preflight, storage, or block-writing stage.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public enum OperationKind {

    /**
     * Creates a new maze from a request and a current configured preset.
     * <p>
     * The operation reserves a new identifier, plans topology, performs preflight,
     * and optionally captures original terrain before writing the structure.
     */
    CREATE,

    /**
     * Rebuilds an existing maze using its frozen preset and a supplied seed.
     * <p>
     * The full structure is reapplied. Any original terrain snapshot is retained
     * for a later restore rather than recaptured from the existing maze.
     */
    REGENERATE,

    /**
     * Repairs planned non-air structure using the existing seed and topology.
     * <p>
     * Planned air positions are skipped to preserve passage contents. Repair uses
     * the regeneration permission and ownership rules.
     */
    REPAIR,

    /**
     * Removes a maze by restoring terrain or clearing its bounded volume.
     * <p>
     * Successful removal deletes the saved record. Its result retains an immutable
     * description with the DELETED status for listeners and completion callbacks.
     */
    DELETE
}
