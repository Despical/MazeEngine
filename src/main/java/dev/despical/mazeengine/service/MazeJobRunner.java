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

import java.util.ArrayList;
import java.util.Map;

/**
 * Distributes one shared block and elapsed-time budget across active maze jobs.
 * <p>
 * Each server tick samples the current configured limits and advances a copy
 * of the active jobs in repeated passes while work progresses. A job waiting
 * for disk or chunk completion does not block the runner from visiting other
 * jobs, and an exhausted budget returns control to the server.
 * <p>
 * Cancellation requests and step failures are routed to the job's cleanup and
 * terminal-state logic. Finished jobs disappear from the owning map, and the
 * remaining jobs update their progress bars after the work pass. All live
 * world steps run on the server thread.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
final class MazeJobRunner {

    private final MazeEnginePlugin plugin;
    private final Map<String, MazeJob> jobs;

    MazeJobRunner(MazeEnginePlugin plugin, Map<String, MazeJob> jobs) {
        this.plugin = plugin;
        this.jobs = jobs;
    }

    void tick() {
        if (jobs.isEmpty()) {
            return;
        }

        long deadline = System.nanoTime() + (long) (plugin.settings().millisPerTick() * 1_000_000);
        int remaining = plugin.settings().blocksPerTick();
        var active = new ArrayList<>(jobs.values());
        boolean progressed;

        do {
            progressed = false;

            for (var job : active) {
                if (!jobs.containsKey(job.record.name())) {
                    continue;
                }

                try {
                    if (job.cancelled) {
                        job.cancel();
                        continue;
                    }

                    if (job.step()) {
                        remaining--;
                        progressed = true;
                    }
                } catch (Throwable error) {
                    job.fail(error);
                }

                if (remaining <= 0 || System.nanoTime() >= deadline) {
                    break;
                }
            }
        } while (progressed && remaining > 0 && System.nanoTime() < deadline);

        for (var job : new ArrayList<>(jobs.values())) {
            job.updateBar();
        }
    }
}
