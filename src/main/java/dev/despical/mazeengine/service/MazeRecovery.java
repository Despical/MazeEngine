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
import dev.despical.mazeengine.storage.MazeRecord;
import dev.despical.mazeengine.storage.MazeRepository;

import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reconciles validated saved records after interrupted plugin or server execution.
 * <p>
 * PREPARING records are discarded with their snapshot files because the job
 * journal places preparation before terrain mutation. Records interrupted
 * during GENERATING or DELETING are retained as FAILED with an explanation
 * that directs administrators toward regeneration or restoration.
 * <p>
 * Recovered records are saved before the ordered collection is returned to
 * the service. The recovery path does not silently remove regions whose
 * terrain may have changed and does not claim to perform a rollback. It
 * relies on MazeRepository to reject malformed or overlapping saved data.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeRecovery {

    private MazeRecovery() {
    }

    static Map<String, MazeRecord> load(MazeEnginePlugin plugin, MazeRepository repository) throws Exception {
        Map<String, MazeRecord> records = new TreeMap<>();

        for (var record : repository.load()) {
            if (record.status() == MazeRecord.Status.PREPARING) {
                repository.delete(record.name());
                Files.deleteIfExists(
                    plugin.getDataFolder().toPath().resolve("snapshots").resolve(record.name() + ".schem"));
                plugin.getLogger().info(
                    "Discarded interrupted preparation for " + record.name() + "; world blocks were not modified.");
                continue;
            }

            if (record.status() == MazeRecord.Status.GENERATING || record.status() == MazeRecord.Status.DELETING) {
                record = record.state(MazeRecord.Status.FAILED, record.hasSnapshot(),
                    "Interrupted operation. Use regenerate or delete --restore.");
                repository.save(record);
            }

            records.put(record.name(), record);
        }

        return records;
    }
}
