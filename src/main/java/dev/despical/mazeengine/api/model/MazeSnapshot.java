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

package dev.despical.mazeengine.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Captures an immutable description of a saved maze at one point in time.
 * <p>
 * The description contains identity, world bounds, frozen preset metadata,
 * topology statistics, and the saved lifecycle state. It can be retained after
 * regeneration or deletion and read on other threads, but never updates itself.
 * <p>
 * Typical use cases:
 * <ul>
 *     <li>Displaying maze ownership, dimensions, and generation settings</li>
 *     <li>Recording the final state delivered by an operation event</li>
 *     <li>Comparing a previous description with a newly queried record</li>
 * </ul>
 * Coordinates and graph statistics describe saved metadata, not a live scan of
 * the world. A captured custom destination does not retain a Bukkit world.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class MazeSnapshot {

    /**
     * The normalized identifier of the captured maze.
     * <p>
     * The identifier can be reused after deletion. Retaining a snapshot does not
     * make it a description of a later maze created under the same name.
     */
    private final MazeId id;

    /**
     * The world name stored in the maze record.
     * <p>
     * Use {@link MazeBounds#origin()} and its world UUID for coordinate identity;
     * the name is descriptive metadata captured in the record.
     */
    private final String worldName;

    /**
     * The UUID of the player who originally created the maze.
     * <p>
     * Creation by a non-player administrative actor uses the zero UUID. This
     * value records ownership rather than the actor of the latest operation.
     */
    private final UUID owner;

    /**
     * The inclusive world block volume occupied by the maze.
     * <p>
     * The volume includes the floor, walls, passages, and any roof. Logical cell
     * coordinates must be interpreted with the saved geometry.
     */
    private final MazeBounds bounds;

    /**
     * The logical dimensions of the maze graph.
     * <p>
     * These dimensions count cells rather than blocks. Corridor width and wall
     * thickness are described by {@link #preset()}.
     */
    private final CellSize cells;

    /**
     * The deterministic seed retained by this maze.
     * <p>
     * Together with the saved generation settings, the seed describes topology
     * generation. It does not describe later player edits to world blocks.
     */
    private final long seed;

    /**
     * The frozen preset metadata retained by this maze.
     * <p>
     * Configuration reloads do not replace this captured metadata. Query the
     * preset registry separately to inspect settings for new mazes.
     */
    private final PresetSnapshot preset;

    /**
     * The lifecycle state at the time this description was captured.
     * <p>
     * The snapshot does not change as an operation progresses. DELETED is used
     * for successful removal results rather than a live registry entry.
     */
    private final Status status;

    /**
     * Whether the record indicates an original terrain snapshot was captured.
     * <p>
     * This flag does not verify that the schematic file still exists. Restore
     * operations check snapshot availability when they are submitted.
     */
    private final boolean hasSnapshot;

    /**
     * The timestamp of the maze's initial creation record.
     * <p>
     * Regeneration and repair retain this timestamp. It is distinct from the
     * completion time of a later operation.
     */
    private final Instant createdAt;

    /**
     * The saved custom arrival point, when one exists.
     * <p>
     * Absence selects the default entrance destination. A custom point can refer
     * to a different world, and capturing it does not require that world to be loaded.
     */
    private final TeleportDestination teleportDestination;

    /**
     * The zero-based cell containing the entrance portal.
     * <p>
     * This is a logical graph coordinate. Use the maze registry to resolve the
     * world-space arrival position and inward-facing yaw.
     */
    private final CellPosition entrance;

    /**
     * The zero-based cell containing the exit portal.
     * <p>
     * The generator selects a farthest boundary exit. The coordinate describes
     * the saved graph even if the corresponding world blocks have been edited.
     */
    private final CellPosition exit;

    /**
     * The shortest entrance-to-exit distance in the saved graph.
     * <p>
     * Distance is measured in cell edges, so a route containing N cells has
     * length N minus one. It is not a distance in world blocks.
     */
    private final int routeLength;

    /**
     * The number of cells with exactly one internal graph connection.
     * <p>
     * External entrance and exit portals are not additional internal edges.
     * This statistic describes topology rather than the current world terrain.
     */
    private final int deadEnds;

    /**
     * The failure explanation retained by the maze record.
     * <p>
     * An empty string means no failure explanation is recorded. Use the failure
     * event's cause when the underlying exception is needed.
     */
    private final String error;

    /**
     * Constructs a point-in-time description from saved maze metadata.
     * <p>
     * The custom destination is supplied directly, or as null when the default
     * entrance should be used. Other values describe the record at capture time;
     * this constructor does not query the world or persist a maze.
     *
     * @param id                  the normalized saved maze identifier
     * @param worldName           the saved world name
     * @param owner               the creator UUID, or the zero UUID for administrative creation
     * @param bounds              the complete world block bounds
     * @param cells               the logical grid dimensions
     * @param seed                the saved generation seed
     * @param preset              the maze's saved preset metadata
     * @param status              the captured lifecycle state
     * @param hasSnapshot         whether original terrain capture is recorded
     * @param createdAt           the initial creation timestamp
     * @param teleportDestination the custom destination, or null to use the default entrance
     * @param entrance            the entrance cell
     * @param exit                the exit cell
     * @param routeLength         the shortest route length in cell edges
     * @param deadEnds            the number of graph dead ends
     * @param error               the recorded failure text, empty when none is recorded
     */
    public MazeSnapshot(MazeId id, String worldName, UUID owner, MazeBounds bounds, CellSize cells, long seed,
        PresetSnapshot preset, Status status, boolean hasSnapshot, Instant createdAt,
        TeleportDestination teleportDestination, CellPosition entrance, CellPosition exit, int routeLength,
        int deadEnds, String error) {
        this.id = id;
        this.worldName = worldName;
        this.owner = owner;
        this.bounds = bounds;
        this.cells = cells;
        this.seed = seed;
        this.preset = preset;
        this.status = status;
        this.hasSnapshot = hasSnapshot;
        this.createdAt = createdAt;
        this.teleportDestination = teleportDestination;
        this.entrance = entrance;
        this.exit = exit;
        this.routeLength = routeLength;
        this.deadEnds = deadEnds;
        this.error = error;
    }

    /**
     * The normalized identifier of the captured maze.
     * <p>
     * The identifier can be reused after deletion. Retaining a snapshot does not
     * make it a description of a later maze created under the same name.
     *
     * @return the normalized saved maze identifier
     */
    public MazeId id() {
        return id;
    }

    /**
     * The world name stored in the maze record.
     * <p>
     * Use {@link MazeBounds#origin()} and its world UUID for coordinate identity;
     * the name is descriptive metadata captured in the record.
     *
     * @return the saved world name
     */
    public String worldName() {
        return worldName;
    }

    /**
     * The UUID of the player who originally created the maze.
     * <p>
     * Creation by a non-player administrative actor uses the zero UUID. This
     * value records ownership rather than the actor of the latest operation.
     *
     * @return the creator UUID
     */
    public UUID owner() {
        return owner;
    }

    /**
     * The inclusive world block volume occupied by the maze.
     * <p>
     * The volume includes the floor, walls, passages, and any roof. Logical cell
     * coordinates must be interpreted with the saved geometry.
     *
     * @return the complete world block bounds
     */
    public MazeBounds bounds() {
        return bounds;
    }

    /**
     * The logical dimensions of the maze graph.
     * <p>
     * These dimensions count cells rather than blocks. Corridor width and wall
     * thickness are described by {@link #preset()}.
     *
     * @return the logical grid dimensions
     */
    public CellSize cells() {
        return cells;
    }

    /**
     * The deterministic seed retained by this maze.
     * <p>
     * Together with the saved generation settings, the seed describes topology
     * generation. It does not describe later player edits to world blocks.
     *
     * @return the saved generation seed
     */
    public long seed() {
        return seed;
    }

    /**
     * The frozen preset metadata retained by this maze.
     * <p>
     * Configuration reloads do not replace this captured metadata. Query the
     * preset registry separately to inspect settings for new mazes.
     *
     * @return the maze's saved preset metadata
     */
    public PresetSnapshot preset() {
        return preset;
    }

    /**
     * The lifecycle state at the time this description was captured.
     * <p>
     * The snapshot does not change as an operation progresses. DELETED is used
     * for successful removal results rather than a live registry entry.
     *
     * @return the captured lifecycle state
     */
    public Status status() {
        return status;
    }

    /**
     * Whether the record indicates an original terrain snapshot was captured.
     * <p>
     * This flag does not verify that the schematic file still exists. Restore
     * operations check snapshot availability when they are submitted.
     *
     * @return whether original terrain capture is recorded
     */
    public boolean hasSnapshot() {
        return hasSnapshot;
    }

    /**
     * The timestamp of the maze's initial creation record.
     * <p>
     * Regeneration and repair retain this timestamp. It is distinct from the
     * completion time of a later operation.
     *
     * @return the initial creation timestamp
     */
    public Instant createdAt() {
        return createdAt;
    }

    /**
     * The saved custom arrival point, when one exists.
     * <p>
     * Absence selects the default entrance destination. A custom point can refer
     * to a different world, and capturing it does not require that world to be loaded.
     *
     * @return the custom destination, or empty when no custom point is saved
     */
    public Optional<TeleportDestination> teleportDestination() {
        return Optional.ofNullable(teleportDestination);
    }

    /**
     * The zero-based cell containing the entrance portal.
     * <p>
     * This is a logical graph coordinate. Use the maze registry to resolve the
     * world-space arrival position and inward-facing yaw.
     *
     * @return the entrance cell
     */
    public CellPosition entrance() {
        return entrance;
    }

    /**
     * The zero-based cell containing the exit portal.
     * <p>
     * The generator selects a farthest boundary exit. The coordinate describes
     * the saved graph even if the corresponding world blocks have been edited.
     *
     * @return the exit cell
     */
    public CellPosition exit() {
        return exit;
    }

    /**
     * The shortest entrance-to-exit distance in the saved graph.
     * <p>
     * Distance is measured in cell edges, so a route containing N cells has
     * length N minus one. It is not a distance in world blocks.
     *
     * @return the shortest route length in cell edges
     */
    public int routeLength() {
        return routeLength;
    }

    /**
     * The number of cells with exactly one internal graph connection.
     * <p>
     * External entrance and exit portals are not additional internal edges.
     * This statistic describes topology rather than the current world terrain.
     *
     * @return the number of graph dead ends
     */
    public int deadEnds() {
        return deadEnds;
    }

    /**
     * The failure explanation retained by the maze record.
     * <p>
     * An empty string means no failure explanation is recorded. Use the failure
     * event's cause when the underlying exception is needed.
     *
     * @return the recorded failure text, empty when none is recorded
     */
    public String error() {
        return error;
    }

    /**
     * Compares all captured values with another instance.
     * <p>
     * Equality is based on contents rather than object identity, including any
     * absent override or custom destination.
     *
     * @param other the object to compare
     * @return whether the other instance contains the same values
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MazeSnapshot that)) {
            return false;
        }

        return Objects.equals(id, that.id) && Objects.equals(worldName, that.worldName)
            && Objects.equals(owner, that.owner) && Objects.equals(bounds, that.bounds)
            && Objects.equals(cells, that.cells) && seed == that.seed && Objects.equals(preset, that.preset)
            && Objects.equals(status, that.status) && hasSnapshot == that.hasSnapshot
            && Objects.equals(createdAt, that.createdAt)
            && Objects.equals(teleportDestination, that.teleportDestination) && Objects.equals(entrance, that.entrance)
            && Objects.equals(exit, that.exit) && routeLength == that.routeLength && deadEnds == that.deadEnds
            && Objects.equals(error, that.error);
    }

    /**
     * Returns a hash code based on all captured values.
     *
     * @return the hash code corresponding to {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, worldName, owner, bounds, cells, seed, preset, status, hasSnapshot, createdAt,
            teleportDestination, entrance, exit, routeLength, deadEnds, error);
    }

    /**
     * Formats the captured values for diagnostics.
     * <p>
     * The representation is intended for logging, not as a persistence format.
     *
     * @return a description containing the captured field values
     */
    @Override
    public String toString() {
        return "MazeSnapshot[" + "id=" + id() + ", worldName=" + worldName() + ", owner=" + owner() + ", bounds="
            + bounds() + ", cells=" + cells() + ", seed=" + seed() + ", preset=" + preset() + ", status=" + status()
            + ", hasSnapshot=" + hasSnapshot() + ", createdAt=" + createdAt() + ", teleportDestination="
            + teleportDestination() + ", entrance=" + entrance() + ", exit=" + exit() + ", routeLength=" + routeLength()
            + ", deadEnds=" + deadEnds() + ", error=" + error() + "]";
    }

    /**
     * Describes the saved lifecycle of a maze record.
     * <p>
     * Busy states describe world work in progress. FAILED may retain partially
     * changed terrain for recovery; it does not mean an automatic rollback ran.
     * DELETED is exposed only in successful removal results and events.
     */
    public enum Status {

        /**
         * The topology exists and preparation is in progress.
         * <p>
         * Preflight and optional terrain capture precede generation. This state does
         * not by itself guarantee that a complete original snapshot is available.
         */
        PREPARING,

        /**
         * The planned maze blocks are being written to the world.
         * <p>
         * Creation, regeneration, and repair apply their structure in tick budgets.
         * The world may contain only part of the intended structure at this stage.
         */
        GENERATING,

        /**
         * The maze's last world-editing operation completed successfully.
         * <p>
         * This state describes the saved record. It does not verify that players
         * have left every generated block unchanged since completion.
         */
        READY,

        /**
         * Removal is restoring original terrain or clearing the maze volume.
         * <p>
         * The record remains visible while removal runs. Successful removal deletes
         * its persisted metadata and releases the identifier.
         */
        DELETING,

        /**
         * An operation was interrupted or failed and retained a recoverable record.
         * <p>
         * Blocks may already have changed. Inspect the saved error and operation
         * failure before deciding whether to regenerate, repair, or remove it.
         */
        FAILED,

        /**
         * Removal completed and the maze record has been deleted.
         * <p>
         * This value appears in removal results and completion events. Deleted
         * mazes are absent from registry queries.
         */
        DELETED
    }
}
