package io.github.trojan_gfw.igniter.settings.presenter;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.settings.contract.SettingsContract;
import io.github.trojan_gfw.igniter.settings.data.ISettingsDataManager;

public class SettingsPresenter implements SettingsContract.Presenter {
    private final SettingsContract.View mView;
    private final ISettingsDataManager mDataManager;
    private boolean mHasModifiedDNS;

    public SettingsPresenter(SettingsContract.View view, ISettingsDataManager dataManager) {
        mView = view;
        mDataManager = dataManager;
        view.setPresenter(this);
    }

    @Override
    public void saveDNSList(@NonNull List<String> dnsList) {
        mView.showLoading();
        Pattern pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
        for (int i = 0, size = dnsList.size(); i < size; i++) {
            if (!pattern.matcher(dnsList.get(i)).matches()) {
                mView.dismissLoading();
                mView.showDNSFormatError(i);
                return;
            }
        }
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                mDataManager.saveExtraDNSList(dnsList);
                mHasModifiedDNS = false;
                mView.dismissLoading();
                mView.showSettingsSaved();
            }
        });
    }

    @Override
    public void addDNSInput() {
        mView.appendDNSInput();
        mHasModifiedDNS = true;
    }

    @Override
    public void removeDNSInput(int index) {
        mView.removeDNSInput(index);
        mHasModifiedDNS = true;
    }

    private boolean configIsDirty() {
        return mHasModifiedDNS;
    }

    @Override
    public void exit() {
        if (configIsDirty()) {
            mView.showExitConfirm();
        } else {
            mView.quit();
        }
    }

    @Override
    public void start() {
        mView.showLoading();
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                List<String> dnsList = mDataManager.loadExtraDNSList();
                mView.showExtraDNSList(dnsList);
                mView.dismissLoading();
            }
        });
    }
}
