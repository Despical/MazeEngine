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
import dev.despical.mazeengine.command.ChatUI;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns short-lived review tokens for destructive chat actions.
 * <p>
 * Deletion requests capture the issuing actor, exact saved record, terrain
 * policy, and expiry before rendering confirm and dismiss controls. Confirm
 * checks actor identity, lifetime, and record identity again so an old request
 * cannot remove a regenerated maze or a replacement record under the same name.
 * <p>
 * Creation undo buttons use the same exact-record safeguard before delegating
 * to authorized deletion. Expired requests are periodically discarded, player
 * disconnect removes their requests, and shutdown clears the remaining tokens.
 * The actual world removal still belongs to MazeService.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class PendingActions implements Listener, AutoCloseable {

    private final MazeEnginePlugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final BukkitTask cleanup;

    /**
     * Captures the exact record and actor authorized for a review request.
     * <p>
     * The terrain policy and expiry belong to the issued token. Confirmation
     * compares the captured record by identity against the current service record
     * so an older button cannot act on a changed maze.
     */
    private record Pending(UUID owner, MazeRecord record, boolean restore, long expires) {
    }

    public PendingActions(MazeEnginePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cleanup = plugin.getServer().getScheduler().runTaskTimer(plugin,
            () -> pending.values().removeIf(pendingAction -> pendingAction.expires < System.currentTimeMillis()), 20,
            20);
    }

    private UUID owner(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : new UUID(0, 0);
    }

    public Component undoButton(CommandSender sender, MazeRecord created) {
        var messages = plugin.messages();

        return new ChatUI(plugin).callbackButton(sender, "button-undo", "hover-undo", "#FF5555", 60, clicked -> {
            if (plugin.mazes().require(created.name()) != created) {
                messages.send(clicked, "undo-unavailable", "name", created.name());

                return;
            }

            plugin.mazes().delete(clicked, created, created.hasSnapshot());
        }).hoverEvent(HoverEvent.showText(messages.format("hover-undo", "method",
            ChatUI.wrap(messages.values().get(created.hasSnapshot() ? "undo-restore" : "undo-clear"), 42))));
    }

    public void delete(CommandSender sender, MazeRecord record, boolean restore) {
        plugin.mazes().authorize(sender, record, "delete");

        if (plugin.mazes().busy(record.name())) {
            throw new IllegalArgumentException("Maze is busy.");
        }

        pending.values().removeIf(pendingAction -> pendingAction.owner.equals(owner(sender)));
        UUID token = UUID.randomUUID();
        pending.put(token, new Pending(owner(sender), record, restore, System.currentTimeMillis() + 60_000));
        var messages = plugin.messages();
        String method = messages.values().get(restore ? "delete-mode-restore" : "delete-mode-clear");
        var ui = new ChatUI(plugin);
        Component confirm = ui
            .callbackButton(sender, "button-delete", "delete-hover", "#C44B5B", 60, "confirm", token.toString())
            .hoverEvent(HoverEvent.showText(messages.format("delete-hover", "name", record.name(), "method", method)));
        Component cancel = ui.callbackButton(sender, "button-cancel", "hover-cancel", "#8792A6", 60, "dismiss",
            token.toString());
        ui.delete(sender, record, restore, cancel.append(Component.text("  ")).append(confirm));

        if (!(sender instanceof Player)) {
            messages.send(sender, "line", "text", "Confirm within 60s: maze confirm " + token);
        }
    }

    public void confirm(CommandSender sender, String token) {
        UUID key = UUID.fromString(token);
        Pending action = pending.get(key);

        if (action == null || !action.owner.equals(owner(sender)) || action.expires < System.currentTimeMillis()) {
            plugin.messages().send(sender, "confirm-expired");

            return;
        }

        pending.remove(key);

        if (plugin.mazes().require(action.record.name()) != action.record) {
            throw new IllegalArgumentException("Maze changed. Request deletion again.");
        }

        plugin.mazes().delete(sender, action.record, action.restore);
    }

    public void dismiss(CommandSender sender, String token) {
        UUID key = UUID.fromString(token);
        Pending action = pending.get(key);

        if (action == null || !action.owner.equals(owner(sender)) || action.expires < System.currentTimeMillis()) {
            plugin.messages().send(sender, "action-already-dismissed");

            return;
        }

        pending.remove(key);
        plugin.messages().send(sender, "action-dismissed", "name", action.record.name());
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        pending.values().removeIf(pendingAction -> pendingAction.owner.equals(event.getPlayer().getUniqueId()));
    }

    @Override
    public void close() {
        cleanup.cancel();
        pending.clear();
    }
}
