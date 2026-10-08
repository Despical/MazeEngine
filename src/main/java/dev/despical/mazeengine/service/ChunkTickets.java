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

import org.bukkit.Chunk;

import java.util.HashMap;
import java.util.Map;

/**
 * Shares plugin chunk tickets between world operations and visual sessions.
 * <p>
 * The first holder adds a Bukkit plugin chunk ticket and later holders increase
 * the reference count. Releasing a holder decrements the count; the final
 * release removes the ticket so one finished session cannot unload a chunk
 * still needed by another operation.
 * <p>
 * Holders must balance acquisitions with releases during completion, failure,
 * and shutdown cleanup. The map retains live Chunk objects and is maintained
 * by the owning services on the server thread, rather than serving as a
 * cross-thread chunk cache.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class ChunkTickets {

    private final MazeEnginePlugin plugin;
    private final Map<Chunk, Integer> chunkReferences;

    ChunkTickets(MazeEnginePlugin plugin) {
        this.plugin = plugin;
        this.chunkReferences = new HashMap<>();
    }

    public void hold(Chunk chunk) {
        if (chunkReferences.merge(chunk, 1, Integer::sum) == 1) {
            chunk.addPluginChunkTicket(plugin);
        }
    }

    public void release(Chunk chunk) {
        int count = chunkReferences.getOrDefault(chunk, 0);

        if (count <= 1) {
            chunkReferences.remove(chunk);
            chunk.removePluginChunkTicket(plugin);
        } else {
            chunkReferences.put(chunk, count - 1);
        }
    }
}
