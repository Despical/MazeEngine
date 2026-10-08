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

import dev.despical.mazeengine.service.MazeService;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.api.MazeEngineApi;
import dev.despical.mazeengine.api.MazeOperations;
import dev.despical.mazeengine.api.MazeRegistry;
import dev.despical.mazeengine.api.PresetRegistry;
import dev.despical.mazeengine.api.event.MazeOperationCompletedEvent;
import dev.despical.mazeengine.api.event.MazeOperationFailedEvent;
import dev.despical.mazeengine.api.model.CellPosition;
import dev.despical.mazeengine.api.model.CellSize;
import dev.despical.mazeengine.api.model.MazeBounds;
import dev.despical.mazeengine.api.model.MazeGeometry;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.model.MazeLocation;
import dev.despical.mazeengine.api.model.MazeSnapshot;
import dev.despical.mazeengine.api.model.PresetSnapshot;
import dev.despical.mazeengine.api.model.TeleportDestination;
import dev.despical.mazeengine.api.operation.MazeOperation;
import dev.despical.mazeengine.api.operation.OperationKind;
import dev.despical.mazeengine.api.operation.OperationResult;
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.MazeLayout;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Implements the Bukkit-registered public API using the plugin's internal services.
 * <p>
 * The provider exposes registry, preset, and operation adapters without placing
 * their implementation classes in the consumable API JAR. It checks server
 * thread and enabled-plugin access, converts saved records to immutable public
 * snapshots, and caches graph summaries by layout identity.
 * <p>
 * Terminal service outcomes are translated into completion or failure events.
 * Successful deletion produces a retained DELETED snapshot; failed planning
 * can produce an event with no maze description. Wrapper exceptions are
 * unwrapped before being exposed as public failure causes.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeApiProvider implements MazeEngineApi {

    private final MazeEnginePlugin plugin;
    private final Map<MazeLayout, GraphSummary> summaries = new WeakHashMap<>();

    /**
     * Retains graph statistics for public snapshot conversion.
     * <p>
     * Route length and dead-end count depend on the immutable layout rather than
     * the current world blocks. The provider caches this value by layout identity
     * to avoid repeating graph traversal for every metadata query.
     */
    private record GraphSummary(int routeLength, int deadEnds) {
    }

    public MazeApiProvider(MazeEnginePlugin plugin) {
        this.plugin = plugin;
        editing = new MazeApiOperations(plugin, this);
        registry = new MazeApiRegistry(plugin, this);
        plugin.mazes().operationListener(this::publish);
    }

    static void mainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Maze Engine API requires the server thread.");
        }
    }

    void available() {
        mainThread();

        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Maze Engine is disabled.");
        }
    }

    void actor(CommandSender actor, String action) {
        available();
        Objects.requireNonNull(actor, "actor");

        for (String permission : List.of("mazeengine.use", "mazeengine." + action)) {
            if (!actor.hasPermission(permission)) {
                throw new IllegalArgumentException("Missing permission: " + permission);
            }
        }
    }

    public PresetSnapshot preset(Preset preset) {
        return new PresetSnapshot(preset.name(), preset.displayName(), preset.description(), preset.accent(),
            PresetSnapshot.Difficulty.valueOf(preset.difficulty().name()),
            new MazeGeometry(preset.pathWidth(), preset.wallThickness(), preset.wallHeight()), preset.roof(),
            preset.snapshot(), PresetSnapshot.Algorithm.valueOf(preset.generation().mode().name()),
            preset.generation().complexity());
    }

    public MazeSnapshot snapshot(MazeRecord record) {
        return snapshot(record, false);
    }

    private MazeSnapshot snapshot(MazeRecord record, boolean deleted) {
        var bounds = record.bounds();
        var layout = record.layout();
        var teleport = record.teleportPoint();
        var graph = summaries.computeIfAbsent(layout,
            graphLayout -> new GraphSummary(graphLayout.solution().length - 1, graphLayout.deadEnds()));

        return new MazeSnapshot(new MazeId(record.name()), record.worldName(), record.owner(),
            new MazeBounds(new MazeLocation(record.worldId(), bounds.x(), bounds.y(), bounds.z()), bounds.width(),
                bounds.height(), bounds.depth()),
            new CellSize(layout.width(), layout.depth()), record.seed(), preset(record.preset()),
            deleted ? MazeSnapshot.Status.DELETED : MazeSnapshot.Status.valueOf(record.status().name()),
            record.hasSnapshot(), record.created(),
            teleport == null ? null
                : new TeleportDestination(teleport.worldId(), teleport.x(), teleport.y(), teleport.z(), teleport.yaw(),
                    teleport.pitch()),
            cell(layout, layout.entrance().cell()), cell(layout, layout.exit().cell()), graph.routeLength(),
            graph.deadEnds(), record.error());
    }

    static CellPosition cell(MazeLayout layout, int index) {
        return new CellPosition(index % layout.width(), index / layout.width());
    }

    static TeleportDestination destination(Location location) {
        return new TeleportDestination(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch());
    }

    OperationResult result(MazeService.OperationTicket ticket, MazeRecord record) {
        return new OperationResult(ticket.id(), ticket.kind(), snapshot(record, ticket.kind() == OperationKind.DELETE),
            Instant.now());
    }

    private void publish(MazeService.OperationOutcome outcome) {
        var ticket = outcome.ticket();

        if (outcome.error() == null) {
            Bukkit.getPluginManager().callEvent(new MazeOperationCompletedEvent(result(ticket, outcome.record())));
        } else {
            Bukkit.getPluginManager().callEvent(new MazeOperationFailedEvent(ticket.id(), new MazeId(outcome.name()),
                ticket.kind(), outcome.record() == null ? null : snapshot(outcome.record()), unwrap(outcome.error())));
        }
    }

    static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException)
            && error.getCause() != null) {
            error = error.getCause();
        }

        return error;
    }

    private final MazeRegistry registry;
    private final PresetRegistry themes = new PresetRegistry() {
        public List<PresetSnapshot> all() {
            available();

            return plugin.settings().presets().values().stream().map(MazeApiProvider.this::preset).toList();
        }

        public Optional<PresetSnapshot> find(String id) {
            available();

            return Optional
                .ofNullable(plugin.settings().presets().get(Objects.requireNonNull(id).toLowerCase(Locale.ROOT)))
                .map(MazeApiProvider.this::preset);
        }
    };
    private final MazeOperations editing;

    MazeOperation handle(MazeId id) {
        return new MazeOperationHandle(plugin, this, id, plugin.mazes().operation(id.value()));
    }

    public MazeRegistry mazes() {
        return registry;
    }

    public PresetRegistry presets() {
        return themes;
    }

    public MazeOperations operations() {
        return editing;
    }
}
