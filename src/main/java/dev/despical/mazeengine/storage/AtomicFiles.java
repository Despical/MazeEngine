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

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes saved documents through temporary files and destination replacement.
 * <p>
 * Text writes create the parent directory, write a sibling temporary file,
 * and force its contents before replacing the destination. The move requests
 * atomic replacement where supported and falls back to ordinary replacement
 * when the filesystem does not support an atomic move.
 * <p>
 * Temporary files are cleaned in a finally block. Maze records and schematic
 * writers share the replacement helper, while callers serialize disk operations
 * through their storage executor. A fallback move is not claimed to provide
 * the same atomicity guarantees as an atomic filesystem replacement.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    public static void move(Path temporary, Path destination) throws IOException {
        try {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void write(Path destination, String value) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, value);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            move(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
