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

package dev.despical.mazeengine;

import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.despical.mazeengine.api.MazeEngineApi;
import dev.despical.mazeengine.command.BrigadierCommands;
import dev.despical.mazeengine.config.DefaultResources;
import dev.despical.mazeengine.config.Messages;
import dev.despical.mazeengine.config.Settings;
import dev.despical.mazeengine.integration.OptionalIntegrations;
import dev.despical.mazeengine.listener.MobSpawnListener;
import dev.despical.mazeengine.listener.OperationSafetyListener;
import dev.despical.mazeengine.listener.TerrainProtectionListener;
import dev.despical.mazeengine.service.api.MazeApiProvider;
import dev.despical.mazeengine.service.MazeService;
import dev.despical.mazeengine.service.visual.MazeVisuals;
import dev.despical.mazeengine.service.PendingActions;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Coordinates the lifecycle of the MazeEngine plugin and its shared services.
 * <p>
 * Startup installs missing resources, validates configuration, loads saved maze
 * state, and registers commands, listeners, visual sessions, and the Bukkit API
 * provider. A startup failure disables the plugin rather than exposing services
 * with incomplete configuration or missing saved records.
 * <p>
 * Reload builds a replacement Settings value before publishing it, so invalid
 * configuration leaves the previous settings active. Shutdown unregisters the
 * API and closes integrations, callbacks, displays, and maze workers in order.
 * The service accessors are intended for enabled-plugin use on the server thread.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeEnginePlugin extends JavaPlugin {

    private volatile Settings settings;
    private MazeService mazes;
    private BrigadierCommands commands;
    private MazeVisuals visuals;
    private PendingActions actions;
    private Metrics metrics;
    private MazeApiProvider api;
    private OptionalIntegrations integrations;

    @Override
    public void onEnable() {
        var pluginManager = getServer().getPluginManager();

        try {
            DefaultResources.install(this);
            reloadSettings();

            mazes = new MazeService(this);
            api = new MazeApiProvider(this);
            getServer().getServicesManager().register(MazeEngineApi.class, api, this, ServicePriority.Normal);

            visuals = new MazeVisuals(this);
            actions = new PendingActions(this);
            commands = new BrigadierCommands(this);

            pluginManager.registerEvents(new MobSpawnListener(this), this);
            pluginManager.registerEvents(new TerrainProtectionListener(this), this);
            pluginManager.registerEvents(new OperationSafetyListener(this), this);

            integrations = new OptionalIntegrations(this);

            initializeMetrics();
            getLogger().info("MazeEngine enabled: " + mazes.records().size() + " saved mazes, " + settings.presets().size() + " presets.");
        } catch (Exception error) {
            getLogger().log(Level.SEVERE, "Cannot enable MazeEngine: " + error.getMessage(), error);
            pluginManager.disablePlugin(this);
        }
    }

    private void initializeMetrics() {
        try {
            metrics = new Metrics(this, 34555);
        } catch (Exception metricsError) {
            getLogger().warning("bStats could not start: " + metricsError.getMessage());
        }
    }

    public void reloadSettings() throws Exception {
        settings = Settings.load(getDataFolder().toPath());

        if (visuals != null) {
            visuals.reloadSettings();
        }
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return settings.messages();
    }

    public MazeService mazes() {
        return mazes;
    }

    public MazeVisuals visuals() {
        return visuals;
    }

    public PendingActions actions() {
        return actions;
    }

    /**
     * Returns the API provider owned by this plugin instance.
     * <p>
     * Successful startup creates this provider and registers the same instance
     * with Bukkit's {@link org.bukkit.plugin.ServicesManager} under
     * {@link MazeEngineApi}. Consumers may use this accessor when they already
     * hold the plugin instance or resolve the registered service after enable.
     * <p>
     * Registry access and operation submission follow the API's server-thread
     * requirements. Disabling unregisters the service and closes its operation
     * coordinator; retaining this reference does not extend the provider's usable
     * lifetime. Before initialization, this accessor returns {@code null}.
     *
     * @return this plugin's API provider, or {@code null} before initialization
     */
    public MazeEngineApi api() {
        return api;
    }

    public LiteralCommandNode<CommandSourceStack> commandTree() {
        return commands.tree().build();
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);

        if (integrations != null) {
            integrations.close();
        }

        if (metrics != null) {
            metrics.shutdown();
        }

        if (commands != null) {
            commands.close();
        }

        if (actions != null) {
            actions.close();
        }

        if (visuals != null) {
            visuals.close();
        }

        if (mazes != null) {
            mazes.close();
        }
    }
}
