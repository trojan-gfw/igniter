package io.github.trojan_gfw.igniter.exempt.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;
import io.github.trojan_gfw.igniter.common.dialog.LoadingDialog;
import io.github.trojan_gfw.igniter.common.utils.SnackbarUtils;
import io.github.trojan_gfw.igniter.exempt.adapter.AppInfoAdapter;
import io.github.trojan_gfw.igniter.exempt.contract.ExemptAppContract;
import io.github.trojan_gfw.igniter.exempt.data.AppInfo;

public class ExemptAppFragment extends BaseFragment implements ExemptAppContract.View {
    public static final String TAG = "ExemptAppFragment";
    private ExemptAppContract.Presenter mPresenter;
    private Toolbar mTopBar;
    private RecyclerView mAppRv;
    private AppInfoAdapter mAppInfoAdapter;
    private LoadingDialog mLoadingDialog;
    private TabLayout mWorkModeTl;

    public ExemptAppFragment() {
        // Required empty public constructor
    }

    public static ExemptAppFragment newInstance() {
        return new ExemptAppFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exempt_app, container, false);
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
        mTopBar = findViewById(R.id.exemptAppTopBar);
        mAppRv = findViewById(R.id.exemptAppRv);
        mWorkModeTl = findViewById(R.id.exemptAppWorkModeTabLayout);
    }

    private void initViews() {
        FragmentActivity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).setSupportActionBar(mTopBar);
            setHasOptionsMenu(true);
        }
        mAppInfoAdapter = new AppInfoAdapter();
        mAppRv.setAdapter(mAppInfoAdapter);
        mAppRv.addItemDecoration(new DividerItemDecoration(mContext, LinearLayoutManager.VERTICAL));
    }

    private void initListeners() {
        mAppInfoAdapter.setOnItemOperationListener(new AppInfoAdapter.OnItemOperationListener() {
            @Override
            public void onToggle(boolean exempt, AppInfo appInfo, int position) {
                mPresenter.updateAppInfo(appInfo, position, exempt);
            }
        });
        mWorkModeTl.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) { // block mode
                    mPresenter.loadBlockAppListConfig();
                } else {
                    mPresenter.loadAllowAppListConfig();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void showAllowAppList(List<AppInfo> packagesNames) {
        mWorkModeTl.selectTab(mWorkModeTl.getTabAt(1));
        mAppInfoAdapter.refreshData(packagesNames);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_exempt_app, menu);

        MenuItem item = menu.findItem(R.id.action_search_app);
        SearchView searchView = null;
        if (item != null) {
            searchView = (SearchView) item.getActionView();
        }
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    mPresenter.filterAppsByName(s);
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_exempt_apps) {
            mPresenter.saveExemptAppInfoList();
            return true;
        }
        return false;
    }

    @Override
    public void showSaveSuccess() {
        SnackbarUtils.showTextShort(mRootView, R.string.common_save_success, R.string.exempt_app_exit, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.exit();
            }
        });
    }

    @Override
    public void showExitConfirm() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.common_alert)
                .setMessage(R.string.exempt_app_exit_without_saving_confirm)
                .setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.common_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mPresenter.exit();
                    }
                }).create().show();
    }

    @Override
    public void showBlockAppList(final List<AppInfo> appInfoList) {
        mWorkModeTl.selectTab(mWorkModeTl.getTabAt(0));
        mAppInfoAdapter.refreshData(appInfoList);
    }

    @Override
    public void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(requireContext());
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setCanceledOnTouchOutside(false);
            mLoadingDialog.setMsg(getString(R.string.exempt_app_loading_tip));
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
    public void exit(boolean configurationChanged) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setResult(configurationChanged ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
            activity.finish();
        }
    }

    @Override
    public void setPresenter(ExemptAppContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
