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

/**
 * Owns optional integrations enabled alongside the plugin's core services.
 * <p>
 * PlaceholderAPI classes are reached only when that plugin is enabled. Expansion
 * startup is isolated so a registration failure can be logged and cleaned up
 * without preventing ordinary maze generation. WorldGuard availability is also
 * reported during initialization.
 * <p>
 * The class keeps external integration lifecycle work out of the plugin entry
 * point. Its close method releases the expansion it successfully started;
 * WorldEdit selections and snapshots are resolved separately by the maze service
 * when an operation actually requests them.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class OptionalIntegrations implements AutoCloseable {

    private MazePlaceholders placeholders;

    public OptionalIntegrations(MazeEnginePlugin plugin) {
        var pluginManager = plugin.getServer().getPluginManager();
        var logger = plugin.getLogger();

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            var expansion = new MazePlaceholders(plugin);

            try {
                expansion.start();

                placeholders = expansion;
                logger.info("PlaceholderAPI integration enabled (mazeengine).");
            } catch (RuntimeException failure) {
                expansion.close();
                logger.warning("PlaceholderAPI integration could not start: " + failure.getMessage());
            }
        }

        if (pluginManager.isPluginEnabled("WorldGuard")) {
            logger.info("WorldGuard area checks enabled.");
        }
    }

    @Override
    public void close() {
        if (placeholders != null) {
            placeholders.close();
        }
    }
}
