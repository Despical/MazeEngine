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

import dev.despical.mazeengine.config.Preset;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Verifies that a bounded preview mesh preserves the complete maze structure.
 * <p>
 * The tests expand merged wall boxes to check coverage of every wall position
 * without covering passage positions. A normal maze must remain fully represented
 * while using substantially fewer boxes than individual wall blocks.
 * <p>
 * Display-limit failures must reject construction rather than return a partial
 * mesh. The fixture evaluates geometry before any display entities are spawned.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class PreviewMeshTest {

    @Test
    void fullMeshCoversEveryWallExactlyOnceWithoutCoveringPassages() {
        for (int path : new int[] { 1, 3, 5 }) {
            for (int wall : new int[] { 1, 2, 4 }) {
                for (boolean roof : new boolean[] { false, true }) {
                    Preset preset = BlockPlanTest.preset(path, wall, roof);
                    var layout = new MazeGenerator().generate(12, 9, 482, preset.generation());
                    var boxes = PreviewMesh.create(layout, preset, 12000);
                    int width = Bounds.span(layout.width(), path, wall),
                        depth = Bounds.span(layout.depth(), path, wall);
                    int[][] coverage = new int[depth][width];
                    for (var box : boxes) {
                        if (box.kind() == PreviewMesh.Kind.WALL) {
                            assertEquals(preset.wallHeight(), box.height());
                            for (int z = box.z(); z < box.z() + box.depth(); z++) {
                                for (int x = box.x(); x < box.x() + box.width(); x++) {
                                    coverage[z][x]++;
                                }
                            }
                        }
                    }
                    var plan = new BlockPlan(layout, preset, 482);
                    for (int z = 0; z < depth; z++) {
                        for (int x = 0; x < width; x++) {
                            assertEquals(plan.passage(x, z) ? 0 : 1, coverage[z][x]);
                        }
                    }
                    assertEquals(roof ? 1 : 0, boxes.stream().filter(candidate -> candidate.kind() == PreviewMesh.Kind.ROOF).count());
                    assertEquals(1, boxes.stream().filter(candidate -> candidate.kind() == PreviewMesh.Kind.ENTRANCE).count());
                    assertEquals(1, boxes.stream().filter(candidate -> candidate.kind() == PreviewMesh.Kind.EXIT).count());
                    var floor = boxes.stream().filter(candidate -> candidate.kind() == PreviewMesh.Kind.FLOOR).findFirst()
                        .orElseThrow();
                    assertEquals(width, floor.width());
                    assertEquals(depth, floor.depth());
                }
            }
        }
    }

    @Test
    void normalMazeIsFullyRepresentedWithFarFewerDisplaysAndLimitsNeverTruncate() {
        var preset = BlockPlanTest.preset(3, 1, false);
        var layout = new MazeGenerator().generate(21, 21, 93, preset.generation());
        List<PreviewMesh.Box> boxes = PreviewMesh.create(layout, preset, 12000);
        assertTrue(boxes.size() < 1000);
        assertThrows(IllegalArgumentException.class, () -> PreviewMesh.create(layout, preset, 10));
    }
}
