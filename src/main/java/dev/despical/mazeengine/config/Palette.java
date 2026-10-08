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

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores validated block-state entries and their deterministic selection policy.
 * <p>
 * Entries pair portable block-state strings with positive finite weights.
 * Parsing resolves Bukkit block data and applies solid, gravity, and decoration
 * constraints appropriate to the palette's use. The entry list is copied and
 * limited so a preset cannot publish an empty or unbounded palette.
 * <p>
 * Random selection consumes a supplied coordinate hash rather than a mutable
 * random generator. Section patterns instead choose entries from X/Z sections.
 * Both policies can reproduce the same block plan during creation, rebuilding,
 * and repair when the saved preset and seed are retained.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record Palette(List<Entry> entries, Pattern pattern, int sectionSize) {

    /**
     * Selects how palette entries are applied to block coordinates.
     * <p>
     * RANDOM uses a supplied coordinate hash and entry weights. SECTIONS chooses
     * entries from X/Z section indices and section size, allowing a repeated theme
     * pattern without retaining a mutable random generator.
     */
    public enum Pattern {

        RANDOM, SECTIONS
    }

    /**
     * Pairs a portable block-state string with its selection weight.
     * <p>
     * Palette construction checks positive finite weights and retains a copied
     * entry list. Parsing validates the block state for the palette's role before
     * the entry is used by deterministic block-plan selection.
     */
    public record Entry(String block, double weight) {
    }

    public Palette(List<Entry> entries) {
        this(entries, Pattern.RANDOM, 8);
    }

    public Palette {
        if (pattern == null || sectionSize < 1 || sectionSize > 128) {
            throw new IllegalArgumentException("Palette section-size must be 1..128.");
        }

        entries = List.copyOf(entries);

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Palette cannot be empty.");
        }

        if (entries.size() > 64) {
            throw new IllegalArgumentException("A palette supports at most 64 entries.");
        }

        double total = 0;

        for (Entry entry : entries) {
            if (!Double.isFinite(entry.weight) || entry.weight <= 0) {
                throw new IllegalArgumentException("Palette weights must be positive.");
            }

            total += entry.weight;
        }

        if (!Double.isFinite(total)) {
            throw new IllegalArgumentException("Palette weight sum is too large.");
        }
    }

    public static Palette parse(List<String> lines, boolean solid) {
        var result = new ArrayList<Entry>();

        for (String line : lines) {
            int split = line.lastIndexOf('@');
            String state = (split < 0 ? line : line.substring(0, split)).trim();

            double weight = split < 0 ? 1 : Double.parseDouble(line.substring(split + 1).trim());
            var data = Bukkit.createBlockData(state);

            Material type = data.getMaterial();
            if (solid && (!type.isSolid() || type.hasGravity() || type.isAir())) {
                throw new IllegalArgumentException("Maze floor/wall palettes require solid, non-gravity blocks: " + state);
            }

            result.add(new Entry(data.getAsString(), weight));
        }

        return new Palette(result);
    }

    public String choose(long hash) {
        double total = 0;
        for (Entry entry : entries) {
            total += entry.weight;
        }

        double value = (hash >>> 11) * 0x1.0p-53 * total;
        for (Entry entry : entries) {
            value -= entry.weight;

            if (value < 0) {
                return entry.block;
            }
        }

        return entries.getLast().block;
    }

    public Palette patterned(Pattern pattern, int size) {
        return new Palette(entries, pattern, size);
    }

    public String choose(long hash, int x, int z) {
        if (pattern == Pattern.RANDOM) {
            return choose(hash);
        }

        return entries.get(Math.floorMod(Math.floorDiv(x, sectionSize) + Math.floorDiv(z, sectionSize), entries.size())).block();
    }

    public static Palette caps(List<String> lines) {
        Palette palette = parse(lines, false);

        for (Entry entry : palette.entries()) {
            Material material = Bukkit.createBlockData(entry.block()).getMaterial();

            if (material != Material.COBWEB && (!material.isSolid() || material.hasGravity() || material.isAir())) {
                throw new IllegalArgumentException("Wall caps require stable solid blocks or cobwebs: " + entry.block());
            }
        }
        return palette;
    }

    public List<String> serialize() {
        return entries.stream().map(entry -> entry.block + "@" + entry.weight).toList();
    }
}
