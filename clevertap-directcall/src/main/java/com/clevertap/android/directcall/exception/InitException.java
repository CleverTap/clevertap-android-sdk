package com.clevertap.android.directcall.exception;

/**
 * Exceptions related to SDK initialization
 */
public class InitException extends DirectCallException {
    private static final int EXCEPTION_DIRECT_CALL_SDK_NOT_INITIALIZED = 2001;
    private static final int EXCEPTION_CONTACT_NOT_REGISTERED = 2002;
    public static final int EXCEPTION_CUID_ALREADY_CONNECTED_ELSEWHERE = 2003;

    public static final InitException DirectCallSdkNotInitializedException = new InitException(EXCEPTION_DIRECT_CALL_SDK_NOT_INITIALIZED, "SDK not initialized", "The server is not able to initialise the sdk due to unknown error");
    public static final InitException ContactNotRegisteredException = new InitException(EXCEPTION_CONTACT_NOT_REGISTERED, "contact not registered", "The server was not able to register the contact");
    public static final InitException SdkNotInitializedDueToCuidConnectedElsewhereException = new InitException(EXCEPTION_CUID_ALREADY_CONNECTED_ELSEWHERE, "CUID is connected elsewhere", "SDK initialization failed because the CUID passed for SDK initialization is already being used(online on CleverTap's server) by some other device");

    public InitException(int errorCode, String errorMessage, String explanation) {
        super(errorCode, errorMessage, explanation);
    }
}
