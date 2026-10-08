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

package dev.despical.mazeengine.service.api;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.api.MazeOperations;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.operation.MazeOperation;
import dev.despical.mazeengine.api.request.CreateMazeRequest;
import dev.despical.mazeengine.config.Preset;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Resolves public API requests into the maze service's authorized operations.
 * <p>
 * Creation starts from the current configured preset and applies explicitly
 * supplied geometry, complexity, roof, snapshot, and placement overrides.
 * An absent seed is generated at submission, and the requested world UUID
 * must resolve to a loaded world before creation is started.
 * <p>
 * The provider enforces server-thread availability and action permissions;
 * the maze service enforces ownership and operation-specific safety rules.
 * Accepted actions return a handle bound to the current operation ticket.
 * Repair reuses the saved seed and topology, and removal resolves its terrain
 * policy without requiring the chat confirmation flow.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeApiOperations implements MazeOperations {

    private final MazeEnginePlugin plugin;
    private final MazeApiProvider provider;
    private final SecureRandom random = new SecureRandom();

    MazeApiOperations(MazeEnginePlugin plugin, MazeApiProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    public MazeOperation create(CommandSender actor, CreateMazeRequest request) {
        provider.actor(actor, "create");
        Objects.requireNonNull(request);
        var base = plugin.settings().presets().get(request.preset());

        if (base == null) {
            throw new IllegalArgumentException("Unknown preset: " + request.preset());
        }

        var map = new LinkedHashMap<>(base.serialize());
        request.geometry().ifPresent(geometry -> {
            map.put("path-width", geometry.pathWidth());
            map.put("wall-thickness", geometry.wallThickness());
            map.put("wall-height", geometry.wallHeight());
        });
        request.complexity().ifPresent(complexity -> {
            map.put("complexity", complexity);
            map.remove("advanced");
        });
        request.roof().ifPresent(enabled -> map.put("roof", enabled));
        request.snapshot().ifPresent(captureSnapshot -> map.put("snapshot", captureSnapshot));
        request.placement().ifPresent(placement -> map.put("placement", placement.name()));
        var resolved = Preset.fromMap(base.name(), map);
        var origin = request.origin();
        var world = Bukkit.getWorld(origin.worldId());

        if (world == null) {
            throw new IllegalArgumentException("The maze world is not loaded.");
        }

        plugin.mazes().create(actor, request.id().value(), world, origin.x(), origin.y(), origin.z(),
            request.cells().width(), request.cells().depth(), request.seed().orElseGet(random::nextLong), resolved);

        return provider.handle(request.id());
    }

    public MazeOperation regenerate(CommandSender actor, MazeId id, long seed) {
        provider.actor(actor, "regenerate");
        plugin.mazes().regenerate(actor, plugin.mazes().require(id.value()), seed);

        return provider.handle(id);
    }

    public MazeOperation repair(CommandSender actor, MazeId id) {
        provider.actor(actor, "regenerate");
        var record = plugin.mazes().require(id.value());
        plugin.mazes().regenerate(actor, record, record.seed(), true);

        return provider.handle(id);
    }

    public MazeOperation delete(CommandSender actor, MazeId id, RemovalMode mode) {
        provider.actor(actor, "delete");
        Objects.requireNonNull(mode);
        var record = plugin.mazes().require(id.value());
        plugin.mazes().delete(actor, record,
            mode == RemovalMode.RESTORE || mode == RemovalMode.AUTO && record.hasSnapshot());

        return provider.handle(id);
    }
}
