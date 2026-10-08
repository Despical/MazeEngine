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

package dev.despical.mazeengine.api.request;

import dev.despical.mazeengine.api.model.CellSize;
import dev.despical.mazeengine.api.model.MazeGeometry;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.model.MazeLocation;

import java.util.*;

/**
 * Describes a new maze and the overrides applied to its selected preset.
 * <p>
 * Required values identify the maze, target origin, logical grid, and preset.
 * Constructor overrides use direct nullable values: null means inherit the
 * preset, except for the seed, which is generated at submission when absent.
 * Both false and zero remain explicit values rather than absence markers.
 * <p>
 * Prefer {@link #builder(String, MazeLocation, CellSize)} when only a few
 * settings need overriding. Override accessors return Optional values so callers
 * can distinguish inherited settings from explicitly supplied ones.
 * <p>
 * Construction validates local values only. The operation service resolves the
 * current preset and checks permissions, world availability, and configured
 * limits when the request is submitted. Built requests are immutable.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class CreateMazeRequest {

    /**
     * The normalized identifier reserved for the new maze.
     */
    private final MazeId id;

    /**
     * The minimum block corner and floor layer of the maze.
     * <p>
     * The world must be loaded when creation is submitted. Negative world coordinates
     * are supported.
     */
    private final MazeLocation origin;

    /**
     * The logical dimensions of the generated cell graph.
     * <p>
     * Cell counts are independent of corridor widths. Server cell and block-volume
     * limits are checked when the request is submitted.
     */
    private final CellSize cells;

    /**
     * The configured preset identifier used to resolve inherited settings.
     * <p>
     * Identifiers are normalized to lowercase. The preset must exist when the
     * request is submitted; constructing a request does not query configuration.
     */
    private final String preset;

    /**
     * The explicitly supplied deterministic generation seed.
     * <p>
     * An absent seed is generated securely at submission. Every long value,
     * including zero and negative values, is a valid explicit seed.
     */
    private final Long seed;

    /**
     * The complete override for corridor and wall dimensions.
     * <p>
     * An absent value inherits all dimensions from the selected preset. Supplying
     * a value replaces path width, wall thickness, and wall height together.
     */
    private final MazeGeometry geometry;

    /**
     * The override used to derive generation biases.
     * <p>
     * A supplied value must be finite and within 0..1 inclusive. Resolving this
     * override removes the preset's advanced generation biases and derives them anew.
     */
    private final Double complexity;

    /**
     * The override controlling whether a ceiling is generated.
     * <p>
     * Both true and false are explicit overrides. An absent value inherits the
     * preset ceiling setting.
     */
    private final Boolean roof;

    /**
     * The override controlling original terrain capture.
     * <p>
     * Enabling capture requires WorldEdit or FAWE when creation is submitted.
     * An absent value inherits the preset snapshot setting.
     */
    private final Boolean snapshot;

    /**
     * The override controlling how occupied blocks are handled.
     * <p>
     * The policy is used during preflight. Palette and advanced generation
     * settings otherwise come from the selected preset.
     */
    private final Placement placement;

    /**
     * Constructs a request using direct values for all settings.
     * <p>
     * Null overrides inherit from the selected preset; a null seed requests a
     * generated seed. Preset identifiers are normalized to lowercase. Palette
     * settings are resolved from configuration when creation is submitted.
     *
     * @param id         the unique maze identifier
     * @param origin     the minimum corner in the target world
     * @param cells      the grid dimensions in cells
     * @param preset     the case-insensitive preset identifier
     * @param seed       the explicit seed, or null to generate one at submission
     * @param geometry   the geometry override, or null to inherit the preset
     * @param complexity the complexity override, or null to inherit the preset
     * @param roof       the ceiling override, or null to inherit the preset
     * @param snapshot   the terrain capture override, or null to inherit the preset
     * @param placement  the occupied-block policy, or null to inherit the preset
     * @throws NullPointerException     if a required field or the preset is null
     * @throws IllegalArgumentException if the preset identifier is invalid or the
     *                                  supplied complexity is non-finite or outside 0..1
     */
    public CreateMazeRequest(MazeId id, MazeLocation origin, CellSize cells, String preset, Long seed,
        MazeGeometry geometry, Double complexity, Boolean roof, Boolean snapshot, Placement placement) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(cells, "cells");

        preset = Objects.requireNonNull(preset, "preset").toLowerCase(Locale.ROOT);

        if (!preset.matches("[a-z0-9_-]{1,48}")) {
            throw new IllegalArgumentException("Invalid preset identifier.");
        }

        if (complexity != null && (!Double.isFinite(complexity) || complexity < 0 || complexity > 1)) {
            throw new IllegalArgumentException("Complexity must be 0..1.");
        }

        this.id = id;
        this.origin = origin;
        this.cells = cells;
        this.preset = preset;
        this.seed = seed;
        this.geometry = geometry;
        this.complexity = complexity;
        this.roof = roof;
        this.snapshot = snapshot;
        this.placement = placement;
    }

    /**
     * The normalized identifier reserved for the new maze.
     *
     * @return the unique maze identifier
     */
    public MazeId id() {
        return id;
    }

    /**
     * The minimum block corner and floor layer of the maze.
     * <p>
     * The world must be loaded when creation is submitted. Negative world coordinates
     * are supported.
     *
     * @return the minimum corner in the target world
     */
    public MazeLocation origin() {
        return origin;
    }

    /**
     * The logical dimensions of the generated cell graph.
     * <p>
     * Cell counts are independent of corridor widths. Server cell and block-volume
     * limits are checked when the request is submitted.
     *
     * @return the grid dimensions in cells
     */
    public CellSize cells() {
        return cells;
    }

    /**
     * The configured preset identifier used to resolve inherited settings.
     * <p>
     * Identifiers are normalized to lowercase. The preset must exist when the
     * request is submitted; constructing a request does not query configuration.
     *
     * @return the case-insensitive preset identifier
     */
    public String preset() {
        return preset;
    }

    /**
     * The explicitly supplied deterministic generation seed.
     * <p>
     * An absent seed is generated securely at submission. Every long value,
     * including zero and negative values, is a valid explicit seed.
     *
     * @return the explicit seed, or empty when no override is supplied
     */
    public OptionalLong seed() {
        return seed == null ? OptionalLong.empty() : OptionalLong.of(seed);
    }

    /**
     * The complete override for corridor and wall dimensions.
     * <p>
     * An absent value inherits all dimensions from the selected preset. Supplying
     * a value replaces path width, wall thickness, and wall height together.
     *
     * @return the geometry override, or empty when no override is supplied
     */
    public Optional<MazeGeometry> geometry() {
        return Optional.ofNullable(geometry);
    }

    /**
     * The override used to derive generation biases.
     * <p>
     * A supplied value must be finite and within 0..1 inclusive. Resolving this
     * override removes the preset's advanced generation biases and derives them anew.
     *
     * @return the complexity override, or empty when no override is supplied
     */
    public OptionalDouble complexity() {
        return complexity == null ? OptionalDouble.empty() : OptionalDouble.of(complexity);
    }

    /**
     * The override controlling whether a ceiling is generated.
     * <p>
     * Both true and false are explicit overrides. An absent value inherits the
     * preset ceiling setting.
     *
     * @return the ceiling override, or empty when no override is supplied
     */
    public Optional<Boolean> roof() {
        return Optional.ofNullable(roof);
    }

    /**
     * The override controlling original terrain capture.
     * <p>
     * Enabling capture requires WorldEdit or FAWE when creation is submitted.
     * An absent value inherits the preset snapshot setting.
     *
     * @return the terrain capture override, or empty when no override is supplied
     */
    public Optional<Boolean> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    /**
     * The override controlling how occupied blocks are handled.
     * <p>
     * The policy is used during preflight. Palette and advanced generation
     * settings otherwise come from the selected preset.
     *
     * @return the occupied-block policy, or empty when no override is supplied
     */
    public Optional<Placement> placement() {
        return Optional.ofNullable(placement);
    }

    /**
     * Compares all captured values with another instance.
     * <p>
     * Equality is based on contents rather than object identity, including any
     * absent override or custom destination.
     *
     * @param other the object to compare
     * @return whether the other instance contains the same values
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CreateMazeRequest that)) {
            return false;
        }

        return Objects.equals(id, that.id) && Objects.equals(origin, that.origin) && Objects.equals(cells, that.cells)
            && Objects.equals(preset, that.preset) && Objects.equals(seed, that.seed)
            && Objects.equals(geometry, that.geometry) && Objects.equals(complexity, that.complexity)
            && Objects.equals(roof, that.roof) && Objects.equals(snapshot, that.snapshot)
            && Objects.equals(placement, that.placement);
    }

    /**
     * Returns a hash code based on all captured values.
     *
     * @return the hash code corresponding to {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, origin, cells, preset, seed, geometry, complexity, roof, snapshot, placement);
    }

    /**
     * Formats the captured values for diagnostics.
     * <p>
     * The representation is intended for logging, not as a persistence format.
     *
     * @return a description containing the captured field values
     */
    @Override
    public String toString() {
        return "CreateMazeRequest[" + "id=" + id() + ", origin=" + origin() + ", cells=" + cells() + ", preset="
            + preset() + ", seed=" + seed() + ", geometry=" + geometry() + ", complexity=" + complexity() + ", roof="
            + roof() + ", snapshot=" + snapshot() + ", placement=" + placement() + "]";
    }

    /**
     * Determines which existing blocks are accepted by creation preflight.
     * <p>
     * These policies affect acceptance of the target volume. Once accepted, all
     * policies build the structure selected by the resolved preset.
     */
    public enum Placement {

        /**
         * Allows the bounded volume to be cleared and rebuilt.
         * <p>
         * Occupied-block preflight does not restrict existing materials. Region,
         * player-safety, and configured size checks still apply.
         */
        CLEAR,

        /**
         * Requires air above the floor and replaceable material on the floor.
         * <p>
         * A non-air block above the floor rejects creation. Non-air floor blocks
         * must appear in the server's configured replaceable-material list.
         */
        SAFE,

        /**
         * Allows replacement of air and configured replaceable materials.
         * <p>
         * Every non-air block in the volume must appear in the server's
         * replaceable-material list, including blocks above the floor.
         */
        REPLACE
    }

    /**
     * Creates a builder with the default preset and no overrides.
     * <p>
     * The builder accepts direct values for overrides. No Optional wrappers are
     * needed; settings not supplied to the builder inherit from the preset.
     *
     * @param name   the case-insensitive maze name
     * @param origin the minimum block corner and floor layer
     * @param cells  the logical grid dimensions
     * @return a new reusable request builder
     * @throws IllegalArgumentException if the maze name is invalid or reserved
     * @throws NullPointerException     if any required argument is null
     */
    public static Builder builder(String name, MazeLocation origin, CellSize cells) {
        return new Builder(new MazeId(name), origin, cells);
    }

    /**
     * Assembles immutable creation requests using direct override values.
     * <p>
     * Each call updates this builder and returns it for chaining. A builder can
     * be reused, but it is mutable and must not be shared between threads.
     * Requests already built are independent of subsequent builder changes.
     * <p>
     * For example:
     * <pre>{@code
     * CreateMazeRequest request = CreateMazeRequest.builder("arena", origin, cells)
     *         .preset("default")
     *         .seed(742L)
     *         .roof(false)
     *         .build();
     * }</pre>
     */
    public static final class Builder {

        private String preset = "default";
        private Long seed;
        private MazeGeometry geometry;
        private Double complexity;
        private Boolean roof;
        private Boolean snapshot;
        private Placement placement;
        private final MazeId id;
        private final MazeLocation origin;
        private final CellSize cells;

        private Builder(MazeId id, MazeLocation origin, CellSize cells) {
            this.id = Objects.requireNonNull(id, "id");
            this.origin = Objects.requireNonNull(origin, "origin");
            this.cells = Objects.requireNonNull(cells, "cells");
        }

        /**
         * Selects the configured preset used to resolve the request.
         * <p>
         * The identifier is normalized and validated when {@link #build()} is
         * called. Its existence is checked when creation is submitted.
         *
         * @param value the configured preset identifier
         * @return this builder using the supplied preset
         */
        public Builder preset(String value) {
            preset = value;
            return this;
        }

        /**
         * Uses an explicit deterministic generation seed.
         * <p>
         * Zero and negative seeds are valid. Omit this call to generate a seed
         * when the request is submitted.
         *
         * @param value the seed to use for topology generation
         * @return this builder with the explicit seed
         */
        public Builder seed(long value) {
            seed = value;
            return this;
        }

        /**
         * Overrides all structural dimensions together.
         * <p>
         * The geometry controls corridor width, wall thickness, and wall height.
         * Server volume limits are still checked at submission.
         *
         * @param value the complete geometry override
         * @return this builder with the supplied geometry
         * @throws NullPointerException if {@code value} is null
         */
        public Builder geometry(MazeGeometry value) {
            geometry = Objects.requireNonNull(value, "value");
            return this;
        }

        /**
         * Overrides complexity and derives the generation biases from it.
         * <p>
         * The value must be finite and within 0..1 inclusive. Validation occurs
         * when {@link #build()} constructs the request.
         *
         * @param value the requested complexity in 0..1
         * @return this builder with the supplied complexity
         */
        public Builder complexity(double value) {
            complexity = value;
            return this;
        }

        /**
         * Overrides the preset's ceiling setting.
         * <p>
         * Passing false explicitly disables the roof even if the preset enables
         * it. Omitting this call inherits the preset setting.
         *
         * @param value whether the maze should have a ceiling
         * @return this builder with the explicit ceiling setting
         */
        public Builder roof(boolean value) {
            roof = value;
            return this;
        }

        /**
         * Overrides the preset's original terrain capture setting.
         * <p>
         * Passing true requires WorldEdit or FAWE at submission. Passing false
         * explicitly disables capture rather than inheriting the preset.
         *
         * @param value whether original terrain should be captured
         * @return this builder with the explicit capture setting
         */
        public Builder snapshot(boolean value) {
            snapshot = value;
            return this;
        }

        /**
         * Overrides the occupied-block preflight policy.
         * <p>
         * The selected policy controls which existing blocks are accepted before
         * world work starts. It does not change the finished maze structure.
         *
         * @param value the occupied-block handling policy
         * @return this builder with the supplied policy
         * @throws NullPointerException if {@code value} is null
         */
        public Builder placement(Placement value) {
            placement = Objects.requireNonNull(value, "value");
            return this;
        }

        /**
         * Constructs an independent immutable request from the current values.
         * <p>
         * This validates the identifier, required fields, and optional complexity.
         * World availability, permissions, presets, and configured server limits
         * are checked when the request is submitted to the operation service.
         *
         * @return a new creation request containing the builder's current values
         * @throws NullPointerException     if the preset identifier is null
         * @throws IllegalArgumentException if the preset identifier or complexity is invalid
         */
        public CreateMazeRequest build() {
            return new CreateMazeRequest(id, origin, cells, preset, seed, geometry, complexity, roof, snapshot,
                placement);
        }
    }
}
