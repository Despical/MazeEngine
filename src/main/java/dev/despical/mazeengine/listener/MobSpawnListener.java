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

package dev.despical.mazeengine.listener;

import dev.despical.mazeengine.MazeEnginePlugin;

import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Cancels mob spawn attempts within maze footprints when prevention is enabled.
 * <p>
 * The listener handles CreatureSpawnEvent at highest priority and leaves already
 * cancelled events alone. Only Mob entities are considered; the service checks
 * the target world and footprint against both saved records and active planning
 * reservations.
 * <p>
 * The protected footprint extends upward from the maze floor, including wall
 * tops, roofs, and space above them. This policy is configured independently of
 * terrain protection. It prevents new spawns rather than removing mobs that
 * already exist in or above a maze.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
public final class MobSpawnListener implements Listener {

    private final MazeEnginePlugin plugin;

    public MobSpawnListener(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Mob && plugin.mazes().mobSpawnBlockedAt(event.getLocation())) {
            event.setCancelled(true);
        }
    }
}
