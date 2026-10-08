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

/**
 * Provides access to the public MazeEngine services.
 * <p>
 * The enabled plugin registers this interface with Bukkit's
 * {@link org.bukkit.plugin.ServicesManager}. Obtain it with
 * {@code Bukkit.getServicesManager().load(MazeEngineApi.class)} after MazeEngine
 * has enabled; Bukkit returns {@code null} when no provider is registered.
 * <p>
 * Typical use cases:
 * <ul>
 *     <li>Looking up saved mazes and their entrance-to-exit routes</li>
 *     <li>Displaying the presets available for new mazes</li>
 *     <li>Starting and tracking creation, regeneration, repair, or removal</li>
 * </ul>
 * Registry queries and operations must run on the server thread. Returned
 * snapshots are immutable and may be retained after a query completes.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public interface MazeEngineApi {

    /**
     * The version of the public API contract.
     * <p>
     * This value is independent of the plugin release version. It identifies the
     * contract exposed by these interfaces rather than a particular plugin build.
     */
    int VERSION = 1;

    /**
     * Returns the registry used to inspect saved mazes.
     * <p>
     * The registry exposes immutable descriptions, teleport destinations, and
     * routes through the saved cell graph. Its queries require the server thread.
     *
     * @return the maze query service
     */
    MazeRegistry mazes();

    /**
     * Returns the registry describing the currently configured presets.
     * <p>
     * Successful configuration reloads affect subsequent preset queries. Preset
     * metadata already captured in a saved maze remains unchanged.
     *
     * @return the current preset query service
     */
    PresetRegistry presets();

    /**
     * Returns the service used to submit world-editing operations.
     * <p>
     * Requests use the plugin's normal permission, ownership, and action-specific
     * safety checks. Each accepted request returns a handle for that operation.
     *
     * @return the maze operation service
     */
    MazeOperations operations();
}
