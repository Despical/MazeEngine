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

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifies a saved maze using the same naming rules as commands.
 * <p>
 * Input is normalized to lowercase with {@link java.util.Locale#ROOT}.
 * Identifiers contain 1 through 48 ASCII letters, digits, underscores, or
 * hyphens. Names reserved by the filesystem, such as {@code con} and
 * {@code lpt1}, are rejected so persistence can use the identifier safely.
 * <p>
 * Equality compares normalized values. The identifier is a name, not a unique
 * operation identity; a deleted name may later identify a newly created maze.
 *
 * @param value the case-insensitive maze name, stored in normalized form
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record MazeId(String value) {

    /**
     * Normalizes and validates a maze name.
     * <p>
     * Surrounding whitespace is not trimmed. Reserved device names are rejected
     * after normalization, including COM1..COM9 and LPT1..LPT9.
     *
     * @param value the maze name to normalize
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if the normalized name is invalid or reserved
     */
    public MazeId {
        value = Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT);

        if (!value.matches("[a-z0-9_-]{1,48}")) {
            throw new IllegalArgumentException("Invalid maze name.");
        }

        if (value.matches("con|prn|aux|nul|com[1-9]|lpt[1-9]")) {
            throw new IllegalArgumentException("This maze name is reserved by the filesystem.");
        }
    }

    /**
     * Returns the normalized identifier itself.
     * <p>
     * This representation can be used wherever the plugin expects a maze name.
     * It does not include a record-style field label or class name.
     *
     * @return the lowercase saved maze name
     */
    @NotNull
    @Override
    public String toString() {
        return value;
    }
}
