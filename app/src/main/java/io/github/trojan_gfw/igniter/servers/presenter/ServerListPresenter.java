package io.github.trojan_gfw.igniter.servers.presenter;

import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.servers.contract.ServerListContract;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataSource;

public class ServerListPresenter implements ServerListContract.Presenter {
    private final ServerListContract.View mView;
    private final ServerListDataSource mDataManager;

    public ServerListPresenter(ServerListContract.View view, ServerListDataSource dataManager) {
        mView = view;
        mDataManager = dataManager;
        view.setPresenter(this);
    }

    @Override
    public void handleServerSelection(TrojanConfig config) {
        mView.selectServerConfig(config);
    }

    @Override
    public void deleteServerConfig(final TrojanConfig config, final int pos) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mDataManager.deleteServerConfig(config);
                mView.removeServerConfig(config, pos);
            }
        }).start();
    }

    @Override
    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadConfigs();
            }
        }).start();
    }

    private void loadConfigs() {
        List<TrojanConfig> trojanConfigs = mDataManager.loadServerConfigList();
        mView.showServerConfigList(trojanConfigs);
    }
}
