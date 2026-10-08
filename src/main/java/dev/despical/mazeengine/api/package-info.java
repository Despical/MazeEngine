/**
 * Public contracts for integrating other Bukkit plugins with MazeEngine.
 * <p>
 * Depend on the API JAR with compile-only scope and declare {@code MazeEngine}
 * as a plugin dependency. Do not shade these classes: integrations and the
 * provider must share the same API class identities for Bukkit service lookup.
 * Obtain {@link dev.despical.mazeengine.api.MazeEngineApi} from Bukkit's
 * {@link org.bukkit.plugin.ServicesManager} after MazeEngine has enabled.
 * <p>
 * The API groups its responsibilities into:
 * <ul>
 *     <li>{@link dev.despical.mazeengine.api.MazeRegistry}: saved records and cell routes</li>
 *     <li>{@link dev.despical.mazeengine.api.PresetRegistry}: current preset metadata</li>
 *     <li>{@link dev.despical.mazeengine.api.MazeOperations}: authorized world operations</li>
 * </ul>
 * Registry queries, submission, progress, and cancellation require the server
 * thread. Immutable snapshots and completion stages may be retained on other
 * threads. Never block the server thread waiting for completion; schedule Bukkit
 * work explicitly when a callback may execute on another thread.
 */
package dev.despical.mazeengine.api;
