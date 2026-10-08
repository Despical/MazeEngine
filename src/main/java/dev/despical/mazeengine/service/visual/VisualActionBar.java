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

package dev.despical.mazeengine.service.visual;

import dev.despical.mazeengine.config.Messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Combines preview status and route-guide status into one private action bar.
 * <p>
 * Preview text distinguishes planning, loading percentage, and fully visible
 * geometry. Guide text reports whether the viewer is outside a passage or
 * shows the remaining cell distance from cached exit routing together with
 * the session's remaining lifetime.
 * <p>
 * When both sessions are active, their components are separated within one
 * bar so separate updates do not overwrite each other. Text comes from the
 * configured message templates. This renderer reads session state and sends
 * presentation only; it does not plan routes or extend session expiry.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class VisualActionBar {

    private VisualActionBar() {
    }

    static void send(Player player, PreviewSession preview, GuideSession solver, long now, Messages messages) {
        Component bar = Component.empty();

        if (preview != null) {
            String status = preview.layout == null ? messages.values().get("preview-planning")
                : preview.pending.isEmpty() ? messages.values().get("preview-visible")
                    : messages.values().get("preview-loading").replace("<percent>",
                        Integer.toString(preview.blocks.size() * 100 / Math.max(1, preview.total)));
            bar = messages.format("preview-bar", "name", preview.name, "preset", preview.preset.displayName(), "status",
                status);
        }

        if (solver != null) {
            if (preview != null) {
                bar = bar.append(Component.text("  |  ", TextColor.color(0x586174)));
            }

            String status = solver.cell < 0 ? messages.values().get("guide-outside")
                : messages.values().get("guide-distance").replace("<steps>",
                    Integer.toString(solver.distances[solver.cell]));
            bar = bar.append(messages.format("guide-bar", "name", solver.name, "status", status, "time",
                time(solver.expires - now)));
        }

        player.sendActionBar(bar);
    }

    private static String time(long millis) {
        long seconds = Math.max(0, millis / 1000);

        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
