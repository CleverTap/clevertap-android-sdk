package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo

/**
 * Represents a validation result, with an error code.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ValidationResult(
    public var errorCode: Int = 0,
    public var errorDesc: String? = null
) {

    public var `object`: Any? = null
}
