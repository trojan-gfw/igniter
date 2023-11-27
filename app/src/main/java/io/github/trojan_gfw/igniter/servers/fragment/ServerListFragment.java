package io.github.trojan_gfw.igniter.servers.fragment;

import android.Manifest;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.trojan_gfw.igniter.Globals;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;
import io.github.trojan_gfw.igniter.common.dialog.LoadingDialog;
import io.github.trojan_gfw.igniter.common.utils.SnackbarUtils;
import io.github.trojan_gfw.igniter.qrcode.ScanQRCodeActivity;
import io.github.trojan_gfw.igniter.servers.ItemVerticalMoveCallback;
import io.github.trojan_gfw.igniter.servers.SubscribeSettingDialog;
import io.github.trojan_gfw.igniter.servers.activity.ServerListActivity;
import io.github.trojan_gfw.igniter.servers.contract.ServerListContract;

import static android.app.Activity.RESULT_OK;

public class ServerListFragment extends BaseFragment implements ServerListContract.View {
    public static final String TAG = "ServerListFragment";
    public static final String KEY_TROJAN_CONFIG = ServerListActivity.KEY_TROJAN_CONFIG;

    private ActivityResultLauncher<String> scanQRCodeRequestPermissionStartActivityLaunch;
    private ActivityResultLauncher<String> importConfigStartActivityLaunch;
    private ActivityResultLauncher<Intent> scanQRCodeGotResultStartActivityLaunch;

    private ServerListContract.Presenter mPresenter;
    private RecyclerView mServerListRv;
    private ItemTouchHelper mItemTouchHelper;
    private ServerListAdapter mServerListAdapter;
    private Dialog mImportConfigDialog;
    private Dialog mLoadingDialog;
    private boolean mBatchOperationMode;

