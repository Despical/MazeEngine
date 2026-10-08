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

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs the bundled configuration and preset files that are missing locally.
 * <p>
 * The resource list contains the main settings, message templates, and shipped
 * theme presets. Installation checks each destination before copying its JAR
 * resource, allowing first startup to create a complete editable configuration.
 * <p>
 * Existing server files are retained, including administrator edits to presets
 * and messages. This class only installs files; Settings and the preset loader
 * perform validation before the plugin publishes any replacement configuration.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class DefaultResources {

    public static final List<String> PRESETS = List.of("default", "hedge", "dungeon", "wilderness", "desert", "temple",
        "frost", "nether", "ocean", "copper", "gladiator", "end", "claustrophobic", "bamboo", "catacombs", "crystal",
        "factory", "library", "monochrome", "ruins", "rainbow", "volcanic", "swamp", "haunted", "sakura", "sky",
        "steampunk", "graveyard", "candy", "abyss");

    private DefaultResources() {
    }

    public static void install(JavaPlugin plugin) {
        List<String> files = new ArrayList<>(List.of("config.yml", "messages.yml"));
        PRESETS.forEach(name -> files.add("presets/" + name + ".yml"));

        for (String file : files) {
            if (!Files.exists(plugin.getDataFolder().toPath().resolve(file))) {
                plugin.saveResource(file, false);
            }
        }
    }
}
