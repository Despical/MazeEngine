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

import java.nio.file.Path;

/**
 * Defines indexed capture, restoration, and persistence of original terrain.
 * <p>
 * Capture and restore operate on the same bounded volume and use its linear
 * block index. Implementations retain complete block state, including block
 * entity data, so deletion can restore the terrain recorded before generation.
 * <p>
 * Live world capture and restoration run on the server thread in operation tick
 * budgets. Writing the completed snapshot runs on the serial storage executor.
 * The operation owner waits for persistence before marking the snapshot usable
 * or starting the corresponding world mutation.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public interface Snapshot {

    /**
     * Copies the original state of one world block into this snapshot.
     * <p>
     * The index uses the bounded volume's X-first, then Z, then Y traversal.
     * Implementations retain block state and supported block-entity data at the
     * corresponding position. Call this method on the server thread before the
     * operation replaces that block; capture does not mutate the world.
     * <p>
     * The operation owner divides capture across its tick budget and waits until
     * the full volume has been captured before persisting the snapshot.
     *
     * @param index the zero-based block index within the snapshot volume
     * @throws Exception if the original block state cannot be captured
     */
    void capture(long index) throws Exception;

    /**
     * Writes one captured block back to its original world position.
     * <p>
     * The index follows the same traversal used during capture. Restoration
     * replaces the live block and restores the block-entity data supported by
     * the implementation. The caller must run on the server thread and perform
     * permission, region, and player-safety checks before beginning restoration.
     * <p>
     * Restoring one index does not complete or commit the surrounding operation.
     * The operation owner advances the remaining indices under its tick budget
     * and records the terminal result after all required writes succeed.
     *
     * @param index the zero-based block index within the snapshot volume
     * @throws Exception if the captured state cannot be restored to the world
     */
    void restore(long index) throws Exception;

    /**
     * Persists the completed snapshot for a later terrain restoration.
     * <p>
     * All capture calls must finish before writing begins. This method consumes
     * the retained snapshot data without reading live world blocks, allowing the
     * operation owner to invoke it on the serial storage executor. Concurrent
     * capture, restoration, or writes against the same snapshot are unsupported.
     * <p>
     * Returning normally means the implementation completed its file write.
     * The caller still owns the operation journal and marks the snapshot usable
     * only after persistence succeeds.
     *
     * @param path the destination snapshot file
     * @throws Exception if the snapshot cannot be encoded or written
     */
    void write(Path path) throws Exception;
}
