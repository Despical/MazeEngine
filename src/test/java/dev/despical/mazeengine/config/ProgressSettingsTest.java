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

import net.kyori.adventure.bossbar.BossBar;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Verifies default and custom operation boss-bar configuration.
 * <p>
 * The tests check that omitted options preserve the existing appearance and
 * that supported enabled, color, and overlay values can be loaded. Invalid
 * types or enum values must reject the replacement configuration.
 * <p>
 * The fixture validates settings without creating a player or displaying a
 * live bar; operation lifecycle and visual updates belong to MazeProgress.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class ProgressSettingsTest {

    @Test
    void defaultsPreserveTheExistingBossBar() {
        var settings = ProgressSettings.read(new YamlConfiguration());
        assertTrue(settings.enabled());
        assertEquals(BossBar.Color.GREEN, settings.color());
        assertEquals(BossBar.Overlay.PROGRESS, settings.overlay());
    }

    @Test
    void acceptsCustomAppearanceAndRejectsInvalidValues() throws Exception {
        var config = new YamlConfiguration();
        config.loadFromString("progress: {enabled: false, color: purple, overlay: notched_10}");
        var settings = ProgressSettings.read(config);
        assertFalse(settings.enabled());
        assertEquals(BossBar.Color.PURPLE, settings.color());
        assertEquals(BossBar.Overlay.NOTCHED_10, settings.overlay());
        config.set("progress.color", "orange");
        assertThrows(IllegalArgumentException.class, () -> ProgressSettings.read(config));
        config.set("progress.color", "GREEN");
        config.set("progress.enabled", "false");
        assertThrows(IllegalArgumentException.class, () -> ProgressSettings.read(config));
    }
}
