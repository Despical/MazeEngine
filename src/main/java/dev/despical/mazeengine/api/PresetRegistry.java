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

import dev.despical.mazeengine.api.model.PresetSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Provides the preset metadata used when submitting new maze requests.
 * <p>
 * Queries require the server thread and an enabled MazeEngine provider;
 * otherwise they throw {@link IllegalStateException}. A successful reload
 * changes subsequent queries, while previously returned snapshots remain intact.
 * <p>
 * These are the current configured presets. To inspect the settings retained
 * by an existing maze, use {@link dev.despical.mazeengine.api.model.MazeSnapshot#preset()}.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public interface PresetRegistry {

    /**
     * Lists all currently configured presets in their display order.
     * <p>
     * The default preset comes first. Other presets follow in display-name order,
     * as prepared by the configuration loader.
     *
     * @return an immutable list of current preset snapshots
     */
    List<PresetSnapshot> all();

    /**
     * Looks up a configured preset by its case-insensitive identifier.
     * <p>
     * The lookup uses the identifier from configuration, not its player-facing
     * display name. An unknown identifier produces an empty result.
     *
     * @param id the configured preset identifier
     * @return the matching current preset, or empty when no preset has that identifier
     * @throws NullPointerException if {@code id} is null
     */
    Optional<PresetSnapshot> find(String id);
}
