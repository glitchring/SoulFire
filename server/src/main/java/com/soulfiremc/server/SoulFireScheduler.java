/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server;

import com.soulfiremc.server.util.RandomUtil;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.concurrent.*;
import java.util.function.Function;

public class SoulFireScheduler implements Executor {
  private static final ForkJoinPool SF_FORK_JOIN_POOL = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism(),
      ForkJoinPool.defaultForkJoinWorkerThreadFactory,
      null, true);
  private static final ScheduledExecutorService MANAGEMENT_SERVICE = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
    .name("SoulFireScheduler-Management-", 0)
    .factory());
  private final PriorityQueue<TimedRunnable> executionQueue = new ObjectHeapPriorityQueue<>();
  private final Logger logger;
  private final Function<Runnable, Runnable> runnableWrapper;
  @Setter
  private boolean blockNewTasks = false;
  private boolean isShutdown = false;

  public SoulFireScheduler(Logger logger) {
    this(logger, r -> r);
  }

  public SoulFireScheduler(Logger logger, Function<Runnable, Runnable> runnableWrapper) {
    this.logger = logger;
    this.runnableWrapper = runnableWrapper;

    MANAGEMENT_SERVICE.submit(this::managementTask);
  }

  public void managementTask() {
    if (isShutdown) {
      return;
    }

    synchronized (executionQueue) {
      while (!blockNewTasks && !executionQueue.isEmpty() && executionQueue.first().isReady()) {
        var timedRunnable = executionQueue.dequeue();
        schedule(() -> runCommand(timedRunnable.runnable()));
      }
    }

    MANAGEMENT_SERVICE.schedule(this::managementTask, 1, TimeUnit.MILLISECONDS);
  }

  public void schedule(Runnable command) {
    if (blockNewTasks) {
      return;
    }

    SF_FORK_JOIN_POOL.execute(() -> runCommand(command));
  }

  public void schedule(Runnable command, long delay, TimeUnit unit) {
    if (blockNewTasks) {
      return;
    }

    synchronized (executionQueue) {
      if (blockNewTasks) {
        return;
      }

      executionQueue.enqueue(TimedRunnable.of(command, delay, unit));
    }
  }

  public void scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
    schedule(() -> {
      scheduleAtFixedRate(command, period, period, unit);
      runCommand(command);
    }, delay, unit);
  }

  public void scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
    schedule(() -> {
      runCommand(command);
      scheduleWithFixedDelay(command, period, period, unit);
    }, delay, unit);
  }

  public void scheduleWithRandomDelay(Runnable command, long minDelay, long maxDelay, TimeUnit unit) {
    schedule(() -> {
      runCommand(command);
      scheduleWithRandomDelay(command, minDelay, maxDelay, unit);
    }, RandomUtil.getRandomLong(minDelay, maxDelay), unit);
  }

  public void drainQueue() {
    synchronized (executionQueue) {
      executionQueue.clear();
    }
  }

  public void shutdown() {
    blockNewTasks = true;
    isShutdown = true;
    drainQueue();
  }

  private void runCommand(Runnable command) {
    if (blockNewTasks) {
      return;
    }

    try {
      runnableWrapper.apply(command).run();
    } catch (Throwable t) {
      logger.error("Error in executor", t);
    }
  }

  @Override
  public void execute(@NotNull Runnable command) {
    schedule(command);
  }

  private record TimedRunnable(Runnable runnable, long time) implements Comparable<TimedRunnable> {
    public static TimedRunnable of(Runnable runnable, long delay, TimeUnit unit) {
      return new TimedRunnable(runnable, System.currentTimeMillis() + unit.toMillis(delay));
    }

    @Override
    public int compareTo(TimedRunnable o) {
      return Long.compare(time, o.time);
    }

    public boolean isReady() {
      return System.currentTimeMillis() >= time;
    }
  }
}
