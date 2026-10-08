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

package dev.despical.mazeengine.core;

/**
 * Controls topology generation independently of Bukkit worlds and block palettes.
 * <p>
 * Branching selects how the active frontier is explored, turn bias affects
 * continuation versus turning, and horizontal bias weights X/Z movement.
 * Complexity supplies the high-level value from which standard biases can be
 * derived; it is not a measured player-difficulty score.
 * <p>
 * Every probability must be finite and within 0..1. PERFECT mode prohibits
 * braiding, while BRAIDED mode can connect dead ends according to braidChance.
 * The immutable value is safe to pass to worker-thread generation together
 * with a fixed seed and grid dimensions.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record GenerationSettings(double complexity, double branching, double turnBias, double horizontalBias, Mode mode,
    double braidChance) {

    /**
     * Selects the topology contract of the generated graph.
     * <p>
     * PERFECT retains a connected spanning tree with one route between each pair
     * of cells. BRAIDED permits added connections at dead ends, controlled by the
     * configured braid chance, and can therefore produce loops.
     */
    public enum Mode {

        PERFECT, BRAIDED
    }

    public GenerationSettings {
        for (double value : new double[] { complexity, branching, turnBias, horizontalBias, braidChance }) {
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Generation values must be between 0 and 1.");
            }
        }
        if (mode == null) {
            throw new IllegalArgumentException("Mode is required.");
        }
        if (mode == Mode.PERFECT && braidChance != 0) {
            throw new IllegalArgumentException("PERFECT mazes cannot contain braids.");
        }
    }

    public static GenerationSettings fromComplexity(double complexity) {
        return new GenerationSettings(complexity, 0.65 * (1 - complexity), 0.2 + 0.7 * complexity, 0.5, Mode.PERFECT,
            0);
    }
}
