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
 * Minecraft event guards for saved maze terrain and active operations.
 * <p>
 * TerrainProtectionListener handles player edits and environmental block
 * changes, OperationSafetyListener guards movement, inventories, physics, and
 * world unload during work, and MobSpawnListener prevents configured mob births
 * within maze footprints from the floor upward.
 * <p>
 * Listeners consult current service records and operation state rather than
 * maintaining another maze registry. The normal terrain toggle and player
 * bypass do not remove busy-operation safeguards. WorldGuard checks for
 * plugin-driven writes are performed by the operation integration layer.
 */
package dev.despical.mazeengine.listener;
