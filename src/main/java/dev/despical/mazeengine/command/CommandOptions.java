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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses positional arguments and named options for maze commands.
 * <p>
 * The parser distinguishes flags from valued options, normalizes option names,
 * and rejects duplicate keys. Signed numeric arguments remain values so negative
 * seeds and world coordinates are not mistaken for option switches.
 * <p>
 * Each handler declares its accepted option names and positional-count range
 * through the validation helper. Unknown keys or malformed argument counts fail
 * before that handler starts world work. Parsed collections belong to one
 * command invocation and are not shared as global command state.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class CommandOptions {

    private static final Set<String> FLAGS = Set.of("selection", "snapshot", "restore", "clear", "confirm", "new-seed",
        "repair");
    final List<String> positional = new ArrayList<>();
    final Map<String, String> values = new HashMap<>();

    static CommandOptions parse(String[] args) {
        var result = new CommandOptions();

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];

            if (!argument.startsWith("--")) {
                result.positional.add(argument);
                continue;
            }

            String key = argument.substring(2).toLowerCase(Locale.ROOT);
            if (result.values.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate option: --" + key);
            }

            if (FLAGS.contains(key)) {
                result.values.put(key, "true");
            } else {
                if (++index >= args.length || args[index].startsWith("--")) {
                    throw new IllegalArgumentException("Option --" + key + " requires a value.");
                }

                result.values.put(key, args[index]);
            }
        }

        return result;
    }

    void check(Set<String> allowed, int min, int max) {
        for (String key : values.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unknown option: --" + key);
            }
        }

        if (positional.size() < min || positional.size() > max) {
            throw new IllegalArgumentException("Incorrect arguments. Use /maze help.");
        }
    }

    boolean has(String key) {
        return values.containsKey(key);
    }

    boolean flag(String key) {
        return "true".equals(values.get(key));
    }

    String value(String key) {
        return values.get(key);
    }

    String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }
}
