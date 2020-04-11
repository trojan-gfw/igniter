package io.github.trojan_gfw.igniter.common.utils;

import androidx.annotation.StringRes;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

public class SnackbarUtils {

    public static void showTextShort(View view, @StringRes int id) {
        Snackbar.make(view, id, Snackbar.LENGTH_SHORT).show();
    }

    public static void showTextShort(View view, @StringRes int id, @StringRes int actionId, View.OnClickListener listener) {
        Snackbar.make(view, id, Snackbar.LENGTH_SHORT).setAction(actionId, listener).show();
    }

    public static void showTextShort(View view, String text, String actionText, View.OnClickListener listener) {
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).setAction(actionText, listener).show();
    }

    public static void showTextShort(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show();
    }

    public static void showTextLong(View view, @StringRes int id) {
        Snackbar.make(view, id, Snackbar.LENGTH_LONG).show();
    }

    public static void showTextLong(View view, @StringRes int id, @StringRes int actionId, View.OnClickListener listener) {
        Snackbar.make(view, id, Snackbar.LENGTH_LONG).setAction(actionId, listener).show();
    }

    public static void showTextLong(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_LONG).show();
    }

    public static void showTextLong(View view, String text, String actionText, View.OnClickListener listener) {
        Snackbar.make(view, text, Snackbar.LENGTH_LONG).setAction(actionText, listener).show();
    }
}
