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
 * Describes structural maze dimensions in Minecraft blocks.
 * <p>
 * Path width is the usable corridor width; wall thickness separates adjacent
 * cells. Wall height counts the vertical wall blocks above the floor and
 * excludes both the floor and an optional roof layer.
 * <p>
 * This value validates the supported structural ranges. The final block volume
 * depends on the logical grid and is checked against server limits at submission.
 *
 * @param pathWidth the corridor width, from 1 through 16 blocks
 * @param wallThickness the wall thickness, from 1 through 8 blocks
 * @param wallHeight the wall height, from 2 through 64 blocks
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record MazeGeometry(int pathWidth, int wallThickness, int wallHeight) {

    /**
     * Constructs a complete set of supported structural dimensions.
     * <p>
     * All three settings are supplied together. A creation-request override replaces
     * the corresponding preset dimensions as one geometry value.
     *
     * @param pathWidth the corridor width in blocks
     * @param wallThickness the wall thickness in blocks
     * @param wallHeight the wall height excluding floor and roof
     * @throws IllegalArgumentException if path width is outside 1..16, wall thickness
     *     is outside 1..8, or wall height is outside 2..64
     */
    public MazeGeometry {
        if (pathWidth < 1
            || pathWidth > 16
            || wallThickness < 1
            || wallThickness > 8
            || wallHeight < 2
            || wallHeight > 64
        ) {
            throw new IllegalArgumentException("path-width=1..16, wall-thickness=1..8, wall-height=2..64 required.");
        }
    }
}
