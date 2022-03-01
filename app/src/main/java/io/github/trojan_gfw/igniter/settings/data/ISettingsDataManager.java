package io.github.trojan_gfw.igniter.settings.data;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.List;

public interface ISettingsDataManager {
    @NonNull
    @WorkerThread
    List<String> loadExtraDNSList();
    @WorkerThread
    void saveExtraDNSList(@NonNull List<String> dnsList);
    void saveFixedPort(int port);

    /***
     * Load fixed port from settings. If no fixed port is specified, returns -1.
     */
    int loadFixedPort();
}
