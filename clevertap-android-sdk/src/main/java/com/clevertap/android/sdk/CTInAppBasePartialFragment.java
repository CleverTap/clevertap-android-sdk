package com.clevertap.android.sdk;

import android.app.FragmentManager;
import android.app.FragmentTransaction;


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
    void generateListener() {
        if (config != null) {
            setListener(CleverTapAPI.instanceWithConfig(getActivity().getBaseContext(),config));
        }
    }

    @Override
    void cleanup() {
        if (!Utils.isActivityDead(getActivity()) && !isCleanedUp.get()) {
            final FragmentManager fragmentManager = parent.getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            try {
                transaction.remove(this).commit();
            } catch (IllegalStateException e) {
                fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss();
            }
        }
        isCleanedUp.set(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        didDismiss(null);
    }
}
