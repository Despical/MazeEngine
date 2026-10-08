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

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Validates maze regions and resolves their spatial protection rules.
 * <p>
 * Submission checks include cell count, volume, chunk count, build height,
 * world border, players, and overlap with saved or planning regions. A preview
 * can validate fit without applying the player-exclusion check used for edits.
 * Saved-record lookup compares the world and inclusive three-dimensional bounds.
 * <p>
 * Terrain protection combines saved volumes with active-operation state.
 * Mob-spawn checks instead use the X/Z footprint from the floor upward,
 * including wall tops and roofs, and account for topology-planning
 * reservations. These live world queries belong on the server thread.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeRegions {

    private final MazeEnginePlugin plugin;
    private final MazeService service;

    MazeRegions(MazeEnginePlugin plugin, MazeService service) {
        this.plugin = plugin;
        this.service = service;
    }

    void validate(World world, Bounds bounds, int cells, String ignore) {
        validate(world, bounds, cells, ignore, true);
    }

    void validate(World world, Bounds bounds, int cells, String ignore, boolean edit) {
        var settings = plugin.settings();

        if (cells > settings.maxCells() || bounds.volume() > settings.maxVolume()) {
            throw new IllegalArgumentException("Maze exceeds configured cell or block limits.");
        }

        long chunks = (long) ((bounds.maxX() >> 4) - (bounds.x() >> 4) + 1)
            * ((bounds.maxZ() >> 4) - (bounds.z() >> 4) + 1);

        if (chunks > settings.maxChunks()) {
            throw new IllegalArgumentException("Maze exceeds the chunk limit.");
        }

        if (bounds.y() < world.getMinHeight() || bounds.maxY() >= world.getMaxHeight()) {
            throw new IllegalArgumentException("Maze would exceed world height.");
        }

        if (edit) {
            ensureEmptyOfPlayers(world, bounds);
        }

        var border = world.getWorldBorder();

        if (!border.isInside(new Location(world, bounds.x(), bounds.y(), bounds.z()))
            || !border.isInside(new Location(world, bounds.maxX() + 0.999, bounds.y(), bounds.maxZ() + 0.999))) {
            throw new IllegalArgumentException("Maze would exceed the world border.");
        }

        for (var record : service.records.values()) {
            if (!record.name().equals(ignore) && record.worldId().equals(world.getUID())
                && record.bounds().overlaps(bounds)) {
                throw new IllegalArgumentException("Region overlaps maze: " + record.name());
            }
        }

        for (var entry : service.operations.planningBounds.entrySet()) {
            if (!entry.getKey().equals(ignore)
                && service.operations.planningWorlds.get(entry.getKey()).equals(world.getUID())
                && entry.getValue().overlaps(bounds)) {
                throw new IllegalArgumentException("Region overlaps a pending generation: " + entry.getKey());
            }
        }
    }

    void ensureEmptyOfPlayers(World world, Bounds bounds) {
        for (Player player : world.getPlayers()) {
            Location location = player.getLocation();

            if (location.getX() + 0.3 >= bounds.x() && location.getX() - 0.3 < bounds.maxX() + 1
                && location.getZ() + 0.3 >= bounds.z() && location.getZ() - 0.3 < bounds.maxZ() + 1
                && location.getY() + 1.8 >= bounds.y() && location.getY() < bounds.maxY() + 1) {
                throw new IllegalArgumentException("Move all players outside the maze region before editing it.");
            }
        }
    }

    public MazeRecord at(Location location) {
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();

        for (var record : service.records.values()) {
            if (record.worldId().equals(location.getWorld().getUID())) {
                var bounds = record.bounds();

                if (x >= bounds.x() && x <= bounds.maxX() && y >= bounds.y() && y <= bounds.maxY() && z >= bounds.z()
                    && z <= bounds.maxZ()) {
                    return record;
                }
            }
        }

        return null;
    }

    public boolean protectedAt(Location location) {
        var record = at(location);

        return record != null && (service.busy(record.name()) || plugin.settings().protection());
    }

    /**
     * Prevent mob births within maze footprints, from their floor upwards, including roof and wall
     * tops.
     */
    public boolean mobSpawnBlockedAt(Location location) {
        UUID world = location.getWorld().getUID();

        for (var record : service.records.values()) {
            if (record.worldId().equals(world) && aboveFootprint(location, record.bounds())) {
                return true;
            }
        }

        for (var entry : service.operations.planningBounds.entrySet()) {
            if (world.equals(service.operations.planningWorlds.get(entry.getKey()))
                && aboveFootprint(location, entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    static boolean aboveFootprint(Location location, Bounds bounds) {
        return location.getX() >= bounds.x() && location.getX() < (double) bounds.maxX() + 1
            && location.getZ() >= bounds.z() && location.getZ() < (double) bounds.maxZ() + 1
            && location.getY() >= bounds.y();
    }
}
