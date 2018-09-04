package com.clevertap.android.sdk;

/**
 * Represents a validation result, with an error code.
 */
final class ValidationResult {
    private Object object;
    private int errorCode;
    private String errorDesc;

    ValidationResult(int errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public Object getObject() {
        return object;
    }

    public int getErrorCode() {
        return errorCode;
    }

    ValidationResult() {
        this.errorCode = 0;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
