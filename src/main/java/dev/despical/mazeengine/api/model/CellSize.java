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
 * Describes the dimensions of a logical maze grid.
 * <p>
 * Each axis contains at least two cells. The total cell count must fit in a
 * signed int because graph storage and routing use integer cell indices.
 * <p>
 * This value counts graph cells, not Minecraft blocks. The occupied block
 * volume also depends on the preset's corridor width, wall thickness, wall
 * height, and roof. Configured server limits are checked at submission.
 *
 * @param width the number of cells along X, at least two
 * @param depth the number of cells along Z, at least two
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record CellSize(int width, int depth) {

    /**
     * Constructs grid dimensions supported by the graph representation.
     *
     * @param width the number of columns
     * @param depth the number of rows
     * @throws IllegalArgumentException if either axis is smaller than two or the
     *     total cell count exceeds {@link Integer#MAX_VALUE}
     */
    public CellSize {
        if (width < 2 || depth < 2 || (long) width * depth > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid cell dimensions.");
        }
    }

    /**
     * Returns the total number of cells in the logical grid.
     * <p>
     * The constructor ensures the product fits in an int. This count is unrelated
     * to the number of blocks written by generation.
     *
     * @return {@code width * depth}
     */
    public int count() {
        return width * depth;
    }
}
