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

import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.MazeLayout;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Future;

/**
 * Retains the specification and display queue for one player's maze preview.
 * <p>
 * The session captures the proposed name, world bounds, cell dimensions, seed,
 * and resolved preset. Planning supplies the generated layout and a queue of
 * VisualBlock values, while the visual ticker gradually creates their private
 * display entities within the current spawn budget.
 * <p>
 * The Future enables cancellation of unfinished planning. The inherited
 * session token and expiry identify valid build and close callbacks, and the
 * base class owns displays and chunk leases. Confirming a preview reuses its
 * captured specification rather than resolving a different random seed.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class PreviewSession extends DisplaySession {

    Future<?> future;
    MazeLayout layout;
    int total;
    final String name;
    final Bounds bounds;
    final int width, depth;
    final long seed;
    final Preset preset;
    final Deque<VisualBlock> pending = new ArrayDeque<>();

    PreviewSession(MazeVisuals owner, Player player, String name, Bounds bounds, long seed, Preset preset) {
        super(owner, player, player.getWorld(), owner.plugin.settings().visuals().previewDurationSeconds() * 1000L);
        this.name = name;
        this.bounds = bounds;
        this.width = Bounds.fit(bounds.width(), preset.pathWidth(), preset.wallThickness());
        this.depth = Bounds.fit(bounds.depth(), preset.pathWidth(), preset.wallThickness());
        this.seed = seed;
        this.preset = preset;
    }
}
