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

import dev.despical.mazeengine.config.Palette;
import dev.despical.mazeengine.config.Preset;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Verifies that generated block plans faithfully represent the saved cell graph.
 * <p>
 * The tests inspect passage connectivity across corridor and wall dimensions,
 * check the two external portals, and exercise deterministic palettes and
 * decorations. Roof generation must not obstruct the logical passage routes.
 * <p>
 * Coordinates are evaluated through the lazy plan without writing live blocks,
 * keeping structural correctness separate from server tick and storage behavior.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class BlockPlanTest {

    static Preset preset(int pathWidth, int wallThickness, boolean roof) {
        Palette floor = new Palette(List.of(new Palette.Entry("floorA", 3), new Palette.Entry("floorB", 1)));
        Palette wall = new Palette(List.of(new Palette.Entry("wall", 1)));
        return Preset.builder("test").pathWidth(pathWidth).wallThickness(wallThickness).roof(roof)
            .floor(floor).wall(wall).ceiling(wall).cap(wall).capChance(0.2).lightSpacing(3)
            .light("light").entranceMarker("entrance").exitMarker("exit").build();
    }

    @Test
    void blockPathsMatchCellEdgesAtAllWidthsAndExactlyTwoBoundaryPortalsExist() {
        for (int pathWidth : new int[] { 1, 2, 3, 7 }) {
            for (int wallThickness : new int[] { 1, 2, 4 }) {
                for (long seed = 0; seed < 10; seed++) {
                    var preset = preset(pathWidth, wallThickness, false);
                    var layout = new MazeGenerator().generate(7, 5, seed, preset.generation());
                    var plan = new BlockPlan(layout, preset, seed);
                    int stride = pathWidth + wallThickness, spanX = Bounds.span(layout.width(), pathWidth, wallThickness), spanZ = Bounds.span(layout.depth(), pathWidth, wallThickness);
                    int boundaryOpenings = 0;
                    for (int x = 0; x < spanX; x++) {
                        if (plan.passage(x, 0)) {
                            boundaryOpenings++;
                        }
                        if (plan.passage(x, spanZ - 1)) {
                            boundaryOpenings++;
                        }
                    }
                    for (int z = 0; z < spanZ; z++) {
                        if (plan.passage(0, z)) {
                            boundaryOpenings++;
                        }
                        if (plan.passage(spanX - 1, z)) {
                            boundaryOpenings++;
                        }
                    }
                    assertEquals(2 * pathWidth, boundaryOpenings);
                    for (int cell = 0; cell < layout.size(); cell++) {
                        int x = wallThickness + (cell % layout.width()) * stride, z = wallThickness + (cell / layout.width()) * stride;
                        assertTrue(plan.passage(x, z));
                        assertEquals("minecraft:air", plan.block(x, 1, z));
                        if (cell % layout.width() < layout.width() - 1) {
                            assertEquals(layout.open(cell, Direction.EAST), plan.passage(x + pathWidth, z));
                        }
                        if (cell / layout.width() < layout.depth() - 1) {
                            assertEquals(layout.open(cell, Direction.SOUTH), plan.passage(x, z + pathWidth));
                        }
                    }
                    boolean[][] visited = new boolean[spanX][spanZ];
                    ArrayDeque<int[]> queue = new ArrayDeque<>();
                    int startX = wallThickness + layout.entrance().cell() % layout.width() * stride,
                        startZ = wallThickness + layout.entrance().cell() / layout.width() * stride;
                    visited[startX][startZ] = true;
                    queue.add(new int[] { startX, startZ });
                    while (!queue.isEmpty()) {
                        var point = queue.remove();
                        for (var direction : Direction.values()) {
                            int nx = point[0] + direction.dx, nz = point[1] + direction.dz;
                            if (nx >= 0 && nz >= 0 && nx < spanX && nz < spanZ && !visited[nx][nz]
                                && plan.passage(nx, nz)) {
                                visited[nx][nz] = true;
                                queue.add(new int[] { nx, nz });
                            }
                        }
                    }
                    for (int x = 0; x < spanX; x++) {
                        for (int z = 0; z < spanZ; z++) {
                            if (plan.passage(x, z)) {
                                assertTrue(visited[x][z]);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void paletteDistributionAndDecorationAreDeterministicAndRoofDoesNotBlockRoutes() {
        var preset = preset(3, 1, true);
        var layout = new MazeGenerator().generate(5, 5, 4, preset.generation());
        var plan = new BlockPlan(layout, preset, 4);
        var same = new BlockPlan(layout, preset, 4);
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 0; y < preset.height(); y++) {
                    assertEquals(plan.block(x, y, z), same.block(x, y, z));
                }
            }
        }
        assertEquals("wall", plan.block(2, preset.height() - 1, 2));
        Palette palette = new Palette(List.of(new Palette.Entry("A", 3), new Palette.Entry("B", 1)));
        int firstCount = 0;
        for (int index = 0; index < 10000; index++) {
            if (palette.choose(BlockPlan.mix(index)).equals("A")) {
                firstCount++;
            }
        }
        assertTrue(firstCount > 7250 && firstCount < 7750);
        assertThrows(IllegalArgumentException.class, () -> new Palette(List.of(new Palette.Entry("A", 0))));
    }
}
