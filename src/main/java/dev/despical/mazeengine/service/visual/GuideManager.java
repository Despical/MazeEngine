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

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.command.ChatUI;
import dev.despical.mazeengine.storage.MazeRecord;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Starts, reuses, and closes each player's private maze route guide.
 * <p>
 * A guide requires a ready maze in the player's current world. A still-valid
 * session for the same saved layout can be reused; otherwise the previous
 * session is released before a new one is stored. Chat feedback includes a
 * close control bound to the session token.
 * <p>
 * Stop requests distinguish the maze name and optional token so an expired
 * button cannot close a replacement session. Session removal releases its
 * private displays and chunk leases, and action-bar cleanup is coordinated
 * with any preview the player still has active.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class GuideManager {

    private final MazeVisuals owner;
    private final MazeEnginePlugin plugin;

    GuideManager(MazeVisuals owner) {
        this.owner = owner;
        this.plugin = owner.plugin;
    }

    public void solve(Player player, MazeRecord record) {
        if (!player.getWorld().getUID().equals(record.worldId())) {
            throw new IllegalArgumentException("Teleport to this maze before enabling the guide.");
        }

        owner.ready(record);
        GuideSession current = owner.solvers.get(player.getUniqueId());

        if (current != null && current.name.equals(record.name()) && current.layout == record.layout()
            && System.currentTimeMillis() < current.expires) {
            guideMessage(player, current, "guide-already-active");

            return;
        }

        removeSolver(player.getUniqueId());
        GuideSession solver = new GuideSession(owner, player, record);
        owner.solvers.put(player.getUniqueId(), solver);
        guideMessage(player, solver, "guide-started");
    }

    private void guideMessage(Player player, GuideSession solver, String key) {
        var messages = plugin.messages();
        messages.sendRich(player, key,
            Map.of("actions",
                new ChatUI(plugin).callbackButton(player, "button-guide-close", "hover-guide-close", "#7ABFCC",
                    plugin.settings().visuals().guideDurationSeconds(), "solve", solver.name, "stop",
                    solver.token.toString())),
            "name", solver.name);
    }

    public void stopSolver(Player player) {
        stopSolver(player, null);
    }

    public void stopSolver(Player player, String token) {
        stopSolver(player, token, null);
    }

    public void stopSolver(Player player, String token, String name) {
        var session = owner.solvers.get(player.getUniqueId());

        if (session == null || token == null && name != null && !session.name.equalsIgnoreCase(name)) {
            if (name == null) {
                plugin.messages().send(player, "guide-already-closed");
            } else {
                plugin.messages().send(player, "guide-already-closed", "name", name);
            }

            owner.clearBar(player);

            return;
        }

        if (token != null && (session == null || !session.token.toString().equals(token))) {
            throw new IllegalArgumentException("This guide has already ended.");
        }

        removeSolver(player.getUniqueId());
        owner.clearBar(player);
        plugin.messages().send(player, "guide-stopped", "name", session.name);
    }

    void removeSolver(UUID id) {
        var solver = owner.solvers.remove(id);

        if (solver != null) {
            solver.release();
        }
    }
}
