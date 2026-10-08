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

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Verifies strict scalar and key validation for nested configuration.
 * <p>
 * The tests reject unknown keys, incorrect value types, fractional integers,
 * and integer overflow while allowing supported nested scalar values.
 * Failures must remain explicit rather than relying on YAML getter coercion.
 * <p>
 * The fixtures use configuration values directly and do not start the plugin,
 * so they isolate validation behavior from file installation and world state.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class ValidationTest {

    @Test
    void rejectsWrongTypesFractionsOverflowAndUnknownKeys() throws Exception {
        var config = new YamlConfiguration();
        config.loadFromString("count: '4000'\n" + "fraction: 1.5\n" + "overflow: 4294967297\n" + "flag: 'true'\n"
            + "list: [stone, 7]\n" + "typo: 1\n");
        assertThrows(IllegalArgumentException.class, () -> Validation.number(config, "count", true));
        assertThrows(IllegalArgumentException.class, () -> Validation.number(config, "fraction", true));
        assertThrows(IllegalArgumentException.class, () -> Validation.number(config, "overflow", true));
        assertThrows(IllegalArgumentException.class, () -> Validation.type(config, "flag", Boolean.class));
        assertThrows(IllegalArgumentException.class, () -> Validation.strings(config, "list"));
        assertThrows(IllegalArgumentException.class,
            () -> Validation.keys(config, Set.of("count", "fraction", "overflow", "flag", "list")));
    }

    @Test
    void acceptsValidNestedScalars() throws Exception {
        var config = new YamlConfiguration();
        config.loadFromString("performance:\n  count: 3000\n  millis: 4.0\nflag: true\nlist: [stone, air]\n");
        assertDoesNotThrow(() -> Validation.number(config, "performance.count", true));
        assertDoesNotThrow(() -> Validation.number(config, "performance.millis", false));
        assertDoesNotThrow(() -> Validation.type(config, "flag", Boolean.class));
        assertDoesNotThrow(() -> Validation.strings(config, "list"));
        assertDoesNotThrow(() -> Validation.keys(config, Set.of("performance.count", "performance.millis", "flag", "list")));
    }
}
