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

package dev.despical.mazeengine.storage;

import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.Direction;
import dev.despical.mazeengine.core.GenerationSettings;
import dev.despical.mazeengine.core.MazeLayout;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Persists one validated YAML document per saved maze.
 * <p>
 * Records contain the world identity, owner, origin, seed, frozen preset,
 * encoded graph, portals, lifecycle state, and optional teleport point. Loading
 * checks the schema, filename identity, graph connectivity and reciprocity,
 * portal placement, and overlap between records in the same world.
 * <p>
 * An invalid saved record aborts loading rather than silently forgetting an
 * occupied region. Saves use AtomicFiles replacement, and normalized names
 * exclude reserved filesystem device names. Operation-state recovery is
 * handled by MazeRecovery after repository validation succeeds.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeRepository {

    private final Path directory;

    public MazeRepository(Path data) throws IOException {
        directory = data.resolve("mazes");

        Files.createDirectories(directory);
    }

    public static String normalize(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);

        if (!normalized.matches("[a-z0-9_-]{1,48}")) {
            throw new IllegalArgumentException("Maze names must contain 1..48 letters, numbers, '_' or '-'.");
        }

        if (normalized.matches("con|prn|aux|nul|com[1-9]|lpt[1-9]")) {
            throw new IllegalArgumentException("This maze name is reserved by the filesystem.");
        }

        return normalized;
    }

    public List<MazeRecord> load() throws Exception {
        List<MazeRecord> records = new ArrayList<>();

        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".yml")).sorted().toList()) {
                var config = new YamlConfiguration();
                config.load(path.toFile());

                if (config.getInt("schema") != 1) {
                    throw new IllegalArgumentException("Unsupported maze schema: " + path);
                }

                String name = normalize(config.getString("name", ""));
                if (!path.getFileName().toString().equals(name + ".yml")) {
                    throw new IllegalArgumentException("Maze filename does not match its name: " + path);
                }

                int width = config.getInt("grid.width"), depth = config.getInt("grid.depth");
                if (width < 2 || depth < 2 || (long) width * depth > 1_000_000) {
                    throw new IllegalArgumentException("Invalid grid in " + path);
                }

                byte[] graph = Base64.getDecoder().decode(config.getString("grid.graph", ""));
                if (graph.length != width * depth) {
                    throw new IllegalArgumentException("Invalid graph length: " + path);
                }

                var entrance = portal(config, "entrance", graph.length);
                var exit = portal(config, "exit", graph.length);
                var layout = new MazeLayout(width, depth, graph, entrance, exit);
                validateGraph(layout);

                var section = config.getConfigurationSection("preset.settings");
                if (section == null) {
                    throw new IllegalArgumentException("Missing frozen preset: " + path);
                }

                var preset = Preset.read(config.getString("preset.name"), section);
                if (preset.generation().mode() == GenerationSettings.Mode.PERFECT) {
                    int edges = 0;

                    for (byte mask : graph) {
                        edges += Integer.bitCount(mask & 15);
                    }

                    if (edges != 2 * (graph.length - 1)) {
                        throw new IllegalArgumentException("Saved PERFECT maze contains a loop: " + path);
                    }
                }

                Bounds bounds = new Bounds(config.getInt("origin.x"), config.getInt("origin.y"), config.getInt("origin.z"),
                    Bounds.span(width, preset.pathWidth(), preset.wallThickness()), preset.height(),
                    Bounds.span(depth, preset.pathWidth(), preset.wallThickness())
                );
                records.add(new MazeRecord(name, UUID.fromString(config.getString("world.id")), config.getString("world.name"),
                    UUID.fromString(config.getString("owner")), bounds, config.getLong("seed"), preset, layout,
                    MazeRecord.Status.valueOf(config.getString("status")), config.getBoolean("snapshot"),
                    Instant.parse(config.getString("created")), config.getString("error", ""), teleportPoint(config))
                );
            }
        }

        for (int index = 0; index < records.size(); index++) {
            for (int otherIndex = 0; otherIndex < index; otherIndex++) {
                if (records.get(index).worldId().equals(records.get(otherIndex).worldId()) && records.get(index).bounds().overlaps(records.get(otherIndex).bounds())) {
                    throw new IllegalArgumentException("Overlapping saved mazes: " + records.get(index).name() + " / " + records.get(otherIndex).name());
                }
            }
        }

        return records;
    }

    private static MazeLayout.Portal portal(YamlConfiguration config, String key, int size) {
        int cell = config.getInt(key + ".cell");

        if (cell < 0 || cell >= size) {
            throw new IllegalArgumentException("Invalid portal cell.");
        }

        return new MazeLayout.Portal(cell, Direction.valueOf(config.getString(key + ".side")));
    }

    private static TeleportPoint teleportPoint(YamlConfiguration config) {
        if (!config.isConfigurationSection("teleport")) {
            return null;
        }

        for (String key : List.of("x", "y", "z", "yaw", "pitch")) {
            if (!(config.get("teleport." + key) instanceof Number)) {
                throw new IllegalArgumentException("Missing or invalid teleport coordinate: " + key);
            }
        }

        return new TeleportPoint(UUID.fromString(config.getString("teleport.world")), config.getDouble("teleport.x"),
            config.getDouble("teleport.y"), config.getDouble("teleport.z"), (float) config.getDouble("teleport.yaw"),
            (float) config.getDouble("teleport.pitch"));
    }

    public static void validateGraph(MazeLayout layout) {
        byte[] passages = layout.passages();

        for (int index = 0; index < layout.size(); index++) {
            if ((passages[index] & ~15) != 0) {
                throw new IllegalArgumentException("Invalid passage mask.");
            }

            for (Direction direction : Direction.values()) {
                if (layout.open(index, direction)) {
                    int next = layout.neighbor(index, direction);

                    if (next < 0 || !layout.open(next, direction.opposite())) {
                        throw new IllegalArgumentException("Asymmetric graph.");
                    }
                }
            }
        }

        if (Arrays.stream(layout.distances(0)).anyMatch(distance -> distance < 0)) {
            throw new IllegalArgumentException("Disconnected graph.");
        }

        for (var portal : List.of(layout.entrance(), layout.exit())) {
            if (layout.neighbor(portal.cell(), portal.side()) >= 0) {
                throw new IllegalArgumentException("Portal is not on an outer boundary.");
            }
        }
    }

    public void save(MazeRecord record) throws Exception {
        var config = new YamlConfiguration();
        config.set("schema", 1);
        config.set("name", record.name());
        config.set("world.id", record.worldId().toString());
        config.set("world.name", record.worldName());
        config.set("owner", record.owner().toString());
        config.set("origin.x", record.bounds().x());
        config.set("origin.y", record.bounds().y());
        config.set("origin.z", record.bounds().z());
        config.set("seed", record.seed());
        config.set("preset.name", record.preset().name());
        config.createSection("preset.settings", record.preset().serialize());
        config.set("grid.width", record.layout().width());
        config.set("grid.depth", record.layout().depth());
        config.set("grid.graph", Base64.getEncoder().encodeToString(record.layout().passages()));
        config.set("entrance.cell", record.layout().entrance().cell());
        config.set("entrance.side", record.layout().entrance().side().name());
        config.set("exit.cell", record.layout().exit().cell());
        config.set("exit.side", record.layout().exit().side().name());
        config.set("status", record.status().name());
        config.set("snapshot", record.hasSnapshot());
        config.set("created", record.created().toString());
        config.set("error", record.error());

        if (record.teleportPoint() != null) {
            var teleportPoint = record.teleportPoint();
            config.set("teleport.world", teleportPoint.worldId().toString());
            config.set("teleport.x", teleportPoint.x());
            config.set("teleport.y", teleportPoint.y());
            config.set("teleport.z", teleportPoint.z());
            config.set("teleport.yaw", teleportPoint.yaw());
            config.set("teleport.pitch", teleportPoint.pitch());
        }

        AtomicFiles.write(directory.resolve(normalize(record.name()) + ".yml"), config.saveToString());
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(directory.resolve(normalize(name) + ".yml"));
    }
}
