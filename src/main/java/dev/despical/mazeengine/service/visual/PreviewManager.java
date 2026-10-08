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
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.core.MazeGenerator;
import dev.despical.mazeengine.core.MazeLayout;
import dev.despical.mazeengine.core.PreviewMesh;
import dev.despical.mazeengine.storage.MazeRepository;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * Plans, confirms, and closes the private maze previews owned by players.
 * <p>
 * Preview creation checks the current world, grid, region fit, and session
 * limits before submitting topology and geometry planning to the bounded
 * preview worker. A successful result is accepted only for the still-current
 * online viewer session and queued for budgeted display spawning.
 * <p>
 * Build callbacks validate the token, expiry, and completed plan before
 * submitting the retained specification to real maze creation. Closing cancels
 * pending planning and releases the session's resources. Failures and stale
 * callbacks cannot replace the player's newer preview.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class PreviewManager {

    private final MazeVisuals owner;
    private final MazeEnginePlugin plugin;

    PreviewManager(MazeVisuals owner) {
        this.owner = owner;
        this.plugin = owner.plugin;
    }

    public void preview(Player player, String name, World world, int x, int y, int z, int width, int depth, long seed,
        Preset preset) {
        name = MazeRepository.normalize(name);

        if (name.equals("stop")) {
            throw new IllegalArgumentException("Choose a preview name other than 'stop'.");
        }

        if (!world.equals(player.getWorld())) {
            throw new IllegalArgumentException("Preview must be in your current world.");
        }

        if (width < 2 || depth < 2 || (long) width * depth > plugin.settings().maxCells()) {
            throw new IllegalArgumentException("Invalid cell dimensions.");
        }

        if (!owner.previews.containsKey(player.getUniqueId())
            && owner.previews.size() >= plugin.settings().visuals().previewMaxSessions()) {
            throw new IllegalArgumentException("All preview slots are in use. Try again shortly.");
        }

        Bounds bounds = new Bounds(x, y, z, Bounds.span(width, preset.pathWidth(), preset.wallThickness()),
            preset.height(), Bounds.span(depth, preset.pathWidth(), preset.wallThickness()));
        plugin.mazes().validatePreview(name, world, bounds, width * depth);
        removePreview(player.getUniqueId());
        var session = new PreviewSession(owner, player, name, bounds, seed, preset);
        owner.previews.put(player.getUniqueId(), session);
        int maxDisplays = plugin.settings().visuals().previewMaxDisplays();

        try {
            session.future = owner.planner.submit(() -> plan(player, session, maxDisplays));
        } catch (RejectedExecutionException error) {
            removePreview(player.getUniqueId());
            throw new IllegalArgumentException("Preview planner is busy. Try again shortly.");
        }

        plugin.messages().sendRich(player, "preview-preparing",
            Map.of("actions",
                new ChatUI(plugin).callbackButton(player, "button-close", "hover-close", "#C44B5B",
                    plugin.settings().visuals().previewDurationSeconds(), "preview", "stop", session.token.toString())),
            "name", session.name);
    }

    public void build(Player player, String token) {
        PreviewSession session = owner.previews.get(player.getUniqueId());

        if (session == null || !session.token.toString().equals(token)
            || System.currentTimeMillis() >= session.expires) {
            throw new IllegalArgumentException("This preview has expired. Create a new preview.");
        }

        confirmPreview(player, session.name, true);
    }

    public boolean confirmPreview(Player player, String name, boolean plain) {
        var session = owner.previews.get(player.getUniqueId());

        if (!plain || session == null || !session.name.equalsIgnoreCase(name)) {
            return false;
        }

        if (System.currentTimeMillis() >= session.expires) {
            throw new IllegalArgumentException("This preview has expired.");
        }

        if (session.layout == null) {
            throw new IllegalArgumentException("Your preview is still being prepared.");
        }

        var bounds = session.bounds;
        plugin.mazes().create(player, session.name, session.world, bounds.x(), bounds.y(), bounds.z(), session.width,
            session.depth, session.seed, session.preset);
        removePreview(player.getUniqueId());

        return true;
    }

    public void stopPreview(Player player) {
        stopPreview(player, null);
    }

    public void stopPreview(Player player, String token) {
        var session = owner.previews.get(player.getUniqueId());

        if (session == null) {
            var messages = plugin.messages();
            owner.clearBar(player);
            Component open = messages.format("button-new-preview").color(TextColor.color(0x7ABFCC))
                .clickEvent(ClickEvent.suggestCommand("/maze preview "))
                .hoverEvent(HoverEvent.showText(messages.format("hover-new-preview")));
            messages.sendRich(player, "preview-already-closed", Map.of("action", open));

            return;
        }

        if (token != null && !session.token.toString().equals(token)) {
            throw new IllegalArgumentException("This preview has already ended.");
        }

        removePreview(player.getUniqueId());
        owner.clearBar(player);
        plugin.messages().send(player, "preview-stopped");
    }

    void removePreview(UUID id) {
        var preview = owner.previews.remove(id);

        if (preview != null) {
            if (preview.future != null) {
                preview.future.cancel(true);
                owner.planner.purge();
            }

            preview.release();
        }
    }

    private void plan(Player player, PreviewSession session, int maxDisplays) {
        try {
            var layout = new MazeGenerator().generate(session.width, session.depth, session.seed,
                session.preset.generation());
            var mesh = PreviewMesh.create(layout, session.preset, maxDisplays);
            var geometry = new PreviewGeometry(maxDisplays).create(layout, session.preset, session.seed, session.bounds,
                mesh);
            owner.onMain(() -> show(player, session, layout, geometry));
        } catch (Exception error) {
            owner.onMain(() -> failed(player, session, error));
        }
    }

    private void show(Player player, PreviewSession session, MazeLayout layout, List<VisualBlock> geometry) {
        if (owner.previews.get(player.getUniqueId()) != session || !player.isOnline()) {
            return;
        }

        session.layout = layout;
        session.pending.addAll(geometry);
        session.total = session.pending.size();
        var ui = new ChatUI(plugin);
        int duration = (int) Math.max(1, (session.expires - System.currentTimeMillis() + 999) / 1000);
        Component build = player.hasPermission("mazeengine.create") ? ui.callbackButton(player, "button-build",
            "hover-build", "#91B78A", duration, "build", session.token.toString()) : Component.empty();
        Component close = ui.callbackButton(player, "button-close", "hover-close", "#C44B5B", duration, "preview",
            "stop", session.token.toString());
        Component actions = build.equals(Component.empty()) ? close : build.append(Component.text("  ")).append(close);
        ui.preview(player, session.name, session.preset, session.width, session.depth, session.bounds, actions);
    }

    private void failed(Player player, PreviewSession session, Exception error) {
        if (owner.previews.get(player.getUniqueId()) != session) {
            return;
        }

        removePreview(player.getUniqueId());
        plugin.messages().send(player, "error", "reason", Objects.toString(error.getMessage(), "Preview failed."));
    }
}
