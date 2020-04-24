package io.github.trojan_gfw.igniter.initializer;

import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;

import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.ProcessUtils;

/**
 * Helper class of application initializations. You can just extend {@link Initializer} to create your
 * own initializer and register it in {@link #registerMainInitializers()} or {@link #registerToolsInitializers()}.
 * You should consider carefully to determine which process your initializers are run in.
 */
public class InitializerHelper {
    private static final String TOOL_PROCESS_POSTFIX = ":tools";
    private static final String PROXY_PROCESS_POSTFIX = ":proxy";
    private static List<Initializer> sInitializerList;

    private static void createInitializerList() {
        sInitializerList = new LinkedList<>();
    }

    private static void registerMainInitializers() {
        createInitializerList();
        sInitializerList.add(new MainInitializer());
    }

    private static void registerToolsInitializers() {
        createInitializerList();
        sInitializerList.add(new ToolInitializer());
    }

    private static void registerProxyInitializers() {
        createInitializerList();
        sInitializerList.add(new ProxyInitializer());
    }

    public static void runInit(Context context) {
        final String processName = ProcessUtils.getProcessNameByPID(context, Process.myPid());
        if (isToolProcess(processName)) {
            registerToolsInitializers();
        } else if (isProxyProcess(processName)) {
            registerProxyInitializers();
        } else {
            registerMainInitializers();
        }
        runInit(context, sInitializerList);
        clearInitializerLists();
    }

    private static void clearInitializerLists() {
        sInitializerList = null;
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

    private static boolean isMainProcess(String processName) {
        return !isToolProcess(processName) && !isProxyProcess(processName);
    }

    private static boolean isToolProcess(String processName) {
        return TextUtils.equals(processName, TOOL_PROCESS_POSTFIX);
    }

    private static boolean isProxyProcess(String processName) {
        return TextUtils.equals(processName, PROXY_PROCESS_POSTFIX);
    }
}
