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

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains the validated configuration published for the running plugin.
 * <p>
 * The value groups tick budgets, concurrent-operation limits, grid and volume
 * limits, terrain protection, replaceable materials, presets, messages, and
 * visual and boss-bar settings. Loading validates all participating files
 * before constructing the replacement value.
 * <p>
 * The plugin publishes the replacement only after loading succeeds, leaving
 * the previous settings available if a reload fails. Loaded maps and sets are
 * copied or exposed read-only. Existing maze records retain their saved presets
 * rather than inheriting subsequent edits to this current configuration.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record Settings(
    int blocksPerTick,
    double millisPerTick,
    int maxConcurrent,
    int maxCells,
    long maxVolume,
    int maxChunks,
    int defaultCells,
    boolean protection,
    Set<String> replaceable,
    Map<String, Preset> presets,
    Messages messages,
    boolean preventMobSpawns,
    VisualSettings visuals,
    ProgressSettings progress
) {

    public static Settings load(Path directory) throws Exception {
        var config = new YamlConfiguration();
        config.load(directory.resolve("config.yml").toFile());

        Set<String> allowed = new HashSet<>(Set.of("performance.blocks-per-tick", "performance.millis-per-tick",
            "performance.max-concurrent", "limits.max-cells", "limits.max-volume", "limits.max-chunks",
            "defaults.cells", "protection.enabled", "placement.replaceable", "protection.prevent-mob-spawns"));
        allowed.addAll(VisualSettings.KEYS);
        allowed.addAll(ProgressSettings.KEYS);
        Validation.keys(config, allowed);

        for (String key : List.of("performance.blocks-per-tick", "performance.max-concurrent", "limits.max-cells", "limits.max-chunks", "defaults.cells")) {
            Validation.number(config, key, true);
        }

        Validation.number(config, "performance.millis-per-tick", false);
        Validation.number(config, "limits.max-volume", true);
        Validation.type(config, "protection.enabled", Boolean.class);
        Validation.type(config, "protection.prevent-mob-spawns", Boolean.class);
        Validation.strings(config, "placement.replaceable");

        int blocks = config.getInt("performance.blocks-per-tick", 3000), jobs = config.getInt("performance.max-concurrent", 1);
        int cells = config.getInt("limits.max-cells", 40000);

        double millis = config.getDouble("performance.millis-per-tick", 4);
        long volume = config.getLong("limits.max-volume", 4_000_000);

        int chunks = config.getInt("limits.max-chunks", 1024), defaults = config.getInt("defaults.cells", 21);

        if (blocks < 1 || blocks > 100000 || millis < 0.1 || millis > 40 || !Double.isFinite(millis) || jobs < 1
            || jobs > 8 || cells < 4 || cells > 1_000_000 || volume < 16 || volume > 16_000_000 || chunks < 1
            || chunks > 4096 || defaults < 2 || (long) defaults * defaults > cells
        ) {
            throw new IllegalArgumentException("Invalid performance/limit settings in config.yml.");
        }

        Set<String> replaceable = new HashSet<>();

        for (String value : config.getStringList("placement.replaceable")) {
            var material = Material.matchMaterial(value);

            if (material == null || !material.isBlock()) {
                throw new IllegalArgumentException("Unknown replaceable block: " + value);
            }

            replaceable.add(material.name());
        }

        return new Settings(blocks, millis, jobs, cells, volume, chunks, defaults,
            config.getBoolean("protection.enabled", true), Set.copyOf(replaceable), PresetLoader.load(directory),
            Messages.load(directory.resolve("messages.yml")), config.getBoolean("protection.prevent-mob-spawns", true),
            VisualSettings.read(config), ProgressSettings.read(config)
        );
    }
}
