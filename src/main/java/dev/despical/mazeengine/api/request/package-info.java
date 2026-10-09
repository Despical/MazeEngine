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
 * Immutable request values for submitting maze creation with preset overrides.
 * <p>
 * {@link dev.despical.mazeengine.api.request.CreateMazeRequest} specifies the
 * maze identifier, world origin, logical grid size, and selected preset. Its
 * builder provides optional overrides for generation and placement settings.
 * Nullable constructor overrides inherit the current preset when absent,
 * except that an absent seed is generated at submission. Explicit {@code false}
 * and zero values remain supplied overrides rather than absence markers.
 * <p>
 * Construction validates local request values only. Submit a built request
 * through {@link dev.despical.mazeengine.api.MazeOperations#create} on the
 * server thread while MazeEngine is enabled. The service resolves the current
 * preset and checks the actor's permissions, world availability, configured
 * limits, and conflicting reservations before accepting an operation.
 * <p>
 * A successfully constructed request does not reserve a maze name or guarantee
 * that world work will succeed. Submission rejection throws directly; failures
 * after acceptance are reported by the returned operation handle and lifecycle
 * events. Retaining an immutable request does not make its preset reference a
 * frozen copy of the configured preset.
 */
package dev.despical.mazeengine.api.request;
