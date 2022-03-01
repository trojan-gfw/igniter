package io.github.trojan_gfw.igniter.common.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.content.ContentResolverCompat;

public abstract class PreferenceUtils {

    public static boolean getBooleanPreference(ContentResolver resolver, Uri uri, String key, boolean defaultValue) {
        return getPreference(resolver, uri, key, defaultValue, ((cursor, columnIndex, type, defValue) -> {
            switch (type) {
                case Cursor.FIELD_TYPE_STRING:
                    return Boolean.parseBoolean(cursor.getString(columnIndex));
                case Cursor.FIELD_TYPE_INTEGER:
                    return cursor.getInt(columnIndex) == 1;
                default:
                    return defValue;
            }
        }));
    }

    public static void putBooleanPreference(ContentResolver resolver, Uri uri, String key, boolean value) {
        putPreference(resolver, uri, key, value, (v, contentValues) -> contentValues.put(key, v));
    }

    public static String getStringPreference(ContentResolver resolver, Uri uri, String key, String defVal) {
        return getPreference(resolver, uri, key, defVal, ((cursor, columnIndex, type, defValue) -> {
            if (Cursor.FIELD_TYPE_STRING == type) {
                return cursor.getString(columnIndex);
            }
            return defVal;
        }));
    }

    public static void putStringPreference(ContentResolver resolver, Uri uri, String key, String value) {
        putPreference(resolver, uri, key, value, (v, contentValues) -> contentValues.put(key, v));
    }

    public static void putIntPreference(ContentResolver resolver, Uri uri, String key, int value) {
        putPreference(resolver, uri, key, value, (v, contentValues) -> contentValues.put(key, v));
    }

    public static int getIntPreference(ContentResolver resolver, Uri uri, String key, int defVal) {
        return getPreference(resolver, uri, key, defVal, ((cursor, columnIndex, type, defValue) -> {
            if (Cursor.FIELD_TYPE_INTEGER == type) {
                return cursor.getInt(columnIndex);
            }
            return defVal;
        }));
    }

    private static <T> void putPreference(ContentResolver resolver, Uri uri, String key, T value,
                                          ContentValuePutter<T> putter) {
        ContentValues contentValues = new ContentValues(1);
        putter.onPutValue(value, contentValues);
        resolver.update(uri, contentValues, null, null);
    }

    private static <T> T getPreference(ContentResolver resolver, Uri uri, String key, T defVal,
                                       CursorReader<T> reader) {
        try (Cursor query = ContentResolverCompat.query(resolver, uri, new String[]{key}, null,
                null, null, null)) {
            if (query == null) return defVal;
            if (query.moveToFirst()) {
                int columnIndex = query.getColumnIndex(key);
                if (columnIndex >= 0) {
                    int type = query.getType(columnIndex);
                    return reader.onReadValue(query, columnIndex, type, defVal);
                }
            }
        }
        return defVal;
    }

    interface ContentValuePutter<T> {
        void onPutValue(T value, ContentValues contentValues);
    }

    interface CursorReader<T> {
        T onReadValue(Cursor cursor, int columnIndex, int type, T defValue);
    }
}
