package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CTPushProviderListener {

    fun onNewToken(token: String?, pushType: PushType?)

}
