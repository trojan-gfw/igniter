package io.github.trojan_gfw.igniter.exempt.data;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.List;
import java.util.Set;

public interface ExemptAppDataSource {
    /**
     * Load exempt applications' package names.
     *
     * @return exempt applications' package names..
     */
    @WorkerThread
    Set<String> loadBlockAppPackageNameSet();

    /**
     * Load package names of applications to whom the proxy is applied.
     */
    @WorkerThread
    Set<String> loadAllowAppPackageNameSet();

    boolean checkExternalExemptAppInfoConfigExistence();

    /**
     * Save exempt applications' package names.
     *
     * @param blockAppPackageNames exempt app package name set
     */
    @WorkerThread
    void saveBlockAppInfoSet(@Nullable Set<String> blockAppPackageNames);

    @WorkerThread
    void saveAllowAppInfoSet(@Nullable Set<String> allowAppPackageNames);

    /**
     * Migrate external exempted application config file to private directory.
     */
    void migrateExternalExemptAppInfo();

    void deleteExternalExemptAppInfo();

    /**
     * Load all application info list, including exempt apps and non-exempt apps.
     *
     * @return all application info list
     */
    @WorkerThread
    List<AppInfo> getAllAppInfoList();
}
