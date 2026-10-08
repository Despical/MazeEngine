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

import net.kyori.adventure.bossbar.BossBar;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Set;

/**
 * Stores the appearance and visibility settings for operation boss bars.
 * <p>
 * The enabled flag determines whether player actors see a bar. Color and
 * overlay use Adventure's supported boss-bar enums, while MazeProgress supplies
 * the operation title and percentage as work advances.
 * <p>
 * Configuration reading checks the accepted value types and resolves enum
 * names without case sensitivity. Missing options preserve the standard bar
 * appearance. These settings control presentation and do not change the world
 * operation's block or time budget.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
public record ProgressSettings(boolean enabled, BossBar.Color color, BossBar.Overlay overlay) {

    static final Set<String> KEYS = Set.of("progress.enabled", "progress.color", "progress.overlay");

    public static ProgressSettings read(ConfigurationSection config) {
        Validation.type(config, "progress.enabled", Boolean.class);
        Validation.type(config, "progress.color", String.class);
        Validation.type(config, "progress.overlay", String.class);

        return new ProgressSettings(config.getBoolean("progress.enabled", true),
            BossBar.Color.valueOf(config.getString("progress.color", "GREEN").toUpperCase(Locale.ROOT)),
            BossBar.Overlay.valueOf(config.getString("progress.overlay", "PROGRESS").toUpperCase(Locale.ROOT)));
    }
}
