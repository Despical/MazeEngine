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

package dev.despical.mazeengine.integration;

import dev.despical.mazeengine.core.Bounds;

import org.bukkit.World;

/**
 * Carries a resolved WorldEdit cuboid selection and its loaded Bukkit world.
 * <p>
 * WorldEditBridge translates the selected minimum and maximum coordinates into
 * inclusive block Bounds. The command layer uses those bounds to compute the
 * largest fitting maze grid and center its structure within the selected area.
 * <p>
 * Unlike public API location values, this internal result retains a live World
 * reference. It belongs to server-thread selection resolution and is not a
 * portable persistence value or a guarantee that a later operation still fits
 * the current world border and configured limits.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record RegionSelection(World world, Bounds bounds) {
}
