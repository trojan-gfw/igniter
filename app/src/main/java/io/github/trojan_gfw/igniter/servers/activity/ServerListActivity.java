package io.github.trojan_gfw.igniter.servers.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import io.github.trojan_gfw.igniter.Globals;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseAppCompatActivity;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataManager;
import io.github.trojan_gfw.igniter.servers.fragment.ServerListFragment;
import io.github.trojan_gfw.igniter.servers.presenter.ServerListPresenter;

public class ServerListActivity extends BaseAppCompatActivity {
    public static final String KEY_TROJAN_CONFIG = "trojan_config";
    private static final String KEY_PROXY_ON = "proxy_on";
    private static final String KEY_PROXY_HOST = "proxy_host";
    private static final String KEY_PROXY_PORT = "proxy_port";

    public static Intent create(Context context, boolean proxyOn, String proxyHost, long proxyPort) {
        Intent intent = new Intent(context, ServerListActivity.class);
        intent.putExtra(KEY_PROXY_ON, proxyOn);
        intent.putExtra(KEY_PROXY_HOST, proxyHost);
        intent.putExtra(KEY_PROXY_PORT, proxyPort);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        FragmentManager fm = getSupportFragmentManager();
        ServerListFragment fragment = (ServerListFragment) fm.findFragmentByTag(ServerListFragment.TAG);
        if (fragment == null) {
            fragment = ServerListFragment.newInstance();
        }
        Intent intent = getIntent();
        boolean proxyOn = intent.getBooleanExtra(KEY_PROXY_ON, false);
        String proxyHost = intent.getStringExtra(KEY_PROXY_HOST);
        long proxyPort = intent.getLongExtra(KEY_PROXY_PORT, 0L);
        new ServerListPresenter(fragment, new ServerListDataManager(Globals.getTrojanConfigListPath(), proxyOn, proxyHost, proxyPort));
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ServerListFragment.TAG)
                .commitAllowingStateLoss();
    }
}
