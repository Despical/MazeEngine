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

package dev.despical.mazeengine.service.visual;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Owns private previews and route guides anchored to maze world coordinates.
 * <p>
 * The service delegates session creation to preview and guide managers,
 * budgets display spawning on a server ticker, updates routes as viewers move,
 * and combines their status in one action bar. Preview geometry planning uses
 * a bounded worker queue; entity and chunk ownership stays on the server thread.
 * <p>
 * Sessions end on expiry, viewer disconnect, relevant world unload, or obsolete
 * maze state. Reload refreshes display appearance and invalidates guide geometry
 * while captured expiry times remain intact. Closing stops planning and ticking
 * and releases displays and chunk leases for every session.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeVisuals implements Listener, AutoCloseable {

    final MazeEnginePlugin plugin;
    final Map<UUID, PreviewSession> previews = new LinkedHashMap<>();
    final Map<UUID, GuideSession> solvers = new LinkedHashMap<>();
    final ThreadPoolExecutor planner = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(8), Thread.ofPlatform().name("mazeengine-preview-", 0).factory());
    private final BukkitTask ticker;
    private final PreviewManager previewManager;
    private final GuideManager guideManager;
    private volatile boolean closed;
    private int ticks;

    public MazeVisuals(MazeEnginePlugin plugin) {
        this.plugin = plugin;
        previewManager = new PreviewManager(this);
        guideManager = new GuideManager(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public void preview(Player player, String name, World world, int x, int y, int z, int width, int depth, long seed,
        Preset preset) {
        previewManager.preview(player, name, world, x, y, z, width, depth, seed, preset);
    }

    public void build(Player player, String token) {
        previewManager.build(player, token);
    }

    public boolean confirmPreview(Player player, String name, boolean plain) {
        return previewManager.confirmPreview(player, name, plain);
    }

    public void stopPreview(Player player) {
        previewManager.stopPreview(player);
    }

    public void stopPreview(Player player, String token) {
        previewManager.stopPreview(player, token);
    }

    private void removePreview(UUID id) {
        previewManager.removePreview(id);
    }

    public void solve(Player player, MazeRecord record) {
        guideManager.solve(player, record);
    }

    public void stopSolver(Player player) {
        guideManager.stopSolver(player);
    }

    public void stopSolver(Player player, String token) {
        guideManager.stopSolver(player, token);
    }

    public void stopSolver(Player player, String token, String name) {
        guideManager.stopSolver(player, token, name);
    }

    private void removeSolver(UUID id) {
        guideManager.removeSolver(id);
    }

    void onMain(Runnable action) {
        if (!closed) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!closed) {
                    action.run();
                }
            });
        }
    }

    void visualFailure(UUID viewer, DisplaySession session) {
        if (previews.get(viewer) == session) {
            removePreview(viewer);
        }

        if (solvers.get(viewer) == session) {
            removeSolver(viewer);
        }

        Player target = Bukkit.getPlayer(viewer);

        if (target != null) {
            clearBar(target);
            plugin.messages().send(target, "error", "reason", "Could not load the visual region. Try again.");
        }
    }

    void clearBar(Player player) {
        if (!previews.containsKey(player.getUniqueId()) && !solvers.containsKey(player.getUniqueId())) {
            player.sendActionBar(Component.empty());
        }
    }

    void ready(MazeRecord record) {
        if (record.status() != MazeRecord.Status.READY || plugin.mazes().busy(record.name())) {
            throw new IllegalArgumentException("Maze is not ready.");
        }
    }

    public void reloadSettings() {
        var settings = plugin.settings().visuals();
        var sessions = new ArrayList<DisplaySession>(previews.values());
        sessions.addAll(solvers.values());

        for (var session : sessions) {
            for (var display : session.blocks.values()) {
                display.setViewRange((float) settings.viewRange());
                display.setBrightness(new Display.Brightness(settings.blockLight(), settings.skyLight()));
            }

            if (!settings.actionBar()) {
                Player player = Bukkit.getPlayer(session.viewer);

                if (player != null) {
                    player.sendActionBar(Component.empty());
                }
            }
        }

        for (var guide : solvers.values()) {
            guide.cell = -2;
        }
    }

    private void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        int remaining = plugin.settings().visuals().spawnsPerTick();

        for (var entry : new ArrayList<>(previews.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            PreviewSession preview = entry.getValue();

            if (player == null || !player.getWorld().equals(preview.world) || now >= preview.expires) {
                removePreview(entry.getKey());

                if (player != null) {
                    clearBar(player);
                    plugin.messages().send(player, "preview-ended");
                }

                continue;
            }

            int attempts = Math.min(remaining, preview.pending.size());

            while (attempts-- > 0 && !preview.pending.isEmpty()) {
                VisualBlock block = preview.pending.removeFirst();

                if (preview.spawn(player, block)) {
                    remaining--;
                } else {
                    preview.pending.addLast(block);
                }
            }
        }

        if (ticks % 5 != 0) {
            return;
        }

        for (var entry : new ArrayList<>(solvers.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            GuideSession solver = entry.getValue();
            MazeRecord record;

            try {
                record = plugin.mazes().require(solver.name);
                ready(record);
            } catch (IllegalArgumentException error) {
                removeSolver(entry.getKey());

                if (player != null) {
                    clearBar(player);
                }

                continue;
            }

            if (player == null || now >= solver.expires || !player.getWorld().getUID().equals(record.worldId())
                || record.layout() != solver.layout) {
                removeSolver(entry.getKey());

                if (player != null) {
                    clearBar(player);
                    plugin.messages().send(player, "guide-ended");
                }

                continue;
            }

            new GuideGeometry(plugin).update(player, record, solver);
        }

        if (ticks % 10 != 0 || !plugin.settings().visuals().actionBar()) {
            return;
        }

        Set<UUID> viewers = new HashSet<>(previews.keySet());
        viewers.addAll(solvers.keySet());

        for (UUID id : viewers) {
            Player player = Bukkit.getPlayer(id);

            if (player == null) {
                continue;
            }

            VisualActionBar.send(player, previews.get(id), solvers.get(id), now, plugin.messages());
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        removePreview(id);
        removeSolver(id);
    }

    @EventHandler(ignoreCancelled = true)
    public void unload(WorldUnloadEvent event) {
        for (var entry : new ArrayList<>(previews.entrySet())) {
            if (entry.getValue().world.equals(event.getWorld())) {
                removePreview(entry.getKey());
            }
        }

        for (var entry : new ArrayList<>(solvers.entrySet())) {
            if (entry.getValue().world.equals(event.getWorld())) {
                removeSolver(entry.getKey());
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        ticker.cancel();
        planner.shutdownNow();

        for (UUID id : new ArrayList<>(previews.keySet())) {
            Player player = Bukkit.getPlayer(id);
            removePreview(id);

            if (player != null) {
                player.sendActionBar(Component.empty());
            }
        }

        for (UUID id : new ArrayList<>(solvers.keySet())) {
            Player player = Bukkit.getPlayer(id);
            removeSolver(id);

            if (player != null) {
                player.sendActionBar(Component.empty());
            }
        }
    }
}
