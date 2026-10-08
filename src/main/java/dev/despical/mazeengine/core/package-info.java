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

/**
 * Deterministic graph generation, routing, and local maze geometry.
 * <p>
 * GenerationSettings and a fixed seed drive MazeGenerator without live Bukkit
 * world access. MazeLayout stores copied passage masks and boundary portals,
 * and its breadth-first searches support shortest routes and graph statistics.
 * PERFECT graphs are trees; BRAIDED settings may add loops while retaining
 * connectivity.
 * <p>
 * Bounds converts between grid dimensions, block spans, and indexed traversal.
 * BlockPlan resolves coordinate-stable structure and decorations lazily, and
 * PreviewMesh merges wall runs within a display budget. Live block placement
 * and entity ownership belong to the service layer.
 */
package dev.despical.mazeengine.core;
