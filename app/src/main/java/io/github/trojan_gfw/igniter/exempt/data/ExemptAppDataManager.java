package io.github.trojan_gfw.igniter.exempt.data;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

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
        List<ApplicationInfo> applicationInfoList = queryCurrentInstalledApps();
        Set<String> installedAppPackageNames = new HashSet<>();
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            installedAppPackageNames.add(applicationInfo.packageName);
        }
        Set<String> ret = new HashSet<>();
        for (String packageName : exemptAppPackageNames) {
            if (installedAppPackageNames.contains(packageName)) {
                ret.add(packageName);
            }
        }
        return ret;
    }

    private List<ApplicationInfo> queryCurrentInstalledApps() {
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags |= PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DISABLED_COMPONENTS;
        } else {
            flags |= PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_DISABLED_COMPONENTS;
        }
        return mPackageManager.getInstalledApplications(flags);
    }

    @Override
    public List<AppInfo> getAllAppInfoList() {
        List<ApplicationInfo> applicationInfoList = queryCurrentInstalledApps();
        List<AppInfo> appInfoList = new ArrayList<>(applicationInfoList.size());
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            AppInfo appInfo = new AppInfo();
            appInfo.setAppName(mPackageManager.getApplicationLabel(applicationInfo).toString());
            appInfo.setPackageName(applicationInfo.packageName);
            appInfo.setIcon(mPackageManager.getApplicationIcon(applicationInfo));
            appInfoList.add(appInfo);
        }
        return appInfoList;
    }
}
