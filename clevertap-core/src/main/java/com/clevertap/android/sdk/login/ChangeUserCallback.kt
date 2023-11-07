package com.clevertap.android.sdk.login

interface ChangeUserCallback {

    fun onChangeUser(deviceId: String, accountId: String)
}