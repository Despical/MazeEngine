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

import dev.despical.mazeengine.core.BlockPlan;
import dev.despical.mazeengine.core.Bounds;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Describes world-anchored block-display geometry and its material.
 * <p>
 * The value retains a corner, dimensions, Y rotation, and portable block-state
 * string. Anchor and transformation helpers place the corresponding display
 * without camera-relative billboarding and compute the chunk needed by the
 * rotated geometry.
 * <p>
 * Passage fitting checks every horizontal footprint corner against the shared
 * BlockPlan, so guide arrow wings cannot extend through a wall merely because
 * their center is inside a corridor. The value does not spawn entities or
 * change terrain; DisplaySession owns those live resources.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
record VisualBlock(double x, double y, double z, float width, float height, float depth, float yaw, String blockData) {

    VisualBlock(double x, double y, double z, float width, float height, float depth, float yaw, Material material) {
        this(x, y, z, width, height, depth, yaw, material.getKey().toString());
    }

    Location anchor(World world) {
        double cos = Math.cos(yaw), sin = Math.sin(yaw);

        return new Location(world, x + (width * cos + depth * sin) / 2, y + height / 2,
            z + (-width * sin + depth * cos) / 2, 0, 0);
    }

    long chunkKey(World world) {
        Location anchor = anchor(world);

        return (long) (anchor.getBlockX() >> 4) << 32 | (anchor.getBlockZ() >> 4) & 0xffffffffL;
    }

    boolean fitsPassage(BlockPlan plan, Bounds bounds) {
        for (double dx : new double[] { 0, width }) {
            for (double dz : new double[] { 0, depth }) {
                int px = (int) Math.floor(x + dx * Math.cos(yaw) + dz * Math.sin(yaw)) - bounds.x();
                int pz = (int) Math.floor(z - dx * Math.sin(yaw) + dz * Math.cos(yaw)) - bounds.z();

                if (px < 0 || pz < 0 || px >= bounds.width() || pz >= bounds.depth() || !plan.passage(px, pz)) {
                    return false;
                }
            }
        }

        return true;
    }

    Transformation transform(Location anchor) {
        return new Transformation(
            new Vector3f((float) (x - anchor.getX()), (float) (y - anchor.getY()), (float) (z - anchor.getZ())),
            new Quaternionf().rotateY(yaw), new Vector3f(width, height, depth), new Quaternionf());
    }
}
