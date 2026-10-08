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
 * Validated configuration values, preset codecs, and message presentation.
 * <p>
 * Loaders check supported keys, scalar types, numeric ranges, block states,
 * and required resources before the plugin publishes replacement Settings.
 * Failed reloads retain the previous active configuration. Bundled defaults
 * install missing files without overwriting existing administrator edits.
 * <p>
 * Presets combine geometry, generation biases, palettes, decoration, and
 * display metadata. Saved mazes freeze their preset independently of later
 * reloads. Messages and panel helpers render MiniMessage templates and select
 * contextual actions using the sender's permissions.
 */
package dev.despical.mazeengine.config;
