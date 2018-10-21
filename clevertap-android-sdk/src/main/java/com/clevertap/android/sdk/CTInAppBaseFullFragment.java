package com.clevertap.android.sdk;

public abstract class CTInAppBaseFullFragment extends CTInAppBaseFragment {

    @Override
    void cleanup() {/* no-op */}

    @Override
    void generateListener() {
        if (parent != null && parent instanceof InAppNotificationActivity) {
            setListener((CTInAppBaseFragment.InAppListener) parent);
        }
    }
}
