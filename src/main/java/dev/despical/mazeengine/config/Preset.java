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

import dev.despical.mazeengine.core.GenerationSettings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Describes the complete theme and generation settings retained by a maze.
 * <p>
 * A preset combines corridor and wall dimensions, roof and snapshot flags,
 * occupied-block policy, graph-generation controls, palettes, and decoration
 * rules. Display metadata supplies the theme name, description, accent color,
 * and author-assigned difficulty shown in chat panels.
 * <p>
 * Configured presets are read through PresetCodec and copied into saved maze
 * records. Existing mazes therefore keep their original geometry and theme
 * after configuration reloads. Serialization stores those frozen settings
 * together with the saved graph for regeneration and recovery.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class Preset {

    private final String name;
    private final int pathWidth;
    private final int wallThickness;
    private final int wallHeight;
    private final boolean roof;
    private final boolean snapshot;
    private final Placement placement;
    private final GenerationSettings generation;
    private final Palette floor;
    private final Palette wall;
    private final Palette ceiling;
    private final Palette cap;
    private final double capChance;
    private final int lightSpacing;
    private final String light;
    private final String entranceMarker;
    private final String exitMarker;
    private final String displayName;
    private final String description;
    private final String accent;
    private final Difficulty difficulty;
    private final Palette wallInlay;
    private final double inlayChance;

    /**
     * Selects creation preflight rules for existing terrain.
     * <p>
     * CLEAR imposes no replaceable-material restriction. SAFE requires air above
     * the floor and replaceable floor materials; REPLACE requires every non-air
     * block to be configured as replaceable. All modes build the same final plan.
     */
    public enum Placement {

        CLEAR, SAFE, REPLACE
    }

    /**
     * Labels the intended challenge of a theme for display.
     * <p>
     * The preset author chooses EASY, MEDIUM, or HARD. The label is independent
     * of graph validation and does not promise a measured minimum route distance,
     * dead-end count, or solver difficulty.
     */
    public enum Difficulty {

        EASY, MEDIUM, HARD
    }

    private Preset(Builder builder) {
        this.name = builder.name;
        this.pathWidth = builder.pathWidth;
        this.wallThickness = builder.wallThickness;
        this.wallHeight = builder.wallHeight;
        this.roof = builder.roof;
        this.snapshot = builder.snapshot;
        this.placement = builder.placement;
        this.generation = builder.generation;
        this.floor = builder.floor;
        this.wall = builder.wall;
        this.ceiling = builder.ceiling;
        this.cap = builder.cap;
        this.capChance = builder.capChance;
        this.lightSpacing = builder.lightSpacing;
        this.light = builder.light;
        this.entranceMarker = builder.entranceMarker;
        this.exitMarker = builder.exitMarker;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.accent = builder.accent;
        this.difficulty = builder.difficulty;
        this.wallInlay = builder.wallInlay;
        this.inlayChance = builder.inlayChance;
        validate();
    }

    /**
     * Starts a named preset whose settings can be supplied without positional arguments.
     * <p>
     * Palette values must be supplied before building. Optional geometry and decoration
     * settings use the same neutral defaults as the configuration reader; theme-specific
     * display metadata and difficulty belong in the preset's YAML configuration.
     *
     * @param name the persistent preset identifier
     * @return a new independently mutable builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static String capitalized(String name) {
        return Arrays.stream(name.replace('-', '_').split("_")).filter(part -> !part.isEmpty())
            .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1)).collect(Collectors.joining("_"));
    }

    private void validate() {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(floor, "floor");
        Objects.requireNonNull(wall, "wall");
        Objects.requireNonNull(ceiling, "ceiling");
        Objects.requireNonNull(cap, "cap");
        Objects.requireNonNull(light, "light");
        Objects.requireNonNull(entranceMarker, "entranceMarker");
        Objects.requireNonNull(exitMarker, "exitMarker");

        if (pathWidth < 1 || pathWidth > 16 || wallThickness < 1 || wallThickness > 8 || wallHeight < 2
            || wallHeight > 64) {
            throw new IllegalArgumentException("path-width=1..16, wall-thickness=1..8, wall-height=2..64 required.");
        }

        if (!Double.isFinite(capChance) || capChance < 0 || capChance > 1 || lightSpacing < 0 || lightSpacing > 10000) {
            throw new IllegalArgumentException("Invalid decoration values.");
        }

        if (displayName == null || displayName.isBlank() || description == null || accent == null
            || !accent.matches("#[0-9a-fA-F]{6}") || difficulty == null) {
            throw new IllegalArgumentException("Preset display name and #RRGGBB accent are required.");
        }
        if (!Double.isFinite(inlayChance) || inlayChance < 0 || inlayChance > 1) {
            throw new IllegalArgumentException("Invalid wall inlay chance.");
        }

        if (wallInlay != null) {
            boolean lava = false;

            for (var entry : wallInlay.entries()) {
                var material = Bukkit.createBlockData(entry.block()).getMaterial();
                lava |= material == Material.LAVA;

                if (material != Material.LAVA && (!material.isSolid() || material.hasGravity() || material.isAir())) {
                    throw new IllegalArgumentException("Wall inlays require lava or stable solid blocks.");
                }
            }

            if (lava) {
                for (var entry : wall.entries()) {
                    var material = Bukkit.createBlockData(entry.block()).getMaterial();

                    if (!material.isOccluding() && material != Material.GLASS
                        && !material.name().endsWith("_STAINED_GLASS")) {
                        throw new IllegalArgumentException("Lava inlays require full solid or glass wall blocks.");
                    }
                }
            }
        }
    }

    public int height() {
        return 1 + wallHeight + (roof ? 1 : 0);
    }

    public static Preset read(String name, ConfigurationSection config) {
        return PresetCodec.read(name, config);
    }

    public Map<String, Object> serialize() {
        return PresetCodec.serialize(this);
    }

    public static Preset fromMap(String name, Map<String, Object> map) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.createSection("preset", map);
        return read(name, yaml.getConfigurationSection("preset"));
    }

    public String name() {
        return name;
    }

    public int pathWidth() {
        return pathWidth;
    }

    public int wallThickness() {
        return wallThickness;
    }

    public int wallHeight() {
        return wallHeight;
    }

    public boolean roof() {
        return roof;
    }

    public boolean snapshot() {
        return snapshot;
    }

    public Placement placement() {
        return placement;
    }

    public GenerationSettings generation() {
        return generation;
    }

    public Palette floor() {
        return floor;
    }

    public Palette wall() {
        return wall;
    }

    public Palette ceiling() {
        return ceiling;
    }

    public Palette cap() {
        return cap;
    }

    public double capChance() {
        return capChance;
    }

    public int lightSpacing() {
        return lightSpacing;
    }

    public String light() {
        return light;
    }

    public String entranceMarker() {
        return entranceMarker;
    }

    public String exitMarker() {
        return exitMarker;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String accent() {
        return accent;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public Palette wallInlay() {
        return wallInlay;
    }

    public double inlayChance() {
        return inlayChance;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof Preset preset && Objects.equals(name, preset.name) && pathWidth == preset.pathWidth
            && wallThickness == preset.wallThickness && wallHeight == preset.wallHeight && roof == preset.roof
            && snapshot == preset.snapshot && Objects.equals(placement, preset.placement)
            && Objects.equals(generation, preset.generation) && Objects.equals(floor, preset.floor)
            && Objects.equals(wall, preset.wall) && Objects.equals(ceiling, preset.ceiling)
            && Objects.equals(cap, preset.cap) && Double.compare(capChance, preset.capChance) == 0
            && lightSpacing == preset.lightSpacing && Objects.equals(light, preset.light)
            && Objects.equals(entranceMarker, preset.entranceMarker) && Objects.equals(exitMarker, preset.exitMarker)
            && Objects.equals(displayName, preset.displayName) && Objects.equals(description, preset.description)
            && Objects.equals(accent, preset.accent) && Objects.equals(difficulty, preset.difficulty)
            && Objects.equals(wallInlay, preset.wallInlay) && Double.compare(inlayChance, preset.inlayChance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pathWidth, wallThickness, wallHeight, roof, snapshot, placement, generation, floor,
            wall, ceiling, cap, capChance, lightSpacing, light, entranceMarker, exitMarker, displayName, description,
            accent, difficulty, wallInlay, inlayChance);
    }

    @Override
    public String toString() {
        return "Preset[name=" + name + ", displayName=" + displayName + ", difficulty=" + difficulty + "]";
    }

    /**
     * Collects named settings before publishing an immutable, validated preset.
     * <p>
     * Each setter changes only this builder. Building copies the current references
     * and scalar values into a new preset, so subsequent builder edits cannot change
     * settings already retained by a maze record. Palettes and generation settings
     * are themselves immutable values.
     * <p>
     * Required palettes have no implicit theme. Geometry, decoration, and display
     * defaults are generic; the builder does not infer a theme's difficulty or
     * replace an explicitly supplied display name based on its identifier.
     */
    public static final class Builder {

        private final String name;
        private int pathWidth = 3;
        private int wallThickness = 1;
        private int wallHeight = 4;
        private boolean roof = false;
        private boolean snapshot = false;
        private Placement placement = Placement.CLEAR;
        private GenerationSettings generation = GenerationSettings.fromComplexity(0.7);
        private Palette floor;
        private Palette wall;
        private Palette ceiling;
        private Palette cap;
        private double capChance = 0.15;
        private int lightSpacing = 4;
        private String light = "minecraft:sea_lantern";
        private String entranceMarker = "minecraft:emerald_block";
        private String exitMarker = "minecraft:gold_block";
        private String displayName;
        private String description = "A custom maze to explore.";
        private String accent = "#8BD5CA";
        private Difficulty difficulty = Difficulty.MEDIUM;
        private Palette wallInlay;
        private double inlayChance = 0;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
            this.displayName = capitalized(name);
        }

        public Builder pathWidth(int pathWidth) {
            this.pathWidth = pathWidth;
            return this;
        }

        public Builder wallThickness(int wallThickness) {
            this.wallThickness = wallThickness;
            return this;
        }

        public Builder wallHeight(int wallHeight) {
            this.wallHeight = wallHeight;
            return this;
        }

        public Builder roof(boolean roof) {
            this.roof = roof;
            return this;
        }

        public Builder snapshot(boolean snapshot) {
            this.snapshot = snapshot;
            return this;
        }

        public Builder placement(Placement placement) {
            this.placement = placement;
            return this;
        }

        public Builder generation(GenerationSettings generation) {
            this.generation = generation;
            return this;
        }

        public Builder floor(Palette floor) {
            this.floor = floor;
            return this;
        }

        public Builder wall(Palette wall) {
            this.wall = wall;
            return this;
        }

        public Builder ceiling(Palette ceiling) {
            this.ceiling = ceiling;
            return this;
        }

        public Builder cap(Palette cap) {
            this.cap = cap;
            return this;
        }

        public Builder capChance(double capChance) {
            this.capChance = capChance;
            return this;
        }

        public Builder lightSpacing(int lightSpacing) {
            this.lightSpacing = lightSpacing;
            return this;
        }

        public Builder light(String light) {
            this.light = light;
            return this;
        }

        public Builder entranceMarker(String entranceMarker) {
            this.entranceMarker = entranceMarker;
            return this;
        }

        public Builder exitMarker(String exitMarker) {
            this.exitMarker = exitMarker;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder accent(String accent) {
            this.accent = accent;
            return this;
        }

        public Builder difficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder wallInlay(Palette wallInlay) {
            this.wallInlay = wallInlay;
            return this;
        }

        public Builder inlayChance(double inlayChance) {
            this.inlayChance = inlayChance;
            return this;
        }

        public Preset build() {
            return new Preset(this);
        }
    }
}
