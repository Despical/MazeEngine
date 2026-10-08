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
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.BlockPlan;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.integration.Snapshot;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Advances one journaled world operation through bounded preparation and mutation.
 * <p>
 * The state machine saves its initial record, loads and leases chunks,
 * preflights the region, optionally captures original terrain, and waits for
 * required journals before block writes begin. Building and restoration
 * advance by indexed blocks under the shared runner's tick budget.
 * <p>
 * Completion waits for final persistence or deletion before releasing resources
 * and settling the operation. An untouched initial preparation can be
 * discarded; failure after mutation retains a FAILED record for recovery.
 * Cancellation is coordinated with in-flight writes and does not promise to
 * undo terrain already changed.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeJob {

    /**
     * Identifies a step of the internal journaled operation state machine.
     * <p>
     * Stages distinguish persistence waits, chunk loading, preflight, terrain
     * capture, block work, and final commit. They guide job advancement and progress
     * labels; public callers use the operation lifecycle state for terminal decisions.
     */
    enum Stage {

        JOURNAL, CHUNKS, PREFLIGHT, SNAPSHOT, SAVE_SNAPSHOT, WRITE_JOURNAL, BUILD, RESTORE, COMMIT, FINISHED
    }

    private final MazeService service;
    private final MazeEnginePlugin plugin;
    private final CommandSender sender;
    private final World world;
    private final boolean deleting, restoring, initial, repairing;
    private final BlockPlan plan;
    private final MazeProgress progress;
    private final Set<Chunk> tickets = new HashSet<>();
    private final Map<String, BlockData> data = new HashMap<>();

    MazeRecord record;
    Stage stage = Stage.JOURNAL;
    private CompletableFuture<?> pending;
    private CompletableFuture<Chunk> chunkFuture;
    private Snapshot snapshot;
    private long index;
    private int chunkX, chunkZ, chunkCount;
    boolean cancelled, mutated, abandoning;
    private Throwable terminalError;

    /**
     * Selects one supported world operation without independent mode flags.
     * <p>
     * Creation may capture a snapshot, regeneration replaces the saved plan, and
     * repair writes only its non-air blocks. Deletion either clears the region or
     * restores the original snapshot. A job has exactly one action throughout its
     * lifetime, preventing unsupported combinations such as repairing a deletion.
     */
    private enum Action {

        CREATE, REGENERATE, REPAIR, CLEAR, RESTORE
    }

    static MazeJob create(MazeService service, CommandSender sender, World world, MazeRecord record) {
        return new MazeJob(service, sender, world, record, Action.CREATE);
    }

    static MazeJob regenerate(MazeService service, CommandSender sender, World world, MazeRecord record) {
        return new MazeJob(service, sender, world, record, Action.REGENERATE);
    }

    static MazeJob repair(MazeService service, CommandSender sender, World world, MazeRecord record) {
        return new MazeJob(service, sender, world, record, Action.REPAIR);
    }

    static MazeJob delete(MazeService service, CommandSender sender, World world, MazeRecord record, boolean restore) {
        return new MazeJob(service, sender, world, record, restore ? Action.RESTORE : Action.CLEAR);
    }

    private MazeJob(MazeService service, CommandSender sender, World world, MazeRecord record, Action action) {
        this.service = service;
        this.plugin = service.plugin;
        this.sender = sender;
        this.world = world;
        this.record = record;
        this.deleting = action == Action.CLEAR || action == Action.RESTORE;
        this.restoring = action == Action.RESTORE;
        this.initial = action == Action.CREATE;
        this.repairing = action == Action.REPAIR;
        plan = new BlockPlan(record.layout(), record.preset(), record.seed());
        progress = new MazeProgress(plugin, sender, record.name());
        chunkX = record.bounds().x() >> 4;
        chunkZ = record.bounds().z() >> 4;
        pending = service.save(record);
    }

    boolean step() throws Exception {
        return switch (stage) {
            case JOURNAL -> stepJournal();
            case CHUNKS -> stepChunks();
            case PREFLIGHT -> stepPreflight();
            case SNAPSHOT -> stepSnapshot();
            case SAVE_SNAPSHOT -> stepSaveSnapshot();
            case WRITE_JOURNAL -> stepWriteJournal();
            case BUILD -> stepBuild();
            case RESTORE -> stepRestore();
            case COMMIT -> stepCommit();
            case FINISHED -> false;
        };
    }

    private boolean stepJournal() throws Exception {
        if (!pending.isDone()) {
            return false;
        }

        pending.join();
        stage = Stage.CHUNKS;

        return true;
    }

    private boolean stepChunks() throws Exception {
        Bounds bounds = record.bounds();

        if (chunkFuture == null) {
            chunkFuture = world.getChunkAtAsync(chunkX, chunkZ, true);
        }

        if (!chunkFuture.isDone()) {
            return false;
        }

        Chunk chunk = chunkFuture.join();
        service.holdChunk(chunk);
        tickets.add(chunk);
        chunkFuture = null;
        chunkCount++;

        if (++chunkX > bounds.maxX() >> 4) {
            chunkX = bounds.x() >> 4;
            chunkZ++;
        }

        if (chunkZ > bounds.maxZ() >> 4) {
            stage = Stage.PREFLIGHT;
        }

        return true;
    }

    private boolean stepPreflight() throws Exception {
        Bounds bounds = record.bounds();

        if (index == 0) {
            service.ensureEmptyOfPlayers(world, bounds);
        }

        Block block = block(index);
        Material material = block.getType();

        if (service.worldGuard != null) {
            service.worldGuard.check(sender, block);
        }

        if (initial && record.preset().placement() == Preset.Placement.SAFE && !material.isAir()
            && (bounds.localY(index) > 0 || !plugin.settings().replaceable().contains(material.name()))) {
            throw new IllegalArgumentException(
                "SAFE placement found an occupied block at " + block.getX() + "," + block.getY() + "," + block.getZ());
        }

        if (initial && record.preset().placement() == Preset.Placement.REPLACE && !material.isAir()
            && !plugin.settings().replaceable().contains(material.name())) {
            throw new IllegalArgumentException("REPLACE placement found a protected material: " + material);
        }

        if (++index == bounds.volume()) {
            index = 0;

            if (restoring) {
                pending = service.io(() -> {
                    try {
                        return service.bridge().read(world, bounds, service.snapshotPath(record));
                    } catch (Exception error) {
                        throw new CompletionException(error);
                    }
                });
                stage = Stage.RESTORE;
            } else if (deleting) {
                stage = Stage.BUILD;
            } else if (initial && record.preset().snapshot()) {
                snapshot = service.bridge().empty(world, bounds);
                stage = Stage.SNAPSHOT;
            } else {
                prepareBuild();
            }
        }

        return true;
    }

    private boolean stepSnapshot() throws Exception {
        Bounds bounds = record.bounds();

        snapshot.capture(index++);

        if (index == bounds.volume()) {
            index = 0;
            stage = Stage.SAVE_SNAPSHOT;
            pending = service.io(() -> {
                try {
                    snapshot.write(service.snapshotPath(record));

                    return null;
                } catch (Exception error) {
                    throw new CompletionException(error);
                }
            });
        }

        return true;
    }

    private boolean stepSaveSnapshot() throws Exception {
        if (!pending.isDone()) {
            return false;
        }

        pending.join();

        if (!record.hasSnapshot()) {
            record = record.state(record.status(), true, "");
            service.records.put(record.name(), record);
            pending = service.save(record);

            return true;
        }

        snapshot = null;
        prepareBuild();

        return true;
    }

    private boolean stepWriteJournal() throws Exception {
        if (!pending.isDone()) {
            return false;
        }

        pending.join();
        stage = Stage.BUILD;

        return true;
    }

    private boolean stepBuild() throws Exception {
        Bounds bounds = record.bounds();

        // CLEAR, SAFE and REPLACE share the same finished plan. Modes differ only in
        // preflight rules.
        String state = deleting ? "minecraft:air"
            : plan.block(bounds.localX(index), bounds.localY(index), bounds.localZ(index));
        Block block = block(index);
        BlockData target = data.computeIfAbsent(state, Bukkit::createBlockData);

        if (service.worldGuard != null) {
            service.worldGuard.check(sender, block);
        }

        if ((!repairing || !target.getMaterial().isAir()) && !block.getBlockData().equals(target)) {
            mutated = true;
            block.setBlockData(target, false);
        }

        if (++index == bounds.volume()) {
            commit();
        }

        return true;
    }

    private boolean stepRestore() throws Exception {
        Bounds bounds = record.bounds();

        if (snapshot == null) {
            if (!pending.isDone()) {
                return false;
            }

            snapshot = (Snapshot) pending.join();
        }

        if (service.worldGuard != null) {
            service.worldGuard.check(sender, block(index));
        }

        mutated = true;
        snapshot.restore(index++);

        if (index == bounds.volume()) {
            commit();
        }

        return true;
    }

    private boolean stepCommit() throws Exception {
        if (!pending.isDone()) {
            return false;
        }

        pending.join();

        if (deleting || abandoning) {
            service.records.remove(record.name());
        } else {
            service.records.put(record.name(), record);
        }

        finish(terminalError);
        Map<String, Component> actions = initial && !deleting && !abandoning && sender.hasPermission("mazeengine.use")
            && sender.hasPermission("mazeengine.delete")
                ? Map.of("trailing-actions", plugin.actions().undoButton(sender, record))
                : Map.of();

        if (!abandoning) {
            plugin.messages().sendRich(sender,
                deleting ? "deleted" : initial ? "completed" : repairing ? "repaired" : "regenerated", actions, "name",
                record.name(), "seed", Long.toString(record.seed()), "preset", record.preset().displayName(), "cells",
                record.layout().width() + "×" + record.layout().depth(), "result",
                restoring ? "Original terrain restored" : "Maze area cleared");
        }

        return true;
    }

    private Block block(long index) {
        Bounds bounds = record.bounds();

        return world.getBlockAt(bounds.x() + bounds.localX(index), bounds.y() + bounds.localY(index),
            bounds.z() + bounds.localZ(index));
    }

    private void prepareBuild() {
        record = record.state(MazeRecord.Status.GENERATING, record.hasSnapshot(), "");
        service.records.put(record.name(), record);
        pending = service.save(record);
        stage = Stage.WRITE_JOURNAL;
    }

    private void commit() {
        stage = Stage.COMMIT;

        if (deleting) {
            pending = service.io(() -> {
                try {
                    service.repository.delete(record.name());
                    Files.deleteIfExists(service.snapshotPath(record));

                    return null;
                } catch (Exception error) {
                    throw new CompletionException(error);
                }
            });
        } else {
            record = record.state(MazeRecord.Status.READY, record.hasSnapshot(), "");
            pending = service.save(record);
        }
    }

    void cancel() {
        // Wait for an in-flight atomic write before publishing the final cancellation state.
        if (pending != null && !pending.isDone()) {
            return;
        }

        if (stage == Stage.COMMIT) {
            cancelled = false;

            return;
        }

        if (!mutated && initial) {
            terminalError = new CancellationException("Operation cancelled.");
            abandon();
        } else {
            fail(new CancellationException("Cancelled. Use regenerate or delete --restore to recover."));
        }

        plugin.messages().send(sender, "cancelled", "name", record.name());
    }

    void fail(Throwable error) {
        if (stage == Stage.FINISHED) {
            return;
        }

        if (initial && !mutated && !abandoning) {
            terminalError = error;
            abandon();
            service.report(sender, error);

            return;
        }

        String reason = Objects.toString(error.getMessage(), error.getClass().getSimpleName());
        var failed = record.state(MazeRecord.Status.FAILED, record.hasSnapshot(), reason);
        service.records.put(record.name(), failed);
        record = failed;
        // Serial executor orders this after any pending journal/snapshot writes.
        service.save(failed).whenComplete((unused, saveError) -> {
            if (saveError != null) {
                plugin.getLogger().log(Level.SEVERE, "Cannot save failed maze state", saveError);
            }
        });
        finish(error);
        service.report(sender, error);
    }

    private void abandon() {
        abandoning = true;
        cancelled = false;
        stage = Stage.COMMIT;
        pending = service.io(() -> {
            try {
                service.repository.delete(record.name());
                Files.deleteIfExists(service.snapshotPath(record));

                return null;
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        });
    }

    int percent() {
        return MazeProgress.percent(stage, index, chunkCount, record.bounds());
    }

    void updateBar() {
        progress.update(record, stage, percent(), deleting, restoring, repairing);
    }

    private void finish(Throwable error) {
        if (stage == Stage.COMMIT && !abandoning) {
            progress.complete();
        }

        stage = Stage.FINISHED;
        service.jobs.remove(record.name());
        release();
        service.settle(record.name(), record, error);
    }

    void release() {
        progress.close();

        for (Chunk chunk : tickets) {
            service.releaseChunk(chunk);
        }

        tickets.clear();
    }
}
