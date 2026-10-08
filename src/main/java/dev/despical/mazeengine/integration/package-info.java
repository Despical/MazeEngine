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
 * Adapters for optional external plugins and original terrain snapshots.
 * <p>
 * WorldEditBridge resolves loaded-world cuboid selections and full block-state
 * clipboard snapshots. WorldGuardBridge checks placement and breaking rights
 * for plugin-driven edits, while MazePlaceholders publishes copied metadata
 * for PlaceholderAPI requests without asynchronous world lookups.
 * <p>
 * Optional dependency classes are reached only when their feature is available
 * or requested. Snapshot capture and restore run on the server thread; completed
 * schematic reads and writes use the storage executor. Integration startup and
 * cleanup are owned by the plugin and its service lifecycle.
 */
package dev.despical.mazeengine.integration;
