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

/**
 * Captures public metadata for a configured or saved maze preset.
 * <p>
 * The preset registry returns current configuration. A maze snapshot contains
 * the frozen metadata saved with that maze, so later configuration reloads do
 * not change its generation settings or description.
 * <p>
 * This value exposes display metadata, structural dimensions, and high-level
 * generation settings. It does not expose mutable Bukkit objects, block
 * palettes, decoration rules, or internal advanced generation parameters.
 * It is suitable for menus, descriptions, and comparison of saved settings.
 *
 * @param id the configured preset identifier
 * @param displayName the player-facing theme name
 * @param description the theme description
 * @param accent the theme color in {@code #RRGGBB} form
 * @param difficulty the author-assigned challenge label
 * @param geometry the structural dimensions in blocks
 * @param roof whether the preset generates a ceiling
 * @param snapshotRequested whether creation should capture original terrain
 * @param algorithm the graph topology mode
 * @param complexity the generation bias in 0..1
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record PresetSnapshot(
        String id,
        String displayName,
        String description,
        String accent,
        Difficulty difficulty,
        MazeGeometry geometry,
        boolean roof,
        boolean snapshotRequested,
        Algorithm algorithm,
        double complexity
) {

    /**
     * Provides the challenge label assigned by the preset author.
     * <p>
     * Difficulty is display metadata. It is not computed from route length,
     * dead ends, or a solver's measured effort.
     */
    public enum Difficulty {

        /**
         * Labels the preset as an easy challenge.
         * <p>
         * The label is selected by the preset author and does not enforce particular
         * grid dimensions or generation biases.
         */
        EASY,

        /**
         * Labels the preset as a moderate challenge.
         * <p>
         * Use the geometry and generation metadata alongside this label when
         * presenting the preset to players.
         */
        MEDIUM,

        /**
         * Labels the preset as a hard challenge.
         * <p>
         * This label is descriptive; the API does not verify a minimum route length
         * or number of dead ends.
         */
        HARD
    }

    /**
     * Describes how the generator connects the logical cell graph.
     * <p>
     * Both modes produce connected mazes. The distinction is whether the graph
     * retains a unique route between every pair of cells or may contain loops.
     */
    public enum Algorithm {

        /**
         * Generates a connected graph without loops.
         * <p>
         * Every pair of cells has exactly one route between them. This is the tree
         * topology commonly called a perfect maze.
         */
        PERFECT,

        /**
         * Allows extra connections to braid dead ends and introduce loops.
         * <p>
         * A connected graph is retained, but multiple routes between cells may exist.
         * The exact number of loops depends on the generation settings.
         */
        BRAIDED
    }
}
