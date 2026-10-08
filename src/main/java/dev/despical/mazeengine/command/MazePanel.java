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
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders saved maze listings and detailed management panels.
 * <p>
 * The panels present identity, owner, lifecycle state, grid dimensions,
 * generation settings, and saved topology information using the shared chat
 * theme. Listings paginate the available records rather than sending every
 * maze in one unbounded chat message.
 * <p>
 * Management buttons are selected for the sender's permissions and maze state.
 * They invoke the normal command routes for teleport, guide, regeneration,
 * or deletion. Rendering a record does not modify it, and a displayed state
 * may change before a player clicks an action.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazePanel extends ChatPanel {

    MazePanel(MazeEnginePlugin plugin) {
        super(plugin);
    }

    public void list(CommandSender sender) {
        list(sender, 1);
    }

    public void list(CommandSender sender, int page) {
        var records = plugin.mazes().records().stream().toList();
        var messages = plugin.messages();
        int pages = Math.max(1, (records.size() + 9) / 10);

        checkPage(page, pages);
        header(sender, "list", "count", Integer.toString(records.size()), "page", Integer.toString(page));

        if (records.isEmpty()) {
            sender.sendMessage(messages.format("empty"));
        }

        for (var record : records.subList(Math.min((page - 1) * 10, records.size()),
            Math.min(page * 10, records.size()))) {

            Component name = Component.text(record.name(), TextColor.fromHexString(record.preset().accent()));
            sender.sendMessage(messages
                .rich("maze-row", Map.of("name", name), "status",
                    Preset.capitalized(record.status().name().toLowerCase(Locale.ROOT)), "preset",
                    record.preset().displayName(), "cells", record.layout().width() + "×" + record.layout().depth())
                .hoverEvent(HoverEvent.showText(details(record)))
                .clickEvent(ClickEvent.runCommand("/maze info " + record.name())));
        }

        hint(sender, "list-footer");
        navigation(sender, "/maze list ", page, pages);
    }

    private Component details(MazeRecord record) {
        return plugin.messages().format("maze-hover", "name", record.name(), "world", record.worldName(), "origin",
            record.bounds().x() + ", " + record.bounds().y() + ", " + record.bounds().z(), "seed",
            Long.toString(record.seed()), "blocks",
            record.bounds().width() + "×" + record.bounds().height() + "×" + record.bounds().depth(), "solution",
            Integer.toString(record.layout().solution().length - 1), "deadends",
            Integer.toString(record.layout().deadEnds()), "snapshot", record.hasSnapshot() ? "Available" : "None");
    }

    public void info(CommandSender sender, MazeRecord record) {
        var messages = plugin.messages();
        header(sender, "info", "name", record.name(), "preset", record.preset().displayName());

        var preset = record.preset();
        var bounds = record.bounds();
        var teleport = record.teleportPoint();
        String point = teleport == null ? messages.values().get("teleport-entrance")
            : messages.values().get("teleport-custom") + " (" + (int) Math.floor(teleport.x()) + ", "
                + (int) Math.floor(teleport.y()) + ", " + (int) Math.floor(teleport.z()) + ")";

        Component snapshot = messages.format("snapshot-label").hoverEvent(HoverEvent.showText(messages.format("snapshot-hover")));
        Component snapshotValue = messages.format(record.hasSnapshot() ? "snapshot-available" : "snapshot-none")
            .color(TextColor.color(0xDCE3EE)).hoverEvent(HoverEvent.showText(messages.format("snapshot-hover")));
        Component teleportValue = Component.text(point, TextColor.color(0xDCE3EE)).hoverEvent(
            HoverEvent.showText(messages.format(teleport == null ? "teleport-entrance-hover" : "teleport-custom-hover")));

        sender.sendMessage(messages.rich("info-card",
            Map.of("difficulty", difficulty(preset), "theme", theme(preset), "snapshot-label", snapshot, "snapshot-value",
                snapshotValue, "teleport-value", teleportValue),
            "name", record.name(), "preset", preset.displayName(), "status",
            Preset.capitalized(record.status().name().toLowerCase(Locale.ROOT)), "cells",
            record.layout().width() + "×" + record.layout().depth(), "blocks",
            bounds.width() + "×" + bounds.height() + "×" + bounds.depth(), "world", record.worldName(), "origin",
            bounds.x() + ", " + bounds.y() + ", " + bounds.z(), "paths", Integer.toString(preset.pathWidth()), "thickness",
            Integer.toString(preset.wallThickness()), "height", Integer.toString(preset.wallHeight()), "roof",
            messages.values().get(preset.roof() ? "ceiling-covered" : "ceiling-open"), "seed", Long.toString(record.seed()),
            "solution", Integer.toString(record.layout().solution().length - 1), "deadends",
            Integer.toString(record.layout().deadEnds()), "snapshot",
            messages.values().get(record.hasSnapshot() ? "snapshot-available" : "snapshot-none"), "teleport", point));

        Component actions = Component.empty();

        for (String action : List.of("tp", "solve")) {
            if (!sender.hasPermission("mazeengine." + action) || !(sender instanceof Player)) {
                continue;
            }

            String label = action.equals("tp") ? "teleport" : action.equals("solve") ? "guide" : "delete";
            if (!actions.equals(Component.empty())) {
                actions = actions.append(Component.text("  "));
            }

            actions = actions.append(messages.button("button-" + label, "/maze " + action + " " + record.name(),
                "hover-" + label, "#91D7E3", "name", record.name()));
        }

        if (sender.hasPermission("mazeengine.regenerate")) {
            if (!actions.equals(Component.empty())) {
                actions = actions.append(Component.text("  "));
            }

            actions = actions.append(messages.button("button-repair", "/maze regenerate " + record.name() + " --repair",
                "hover-repair", "#91D7E3"));
        }

        Component copySeed = messages.format("button-copy-seed").color(TextColor.color(0x7ABFCC))
            .clickEvent(ClickEvent.copyToClipboard(Long.toString(record.seed())))
            .hoverEvent(HoverEvent.showText(messages.format("hover-copy-seed")));

        if (!actions.equals(Component.empty())) {
            actions = actions.append(Component.text("  "));
        }

        actions = actions.append(copySeed);
        if (sender.hasPermission("mazeengine.delete")) {
            actions = actions.append(Component.text("  ")).append(messages.button("button-delete",
                "/maze delete " + record.name(), "hover-delete", "#C44B5B", "name", record.name()));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(center(actions, false));
        sender.sendMessage(Component.empty());

        if (!record.error().isBlank()) {
            messages.send(sender, "error", "reason", record.error());
        }
    }
}
