package com.clevertap.android.sdk;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@RestrictTo(Scope.LIBRARY)
public class CallbackManager extends BaseCallbackManager {

    private final List<PushPermissionResponseListener> pushPermissionResponseListenerList = new ArrayList<>();

    @Deprecated
    private WeakReference<CTProductConfigListener> productConfigListener;

    public CallbackManager() {
    }

    @Override
    public List<PushPermissionResponseListener> getPushPermissionResponseListenerList() {
        return pushPermissionResponseListenerList;
    }

    @Override
    public void registerPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener) {
        this.pushPermissionResponseListenerList.add(pushPermissionResponseListener);
    }

    @Override
    public void unregisterPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener) {
        this.pushPermissionResponseListenerList.remove(pushPermissionResponseListener);
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    @Override
    public CTProductConfigListener getProductConfigListener() {
        if (productConfigListener != null && productConfigListener.get() != null) {
            return productConfigListener.get();
        }
        return null;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    @Override
    public void setProductConfigListener(final CTProductConfigListener productConfigListener) {
        if (productConfigListener != null) {
            this.productConfigListener = new WeakReference<>(productConfigListener);
        }
    }
}