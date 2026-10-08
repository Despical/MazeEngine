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

import java.util.Arrays;

/**
 * Stores an immutable logical cell graph with entrance and exit portals.
 * <p>
 * One byte per cell records cardinal passage bits. Constructor input and
 * returned passage arrays are copied so callers cannot mutate the graph after
 * publication. External portals describe boundary openings and are separate
 * from the internal connections used by graph statistics.
 * <p>
 * Breadth-first distances support shortest routes, the entrance-to-exit
 * solution, and cached guide routing. Route arrays contain both endpoints.
 * The layout stores no world coordinates or block states; geometry and origin
 * are supplied separately when interpreting it in the world.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeLayout {

    /**
     * Identifies a boundary cell and the side opening to the outside.
     * <p>
     * The cell index belongs to the layout's logical grid. The side defines the
     * external opening used by the block plan and arrival geometry; it is not an
     * additional internal edge when routing or counting dead ends.
     */
    public record Portal(int cell, Direction side) {
    }

    private final int width, depth;
    private final byte[] passages;
    private final Portal entrance, exit;

    public MazeLayout(int width, int depth, byte[] passages, Portal entrance, Portal exit) {
        this.width = width;
        this.depth = depth;
        this.passages = passages.clone();
        this.entrance = entrance;
        this.exit = exit;
    }

    public int width() {
        return width;
    }

    public int depth() {
        return depth;
    }

    public int size() {
        return passages.length;
    }

    public Portal entrance() {
        return entrance;
    }

    public Portal exit() {
        return exit;
    }

    public byte[] passages() {
        return passages.clone();
    }

    public boolean open(int cell, Direction direction) {
        return (passages[cell] & direction.bit()) != 0;
    }

    public int neighbor(int cell, Direction direction) {
        int x = cell % width + direction.dx, z = cell / width + direction.dz;
        return x < 0 || x >= width || z < 0 || z >= depth ? -1 : z * width + x;
    }

    public int[] distances(int start) {
        int[] distances = new int[size()], queue = new int[size()];
        Arrays.fill(distances, -1);
        int head = 0, tail = 0;
        queue[tail++] = start;
        distances[start] = 0;
        while (head < tail) {
            int cell = queue[head++];
            for (Direction direction : Direction.values()) {
                int next = neighbor(cell, direction);
                if (next >= 0 && open(cell, direction) && distances[next] < 0) {
                    distances[next] = distances[cell] + 1;
                    queue[tail++] = next;
                }
            }
        }
        return distances;
    }

    public int[] solution() {
        return route(entrance.cell(), exit.cell());
    }

    public int[] route(int start, int target) {
        if (start < 0 || start >= size() || target < 0 || target >= size()) {
            throw new IllegalArgumentException("Invalid route endpoint.");
        }
        int[] distance = distances(target);
        if (distance[start] < 0) {
            throw new IllegalArgumentException("No route to target.");
        }
        return route(start, distance);
    }

    public int[] route(int start, int[] distance) {
        if (start < 0 || start >= size() || distance.length != size() || distance[start] < 0) {
            throw new IllegalArgumentException("Invalid route distances.");
        }
        int[] result = new int[distance[start] + 1];
        int cell = start;
        for (int index = 0; index < result.length; index++) {
            result[index] = cell;
            for (Direction direction : Direction.values()) {
                int next = neighbor(cell, direction);
                if (next >= 0 && open(cell, direction) && distance[next] == distance[cell] - 1) {
                    cell = next;
                    break;
                }
            }
        }
        return result;
    }

    public int deadEnds() {
        int count = 0;
        for (byte mask : passages) {
            if (Integer.bitCount(mask & 15) == 1) {
                count++;
            }
        }
        return count;
    }
}
