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

/**
 * Resolves the planned block state at a local coordinate without storing a volume.
 * <p>
 * The plan combines an immutable cell graph, frozen preset, and generation seed
 * to determine floors, walls, roof blocks, lights, portal markers, and optional
 * decorations. A coordinate hash keeps palette and decoration choices stable
 * regardless of the order in which blocks are queried.
 * <p>
 * Passage checks translate local X/Z block coordinates into graph connections
 * and external portals. Generation jobs and visual geometry reuse that same
 * interpretation. Memory follows the saved graph and preset rather than the
 * number of positions in the three-dimensional world volume.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class BlockPlan {

    private final MazeLayout layout;
    private final Preset preset;
    private final long seed;

    public BlockPlan(MazeLayout layout, Preset preset, long seed) {
        this.layout = layout;
        this.preset = preset;
        this.seed = seed;
    }

    public MazeLayout layout() {
        return layout;
    }

    public String block(int x, int y, int z) {
        long hash = mix(seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B97F4A7C15L) ^ (z * 0xC6BC279692B5CC83L));
        boolean path = passage(x, z);
        if (y == 0) {
            if (portal(x, z, layout.entrance())) {
                return preset.entranceMarker();
            }
            if (portal(x, z, layout.exit())) {
                return preset.exitMarker();
            }
            int stride = preset.pathWidth() + preset.wallThickness();
            if (path && preset.lightSpacing() > 0 && x % stride == preset.wallThickness() + preset.pathWidth() / 2
                && z % stride == preset.wallThickness() + preset.pathWidth() / 2
                && ((x / stride) + (z / stride)) % preset.lightSpacing() == 0) {
                return preset.light();
            }
            return preset.floor().choose(hash, x, z);
        }
        if (preset.roof() && y == preset.height() - 1) {
            return preset.ceiling().choose(hash, x, z);
        }
        if (path) {
            return "minecraft:air";
        }
        if (preset.wallInlay() != null && y >= 2 && y < preset.wallHeight() - 1
            && y == Math.max(2, preset.wallHeight() / 2) && x > 0 && z > 0
            && x < Bounds.span(layout.width(), preset.pathWidth(), preset.wallThickness()) - 1
            && z < Bounds.span(layout.depth(), preset.pathWidth(), preset.wallThickness()) - 1 && !passage(x - 1, z)
            && !passage(x + 1, z) && !passage(x, z - 1) && !passage(x, z + 1)
            && (mix(hash) >>> 11) * 0x1.0p-53 < preset.inlayChance()) {
            return preset.wallInlay().choose(hash);
        }
        if (y == preset.wallHeight() && (mix(hash) >>> 11) * 0x1.0p-53 < preset.capChance()) {
            return preset.cap().choose(hash, x, z);
        }
        return preset.wall().choose(hash, x, z);
    }

    public boolean passage(int x, int z) {
        if (portal(x, z, layout.entrance()) || portal(x, z, layout.exit())) {
            return true;
        }
        int wallThickness = preset.wallThickness(), pathWidth = preset.pathWidth(), stride = wallThickness + pathWidth;
        int cx = (x - wallThickness) / stride, cz = (z - wallThickness) / stride;
        if (x < wallThickness || z < wallThickness || cx >= layout.width() || cz >= layout.depth()) {
            return false;
        }
        int rx = (x - wallThickness) % stride, rz = (z - wallThickness) % stride;
        if (rx < pathWidth && rz < pathWidth) {
            return true;
        }
        int cell = cz * layout.width() + cx;
        if (rx >= pathWidth && rz < pathWidth) {
            return cx + 1 < layout.width() && layout.open(cell, Direction.EAST);
        }
        if (rz >= pathWidth && rx < pathWidth) {
            return cz + 1 < layout.depth() && layout.open(cell, Direction.SOUTH);
        }
        return false;
    }

    private boolean portal(int x, int z, MazeLayout.Portal portal) {
        int wallThickness = preset.wallThickness(), pathWidth = preset.pathWidth(), stride = wallThickness + pathWidth;
        int cx = portal.cell() % layout.width(), cz = portal.cell() / layout.width();
        int startX = wallThickness + cx * stride, startZ = wallThickness + cz * stride;
        int spanX = Bounds.span(layout.width(), pathWidth, wallThickness), spanZ = Bounds.span(layout.depth(), pathWidth, wallThickness);
        return switch (portal.side()) {
            case NORTH -> x >= startX && x < startX + pathWidth && z < wallThickness;
            case SOUTH -> x >= startX && x < startX + pathWidth && z >= spanZ - wallThickness;
            case WEST -> z >= startZ && z < startZ + pathWidth && x < wallThickness;
            case EAST -> z >= startZ && z < startZ + pathWidth && x >= spanX - wallThickness;
        };
    }

    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
