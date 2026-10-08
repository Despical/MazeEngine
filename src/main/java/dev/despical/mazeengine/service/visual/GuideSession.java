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

package dev.despical.mazeengine.service.visual;

import dev.despical.mazeengine.core.BlockPlan;
import dev.despical.mazeengine.core.MazeLayout;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Retains the routing and display state for one player's active guide.
 * <p>
 * The session captures the maze name and layout identity, computes distances
 * from the exit once, and creates the shared passage plan. The current cell
 * and desired arrow set are updated as the player moves through the maze.
 * <p>
 * The base display session owns the viewer, expiry token, entities, and chunk
 * leases. Layout identity allows the visual service to end a guide after the
 * maze is regenerated instead of drawing a route from an obsolete graph.
 * The mutable session state is maintained on the server thread.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class GuideSession extends DisplaySession {

    final String name;
    final MazeLayout layout;
    final int[] distances;
    final BlockPlan plan;
    int cell = -2;
    Set<VisualBlock> desired = Set.of();

    GuideSession(MazeVisuals owner, Player player, MazeRecord record) {
        super(owner, player, player.getWorld(), owner.plugin.settings().visuals().guideDurationSeconds() * 1000L);
        name = record.name();
        layout = record.layout();
        distances = layout.distances(layout.exit().cell());
        plan = new BlockPlan(layout, record.preset(), record.seed());
    }
}
