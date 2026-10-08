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

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.service.MazeJob.Stage;
import dev.despical.mazeengine.storage.MazeRecord;

import net.kyori.adventure.bossbar.BossBar;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Renders progress for one operation independently of its mutation state machine.
 * <p>
 * Stage, indexed block progress, and loaded chunk count determine a bounded
 * percentage estimate. Configured message templates supply stage titles,
 * with specific labels for clearing and repair. The bar uses the configured
 * color, overlay, and player-visibility flag captured when it is created.
 * <p>
 * Percentage describes the operation's work stages rather than a reliable time
 * estimate. Completion fills the bar, and closing hides it from a player
 * actor. Rendering progress does not advance the job or control its block
 * and elapsed-time budgets.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeProgress implements AutoCloseable {

    private final MazeEnginePlugin plugin;
    private final CommandSender sender;
    private final BossBar bar;

    MazeProgress(MazeEnginePlugin plugin, CommandSender sender, String name) {
        this.plugin = plugin;
        this.sender = sender;
        bar = BossBar.bossBar(plugin.messages().format("progress", "name", name, "stage", "PLANNING", "percent", "0"),
            0, plugin.settings().progress().color(), plugin.settings().progress().overlay());

        if (sender instanceof Player player && plugin.settings().progress().enabled()) {
            player.showBossBar(bar);
        }
    }

    static int percent(MazeJob.Stage stage, long index, int chunkCount, Bounds bounds) {
        double fraction = index / (double) bounds.volume();
        double value = switch (stage) {
            case JOURNAL -> 0;
            case CHUNKS -> 0.05 * chunkCount / Math.max(1,
                ((bounds.maxX() >> 4) - (bounds.x() >> 4) + 1) * ((bounds.maxZ() >> 4) - (bounds.z() >> 4) + 1));
            case PREFLIGHT -> 0.05 + 0.1 * fraction;
            case SNAPSHOT -> 0.15 + 0.25 * fraction;
            case SAVE_SNAPSHOT -> 0.4;
            case WRITE_JOURNAL -> 0.4;
            case BUILD, RESTORE -> 0.4 + 0.59 * fraction;
            case COMMIT -> 0.99;
            case FINISHED -> 1;
        };

        return (int) (100 * value);
    }

    void update(MazeRecord record, MazeJob.Stage stage, int percent, boolean deleting, boolean restoring,
        boolean repairing) {
        bar.progress(percent / 100f);
        String key = "stage-" + stage.name().toLowerCase(Locale.ROOT);
        String title = plugin.messages().values().getOrDefault(key, stage.name());

        if (deleting && !restoring && stage == Stage.BUILD) {
            title = "Clearing region";
        }

        if (repairing && stage == Stage.BUILD) {
            title = "Repairing blocks";
        }

        bar.name(plugin.messages().format("progress", "name", record.name(), "stage", title, "percent",
            Integer.toString(percent)));
    }

    void complete() {
        bar.progress(1);
    }

    @Override
    public void close() {
        if (sender instanceof Player player) {
            player.hideBossBar(bar);
        }
    }
}
