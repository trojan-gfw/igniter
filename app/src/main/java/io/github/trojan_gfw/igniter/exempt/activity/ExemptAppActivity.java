package io.github.trojan_gfw.igniter.exempt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import androidx.fragment.app.FragmentManager;

import io.github.trojan_gfw.igniter.Globals;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseAppCompatActivity;
import io.github.trojan_gfw.igniter.exempt.contract.ExemptAppContract;
import io.github.trojan_gfw.igniter.exempt.data.ExemptAppDataManager;
import io.github.trojan_gfw.igniter.exempt.fragment.ExemptAppFragment;
import io.github.trojan_gfw.igniter.exempt.presenter.ExemptAppPresenter;

public class ExemptAppActivity extends BaseAppCompatActivity {
    private ExemptAppContract.Presenter mPresenter;

    public static Intent create(Context context) {
        return new Intent(context, ExemptAppActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_exempt_app);
        FragmentManager fm = getSupportFragmentManager();
        ExemptAppFragment fragment = (ExemptAppFragment) fm.findFragmentByTag(ExemptAppFragment.TAG);
        if (fragment == null) {
            fragment = ExemptAppFragment.newInstance();
        }
        mPresenter = new ExemptAppPresenter(this, fragment, new ExemptAppDataManager(getApplicationContext(),
                Globals.getInternalBlockAppListPath(), Globals.getExternalExemptedAppListPath(), Globals.getAllowedAppListPath()));
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ExemptAppFragment.TAG)
                .commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if (mPresenter == null || !mPresenter.handleBackPressed()) {
            super.onBackPressed();
        }
    }
}
