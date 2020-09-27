package io.github.trojan_gfw.igniter.exempt.presenter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.github.trojan_gfw.igniter.common.constants.Constants;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.PreferenceUtils;
import io.github.trojan_gfw.igniter.exempt.contract.ExemptAppContract;
import io.github.trojan_gfw.igniter.exempt.data.AppInfo;
import io.github.trojan_gfw.igniter.exempt.data.ExemptAppDataSource;

public class ExemptAppPresenter implements ExemptAppContract.Presenter {
    private final Context mContext;
    private final ExemptAppContract.View mView;
    private final ExemptAppDataSource mDataSource;
    private boolean mDirty;
    private boolean mConfigurationChanged;
    private boolean mWorkInAllowMode;
    private List<AppInfo> mAllAppInfoList;
    private Set<String> mBlockAppPackageNameSet;
    private Set<String> mAllowAppPackageNameSet;

    public ExemptAppPresenter(Context context, ExemptAppContract.View view, ExemptAppDataSource dataSource) {
        super();
        mContext = context;
        mView = view;
        mDataSource = dataSource;
        view.setPresenter(this);
    }

    @Override
    public void updateAppInfo(AppInfo appInfo, int position, boolean isChecked) {
        mDirty = true;
        String packageName = appInfo.getPackageName();
        Set<String> packageNameSet = getAppPackageNameSet(mWorkInAllowMode);
        if (packageNameSet.contains(packageName)) {
            if (!isChecked) {
                packageNameSet.remove(packageName);
            }
        } else if (isChecked) {
            packageNameSet.add(packageName);
        }
        appInfo.setExempt(isChecked);
    }

    @UiThread
    private void displayAppList(@Nullable List<AppInfo> appInfoList) {
        if (appInfoList == null) {
            appInfoList = Collections.emptyList();
        }
        if (mWorkInAllowMode) {
            mView.showAllowAppList(appInfoList);
        } else {
            mView.showBlockAppList(appInfoList);
        }
    }

    @Override
    public void filterAppsByName(final String name) {
        if (TextUtils.isEmpty(name)) {
            displayAppList(mAllAppInfoList);
            return;
        }
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                final List<AppInfo> tmpInfoList = new ArrayList<>();
                final List<AppInfo> allAppInfoList = mAllAppInfoList;
                final String lowercaseName = name.toLowerCase();
                for (AppInfo appInfo : allAppInfoList) {
                    if (appInfo.getAppNameInLowercase().contains(lowercaseName)) {
                        tmpInfoList.add(appInfo);
                    }
                }
                Threads.instance().runOnUiThread(() -> displayAppList(tmpInfoList));
            }
        });
    }

    @Override
    public void saveExemptAppInfoList() {
        if (!mDirty) {
            mView.showSaveSuccess();
            return;
        }
        mConfigurationChanged = true;
        mView.showLoading();
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                PreferenceUtils.putBooleanPreference(mContext.getContentResolver(),
                        Uri.parse(Constants.PREFERENCE_URI),
                        Constants.PREFERENCE_KEY_PROXY_IN_ALLOW_MODE, mWorkInAllowMode);
                mDataSource.saveBlockAppInfoSet(mBlockAppPackageNameSet);
                mDataSource.saveAllowAppInfoSet(mAllowAppPackageNameSet);
                mDirty = false;
                Threads.instance().runOnUiThread(() -> {
                    mView.dismissLoading();
                    mView.showSaveSuccess();
                });
            }
        });
    }

    @Override
    public boolean handleBackPressed() {
        if (mDirty) {
            mView.showExitConfirm();
        }
        return mDirty;
    }

    @Override
    public void exit() {
        mView.exit(mConfigurationChanged);
    }

    private void showViewLoading() {
        Threads.instance().runOnUiThread(mView::showLoading);
    }

    private Set<String> getAppPackageNameSet(boolean workInAllowMode) {
        if (workInAllowMode) {
            if (mAllowAppPackageNameSet == null) {
                mAllowAppPackageNameSet = mDataSource.loadAllowAppPackageNameSet();
            }
            return mAllowAppPackageNameSet;
        } else {
            if (mBlockAppPackageNameSet == null) {
                mBlockAppPackageNameSet = mDataSource.loadBlockAppPackageNameSet();
            }
            return mBlockAppPackageNameSet;
        }
    }

    @WorkerThread
    private void loadAppListConfigInner(boolean inAllowMode) {
        showViewLoading();
        if (mAllAppInfoList == null) {
            mAllAppInfoList = mDataSource.getAllAppInfoList();
        }
        List<AppInfo> allAppInfoList = mAllAppInfoList;
        Set<String> packageNameSet = getAppPackageNameSet(inAllowMode);
        for (AppInfo appInfo : allAppInfoList) {
            appInfo.setExempt(packageNameSet.contains(appInfo.getPackageName()));
        }
        Collections.sort(allAppInfoList, AppInfo::compareTo);
        Threads.instance().runOnUiThread(() -> {
            if (inAllowMode) {
                mView.showAllowAppList(allAppInfoList);
            } else {
                mView.showBlockAppList(allAppInfoList);
            }
            mView.dismissLoading();
        });
    }

    @Override
    public void loadAllowAppListConfig() {
        if (mWorkInAllowMode) return;
        mDirty = true;
        mWorkInAllowMode = true;
        loadAppListConfigInner(true);
    }

    @Override
    public void loadBlockAppListConfig() {
        if (!mWorkInAllowMode) return;
        mDirty = true;
        mWorkInAllowMode = false;
        loadAppListConfigInner(false);
    }

    @Override
    public void start() {
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                mWorkInAllowMode = PreferenceUtils.getBooleanPreference(mContext.getContentResolver(),
                        Uri.parse(Constants.PREFERENCE_URI), Constants.PREFERENCE_KEY_PROXY_IN_ALLOW_MODE, false);
                loadAppListConfigInner(mWorkInAllowMode);
            }
        });
    }
}
