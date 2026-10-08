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

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;

/**
 * Prevents player and block interactions from disrupting active maze operations.
 * <p>
 * While a saved maze is busy, the listener blocks entry and teleportation into
 * its volume, denies relevant block interactions, and suppresses block physics.
 * Inventory opening, clicking, dragging, and hopper transfers are also checked
 * against inventories located inside a busy maze.
 * <p>
 * World unload is cancelled when the world still contains a busy saved record.
 * These guards protect an in-progress mutation independently of the ordinary
 * terrain-protection toggle. The listener delegates spatial lookup and busy
 * state to the service instead of maintaining its own operation registry.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
public final class OperationSafetyListener implements Listener {

    private final MazeEnginePlugin plugin;

    public OperationSafetyListener(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void physics(BlockPhysicsEvent event) {
        var mazeRecord = plugin.mazes().at(event.getBlock().getLocation());

        if (mazeRecord != null && plugin.mazes().busy(mazeRecord.name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        var mazeRecord = plugin.mazes().at(event.getClickedBlock().getLocation());
        if (mazeRecord != null && plugin.mazes().busy(mazeRecord.name())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void inventory(InventoryOpenEvent event) {
        Location location = event.getInventory().getLocation();
        var mazeRecord = location == null ? null : plugin.mazes().at(location);

        if (mazeRecord != null && plugin.mazes().busy(mazeRecord.name())) {
            event.setCancelled(true);
        }
    }

    private boolean activeInventory(Inventory inventory) {
        Location location = inventory.getLocation();
        var mazeRecord = location == null ? null : plugin.mazes().at(location);
        return mazeRecord != null && plugin.mazes().busy(mazeRecord.name());
    }

    @EventHandler(ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        if (activeInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void drag(InventoryDragEvent event) {
        if (activeInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void hopper(InventoryMoveItemEvent event) {
        if (activeInventory(event.getSource()) || activeInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        var to = event.getTo();
        var mazeRecord = plugin.mazes().at(to);

        if (mazeRecord != null && plugin.mazes().busy(mazeRecord.name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) {
        var to = event.getTo();
        var mazeRecord = plugin.mazes().at(to);

        if (mazeRecord != null && plugin.mazes().busy(mazeRecord.name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void unload(WorldUnloadEvent event) {
        var mazeService = plugin.mazes();

        if (mazeService.records().stream().anyMatch(record -> record.worldId().equals(event.getWorld().getUID()) && mazeService.busy(record.name()))) {
            event.setCancelled(true);
        }
    }
}
