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

package dev.despical.mazeengine.api.model;

/**
 * Identifies one cell in a maze's logical graph.
 * <p>
 * The X coordinate is the zero-based column and Z is the zero-based row.
 * These indices are independent of world block coordinates, corridor width,
 * wall thickness, and the maze's world origin.
 * <p>
 * Construction rejects negative indices. A position is not tied to one maze;
 * {@link dev.despical.mazeengine.api.MazeRegistry#route} checks its upper bounds
 * against the selected maze when a route is requested.
 *
 * @param x the nonnegative column index
 * @param z the nonnegative row index
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record CellPosition(int x, int z) {

    /**
     * Constructs a cell position with nonnegative indices.
     * <p>
     * No maze-specific upper bound is checked here. The same position can be used
     * with any maze whose grid contains the selected column and row.
     *
     * @param x the zero-based column index
     * @param z the zero-based row index
     * @throws IllegalArgumentException if either index is negative
     */
    public CellPosition {
        if (x < 0 || z < 0) {
            throw new IllegalArgumentException("Invalid cell position.");
        }
    }
}
