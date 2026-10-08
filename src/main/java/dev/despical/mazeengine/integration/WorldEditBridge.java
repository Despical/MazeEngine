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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;

import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.storage.AtomicFiles;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Adapts optional WorldEdit functionality to maze selections and terrain snapshots.
 * <p>
 * Selection resolution accepts complete cuboids in a loaded world and returns
 * their inclusive block bounds. Snapshot creation uses a clipboard whose local
 * coordinates correspond to the maze volume; loaded schematics must match the
 * expected dimensions before they can be restored.
 * <p>
 * Clipboard snapshots retain full block state and block-entity data. Writes use
 * Sponge v3 schematics and temporary-file replacement, while restore suppresses
 * neighbor and validation side effects during the indexed batch. The service
 * only loads this bridge when a requested feature requires WorldEdit or FAWE.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class WorldEditBridge {

    public RegionSelection selection(Player player) throws Exception {
        var session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        var world = session.getSelectionWorld();

        if (world == null) {
            throw new IllegalArgumentException("Make a complete cuboid WorldEdit selection first.");
        }

        var region = session.getSelection(world);
        if (!(region instanceof CuboidRegion)) {
            throw new IllegalArgumentException("Only cuboid WorldEdit selections are supported.");
        }

        var bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("The selected world is not loaded.");
        }

        var min = region.getMinimumPoint();
        var max = region.getMaximumPoint();
        return new RegionSelection(bukkitWorld,
            new Bounds(min.x(), min.y(), min.z(), max.x() - min.x() + 1, max.y() - min.y() + 1, max.z() - min.z() + 1));
    }

    public Snapshot empty(World world, Bounds bounds) {
        var region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(bounds.width() - 1, bounds.height() - 1, bounds.depth() - 1));
        return new ClipboardSnapshot(world, bounds, new BlockArrayClipboard(region));
    }

    public Snapshot read(World world, Bounds bounds, Path path) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(path.toFile());

        if (format == null) {
            throw new IllegalArgumentException("Unknown snapshot format: " + path);
        }

        try (var input = Files.newInputStream(path); var reader = format.getReader(input)) {
            var clipboard = reader.read();
            var dimensions = clipboard.getDimensions();

            if (dimensions.x() != bounds.width() || dimensions.y() != bounds.height() || dimensions.z() != bounds.depth()) {
                throw new IllegalArgumentException("Snapshot dimensions do not match the maze.");
            }

            return new ClipboardSnapshot(world, bounds, clipboard);
        }
    }

    /**
     * Maps indexed maze-volume blocks to a WorldEdit clipboard.
     * <p>
     * Capture reads full world blocks into local clipboard positions; restore
     * applies their state and block-entity data with batch-safe side effects.
     * Completed clipboard contents are written as a Sponge v3 schematic through
     * temporary-file replacement on the storage executor.
     */
    private static final class ClipboardSnapshot implements Snapshot {

        private final com.sk89q.worldedit.world.World world;
        private final Bounds bounds;
        private final Clipboard clipboard;

        private ClipboardSnapshot(World world, Bounds bounds, Clipboard clipboard) {
            this.world = BukkitAdapter.adapt(world);
            this.bounds = bounds;
            this.clipboard = clipboard;
        }

        private BlockVector3 local(long index) {
            return clipboard.getRegion().getMinimumPoint().add(bounds.localX(index), bounds.localY(index), bounds.localZ(index));
        }

        private BlockVector3 global(long index) {
            return BlockVector3.at(bounds.x() + bounds.localX(index), bounds.y() + bounds.localY(index), bounds.z() + bounds.localZ(index));
        }

        @Override
        public void capture(long index) throws Exception {
            clipboard.setBlock(local(index), world.getFullBlock(global(index)));
        }

        @Override
        public void restore(long index) throws Exception {
            // Restore exact states without neighbor validation destroying multi-block fixtures
            // mid-batch.
            var effects = SideEffectSet.defaults().with(SideEffect.NEIGHBORS, SideEffect.State.OFF)
                .with(SideEffect.UPDATE, SideEffect.State.OFF).with(SideEffect.VALIDATION, SideEffect.State.OFF);
            world.setBlock(global(index), clipboard.getFullBlock(local(index)), effects);
        }

        @Override
        public void write(Path path) throws Exception {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

            try {
                try (var output = Files.newOutputStream(temporary);
                    var writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(output)) {
                    writer.write(clipboard);
                }

                try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    channel.force(true);
                }

                AtomicFiles.move(temporary, path);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
