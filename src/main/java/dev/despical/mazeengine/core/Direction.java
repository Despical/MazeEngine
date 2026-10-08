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
 * Defines the cardinal connections used by the logical maze graph.
 * <p>
 * Each direction has an X/Z step and one graph-mask bit. Adjacent cells store
 * reciprocal bits, so opening east from one cell also opens west from its
 * neighbor. The enum order supplies both the bit index and opposite lookup.
 * <p>
 * The generator, graph validation, route search, and block plan share these
 * directions. Entrance and exit portals use a side to describe an external
 * opening rather than adding another internal edge to the cell graph.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public enum Direction {

    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

    public final int dx, dz;

    Direction(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public Direction opposite() {
        return values()[(ordinal() + 2) % 4];
    }

    public int bit() {
        return 1 << ordinal();
    }
}
