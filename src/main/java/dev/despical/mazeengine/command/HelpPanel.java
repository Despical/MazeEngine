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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders command help and explanations of maze-generation concepts.
 * <p>
 * Command groups describe syntax, suggestions, permissions, and player-only
 * requirements. The renderer filters the available entries for the sender and
 * paginates the remaining content using the shared chat layout.
 * <p>
 * Concept pages explain settings separately from command syntax and draw their
 * text from configured message templates. Help buttons suggest or navigate to
 * commands rather than initiating world changes, so opening help does not
 * reserve a maze name or create an operation.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class HelpPanel extends ChatPanel {

    HelpPanel(MazeEnginePlugin plugin) {
        super(plugin);
    }

    /**
     * Describes one command entry before sender-specific help filtering.
     * <p>
     * The identifier selects configured text, while permission and player flags
     * decide whether the row is available. Syntax and suggestion supply the visible
     * usage and the command text inserted by the help control.
     */
    private record HelpCommand(String id, String permission, boolean player, String syntax, String suggestion) {
    }

    /**
     * Groups related command entries under one help-section heading.
     * <p>
     * Grouping separates presentation order from the command handlers themselves.
     * The renderer can filter unavailable rows and paginate the visible groups
     * without changing the syntax or permission metadata of an entry.
     */
    private record HelpGroup(String name, List<HelpCommand> commands) {
    }

    private static HelpCommand command(String id, String permission, boolean player, String syntax, String suggestion) {
        return new HelpCommand(id, permission, player, "/maze " + syntax, "/maze " + suggestion);
    }

    private static final List<HelpGroup> HELP = List.of(
        new HelpGroup("help-group-build",
            List.of(command("create", "create", false, "create <name> [width depth]", "create "),
                command("preview", "preview", true, "preview <name> [width depth]", "preview "),
                command("preview-stop", "preview", true, "preview stop", "preview stop"),
                command("presets", "presets", false, "presets [page]", "presets"))),
        new HelpGroup("help-group-explore",
            List.of(command("list", "list", false, "list [page]", "list"),
                command("info", "info", false, "info <name>", "info "), command("tp", "tp", true, "tp <name>", "tp "),
                command("solve", "solve", true, "solve <name>", "solve "),
                command("solve-stop", "solve", true, "solve <name> stop", "solve <name> stop"))),
        new HelpGroup("help-group-manage",
            List.of(command("setspawn", "setspawn", true, "setspawn <name>", "setspawn "),
                command("setspawn-reset", "setspawn", true, "setspawn <name> reset", "setspawn <name> reset"),
                command("regenerate", "regenerate", false, "regenerate <name>", "regenerate "),
                command("delete", "delete", false, "delete <name>", "delete "),
                command("cancel", "cancel", false, "cancel <name>", "cancel "),
                command("reload", "reload", false, "reload", "reload"))),
        new HelpGroup("help-group-reference", List.of(command("help", "use", false, "help [page]", "help"))));

    public void help(CommandSender sender, int page) {
        List<List<HelpGroup>> pages = new ArrayList<>();
        List<HelpGroup> current = new ArrayList<>();

        int count = 0;

        for (HelpGroup group : HELP) {
            var visible = group.commands.stream()
                .filter(helpCommand -> sender.hasPermission("mazeengine." + helpCommand.permission) && (!helpCommand.player || sender instanceof Player))
                .toList();

            if (visible.isEmpty()) {
                continue;
            }

            if (count + visible.size() > 9) {
                pages.add(current);
                current = new ArrayList<>();
                count = 0;
            }

            current.add(new HelpGroup(group.name, visible));
            count += visible.size();
        }

        if (!current.isEmpty()) {
            pages.add(current);
        }

        if (pages.isEmpty()) {
            pages.add(List.of());
        }

        checkPage(page, pages.size());

        var messages = plugin.messages();
        header(sender, "help", "page", Integer.toString(page));

        boolean first = true;

        for (HelpGroup group : pages.get(page - 1)) {
            if (!first) {
                sender.sendMessage(Component.empty());
            }

            first = false;

            sender.sendMessage(messages.format("help-section", "section", messages.values().get(group.name)));

            for (HelpCommand entry : group.commands) {
                Component command = Component.text(entry.syntax, TextColor.color(0x91D7E3));
                sender.sendMessage(messages
                    .rich("help-row", Map.of("command", command), "description", messages.values().get("help-desc-" + entry.id))
                    .clickEvent(ClickEvent.suggestCommand(entry.suggestion))
                    .hoverEvent(HoverEvent.showText(messages.format("help-command-hover", "command", entry.syntax, "description", wrap(messages.values().get("help-detail-" + entry.id), 38)))));
            }
        }

        hint(sender, "help-footer");
        navigation(sender, "/maze help ", page, pages.size());
    }

    /**
     * Describes an explanatory concept and its command example.
     * <p>
     * The identifier selects the corresponding message templates. The example
     * provides concrete usage beside that explanation, keeping concept pages
     * independent from the permission-filtered command groups.
     */
    private record Concept(String id, String example) {
    }

    private static final List<Concept> CONCEPTS = List.of(new Concept("seed", "/maze preview example --seed 42"),
        new Concept("complexity", "/maze preview example --complexity 0.8"),
        new Concept("cells", "/maze preview example 15 15"),
        new Concept("geometry", "/maze preview example --wall-height 6"),
        new Concept("preset", "/maze preview garden --preset Hedge"),
        new Concept("connections", "/maze preview dunes --preset Desert"),
        new Concept("placement", "/maze create example --mode SAFE"),
        new Concept("snapshot", "/maze create example --snapshot"),
        new Concept("selection", "/maze preview example --selection"),
        new Concept("origin", "/maze preview example --world world --x 100 --y 80 --z 100"));

    public void concepts(CommandSender sender, int page) {
        int pages = (CONCEPTS.size() + 1) / 2;
        checkPage(page, pages);

        var messages = plugin.messages();
        header(sender, "concepts", "page", Integer.toString(page));

        for (int index = (page - 1) * 2; index < Math.min(CONCEPTS.size(), page * 2); index++) {
            Concept concept = CONCEPTS.get(index);

            if (index % 2 != 0) {
                sender.sendMessage(Component.empty());
                separator(sender);
                sender.sendMessage(Component.empty());
            }

            Component example = Component.text(concept.example, TextColor.color(0x91D7E3))
                .clickEvent(ClickEvent.suggestCommand(concept.example))
                .hoverEvent(HoverEvent.showText(messages.format("hover-example")));
            sender.sendMessage(messages.rich("concept-card", Map.of("example", example), "term",
                messages.values().get("concept-name-" + concept.id), "description",
                "      " + wrap(messages.values().get("concept-text-" + concept.id), 40).replace("\n", "\n      ")));
        }

        navigation(sender, "/maze help concepts ", page, pages);
    }
}
