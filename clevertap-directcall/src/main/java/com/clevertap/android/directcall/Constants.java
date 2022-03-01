package com.clevertap.android.directcall;

public interface Constants {
    String DirectCall_STORAGE_TAG = "DirectCall";
    String LOG_TAG = "CleverTap";
    String CALLING_LOG_TAG_SUFFIX = "DirectCall";
    int WAKE_LOCK_TIMEOUT = 30_000;
    int SIG_SOCK_PING_TIMEOUT = 10000;
    int MAKE_CALL_TIMEOUT = 10000;
    int OUTGOING_CALL_TIME_DURATION = 35000;
    int NETWORK_LATENCY_CHECK_FREQUENCY = 5000;

    int HTTP_CONNECT_TIMEOUT = 10;
    int HTTP_READ_TIMEOUT = 10;
    int HTTP_MAX_RETRIES = 5;
    int HTTP_INFINITE_RETRIES = -1;
    int HTTP_RETRY_DELAY = 15;
    boolean ENABLE_HTTP_LOG = false;

    int DEFAULT_PIN_VIEW_ITEM_COUNT = 4;

    String VOICE_CHANNEL_LOW_IMPORTANCE = "notification-channel-low-importance";
    String VOICE_CHANNEL_HIGH_IMPORTANCE = "notification-channel-high-importance";

    String BROADCAST_ACTION = "broadcastAction";
    String ACTION_INCOMING_CALL = "incoming-call";
    String ACTION_CALL_OVER = "call-over";
    String ACTION_CANCEL_CALL = "cancel";
    String ACTION_CONNECTION_STATUS = "com.clevertap.directcall.CONNECTION_STATUS";
    String ACTION_FCM_NOTIFICATION_RECEIVED = "notification";
    String ACTION_ACTIVITY_FINISH = "activityFinish";
    String ACTION_OUTGOING_HANGUP = "outgoingHangup";

    int SUCCESS_SDK_CONNECTED = 102;

    String CALL_FINISH_ACTION = "com.clevertap.android.directCall.DirectOutgoingCallFragment";
    String ACTIVITY_RELAUNCH_REASON = "launchReason";

    String FCM_NOTIFICATION_PAYLOAD = "FCM_NOTIFICATION_PAYLOAD";

    String KEY_STATUS = "status";
    String KEY_ACCOUNT_ID = "accountId";
    String KEY_API_KEY = "apikey";
    String KEY_CLEVERTAP_ACCOUNT_ID = "ctAccountId";
    String KEY_CLEVERTAP_API_KEY = "ctApiToken";
    String KEY_AUTHORIZATION_HEADER = "Authorization";
    String KEY_SECONDARY_BASE_URL = "baseUrl";
    String KEY_RETRY_DELAY = "retryTime";
    String KEY_ACCESS_TOKEN = "accessToken";
    String KEY_CONTACT_ID = "contactId";
    String KEY_CONTACT_CUID = "cuid";
    String KEY_CLEVERTAP_ID = "guid";
    String KEY_CALL_CONTEXT = "context";
    String KEY_CONTACT_NAME = "name";
    String KEY_CONTACT_CC = "cc";
    String KEY_CONTACT_PHONE = "phone";
    String KEY_FCM_TOKEN = "fcmToken";
    String KEY_BRANDING = "branding";
    String KEY_TEXT_COLOR = "color";
    String KEY_BG_COLOR = "bgColor";
    String KEY_BRAND_LOGO = "logo";
    String KEY_RECORDING = "recording";
    String KEY_WEBHOOK = "webhook";
    String KEY_VAR1 = "var1";
    String KEY_VAR2 = "var2";
    String KEY_VAR3 = "var3";
    String KEY_VAR4 = "var4";
    String KEY_VAR5 = "var5";
    String KEY_TAGS = "tags";
    String KEY_ECTA = "ecta";
    String KEY_CLI_LIST = "clis";
    String KEY_APP_ID = "appId";
    String KEY_PLATFORM = "platform";
    String KEY_DEVICE_ID = "device_id";
    String KEY_RINGTONE = "ringtone";
    String KEY_SDK_VERSION = "sdkVersion";
    String KEY_JWT_TOKEN = "token";
    String KEY_SNA = "sna";
    String KEY_PSTN = "pstn";
    String KEY_ACTIVE_SESSION = "activeSession";
    String KEY_TEMPLATE_PIN_VIEW_CONFIG = "templatePinViewConfig";
    String KEY_TEMPLATE_SCRATCHCARD_CONFIG = "templateScratchCardConfig";
    String KEY_MISSED_CALL_INITIATOR_HOST = "missedCallInitiatorHost";
    String KEY_MISSED_CALL_RECEIVER_HOST = "missedCallReceiverHost";
    String KEY_MISSED_CALL_INITIATOR_ACTIONS = "missedCallInitiatorActions";
    String KEY_MISSED_CALL_RECEIVER_ACTIONS = "missedCallReceiverActions";
    String KEY_NEVER_ASK_AGAIN_PERMISSION = "direct_call_never_ask_again";
    String KEY_DECLINE_REASON = "reason";
    String KEY_DECLINE_REASON_CODE = "reasonCode";
    int REASON_CODE_USER_BUSY = 1001;
    int REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED = 1002;
    int REASON_CODE_INVALID_CUID = 1003;

    //call-details
    String KEY_CALL_DATA = "data";
    String KEY_CALL_ID = "call";
    String KEY_CALL_TYPE = "callType";
    String KEY_CALL_HOST = "host";
    String KEY_INITIATOR_DATA = "initiatorData";
    String KEY_TO_Data = "to";
    String KEY_FROM_DATA = "from";

    String TRANSPORT_ERROR = "transport error";
    String IO_SERVER_DISCONNECT = "io server disconnect";
    String IO_CLIENT_DISCONNECT = "io client disconnect";
    String SOCKET_NO_ACK = "No Ack";

    int MAKE_CALL_ERR_CODE_RECEIVER_NOT_REACHABLE = 404;
    int MAKE_CALL_ERR_CODE_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL = 400;
    String MAKE_CALL_ERR_MSG_INVALID_CALL_TOKEN = "Invalid Call Token";
    String MAKE_CALL_ERR_MSG_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL = "Missing CC/Phone to make a PSTN call";
    String MAKE_CALL_ERR_MSG_MALFORMED_JWT = "jwt malformed";

    //SIP events
    String SIP_CLIENT_INIT_SDK = "javascript:DirectCallAndroid.init()";
    String SIP_CLIENT_INIT_JSSIP = "javascript:DirectCallAndroid.initJSSIP()";
    String SIP_CLIENT_MUTE = "javascript:DirectCallAndroid.mute()";
    String SIP_CLIENT_UN_MUTE = "javascript:DirectCallAndroid.unmute()";
    String SIP_CLIENT_HANGUP = "javascript:DirectCallAndroid.hangup()";
}
