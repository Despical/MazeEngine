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

import dev.despical.mazeengine.config.Preset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * Builds a bounded box mesh representing the complete planned maze footprint.
 * <p>
 * The mesh merges identical wall runs across neighboring rows while retaining
 * every passage opening. Separate boxes mark the floor, entrance, exit, and
 * optional roof, allowing previews to show the full structure with fewer
 * display entities than one entity per block.
 * <p>
 * Mesh construction uses the same passage interpretation as BlockPlan and
 * checks interruption during row traversal. If the display limit would be
 * exceeded, it rejects the preview rather than returning a truncated maze.
 * The result contains local geometry and performs no world mutations.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class PreviewMesh {

    /**
     * Identifies the structural role of one preview mesh box.
     * <p>
     * Wall, floor, portal, and roof roles let the themed display builder choose
     * their materials and visible geometry. The role is presentation metadata and
     * does not alter the maze graph represented by the mesh.
     */
    public enum Kind {

        WALL, FLOOR, ENTRANCE, EXIT, ROOF
    }

    /**
     * Stores one local rectangular region in the preview mesh.
     * <p>
     * The minimum corner and extents describe merged structure relative to the
     * maze origin. Fractional Y and height support thin portal and roof markers;
     * the display layer adds world coordinates and material sampling separately.
     */
    public record Box(int x, double y, int z, int width, double height, int depth, Kind kind) {
    }

    /**
     * Identifies a contiguous horizontal wall run in one mesh row.
     * <p>
     * Matching start and width values allow neighboring Z rows to extend an active
     * wall box. Runs that do not continue are committed to the mesh, preserving
     * passage openings while reducing the number of boxes.
     */
    private record Run(int x, int width) {
    }

    private PreviewMesh() {
    }

    public static List<Box> create(MazeLayout layout, Preset preset, int limit) {
        int width = Bounds.span(layout.width(), preset.pathWidth(), preset.wallThickness());
        int depth = Bounds.span(layout.depth(), preset.pathWidth(), preset.wallThickness());
        var plan = new BlockPlan(layout, preset, 0);
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(0, 0.02, 0, width, 0.05, depth, Kind.FLOOR));
        Map<Run, Box> active = new LinkedHashMap<>();
        for (int z = 0; z < depth; z++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            Map<Run, Box> next = new LinkedHashMap<>();
            for (int x = 0; x < width;) {
                if (plan.passage(x, z)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < width && !plan.passage(x, z)) {
                    x++;
                }
                Run run = new Run(start, x - start);
                Box old = active.remove(run);
                next.put(run, old == null ? new Box(start, 1, z, run.width, preset.wallHeight(), 1, Kind.WALL)
                    : new Box(old.x, old.y, old.z, old.width, old.height, old.depth + 1, old.kind));
            }
            boxes.addAll(active.values());
            active = next;
            if (boxes.size() + active.size() + 3 > limit) {
                throw new IllegalArgumentException("This preview is too detailed. Use fewer cells.");
            }
        }
        boxes.addAll(active.values());
        boxes.add(portal(layout, preset, layout.entrance(), Kind.ENTRANCE));
        boxes.add(portal(layout, preset, layout.exit(), Kind.EXIT));
        if (preset.roof()) {
            boxes.add(new Box(0, preset.height() - 1, 0, width, 0.04, depth, Kind.ROOF));
        }
        return List.copyOf(boxes);
    }

    private static Box portal(MazeLayout layout, Preset preset, MazeLayout.Portal portal, Kind kind) {
        int wallThickness = preset.wallThickness(), pathWidth = preset.pathWidth(), stride = wallThickness + pathWidth;
        int x = wallThickness + portal.cell() % layout.width() * stride, z = wallThickness + portal.cell() / layout.width() * stride;
        return switch (portal.side()) {
            case NORTH -> new Box(x, 0.09, 0, pathWidth, 0.06, wallThickness, kind);
            case SOUTH -> new Box(x, 0.09, Bounds.span(layout.depth(), pathWidth, wallThickness) - wallThickness, pathWidth, 0.06, wallThickness, kind);
            case WEST -> new Box(0, 0.09, z, wallThickness, 0.06, pathWidth, kind);
            case EAST -> new Box(Bounds.span(layout.width(), pathWidth, wallThickness) - wallThickness, 0.09, z, wallThickness, 0.06, pathWidth, kind);
        };
    }
}
