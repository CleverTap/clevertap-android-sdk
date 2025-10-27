package com.clevertap.android.sdk;

import com.clevertap.android.sdk.product_config.CTProductConfigListener;

import java.util.List;

public abstract class BaseCallbackManager {
    public abstract List<PushPermissionResponseListener> getPushPermissionResponseListenerList();

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract CTProductConfigListener getProductConfigListener();

    public abstract void unregisterPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener);

    public abstract void registerPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener);

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract void setProductConfigListener(
            CTProductConfigListener productConfigListener);
}
