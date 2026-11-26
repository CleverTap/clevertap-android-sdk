package com.clevertap.android.sdk.task

import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MainLooperHandler : Handler(Looper.getMainLooper())
