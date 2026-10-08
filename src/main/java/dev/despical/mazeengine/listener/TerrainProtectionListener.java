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

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * Protects saved maze terrain from player edits and environmental changes.
 * <p>
 * The listener covers block placement and breaking, buckets, pistons, fluids,
 * fire, growth, fading, formation, explosions, and entity block changes.
 * Events that provide changed-block lists can filter protected entries while
 * allowing unrelated changes outside maze volumes.
 * <p>
 * Normal player edits can use the configured terrain-protection bypass, but a
 * busy maze remains protected during world work. Spatial and protection checks
 * come from the current maze service. This listener manages Minecraft events;
 * WorldGuard authorization for plugin-driven edits is handled separately.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
public final class TerrainProtectionListener implements Listener {

    private final MazeEnginePlugin plugin;

    public TerrainProtectionListener(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean locked(Block block) {
        return plugin.mazes().protectedAt(block.getLocation());
    }

    private boolean denied(Player player, Block block) {
        var record = plugin.mazes().at(block.getLocation());
        return record != null && (plugin.mazes().busy(record.name())
            || plugin.settings().protection() && !player.hasPermission("mazeengine.bypass"));
    }

    @EventHandler(ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void multiple(BlockMultiPlaceEvent event) {
        if (event.getReplacedBlockStates().stream().anyMatch(blockState -> denied(event.getPlayer(), blockState.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void fertilize(BlockFertilizeEvent event) {
        event.getBlocks().removeIf(blockState -> locked(blockState.getBlock()));
    }

    @EventHandler(ignoreCancelled = true)
    public void tree(StructureGrowEvent event) {
        event.getBlocks().removeIf(blockState -> locked(blockState.getBlock()));
    }

    @EventHandler(ignoreCancelled = true)
    public void fluid(BlockFromToEvent event) {
        if (locked(event.getToBlock()) || locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void burn(BlockBurnEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void ignite(BlockIgniteEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void spread(BlockSpreadEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void grow(BlockGrowEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void fade(BlockFadeEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void form(BlockFormEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void entityChange(EntityChangeBlockEvent event) {
        if (locked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void explosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::locked);
    }

    @EventHandler(ignoreCancelled = true)
    public void explosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::locked);
    }

    @EventHandler(ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) {
        if (locked(event.getBlock()) || event.getBlocks().stream().anyMatch(block -> locked(block) || locked(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent event) {
        if (locked(event.getBlock()) || event.getBlocks().stream().anyMatch(block -> locked(block) || locked(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void bucket(PlayerBucketEmptyEvent event) {
        if (denied(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void bucket(PlayerBucketFillEvent event) {
        if (denied(event.getPlayer(), event.getBlockClicked())) {
            event.setCancelled(true);
        }
    }
}
