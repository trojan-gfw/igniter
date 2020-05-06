package io.github.trojan_gfw.igniter.common.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.content.ContentResolverCompat;

public abstract class PreferenceUtils {

    public static boolean getBooleanPreference(ContentResolver resolver, Uri uri, String key, boolean defaultValue) {
        try (Cursor query = ContentResolverCompat.query(resolver, uri, new String[]{key}, null,
                null, null, null)) {
            if (query.moveToFirst()) {
                int columnIndex = query.getColumnIndex(key);
                if (columnIndex >= 0) {
                    int type = query.getType(columnIndex);
                    switch (type) {
                        case Cursor.FIELD_TYPE_STRING:
                            return Boolean.parseBoolean(query.getString(columnIndex));
                        case Cursor.FIELD_TYPE_INTEGER:
                            return query.getInt(columnIndex) == 1;
                        default:
                            return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    public static void putBooleanPreference(ContentResolver resolver, Uri uri, String key, boolean value) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(key, value);
        resolver.update(uri, contentValues, null, null);
    }
}
