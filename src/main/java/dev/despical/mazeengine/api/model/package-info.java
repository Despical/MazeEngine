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
 * Immutable values describing maze identity, coordinates, geometry, and metadata.
 * <p>
 * {@link dev.despical.mazeengine.api.model.MazeId} identifies a saved maze,
 * while {@link dev.despical.mazeengine.api.model.CellPosition} and
 * {@link dev.despical.mazeengine.api.model.CellSize} describe the logical cell
 * grid. {@link dev.despical.mazeengine.api.model.MazeLocation},
 * {@link dev.despical.mazeengine.api.model.MazeBounds}, and
 * {@link dev.despical.mazeengine.api.model.MazeGeometry} describe world block
 * placement and structural dimensions without retaining mutable Bukkit objects.
 * <p>
 * {@link dev.despical.mazeengine.api.model.MazeSnapshot} captures a saved record
 * at one point in time. Its preset metadata remains frozen with that record,
 * whereas {@link dev.despical.mazeengine.api.PresetRegistry} exposes the current
 * configured {@link dev.despical.mazeengine.api.model.PresetSnapshot} values.
 * Coordinates, lifecycle state, and topology statistics describe saved metadata
 * rather than a live scan of world blocks.
 * <p>
 * These values may be retained and read on other threads after a server-thread
 * registry query or operation callback. They do not update after configuration
 * reloads, regeneration, or deletion; query the registry again for a newer view.
 * Retaining a maze identifier or snapshot does not reserve its name against
 * reuse after deletion.
 */
package dev.despical.mazeengine.api.model;
