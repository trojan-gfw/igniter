package io.github.trojan_gfw.igniter.exempt.contract;

import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;

import java.util.List;

import io.github.trojan_gfw.igniter.common.mvp.BasePresenter;
import io.github.trojan_gfw.igniter.common.mvp.BaseView;
import io.github.trojan_gfw.igniter.exempt.data.AppInfo;

public interface ExemptAppContract {
    interface Presenter extends BasePresenter {
        void updateAppInfo(AppInfo appInfo, int position, boolean exempt);

        void saveExemptAppInfoList();

        /**
         * @return true if exit directly, false to cancel exiting.
         */
        boolean handleBackPressed();

        void filterAppsByName(String name);

        void ignoreExternalExemptedAppListConfigForever();

        void migrateExternalExemptedAppListFileToPrivateDirectory();

        void loadBlockAppListConfig();

        void loadAllowAppListConfig();

        void exit();
    }

    interface View extends BaseView<Presenter> {
        @UiThread
        void showExemptedAppListMigrationNotice();

        @UiThread
        void showLoading();

        @UiThread
        void dismissLoading();

        @UiThread
        void showSaveSuccess();

        @UiThread
        void showExitConfirm();

        @UiThread
        void showBlockAppList(List<AppInfo> packageNames);

        @UiThread
        void showAllowAppList(List<AppInfo> packagesNames);

        @AnyThread
        void exit(boolean configurationChanged);
    }
}
