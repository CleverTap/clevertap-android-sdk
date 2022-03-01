package com.clevertap.android.directcall.exception;

/**
 * Exceptions related to making and receiving calls.
 */
public class CallException extends DirectCallException {
    private static final int ERR_MICROPHONE_PERMISSION_NOT_GRANTED = 5001;
    private static final int FAILURE_INTERNET_LOST_AT_RECEIVER_END = 5002;
    private static final int ERR_CONTACT_NOT_REACHABLE = 5003;
    private static final int ERR_NUMBER_NOT_EXISTS = 5004;
    private static final int ERR_BAD_NETWORK = 5005;
    private static final int ERR_MISSING_CLI = 5006;
    private static final int ERR_MISSING_CC_OR_PHONE_IN_CLI = 5007;
    private static final int ERR_INVALID_CC_LENGTH = 5008;
    private static final int ERR_INVALID_PHONE_NUMBER_LENGTH = 5009;
    private static final int ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI = 5010;
    private static final int ERR_TAGS_COUNT_EXCEEDED_BY_10 = 5011;
    private static final int ERR_TAG_LENGTH_EXCEEDED_BY_32 = 5012;
    private static final int ERR_INVALID_FORMAT_OF_WEBHOOK = 5013;
    private static final int ERR_INVALID_CALLEE_CUID = 5014;
    private static final int ERR_VAR_LENGTH_EXCEEDED_BY_128 = 5015;
    private static final int ERR_SOMETHING_WENT_WRONG = 5016;
    private static final int ERR_CALLEE_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN = 5017;
    private static final int ERR_CALLER_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN = 5018;
    private static final int ERR_UNAUTHORIZED_CLI = 5019;
    private static final int ERR_EMPTY_VERIFIED_CLI_LIST = 5020;
    private static final int ERR_CC_PHONE_MISSING_FOR_CUID_IN_PSTN_CALL = 5021;
    private static final int ERR_INVALID_CALL_TOKEN = 5022;
    private static final int ERR_CUID_ALREADY_CONNECTED_ELSEWHERE = 5023;
    private static final int ERR_BOTH_CC_AND_PHONE_REQUIRED = 5024;
    private static final int ERR_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL = 5025;
    private static final int FAILURE_CAN_NOT_CALL_SELF = 5026;
    private static final int ERR_CALL_CONTEXT_REQUIRED = 5027;
    private static final int ERR_CALL_CONTEXT_LENGTH_EXCEEDED_BY_64 = 5028;
    private static final int ERR_INVALID_ACTIVITY_CONTEXT = 5029;
    private static final int ERR_CALL_OPTIONS_REQUIRED = 5030;
    private static final int ERR_WHILE_MAKING_VOIP_CALL = 5031;

