package com.clevertap.android.sdk.inapp;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.clevertap.android.sdk.CTXtensions;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Utils;


public abstract class CTInAppBasePartialFragment extends CTInAppBaseFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (isCleanedUp.get()) {
            cleanup();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    void cleanup() {
        if (!Utils.isActivityDead(getActivity()) && !isCleanedUp.get()) {
            final FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction;
            if (fragmentManager != null) {
                transaction = fragmentManager.beginTransaction();
                try {
                    transaction.remove(this).commit();
                } catch (IllegalStateException e) {
                    fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss();
                }
            }

        }
        isCleanedUp.set(true);
    }

    @Override
    void generateListener() {
        if (config != null) {
            setListener(CleverTapAPI.instanceWithConfig(this.context, config).getCoreState().getInAppController());
        }
    }

}
