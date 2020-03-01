package io.github.trojan_gfw.igniter.servers.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.servers.contract.ServerListContract;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataManager;
import io.github.trojan_gfw.igniter.servers.fragment.ServerListFragment;
import io.github.trojan_gfw.igniter.servers.presenter.ServerListPresenter;

public class ServerListActivity extends AppCompatActivity {
    public static final String KEY_TROJAN_CONFIG = "trojan_config";
    private ServerListContract.Presenter mPresenter;

    public static Intent create(Context context) {
        return new Intent(context, ServerListActivity.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_scan_qr_code == item.getItemId()) {
            mPresenter.gotoScanQRCode();
            return true;
        } else if (R.id.action_import_from_file == item.getItemId()) {
            mPresenter.importConfigFromFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        mPresenter = new ServerListPresenter(fragment, new ServerListDataManager(getCacheDir().getAbsolutePath()));
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ServerListFragment.TAG).commitAllowingStateLoss();
    }
}