    public ServerListFragment() {
        // Required empty public constructor
    }

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanQRCodeRequestPermissionStartActivityLaunch = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if (result) {
                            scanQRCodeGotResultStartActivityLaunch.launch(ScanQRCodeActivity.create(mContext, false));
                        } else {
                            Toast.makeText(mContext.getApplicationContext(), R.string.server_list_lack_of_camera_permission, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        importConfigStartActivityLaunch = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            mPresenter.parseConfigsInFileStream(getContext(), uri);
                        }
                    }
                });

        scanQRCodeGotResultStartActivityLaunch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data == null) return;
                            mPresenter.addServerConfig(data.getStringExtra(ScanQRCodeActivity.KEY_SCAN_CONTENT));
                        }
                    }
                });
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
            ((AppCompatActivity) activity).setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
            setHasOptionsMenu(true);
        }
        mServerListRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mServerListRv.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        mServerListAdapter = new ServerListAdapter(getContext(), new ArrayList<TrojanConfig>());
        mItemTouchHelper = new ItemTouchHelper(new ItemVerticalMoveCallback(mServerListAdapter));
        mServerListRv.setAdapter(mServerListAdapter);
    }

    private void initListeners() {
        mServerListAdapter.setOnItemClickListener(new ServerListAdapter.OnItemClickListener() {
            @Override
            public void onItemSelected(TrojanConfig config, int pos) {
                mPresenter.handleServerSelection(config);
            }

            @Override
            public void onItemBatchSelected(TrojanConfig config, int pos, boolean checked) {
                mPresenter.selectServer(config, checked);
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
    public void askTheWayToScanQRCode() {
        new AlertDialog.Builder(mContext)
                .setItems(R.array.scan_qr_code_choices,
                        (dialog, which) -> mPresenter.gotoScanQRCode(1 == which))
                .show();
    }

    @Override
    public void scanQRCodeFromCamera() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)) {
            scanQRCodeGotResultStartActivityLaunch.launch(ScanQRCodeActivity.create(mContext, false));
        } else {
            scanQRCodeRequestPermissionStartActivityLaunch.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void scanQRCodeFromGallery() {
        scanQRCodeGotResultStartActivityLaunch.launch(ScanQRCodeActivity.create(mContext, true));
    }

    @Override
    public void selectServerConfig(TrojanConfig config) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent();
            intent.putExtra(KEY_TROJAN_CONFIG, config);
            activity.setResult(RESULT_OK, intent);
            activity.finish();
        }
    }

    private SubscribeSettingDialog mSubscribeSettingDialog;

    @Override
    public void showSubscribeSettings(String url) {
        if (mSubscribeSettingDialog == null) {
            mSubscribeSettingDialog = new SubscribeSettingDialog(mContext);
            mSubscribeSettingDialog.setOnButtonClickListener(new SubscribeSettingDialog.OnButtonClickListener() {
                @Override
                public void onConfirm(String url) {
                    mPresenter.saveSubscribeSettings(url);
                    mPresenter.hideSubscribeSettings();
                }

                @Override
                public void onCancel() {
                    mPresenter.hideSubscribeSettings();
                }
            });
        }
        mSubscribeSettingDialog.setSubscribeUrl(url);
        mSubscribeSettingDialog.show();
    }

    @Override
    public void dismissSubscribeSettings() {
        if (mSubscribeSettingDialog != null && mSubscribeSettingDialog.isShowing()) {
            mSubscribeSettingDialog.dismiss();
        }
    }

    @Override
    public void showSubscribeUpdateSuccess() {
        SnackbarUtils.showTextShort(mRootView, R.string.subscribe_servers_success);
    }

    @Override
    public void showSubscribeUpdateFailed() {
        SnackbarUtils.showTextShort(mRootView, R.string.subscribe_servers_failed);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_server_list, menu);
        MenuItem qrCodeItem = menu.findItem(R.id.action_scan_qr_code).setVisible(!mBatchOperationMode);
        menu.findItem(R.id.action_import_from_file).setVisible(!mBatchOperationMode);
        menu.findItem(R.id.action_export_to_file).setVisible(!mBatchOperationMode);
        menu.findItem(R.id.action_enter_batch_mode).setVisible(!mBatchOperationMode);
        menu.findItem(R.id.action_subscribe_settings).setVisible(!mBatchOperationMode);
        menu.findItem(R.id.action_subscribe_servers).setVisible(!mBatchOperationMode);
        menu.findItem(R.id.action_exit_batch_operation).setVisible(mBatchOperationMode);
        menu.findItem(R.id.action_select_all_servers).setVisible(mBatchOperationMode);
        menu.findItem(R.id.action_deselect_all_servers).setVisible(mBatchOperationMode);
        menu.findItem(R.id.action_batch_delete_servers).setVisible(mBatchOperationMode);
        menu.findItem(R.id.action_test_all_proxy_server).setVisible(!mBatchOperationMode);
        // Tint scan QRCode icon to white.
        if (qrCodeItem.getIcon() != null) {
            Drawable drawable = qrCodeItem.getIcon();
            Drawable wrapper = DrawableCompat.wrap(drawable);
            drawable.mutate();
            DrawableCompat.setTint(wrapper, ContextCompat.getColor(mContext, android.R.color.white));
            qrCodeItem.setIcon(drawable);
        }
    }

    @Override
    public void showServerListBatchOperation() {
        enableBatchOperationMode(true);
    }

    @Override
    public void hideServerListBatchOperation() {
        enableBatchOperationMode(false);
        mServerListAdapter.setAllSelected(false);
    }

    private void enableBatchOperationMode(boolean enable) {
        mBatchOperationMode = enable;
        requireActivity().invalidateOptionsMenu();
        mServerListAdapter.setBatchDeleteMode(enable);
        mItemTouchHelper.attachToRecyclerView(enable ? mServerListRv : null);
    }

    @Override
    public void showBatchDeletionSuccess() {
        SnackbarUtils.showTextShort(mRootView, R.string.batch_delete_server_list_success);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_scan_qr_code){
            askTheWayToScanQRCode();
            return true;
        }
        if (item.getItemId() == R.id.action_import_from_file){
            mPresenter.displayImportFileDescription();
            return true;
        }
        if (item.getItemId() == R.id.action_export_to_file){
            mPresenter.exportServerListToFile();
            return true;
        }
        if (item.getItemId() == R.id.action_enter_batch_mode){
            mPresenter.batchOperateServerList();
            return true;
        }
        if (item.getItemId() == R.id.action_exit_batch_operation){
            mPresenter.saveServerList(mServerListAdapter.getData());
            mPresenter.exitServerListBatchOperation();
            return true;
        }
        if (item.getItemId() == R.id.action_select_all_servers){
            mPresenter.selectAll(mServerListAdapter.getData());
            return true;
        }
        if (item.getItemId() == R.id.action_deselect_all_servers){
            mPresenter.deselectAll(mServerListAdapter.getData());
            return true;
        }
        if (item.getItemId() == R.id.action_batch_delete_servers){
            mPresenter.batchDelete();
            return true;
        }
        if (item.getItemId() == R.id.action_subscribe_settings){
            mPresenter.displaySubscribeSettings();
            return true;
        }
        if (item.getItemId() == R.id.action_subscribe_servers){
            mPresenter.updateSubscribeServers();
            return true;
        }
        if (item.getItemId() == R.id.action_test_all_proxy_server){
            mPresenter.pingAllProxyServer(mServerListAdapter.getData());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(mContext);
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setCanceledOnTouchOutside(false);
        }
        mLoadingDialog.show();
    }

    @Override
    public void dismissLoading() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void batchDelete(Set<TrojanConfig> configList) {
        mServerListAdapter.deleteServers(configList);
    }

    @Override
    public void selectAllServers() {
        mServerListAdapter.setAllSelected(true);
    }

    @Override
    public void deselectAllServers() {
        mServerListAdapter.setAllSelected(false);
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
        importConfigStartActivityLaunch.launch("*/*");
    }

    @Override
    public void showExportServerListSuccess() {
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.export_server_list_success, Globals.getIgniterExportPath()), Toast.LENGTH_LONG).show();
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

    @Override
    public void setPingServerDelayTime(TrojanConfig config, float timeout) {
        mServerListAdapter.setPingServerDelayTime(config, timeout);
    }
}
