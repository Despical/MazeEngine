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

package dev.despical.mazeengine.storage;

import java.util.UUID;

/**
 * Stores a saved custom arrival position and facing in a specific world.
 * <p>
 * The world UUID persists without retaining a Bukkit World. Coordinates are
 * fractional player positions, and yaw and pitch retain the orientation chosen
 * when a custom spawn point is saved.
 * <p>
 * Construction rejects missing world identity, non-finite values, horizontal
 * coordinates outside the supported world extent, and pitch outside -90..90.
 * The value does not check whether the world is loaded or the terrain is safe;
 * destination resolution and teleport checks perform those live validations.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record TeleportPoint(UUID worldId, double x, double y, double z, float yaw, float pitch) {

    public TeleportPoint {
        if (worldId == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw)
            || !Float.isFinite(pitch) || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(pitch) > 90
        ) {
            throw new IllegalArgumentException("Invalid teleport point.");
        }
    }
}
