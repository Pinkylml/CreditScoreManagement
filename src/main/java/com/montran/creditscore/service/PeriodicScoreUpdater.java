package com.montran.creditscore.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Schedules periodic credit-score recalculation for all users.
 *
 * <p>On each tick, this component invokes {@link CreditScoreEngine#recalculateAllUsers()},
 * which acquires the per-user {@link java.util.concurrent.locks.ReentrantLock} for every
 * user so that scheduled updates are fully serialised against live transactions.</p>
 *
 * <h3>Lifecycle</h3>
 * <pre>{@code
 * PeriodicScoreUpdater updater = new PeriodicScoreUpdater(engine);
 * updater.start(0, 2, TimeUnit.SECONDS);   // starts immediately, runs every 2 s
 * // ... application runs ...
 * updater.stop();                           // shuts down cleanly
 * }</pre>
 *
 * <h3>Thread model</h3>
 * <p>A single-threaded {@link ScheduledExecutorService} is used so that overlapping
 * scheduled runs cannot occur (the next run waits until the current one finishes).
 * If a recalculation run throws an unchecked exception it is caught, logged, and the
 * schedule continues uninterrupted.</p>
 */
public class PeriodicScoreUpdater {

    private static final Logger LOGGER = Logger.getLogger(PeriodicScoreUpdater.class.getName());

    private final CreditScoreEngine engine;
    private final ScheduledExecutorService scheduler;

    /** Handle returned by the executor so the task can be cancelled independently of shutdown. */
    private volatile ScheduledFuture<?> scheduledTask;

    /**
     * Creates the updater with a dedicated single-threaded scheduler.
     *
     * @param engine The engine whose {@link CreditScoreEngine#recalculateAllUsers()} will be invoked.
     * @throws IllegalArgumentException if {@code engine} is null.
     */
    public PeriodicScoreUpdater(CreditScoreEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("CreditScoreEngine must not be null.");
        }
        this.engine = engine;
        // Single thread: prevents overlapping scheduled runs.
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "periodic-score-updater");
            t.setDaemon(true); // does not prevent JVM shutdown
            return t;
        });
    }

    /**
     * Creates the updater using a caller-supplied executor (useful for testing).
     *
     * @param engine    The scoring engine.
     * @param scheduler The executor to schedule tasks on.
     */
    public PeriodicScoreUpdater(CreditScoreEngine engine, ScheduledExecutorService scheduler) {
        if (engine == null || scheduler == null) {
            throw new IllegalArgumentException("Engine and scheduler must not be null.");
        }
        this.engine = engine;
        this.scheduler = scheduler;
    }

    /**
     * Starts the periodic update task.
     *
     * @param initialDelay Delay before the first execution.
     * @param period       Time between the <em>end</em> of one run and the <em>start</em> of the next
     *                     (uses {@code scheduleWithFixedDelay} to prevent pile-up if a run is slow).
     * @param unit         The time unit for {@code initialDelay} and {@code period}.
     * @throws IllegalStateException if this updater has already been started.
     */
    public void start(long initialDelay, long period, TimeUnit unit) {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            throw new IllegalStateException("PeriodicScoreUpdater is already running.");
        }

        scheduledTask = scheduler.scheduleWithFixedDelay(
                this::runUpdate, initialDelay, period, unit);

        LOGGER.info(String.format(
                "[PeriodicScoreUpdater] Scheduled with initialDelay=%d %s, period=%d %s.",
                initialDelay, unit, period, unit));
    }

    /**
     * Stops the periodic update task and shuts down the executor.
     * Waits up to 5 seconds for any in-progress run to complete cleanly.
     */
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false); // let any running invocation finish
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warning("[PeriodicScoreUpdater] Executor did not terminate in time; forcing shutdown.");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("[PeriodicScoreUpdater] Stopped.");
    }

    /** The task submitted to the scheduler. Exceptions are caught to keep the schedule alive. */
    private void runUpdate() {
        try {
            System.out.println("[PeriodicScoreUpdater] Running scheduled credit-score recalculation for all users...");
            engine.recalculateAllUsers();
            System.out.println("[PeriodicScoreUpdater] Recalculation complete.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[PeriodicScoreUpdater] Unhandled exception during scheduled recalculation.", e);
        }
    }
}
