package io.github.trojan_gfw.igniter.servers.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;
import java.util.Set;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.common.mvp.BasePresenter;
import io.github.trojan_gfw.igniter.common.mvp.BaseView;

public interface ServerListContract {
    interface Presenter extends BasePresenter {
        void addServerConfig(String trojanUrl);
        void handleServerSelection(TrojanConfig config);
        void gotoScanQRCode();
        void displayImportFileDescription();
        void hideImportFileDescription();
        void importConfigFromFile();
        void parseConfigsInFileStream(Context context, Uri fileUri);
        void exportServerListToFile();
        void batchOperateServerList();
        void exitServerListBatchOperation();
        void selectServer(TrojanConfig config, boolean checked);
        void selectAll(List<TrojanConfig> configList);
        void deselectAll(List<TrojanConfig> configList);
        void batchDelete();
        void displaySubscribeSettings();
        void updateSubscribeServers();
        void saveSubscribeSettings(String url);
        void hideSubscribeSettings();
        void saveServerList(List<TrojanConfig> configList);
    }

    interface View extends BaseView<Presenter> {
        void showAddTrojanConfigSuccess();
        void showQRCodeScanError(String scanContent);
        void selectServerConfig(TrojanConfig config);
        void showServerConfigList(List<TrojanConfig> configs);
        void removeServerConfig(TrojanConfig config, int pos);
        void gotoScanQRCode();
        void showImportFileDescription();
        void dismissImportFileDescription();
        void openFileChooser();
        void showExportServerListSuccess();
        void showExportServerListFailure();
        void showServerListBatchOperation();
        void hideServerListBatchOperation();
        void selectAllServers();
        void deselectAllServers();
        void showBatchDeletionSuccess();
        void showLoading();
        void dismissLoading();
        void batchDelete(Set<TrojanConfig> configList);
        void showSubscribeSettings(String url);
        void dismissSubscribeSettings();
        void showSubscribeUpdateSuccess();
        void showSubscribeUpdateFailed();
    }
}
