package io.github.trojan_gfw.igniter.settings.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseAppCompatActivity;
import io.github.trojan_gfw.igniter.settings.data.SettingsDataManager;
import io.github.trojan_gfw.igniter.settings.fragment.SettingsFragment;
import io.github.trojan_gfw.igniter.settings.presenter.SettingsPresenter;

public class SettingsActivity extends BaseAppCompatActivity {

    public static Intent create(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FragmentManager fm = getSupportFragmentManager();
        SettingsFragment fragment = (SettingsFragment) fm.findFragmentByTag(SettingsFragment.TAG);
        if (fragment == null) {
            fragment = SettingsFragment.newInstance();
        }
        new SettingsPresenter(fragment, new SettingsDataManager(this));
        fm.beginTransaction().replace(R.id.settings_parent_fl, fragment, SettingsFragment.TAG)
                .commitAllowingStateLoss();
    }

}