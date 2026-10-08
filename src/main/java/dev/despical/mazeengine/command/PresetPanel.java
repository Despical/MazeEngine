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

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.config.Preset;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Renders available maze presets with aligned theme and geometry information.
 * <p>
 * Rows show the preset name, challenge label, structural dimensions, and
 * description using its configured accent color. Shared pixel-width helpers
 * keep the columns readable in Minecraft chat while pagination bounds each
 * page's output.
 * <p>
 * Preview actions are offered only where the sender can use them and suggest
 * the appropriate command arguments. The panel describes current configured
 * presets; settings retained by an existing maze are displayed through that
 * maze's own frozen record.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class PresetPanel extends ChatPanel {

    PresetPanel(MazeEnginePlugin plugin) {
        super(plugin);
    }

    public void presets(CommandSender sender, int page) {
        var presets = plugin.settings().presets().values().stream().toList();
        int pages = Math.max(1, (presets.size() + 9) / 10);
        checkPage(page, pages);

        var messages = plugin.messages();
        header(sender, "presets", "count", Integer.toString(presets.size()), "page", Integer.toString(page));

        Component[] headings = {messages.format("column-theme"), messages.format("column-difficulty"), messages.format("column-paths"),
            messages.format("column-height")};
        float[] anchors = presetAnchors(headings);

        sender.sendMessage(messages.rich("presets-columns",
            Map.of("columns", presetColumns(Component.empty(), anchors, headings), "theme-space",
                templatePadding(headings[0], 88), "difficulty-column", templateColumn(headings[1], 64), "paths-space",
                templatePadding(headings[2], 58)),
            "theme", messages.values().get("column-theme"), "difficulty", messages.values().get("column-difficulty"), "paths",
            messages.values().get("column-paths"), "height", messages.values().get("column-height")));

        for (Preset preset : presets.subList(Math.min((page - 1) * 10, presets.size()), Math.min(page * 10, presets.size()))) {
            Component difficulty = difficulty(preset);

            String paths = preset.pathWidth() + (preset.pathWidth() == 1 ? " block" : " blocks");
            String height = preset.wallHeight() + (preset.wallHeight() == 1 ? " block" : " blocks");

            Component name = Component.text(preset.displayName(), TextColor.fromHexString(preset.accent()));
            Component pathValue = Component.text(paths, TextColor.fromHexString("#DCE3EE"));
            Component hover = presetHover(preset);

            Component row = messages.rich("preset-row",
                    Map.of("columns", presetColumns(messages.format("preset-row-prefix"), anchors, name, difficulty, pathValue,
                            Component.text(height, TextColor.fromHexString("#DCE3EE"))),
                        "name", name, "name-space", templatePadding(name, 88), "difficulty-column",
                        templateColumn(difficulty, 64), "paths-space", templatePadding(pathValue, 58)),
                    "paths", paths, "width", Integer.toString(preset.pathWidth()), "thickness",
                    Integer.toString(preset.wallThickness()), "height", height)
                .hoverEvent(HoverEvent.showText(hover));

            if (sender instanceof Player && sender.hasPermission("mazeengine.preview")) {
                row = row.clickEvent(ClickEvent.runCommand(
                    "/maze preview preview_" + preset.name() + " --preset " + Preset.capitalized(preset.name())));
            }

            sender.sendMessage(row);
        }

        hint(sender, "presets-footer");
        navigation(sender, "/maze presets ", page, pages);
    }

    private static float[] presetAnchors(Component[] headings) {
        int[] starts = {20, 112, 188, 252}, widths = {88, 72, 64, 64};
        float[] anchors = new float[4];

        int cursor = 0;

        for (int index = 0; index < headings.length; index++) {
            int width = pixelWidth(PlainTextComponentSerializer.plainText().serialize(headings[index]), false);
            float target = starts[index] + (index == 0 ? 0 : (widths[index] - width) / 2f);

            cursor += Math.max(0, Math.round((target - cursor) / 4f)) * 4;
            anchors[index] = cursor + (index == 0 ? 0 : width / 2f);
            cursor += width;
        }
        return anchors;
    }

    private static Component presetColumns(Component prefix, float[] anchors, Component... values) {
        Component result = prefix;
        int cursor = pixelWidth(PlainTextComponentSerializer.plainText().serialize(prefix), false);

        for (int index = 0; index < values.length; index++) {
            int width = pixelWidth(PlainTextComponentSerializer.plainText().serialize(values[index]), false);
            float target = anchors[index] - (index == 0 ? 0 : width / 2f);
            int spaces = Math.max(0, Math.round((target - cursor) / 4f));

            result = result.append(Component.text(" ".repeat(spaces))).append(values[index]);
            cursor += spaces * 4 + width;
        }
        return result;
    }

    // Retain placeholders used by existing customized table templates.
    private static Component templatePadding(Component value, int width) {
        int pixels = pixelWidth(PlainTextComponentSerializer.plainText().serialize(value), false);
        return Component.text(" ".repeat(Math.max(1, Math.round((width - pixels) / 4f))));
    }

    private static Component templateColumn(Component value, int width) {
        int pixels = pixelWidth(PlainTextComponentSerializer.plainText().serialize(value), false);
        int spaces = Math.max(0, Math.round((width - pixels) / 4f));
        int left = (spaces + 1) / 2;
        return Component.text(" ".repeat(left)).append(value).append(Component.text(" ".repeat(spaces - left)));
    }
}
