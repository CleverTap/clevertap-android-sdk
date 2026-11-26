package com.clevertap.android.sdk.cryption

import androidx.annotation.RestrictTo
import android.util.Base64

// Utility extension functions for Base64 encoding/decoding
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
