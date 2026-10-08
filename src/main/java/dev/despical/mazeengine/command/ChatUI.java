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
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;

/**
 * Exposes a command-facing facade for the plugin's chat panels.
 * <p>
 * Callers request help, preset listings, maze lists, details, previews, or
 * confirmation panels without constructing their individual renderers. Shared
 * button, centering, and wrapping helpers delegate to the common panel support.
 * <p>
 * This facade formats and sends Adventure components using current messages
 * and settings. It does not perform maze generation or terrain changes itself;
 * interactive actions return through the command and service layers for their
 * normal permission, ownership, and stale-token checks.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class ChatUI extends ChatPanel {

    public ChatUI(MazeEnginePlugin plugin) {
        super(plugin);
    }

    public void help(CommandSender sender, int page) {
        new HelpPanel(plugin).help(sender, page);
    }

    public void concepts(CommandSender sender, int page) {
        new HelpPanel(plugin).concepts(sender, page);
    }

    public void presets(CommandSender sender) {
        presets(sender, 1);
    }

    public void presets(CommandSender sender, int page) {
        new PresetPanel(plugin).presets(sender, page);
    }

    public void list(CommandSender sender) {
        list(sender, 1);
    }

    public void list(CommandSender sender, int page) {
        new MazePanel(plugin).list(sender, page);
    }

    public void info(CommandSender sender, MazeRecord record) {
        new MazePanel(plugin).info(sender, record);
    }

    public void preview(CommandSender sender, String name, Preset preset, int width, int depth, Bounds bounds,
        Component actions) {
        new ConfirmationPanel(plugin).preview(sender, name, preset, width, depth, bounds, actions);
    }

    public void delete(CommandSender sender, MazeRecord record, boolean restore, Component actions) {
        new ConfirmationPanel(plugin).delete(sender, record, restore, actions);
    }
}
