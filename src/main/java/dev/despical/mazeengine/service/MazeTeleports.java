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

package dev.despical.mazeengine.service;

import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Set;

/**
 * Resolves maze arrival coordinates and checks current destination safety.
 * <p>
 * The default entrance is centered in its logical corridor, one block above
 * the floor, and faces inward from the portal side. A saved custom teleport
 * point can select another loaded world and retain exact coordinates and
 * player orientation.
 * <p>
 * Safety checks require clear, non-liquid feet and head space, a solid
 * nonhazardous floor, world-border inclusion, and no busy maze at the point.
 * Resolution alone does not teleport a player. The command handler loads the
 * arrival chunk and rechecks state before performing the actual teleport.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeTeleports {

    private final MazeService service;

    MazeTeleports(MazeService service) {
        this.service = service;
    }

    public Location entrance(MazeRecord record) {
        var portal = record.layout().entrance();
        int stride = record.preset().pathWidth() + record.preset().wallThickness();
        double x = record.bounds().x() + record.preset().wallThickness()
            + (portal.cell() % record.layout().width()) * stride + record.preset().pathWidth() / 2.0;
        double z = record.bounds().z() + record.preset().wallThickness()
            + (portal.cell() / record.layout().width()) * stride + record.preset().pathWidth() / 2.0;
        float yaw = switch (portal.side()) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> -90;
        };

        return new Location(service.world(record), x, record.bounds().y() + 1, z, yaw, 0);
    }

    public Location destination(MazeRecord record) {
        var teleport = record.teleportPoint();

        if (teleport == null) {
            return entrance(record);
        }

        World world = Bukkit.getWorld(teleport.worldId());

        if (world == null) {
            throw new IllegalArgumentException("The teleport world is not loaded.");
        }

        return new Location(world, teleport.x(), teleport.y(), teleport.z(), teleport.yaw(), teleport.pitch());
    }

    public boolean safeDestination(Location location) {
        var floor = location.clone().add(0, -0.05, 0).getBlock();

        return location.getBlock().isPassable() && !location.getBlock().isLiquid()
            && location.clone().add(0, 1, 0).getBlock().isPassable()
            && !location.clone().add(0, 1, 0).getBlock().isLiquid() && floor.getType().isSolid()
            && !Set.of(Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS)
                .contains(floor.getType())
            && location.getWorld().getWorldBorder().isInside(location) && !busyAt(location);
    }

    private boolean busyAt(Location location) {
        var record = service.at(location);

        return record != null && service.busy(record.name());
    }
}
