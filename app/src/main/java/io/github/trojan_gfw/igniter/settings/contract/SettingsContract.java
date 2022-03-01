package io.github.trojan_gfw.igniter.settings.contract;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import java.util.List;

import io.github.trojan_gfw.igniter.common.mvp.BasePresenter;
import io.github.trojan_gfw.igniter.common.mvp.BaseView;

public interface SettingsContract {
    interface Presenter extends BasePresenter {
        void addDNSInput();
        void removeDNSInput(int viewIndex);
        void saveSettings(@NonNull List<String> dnsList, String port);
        void exit();
    }
    interface View extends BaseView<Presenter> {
        @AnyThread
        void showExtraDNSList(@NonNull List<String> dnsList);
        void showDNSFormatError(int viewIndex);
        @AnyThread
        void showSettingsSaved();
        void appendDNSInput();
        void removeDNSInput(int index);
        void showExitConfirm();
        void showPortNumberError();
        @AnyThread
        void showLoading();
        @AnyThread
        void dismissLoading();
        void quit();
    }
}
