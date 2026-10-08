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

import java.util.Objects;
import java.util.UUID;

/**
 * Stores a world block position without retaining a live Bukkit world.
 * <p>
 * The UUID identifies the world persistently, even while it is unloaded. X, Y,
 * and Z are integer block coordinates and may be negative. This value does not
 * check build height, world borders, or whether the world currently exists.
 * <p>
 * Creation requests interpret this position as the maze's minimum block corner
 * and floor layer. Route endpoints instead use {@link CellPosition} indices.
 *
 * @param worldId the persistent world UUID
 * @param x the world block X coordinate
 * @param y the world block Y coordinate
 * @param z the world block Z coordinate
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record MazeLocation(UUID worldId, int x, int y, int z) {

    /**
     * Constructs a block position with a required world identity.
     * <p>
     * No Bukkit world lookup occurs here. APIs that resolve a live destination
     * or submit world work check world availability separately.
     *
     * @param worldId the world UUID
     * @param x the world block X coordinate
     * @param y the world block Y coordinate
     * @param z the world block Z coordinate
     * @throws NullPointerException if {@code worldId} is null
     */
    public MazeLocation {
        Objects.requireNonNull(worldId, "worldId");
    }
}
