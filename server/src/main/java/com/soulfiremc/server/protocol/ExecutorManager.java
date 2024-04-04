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
package com.soulfiremc.server.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExecutorManager {
  public static final ThreadLocal<BotConnection> BOT_CONNECTION_THREAD_LOCAL = new ThreadLocal<>();
  private final List<ExecutorService> executors = Collections.synchronizedList(new ArrayList<>());
  private final String threadPrefix;
  private boolean shutdown = false;

  public ScheduledExecutorService newScheduledExecutorService(
    BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor =
      Executors.newSingleThreadScheduledExecutor(getThreadFactory(botConnection, threadName));

    executors.add(executor);

    return executor;
  }

  public ExecutorService newExecutorService(BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor = Executors.newSingleThreadExecutor(getThreadFactory(botConnection, threadName));

    executors.add(executor);

    return executor;
  }

  public ExecutorService newFixedExecutorService(
    int threadAmount, BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor =
      Executors.newFixedThreadPool(threadAmount, getThreadFactory(botConnection, threadName));

    executors.add(executor);

    return executor;
  }

  public ExecutorService newCachedExecutorService(BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor = Executors.newCachedThreadPool(getThreadFactory(botConnection, threadName));

    executors.add(executor);

    return executor;
  }

  private ThreadFactory getThreadFactory(BotConnection botConnection, String threadName) {
    return runnable -> {
      var usedThreadName = threadName;
      if (runnable instanceof NamedRunnable named) {
        usedThreadName = named.name();
      }

      return Thread.ofPlatform()
        .name(threadPrefix + "-" + usedThreadName)
        .daemon()
        .unstarted(
          () -> {
            BOT_CONNECTION_THREAD_LOCAL.set(botConnection);
            runnable.run();
            BOT_CONNECTION_THREAD_LOCAL.remove();
          });
    };
  }

  public void shutdownAll() {
    shutdown = true;
    executors.forEach(ExecutorService::shutdownNow);
  }

  private record NamedRunnable(Runnable runnable, String name) implements Runnable {
    @Override
    public void run() {
      runnable.run();
    }
  }
}
