package io.github.trojan_gfw.igniter;

import android.util.Log;

public final class LogHelper {

    // Logcat is line-buffered

    private static final int maxLogSize = 1000;

    private static final ILogFunction _v = new ILogFunction() {
        @Override
        public int output(String tag, String msg) {
            return Log.v(tag, msg);
        }
    };
    private static final ILogFunction _d = new ILogFunction() {
        @Override
        public int output(String tag, String msg) {
            return Log.d(tag, msg);
        }
    };

    private static final ILogFunction _i = new ILogFunction() {
        @Override
        public int output(String tag, String msg) {
            return Log.i(tag, msg);
        }
    };

    private static final ILogFunction _w = new ILogFunction() {
        @Override
        public int output(String tag, String msg) {
            return Log.w(tag, msg);
        }
    };

    private static final ILogFunction _e = new ILogFunction() {
        @Override
        public int output(String tag, String msg) {
            return Log.e(tag, msg);
        }
    };

    private static void UnderlyingLog(String veryLongString, ILogFunction func, String TAG) {

        if (veryLongString.length() < maxLogSize) {
            func.output(TAG, veryLongString);
            return;
        }

        for (int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = end > veryLongString.length() ? veryLongString.length() : end;
            func.output(TAG, veryLongString.substring(start, end));
        }
    }

    public static void v(String tag, String msg) {
        UnderlyingLog(msg, _v, tag);
    }

    public static void d(String tag, String msg) {
        UnderlyingLog(msg, _d, tag);
    }

    public static void i(String tag, String msg) {
        UnderlyingLog(msg, _i, tag);
    }

    public static void w(String tag, String msg) {
        UnderlyingLog(msg, _w, tag);
    }

    public static void e(String tag, String msg) {
        UnderlyingLog(msg, _e, tag);
    }


}
