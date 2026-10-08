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
 * Connects the public API contracts to the internal world-operation coordinator.
 * <p>
 * Providers resolve immutable registry views, validate operation requests, and
 * adapt service tickets into public handles. Completion is published through the
 * operation future and Bukkit lifecycle events after the service settles its work.
 * <p>
 * These adapters share no mutable world-operation state with API consumers. The
 * owning plugin registers the provider while enabled; request submission and live
 * registry access follow the server-thread rules of the public API.
 */
package dev.despical.mazeengine.service.api;
