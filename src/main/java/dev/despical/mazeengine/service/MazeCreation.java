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
import dev.despical.mazeengine.api.operation.OperationKind;
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.MazeGenerator;
import dev.despical.mazeengine.storage.MazeRecord;
import dev.despical.mazeengine.storage.MazeRepository;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

/**
 * Validates a new maze specification and starts asynchronous topology planning.
 * <p>
 * The creation path normalizes the name, rejects existing records, checks grid
 * and region limits, and resolves required snapshot support before reserving
 * the identifier. Planning bounds, world, and sender are retained while the
 * worker generates a graph from the supplied seed and frozen preset.
 * <p>
 * The callback verifies the reservation token and loaded world before inserting
 * a PREPARING record and creating a journaled MazeJob. Planning failures settle
 * the ticket without inventing a saved maze. World writes and persistence are
 * owned by the job and the service's separate execution budgets.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeCreation {

    private final MazeService service;
    private final MazeEnginePlugin plugin;

    MazeCreation(MazeService service) {
        this.service = service;
        this.plugin = service.plugin;
    }

    void execute(CommandSender sender, String name, World world, int x, int y, int z, int width, int depth, long seed,
        Preset preset) {
        name = MazeRepository.normalize(name);

        if (service.records.containsKey(name)) {
            throw new MazeService.MazeExistsException(name);
        }

        if (width < 2 || depth < 2 || (long) width * depth > plugin.settings().maxCells()) {
            throw new IllegalArgumentException("Invalid cell dimensions.");
        }

        Bounds bounds = new Bounds(x, y, z, Bounds.span(width, preset.pathWidth(), preset.wallThickness()),
            preset.height(), Bounds.span(depth, preset.pathWidth(), preset.wallThickness()));
        service.validateRegion(world, bounds, width * depth, name);

        if (preset.snapshot()) {
            service.bridge();
        }

        service.reserve(name, OperationKind.CREATE);
        service.operations.planningBounds.put(name, bounds);
        service.operations.planningWorlds.put(name, world.getUID());
        service.operations.planningSenders.put(name, sender);
        final String id = name;
        UUID token = service.operations.operationTokens.get(id);
        UUID owner = sender instanceof Player player ? player.getUniqueId() : new UUID(0, 0);
        service.plan(id, token, () -> new MazeGenerator().generate(width, depth, seed, preset.generation()),
            (layout, error) -> {
                if (!token.equals(service.operations.operationTokens.get(id)) || service.closed()) {
                    return;
                }

                service.clearPlanning(id);

                if (error != null) {
                    service.settle(id, null, error);
                    service.report(sender, error);

                    return;
                }

                if (Bukkit.getWorld(world.getUID()) != world) {
                    var failure = new IllegalArgumentException("World unloaded while planning.");
                    service.settle(id, null, failure);
                    service.report(sender, failure);

                    return;
                }

                var record = new MazeRecord(id, world.getUID(), world.getName(), owner, bounds, seed, preset, layout,
                    MazeRecord.Status.PREPARING, false, Instant.now(), "");
                service.records.put(id, record);
                service.jobs.put(id, MazeJob.create(service, sender, world, record));
            });
        plugin.messages().send(sender, "creation-queued", "name", id, "preset", preset.displayName(), "cells",
            width + "×" + depth);
    }
}
