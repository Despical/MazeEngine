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

package dev.despical.mazeengine.api.model;

import java.util.UUID;

/**
 * Describes an exact arrival position and orientation in a world.
 * <p>
 * Unlike {@link MazeLocation}, this value retains fractional coordinates and
 * player-facing angles. Y is the position of the player's feet; yaw and pitch
 * use Bukkit's angle conventions in degrees.
 * <p>
 * No live Bukkit world is retained. Registry destination queries resolve a
 * loaded world before returning this value, but a saved custom destination in
 * a maze snapshot may refer to an unloaded world. Converting it to a Bukkit
 * Location requires looking up its world UUID.
 * <p>
 * This value describes coordinates only. Constructing it does not validate
 * terrain safety or teleport a player.
 *
 * @param worldId the target world UUID
 * @param x       the exact world X coordinate
 * @param y       the exact world Y coordinate at the player's feet
 * @param z       the exact world Z coordinate
 * @param yaw     the horizontal facing angle in degrees
 * @param pitch   the vertical facing angle in degrees
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record TeleportDestination(UUID worldId, double x, double y, double z, float yaw, float pitch) {
}
