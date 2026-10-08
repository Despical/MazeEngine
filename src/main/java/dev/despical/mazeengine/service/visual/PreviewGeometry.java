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

package dev.despical.mazeengine.service.visual;

import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.BlockPlan;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.MazeLayout;
import dev.despical.mazeengine.core.PreviewMesh;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * Converts a complete maze mesh into bounded, themed private-display geometry.
 * <p>
 * The builder samples BlockPlan materials for floors, walls, caps, and portal
 * markers and converts local mesh positions to world coordinates. It tries
 * progressively larger tiles to fit the display budget while keeping wall
 * caps one block high.
 * <p>
 * Roof geometry is omitted from the visible preview so the interior can be
 * inspected. Construction checks interruption and rejects an oversized preview
 * instead of returning partial geometry. The result is a list of VisualBlock
 * descriptions; entity spawning and chunk loading happen in the session layer.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class PreviewGeometry {

    private final int MAX_DISPLAYS;

    PreviewGeometry(int maxDisplays) {
        MAX_DISPLAYS = maxDisplays;
    }

    List<VisualBlock> create(MazeLayout layout, Preset preset, long seed, Bounds bounds, List<PreviewMesh.Box> mesh) {
        var plan = new BlockPlan(layout, preset, seed);

        for (int tile = 1; tile <= 4; tile++) {
            List<VisualBlock> blocks = tiledPreview(plan, preset, bounds, mesh, tile);

            if (blocks != null) {
                return blocks;
            }
        }

        throw new IllegalArgumentException("This preview is too detailed. Use fewer cells.");
    }

    private List<VisualBlock> tiledPreview(BlockPlan plan, Preset preset, Bounds bounds, List<PreviewMesh.Box> mesh,
        int tile) {
        List<VisualBlock> blocks = new ArrayList<>();

        for (var box : mesh) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }

            if (box.kind() == PreviewMesh.Kind.ROOF) {
                continue;
            }

            for (int z = 0; z < box.depth(); z += tile) {
                for (int x = 0; x < box.width(); x += tile) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new CancellationException();
                    }

                    int width = Math.min(tile, box.width() - x), depth = Math.min(tile, box.depth() - z);
                    int localX = box.x() + x, localZ = box.z() + z;

                    switch (box.kind()) {
                        case FLOOR -> blocks.add(new VisualBlock(bounds.x() + localX, bounds.y(), bounds.z() + localZ,
                            width, 1, depth, 0, plan.block(localX + width / 2, 0, localZ + depth / 2)));
                        case WALL -> {
                            // Keep caps one block high, while limiting all other stretched axes to
                            // the tile size.
                            for (int y = 1; y < preset.wallHeight(); y += tile) {
                                int height = Math.min(tile, preset.wallHeight() - y);
                                blocks.add(new VisualBlock(bounds.x() + localX, bounds.y() + y, bounds.z() + localZ,
                                    width, height, depth, 0,
                                    plan.block(localX + width / 2, y + height / 2, localZ + depth / 2)));
                            }

                            blocks.add(new VisualBlock(bounds.x() + localX, bounds.y() + preset.wallHeight(),
                                bounds.z() + localZ, width, 1, depth, 0,
                                plan.block(localX + width / 2, preset.wallHeight(), localZ + depth / 2)));
                        }
                        case ENTRANCE, EXIT -> blocks.add(new VisualBlock(bounds.x() + localX, bounds.y() + 1.01,
                            bounds.z() + localZ, width, 0.02f, depth, 0,
                            box.kind() == PreviewMesh.Kind.ENTRANCE ? preset.entranceMarker() : preset.exitMarker()));
                        case ROOF -> {
                        }
                    }

                    if (blocks.size() > MAX_DISPLAYS) {
                        return null;
                    }
                }
            }
        }

        return blocks;
    }
}
