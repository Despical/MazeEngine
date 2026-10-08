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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.concurrent.CancellationException;

/**
 * Generates a deterministic connected cell graph using a growing-tree algorithm.
 * <p>
 * A seed-local SplittableRandom chooses the initial cell, frontier exploration,
 * and weighted directions. Reciprocal connections build a spanning tree;
 * BRAIDED mode can then introduce extra edges at dead ends while retaining
 * connectivity. Generation periodically checks interruption for cancellation.
 * <p>
 * An external boundary entrance is selected and the exit is chosen from the
 * farthest other boundary cells by graph distance. The result contains topology
 * only, so planning can run on bounded workers without accessing Bukkit worlds
 * or placing blocks. Complexity affects biases rather than promising a score.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeGenerator {

    public MazeLayout generate(int width, int depth, long seed, GenerationSettings settings) {
        if (width < 2 || depth < 2 || (long) width * depth > 1_000_000) {
            throw new IllegalArgumentException("Grid must be at least 2x2 and at most 1,000,000 cells.");
        }
        SplittableRandom random = new SplittableRandom(seed);
        int size = width * depth, start = random.nextInt(size), count = 1;
        byte[] graph = new byte[size];
        boolean[] visited = new boolean[size];
        int[] frontier = new int[size], positions = new int[size], previous = new int[size], following = new int[size],
            arrival = new int[size];
        Arrays.fill(previous, -1);
        Arrays.fill(following, -1);
        int newest = start;
        Arrays.fill(arrival, -1);
        frontier[0] = start;
        visited[start] = true;
        while (count > 0) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            int index = random.nextDouble() < settings.branching() ? random.nextInt(count) : positions[newest];
            int cell = frontier[index], chosen = -1;
            double total = 0;
            for (Direction direction : Direction.values()) {
                int next = neighbor(cell, direction, width, depth);
                if (next < 0 || visited[next]) {
                    continue;
                }
                double turn = arrival[cell] == direction.ordinal() ? 1 - settings.turnBias() : settings.turnBias();
                double axis = direction.dx == 0 ? 1 - settings.horizontalBias() : settings.horizontalBias();
                double weight = (0.02 + turn) * (0.02 + axis);
                total += weight;
                if (random.nextDouble() * total < weight) {
                    chosen = direction.ordinal();
                }
            }
            if (chosen < 0) {
                if (previous[cell] >= 0) {
                    following[previous[cell]] = following[cell];
                }
                if (following[cell] >= 0) {
                    previous[following[cell]] = previous[cell];
                }
                if (newest == cell) {
                    newest = previous[cell];
                }
                int moved = frontier[--count];
                frontier[index] = moved;
                positions[moved] = index;
                continue;
            }
            Direction chosenDirection = Direction.values()[chosen];
            int next = neighbor(cell, chosenDirection, width, depth);
            graph[cell] |= (byte) chosenDirection.bit();
            graph[next] |= (byte) chosenDirection.opposite().bit();
            arrival[next] = chosen;
            visited[next] = true;
            previous[next] = newest;
            following[newest] = next;
            newest = next;
            positions[next] = count;
            frontier[count++] = next;
        }
        if (settings.mode() == GenerationSettings.Mode.BRAIDED) {
            for (int cell = 0; cell < size; cell++) {
                if (Integer.bitCount(graph[cell] & 15) != 1 || random.nextDouble() >= settings.braidChance()) {
                    continue;
                }
                Direction[] directions = Direction.values();
                for (int shuffleIndex = 3; shuffleIndex > 0; shuffleIndex--) {
                    int otherIndex = random.nextInt(shuffleIndex + 1);
                    Direction swappedDirection = directions[shuffleIndex];
                    directions[shuffleIndex] = directions[otherIndex];
                    directions[otherIndex] = swappedDirection;
                }
                for (Direction direction : directions) {
                    int next = neighbor(cell, direction, width, depth);
                    if (next >= 0 && (graph[cell] & direction.bit()) == 0) {
                        graph[cell] |= (byte) direction.bit();
                        graph[next] |= (byte) direction.opposite().bit();
                        break;
                    }
                }
            }
        }
        var boundary = new ArrayList<MazeLayout.Portal>();
        for (int x = 0; x < width; x++) {
            boundary.add(new MazeLayout.Portal(x, Direction.NORTH));
            boundary.add(new MazeLayout.Portal((depth - 1) * width + x, Direction.SOUTH));
        }
        for (int z = 0; z < depth; z++) {
            boundary.add(new MazeLayout.Portal(z * width, Direction.WEST));
            boundary.add(new MazeLayout.Portal(z * width + width - 1, Direction.EAST));
        }
        var entrance = boundary.get(random.nextInt(boundary.size()));
        var initial = new MazeLayout(width, depth, graph, entrance, entrance);
        int[] distances = initial.distances(entrance.cell());
        var exit = entrance;
        int farthest = -1;
        for (var portal : boundary) {
            if (portal.cell() != entrance.cell() && distances[portal.cell()] > farthest) {
                farthest = distances[portal.cell()];
                exit = portal;
            }
        }
        return new MazeLayout(width, depth, graph, entrance, exit);
    }

    private static int neighbor(int cell, Direction direction, int width, int depth) {
        int x = cell % width + direction.dx, z = cell / width + direction.dz;
        return x < 0 || x >= width || z < 0 || z >= depth ? -1 : z * width + x;
    }
}
