package io.github.trojan_gfw.igniter.exempt.presenter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.exempt.contract.ExemptAppContract;
import io.github.trojan_gfw.igniter.exempt.data.AppInfo;
import io.github.trojan_gfw.igniter.exempt.data.ExemptAppDataSource;

public class ExemptAppPresenter implements ExemptAppContract.Presenter {
    private final ExemptAppContract.View mView;
    private final ExemptAppDataSource mDataSource;
    private boolean mDirty;
    private boolean mConfigurationChanged;
    private Set<String> mExemptAppPackageNameSet;

    public ExemptAppPresenter(ExemptAppContract.View view, ExemptAppDataSource dataSource) {
        super();
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
    public void start() {
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
}
