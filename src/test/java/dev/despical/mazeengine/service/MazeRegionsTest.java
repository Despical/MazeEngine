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

package dev.despical.mazeengine.service;

import static org.junit.jupiter.api.Assertions.*;

import dev.despical.mazeengine.core.Bounds;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

/**
 * Verifies the upward maze footprint used by mob-spawn prevention.
 * <p>
 * The fixture checks that wall tops, roofs, and higher positions within the
 * horizontal footprint are included. Adjacent block columns and positions
 * below the maze floor must remain outside the spawn-protection area.
 * <p>
 * These checks isolate coordinate boundaries from event dispatch, live world
 * lookups, and the configuration toggle applied by the maze service.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class MazeRegionsTest {

    @Test
    void spawnFootprintIncludesWallTopsAndRoofsButExcludesAdjacentColumnsAndBelowFloor() {
        var bounds = new Bounds(-16, 64, -32, 17, 6, 9);
        assertTrue(MazeRegions.aboveFootprint(new Location(null, -16, 64, -32), bounds));
        assertTrue(MazeRegions.aboveFootprint(new Location(null, 0.999, 300, -23.001), bounds));
        assertFalse(MazeRegions.aboveFootprint(new Location(null, 1, 70, -24), bounds));
        assertFalse(MazeRegions.aboveFootprint(new Location(null, 0, 70, -23), bounds));
        assertFalse(MazeRegions.aboveFootprint(new Location(null, -16.001, 70, -32), bounds));
        assertFalse(MazeRegions.aboveFootprint(new Location(null, -15, 63.999, -31), bounds));
    }
}
