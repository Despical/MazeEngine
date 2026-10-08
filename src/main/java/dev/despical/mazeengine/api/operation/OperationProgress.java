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

package dev.despical.mazeengine.api.operation;

/**
 * Captures a point-in-time progress sample for one operation handle.
 * <p>
 * State is the stable lifecycle contract. Stage is diagnostic text describing
 * an internal step, such as planning or block writing, and should not be parsed
 * as a fixed enum. Query the handle again to obtain a later sample.
 * <p>
 * Percentage is an estimate for the current operation stage rather than elapsed
 * time. A successful terminal sample reports 100; failed and cancelled samples
 * report zero. Keep using state to decide whether an operation has ended.
 *
 * @param state the sampled lifecycle state
 * @param stage the diagnostic internal stage text
 * @param percent the estimated progress in 0..100
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record OperationProgress(State state, String stage, int percent) {

    /**
     * Describes whether an operation is active or has reached a final outcome.
     * <p>
     * Terminal states remain attached to the original handle after later work uses
     * the same maze identifier. Failure and cancellation do not imply rollback.
     */
    public enum State {

        /**
         * The accepted operation has not reached a final outcome.
         * <p>
         * This includes topology planning, preparation, world work, and waiting for
         * required persistence; blocks need not be changing at every moment.
         */
        RUNNING,

        /**
         * The requested operation and its final persistence finished successfully.
         * <p>
         * The completion stage contains an OperationResult and the reservation has
         * been released. This state does not imply that the actor was teleported.
         */
        COMPLETED,

        /**
         * The operation ended with an error other than cancellation.
         * <p>
         * The completion stage is exceptional. Blocks may already have changed,
         * so a retained failed record may need repair, regeneration, or removal.
         */
        FAILED,

        /**
         * The operation ended because it was cancelled or the plugin stopped.
         * <p>
         * The completion stage uses CancellationException. World changes already
         * applied are not guaranteed to have been undone.
         */
        CANCELLED
    }
}
