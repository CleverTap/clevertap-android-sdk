package com.clevertap.android.sdk.login

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ChangeUserCallback {

    public fun onChangeUser(deviceId: String, accountId: String)
}