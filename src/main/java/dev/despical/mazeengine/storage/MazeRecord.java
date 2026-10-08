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

package dev.despical.mazeengine.storage;

import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.MazeLayout;

import java.time.Instant;
import java.util.UUID;

/**
 * Retains the saved state needed to manage and reconstruct one maze.
 * <p>
 * The record combines persistent identity, owner, world and bounds, generation
 * seed, frozen preset, graph, lifecycle status, snapshot flag, creation time,
 * failure text, and an optional custom teleport point. It describes a saved
 * maze rather than a mutable live Bukkit world.
 * <p>
 * State changes create replacement records. Regeneration retains the original
 * terrain snapshot, creation timestamp, and custom destination while replacing
 * the seed and graph. MazeRepository persists the record, and the service
 * publishes replacements as operations progress.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record MazeRecord(
    String name,
    UUID worldId,
    String worldName,
    UUID owner,
    Bounds bounds,
    long seed,
    Preset preset,
    MazeLayout layout,
    Status status,
    boolean hasSnapshot,
    Instant created,
    String error,
    TeleportPoint teleportPoint
) {

    public MazeRecord(String name, UUID worldId, String worldName, UUID owner, Bounds bounds, long seed, Preset preset,
        MazeLayout layout, Status status, boolean hasSnapshot, Instant created, String error
    ) {
        this(name, worldId, worldName, owner, bounds, seed, preset, layout, status, hasSnapshot, created, error, null);
    }

    /**
     * Describes the saved record's preparation, mutation, and recovery lifecycle.
     * <p>
     * PREPARING precedes terrain mutation, GENERATING and DELETING indicate world
     * work, and READY marks successful completion. FAILED retains a region that
     * may need recovery; successful removal deletes the record instead of storing
     * a separate deleted state.
     */
    public enum Status {

        PREPARING, GENERATING, READY, DELETING, FAILED
    }

    public MazeRecord state(Status status, boolean snapshot, String error) {
        return new MazeRecord(name, worldId, worldName, owner, bounds, seed, preset, layout, status, snapshot, created,
            error, teleportPoint);
    }

    public MazeRecord regenerate(long newSeed, MazeLayout newLayout) {
        return new MazeRecord(name, worldId, worldName, owner, bounds, newSeed, preset, newLayout, Status.GENERATING,
            hasSnapshot, created, "", teleportPoint);
    }

    public MazeRecord withTeleportPoint(TeleportPoint point) {
        return new MazeRecord(name, worldId, worldName, owner, bounds, seed, preset, layout, status, hasSnapshot,
            created, error, point);
    }
}
