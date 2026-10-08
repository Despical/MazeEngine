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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies the persisted settings and immutability of a frozen maze preset.
 * <p>
 * The test constructs preset metadata and inspects the nested representation
 * used by saved records, including structural, generation, palette, and
 * decoration settings. Builder reuse must leave earlier presets intact, and
 * configured theme metadata must survive independently of preset identifiers.
 * <p>
 * The fixture focuses on the codec's saved-data contract rather than loading
 * live Bukkit block data or exercising a complete server configuration reload.
 * Bundled YAML checks ensure every shipped theme defines its display metadata.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class PresetCodecTest {

    @Test
    void serializedFrozenPresetRetainsTheExistingYamlContract() {
        var palette = new Palette(List.of(new Palette.Entry("minecraft:stone", 1)));
        var preset = Preset.builder("test").floor(palette).wall(palette).ceiling(palette).cap(palette)
            .capChance(0.2).lightSpacing(3).build();
        var saved = preset.serialize();
        assertEquals(
            Set.of("display-name", "description", "accent", "difficulty", "path-width", "wall-thickness", "wall-height",
                "roof", "snapshot", "placement", "complexity", "algorithm", "advanced", "palette", "decoration"),
            saved.keySet());
        assertEquals(Set.of("floor", "wall", "ceiling", "cap", "pattern", "section-size"),
            ((Map<?, ?>) saved.get("palette")).keySet());
        assertEquals(Set.of("cap-chance", "light-spacing", "light", "entrance-marker", "exit-marker", "inlay-chance"),
            ((Map<?, ?>) saved.get("decoration")).keySet());
        assertEquals(3, saved.get("path-width"));
        assertEquals("PERFECT", saved.get("algorithm"));
        assertEquals("CLEAR", saved.get("placement"));
        assertEquals(false, saved.get("snapshot"));
        assertEquals(List.of("minecraft:stone@1.0"), ((Map<?, ?>) saved.get("palette")).get("floor"));
    }

    @Test
    void rebuildingDoesNotChangeThePresetAlreadyRetainedByAMaze() {
        var builder = builder("test");
        var frozen = builder.build();
        var originalSettings = frozen.serialize();
        var changed = builder.pathWidth(7).roof(true).snapshot(true).difficulty(Preset.Difficulty.HARD).build();

        assertEquals(3, frozen.pathWidth());
        assertFalse(frozen.roof());
        assertFalse(frozen.snapshot());
        assertEquals(Preset.Difficulty.MEDIUM, frozen.difficulty());
        assertEquals(originalSettings, frozen.serialize());
        assertEquals(7, changed.pathWidth());
        assertTrue(changed.roof());
        assertTrue(changed.snapshot());
        assertNotEquals(frozen, changed);
        assertEquals(frozen, builder("test").build());
        assertEquals(frozen.hashCode(), builder("test").build().hashCode());
    }

    @Test
    void themeIdentifiersDoNotOverrideExplicitDisplayMetadataOrInferDifficulty() {
        var names = Map.of("default", "Stoneworks", "hedge", "Hedge Garden", "copper", "Copperworks");

        for (var entry : names.entrySet()) {
            var preset = builder(entry.getKey()).displayName(entry.getValue()).difficulty(Preset.Difficulty.HARD).build();

            assertEquals(entry.getValue(), preset.serialize().get("display-name"));
            assertEquals("HARD", preset.serialize().get("difficulty"));
            assertEquals(Preset.Difficulty.MEDIUM, builder(entry.getKey()).build().difficulty());
        }
    }

    @Test
    void namedConstructionStillRejectsInvalidGeometryAndDecoration() {
        assertThrows(IllegalArgumentException.class, () -> builder("test").pathWidth(0).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test").wallHeight(65).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test").capChance(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test").inlayChance(1.01).build());
        assertThrows(IllegalArgumentException.class, () -> builder("test").displayName(" ").build());
        assertThrows(IllegalArgumentException.class, () -> builder("test").accent("red").build());
        assertThrows(NullPointerException.class, () -> Preset.builder("test").build());
    }

    @Test
    void everyBundledThemeDefinesItsOwnDisplayNameAndDifficulty() throws Exception {
        try (var paths = Files.list(Path.of("src/main/resources/presets"))) {
            var presets = paths.filter(path -> path.toString().endsWith(".yml")).toList();
            assertEquals(30, presets.size());

            for (var path : presets) {
                var config = new YamlConfiguration();
                config.load(path.toFile());

                assertTrue(config.isString("display-name"), path.toString());
                assertFalse(config.getString("display-name").isBlank(), path.toString());
                assertTrue(config.isString("difficulty"), path.toString());
                assertDoesNotThrow(() -> Preset.Difficulty.valueOf(config.getString("difficulty")), path.toString());
            }
        }
    }

    private static Preset.Builder builder(String name) {
        var palette = new Palette(List.of(new Palette.Entry("minecraft:stone", 1)));
        return Preset.builder(name).floor(palette).wall(palette).ceiling(palette).cap(palette);
    }
}
