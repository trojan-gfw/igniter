package io.github.trojan_gfw.igniter.servers.presenter;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.TrojanURLHelper;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
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
    public void importConfigFromFile() {
        mView.importConfigFromFile();
    }

    @Override
    public void parseConfigsInFileStream(final Context context, final Uri fileUri) {
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(fileUri)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<TrojanConfig> trojanConfigs = parseTrojanConfigsFromFileContent(sb.toString());
                List<TrojanConfig> currentConfigs = mDataManager.loadServerConfigList();
                currentConfigs.addAll(trojanConfigs);
                // remove repeated configurations
                Set<String> newTrojanConfigRemoteAddrSet = new HashSet<>();
                for (TrojanConfig config : trojanConfigs) {
                    newTrojanConfigRemoteAddrSet.add(config.getRemoteAddr());
                }
                for (int i = currentConfigs.size() - 1; i >= 0; i--) {
                    if (newTrojanConfigRemoteAddrSet.contains(currentConfigs.get(i).getRemoteAddr())) {
                        currentConfigs.remove(i);
                    }
                }
                currentConfigs.addAll(trojanConfigs);
                mDataManager.replaceServerConfigs(currentConfigs);
                loadConfigs();
                mView.showAddTrojanConfigSuccess();
            }
        });
    }

    private List<TrojanConfig> parseTrojanConfigsFromFileContent(String fileContent) {
        try {
            JSONObject jsonObject = new JSONObject(fileContent);
            JSONArray configs = jsonObject.optJSONArray("configs");
            if (configs == null) {
                return Collections.emptyList();
            }
            final int len = configs.length();
            List<TrojanConfig> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                JSONObject config = configs.getJSONObject(i);
                String remoteAddr = config.optString("server", null);
                if (remoteAddr == null) {
                    continue;
                }
                TrojanConfig tmp = new TrojanConfig();
                tmp.setRemoteAddr(remoteAddr);
                tmp.setRemotePort(config.optInt("server_port"));
                tmp.setPassword(config.optString("password"));
                tmp.setVerifyCert(config.optBoolean("verify"));
                list.add(tmp);
            }
            return list;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void addServerConfig(final String trojanUrl) {
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                TrojanConfig config = TrojanURLHelper.ParseTrojanURL(trojanUrl);
                if (config != null) {
                    mDataManager.saveServerConfig(config);
                    loadConfigs();
                    mView.showAddTrojanConfigSuccess();
                } else {
                    mView.showQRCodeScanError(trojanUrl);
                }
            }
        });
    }

    @Override
    public void handleServerSelection(TrojanConfig config) {
        mView.selectServerConfig(config);
    }

    @Override
    public void deleteServerConfig(final TrojanConfig config, final int pos) {
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                mDataManager.deleteServerConfig(config);
                mView.removeServerConfig(config, pos);
            }
        });
    }

    @Override
    public void start() {
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                loadConfigs();
            }
        });
    }

    @Override
    public void gotoScanQRCode() {
        mView.gotoScanQRCode();
    }

    private void loadConfigs() {
        List<TrojanConfig> trojanConfigs = mDataManager.loadServerConfigList();
        mView.showServerConfigList(trojanConfigs);
    }
}
