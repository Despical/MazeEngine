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

import dev.despical.mazeengine.api.operation.OperationKind;
import dev.despical.mazeengine.core.Bounds;
import dev.despical.mazeengine.service.MazeService.OperationOutcome;
import dev.despical.mazeengine.service.MazeService.OperationTicket;
import dev.despical.mazeengine.storage.MazeRecord;
import dev.despical.mazeengine.storage.MazeRepository;

import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reserves operation identities and publishes each terminal outcome once.
 * <p>
 * The tracker enforces the shared concurrent-operation limit and rejects a
 * second reservation for a busy name. Tickets retain a unique UUID, operation
 * kind, and completion future, while planning maps expose the temporarily
 * reserved bounds, world, and actor to the service.
 * <p>
 * Settlement releases the reservation and publishes the outcome before
 * completing its future. Removing the ticket makes repeated settlement inert
 * and prevents stale handles from matching replacement operations. Shutdown
 * settles outstanding tickets with cancellation and clears planning state.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class OperationTracker {

    private final IntSupplier maxConcurrent;
    private final Logger logger;
    private final BooleanSupplier stopping;

    OperationTracker(IntSupplier maxConcurrent, BooleanSupplier stopping, Logger logger) {
        this.maxConcurrent = maxConcurrent;
        this.stopping = stopping;
        this.logger = logger;
    }

    private final Map<String, OperationTicket> tickets = new HashMap<>();
    private Consumer<OperationOutcome> operationListener = ignored -> {
    };
    final Set<String> reserved = new HashSet<>();
    final Map<String, Bounds> planningBounds = new HashMap<>();
    final Map<String, UUID> planningWorlds = new HashMap<>();
    final Map<String, CommandSender> planningSenders = new HashMap<>();
    final Map<String, UUID> operationTokens = new HashMap<>();

    public void operationListener(Consumer<OperationOutcome> listener) {
        operationListener = Objects.requireNonNull(listener);
    }

    public OperationTicket operation(String name) {
        var ticket = tickets.get(MazeRepository.normalize(name));

        if (ticket == null) {
            throw new IllegalArgumentException("No active world operation for this maze.");
        }

        return ticket;
    }

    public boolean currentOperation(String name, UUID id) {
        var ticket = tickets.get(name);

        return ticket != null && ticket.id().equals(id);
    }

    void reserve(String name) {
        reserve(name, null);
    }

    void reserve(String name, OperationKind kind) {
        if (stopping.getAsBoolean()) {
            throw new IllegalArgumentException("Plugin is stopping.");
        }

        if (reserved.contains(name)) {
            throw new IllegalArgumentException("An operation is already running for this maze.");
        }

        if (reserved.size() >= maxConcurrent.getAsInt()) {
            throw new IllegalArgumentException("Generation limit reached; wait for the active operation.");
        }

        reserved.add(name);
        operationTokens.put(name, UUID.randomUUID());

        if (kind != null) {
            tickets.put(name, new OperationTicket(operationTokens.get(name), kind, new CompletableFuture<>()));
        }
    }

    void settle(String name, MazeRecord record, Throwable error) {
        reserved.remove(name);
        operationTokens.remove(name);
        var ticket = tickets.remove(name);

        if (ticket == null) {
            return;
        }

        try {
            operationListener.accept(new OperationOutcome(name, ticket, record, error));
        } catch (RuntimeException listenerError) {
            logger.log(Level.SEVERE, "Operation observer failed", listenerError);
        }

        if (error == null) {
            ticket.result().complete(record);
        } else {
            ticket.result().completeExceptionally(error);
        }
    }

    void clearPlanning(String name) {
        planningBounds.remove(name);
        planningWorlds.remove(name);
        planningSenders.remove(name);
    }

    void close(Map<String, MazeRecord> records) {
        for (String name : List.copyOf(tickets.keySet())) {
            settle(name, records.get(name), new CancellationException("MazeEngine disabled."));
        }

        reserved.clear();
        operationTokens.clear();
    }
}
