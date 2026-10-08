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

package dev.despical.mazeengine.config;

import dev.despical.mazeengine.command.ChatUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Selects contextual chat actions independently of message-template rendering.
 * <p>
 * Operation outcomes, errors, preview status, and guide status each receive
 * appropriate follow-up buttons such as details, cancel, teleport, or help.
 * The renderer checks permissions and player-only requirements before adding
 * an action and can combine caller-supplied controls with standard ones.
 * <p>
 * Configured title and body templates provide the text, while shared ChatUi
 * helpers provide centering and wrapping. This class sends a presentation of
 * the outcome; clicking its actions still goes through normal command checks.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MessagePanels {

    private final Messages messages;

    MessagePanels(Messages messages) {
        this.messages = messages;
    }

    private static String argument(String key, String... args) {
        for (int index = 0; index < args.length; index += 2) {
            if (args[index].equals(key)) {
                return args[index + 1];
            }
        }

        return null;
    }

    void send(CommandSender sender, String key, Map<String, Component> components, String... args) {
        var buttons = new ArrayList<Component>();
        Component supplied = components.getOrDefault("actions", components.getOrDefault("action", Component.empty()));

        if (!supplied.equals(Component.empty())) {
            buttons.add(supplied);
        }

        String name = argument("name", args);
        if (name != null && Set.of("queued", "creation-queued", "regenerate-queued", "repair-queued").contains(key)) {
            action(buttons, sender, "cancel", "button-cancel", "/maze cancel " + name, "hover-cancel", "#D5B877", false,
                false, name);
        }

        if (name != null && Set.of("completed", "regenerated", "repaired", "spawn-saved", "spawn-reset", "spawn-already-default").contains(key)) {
            action(buttons, sender, "tp", "button-teleport", "/maze tp " + name, "hover-teleport", "#91D7E3", true,
                false, name);
        }

        if (name != null && Set.of("completed", "regenerated", "repaired", "teleported", "spawn-saved", "spawn-reset",
            "spawn-already-default", "action-dismissed").contains(key)) {
            action(buttons, sender, "info", "button-details", "/maze info " + name, "hover-details", "#7ABFCC", false,
                false, name);
        }

        if (name != null && key.equals("teleported")) {
            action(buttons, sender, "solve", "button-guide", "/maze solve " + name, "hover-guide", "#8BD5CA", true,
                false, name);
        }

        if (Set.of("unknown-maze", "maze-exists", "deleted", "cancelled", "confirm-expired", "action-already-dismissed",
            "undo-unavailable").contains(key) || key.equals("action-dismissed") && name == null) {
            action(buttons, sender, "list", "button-available-mazes", "/maze list", "hover-available-mazes", "#7ABFCC",
                false, false, name);
        }

        if (Set.of("unknown-preset", "reloaded", "empty").contains(key)) {
            action(buttons, sender, "presets", "button-presets", "/maze presets", "hover-presets", "#7ABFCC", false,
                false, name);
        }

        if (key.equals("empty")) {
            action(buttons, sender, "preview", "button-new-preview", "/maze preview ", "hover-new-preview", "#7ABFCC",
                true, true, name);
        }

        if (key.equals("preview-stopped") && supplied.equals(Component.empty())) {
            action(buttons, sender, "presets", "button-view-presets", "/maze presets", "hover-presets", "#7ABFCC",
                false, false, name);
        }

        if (Set.of("preview-ended", "preview-already-closed").contains(key) && supplied.equals(Component.empty())) {
            action(buttons, sender, "preview", "button-new-preview", "/maze preview ", "hover-new-preview", "#7ABFCC",
                true, true, name);
        }

        if (Set.of("guide-stopped", "guide-ended", "guide-already-closed").contains(key)) {
            if (name != null) {
                action(buttons, sender, "solve", "button-guide-open", "/maze solve " + name, "hover-guide-open",
                    "#8BD5CA", true, false, name);
            } else {
                action(buttons, sender, "list", "button-available-mazes", "/maze list", "hover-available-mazes",
                    "#7ABFCC", false, false, name);
            }
        }

        if (key.equals("reloaded") || key.equals("error")
            && !Objects.toString(argument("reason", args), "").startsWith("Missing permission:")) {
            action(buttons, sender, "help", "button-help", "/maze help", "hover-help", "#7ABFCC", false, false, name);
        }

        Component trailing = components.getOrDefault("trailing-actions", Component.empty());
        if (!trailing.equals(Component.empty())) {
            buttons.add(trailing);
        }

        sender.sendMessage(ChatUI.center(messages.format("panel-separator"), false));
        sender.sendMessage(Component.empty());
        sender.sendMessage(ChatUI.center(messages.format(key + "-title", args), true));

        if (key.equals("error")) {
            for (String line : ChatUI.wrap(Objects.toString(argument("reason", args), "Please try again."), 48)
                .split("\n")) {
                sender.sendMessage(ChatUI.center(messages.format("error-body", "reason", line), false));
            }
        } else {
            sender.sendMessage(ChatUI.center(messages.rich(key + "-body", components, args), false));
        }

        if (!buttons.isEmpty()) {
            Component actions = Component.empty();

            for (int index = 0; index < buttons.size(); index++) {
                if (index > 0) {
                    actions = actions.append(Component.text("  "));
                }

                actions = actions.append(buttons.get(index));
            }

            sender.sendMessage(Component.empty());
            sender.sendMessage(ChatUI.center(actions, false));
        }

        sender.sendMessage(Component.empty());
    }

    private void action(List<Component> buttons, CommandSender sender, String permission, String label, String command,
        String hover, String color, boolean playerOnly, boolean suggest, String name) {

        if (!sender.hasPermission("mazeengine.use") || !sender.hasPermission("mazeengine." + permission)
            || playerOnly && !(sender instanceof Player)) {
            return;
        }

        Component action = messages.button(label, command, hover, color, "name", name == null ? "" : name);
        if (suggest) {
            action = action.clickEvent(ClickEvent.suggestCommand(command));
        }

        buttons.add(action);
    }
}
