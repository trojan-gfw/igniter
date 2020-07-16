package io.github.trojan_gfw.igniter.servers.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.TrojanHelper;
import io.github.trojan_gfw.igniter.TrojanURLHelper;
import io.github.trojan_gfw.igniter.common.utils.DecodeUtils;

public class ServerListDataManager implements ServerListDataSource {
    private final String mConfigFilePath;
    private boolean mProxyOn;
    private String mProxyHost;
    private long mProxyPort;

    public ServerListDataManager(String configFilePath, boolean proxyOn, String proxyHost, long proxyPort) {
        mConfigFilePath = configFilePath;
        mProxyOn = proxyOn;
        mProxyHost = proxyHost;
        mProxyPort = proxyPort;
    }

    @Override
    @NonNull
    public List<TrojanConfig> loadServerConfigList() {
        return new ArrayList<>(TrojanHelper.readTrojanServerConfigList(mConfigFilePath));
    }

    @Override
    public void batchDeleteServerConfigs(Collection<TrojanConfig> configs) {
        Set<String> remoteAddrSet = new HashSet<>();
        for (TrojanConfig config : configs) {
            remoteAddrSet.add(config.getRemoteAddr());
        }
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            if (remoteAddrSet.contains(trojanConfigs.get(i).getRemoteAddr())) {
                trojanConfigs.remove(i);
            }
        }
        replaceServerConfigs(trojanConfigs);
    }

    @Override
    public void deleteServerConfig(TrojanConfig config) {
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            if (trojanConfigs.get(i).getRemoteAddr().equals(config.getRemoteAddr())) {
                trojanConfigs.remove(i);
                replaceServerConfigs(trojanConfigs);
                break;
            }
        }
    }

    @Override
    public void saveServerConfig(TrojanConfig config) {
        if (config == null) {
            return;
        }
        final String remoteAddr = config.getRemoteAddr();
        if (remoteAddr == null) {
            return;
        }
        boolean configRemoteAddrExists = false;
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            TrojanConfig cacheConfig = trojanConfigs.get(i);
            if (cacheConfig == null) continue;
            if (remoteAddr.equals(cacheConfig.getRemoteAddr())) {
                trojanConfigs.set(i, config);
                configRemoteAddrExists = true;
                break;
            }
        }
        if (!configRemoteAddrExists) {
            trojanConfigs.add(config);
        }
        replaceServerConfigs(trojanConfigs);
    }

    @Override
    public void replaceServerConfigs(List<TrojanConfig> list) {
        TrojanHelper.writeTrojanServerConfigList(list, mConfigFilePath);
        TrojanHelper.ShowTrojanConfigList(mConfigFilePath);
    }

    @Override
    public void requestSubscribeServerConfigs(String urlStr, @NonNull Callback callback) {
        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            callback.onFailed();
            return;
        }
        HttpURLConnection connection = null;
        try {
            if (mProxyOn) {
                InetSocketAddress proxyAddress = new InetSocketAddress(mProxyHost, (int) mProxyPort);
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setReadTimeout(10000); // 10s timeout
            connection.setConnectTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                callback.onFailed();
                return;
            }
            try (InputStream stream = connection.getInputStream()) {
                if (stream != null) {
                    String response = DecodeUtils.decodeBase64(readStringFromStream(stream));
                    if (TextUtils.isEmpty(response)) {
                        callback.onFailed();
                    } else {
                        parseAndSaveSubscribeServers(response);
                        callback.onSuccess();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            callback.onFailed();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void parseAndSaveSubscribeServers(@NonNull String configLines) {
        int idxOfLineStart = 0, idxOfSharp = -1, idxOfLineEnd = -1;
        Map<String, TrojanConfig> configMap = new HashMap<>(60);
        for (int i = 0, len = configLines.length(); i < len; i++) {
            char c = configLines.charAt(i);
            if (c == '#') {
                idxOfSharp = i;
            } else if (c == '\n') {
                idxOfLineEnd = i;
                if (idxOfSharp != -1) {
                    String trojanUrl = configLines.substring(idxOfLineStart, idxOfSharp);
                    String remark = configLines.substring(idxOfSharp + 1, idxOfLineEnd).trim();
                    TrojanConfig config = TrojanURLHelper.ParseTrojanURL(trojanUrl);
                    if (config != null) {
                        config.setRemoteServerRemark(remark);
                        configMap.put(config.getRemoteAddr(), config);
                    }
                }
                idxOfLineStart = idxOfLineEnd + 1;
                idxOfSharp = -1;
            }
        }
        List<TrojanConfig> previousList = loadServerConfigList();
        for (int i = 0, size = previousList.size(); i < size; i++) {
            TrojanConfig config = previousList.get(i);
            String remoteAddr = config.getRemoteAddr();
            if (configMap.containsKey(remoteAddr)) {
                previousList.set(i, configMap.remove(remoteAddr));
            }
        }
        previousList.addAll(configMap.values());
        replaceServerConfigs(previousList);
    }

    private String readStringFromStream(InputStream inputStream) {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();
            int readSize;
            while ((readSize = reader.read(buf)) != -1) {
                sb.append(buf, 0, readSize);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
