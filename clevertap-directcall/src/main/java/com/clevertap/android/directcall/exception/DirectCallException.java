package com.clevertap.android.directcall.exception;

/**
 * A parent class of all failures.
 */
public class DirectCallException extends Exception{
    private static final int EXCEPTION_NETWORK_NOT_AVAILABLE = 100;
    private static final int EXCEPTION_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM = 101;
    private static final int EXCEPTION_DIRECT_Call_SDK_NOT_INITIALIZED_RESTART_THE_APP = 102;
    private static final int EXCEPTION_EXPECT_CALL_TOKEN_TO_MAKE_CALL = 103;

    public static final DirectCallException NoInternetException = new DirectCallException(EXCEPTION_NETWORK_NOT_AVAILABLE, "Device is offline", "Direct Call SDK requires active Internet connection");
    public static final DirectCallException ClientDisconnectedDueToNetworkProblemException = new DirectCallException(EXCEPTION_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM, "Client Disconnected", "Due to poor network connectivity, connection to CleverTap server is terminated");
    public static final DirectCallException SdkNotInitializedAppRestartRequiredException = new DirectCallException(EXCEPTION_DIRECT_Call_SDK_NOT_INITIALIZED_RESTART_THE_APP, "SDK initialization failed", "SDK failed to initialize, retry again with the another initialization");
    public static final DirectCallException CallTokenExpectedException = new DirectCallException(EXCEPTION_EXPECT_CALL_TOKEN_TO_MAKE_CALL, "ECTA token required", "Enhanced call time authentication token(ECTA) is not provided on Dashboard which is required to initiate the calls");

    private int errorCode;
    private String errorMessage;
    private String explanation;

    DirectCallException(int errorCode, String errorMessage, String explanation) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.explanation = explanation;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the explanation related to the exception. This value may be null in some cases.
     */
    public String getExplanation() {
        return explanation;
    }
}
