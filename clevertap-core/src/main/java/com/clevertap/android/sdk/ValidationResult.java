package com.clevertap.android.sdk;

/**
 * Represents a validation result, with an error code.
 */
final class ValidationResult {

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

    String getErrorDesc() {
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
