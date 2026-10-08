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

import dev.despical.mazeengine.service.MazeService;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.api.model.MazeId;
import dev.despical.mazeengine.api.operation.MazeOperation;
import dev.despical.mazeengine.api.operation.OperationKind;
import dev.despical.mazeengine.api.operation.OperationProgress;
import dev.despical.mazeengine.api.operation.OperationResult;

import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Tracks one public operation identity without exposing mutable service state.
 * <p>
 * The handle captures the maze identifier and accepted ticket, then observes
 * its result future to retain a terminal progress sample and public result.
 * Failure causes are unwrapped, and cancellation is represented separately
 * from ordinary operation errors.
 * <p>
 * The completion accessor exposes a minimal read-only stage. Progress is
 * sampled on the server thread, while cancellation checks that this exact
 * ticket is still current before applying actor permissions and ownership.
 * An old handle therefore cannot cancel newer work using the same maze name.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeOperationHandle implements MazeOperation {

    private final MazeEnginePlugin plugin;
    private final MazeApiProvider provider;
    private final MazeId maze;
    private final MazeService.OperationTicket ticket;
    private final CompletableFuture<OperationResult> completion = new CompletableFuture<>();
    private volatile OperationProgress terminal;

    MazeOperationHandle(MazeEnginePlugin plugin, MazeApiProvider provider, MazeId maze,
        MazeService.OperationTicket ticket) {
        this.plugin = plugin;
        this.provider = provider;
        this.maze = maze;
        this.ticket = ticket;
        ticket.result().whenComplete((record, error) -> {
            if (error == null) {
                terminal = new OperationProgress(OperationProgress.State.COMPLETED, "COMPLETED", 100);
                completion.complete(provider.result(ticket, record));
            } else {
                var cause = provider.unwrap(error);
                var state = cause instanceof CancellationException ? OperationProgress.State.CANCELLED
                    : OperationProgress.State.FAILED;
                terminal = new OperationProgress(state, state.name(), 0);
                completion.completeExceptionally(cause);
            }
        });
    }

    public UUID id() {
        return ticket.id();
    }

    public MazeId mazeId() {
        return maze;
    }

    public OperationKind kind() {
        return ticket.kind();
    }

    public OperationProgress progress() {
        provider.mainThread();

        if (terminal != null) {
            return terminal;
        }

        return new OperationProgress(OperationProgress.State.RUNNING, plugin.mazes().progress(maze.value()),
            plugin.mazes().progressPercent(maze.value()));
    }

    public CompletionStage<OperationResult> completion() {
        return completion.minimalCompletionStage();
    }

    public boolean cancel(CommandSender actor) {
        provider.available();

        if (!plugin.mazes().currentOperation(maze.value(), ticket.id())) {
            return false;
        }

        provider.actor(actor, "cancel");
        plugin.mazes().cancel(actor, maze.value());

        return true;
    }
}
