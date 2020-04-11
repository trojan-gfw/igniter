package io.github.trojan_gfw.igniter.common.os;


import android.os.Process;
import androidx.annotation.WorkerThread;

/**
 * A wrapper of Runnable.
 */
public abstract class Task implements Runnable {
    private int priority = Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE;

    public Task() {
    }

    /**
     * Construct a task with priority.
     *
     * @param priority {@link Process#THREAD_PRIORITY_BACKGROUND}
     */
    public Task(int priority) {
        this.priority = priority;
    }

    @Override
    public void run() {
        Process.setThreadPriority(priority);
        onRun();
    }

    @WorkerThread
    public abstract void onRun();
}

