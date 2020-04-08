package io.github.trojan_gfw.igniter.exempt.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

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
 * file named {@link #EXEMPT_APP_LIST_FILE_NAME} under {@link Context#getFilesDir()}. The exempted app
 * package names will be written line by line in the file.
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
    private static final String EXEMPT_APP_LIST_FILE_NAME = "exempt_list";
    private final PackageManager mPackageManager;
    private final String mExemptAppListFilePath;

    public ExemptAppDataManager(Context context) {
        super();
        mPackageManager = context.getPackageManager();
        String dir = context.getFilesDir().getAbsolutePath();
        if (dir.endsWith(File.separator)) {
            mExemptAppListFilePath = dir + EXEMPT_APP_LIST_FILE_NAME;
        } else {
            mExemptAppListFilePath = dir + File.separator + EXEMPT_APP_LIST_FILE_NAME;
        }
    }

    @Override
    public void saveExemptAppInfoSet(Set<String> exemptAppPackageNames) {
        File file = new File(mExemptAppListFilePath);
        if (file.exists()) {
            file.delete();
        }
        if (exemptAppPackageNames == null || exemptAppPackageNames.isEmpty()) {
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos);
             BufferedWriter bw = new BufferedWriter(osw)) {
            for (String name : exemptAppPackageNames) {
                bw.write(name);
                bw.write('\n');
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private Set<String> readExemptAppListConfig() {
        File file = new File(mExemptAppListFilePath);
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
    public Set<String> loadExemptAppPackageNameSet() {
        Set<String> exemptAppPackageNames = readExemptAppListConfig();
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
