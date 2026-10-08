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
import dev.despical.mazeengine.core.MazeGenerator;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.nio.file.Files;
import java.util.UUID;

/**
 * Plans a full maze rebuild or structural repair while retaining original terrain.
 * <p>
 * The path checks regeneration ownership, the loaded world, region safety,
 * and any required original snapshot before reserving the operation. An
 * unchanged seed reuses the saved layout; a new seed generates replacement
 * topology using the frozen preset and existing grid dimensions.
 * <p>
 * The callback checks its token and world before replacing the record and
 * starting a MazeJob. Repair keeps the seed and graph and skips planned air
 * positions during writes so passage contents survive. The original terrain
 * snapshot is retained rather than recaptured from the maze.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeRegeneration {

    private final MazeService service;
    private final MazeEnginePlugin plugin;

    MazeRegeneration(MazeService service) {
        this.service = service;
        this.plugin = service.plugin;
    }

    void execute(CommandSender sender, MazeRecord record, long seed, boolean repair) {
        if (repair && seed != record.seed()) {
            throw new IllegalArgumentException("Repair cannot change the seed.");
        }

        service.authorize(sender, record, "regenerate");
        World world = service.world(record);
        service.validateRegion(world, record.bounds(), record.layout().size(), record.name());

        if (record.hasSnapshot() && !Files.isRegularFile(service.snapshotPath(record))) {
            throw new IllegalArgumentException("The original snapshot is missing; recover it before regenerating.");
        }

        if (record.preset().snapshot() && !record.hasSnapshot()) {
            throw new IllegalArgumentException(
                "Incomplete initial snapshot. Delete this record before creating again.");
        }

        service.reserve(record.name(), repair ? OperationKind.REPAIR : OperationKind.REGENERATE);
        UUID token = service.operations.operationTokens.get(record.name());
        service.operations.planningSenders.put(record.name(), sender);
        service.plan(record.name(), token,
            () -> seed == record.seed() ? record.layout()
                : new MazeGenerator().generate(record.layout().width(), record.layout().depth(), seed,
                    record.preset().generation()),
            (layout, error) -> {
                if (!token.equals(service.operations.operationTokens.get(record.name())) || service.closed()) {
                    return;
                }

                service.clearPlanning(record.name());

                if (error != null) {
                    service.settle(record.name(), record, error);
                    service.report(sender, error);

                    return;
                }

                if (Bukkit.getWorld(world.getUID()) != world) {
                    var failure = new IllegalArgumentException("World unloaded while planning.");
                    service.settle(record.name(), record, failure);
                    service.report(sender, failure);

                    return;
                }

                var target = record.regenerate(seed, layout);
                service.records.put(target.name(), target);
                service.jobs.put(target.name(), repair ? MazeJob.repair(service, sender, world, target)
                    : MazeJob.regenerate(service, sender, world, target));
            });
        plugin.messages().send(sender, repair ? "repair-queued" : "regenerate-queued", "name", record.name(), "preset",
            record.preset().displayName(), "cells", record.layout().width() + "×" + record.layout().depth());
    }
}
