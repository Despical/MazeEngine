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

package dev.despical.mazeengine.command;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Coordinates a player's asynchronous arrival at a saved maze destination.
 * <p>
 * The handler resolves the custom spawn point or default entrance and loads
 * the destination chunk before teleporting. Once loading completes, it returns
 * to the server thread and rechecks the player, saved record, operation state,
 * and current arrival safety.
 * <p>
 * The delayed checks prevent a command issued against an earlier record from
 * teleporting into a changed or busy maze. Player-facing feedback uses the
 * configured message panels. Resolving coordinates alone is not treated as a
 * guarantee that the destination is safe when the player actually arrives.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeTeleportCommand {

    private final MazeEnginePlugin plugin;

    MazeTeleportCommand(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    void execute(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 1, 1);

        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Only players can teleport.");
        }

        var service = plugin.mazes();
        var record = service.require(options.positional.getFirst());

        if (record.status() != MazeRecord.Status.READY || service.busy(record.name())) {
            throw new IllegalArgumentException("Maze is not ready.");
        }

        var destination = service.destination(record);
        destination.getWorld().getChunkAtAsync(destination).thenAccept(
            chunk -> plugin.getServer().getScheduler().runTask(plugin, () -> teleport(player, record, destination)));
    }

    private void teleport(Player player, MazeRecord record, Location destination) {
        var service = plugin.mazes();
        if (!player.isOnline() || service.busy(record.name()) || service.records().stream()
            .noneMatch(current -> current == record && current.status() == MazeRecord.Status.READY)) {
            return;
        }

        if (!service.safeDestination(destination)) {
            plugin.messages().send(player, "error", "reason", "Teleport point is obstructed or has no safe floor.");
            return;
        }

        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                plugin.getServer().getScheduler().runTask(plugin, () -> completed(player, record));
            }
        });
    }

    private void completed(Player player, MazeRecord record) {
        plugin.messages().send(player, "teleported", "name", record.name(), "destination",
            plugin.messages().values().get(record.teleportPoint() == null ? "teleport-entrance" : "teleport-custom"));
    }
}
