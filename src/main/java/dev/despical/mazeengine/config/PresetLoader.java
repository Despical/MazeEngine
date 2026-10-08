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

import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads the configured preset directory as one validated ordered collection.
 * <p>
 * Each YAML filename supplies a lowercase preset identifier and is checked
 * against the accepted naming rules before its contents are decoded. The
 * default preset must exist, and an invalid file aborts the replacement load
 * instead of publishing only the presets that happened to succeed.
 * <p>
 * The result places the default theme first and orders other themes by display
 * name with identifier tie-breaking. It is exposed as an unmodifiable map so
 * menus and public preset queries share the same stable display order.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class PresetLoader {

    private PresetLoader() {
    }

    static Map<String, Preset> load(Path directory) throws Exception {
        Map<String, Preset> presets = new TreeMap<>();

        try (var paths = Files.list(directory.resolve("presets"))) {
            for (Path path : paths.filter(candidate -> candidate.getFileName().toString().endsWith(".yml")).sorted().toList()) {
                String name = path.getFileName().toString().replaceFirst("\\.yml$", "");

                if (!name.matches("[a-z0-9_-]{1,48}")) {
                    throw new IllegalArgumentException("Invalid preset filename: " + name);
                }

                var config = new YamlConfiguration();
                config.load(path.toFile());

                presets.put(name, Preset.read(name, config));
            }
        }

        if (!presets.containsKey("default")) {
            throw new IllegalArgumentException("presets/default.yml is required.");
        }

        Map<String, Preset> ordered = new LinkedHashMap<>();
        ordered.put("default", presets.remove("default"));

        presets.values().stream()
            .sorted(
                Comparator.comparing(Preset::displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(Preset::name))
            .forEach(preset -> ordered.put(preset.name(), preset));
        return Collections.unmodifiableMap(ordered);
    }
}
