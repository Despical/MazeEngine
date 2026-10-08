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

package dev.despical.mazeengine.integration;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.api.model.MazeSnapshot;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes maze metadata through the optional PlaceholderAPI expansion.
 * <p>
 * A server-thread task captures global counts, named-maze fields, current-player
 * maze fields, and ownership counts into copied maps. Request handlers read one
 * volatile cache value rather than querying live Bukkit worlds or maze services
 * from a potentially asynchronous placeholder invocation.
 * <p>
 * The expansion supports the mazeengine identifier and persists across
 * PlaceholderAPI reloads. Closing it cancels the refresh task, unregisters the
 * expansion, and clears cached values. Recognized fields with no matching maze
 * return empty text, while unknown placeholders return null.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazePlaceholders extends PlaceholderExpansion implements AutoCloseable {

    private BukkitTask task;
    private boolean registered;
    private final MazeEnginePlugin plugin;

    /**
     * Captures the copied maps served by one placeholder refresh.
     * <p>
     * Global, named, current-player, and ownership values are published together
     * through one volatile reference. A request uses one captured Cache instance
     * so it cannot combine maps from different refreshes.
     */
    private record Cache(Map<String, String> globals, Map<String, String> named, Map<UUID, Map<String, String>> current, Map<UUID, Integer> owned) {
    }

    private volatile Cache cache = new Cache(Map.of(), Map.of(), Map.of(), Map.of());
    private static final Set<String> FIELDS = Set.of("name", "theme", "difficulty", "seed", "status", "cells_width",
        "cells_depth", "path_width", "wall_thickness", "wall_height", "roof", "route_length", "dead_ends", "progress",
        "snapshot"
    );

    public MazePlaceholders(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!register()) {
            throw new IllegalStateException("The mazeengine placeholder identifier is already registered.");
        }

        registered = true;

        refresh();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refresh, 10, 10);
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "mazeengine";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "Despical";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    private Map<String, String> values(MazeSnapshot maze) {
        var preset = maze.preset();
        var geometry = preset.geometry();

        Map<String, String> values = new HashMap<>();
        values.put("name", maze.id().value());
        values.put("theme", preset.displayName());
        values.put("difficulty", label(preset.difficulty().name()));
        values.put("seed", Long.toString(maze.seed()));
        values.put("status", label(maze.status().name()));
        values.put("cells_width", Integer.toString(maze.cells().width()));
        values.put("cells_depth", Integer.toString(maze.cells().depth()));
        values.put("path_width", Integer.toString(geometry.pathWidth()));
        values.put("wall_thickness", Integer.toString(geometry.wallThickness()));
        values.put("wall_height", Integer.toString(geometry.wallHeight()));
        values.put("roof", Boolean.toString(preset.roof()));
        values.put("snapshot", Boolean.toString(maze.hasSnapshot()));
        values.put("route_length", Integer.toString(maze.routeLength()));
        values.put("dead_ends", Integer.toString(maze.deadEnds()));
        values.put("progress",
            Integer.toString(plugin.mazes().busy(maze.id().value()) ? plugin.mazes().progressPercent(maze.id().value())
                : maze.status() == MazeSnapshot.Status.READY ? 100 : 0));
        return Map.copyOf(values);
    }

    private static String label(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private void refresh() {
        var mazes = plugin.api().mazes().all();

        Map<String, String> named = new HashMap<>();
        Map<String, Map<String, String>> byName = new HashMap<>();
        Map<UUID, Integer> owned = new HashMap<>();
        Map<UUID, Map<String, String>> current = new HashMap<>();

        for (var maze : mazes) {
            var values = values(maze);

            byName.put(maze.id().value(), values);
            values.forEach((field, value) -> named.put("maze_" + maze.id().value() + "_" + field, value));
            owned.merge(maze.owner(), 1, Integer::sum);
        }

        for (var player : plugin.getServer().getOnlinePlayers()) {
            var record = plugin.mazes().at(player.getLocation());

            if (record != null) {
                current.put(player.getUniqueId(), byName.get(record.name()));
            }
        }

        cache = new Cache(
            Map.of("count", Integer.toString(mazes.size()), "preset_count",
                Integer.toString(plugin.settings().presets().size()), "active_operations",
                Integer.toString(plugin.mazes().activeOperationCount())),
            Map.copyOf(named), Map.copyOf(current), Map.copyOf(owned));
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        Cache snapshot = cache;

        if (snapshot.globals().containsKey(key)) {
            return snapshot.globals().get(key);
        }

        if (key.equals("owned_count")) {
            return Integer.toString(player == null ? 0 : snapshot.owned().getOrDefault(player.getUniqueId(), 0));
        }

        if (key.startsWith("current_")) {
            String field = key.substring(8);

            if (!FIELDS.contains(field)) {
                return null;
            }

            return player == null ? "" : snapshot.current().getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(field, "");
        }

        if (key.startsWith("maze_")) {
            if (snapshot.named().containsKey(key)) {
                return snapshot.named().get(key);
            }

            return FIELDS.stream().anyMatch(field -> key.endsWith("_" + field)) ? "" : null;
        }

        return null;
    }

    @Override
    public void close() {
        if (task != null) {
            task.cancel();
        }

        if (registered) {
            unregister();

            registered = false;
        }

        cache = new Cache(Map.of(), Map.of(), Map.of(), Map.of());
    }
}
