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
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.nio.file.Files;

/**
 * Starts authorized removal of a saved maze and its occupied block volume.
 * <p>
 * Deletion checks ownership, the loaded maze world, and players within the
 * volume. Restoration additionally requires the WorldEdit bridge and a complete
 * original snapshot file. Once accepted, the name is reserved and a DELETING
 * record replaces the current saved state.
 * <p>
 * A MazeJob restores indexed terrain or clears the bounds before removing
 * metadata and snapshot storage. Chat confirmation is owned by PendingActions
 * and is not implemented here, allowing the same deletion path to serve
 * already-authorized API and command requests.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeDeletion {

    private final MazeService service;
    private final MazeEnginePlugin plugin;

    MazeDeletion(MazeService service) {
        this.service = service;
        this.plugin = service.plugin;
    }

    void execute(CommandSender sender, MazeRecord record, boolean restore) {
        service.authorize(sender, record, "delete");
        World world = service.world(record);
        service.ensureEmptyOfPlayers(world, record.bounds());

        if (restore) {
            service.bridge();

            if (!record.hasSnapshot() || !Files.isRegularFile(service.snapshotPath(record))) {
                throw new IllegalArgumentException("This maze has no complete original snapshot.");
            }
        }

        service.reserve(record.name(), OperationKind.DELETE);
        var target = record.state(MazeRecord.Status.DELETING, record.hasSnapshot(), "");
        service.records.put(target.name(), target);
        service.jobs.put(target.name(), MazeJob.delete(service, sender, world, target, restore));
    }
}
