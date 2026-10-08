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
 * Coordinates authorized world operations, bounded planning, and persistent maze state.
 * <p>
 * MazeService owns records and reservations and delegates operation validation
 * to focused collaborators. Bounded workers generate topology, serial storage
 * tasks persist journals and snapshots, and MazeJobRunner advances live block
 * work within one shared server-tick budget.
 * <p>
 * The api subpackage adapts these operations to public handles and terminal
 * events, while the visual subpackage owns preview and guide sessions. Chunk
 * leases remain shared through this coordinator. Cancellation and recovery retain changed regions
 * when needed; completion does not imply an automatic terrain rollback.
 */
package dev.despical.mazeengine.service;
