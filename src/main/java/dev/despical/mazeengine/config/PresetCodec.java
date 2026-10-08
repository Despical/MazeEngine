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

import dev.despical.mazeengine.core.GenerationSettings;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Translates preset configuration into validated theme values and back again.
 * <p>
 * Reading rejects unknown keys and unexpected scalar or list types, resolves
 * generation defaults from complexity, and parses block palettes and
 * decorations. Optional advanced controls replace the derived generation
 * biases when they are explicitly configured.
 * <p>
 * Serialization writes geometry, display metadata, advanced generation values,
 * palette patterns, and decorations into a portable nested map. MazeRepository
 * uses that map to retain the full preset with a saved maze rather than relying
 * on whichever configuration happens to exist at the next startup.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class PresetCodec {

    private PresetCodec() {
    }

    static Preset read(String name, ConfigurationSection config) {
        Validation.keys(config,
            Set.of("path-width", "wall-thickness", "wall-height", "roof", "snapshot", "placement", "algorithm",
                "complexity", "advanced.branching", "advanced.turn-bias", "advanced.horizontal-bias",
                "advanced.braid-chance", "palette.floor", "palette.wall", "palette.ceiling", "palette.cap",
                "palette.pattern", "palette.section-size", "palette.wall-inlay", "decoration.inlay-chance",
                "decoration.cap-chance", "decoration.light-spacing", "decoration.light", "decoration.entrance-marker",
                "decoration.exit-marker", "display-name", "description", "accent", "difficulty"));

        for (String key : List.of("path-width", "wall-thickness", "wall-height", "decoration.light-spacing",
            "palette.section-size")) {
            Validation.number(config, key, true);
        }

        for (String key : List.of("complexity", "advanced.branching", "advanced.turn-bias", "advanced.horizontal-bias",
            "advanced.braid-chance", "decoration.cap-chance", "decoration.inlay-chance")) {
            Validation.number(config, key, false);
        }

        for (String key : List.of("roof", "snapshot")) {
            Validation.type(config, key, Boolean.class);
        }

        for (String key : List.of("placement", "algorithm", "decoration.light", "decoration.entrance-marker",
            "decoration.exit-marker", "display-name", "description", "accent", "difficulty", "palette.pattern")) {
            Validation.type(config, key, String.class);
        }

        for (String key : List.of("palette.floor", "palette.wall", "palette.ceiling", "palette.cap")) {
            Validation.strings(config, key);
        }

        if (config.contains("palette.wall-inlay")) {
            Validation.strings(config, "palette.wall-inlay");
        }

        var pattern = Palette.Pattern.valueOf(config.getString("palette.pattern", "RANDOM").toUpperCase(Locale.ROOT));
        int sectionSize = config.getInt("palette.section-size", 8);
        double complexity = config.getDouble("complexity", 0.7);

        var defaults = GenerationSettings.fromComplexity(complexity);
        var mode = GenerationSettings.Mode.valueOf(config.getString("algorithm", "PERFECT").toUpperCase(Locale.ROOT));
        var settings = new GenerationSettings(complexity, config.getDouble("advanced.branching", defaults.branching()),
            config.getDouble("advanced.turn-bias", defaults.turnBias()),
            config.getDouble("advanced.horizontal-bias", 0.5), mode,
            config.getDouble("advanced.braid-chance", mode == GenerationSettings.Mode.BRAIDED ? 0.3 : 0));

        return Preset.builder(name).pathWidth(config.getInt("path-width", 3))
            .wallThickness(config.getInt("wall-thickness", 1)).wallHeight(config.getInt("wall-height", 4))
            .roof(config.getBoolean("roof")).snapshot(config.getBoolean("snapshot"))
            .placement(Preset.Placement.valueOf(config.getString("placement", "CLEAR").toUpperCase(Locale.ROOT)))
            .generation(settings)
            .floor(Palette.parse(config.getStringList("palette.floor"), true).patterned(pattern, sectionSize))
            .wall(Palette.parse(config.getStringList("palette.wall"), true).patterned(pattern, sectionSize))
            .ceiling(Palette.parse(config.getStringList("palette.ceiling"), true).patterned(pattern, sectionSize))
            .cap(Palette.caps(config.getStringList("palette.cap")).patterned(pattern, sectionSize))
            .capChance(config.getDouble("decoration.cap-chance", 0.15))
            .lightSpacing(config.getInt("decoration.light-spacing", 4))
            .light(block(config.getString("decoration.light", "minecraft:sea_lantern")))
            .entranceMarker(block(config.getString("decoration.entrance-marker", "minecraft:emerald_block")))
            .exitMarker(block(config.getString("decoration.exit-marker", "minecraft:gold_block")))
            .displayName(config.getString("display-name", Preset.capitalized(name)))
            .description(config.getString("description", "A custom maze to explore."))
            .accent(config.getString("accent", "#8BD5CA"))
            .difficulty(Preset.Difficulty.valueOf(config.getString("difficulty", "MEDIUM").toUpperCase(Locale.ROOT)))
            .wallInlay(config.getStringList("palette.wall-inlay").isEmpty() ? null
                : Palette.parse(config.getStringList("palette.wall-inlay"), false))
            .inlayChance(config.getDouble("decoration.inlay-chance", 0)).build();
    }

    private static String block(String value) {
        return Palette.parse(List.of(value), true).entries().getFirst().block();
    }

    static Map<String, Object> serialize(Preset preset) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("display-name", preset.displayName());
        values.put("description", preset.description());
        values.put("accent", preset.accent());
        values.put("difficulty", preset.difficulty().name());
        values.put("path-width", preset.pathWidth());
        values.put("wall-thickness", preset.wallThickness());
        values.put("wall-height", preset.wallHeight());
        values.put("roof", preset.roof());
        values.put("snapshot", preset.snapshot());
        values.put("placement", preset.placement().name());
        values.put("complexity", preset.generation().complexity());
        values.put("algorithm", preset.generation().mode().name());
        values.put("advanced",
            Map.of("branching", preset.generation().branching(), "turn-bias", preset.generation().turnBias(),
                "horizontal-bias", preset.generation().horizontalBias(), "braid-chance",
                preset.generation().braidChance()));

        Map<String, Object> palette = new LinkedHashMap<>(Map.of("floor", preset.floor().serialize(), "wall",
            preset.wall().serialize(), "ceiling", preset.ceiling().serialize(), "cap", preset.cap().serialize(),
            "pattern", preset.floor().pattern().name(), "section-size", preset.floor().sectionSize()));

        if (preset.wallInlay() != null) {
            palette.put("wall-inlay", preset.wallInlay().serialize());
        }

        values.put("palette", palette);
        values.put("decoration",
            Map.of("cap-chance", preset.capChance(), "light-spacing", preset.lightSpacing(), "light", preset.light(),
                "entrance-marker", preset.entranceMarker(), "exit-marker", preset.exitMarker(), "inlay-chance",
                preset.inlayChance()));
        return values;
    }
}
