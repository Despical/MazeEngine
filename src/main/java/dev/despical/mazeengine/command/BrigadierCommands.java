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

import co.aikar.commands.BukkitCommandManager;
import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.storage.MazeRecord;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.function.Supplier;

/**
 * Builds the native command tree sent to Minecraft clients.
 * <p>
 * The tree describes subcommands, typed arguments, option redirects, and current
 * maze, preset, and world suggestions. Execution delegates to the ACF command
 * instance so the same annotated handlers and permission checks process typed
 * commands and private chat callbacks.
 * <p>
 * The per-player command-tree listener hides callback-only branches and tokens
 * from client suggestions without disabling their server execution. An identity
 * set prevents revisiting redirected nodes. Closing the adapter unregisters its
 * ACF commands as part of plugin shutdown.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class BrigadierCommands implements Listener, AutoCloseable {

    private final MazeEnginePlugin plugin;
    private final BukkitCommandManager manager;
    private final Command executor;

    public BrigadierCommands(MazeEnginePlugin plugin) {
        this.plugin = plugin;
        this.manager = new BukkitCommandManager(plugin);
        this.manager.registerCommand(new MazeCommands(plugin));
        this.executor = Objects.requireNonNull(Bukkit.getCommandMap().getCommand("__mazeengine"));
        this.manager.unregisterCommands();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> event.registrar().register(tree().build(), "Generate and manage mazes", List.of("mazeengine")));
    }

    /**
     * Filter only the per-player client tree; the server still executes private chat callbacks
     * normally.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void visibleCommands(AsyncPlayerSendCommandsEvent<?> event) {
        if (!event.isAsynchronous() && event.hasFiredAsync()) {
            return;
        }

        Set<CommandNode<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> hidden = Set.of("maze", "mazeengine", "mazeengine:maze", "mazeengine:mazeengine");

        for (var node : event.getCommandNode().getChildren()) {
            if (hidden.contains(node.getName())) {
                hideCallbacks(node, seen);
            }
        }
    }

    private void hideCallbacks(CommandNode<?> node, Set<CommandNode<?>> seen) {
        if (!seen.add(node)) {
            return;
        }

        Set<String> hidden = Set.of("build", "confirm", "dismiss", "token", "session", "--confirm");
        node.getChildren().removeIf(child -> hidden.contains(child.getName()));

        for (var child : node.getChildren()) {
            hideCallbacks(child, seen);
        }

        if (node.getRedirect() != null) {
            hideCallbacks(node.getRedirect(), seen);
        }
    }

    private int run(CommandContext<CommandSourceStack> context) {
        String input = context.getInput();
        int separator = input.indexOf(' ');

        String[] args = separator < 0 ? new String[0] : input.substring(separator + 1).trim().split("\\s+");
        executor.execute(context.getSource().getSender(), "__mazeengine", args);
        return 1;
    }

    private LiteralArgumentBuilder<CommandSourceStack> action(String name, boolean player) {
        return Commands.literal(name).requires(
            source -> source.getSender().hasPermission("mazeengine." + name) && (!player || source.getSender() instanceof Player));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> names() {
        return suggested("name", () -> plugin.mazes().records().stream().map(MazeRecord::name).toList());
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> suggested(String name, Supplier<List<String>> values) {
        return Commands.argument(name, StringArgumentType.word()).suggests((context, builder) -> {
            values.get().stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    public LiteralArgumentBuilder<CommandSourceStack> tree() {
        var root = Commands.literal("maze").requires(source -> source.getSender().hasPermission("mazeengine.use"))
            .executes(this::run);
        root.then(Commands.literal("help").executes(this::run)
            .then(Commands.argument("page", IntegerArgumentType.integer(1, 100)).executes(this::run))
            .then(Commands.literal("concepts").executes(this::run)
            .then(Commands.argument("page", IntegerArgumentType.integer(1, 100)).executes(this::run))));

        for (String action : List.of("confirm", "dismiss")) {
            root.then(Commands.literal(action).then(Commands.argument("token", StringArgumentType.word()).executes(this::run)));
        }

        root.then(Commands.literal("build")
            .requires(source -> source.getSender() instanceof Player && source.getSender().hasPermission("mazeengine.create"))
            .then(Commands.argument("token", StringArgumentType.word()).executes(this::run)));
        root.then(action("reload", false).executes(this::run));

        for (String action : List.of("list", "presets")) {
            root.then(action(action, false).executes(this::run)
                .then(Commands.argument("page", IntegerArgumentType.integer(1, 100000)).executes(this::run)));
        }

        for (String action : List.of("info", "tp", "cancel", "solve", "setspawn", "delete", "regenerate")) {
            var name = names().executes(this::run).build();
            if (action.equals("solve")) {
                name.addChild(Commands.literal("stop").executes(this::run)
                    .then(Commands.argument("session", StringArgumentType.word()).executes(this::run)).build());
            }

            if (action.equals("setspawn")) {
                name.addChild(Commands.literal("reset").executes(this::run).build());
            }

            if (action.equals("delete")) {
                flags(name, List.of("restore", "clear"), Map.of());
                name.addChild(Commands.literal("--confirm").executes(this::run).redirect(name).build());
            }

            if (action.equals("regenerate")) {
                flags(name, List.of("new-seed", "repair"), Map.of("seed", LongArgumentType.longArg()));
            }

            var branch = action(action, List.of("tp", "solve", "setspawn").contains(action)).build();
            branch.addChild(name);

            root.then(branch);
        }
        for (String action : List.of("create", "preview")) {
            var name = Commands.argument("name", StringArgumentType.word()).executes(this::run).build();
            var depth = Commands.argument("depth", IntegerArgumentType.integer(2, 500_000)).executes(this::run).build();
            var width = Commands.argument("width", IntegerArgumentType.integer(2, 500_000)).build();

            width.addChild(depth);
            name.addChild(width);

            Map<String, ArgumentType<?>> values = new LinkedHashMap<>();
            values.put("preset", StringArgumentType.word());
            values.put("seed", LongArgumentType.longArg());
            values.put("complexity", DoubleArgumentType.doubleArg(0, 1));
            values.put("mode", StringArgumentType.word());
            values.put("path-width", IntegerArgumentType.integer(1, 16));
            values.put("wall-thickness", IntegerArgumentType.integer(1, 8));
            values.put("wall-height", IntegerArgumentType.integer(2, 64));
            values.put("world", StringArgumentType.word());

            for (String key : List.of("x", "y", "z")) {
                values.put(key, IntegerArgumentType.integer());
            }

            flags(name, List.of("selection", "snapshot"), values);
            flags(depth, List.of("selection", "snapshot"), values);

            var branch = action(action, action.equals("preview")).build();
            branch.addChild(name);

            if (action.equals("preview")) {
                branch.addChild(Commands.literal("stop").executes(this::run)
                    .then(Commands.argument("session", StringArgumentType.word()).executes(this::run)).build());
            }

            root.then(branch);
        }

        return root;
    }

    private void flags(CommandNode<CommandSourceStack> hub, List<String> flags, Map<String, ArgumentType<?>> values) {
        for (String flag : flags) {
            hub.addChild(Commands.literal("--" + flag).executes(this::run).redirect(hub).build());
        }

        values.forEach((key, type) -> {
            var value = Commands.argument(key, type).executes(this::run).redirect(hub);

            if (key.equals("preset") || key.equals("mode") || key.equals("world")) {
                value.suggests((context, builder) -> {
                    List<String> choices = switch (key) {
                        case "preset" -> plugin.settings().presets().keySet().stream()
                            .map(dev.despical.mazeengine.config.Preset::capitalized).toList();
                        case "mode" -> List.of("CLEAR", "SAFE", "REPLACE");
                        default -> Bukkit.getWorlds().stream().map(World::getName).toList();
                    };

                    choices.stream().filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())).forEach(builder::suggest);
                    return builder.buildFuture();
                });
            }

            hub.addChild(Commands.literal("--" + key).then(value).build());
        });
    }

    @Override
    public void close() {
        manager.unregisterCommands();
    }
}
