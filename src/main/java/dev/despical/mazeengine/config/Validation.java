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

package dev.despical.mazeengine.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;

/**
 * Checks configuration structure and primitive value types before publication.
 * <p>
 * The helpers reject unknown leaf keys, unexpected scalar types, non-finite
 * numbers, fractional integer values, and malformed string lists. Nested
 * configuration sections are traversed so a misspelled advanced option cannot
 * silently fall back to a default.
 * <p>
 * These checks complement the range and domain checks in the individual
 * settings loaders. Optional missing scalars may use their loader's defaults;
 * present invalid values fail explicitly instead of being coerced into a
 * different setting.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
final class Validation {

    private Validation() {
    }

    static void keys(ConfigurationSection section, Set<String> allowed) {
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }

            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unknown configuration key: " + key);
            }
        }
    }

    static void number(ConfigurationSection section, String key, boolean integer) {
        if (!section.contains(key)) {
            return;
        }

        Object value = section.get(key);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new IllegalArgumentException(key + " must be a finite number.");
        }

        if (integer && (number.doubleValue() != Math.rint(number.doubleValue()) || number.doubleValue() < Integer.MIN_VALUE
            || number.doubleValue() > Integer.MAX_VALUE)
        ) {
            throw new IllegalArgumentException(key + " must be an integer.");
        }
    }

    static void type(ConfigurationSection section, String key, Class<?> type) {
        if (section.contains(key) && !type.isInstance(section.get(key))) {
            throw new IllegalArgumentException(key + " must be " + type.getSimpleName() + ".");
        }
    }

    static void strings(ConfigurationSection section, String key) {
        if (!(section.get(key) instanceof List<?> list) || list.stream().anyMatch(value -> !(value instanceof String))) {
            throw new IllegalArgumentException(key + " must be a list of block strings.");
        }
    }
}
