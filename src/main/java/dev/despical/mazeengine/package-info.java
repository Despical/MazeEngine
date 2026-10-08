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
 * Plugin lifecycle and the shared entry point for MazeEngine.
 * <p>
 * MazeEnginePlugin coordinates resource installation, validated settings,
 * saved-maze recovery, commands, listeners, optional integrations, and private
 * visual sessions. It registers the public API as a Bukkit service only while
 * the plugin is enabled and closes its owned resources during shutdown.
 * <p>
 * Implementation responsibilities are split between configuration, pure graph
 * planning, world-operation services, storage, commands, and event listeners.
 * Integrating plugins should use the public API contracts rather than retaining
 * internal service instances across disable or reload.
 */
package dev.despical.mazeengine;
