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

package dev.despical.mazeengine.core;

/**
 * Describes an inclusive block volume and its indexed traversal coordinates.
 * <p>
 * The origin is the minimum block corner and dimensions count blocks, including
 * the maximum faces. Construction requires positive dimensions and checks
 * maximum-coordinate arithmetic so a region cannot wrap around integer limits.
 * <p>
 * Indexed conversion visits X within each Z row, then advances the Y layer.
 * Grid sizing helpers convert between logical cell counts and structural block
 * spans. Bounds contains no world identity; callers compare worlds separately
 * when checking overlap or locating a saved maze.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record Bounds(int x, int y, int z, int width, int height, int depth) {

    public Bounds {
        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("Invalid region dimensions.");
        }
        Math.addExact(x, width - 1);
        Math.addExact(y, height - 1);
        Math.addExact(z, depth - 1);
    }

    public long volume() {
        return (long) width * height * depth;
    }

    public int maxX() {
        return x + width - 1;
    }

    public int maxY() {
        return y + height - 1;
    }

    public int maxZ() {
        return z + depth - 1;
    }

    public boolean overlaps(Bounds other) {
        return x <= other.maxX() && maxX() >= other.x && y <= other.maxY() && maxY() >= other.y && z <= other.maxZ()
            && maxZ() >= other.z;
    }

    public int localX(long index) {
        return (int) (index % width);
    }

    public int localZ(long index) {
        return (int) ((index / width) % depth);
    }

    public int localY(long index) {
        return (int) (index / ((long) width * depth));
    }

    public static int span(int cells, int path, int wall) {
        return Math.toIntExact((long) cells * (path + wall) + wall);
    }

    public static int fit(int blocks, int path, int wall) {
        return (blocks - wall) / (path + wall);
    }
}
