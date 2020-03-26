package io.github.trojan_gfw.igniter.servers.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;
import io.github.trojan_gfw.igniter.qrcode.ScanQRCodeActivity;
import io.github.trojan_gfw.igniter.servers.activity.ServerListActivity;
import io.github.trojan_gfw.igniter.servers.contract.ServerListContract;

public class ServerListFragment extends BaseFragment implements ServerListContract.View {
    private static final int REQUEST_READ_PERMISSION_CODE = 115;
    private static final int FILE_IMPORT_REQUEST_CODE = 120;
    private static final int SCAN_QR_CODE_REQUEST_CODE = 110;
    private static final int REQUEST_CAMERA_CODE = 114;
    public static final String TAG = "ServerListFragment";
    public static final String KEY_TROJAN_CONFIG = ServerListActivity.KEY_TROJAN_CONFIG;
    private ServerListContract.Presenter mPresenter;
    private RecyclerView mServerListRv;
    private ServerListAdapter mServerListAdapter;

    public ServerListFragment() {
        // Required empty public constructor
    }

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_server_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews();
        initViews();
        initListeners();
        mPresenter.start();
    }

    private void findViews() {
        mServerListRv = findViewById(R.id.serverListRv);
    }

    private void initViews() {
        mServerListRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mServerListAdapter = new ServerListAdapter(getContext(), new ArrayList<TrojanConfig>());
        mServerListRv.setAdapter(mServerListAdapter);
    }

    private void initListeners() {
        mServerListAdapter.setOnItemClickListener(new ServerListAdapter.OnItemClickListener() {
            @Override
            public void onItemSelected(TrojanConfig config, int pos) {
                mPresenter.handleServerSelection(config);
            }

            @Override
            public void onItemDelete(TrojanConfig config, int pos) {
                mPresenter.deleteServerConfig(config, pos);
            }
        });
    }

    private Context getApplicationContext() {
        if (getActivity() != null) {
            return getActivity().getApplicationContext();
        }
        return null;
    }

    @Override
    public void showAddTrojanConfigSuccess() {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), R.string.scan_qr_code_success, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void showQRCodeScanError(final String scanContent) {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.scan_qr_code_failed, scanContent), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void gotoScanQRCode() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)) {
            gotoScanQRCodeInner();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
        }
    }

    private void gotoScanQRCodeInner() {
        startActivityForResult(ScanQRCodeActivity.create(mContext), SCAN_QR_CODE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SCAN_QR_CODE_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK && data != null) {
            mPresenter.addServerConfig(data.getStringExtra(ScanQRCodeActivity.KEY_SCAN_CONTENT));
        } else if (FILE_IMPORT_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                mPresenter.parseConfigsInFileStream(getContext(), uri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQUEST_CAMERA_CODE == requestCode) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                gotoScanQRCodeInner();
            } else {
                Toast.makeText(getContext(), R.string.server_list_lack_of_camera_permission, Toast.LENGTH_SHORT).show();
            }
        } else if (REQUEST_READ_PERMISSION_CODE == requestCode) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                openFileChooser();
            } else {
                Toast.makeText(getContext(), R.string.server_list_lack_of_read_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void selectServerConfig(TrojanConfig config) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent();
            intent.putExtra(KEY_TROJAN_CONFIG, config);
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent()
                .setType("text/plain")
                .setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.server_list_file_chooser_msg)), FILE_IMPORT_REQUEST_CODE);
    }

    @Override
    public void importConfigFromFile() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openFileChooser();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_PERMISSION_CODE);
        }
    }

    @Override
    public void showServerConfigList(final List<TrojanConfig> configs) {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                mServerListAdapter.replaceData(configs);
            }
        });
    }

    @Override
    public void removeServerConfig(TrojanConfig config, final int pos) {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                mServerListAdapter.removeItemOnPosition(pos);
            }
        });
    }

    @Override
    public void setPresenter(ServerListContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
