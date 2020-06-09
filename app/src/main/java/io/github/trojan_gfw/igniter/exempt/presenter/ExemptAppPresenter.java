package io.github.trojan_gfw.igniter.exempt.presenter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.github.trojan_gfw.igniter.common.constants.Constants;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.PreferenceUtils;
import io.github.trojan_gfw.igniter.exempt.contract.ExemptAppContract;
import io.github.trojan_gfw.igniter.exempt.data.AppInfo;
import io.github.trojan_gfw.igniter.exempt.data.ExemptAppDataSource;

public class ExemptAppPresenter implements ExemptAppContract.Presenter {
    private static final String KEY_CHECK_EXTERNAL_EXEMPTED_APP_LIST_CONFIG = "check_external_exempted";
    private final Context mContext;
    private final ExemptAppContract.View mView;
    private final ExemptAppDataSource mDataSource;
    private boolean mDirty;
    private boolean mConfigurationChanged;
    private List<AppInfo> mAllAppInfoList;
    private Set<String> mExemptAppPackageNameSet;

    public ExemptAppPresenter(Context context, ExemptAppContract.View view, ExemptAppDataSource dataSource) {
        super();
        mContext = context;
        mView = view;
        mDataSource = dataSource;
        view.setPresenter(this);
    }

    @Override
    public void updateAppInfo(AppInfo appInfo, int position, boolean exempt) {
        mDirty = true;
        String packageName = appInfo.getPackageName();
        if (mExemptAppPackageNameSet.contains(packageName)) {
            if (!exempt) {
                mExemptAppPackageNameSet.remove(packageName);
            }
        } else if (exempt) {
            mExemptAppPackageNameSet.add(packageName);
        }
        appInfo.setExempt(exempt);
    }

    @Override
    public void filterAppsByName(final String name) {
        if (TextUtils.isEmpty(name)) {
            mView.showAppList(mAllAppInfoList);
            return;
        }
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                final List<AppInfo> tmpInfoList = new ArrayList<>();
                final String lowercaseName = name.toLowerCase();
                for (AppInfo appInfo : mAllAppInfoList) {
                    if (appInfo.getAppNameInLowercase().contains(lowercaseName)) {
                        tmpInfoList.add(appInfo);
                    }
                }
                Threads.instance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mView.showAppList(tmpInfoList);
                    }
                });
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
                mDataSource.saveExemptAppInfoSet(mExemptAppPackageNameSet);
                mDirty = false;
                Threads.instance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mView.dismissLoading();
                        mView.showSaveSuccess();
                    }
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

    @Override
    public void migrateExternalExemptedAppListFileToPrivateDirectory() {
        mView.showLoading();
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                mDataSource.migrateExternalExemptAppInfo();
                mDataSource.deleteExternalExemptAppInfo();
                loadExemptedAppListConfig();
            }
        });
    }

    @Override
    public void ignoreExternalExemptedAppListConfigForever() {
        PreferenceUtils.putBooleanPreference(mContext.getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI), KEY_CHECK_EXTERNAL_EXEMPTED_APP_LIST_CONFIG,
                false);
    }

    @Override
    public void loadExemptedAppListConfig() {
        mView.showLoading();
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                final List<AppInfo> allAppInfoList = mDataSource.getAllAppInfoList();
                mExemptAppPackageNameSet = mDataSource.loadExemptAppPackageNameSet();
                for (AppInfo appInfo : allAppInfoList) {
                    if (mExemptAppPackageNameSet.contains(appInfo.getPackageName())) {
                        appInfo.setExempt(true);
                    }
                }
                // cluster exempted apps.
                Collections.sort(allAppInfoList, new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo o1, AppInfo o2) {
                        if (o1.isExempt() != o2.isExempt()) {
                            return o1.isExempt() ? -1 : 1;
                        }
                        return o1.getAppName().compareTo(o2.getAppName());
                    }
                });
                mAllAppInfoList = allAppInfoList;
                Threads.instance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mView.showAppList(allAppInfoList);
                        mView.dismissLoading();
                    }
                });
            }
        });
    }

    private boolean needCheckExternalExemptedAppListConfig() {
        return PreferenceUtils.getBooleanPreference(mContext.getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI), KEY_CHECK_EXTERNAL_EXEMPTED_APP_LIST_CONFIG,
                true);
    }

    @Override
    public void start() {
        if (needCheckExternalExemptedAppListConfig() &&
                mDataSource.checkExternalExemptAppInfoConfigExistence()) {
            mView.showExemptedAppListMigrationNotice();
        } else {
            loadExemptedAppListConfig();
        }
    }
}
