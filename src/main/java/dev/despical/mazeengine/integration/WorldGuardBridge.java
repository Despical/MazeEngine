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

package dev.despical.mazeengine.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Checks optional WorldGuard build permissions before maze terrain is edited.
 * <p>
 * Each queried block must permit both placement and breaking for the actor.
 * Players are adapted to WorldGuard local players, and WorldGuard's own session
 * bypass is respected. Console and remote-console senders are trusted
 * administrative actors.
 * <p>
 * MazeEngine's terrain-protection bypass does not bypass WorldGuard. If the
 * integration becomes unavailable or a region denies the action, the check
 * throws and world work stops through the operation's normal failure path.
 * Checks access live region and block state on the server thread.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public final class WorldGuardBridge {

    private final WorldGuardPlugin plugin = WorldGuardPlugin.inst();
    private final RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

    public void check(CommandSender actor, Block block) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("WorldGuard is unavailable; editing has been stopped.");
        }

        // Console is trusted. MazeEngine bypass never bypasses WorldGuard.
        if (actor instanceof ConsoleCommandSender || actor instanceof RemoteConsoleCommandSender) {
            return;
        }

        var local = actor instanceof Player player ? plugin.wrapPlayer(player) : null;
        if (local != null && WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(local,
            BukkitAdapter.adapt(block.getWorld()))) {
            return;
        }

        var location = BukkitAdapter.adapt(block.getLocation());
        if (!query.testBuild(location, local, Flags.BLOCK_PLACE) || !query.testBuild(location, local, Flags.BLOCK_BREAK)) {
            throw new IllegalArgumentException("WorldGuard does not allow editing at " + block.getX() + ", " + block.getY() + ", " + block.getZ() + ".");
        }
    }
}
