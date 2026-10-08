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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Owns bounded topology-planning workers and serialized storage execution.
 * <p>
 * Named planning tasks run in a fixed-size pool with a bounded queue. The
 * service retains their Future values so cancellation can interrupt work and
 * purge cancelled queue entries. Disk suppliers run on one separate executor
 * to preserve the ordering of journals, snapshots, and terminal record writes.
 * <p>
 * Workers do not authorize operations or mutate live worlds. Their callers
 * return results to the server thread and validate current reservation tokens.
 * Closing interrupts planning, shuts down storage, and allows a bounded wait
 * for queued disk work before forcing termination.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeWorkers implements AutoCloseable {

    private final ThreadPoolExecutor compute = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(64), Thread.ofPlatform().name("mazeengine-plan-", 0).factory());
    private final ExecutorService disk = Executors
        .newSingleThreadExecutor(Thread.ofPlatform().name("mazeengine-storage-", 0).factory());

    private final Map<String, Future<?>> computations = new HashMap<>();

    void plan(String name, Runnable task) {
        computations.put(name, compute.submit(task));
    }

    void finished(String name) {
        computations.remove(name);
    }

    void cancel(String name) {
        Future<?> calculation = computations.remove(name);

        if (calculation != null) {
            calculation.cancel(true);
            compute.purge();
        }
    }

    <T> CompletableFuture<T> io(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, disk);
    }

    @Override
    public void close() {
        computations.clear();
        compute.shutdownNow();
        disk.shutdown();

        try {
            if (!disk.awaitTermination(10, TimeUnit.SECONDS)) {
                disk.shutdownNow();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
