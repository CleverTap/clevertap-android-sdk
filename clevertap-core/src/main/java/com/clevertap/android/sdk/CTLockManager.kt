package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

@RestrictTo(Scope.LIBRARY)
internal open class CTLockManager {

    val eventLock: Any = Any()

    val inboxControllerLock: Any = Any()
}
