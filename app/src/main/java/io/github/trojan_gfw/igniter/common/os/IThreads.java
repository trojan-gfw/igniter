package io.github.trojan_gfw.igniter.common.os;

import java.util.concurrent.Executor;

/**
 * Interface of thread pool. Provides methods to run {@link Task} or {@link Runnable} in main thread
 * or in background.
 */
public interface IThreads {
    Executor getThreadPoolExecutor();

    void runOnUiThread(Runnable runnable);

    void runOnUiThread(Runnable runnable, long delayMillis);

    void runOnWorkThread(Task task);

    void runOnWorkThread(final Task task, long delayMillis);

    void removeDelayedAction(Runnable action);
}
