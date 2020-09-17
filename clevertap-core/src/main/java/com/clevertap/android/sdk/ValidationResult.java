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

    String getErrorDesc() {
        return errorDesc;
    }

    void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    Object getObject() {
        return object;
    }

    int getErrorCode() {
        return errorCode;
    }

    ValidationResult() {
        this.errorCode = 0;
    }

    void setObject(Object object) {
        this.object = object;
    }

    void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
