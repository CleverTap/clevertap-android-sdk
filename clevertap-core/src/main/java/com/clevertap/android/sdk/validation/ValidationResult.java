package com.clevertap.android.sdk.validation;

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

    public ValidationResult(int errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    public ValidationResult() {
        this.errorCode = 0;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public Object getObject() {
        return object;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
