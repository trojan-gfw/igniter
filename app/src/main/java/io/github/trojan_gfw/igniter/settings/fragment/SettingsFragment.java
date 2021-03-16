package io.github.trojan_gfw.igniter.settings.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;
import io.github.trojan_gfw.igniter.common.dialog.LoadingDialog;
import io.github.trojan_gfw.igniter.common.utils.SnackbarUtils;
import io.github.trojan_gfw.igniter.settings.InputEntryView;
import io.github.trojan_gfw.igniter.settings.contract.SettingsContract;

public class SettingsFragment extends BaseFragment
        implements SettingsContract.View, InputEntryView.Listener {
    public static final String TAG = "SettingsFragment";
    private SettingsContract.Presenter mPresenter;
    private LinearLayout mParentLl, mDNSInputLl;
    private ImageView mAddDNSIv;
    private LoadingDialog mLoadingDialog;
    private final List<InputEntryView> mExtraDNSInputList = new LinkedList<>();

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews();
        initListeners();
        mPresenter.start();
    }

    private void findViews() {
        mParentLl = findViewById(R.id.settings_parent_ll);
        mDNSInputLl = findViewById(R.id.settings_dns_list_ll);
        mAddDNSIv = findViewById(R.id.settings_add_dns_input_iv);
        FragmentActivity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            Toolbar toolbar = findViewById(R.id.settings_top_bar);
            toolbar.setTitle(R.string.settings_title);
            ((AppCompatActivity) activity).setSupportActionBar(toolbar);
            setHasOptionsMenu(true);
        }
    }

    private void initListeners() {
        mAddDNSIv.setOnClickListener(v -> mPresenter.addDNSInput());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        mPresenter.exit();
                    }
                });
    }

    @Override
    public void appendDNSInput() {
        createInputEntry("");
    }

    @Override
    public void removeDNSInput(int viewIndex) {
        InputEntryView view = (InputEntryView) mDNSInputLl.getChildAt(viewIndex);
        view.setListener(null);
        mDNSInputLl.removeView(view);
        mExtraDNSInputList.remove(view);
    }

    private void createInputEntry(String text) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        InputEntryView v = new InputEntryView(mContext);
        v.setLayoutParams(lp);
        v.setListener(this);
        v.setText(text);
        v.setError(null);
        mDNSInputLl.addView(v);
        mExtraDNSInputList.add(v);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_settings, menu);
    }

    private List<String> getInputDNSList() {
        int size = mExtraDNSInputList.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(size);
        for (InputEntryView inputEntryView : mExtraDNSInputList) {
            list.add(inputEntryView.getText().toString());
        }
        return list;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (R.id.action_save_settings == id) {
            mPresenter.saveDNSList(getInputDNSList());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showLoading() {
        runOnUiThread(() -> {
            if (mLoadingDialog == null) {
                mLoadingDialog = new LoadingDialog(requireContext());
            }
            mLoadingDialog.show();
        });
    }

    @Override
    public void dismissLoading() {
        runOnUiThread(() -> {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        });
    }

    @Override
    public void onDelete(InputEntryView view) {
        mPresenter.removeDNSInput(mDNSInputLl.indexOfChild(view));
    }

    @Override
    public void showExtraDNSList(@NonNull List<String> dnsList) {
        runOnUiThread(() -> {
            LinearLayout layout = mDNSInputLl;
            for (int i = 0, size = dnsList.size(); i < size; i++) {
                String dns = dnsList.get(i);
                createInputEntry(dns);
            }
        });
    }

    @Override
    public void showDNSFormatError(int errorIndexInList) {
        mExtraDNSInputList.get(errorIndexInList).setError(getString(R.string.settings_dns_format_error));
    }

    @Override
    public void showSettingsSaved() {
        runOnUiThread(() -> SnackbarUtils.makeTextShort(requireView(), R.string.settings_save_success)
                .setAction(R.string.common_exit, v -> mPresenter.exit())
                .show());
    }

    @Override
    public void showExitConfirm() {
        new AlertDialog.Builder(requireContext()).setMessage(R.string.settings_exit_confirm)
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    quit();
                }).show();
    }

    @Override
    public void quit() {
        finishActivity();
    }

    @Override
    public void setPresenter(SettingsContract.Presenter presenter) {
        mPresenter = presenter;
    }
}