package io.github.trojan_gfw.igniter.common.utils;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.Nullable;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

public class ProcessUtils {

    @Nullable
    public static String getProcessNameByPID(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
            if (info.pid == pid) {
                return info.processName;
            }
        }
        return null;
    }
}
