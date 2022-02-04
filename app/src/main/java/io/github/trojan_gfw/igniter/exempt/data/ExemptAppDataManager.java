package io.github.trojan_gfw.igniter.exempt.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link ExemptAppDataSource}. This class reads and writes exempted app list in a
 * file. The exempted app package names will be written line by line in the file.
 * <br/>
 * Example:
 * <br/>
 * com.google.playstore
 * <br/>
 * io.github.trojan_gfw.igniter
 * <br/>
 * com.android.something
 */
public class ExemptAppDataManager implements ExemptAppDataSource {
    private final PackageManager mPackageManager;
    private final String mBlockAppListFilePath;
    private final String mAllowAppListFilePath;

    public ExemptAppDataManager(Context context, String blockAppListFilePath,
                                String allowAppListFilePath) {
        super();
        mPackageManager = context.getPackageManager();
        mBlockAppListFilePath = blockAppListFilePath;
        mAllowAppListFilePath = allowAppListFilePath;
    }

    @Override
    public void saveAllowAppInfoSet(@Nullable Set<String> allowAppPackageNames) {
        saveAppPackageNameSet(allowAppPackageNames, mAllowAppListFilePath);
    }

    @Override
    public void saveBlockAppInfoSet(@Nullable Set<String> blockAppPackageNames) {
        saveAppPackageNameSet(blockAppPackageNames, mBlockAppListFilePath);
    }

    private void saveAppPackageNameSet(@Nullable Set<String> packageNameSet, String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        if (packageNameSet == null || packageNameSet.isEmpty()) {
            return;
        }
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos);
             BufferedWriter bw = new BufferedWriter(osw)) {
            for (String name : packageNameSet) {
                bw.write(name);
                bw.write('\n');
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private Set<String> readExemptAppListConfig(String filePath) {
        File file = new File(filePath);
        Set<String> exemptAppSet = new HashSet<>();
        if (!file.exists()) {
            return exemptAppSet;
        }
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {
            String tmp = reader.readLine();
            while (!TextUtils.isEmpty(tmp)) {
                exemptAppSet.add(tmp);
                tmp = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exemptAppSet;
    }

    @Override
    public Set<String> loadAllowAppPackageNameSet() {
        return loadAppPackageNameSet(mAllowAppListFilePath);
    }

    @Override
    public Set<String> loadBlockAppPackageNameSet() {
        return loadAppPackageNameSet(mBlockAppListFilePath);
    }

    private Set<String> loadAppPackageNameSet(String filePath) {
        Set<String> exemptAppPackageNames = readExemptAppListConfig(filePath);
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
        } else { // These flags are deprecated since Nougat.
            flags |= PackageManager.GET_UNINSTALLED_PACKAGES;
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
