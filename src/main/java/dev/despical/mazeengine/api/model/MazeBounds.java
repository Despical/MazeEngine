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

import java.util.Objects;

/**
 * Describes an immutable inclusive volume of world blocks.
 * <p>
 * The origin is the minimum block corner. Each dimension is a block count, so
 * the maximum X coordinate is {@code origin.x() + width - 1}; the same rule
 * applies to Y and Z. Maze bounds include the floor and any generated roof.
 * <p>
 * Construction validates positive dimensions, maximum-coordinate overflow,
 * and a volume representable as a long. It does not require a loaded world or
 * check the world border, build-height limits, or the server's maze-size limits.
 *
 * @param origin the minimum corner and world identity
 * @param width the positive block count along X
 * @param height the positive block count along Y
 * @param depth the positive block count along Z
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record MazeBounds(MazeLocation origin, int width, int height, int depth) {

    /**
     * Constructs a block volume with representable coordinates and size.
     * <p>
     * All dimensions must be positive. Inclusive maximum coordinates are checked
     * with exact arithmetic before the volume can be used for containment queries.
     *
     * @param origin the minimum world block corner
     * @param width the block count along X
     * @param height the block count along Y
     * @param depth the block count along Z
     * @throws NullPointerException if {@code origin} is null
     * @throws IllegalArgumentException if any dimension is nonpositive
     * @throws ArithmeticException if a maximum coordinate or the volume overflows
     */
    public MazeBounds {
        Objects.requireNonNull(origin, "origin");

        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("Invalid bounds.");
        }

        Math.addExact(origin.x(), width - 1);
        Math.addExact(origin.y(), height - 1);
        Math.addExact(origin.z(), depth - 1);
        Math.multiplyExact(Math.multiplyExact((long) width, height), depth);
    }

    /**
     * Returns the number of blocks contained in this volume.
     * <p>
     * This includes air positions as well as solid blocks. It is the full bounded
     * volume, not the count of structure blocks in a generation plan.
     *
     * @return the product of width, height, and depth
     */
    public long volume() {
        return (long) width * height * depth;
    }

    /**
     * Tests whether a world block position lies inside this inclusive volume.
     * <p>
     * The point must have the same world UUID and fall within all three coordinate
     * ranges. A position on any minimum or maximum face is included.
     *
     * @param point the world block position to test
     * @return true when the world and every coordinate are within the bounds
     * @throws NullPointerException if {@code point} is null
     */
    public boolean contains(MazeLocation point) {
        return origin.worldId().equals(point.worldId())
                && point.x() >= origin.x()
                && point.x() <= origin.x() + width - 1
                && point.y() >= origin.y()
                && point.y() <= origin.y() + height - 1
                && point.z() >= origin.z()
                && point.z() <= origin.z() + depth - 1;
    }
}
