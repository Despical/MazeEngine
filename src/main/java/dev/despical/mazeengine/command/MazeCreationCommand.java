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

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves creation and preview arguments into a complete maze specification.
 * <p>
 * The handler selects a configured preset, applies explicit geometry and
 * generation overrides, and resolves the target world and floor origin. A
 * WorldEdit cuboid selection instead determines the largest fitting centered
 * cell grid and supplies its origin automatically.
 * <p>
 * Arguments must provide both cell dimensions and a complete coordinate set
 * when required. Once resolved, the same specification goes to the visual
 * preview service or the maze creation service. A plain create request can
 * also confirm the player's matching active preview.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeCreationCommand {

    private final MazeEnginePlugin plugin;

    MazeCreationCommand(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    void execute(CommandSender sender, CommandOptions options, boolean preview) throws Exception {
        options.check(Set.of("preset", "seed", "selection", "complexity", "snapshot", "mode", "world", "x", "y", "z",
            "path-width", "wall-thickness", "wall-height"), 1, 3);

        if (!preview && sender instanceof Player player && plugin.visuals().confirmPreview(player,
            options.positional.getFirst(), options.positional.size() == 1 && options.values.isEmpty())) {
            return;
        }

        if (options.positional.size() == 2) {
            throw new IllegalArgumentException("Supply both width and depth, or neither.");
        }

        var base = plugin.settings().presets().get(options.get("preset", "default").toLowerCase(Locale.ROOT));
        if (base == null) {
            plugin.messages().send(sender, "unknown-preset", "name", options.value("preset"));
            return;
        }

        var map = new LinkedHashMap<>(base.serialize());
        for (String key : List.of("path-width", "wall-thickness", "wall-height")) {
            if (options.has(key)) {
                map.put(key, Integer.parseInt(options.value(key)));
            }
        }

        if (options.has("complexity")) {
            map.put("complexity", Double.parseDouble(options.value("complexity")));
            map.remove("advanced");
        }

        if (options.flag("snapshot")) {
            map.put("snapshot", true);
        }

        if (options.has("mode")) {
            map.put("placement", options.value("mode"));
        }

        Preset preset = Preset.fromMap(base.name(), map);
        World world;
        int x, y, z;

        int width = options.positional.size() == 3 ? Integer.parseInt(options.positional.get(1)) : plugin.settings().defaultCells();
        int depth = options.positional.size() == 3 ? Integer.parseInt(options.positional.get(2)) : plugin.settings().defaultCells();

        if (options.flag("selection")) {
            if (!(sender instanceof Player player)) {
                throw new IllegalArgumentException("WorldEdit selection requires a player.");
            }

            if (options.positional.size() != 1 || options.has("world") || options.has("x") || options.has("y") || options.has("z")) {
                throw new IllegalArgumentException("Selection computes dimensions and origin automatically.");
            }

            var selection = plugin.mazes().selection(player);
            var bounds = selection.bounds();

            world = selection.world();
            width = Bounds.fit(bounds.width(), preset.pathWidth(), preset.wallThickness());
            depth = Bounds.fit(bounds.depth(), preset.pathWidth(), preset.wallThickness());

            if (bounds.height() < preset.height()) {
                throw new IllegalArgumentException("Selection is too short for this preset.");
            }

            if (width < 2 || depth < 2) {
                throw new IllegalArgumentException("Selection cannot fit a 2x2 maze.");
            }

            x = bounds.x() + (bounds.width() - Bounds.span(width, preset.pathWidth(), preset.wallThickness())) / 2;
            z = bounds.z() + (bounds.depth() - Bounds.span(depth, preset.pathWidth(), preset.wallThickness())) / 2;
            y = bounds.y();
        } else {
            Player player = sender instanceof Player senderPlayer ? senderPlayer : null;
            world = options.has("world") ? Bukkit.getWorld(options.value("world")) : player == null ? null : player.getWorld();

            if (world == null) {
                throw new IllegalArgumentException("Supply a loaded --world.");
            }

            boolean coordinates = options.has("x") && options.has("y") && options.has("z");
            if ((options.has("x") || options.has("y") || options.has("z")) && !coordinates) {
                throw new IllegalArgumentException("Supply --x, --y and --z together.");
            }

            if (player == null && !coordinates) {
                throw new IllegalArgumentException("Console requires --world, --x, --y and --z.");
            }

            if (options.has("world") && player != null && !world.equals(player.getWorld()) && !coordinates) {
                throw new IllegalArgumentException("A different world requires explicit coordinates.");
            }

            x = coordinates ? Integer.parseInt(options.value("x")) : player.getLocation().getBlockX() + 3;
            y = coordinates ? Integer.parseInt(options.value("y")) : player.getLocation().getBlockY() - 1;
            z = coordinates ? Integer.parseInt(options.value("z")) : player.getLocation().getBlockZ() + 3;
        }

        long seed = options.has("seed") ? Long.parseLong(options.value("seed")) : new SecureRandom().nextLong();

        if (preview) {
            plugin.visuals().preview((Player) sender, options.positional.getFirst(), world, x, y, z, width, depth, seed, preset);
        } else {
            plugin.mazes().create(sender, options.positional.getFirst(), world, x, y, z, width, depth, seed, preset);
        }
    }
}
