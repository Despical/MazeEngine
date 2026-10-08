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

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.core.BlockPlan;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Updates private route arrows inside the passages of a saved maze.
 * <p>
 * The player's world position is mapped to a logical cell only when it lies
 * within the maze passage footprint. A changed cell selects a shortest route
 * from cached exit distances, then draws a bounded look-ahead segment and an
 * outward exit arrow when the route reaches the portal.
 * <p>
 * Arrow rods and wings must fit the complete passage footprint before being
 * displayed. Obsolete displays and unused chunk leases are released as the
 * desired geometry changes. The arrows are visual entities and never replace
 * terrain blocks or change the saved topology.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class GuideGeometry {

    private final MazeEnginePlugin plugin;

    GuideGeometry(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    void update(Player player, MazeRecord record, GuideSession solver) {
        Bounds bounds = record.bounds();
        Location location = player.getLocation();
        int x = location.getBlockX() - bounds.x(), z = location.getBlockZ() - bounds.z();
        int cell = -1;

        if (x >= 0 && z >= 0 && x < bounds.width() && z < bounds.depth() && location.getY() >= bounds.y()
            && location.getY() < bounds.maxY() + 2 && solver.plan.passage(x, z)) {
            int wall = record.preset().wallThickness(), stride = wall + record.preset().pathWidth();
            int cx = Math.max(0, Math.min(record.layout().width() - 1, (x - wall) / stride));
            int cz = Math.max(0, Math.min(record.layout().depth() - 1, (z - wall) / stride));
            cell = cz * record.layout().width() + cx;
        }

        if (cell != solver.cell) {
            solver.cell = cell;
            solver.desired = new LinkedHashSet<>();

            if (cell >= 0) {
                int[] route = record.layout().route(cell, solver.distances);

                for (int index = 0; index < Math.min(route.length - 1,
                    plugin.settings().visuals().guideLookAhead()); index++) {
                    arrow(solver.desired, center(record, route[index]), center(record, route[index + 1]),
                        record.preset().pathWidth(), solver.plan, bounds);
                }

                if (route.length <= plugin.settings().visuals().guideLookAhead() + 1) {
                    Location end = center(record, record.layout().exit().cell());
                    var side = record.layout().exit().side();
                    arrow(solver.desired, end,
                        end.clone().add(side.dx * (record.preset().pathWidth() + record.preset().wallThickness()), 0,
                            side.dz * (record.preset().pathWidth() + record.preset().wallThickness())),
                        record.preset().pathWidth(), solver.plan, bounds);
                }
            }
        }

        Set<VisualBlock> desired = solver.desired;

        for (var entry : new ArrayList<>(solver.blocks.entrySet())) {
            if (!desired.contains(entry.getKey())) {
                entry.getValue().remove();
                solver.blocks.remove(entry.getKey());
            }
        }

        for (VisualBlock block : desired) {
            if (!solver.blocks.containsKey(block)) {
                solver.spawn(player, block);
            }
        }

        solver.retainChunks(desired.stream().map(block -> block.chunkKey(solver.world)).collect(Collectors.toSet()));
    }

    private Location center(MazeRecord record, int cell) {
        int stride = record.preset().pathWidth() + record.preset().wallThickness();
        double offset = record.preset().wallThickness() + record.preset().pathWidth() / 2.0;

        return new Location(Bukkit.getWorld(record.worldId()),
            record.bounds().x() + cell % record.layout().width() * stride + offset,
            record.bounds().y() + plugin.settings().visuals().guideElevation(),
            record.bounds().z() + (double) cell / record.layout().width() * stride + offset);
    }

    private void arrow(Set<VisualBlock> blocks, Location from, Location to, int pathWidth, BlockPlan plan,
        Bounds bounds) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        Location tip = from.clone().add(direction.clone().multiply(Math.min(from.distance(to) * 0.6, 2.0)));
        Vector side = new Vector(-direction.getZ(), 0, direction.getX());
        double size = Math.min(1.0, pathWidth * 0.7);
        Set<VisualBlock> arrow = new LinkedHashSet<>();
        rod(arrow, tip.clone().subtract(direction.clone().multiply(size)), tip);

        for (int sign : new int[] { -1, 1 }) {
            rod(arrow, tip, tip.clone().subtract(direction.clone().multiply(size * 0.5))
                .add(side.clone().multiply(sign * size * 0.35)));
        }

        // Check the whole footprint, including arrowhead wings, rather than just its center.
        if (arrow.stream().allMatch(block -> block.fitsPassage(plan, bounds))) {
            blocks.addAll(arrow);
        }
    }

    private void rod(Set<VisualBlock> blocks, Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        Vector side = new Vector(-delta.getZ(), 0, delta.getX()).normalize()
            .multiply(plugin.settings().visuals().guideWidth() / 2);
        blocks.add(
            new VisualBlock(from.getX() - side.getX(), from.getY(), from.getZ() - side.getZ(), (float) delta.length(),
                (float) plugin.settings().visuals().guideThickness(), (float) plugin.settings().visuals().guideWidth(),
                (float) Math.atan2(-delta.getZ(), delta.getX()), plugin.settings().visuals().guideBlock()));
    }
}
