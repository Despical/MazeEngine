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

package dev.despical.mazeengine.service;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.api.operation.OperationKind;
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.MazeLayout;
import dev.despical.mazeengine.integration.RegionSelection;
import dev.despical.mazeengine.integration.WorldEditBridge;
import dev.despical.mazeengine.integration.WorldGuardBridge;
import dev.despical.mazeengine.storage.MazeRecord;
import dev.despical.mazeengine.storage.MazeRepository;
import dev.despical.mazeengine.storage.TeleportPoint;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Coordinates saved maze state, authorized operations, and their execution resources.
 * <p>
 * The service loads validated records through recovery, maintains name and
 * region reservations, and delegates creation, regeneration, repair, removal,
 * teleport resolution, and spatial checks to focused collaborators. A shared
 * server-tick runner owns live world steps while bounded workers plan topology
 * and a serial executor orders disk operations.
 * <p>
 * Operation tickets connect internal outcomes to public handles and events.
 * State replacement and terminal settlement happen on the server thread;
 * delayed worker callbacks are checked against their reservation tokens.
 * Shutdown stops the ticker, releases resources, cancels pending work, and
 * settles outstanding tickets without forgetting retained maze records.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeService {

    final MazeEnginePlugin plugin;
    final MazeRepository repository;
    private final WorldEditBridge worldEdit;
    final WorldGuardBridge worldGuard;

    /**
     * Binds a unique accepted operation to its internal completion future.
     * <p>
     * The UUID separates this action from later work using the same maze name.
     * The kind describes the submitted action, and the result future completes
     * with a saved record or terminal failure after the tracker publishes its outcome.
     */
    public record OperationTicket(UUID id, OperationKind kind, CompletableFuture<MazeRecord> result) {
    }

    /**
     * Carries the terminal record or failure delivered to the operation observer.
     * <p>
     * The ticket preserves operation identity and kind. A failed planning action
     * can have no record, while an error after world work can retain a failed
     * record. This notification is published before the ticket future completes.
     */
    public record OperationOutcome(String name, OperationTicket ticket, MazeRecord record, Throwable error) {
    }

    final Map<String, MazeRecord> records = new TreeMap<>();
    final Map<String, MazeJob> jobs = new LinkedHashMap<>();
    final OperationTracker operations;
    private final MazeWorkers workers = new MazeWorkers();
    private final ChunkTickets chunkTickets;
    private final MazeRegions regions;
    private final MazeTeleports teleports;
    private BukkitTask ticker;
    private volatile boolean closed;

    public MazeService(MazeEnginePlugin plugin) throws Exception {
        this.plugin = plugin;
        this.operations = new OperationTracker(() -> plugin.settings().maxConcurrent(), () -> closed,
            plugin.getLogger());
        this.chunkTickets = new ChunkTickets(plugin);
        this.regions = new MazeRegions(plugin, this);
        this.teleports = new MazeTeleports(this);
        this.repository = new MazeRepository(plugin.getDataFolder().toPath());
        worldEdit = plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit")
            || plugin.getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit") ? new WorldEditBridge()
                : null;
        worldGuard = plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard") ? new WorldGuardBridge()
            : null;
        records.putAll(MazeRecovery.load(plugin, repository));
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, new MazeJobRunner(plugin, jobs)::tick, 1, 1);
    }

    public Collection<MazeRecord> records() {
        return List.copyOf(records.values());
    }

    public int activeOperationCount() {
        return operations.reserved.size();
    }

    public void operationListener(Consumer<OperationOutcome> listener) {
        operations.operationListener(listener);
    }

    public OperationTicket operation(String name) {
        return operations.operation(name);
    }

    public boolean currentOperation(String name, UUID id) {
        return operations.currentOperation(name, id);
    }

    public int progressPercent(String name) {
        var job = jobs.get(name);

        return job == null ? 0 : job.percent();
    }

    public void holdChunk(Chunk chunk) {
        chunkTickets.hold(chunk);
    }

    public void releaseChunk(Chunk chunk) {
        chunkTickets.release(chunk);
    }

    public MazeRecord require(String name) {
        var record = records.get(MazeRepository.normalize(name));

        if (record == null) {
            throw new UnknownMazeException(name);
        }

        return record;
    }

    /**
     * Reports an identifier with no current saved maze record.
     * <p>
     * The exception retains the requested name for dedicated command feedback.
     * A new creation still in topology planning may also have no saved record;
     * its accepted operation is tracked separately by its reservation and ticket.
     */
    public static final class UnknownMazeException extends IllegalArgumentException {

        private final String mazeName;

        public UnknownMazeException(String name) {
            super("Unknown maze: " + name);
            mazeName = name;
        }

        public String mazeName() {
            return mazeName;
        }
    }

    /**
     * Reports a creation request conflicting with an existing saved maze name.
     * <p>
     * The exception retains the normalized name so the command layer can render
     * its existing-maze message rather than a generic error. It is raised before
     * new topology planning or terrain mutation is submitted.
     */
    public static final class MazeExistsException extends IllegalArgumentException {

        private final String mazeName;

        public MazeExistsException(String name) {
            super("A maze with this name already exists.");
            mazeName = name;
        }

        public String mazeName() {
            return mazeName;
        }
    }

    public boolean busy(String name) {
        return operations.reserved.contains(MazeRepository.normalize(name));
    }

    public RegionSelection selection(Player player) throws Exception {
        return bridge().selection(player);
    }

    WorldEditBridge bridge() {
        if (worldEdit == null) {
            throw new IllegalArgumentException("This operation requires a compatible WorldEdit installation.");
        }

        return worldEdit;
    }

    public World world(MazeRecord record) {
        var world = Bukkit.getWorld(record.worldId());

        if (world == null) {
            throw new IllegalArgumentException("The maze world is not loaded: " + record.worldName());
        }

        return world;
    }

    public void authorize(CommandSender sender, MazeRecord record, String action) {
        if (!sender.hasPermission("mazeengine." + action)) {
            throw new IllegalArgumentException("Missing permission: mazeengine." + action);
        }

        if (sender instanceof Player player && !player.getUniqueId().equals(record.owner())
            && !sender.hasPermission("mazeengine.manage.others")) {
            throw new IllegalArgumentException("You cannot manage another player's maze.");
        }
    }

    boolean closed() {
        return closed;
    }

    void validateRegion(World world, Bounds bounds, int cells, String ignore) {
        regions.validate(world, bounds, cells, ignore);
    }

    public void validatePreview(String name, World world, Bounds bounds, int cells) {
        name = MazeRepository.normalize(name);

        if (records.containsKey(name) || busy(name)) {
            throw new MazeExistsException(name);
        }

        regions.validate(world, bounds, cells, name, false);
    }

    void reserve(String name) {
        operations.reserve(name);
    }

    void reserve(String name, OperationKind kind) {
        operations.reserve(name, kind);
    }

    void ensureEmptyOfPlayers(World world, Bounds bounds) {
        regions.ensureEmptyOfPlayers(world, bounds);
    }

    public void create(CommandSender sender, String name, World world, int x, int y, int z, int width, int depth,
        long seed, Preset preset) {
        new MazeCreation(this).execute(sender, name, world, x, y, z, width, depth, seed, preset);
    }

    public void regenerate(CommandSender sender, MazeRecord record, long seed) {
        regenerate(sender, record, seed, false);
    }

    public void regenerate(CommandSender sender, MazeRecord record, long seed, boolean repair) {
        new MazeRegeneration(this).execute(sender, record, seed, repair);
    }

    public void delete(CommandSender sender, MazeRecord record, boolean restore) {
        new MazeDeletion(this).execute(sender, record, restore);
    }

    public void cancel(CommandSender sender, String name) {
        name = MazeRepository.normalize(name);
        var job = jobs.get(name);

        if (job != null) {
            authorize(sender, job.record, "cancel");
            job.cancelled = true;

            return;
        }

        var owner = operations.planningSenders.get(name);

        if (owner == null) {
            throw new IllegalArgumentException("No active operation for this maze.");
        }

        if (sender != owner && !sender.hasPermission("mazeengine.manage.others")) {
            throw new IllegalArgumentException("You cannot cancel another player's operation.");
        }

        workers.cancel(name);
        clearPlanning(name);
        unreserve(name);
        plugin.messages().send(sender, "cancelled", "name", name);
    }

    void plan(String name, UUID token, Supplier<MazeLayout> calculation, BiConsumer<MazeLayout, Throwable> completion) {
        try {
            workers.plan(name, () -> {
                MazeLayout layout = null;
                Throwable error = null;

                try {
                    layout = calculation.get();
                } catch (Throwable planningError) {
                    error = planningError;
                }

                MazeLayout result = layout;
                Throwable failure = error;
                onMain(() -> {
                    if (!token.equals(operations.operationTokens.get(name))) {
                        return;
                    }

                    workers.finished(name);

                    try {
                        completion.accept(result, failure);
                    } catch (Throwable callbackError) {
                        var job = jobs.get(name);

                        if (job != null) {
                            job.fail(callbackError);
                        } else {
                            var partial = records.get(name);

                            if (partial != null && partial.status() == MazeRecord.Status.PREPARING) {
                                records.remove(name);
                            }

                            clearPlanning(name);
                            settle(name, records.get(name), callbackError);
                            plugin.getLogger().log(Level.SEVERE, "Cannot start maze operation " + name, callbackError);
                        }
                    }
                });
            });
        } catch (RejectedExecutionException rejection) {
            clearPlanning(name);
            settle(name, records.get(name), rejection);
            throw new IllegalArgumentException("Planner is busy; try again shortly.");
        }
    }

    void unreserve(String name) {
        settle(name, records.get(name), new CancellationException("Operation cancelled."));
    }

    void settle(String name, MazeRecord record, Throwable error) {
        operations.settle(name, record, error);
    }

    void clearPlanning(String name) {
        operations.clearPlanning(name);
    }

    public String progress(String name) {
        MazeJob job = jobs.get(name);

        return job == null ? (operations.reserved.contains(name) ? "PLANNING" : "IDLE")
            : job.stage + " " + job.percent() + "%";
    }

    public Location entrance(MazeRecord record) {
        return teleports.entrance(record);
    }

    public Location destination(MazeRecord record) {
        return teleports.destination(record);
    }

    public boolean safeDestination(Location location) {
        return teleports.safeDestination(location);
    }

    public void setSpawn(Player player, MazeRecord record, Location location) {
        authorize(player, record, "setspawn");

        if (record.status() != MazeRecord.Status.READY || busy(record.name())) {
            throw new IllegalArgumentException("Maze is not ready.");
        }

        if (location == null && record.teleportPoint() == null) {
            plugin.messages().send(player, "spawn-already-default", "name", record.name());

            return;
        }

        if (location != null && !safeDestination(location)) {
            throw new IllegalArgumentException("Stand on a safe solid floor with two clear blocks above it.");
        }

        var point = location == null ? null
            : new TeleportPoint(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        var updated = record.withTeleportPoint(point);
        reserve(record.name());
        save(updated).whenComplete((ignored, error) -> onMain(() -> {
            unreserve(record.name());

            if (error != null) {
                report(player, error);
            } else {
                records.put(record.name(), updated);
                plugin.messages().send(player, location == null ? "spawn-reset" : "spawn-saved", "name", record.name());
            }
        }));
    }

    public MazeRecord at(Location location) {
        return regions.at(location);
    }

    public boolean protectedAt(Location location) {
        return regions.protectedAt(location);
    }

    public boolean mobSpawnBlockedAt(Location location) {
        return plugin.settings().preventMobSpawns() && regions.mobSpawnBlockedAt(location);
    }

    Path snapshotPath(MazeRecord record) {
        return plugin.getDataFolder().toPath().resolve("snapshots").resolve(record.name() + ".schem");
    }

    void onMain(Runnable action) {
        if (!closed && plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!closed) {
                    action.run();
                }
            });
        }
    }

    <T> CompletableFuture<T> io(Supplier<T> task) {
        return workers.io(task);
    }

    CompletableFuture<Void> save(MazeRecord record) {
        return io(() -> {
            try {
                repository.save(record);

                return null;
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        });
    }

    void report(CommandSender sender, Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException)
            && error.getCause() != null) {
            error = error.getCause();
        }

        plugin.messages().send(sender, "error", "reason",
            Objects.toString(error.getMessage(), error.getClass().getSimpleName()));

        if (!(error instanceof IllegalArgumentException || error instanceof CancellationException)) {
            plugin.getLogger().log(Level.SEVERE, "Maze operation failed", error);
        }
    }

    public void close() {
        closed = true;

        if (ticker != null) {
            ticker.cancel();
        }

        for (var job : jobs.values()) {
            job.release();
        }

        operations.close(records);
        jobs.clear();
        workers.close();
    }
}
