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

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Stores validated preview, guide, and private-display configuration.
 * <p>
 * The settings bound session durations, preview slots and display counts,
 * per-tick spawning, guide look-ahead, arrow geometry, lighting, and view range.
 * The guide's block-state string determines its visible material and color.
 * <p>
 * Reading checks expected types and finite supported ranges before publication.
 * The production block parser resolves visible Bukkit block data; a supplied
 * parser supports isolated validation tests. Active sessions capture their
 * expiry when created, while the visual service can refresh applicable display
 * appearance and guide geometry after reload.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
public record VisualSettings(
    int previewDurationSeconds,
    int previewMaxSessions,
    int previewMaxDisplays,
    int spawnsPerTick,
    int guideDurationSeconds,
    int guideLookAhead,
    String guideBlock,
    double guideElevation,
    double guideWidth,
    double guideThickness,
    double viewRange,
    int blockLight,
    int skyLight,
    boolean actionBar
) {

    static final Set<String> KEYS = Set.of("visuals.preview.duration-seconds", "visuals.preview.max-sessions",
        "visuals.preview.max-displays", "visuals.spawns-per-tick", "visuals.guide.duration-seconds",
        "visuals.guide.look-ahead", "visuals.guide.block", "visuals.guide.elevation", "visuals.guide.width",
        "visuals.guide.thickness", "visuals.view-range", "visuals.brightness.block", "visuals.brightness.sky",
        "visuals.action-bar");

    public static VisualSettings read(ConfigurationSection config) {
        return read(config, value -> {
            var data = Bukkit.createBlockData(value);

            if (data.getMaterial().isAir()) {
                throw new IllegalArgumentException("visuals.guide.block must be a visible block.");
            }

            return data.getAsString();
        });
    }

    static VisualSettings read(ConfigurationSection config, UnaryOperator<String> blockParser) {
        for (String key : KEYS) {
            if (key.equals("visuals.guide.block")) {
                Validation.type(config, key, String.class);
            } else if (key.equals("visuals.action-bar")) {
                Validation.type(config, key, Boolean.class);
            } else {
                Validation.number(config, key, !Set.of("visuals.guide.elevation", "visuals.guide.width",
                    "visuals.guide.thickness", "visuals.view-range").contains(key));
            }
        }

        String block = config.getString("visuals.guide.block", "minecraft:lime_concrete");
        String blockData = blockParser.apply(block);

        return new VisualSettings(integer(config, "visuals.preview.duration-seconds", 300, 1, 3600),
            integer(config, "visuals.preview.max-sessions", 8, 1, 32),
            integer(config, "visuals.preview.max-displays", 12000, 100, 12000),
            integer(config, "visuals.spawns-per-tick", 100, 1, 1000),
            integer(config, "visuals.guide.duration-seconds", 600, 1, 3600),
            integer(config, "visuals.guide.look-ahead", 7, 1, 32), blockData,
            decimal(config, "visuals.guide.elevation", 1.14, 1.01, 2.0),
            decimal(config, "visuals.guide.width", 0.05, 0.01, 0.5),
            decimal(config, "visuals.guide.thickness", 0.025, 0.005, 0.25),
            decimal(config, "visuals.view-range", 8.0, 0.1, 16), integer(config, "visuals.brightness.block", 15, 0, 15),
            integer(config, "visuals.brightness.sky", 15, 0, 15), config.getBoolean("visuals.action-bar", true));
    }

    private static int integer(ConfigurationSection config, String key, int fallback, int min, int max) {
        int value = config.getInt(key, fallback);

        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " must be between " + min + " and " + max + ".");
        }

        return value;
    }

    private static double decimal(ConfigurationSection config, String key, double fallback, double min, double max) {
        double value = config.getDouble(key, fallback);

        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(key + " must be between " + min + " and " + max + ".");
        }

        return value;
    }
}
