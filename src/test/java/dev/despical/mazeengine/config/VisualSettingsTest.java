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

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

/**
 * Verifies the defaults and supported ranges of preview and guide settings.
 * <p>
 * The tests compare missing options with bundled configuration and check
 * rejection of wrong types, fractional integer values, and out-of-range
 * geometry or display settings. A supplied block parser avoids live world access.
 * <p>
 * The fixture tests replacement-configuration acceptance, not display spawning
 * or entity appearance in a running Minecraft client.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class VisualSettingsTest {

    @Test
    void missingOptionsAndBundledOptionsProduceTheSameStandardAppearance() throws Exception {
        var defaults = VisualSettings.read(new YamlConfiguration(), UnaryOperator.identity());
        var bundled = new YamlConfiguration();
        try (var input = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(input);
            bundled.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        assertEquals(defaults, VisualSettings.read(bundled, UnaryOperator.identity()));
        assertEquals(300, defaults.previewDurationSeconds());
        assertEquals(600, defaults.guideDurationSeconds());
        assertEquals("minecraft:lime_concrete", defaults.guideBlock());
        assertEquals(7, defaults.guideLookAhead());
        assertEquals(1.14, defaults.guideElevation());
        assertEquals(0.05, defaults.guideWidth());
        assertEquals(0.025, defaults.guideThickness());
    }

    @Test
    void invalidTypesFractionsAndRangesRejectTheWholeVisualConfiguration() {
        for (String key : new String[] { "visuals.preview.duration-seconds", "visuals.spawns-per-tick",
                "visuals.guide.look-ahead" }) {
            var config = new YamlConfiguration();
            config.set(key, 0);
            assertThrows(IllegalArgumentException.class, () -> VisualSettings.read(config, UnaryOperator.identity()),
                key);
            config.set(key, 1.5);
            assertThrows(IllegalArgumentException.class, () -> VisualSettings.read(config, UnaryOperator.identity()),
                key);
            config.set(key, "100");
            assertThrows(IllegalArgumentException.class, () -> VisualSettings.read(config, UnaryOperator.identity()),
                key);
        }
        for (String key : new String[] { "visuals.brightness.block", "visuals.brightness.sky" }) {
            var config = new YamlConfiguration();
            config.set(key, 16);
            assertThrows(IllegalArgumentException.class, () -> VisualSettings.read(config, UnaryOperator.identity()));
        }
        var config = new YamlConfiguration();
        config.set("visuals.guide.width", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> VisualSettings.read(config, UnaryOperator.identity()));
        config.set("visuals.guide.width", 0.8);
        assertThrows(IllegalArgumentException.class, () -> VisualSettings.read(config, UnaryOperator.identity()));
    }
}
