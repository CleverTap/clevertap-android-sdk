package com.clevertap.android.sdk.login

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface ChangeUserCallback {

    fun onChangeUser(deviceId: String, accountId: String)
}