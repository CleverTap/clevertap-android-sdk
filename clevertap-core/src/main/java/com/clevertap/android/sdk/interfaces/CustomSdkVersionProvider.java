package com.clevertap.android.sdk.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Implemented by a {@link NotificationHandler} that belongs to a separate CleverTap module, such as Push
 * Templates, so Core can register the module's name and version with every CleverTapAPI instance.
 * Core sends it in the af header of every request, the same as CleverTapAPI.setCustomSdkVersion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CustomSdkVersionProvider {

    @NonNull
    String getCustomSdkName();

    int getCustomSdkVersion();
}
