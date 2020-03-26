package io.github.trojan_gfw.igniter.initializer;

import android.content.Context;
import android.os.Process;

import java.util.LinkedList;
import java.util.List;

import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.ProcessUtils;

/**
 * Helper class of application initializations. You can just extend {@link Initializer} to create your
 * own initializer and register it in {@link #registerMainInitializers()} or {@link #registerToolsInitializers()}.
 * You should consider carefully to determine which process your initializers are run in.
 */
public class InitializerHelper {
    private static List<Initializer> sMainInitializerList;
    private static List<Initializer> sToolsInitializerList;

    static {
        sMainInitializerList = new LinkedList<>();
        sToolsInitializerList = new LinkedList<>();
        registerMainInitializers();
        registerToolsInitializers();
    }

    private static void registerMainInitializers() {
        sMainInitializerList.add(new MainInitializer());
    }

    private static void registerToolsInitializers() {
        sToolsInitializerList.add(new ToolInitializer());
    }

    public static void runInit(Context context) {
        if (isToolProcess(context)) {
            runInit(context, sToolsInitializerList);
        } else {
            runInit(context, sMainInitializerList);
        }
        clearInitializerLists();
    }

    private static void clearInitializerLists() {
        sMainInitializerList = null;
        sToolsInitializerList = null;
    }

    private static void runInit(final Context context, List<Initializer> initializerList) {
        final List<Initializer> runInWorkerThreadList = new LinkedList<>();
        for (int i = initializerList.size() - 1; i >= 0; i--) {
            if (initializerList.get(i).runsInWorkerThread()) {
                runInWorkerThreadList.add(initializerList.remove(i));
            }
        }
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                runInitList(context, runInWorkerThreadList);
            }
        });
        runInitList(context, initializerList);
    }

    private static void runInitList(Context context, List<Initializer> initializers) {
        for (int i = initializers.size() - 1; i >= 0; i--) {
            initializers.remove(i).init(context);
        }
    }

    public static boolean isMainProcess(Context context) {
        return !isToolProcess(context);
    }

    public static boolean isToolProcess(Context context) {
        String currentProcessName = ProcessUtils.getProcessNameByPID(context, Process.myPid());
        if (currentProcessName != null) {
            return currentProcessName.endsWith(":tools");
        }
        return false;
    }
}
