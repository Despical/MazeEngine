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

package dev.despical.mazeengine.api;

import dev.despical.mazeengine.api.model.CellPosition;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.model.MazeLocation;
import dev.despical.mazeengine.api.model.MazeSnapshot;
import dev.despical.mazeengine.api.model.TeleportDestination;

import java.util.List;
import java.util.Optional;

/**
 * Provides read-only access to saved maze records and their cell graphs.
 * <p>
 * All queries must be called on the server thread while MazeEngine is enabled.
 * Calling from another thread or using the provider after disable throws
 * {@link IllegalStateException}. Returned lists and snapshots are immutable;
 * retaining them does not retain live Bukkit worlds or update their contents.
 * <p>
 * The registry includes records that are busy or have failed. A creation request
 * still planning its topology has no record yet and is absent from queries.
 * Use an operation handle to track that request until its record exists.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public interface MazeRegistry {

    /**
     * Returns all records currently held by the maze service.
     * <p>
     * Records are ordered by maze name and include preparing, generating,
     * deleting, and failed mazes. Later changes are not reflected in this list.
     *
     * @return an immutable list of maze snapshots, or an empty list when none exist
     */
    List<MazeSnapshot> all();

    /**
     * Looks up a saved maze by its normalized identifier.
     * <p>
     * A missing name and a new maze still in topology planning both produce an
     * empty result. An existing busy or failed record can still be returned.
     *
     * @param id the maze identifier to look up
     * @return the current maze snapshot, or empty when no record exists
     * @throws NullPointerException if {@code id} is null
     */
    Optional<MazeSnapshot> find(MazeId id);

    /**
     * Finds a maze whose block volume contains the supplied world position.
     * <p>
     * The world UUID and all three coordinates are checked against the inclusive
     * bounds. A block above the roof or below the floor is outside the volume.
     *
     * @param position the world block position to test
     * @return the containing maze snapshot, or empty when the block is outside every maze
     * @throws NullPointerException if {@code position} is null
     */
    Optional<MazeSnapshot> at(MazeLocation position);

    /**
     * Resolves the default arrival point at the maze's entrance cell.
     * <p>
     * The destination is centered in the corridor, one block above the floor,
     * and faces inward from the entrance portal. This only resolves coordinates;
     * it does not teleport a player or check current arrival-block safety.
     *
     * @param id the maze whose entrance should be resolved
     * @return the entrance destination in the loaded maze world
     * @throws IllegalArgumentException if the maze is missing or its world is not loaded
     */
    TeleportDestination entrance(MazeId id);

    /**
     * Resolves the maze's saved custom destination or its default entrance.
     * <p>
     * A custom point may belong to a different world. The selected destination's
     * world must be loaded. This query does not move a player or validate the
     * current terrain at the returned point.
     *
     * @param id the maze whose arrival point should be resolved
     * @return the custom destination, or the entrance when no custom point is saved
     * @throws IllegalArgumentException if the maze is missing or the destination world is not loaded
     */
    TeleportDestination destination(MazeId id);

    /**
     * Finds a shortest route between two cells in the saved maze graph.
     * <p>
     * Coordinates are zero-based logical cells, not world blocks. The returned
     * route contains both endpoints; identical endpoints produce a single cell.
     * The query uses saved topology and does not inspect blocks placed by players.
     * <p>
     * The search is proportional to the maze's cell count and runs on the calling
     * server thread. Retain a returned route when displaying it repeatedly.
     *
     * @param id the maze whose graph should be searched
     * @param from the starting cell within that maze
     * @param to the target cell within that maze
     * @return an immutable ordered list from {@code from} to {@code to}
     * @throws IllegalArgumentException if the maze is missing or either cell is outside its grid
     */
    List<CellPosition> route(MazeId id, CellPosition from, CellPosition to);
}
