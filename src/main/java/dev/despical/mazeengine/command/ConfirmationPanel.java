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
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Renders review panels for preview builds and terrain removal.
 * <p>
 * The panels show the selected maze or preview together with the action the
 * player is about to perform. Build, confirm, dismiss, and close controls reuse
 * the common theme, hover descriptions, and owner-bound callback buttons.
 * <p>
 * Rendering a confirmation does not change terrain. The callback returns to
 * the command layer, where preview or pending-action state is checked again
 * before submission. Expired tokens and changed records are handled by the
 * services that own those actions.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class ConfirmationPanel extends ChatPanel {

    ConfirmationPanel(MazeEnginePlugin plugin) {
        super(plugin);
    }

    public void preview(CommandSender sender, String name, Preset preset, int width, int depth, Bounds bounds, Component actions) {
        var messages = plugin.messages();
        header(sender, "preview", "name", name, "preset", preset.displayName());

        sender.sendMessage(messages.rich("preview-card", Map.of("difficulty", difficulty(preset), "theme", theme(preset)),
            "preset", preset.displayName(), "cells", width + "×" + depth, "blocks",
            bounds.width() + "×" + bounds.height() + "×" + bounds.depth(), "paths",
            Integer.toString(preset.pathWidth()), "thickness", Integer.toString(preset.wallThickness()), "height",
            Integer.toString(preset.wallHeight()), "roof",
            messages.values().get(preset.roof() ? "preview-roof-covered" : "ceiling-open"))
        );

        if (preset.roof()) {
            sender.sendMessage(messages.format("preview-roof-note"));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(center(actions, false));
        hint(sender, "preview-footer");
        sender.sendMessage(Component.empty());
    }

    public void delete(CommandSender sender, MazeRecord record, boolean restore, Component actions) {
        var messages = plugin.messages();
        header(sender, "delete", "name", record.name());

        String method = restore ? "delete-restore" : record.hasSnapshot() ? "delete-clear-snapshot" : "delete-clear";
        sender.sendMessage(messages.rich("delete-card",
            Map.of("theme", theme(record.preset()), "difficulty", difficulty(record.preset())), "name", record.name(),
            "cells", record.layout().width() + "×" + record.layout().depth(), "world", record.worldName(), "blocks",
            record.bounds().width() + "×" + record.bounds().height() + "×" + record.bounds().depth())
        );
        sender.sendMessage(Component.empty());

        for (String line : wrap(messages.values().get(method), 44).split("\n")) {
            sender.sendMessage(center(messages.format("delete-method", "text", line), false));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(center(actions, false));
        sender.sendMessage(Component.empty());
    }
}
