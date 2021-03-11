package com.clevertap.android.sdk.inapp;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.utils.Utils;


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
        didDismiss(null);
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
