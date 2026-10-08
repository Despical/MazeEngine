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

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.Set;

/**
 * Dispatches validated command actions to their specialized handlers.
 * <p>
 * The command entry point supplies the normalized action, sender, and parsed
 * options after common permission checks. This dispatcher selects creation,
 * teleport, management, visual-session, and configuration-reload behavior and
 * checks the options accepted by each action.
 * <p>
 * Maze edits go through the service layer for ownership, reservation, and
 * safety checks. Informational actions use chat panels. Private confirmation
 * and session callbacks are delegated to their owners so a stale button cannot
 * silently act on a replacement maze or visual session.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeCommandDispatcher {

    private final MazeEnginePlugin plugin;

    MazeCommandDispatcher(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    void execute(String action, CommandSender sender, CommandOptions options) throws Exception {
        switch (action) {
            case "create" -> new MazeCreationCommand(plugin).execute(sender, options, false);
            case "help" -> help(sender, options);
            case "preview" -> preview(sender, options);
            case "solve" -> solve(sender, options);
            case "setspawn" -> setspawn(sender, options);
            case "list" -> list(sender, options);
            case "presets" -> presets(sender, options);
            case "info" -> info(sender, options);
            case "build" -> build(sender, options);
            case "confirm" -> confirm(sender, options);
            case "dismiss" -> dismiss(sender, options);
            case "tp" -> tp(sender, options);
            case "delete" -> delete(sender, options);
            case "regenerate" -> regenerate(sender, options);
            case "cancel" -> cancel(sender, options);
            case "reload" -> reload(sender, options);
            default -> throw new IllegalArgumentException("Unknown command. Use /maze help.");
        }
    }

    private void help(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 0, 2);

        var ui = new ChatUI(plugin);

        if (!options.positional.isEmpty() && options.positional.getFirst().equalsIgnoreCase("concepts")) {
            ui.concepts(sender, options.positional.size() == 2 ? Integer.parseInt(options.positional.get(1)) : 1);
        } else {
            if (options.positional.size() > 1) {
                throw new IllegalArgumentException("Use /maze help [page].");
            }

            ui.help(sender, options.positional.isEmpty() ? 1 : Integer.parseInt(options.positional.getFirst()));
        }
    }

    private void preview(CommandSender sender, CommandOptions options) throws Exception {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Preview requires a player.");
        }

        if (!options.positional.isEmpty() && options.positional.getFirst().equals("stop")) {
            options.check(Set.of(), 1, 2);

            plugin.visuals().stopPreview(player, options.positional.size() == 2 ? options.positional.get(1) : null);
        } else {
            new MazeCreationCommand(plugin).execute(sender, options, true);
        }
    }

    private void solve(CommandSender sender, CommandOptions options) {
        var service = plugin.mazes();

        options.check(Set.of(), 1, 3);
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Solver requires a player.");
        }

        if (options.positional.size() >= 2 && !options.positional.get(1).equalsIgnoreCase("stop")) {
            throw new IllegalArgumentException("Use /maze solve <name> [stop].");
        }

        if (options.positional.size() >= 2) {
            plugin.visuals().stopSolver(player, options.positional.size() == 3 ? options.positional.get(2) : null, options.positional.getFirst());
        } else {
            plugin.visuals().solve(player, service.require(options.positional.getFirst()));
        }
    }

    private void setspawn(CommandSender sender, CommandOptions options) {
        var service = plugin.mazes();

        options.check(Set.of(), 1, 2);
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Setting a teleport point requires a player.");
        }

        if (options.positional.size() == 2 && !options.positional.get(1).equalsIgnoreCase("reset")) {
            throw new IllegalArgumentException("Use /maze setspawn <name> [reset].");
        }

        service.setSpawn(player, service.require(options.positional.getFirst()),
            options.positional.size() == 2 ? null : player.getLocation());
    }

    private void list(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 0, 1);
        new ChatUI(plugin).list(sender,
            options.positional.isEmpty() ? 1 : Integer.parseInt(options.positional.getFirst()));
    }

    private void presets(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 0, 1);

        new ChatUI(plugin).presets(sender, options.positional.isEmpty() ? 1 : Integer.parseInt(options.positional.getFirst()));
    }

    private void info(CommandSender sender, CommandOptions options) {
        var service = plugin.mazes();

        options.check(Set.of(), 1, 1);
        new ChatUI(plugin).info(sender, service.require(options.positional.getFirst()));
    }

    private void build(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 1, 1);

        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Building a preview requires a player.");
        }

        plugin.visuals().build(player, options.positional.getFirst());
    }

    private void confirm(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 1, 1);

        plugin.actions().confirm(sender, options.positional.getFirst());
    }

    private void dismiss(CommandSender sender, CommandOptions options) {
        options.check(Set.of(), 1, 1);

        plugin.actions().dismiss(sender, options.positional.getFirst());
    }

    private void tp(CommandSender sender, CommandOptions options) {
        new MazeTeleportCommand(plugin).execute(sender, options);
    }

    private void delete(CommandSender sender, CommandOptions options) {
        var service = plugin.mazes();

        options.check(Set.of("restore", "clear", "confirm"), 1, 1);

        if (options.flag("restore") && options.flag("clear")) {
            throw new IllegalArgumentException("Choose --restore or --clear.");
        }

        var mazeRecord = service.require(options.positional.getFirst());
        boolean restore = options.flag("restore") || (mazeRecord.hasSnapshot() && !options.flag("clear"));

        if (options.flag("confirm")) {
            service.delete(sender, mazeRecord, restore);
        } else {
            plugin.actions().delete(sender, mazeRecord, restore);
        }
    }

    private void regenerate(CommandSender sender, CommandOptions options) {
        var service = plugin.mazes();

        options.check(Set.of("seed", "new-seed", "repair"), 1, 1);

        if (options.flag("repair") && (options.has("seed") || options.flag("new-seed"))) {
            throw new IllegalArgumentException("Use --repair on its own. It cannot be combined with --seed or" + " --new-seed.");
        }

        if (options.has("seed") && options.flag("new-seed")) {
            throw new IllegalArgumentException("Choose --seed or --new-seed.");
        }

        var mazeRecord = service.require(options.positional.getFirst());
        long seed = options.has("seed") ? Long.parseLong(options.value("seed"))
            : options.flag("new-seed") ? new SecureRandom().nextLong() : mazeRecord.seed();
        service.regenerate(sender, mazeRecord, seed, options.flag("repair"));
    }

    private void cancel(CommandSender sender, CommandOptions options) {
        var service = plugin.mazes();

        options.check(Set.of(), 1, 1);
        service.cancel(sender, options.positional.getFirst());
    }

    private void reload(CommandSender sender, CommandOptions options) throws Exception {
        options.check(Set.of(), 0, 0);

        plugin.reloadSettings();
        plugin.messages().send(sender, "reloaded");
    }
}
