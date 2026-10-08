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
 * Persistent maze records, graph validation, and temporary-file replacement.
 * <p>
 * MazeRepository stores one YAML document per normalized identifier, including
 * world identity, owner, origin, graph, portals, frozen preset, lifecycle state,
 * and custom teleport point. Invalid schemas, malformed graphs, and overlapping
 * saved regions abort loading instead of silently losing occupied maze areas.
 * <p>
 * MazeRecord replacements describe state transitions without mutating retained
 * values. AtomicFiles requests atomic replacement and provides a fallback for
 * filesystems that do not support it. Operation recovery and live world changes
 * are coordinated by services after repository validation.
 */
package dev.despical.mazeengine.storage;
