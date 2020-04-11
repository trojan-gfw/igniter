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

        void exit();
    }

    interface View extends BaseView<Presenter> {
        @UiThread
        void showLoading();

        @UiThread
        void dismissLoading();

        @UiThread
        void showSaveSuccess();

        @UiThread
        void showExitConfirm();

        @UiThread
        void showAppList(List<AppInfo> packageNames);

        @AnyThread
        void exit(boolean configurationChanged);
    }
}
