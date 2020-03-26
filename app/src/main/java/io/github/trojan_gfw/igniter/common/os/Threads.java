package io.github.trojan_gfw.igniter.common.os;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton implementation of {@link IThreads}. Call {@link #instance()} to get the instance.
 */
public final class Threads implements IThreads {
    private ExecutorService mThreadPool;
    private Handler mHandler;

    private Threads() {
        mThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                30L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new DefaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * The default thread factory
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "ThreadHelperPool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            return t;
        }
    }

    public static IThreads instance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final Threads INSTANCE = new Threads();
    }

    @Override
    public Executor getThreadPoolExecutor() {
        return mThreadPool;
    }

    @Override
    public void runOnWorkThread(Task task) {
        mThreadPool.execute(task);
    }

    @Override
    public void runOnWorkThread(final Task task, long delayMillis) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mThreadPool.execute(task);
            }
        }, delayMillis);
    }

    @Override
    public void runOnUiThread(Runnable action) {
        mHandler.post(action);
    }

    @Override
    public void runOnUiThread(Runnable action, long delayMillis) {
        mHandler.postDelayed(action, delayMillis);
    }

    @Override
    public void removeDelayedAction(Runnable action) {
        mHandler.removeCallbacks(action);
    }
}
