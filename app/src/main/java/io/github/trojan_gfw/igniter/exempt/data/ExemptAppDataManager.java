package io.github.trojan_gfw.igniter.exempt.data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.trojan_gfw.igniter.common.os.CommonSP;

public class ExemptAppDataManager implements ExemptAppDataSource {
    private final PackageManager mPackageManager;

    public ExemptAppDataManager(PackageManager packageManager) {
        super();
        mPackageManager = packageManager;
    }

    @Override
    public void saveExemptAppInfoSet(Set<String> exemptAppPackageNames) {
        JSONArray jsonArray = new JSONArray();
        for (String name : exemptAppPackageNames) {
            jsonArray.put(name);
        }
        CommonSP.setExemptAppListConfig(jsonArray.toString());
    }

    @Override
    public Set<String> loadExemptAppPackageNameSet() {
        String config = CommonSP.getExemptAppListConfig(null);
        Set<String> exemptAppPackageNames = new HashSet<>();
        if (null == config) {
            return exemptAppPackageNames;
        }
        try {
            JSONArray jsonArray = new JSONArray(config);
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                String packageName = jsonArray.optString(i);
                exemptAppPackageNames.add(packageName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // filter uninstalled apps
        List<ResolveInfo> resolveInfoList = queryCurrentInstalledApps();
        Set<String> installedAppPackageNames = new HashSet<>();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            installedAppPackageNames.add(resolveInfo.activityInfo.packageName);
        }
        Set<String> ret = new HashSet<>();
        for (String packageName : exemptAppPackageNames) {
            if (installedAppPackageNames.contains(packageName)) {
                ret.add(packageName);
            }
        }
        return ret;
    }

    private List<ResolveInfo> queryCurrentInstalledApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return mPackageManager.queryIntentActivities(intent, 0);
    }

    @Override
    public List<AppInfo> getAllAppInfoList() {
        List<ResolveInfo> resolveInfoList = queryCurrentInstalledApps();
        List<AppInfo> appInfoList = new ArrayList<>(resolveInfoList.size());
        for (ResolveInfo resolveInfo : resolveInfoList) {
            AppInfo appInfo = new AppInfo();
            appInfo.setAppName(resolveInfo.loadLabel(mPackageManager).toString());
            appInfo.setPackageName(resolveInfo.activityInfo.packageName);
            appInfo.setIcon(resolveInfo.activityInfo.loadIcon(mPackageManager));
            appInfoList.add(appInfo);
        }
        return appInfoList;
    }
}
