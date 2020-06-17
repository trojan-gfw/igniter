package io.github.trojan_gfw.igniter.servers.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;
import io.github.trojan_gfw.igniter.qrcode.ScanQRCodeActivity;
import io.github.trojan_gfw.igniter.servers.activity.ServerListActivity;
import io.github.trojan_gfw.igniter.servers.contract.ServerListContract;

public class ServerListFragment extends BaseFragment implements ServerListContract.View {
    private static final int FILE_IMPORT_REQUEST_CODE = 120;
    private static final int SCAN_QR_CODE_REQUEST_CODE = 110;
    private static final int REQUEST_CAMERA_CODE = 114;
    public static final String TAG = "ServerListFragment";
    public static final String KEY_TROJAN_CONFIG = ServerListActivity.KEY_TROJAN_CONFIG;
    private ServerListContract.Presenter mPresenter;
    private RecyclerView mServerListRv;
    private ServerListAdapter mServerListAdapter;
    private Dialog mImportConfigDialog;

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
        FragmentActivity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
            setHasOptionsMenu(true);
        }
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



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_server_list, menu);
        MenuItem item = menu.getItem(0);
        // Tint scan QRCode icon to white.
        if (item.getIcon() != null) {
            Drawable drawable = item.getIcon();
            Drawable wrapper = DrawableCompat.wrap(drawable);
            drawable.mutate();
            DrawableCompat.setTint(wrapper, ContextCompat.getColor(mContext, android.R.color.white));
            item.setIcon(drawable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan_qr_code:
                mPresenter.gotoScanQRCode();
                return true;
            case R.id.action_import_from_file:
                mPresenter.displayImportFileDescription();
                return true;
            case R.id.action_export_to_file:
                mPresenter.exportServerListToFile();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showImportFileDescription() {
        mImportConfigDialog = new AlertDialog.Builder(mContext).setTitle(R.string.common_alert)
                .setMessage(R.string.server_list_import_file_desc)
                .setPositiveButton(R.string.common_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.importConfigFromFile();
                    }
                }).setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.hideImportFileDescription();
                    }
                }).create();
        mImportConfigDialog.show();
    }

    @Override
    public void dismissImportFileDescription() {
        if (mImportConfigDialog != null && mImportConfigDialog.isShowing()) {
            mImportConfigDialog.dismiss();
            mImportConfigDialog = null;
        }
    }

    @Override
    public void openFileChooser() {
        Intent intent = new Intent()
                .setType("text/plain")
                .setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.server_list_file_chooser_msg)), FILE_IMPORT_REQUEST_CODE);
    }

    @Override
    public void showExportServerListSuccess() {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.export_server_list_success), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void showExportServerListFailure() {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.export_server_list_error), Toast.LENGTH_SHORT).show();
            }
        });
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
