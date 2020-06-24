package io.github.trojan_gfw.igniter.servers.data;

import androidx.annotation.Nullable;

import io.github.trojan_gfw.igniter.TrojanConfig;

public class TrojanConfigWrapper extends TrojanConfig {
    private final TrojanConfig mDelegate;
    private boolean mSelected;

    public TrojanConfigWrapper(TrojanConfig delegate) {
        mDelegate = delegate;
    }

    public TrojanConfig getDelegate() {
        return mDelegate;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    @Override
    public String generateTrojanConfigJSON() {
        return mDelegate.generateTrojanConfigJSON();
    }

    @Override
    public void fromJSON(String jsonStr) {
        mDelegate.fromJSON(jsonStr);
    }

    @Override
    public void copyFrom(TrojanConfig that) {
        mDelegate.copyFrom(that);
    }

    @Override
    public boolean isValidRunningConfig() {
        return mDelegate.isValidRunningConfig();
    }

    @Override
    public String getLocalAddr() {
        return mDelegate.getLocalAddr();
    }

    @Override
    public TrojanConfig setLocalAddr(String localAddr) {
        return mDelegate.setLocalAddr(localAddr);
    }

    @Override
    public int getLocalPort() {
        return mDelegate.getLocalPort();
    }

    @Override
    public TrojanConfig setLocalPort(int localPort) {
        return mDelegate.setLocalPort(localPort);
    }

    @Override
    public String getRemoteServerRemark() {
        return mDelegate.getRemoteServerRemark();
    }

    @Override
    public TrojanConfig setRemoteServerRemark(String remoteServerRemark) {
        return mDelegate.setRemoteServerRemark(remoteServerRemark);
    }

    @Override
    public String getRemoteAddr() {
        return mDelegate.getRemoteAddr();
    }

    @Override
    public TrojanConfig setRemoteAddr(String remoteAddr) {
        return mDelegate.setRemoteAddr(remoteAddr);
    }

    @Override
    public int getRemotePort() {
        return mDelegate.getRemotePort();
    }

    @Override
    public TrojanConfig setRemotePort(int remotePort) {
        return mDelegate.setRemotePort(remotePort);
    }

    @Override
    public String getPassword() {
        return mDelegate.getPassword();
    }

    @Override
    public TrojanConfig setPassword(String password) {
        return mDelegate.setPassword(password);
    }

    @Override
    public boolean getVerifyCert() {
        return mDelegate.getVerifyCert();
    }

    @Override
    public TrojanConfig setVerifyCert(boolean verifyCert) {
        return mDelegate.setVerifyCert(verifyCert);
    }

    @Override
    public String getCaCertPath() {
        return mDelegate.getCaCertPath();
    }

    @Override
    public TrojanConfig setCaCertPath(String caCertPath) {
        return mDelegate.setCaCertPath(caCertPath);
    }

    @Override
    public boolean getEnableIpv6() {
        return mDelegate.getEnableIpv6();
    }

    @Override
    public TrojanConfig setEnableIpv6(boolean enableIpv6) {
        return mDelegate.setEnableIpv6(enableIpv6);
    }

    @Override
    public String getCipherList() {
        return mDelegate.getCipherList();
    }

    @Override
    public TrojanConfig setCipherList(String cipherList) {
        return mDelegate.setCipherList(cipherList);
    }

    @Override
    public String getTls13CipherList() {
        return mDelegate.getTls13CipherList();
    }

    @Override
    public TrojanConfig setTls13CipherList(String tls13CipherList) {
        return mDelegate.setTls13CipherList(tls13CipherList);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return mDelegate.equals(obj);
    }
}
