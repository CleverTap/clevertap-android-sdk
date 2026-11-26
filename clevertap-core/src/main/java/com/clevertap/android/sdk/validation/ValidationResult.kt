package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo

/**
 * Represents a validation result, with an error code.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ValidationResult(
    var errorCode: Int = 0,
    var errorDesc: String? = null
) {

    var `object`: Any? = null
}
