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

package dev.despical.mazeengine.service.api;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.api.MazeRegistry;
import dev.despical.mazeengine.api.model.CellPosition;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.model.MazeLocation;
import dev.despical.mazeengine.api.model.MazeSnapshot;
import dev.despical.mazeengine.api.model.TeleportDestination;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapts internal saved records and routes to the public read-only registry.
 * <p>
 * Queries check provider availability and return copied public descriptions
 * instead of exposing mutable service collections or live Bukkit Locations.
 * Lookups use normalized identifiers or complete world block bounds, and
 * destination queries convert the service's resolved arrival point.
 * <p>
 * Routing validates logical cell endpoints against the selected saved layout
 * and maps its shortest-route indices to public CellPosition values. The
 * adapter runs on the server thread; returned lists and snapshots can be
 * retained without making them update with later maze changes.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeApiRegistry implements MazeRegistry {

    private final MazeEnginePlugin plugin;
    private final MazeApiProvider provider;

    MazeApiRegistry(MazeEnginePlugin plugin, MazeApiProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    public List<MazeSnapshot> all() {
        provider.available();

        return plugin.mazes().records().stream().map(provider::snapshot).toList();
    }

    public Optional<MazeSnapshot> find(MazeId id) {
        provider.available();
        Objects.requireNonNull(id);

        return plugin.mazes().records().stream().filter(record -> record.name().equals(id.value())).findFirst()
            .map(provider::snapshot);
    }

    public Optional<MazeSnapshot> at(MazeLocation position) {
        provider.available();
        Objects.requireNonNull(position);

        return plugin.mazes().records().stream().filter(record -> record.worldId().equals(position.worldId()))
            .map(provider::snapshot).filter(snapshot -> snapshot.bounds().contains(position)).findFirst();
    }

    public TeleportDestination entrance(MazeId id) {
        provider.available();

        return MazeApiProvider.destination(plugin.mazes().entrance(plugin.mazes().require(id.value())));
    }

    public TeleportDestination destination(MazeId id) {
        provider.available();

        return MazeApiProvider.destination(plugin.mazes().destination(plugin.mazes().require(id.value())));
    }

    public List<CellPosition> route(MazeId id, CellPosition from, CellPosition to) {
        provider.available();
        var layout = plugin.mazes().require(id.value()).layout();

        for (CellPosition point : List.of(from, to)) {
            if (point.x() >= layout.width() || point.z() >= layout.depth()) {
                throw new IllegalArgumentException("Cell is outside the maze.");
            }
        }

        return Arrays.stream(layout.route(from.z() * layout.width() + from.x(), to.z() * layout.width() + to.x()))
            .mapToObj(index -> provider.cell(layout, index)).toList();
    }
}
