package io.github.trojan_gfw.igniter.servers.contract;

import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.common.mvp.BasePresenter;
import io.github.trojan_gfw.igniter.common.mvp.BaseView;

public interface ServerListContract {
    interface Presenter extends BasePresenter {
        void handleServerSelection(TrojanConfig config);
        void deleteServerConfig(TrojanConfig config, int pos);
    }

    interface View extends BaseView<Presenter> {
        void selectServerConfig(TrojanConfig config);
        void showServerConfigList(List<TrojanConfig> configs);
        void removeServerConfig(TrojanConfig config, int pos);
    }
}
