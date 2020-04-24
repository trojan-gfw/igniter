package io.github.trojan_gfw.igniter.exempt.data;

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
    Set<String> loadExemptAppPackageNameSet();

    /**
     * Save exempt applications' package names.
     *
     * @param exemptAppPackageNames exempt app package name set
     */
    @WorkerThread
    void saveExemptAppInfoSet(Set<String> exemptAppPackageNames);

    /**
     * Load all application info list, including exempt apps and non-exempt apps.
     * @return all application info list
     */
    @WorkerThread
    List<AppInfo> getAllAppInfoList();
}
