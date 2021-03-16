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
}
