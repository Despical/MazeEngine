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

package dev.despical.mazeengine.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Verifies the argument parser's treatment of signed values and option keys.
 * <p>
 * The fixture exercises negative seeds and coordinates alongside valued options
 * and flags. It also checks that duplicate and unsupported options fail instead
 * of silently replacing an earlier value or reaching a command handler.
 * <p>
 * These checks run without a server and focus on the command-input contract,
 * leaving permission checks and actual world operations to their owning layers.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class OptionsTest {

    @Test
    void acceptsSignedSeedAndCoordinatesAndRejectsUnknownOrDuplicateOptions() {
        var options = CommandOptions.parse(new String[] { "maze", "--seed", "-42", "--snapshot", "--x", "-16" });
        options.check(Set.of("seed", "snapshot", "x"), 1, 1);

        assertEquals("-42", options.value("seed"));
        assertTrue(options.flag("snapshot"));
        assertEquals("-16", options.value("x"));
        assertThrows(IllegalArgumentException.class, () -> CommandOptions.parse(new String[] { "--seed" }));
        assertThrows(IllegalArgumentException.class,
            () -> CommandOptions.parse(new String[] { "--seed", "1", "--seed", "2" }));
        assertThrows(IllegalArgumentException.class, () -> options.check(Set.of("seed"), 1, 1));
        assertThrows(IllegalArgumentException.class,
            () -> CommandOptions.parse(new String[] { "--confirm", "true" }).check(Set.of("confirm"), 0, 0));
    }
}
