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
 * Command parsing, native argument trees, and player-facing chat panels.
 * <p>
 * The Brigadier adapter describes typed arguments and suggestions, while ACF
 * handlers provide the common execution and permission path. Specialized
 * handlers resolve creation and teleport requests, and the dispatcher connects
 * management commands to the maze and visual services.
 * <p>
 * Chat panels share typography, pagination, theme colors, and owner-bound
 * callbacks. They present current records and configured presets without
 * performing terrain changes while rendering. Callback execution returns to
 * the service layer for ownership, expiry, and exact-record checks.
 */
package dev.despical.mazeengine.command;
