package com.clevertap.android.sdk;

import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.pushnotification.PushProviders;

public class ControllerManager {

    private PushProviders mPushProviders;

    private InAppController mInAppController;

    private CTDisplayUnitController mCTDisplayUnitController;

    private CTInboxController mCTInboxController;

    private CTProductConfigController mCTProductConfigController;

    private CTFeatureFlagsController mCTFeatureFlagsController;

    public CTDisplayUnitController getCTDisplayUnitController() {
        return mCTDisplayUnitController;
    }

    public CTFeatureFlagsController getCTFeatureFlagsController() {

        return mCTFeatureFlagsController;
    }

    public CTInboxController getCTInboxController() {
        return mCTInboxController;
    }

    public CTProductConfigController getCTProductConfigController() {
        return mCTProductConfigController;
    }

    public InAppController getInAppController() {
        return mInAppController;
    }

    public PushProviders getPushProviders() {
        return mPushProviders;
    }

    public void setCTDisplayUnitController(
            final CTDisplayUnitController CTDisplayUnitController) {
        mCTDisplayUnitController = CTDisplayUnitController;
    }

    public void setCTFeatureFlagsController(
            final CTFeatureFlagsController CTFeatureFlagsController) {
        mCTFeatureFlagsController = CTFeatureFlagsController;
    }

    public void setCTInboxController(final CTInboxController CTInboxController) {
        mCTInboxController = CTInboxController;
    }

    public void setCTProductConfigController(
            final CTProductConfigController CTProductConfigController) {
        mCTProductConfigController = CTProductConfigController;
    }

    public void setInAppController(final InAppController inAppController) {
        mInAppController = inAppController;
    }

    public void setPushProviders(final PushProviders pushProviders) {
        mPushProviders = pushProviders;
    }
}
