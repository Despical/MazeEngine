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

import static org.junit.jupiter.api.Assertions.*;

import dev.despical.mazeengine.storage.MazeRepository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Verifies topology invariants, deterministic generation, and shortest routing.
 * <p>
 * The tests check reciprocal connected passages, tree properties of PERFECT
 * mazes, cycles in BRAIDED mazes, seed stability, and farthest-boundary exit
 * selection. Routes must remain shortest and valid from every tested cell.
 * <p>
 * Additional cases exercise extreme dimensions, complexity values, and region
 * fitting arithmetic. All checks run against pure graph and sizing values
 * without constructing or modifying a Bukkit world.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class MazeGeneratorTest {

    @Test
    void solverFindsShortestRoutesFromEveryCellIncludingBraidedLoops() {
        for (GenerationSettings settings : List.of(GenerationSettings.fromComplexity(0.8),
            new GenerationSettings(0.5, 0.4, 0.5, 0.5, GenerationSettings.Mode.BRAIDED, 1))) {
            var maze = new MazeGenerator().generate(12, 9, 742, settings);
            int target = maze.exit().cell();
            int[] distances = maze.distances(target);
            for (int start = 0; start < maze.size(); start++) {
                int[] route = maze.route(start, target);
                assertEquals(start, route[0]);
                assertEquals(target, route[route.length - 1]);
                assertEquals(distances[start] + 1, route.length);
                for (int index = 1; index < route.length; index++) {
                    int from = route[index - 1], next = route[index];
                    assertTrue(Arrays.stream(Direction.values())
                        .anyMatch(direction -> maze.neighbor(from, direction) == next && maze.open(from, direction)));
                }
            }
            assertArrayEquals(new int[] { target }, maze.route(target, target));
            assertThrows(IllegalArgumentException.class, () -> maze.route(-1, target));
        }
    }

    @Test
    void perfectGraphsAreConnectedSymmetricTreesAndExitIsFarthestBoundary() {
        for (long seed = -25; seed <= 25; seed++) {
            for (double complexity : new double[] { 0, 0.5, 1 }) {
                MazeLayout layout = new MazeGenerator().generate(13, 9, seed, GenerationSettings.fromComplexity(complexity));
                MazeRepository.validateGraph(layout);
                int edges = 0;
                for (byte value : layout.passages()) {
                    edges += Integer.bitCount(value & 15);
                }
                assertEquals((layout.size() - 1) * 2, edges);
                int[] distances = layout.distances(layout.entrance().cell());
                int farthest = 0;
                for (int cell = 0; cell < layout.size(); cell++) {
                    if (cell % layout.width() == 0 || cell % layout.width() == layout.width() - 1 || cell / layout.width() == 0
                        || cell / layout.width() == layout.depth() - 1) {
                        farthest = Math.max(farthest, distances[cell]);
                    }
                }
                assertEquals(farthest, distances[layout.exit().cell()]);
                assertEquals(farthest + 1, layout.solution().length);
                assertNotEquals(layout.entrance().cell(), layout.exit().cell());
            }
        }
    }

    @Test
    void fixedSeedIsStableAndDifferentSeedsChangeGraph() {
        var settings = GenerationSettings.fromComplexity(0.7);
        var firstLayout = new MazeGenerator().generate(30, 20, 987654321, settings);
        var secondLayout = new MazeGenerator().generate(30, 20, 987654321, settings);
        assertArrayEquals(firstLayout.passages(), secondLayout.passages());
        assertEquals(firstLayout.entrance(), secondLayout.entrance());
        assertEquals(firstLayout.exit(), secondLayout.exit());
        assertFalse(Arrays.equals(firstLayout.passages(), new MazeGenerator().generate(30, 20, 987654322, settings).passages()));
    }

    @Test
    void braidedGraphsStayConnectedAndAddCycles() {
        var settings = new GenerationSettings(0.5, 0.4, 0.5, 0.5, GenerationSettings.Mode.BRAIDED, 1);
        var layout = new MazeGenerator().generate(20, 20, 99, settings);
        MazeRepository.validateGraph(layout);
        int edges = 0;
        for (byte passages : layout.passages()) {
            edges += Integer.bitCount(passages & 15);
        }
        assertTrue(edges > 2 * (layout.size() - 1));
        assertTrue(layout.solution().length > 1);
    }

    @Test
    void extremeDimensionsAndComplexityRemainValid() {
        for (int[] dim : List.of(new int[] { 2, 2 }, new int[] { 2, 80 }, new int[] { 80, 2 },
            new int[] { 200, 200 })) {
            MazeRepository
                .validateGraph(new MazeGenerator().generate(dim[0], dim[1], 42, GenerationSettings.fromComplexity(1)));
        }
        assertThrows(IllegalArgumentException.class, () -> GenerationSettings.fromComplexity(Double.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> new MazeGenerator().generate(1, 9, 1, GenerationSettings.fromComplexity(0.5)));
        assertThrows(IllegalArgumentException.class,
            () -> new GenerationSettings(0.5, 0, 0, 0, GenerationSettings.Mode.PERFECT, 0.1));
    }

    @Test
    void fittingNeverOverflowsSelectedArea() {
        for (int path = 1; path <= 16; path++) {
            for (int wall = 1; wall <= 8; wall++) {
                for (int extent = 40; extent <= 150; extent++) {
                    int cells = Bounds.fit(extent, path, wall);
                    if (cells >= 2) {
                        assertTrue(Bounds.span(cells, path, wall) <= extent);
                        assertTrue(Bounds.span(cells + 1, path, wall) > extent);
                    }
                }
            }
        }
        assertTrue(new Bounds(-5, 1, 3, 6, 4, 7).overlaps(new Bounds(0, 1, 3, 4, 2, 2)));
        assertFalse(new Bounds(-5, 1, 3, 5, 4, 7).overlaps(new Bounds(0, 1, 3, 4, 2, 2)));
    }
}
