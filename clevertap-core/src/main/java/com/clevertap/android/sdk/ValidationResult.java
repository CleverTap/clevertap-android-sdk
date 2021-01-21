package com.clevertap.android.sdk;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Represents a validation result, with an error code.
 */
@RestrictTo(Scope.LIBRARY)
public final class ValidationResult {

    private int errorCode;

    private String errorDesc;

    private Object object;

    ValidationResult(int errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    ValidationResult() {
        this.errorCode = 0;
    }

    int getErrorCode() {
        return errorCode;
    }

    void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    Object getObject() {
        return object;
    }

    void setObject(Object object) {
        this.object = object;
    }
}
