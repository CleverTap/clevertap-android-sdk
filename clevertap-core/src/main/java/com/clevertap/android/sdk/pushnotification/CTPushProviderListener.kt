package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface CTPushProviderListener {

    public fun onNewToken(token: String?, pushType: PushType?)

}
