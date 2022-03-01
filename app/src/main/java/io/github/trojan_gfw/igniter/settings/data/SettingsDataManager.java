package io.github.trojan_gfw.igniter.settings.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.trojan_gfw.igniter.common.constants.Constants;
import io.github.trojan_gfw.igniter.common.utils.PreferenceUtils;

public class SettingsDataManager implements ISettingsDataManager {
    private final Context mContext;

    public SettingsDataManager(Context context) {
        super();
        mContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public List<String> loadExtraDNSList() {
        String dnsJsonArrayString = PreferenceUtils.getStringPreference(mContext.getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI), Constants.PREFERENCE_KEY_EXTRA_DNS, "");
        if (TextUtils.isEmpty(dnsJsonArrayString)) {
            return Collections.emptyList();
        }
        try {
            JSONArray jsonArray = new JSONArray(dnsJsonArrayString);
            int len = jsonArray.length();
            List<String> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(jsonArray.getString(i));
            }
            return list;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void saveExtraDNSList(@NonNull List<String> dnsList) {
        JSONArray jsonArray = new JSONArray();
        for (String dns : dnsList) {
            jsonArray.put(dns);
        }
        PreferenceUtils.putStringPreference(mContext.getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI), Constants.PREFERENCE_KEY_EXTRA_DNS,
                jsonArray.toString());
    }

    @Override
    public void saveFixedPort(int port) {
        PreferenceUtils.putIntPreference(mContext.getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI),
                Constants.PREFERENCE_KEY_FIXED_PORT, port);
    }

    @Override
    public int loadFixedPort() {
        return PreferenceUtils.getIntPreference(mContext.getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI),
                Constants.PREFERENCE_KEY_FIXED_PORT, -1);
    }
}
