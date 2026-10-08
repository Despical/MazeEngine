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

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Owns the private block displays and chunk leases of one visual session.
 * <p>
 * Each session captures its viewer, world, token, and expiry. Display spawning
 * requests missing chunks asynchronously and returns control until a chunk
 * lease is ready. Completion is handed back to the server thread and discarded
 * when the session is no longer active.
 * <p>
 * Displays are nonpersistent, fixed to world coordinates, and visible only to
 * their viewer. Session release removes displays, cancels pending loads, and
 * returns every held chunk ticket. Preview and guide sessions extend this
 * ownership model with their own plan or route data.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
abstract class DisplaySession {

    private final MazeVisuals owner;
    protected final MazeEnginePlugin plugin;
    final UUID token = UUID.randomUUID();
    final World world;
    final long expires;
    final Map<VisualBlock, BlockDisplay> blocks = new LinkedHashMap<>();
    final Map<Long, Chunk> leases = new HashMap<>();
    final Map<Long, CompletableFuture<Chunk>> loading = new HashMap<>();
    final UUID viewer;
    boolean active = true;

    DisplaySession(MazeVisuals owner, Player player, World world, long duration) {
        this.owner = owner;
        this.plugin = owner.plugin;
        this.world = world;

        viewer = player.getUniqueId();
        expires = System.currentTimeMillis() + duration;
    }

    boolean spawn(Player player, VisualBlock block) {
        if (!active) {
            return false;
        }

        Location anchor = block.anchor(world);
        long key = block.chunkKey(world);

        if (!leases.containsKey(key)) {
            if (!loading.containsKey(key)) {
                var future = world.getChunkAtAsync(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4, true);
                loading.put(key, future);

                future.whenComplete((chunk, error) -> owner.onMain(() -> {
                    if (!active || loading.get(key) != future) {
                        return;
                    }

                    loading.remove(key);

                    if (error != null) {
                        owner.visualFailure(viewer, this);

                        return;
                    }

                    plugin.mazes().holdChunk(chunk);
                    leases.put(key, chunk);
                }));
            }

            return false;
        }

        BlockDisplay display = world.spawn(anchor, BlockDisplay.class, entity -> {
            var settings = plugin.settings();
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setBlock(Bukkit.createBlockData(block.blockData()));
            entity.setRotation(0, 0);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setGravity(false);
            entity
                .setBrightness(new Display.Brightness(settings.visuals().blockLight(), settings.visuals().skyLight()));
            entity.setShadowRadius(0);
            entity.setShadowStrength(0);
            entity.setViewRange((float) settings.visuals().viewRange());
            entity.setInterpolationDuration(0);
            entity.setTeleportDuration(0);
            entity.setTransformation(block.transform(anchor));
        });

        blocks.put(block, display);
        player.showEntity(plugin, display);

        return true;
    }

    void retainChunks(Set<Long> used) {
        for (long key : new HashSet<>(leases.keySet())) {
            if (!used.contains(key)) {
                plugin.mazes().releaseChunk(leases.remove(key));
            }
        }

        for (long key : new HashSet<>(loading.keySet())) {
            if (!used.contains(key)) {
                loading.remove(key).cancel(false);
            }
        }
    }

    void release() {
        active = false;
        blocks.values().forEach(BlockDisplay::remove);
        blocks.clear();
        loading.values().forEach(future -> future.cancel(false));
        loading.clear();
        leases.values().forEach(plugin.mazes()::releaseChunk);
        leases.clear();
    }
}