    public static final CallException MicrophonePermissionNotGrantedException = new CallException(ERR_MICROPHONE_PERMISSION_NOT_GRANTED, "Microphone permission not available", "Microphone permission needs to be granted before initiating the voice call");
    public static final CallException InternetLostAtReceiverEndException = new CallException(FAILURE_INTERNET_LOST_AT_RECEIVER_END, "Internet lost at receiver's end", "Server is not able to process the call because receiver lost the connection to routing channel");
    public static final CallException ContactNotReachableException = new CallException(ERR_CONTACT_NOT_REACHABLE, "Receiver is not reachable", "Receiver is not available on any call-routing channel, can't initiate the call");
    public static final CallException NumberNotExistsException = new CallException(ERR_NUMBER_NOT_EXISTS, "CC and Phone doesn't exist", "CC and Phone number is not available for the CUID passed inside callOptions");
    public static final CallException BadNetworkException = new CallException(ERR_BAD_NETWORK, "Bad Network", "Internet connectivity strength is not as per the requirements to initiate a VoIP call");
    public static final CallException MissingCliException = new CallException(ERR_MISSING_CLI, "CLI is not available", "CLI needs to be provided on Dashboard to initiate the PSTN call");
    public static final CallException MissingCcPhoneInCliException = new CallException(ERR_MISSING_CC_OR_PHONE_IN_CLI, "Missing details in CLI Object", "CLI provided on dashboard is missing the detail of either country-code or phone number");
    public static final CallException InvalidCcLengthException = new CallException(ERR_INVALID_CC_LENGTH, "Invalid length of CC", "Invalid CC is provided to initiate the PSTN call, valid range of length is in between 1 to 4");
    public static final CallException InvalidPhoneNumberLengthException = new CallException(ERR_INVALID_PHONE_NUMBER_LENGTH, "Invalid length of Phone", "Invalid CC is provided to initiate the PSTN call, valid range of length is in between 6 to 20");
    public static final CallException InvalidLengthOfCcOrPhoneInCliException = new CallException(ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI, "Invalid length of CC or Phone in CLI", "Invalid length of either CC or Phone is provided in CLI, please check CLI provided over Dashboard");
    public static final CallException TagCountExceededBy10Exception = new CallException(ERR_TAGS_COUNT_EXCEEDED_BY_10, "Tags count limit is exceeded", "Maximum limit of tags that can be passed while initiating a call is 10");
    public static final CallException TagLengthExceededBy32Exception = new CallException(ERR_TAG_LENGTH_EXCEEDED_BY_32, "Invalid length of Tag(s)", "Valid range of length of a tag is in between 1 to 32");
    public static final CallException InvalidWebhookException = new CallException(ERR_INVALID_FORMAT_OF_WEBHOOK, "Invalid Webhook URL", "Invalid format of Webhook URL passed inside callOptions object");
    public static final CallException InvalidCalleeCuidException = new CallException(ERR_INVALID_CALLEE_CUID, "Invalid Receiver's CUID", "Receiver's cuid should not be null or of empty length");
    public static final CallException VarLengthExceededBy128Exception = new CallException(ERR_VAR_LENGTH_EXCEEDED_BY_128, "Invalid Length of Var(s)", "valid range of length of var(s) in between 1 to 128");
    public static final CallException SomethingWentWrongException = new CallException(ERR_SOMETHING_WENT_WRONG, "Something goes wrong", "Fail to initiate the call due to unknown reason");
    public static final CallException CcPhoneOfCalleeNeededToMakePstnToPstnException = new CallException(ERR_CALLEE_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN, "Callee's cc and phone is not available", "To initiate the PSTN to PSTN call caller's cc and phone are needed");
    public static final CallException CcPhoneOfCallerNeededToMakePstnToPstnException = new CallException(ERR_CALLER_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN, "Caller's cc and phone is not available", "To initiate the PSTN to PSTN call callee's cc and phone are needed");
    public static final CallException UnauthorizedCliException = new CallException(ERR_UNAUTHORIZED_CLI, "Unauthorized CLI", "CLI provided to initiate the PSTN is not authorized as it is not the part of the CLI list provided over Dashboard");
    public static final CallException EmptyVerifiedCliListException = new CallException(ERR_EMPTY_VERIFIED_CLI_LIST, "No CLI is available over Dashboard", "To place the PSTN call at least one CLI needs to be provided over Dashboard");
    public static final CallException CcPhoneMissingForCuidInPstnCallException = new CallException(ERR_CC_PHONE_MISSING_FOR_CUID_IN_PSTN_CALL, "CC, Phone missing in receiver's CUID", "CC, phone is not attached to the receiver's CUID which is required to initiate the PSTN call");
    public static final CallException InvalidCallTokenException = new CallException(ERR_INVALID_CALL_TOKEN, "Invalid/Unauthorized call token", "ECTA(Enhanced call time authentication token) passed in callOptions object is not valid/unauthorized");
    public static final CallException CuidConnectedElsewhereException = new CallException(ERR_CUID_ALREADY_CONNECTED_ELSEWHERE, "CUID is connected elsewhere", "logged-in CUID on the current device is now being used on some other device");
    public static final CallException BothCcPhoneRequiredException = new CallException(ERR_BOTH_CC_AND_PHONE_REQUIRED, "Both CC and Phone required", "Both country code and the phone number of the receiver's are required to initiate the PSTN call");
    public static final CallException MissingCcPhoneToMakePstnCallException = new CallException(ERR_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL, "CC, Phone missing in receiver's CUID", "CC, phone is not attached to the receiver's CUID which is required to initiate the PSTN call");
    public static final CallException CanNotCallSelfException = new CallException(FAILURE_CAN_NOT_CALL_SELF, "Can not call self", "Receiver's cuid and the initiator's cuid can't be the same");
    public static final CallException CallContextRequiredException = new CallException(ERR_CALL_CONTEXT_REQUIRED, "Context of the call required", "Context of the call is the mandatory parameter to be passed inside callOptions object");
    public static final CallException CallContextExceededBy64Exception = new CallException(ERR_CALL_CONTEXT_LENGTH_EXCEEDED_BY_64, "Invalid length of context of the call", "Length of the context of the call should be in the range 1 to 64");
    public static final CallException InvalidActivityContextException = new CallException(ERR_INVALID_ACTIVITY_CONTEXT, "Invalid context of Activity", "Context of the activity, provided should be non-null");
    public static final CallException CallOptionsRequiredException = new CallException(ERR_CALL_OPTIONS_REQUIRED, "Call Options is required", "Object of the callOptions is required to initiate the call and should be non-null");
    public static final CallException VoIPCallException = new CallException(ERR_WHILE_MAKING_VOIP_CALL, "VoIP call error", "An unknown error occured while making the call");

    public CallException(int errorCode, String errorMessage, String explanation) {
        super(errorCode, errorMessage, explanation);
    }
}
