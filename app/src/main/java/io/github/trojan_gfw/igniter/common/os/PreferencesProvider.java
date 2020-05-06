package io.github.trojan_gfw.igniter.common.os;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.trojan_gfw.igniter.Globals;
import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.common.constants.Constants;

public class PreferencesProvider extends ContentProvider {
    private static final String TAG = "PreferencesProvider";
    public static final String AUTHORITY = Constants.PREFERENCE_AUTHORITY;
    public static final String PATH = Constants.PREFERENCE_PATH;
    private static final int CODE_PREFERENCES = 2077;

    private Map<String, Object> mCachePreferences;
    private final UriMatcher mUriMatcher;
    private String mPreferencesFilePath;

    public PreferencesProvider() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, PATH, CODE_PREFERENCES);
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        LogHelper.e(TAG, "attachInfo");
        mPreferencesFilePath = Globals.getPreferencesFilePath();
    }

    private boolean isUriNotValid(Uri uri) {
        return mUriMatcher.match(uri) != CODE_PREFERENCES;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (isUriNotValid(uri)) return 0;
        mCachePreferences.clear();
        File preferencesFile = new File(mPreferencesFilePath);
        if (preferencesFile.exists()) {
            preferencesFile.delete();
            return 1;
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (isUriNotValid(uri)) return null;
        update(uri, values, null, null);
        return uri;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private void ensureCacheReady() {
        if (mCachePreferences == null) {
            mCachePreferences = new LinkedHashMap<>();
            readPreferencesToCache();
        }
    }

    private void readPreferencesToCache() {
        File preferencesFile = new File(mPreferencesFilePath);
        if (!preferencesFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(preferencesFile);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                sb.append(tmp);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            Iterator<String> it = jsonObject.keys();
            while (it.hasNext()) {
                String key = it.next();
                mCachePreferences.put(key, jsonObject.opt(key));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        LogHelper.i(TAG, "query values with: " + this);
        if (isUriNotValid(uri)) {
            return new MatrixCursor(new String[0], 1);
        }
        ensureCacheReady();
        String[] queryKeys;
        if (projection == null) {
            Set<String> keySet = mCachePreferences.keySet();
            queryKeys = new String[keySet.size()];
            int i = 0;
            for (String key : keySet) {
                queryKeys[i++] = key;
            }
        } else {
            queryKeys = projection;
        }
        MatrixCursor cursor = new MatrixCursor(queryKeys, 1);
        Object[] values = new Object[queryKeys.length];
        for (int i = 0; i < queryKeys.length; i++) {
            values[i] = mCachePreferences.get(queryKeys[i]);
        }
        cursor.addRow(values);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        LogHelper.i(TAG, "update values with: " + this);
        if (isUriNotValid(uri)) {
            return 0;
        }
        ensureCacheReady();
        Set<String> keySet = values.keySet();
        boolean valueChanged = false;
        for (String key : keySet) {
            Object previousValue = mCachePreferences.get(key);
            Object nextValue = values.get(key);
            if (!Objects.equals(previousValue, nextValue)) {
                valueChanged = true;
                mCachePreferences.put(key, nextValue);
            }
        }
        if (valueChanged) {
            writeCacheIntoFile();
            return 1;
        }
        return 0;
    }

    private void writeCacheIntoFile() {
        if (mCachePreferences == null) return;
        LogHelper.i(TAG, "write preferences to file: " + this);
        JSONObject jsonObject = new JSONObject();
        try {
            for (String key : mCachePreferences.keySet()) {
                jsonObject.put(key, mCachePreferences.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        File preferencesFile = new File(mPreferencesFilePath);
        try (FileOutputStream fos = new FileOutputStream(preferencesFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            osw.write(jsonObject.toString());
            osw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
