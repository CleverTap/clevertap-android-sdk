package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo

/**
 * Represents a validation result, with an error code.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ValidationResult(
    var errorCode: Int,
    var errorDesc: String?
) {

    var `object`: Any? = null

    constructor() : this(0, null)
}
