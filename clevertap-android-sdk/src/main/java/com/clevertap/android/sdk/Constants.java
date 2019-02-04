package com.clevertap.android.sdk;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Constants {
    static final String LABEL_ACCOUNT_ID = "CLEVERTAP_ACCOUNT_ID";
    static final String LABEL_TOKEN = "CLEVERTAP_TOKEN";
    static final String LABEL_NOTIFICATION_ICON = "CLEVERTAP_NOTIFICATION_ICON";
    static final String LABEL_INAPP_EXCLUDE = "CLEVERTAP_INAPP_EXCLUDE";
    static final String LABEL_REGION = "CLEVERTAP_REGION";
    static final String LABEL_DISABLE_APP_LAUNCH = "CLEVERTAP_DISABLE_APP_LAUNCHED";
    static final String LABEL_SSL_PINNING = "CLEVERTAP_SSL_PINNING";
    static final String LABEL_BACKGROUND_SYNC = "CLEVERTAP_BACKGROUND_SYNC";
    static final String CLEVERTAP_OPTOUT = "ct_optout";
    static final String CLEVERTAP_USE_GOOGLE_AD_ID = "CLEVERTAP_USE_GOOGLE_AD_ID";
    static final String CLEVERTAP_STORAGE_TAG = "WizRocket";
    static final String CLEVERTAP_LOG_TAG = "CleverTap";
    static final int SESSION_LENGTH_MINS = 20;
    static final String DEVICE_ID_TAG = "deviceId";
    static final SimpleDateFormat FB_DOB_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    static final SimpleDateFormat GP_DOB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    static final int PAGE_EVENT = 1;
    static final int PING_EVENT = 2;
    static final int PROFILE_EVENT = 3;
    static final int RAISED_EVENT = 4;
    static final int DATA_EVENT = 5;
    static final String ICON_BASE_URL = "http://static.wizrocket.com/android/ico/";
    static final String NOTIFICATION_CLICKED_EVENT_NAME = "Notification Clicked";
    static final String NOTIFICATION_VIEWED_EVENT_NAME = "Notification Viewed";
    static final String APP_LAUNCHED_EVENT = "App Launched";
    static final String ERROR_KEY = "wzrk_error";
    static final int PUSH_DELAY_MS = 1000;
    static final String INAPP_PREVIEW_PUSH_PAYLOAD_KEY = "wzrk_inapp";
    static final String INBOX_PREVIEW_PUSH_PAYLOAD_KEY = "wzrk_inbox";
    static final String INAPP_DATA_TAG = "html";
    static final String INAPP_X_PERCENT = "xp";
    static final String INAPP_Y_PERCENT = "yp";
    static final String INAPP_X_DP = "xdp";
    static final String INAPP_Y_DP = "ydp";
    static final String INAPP_POSITION = "pos";
    static final char INAPP_POSITION_TOP = 't';
    static final char INAPP_POSITION_RIGHT = 'r';
    static final char INAPP_POSITION_BOTTOM = 'b';
    static final char INAPP_POSITION_LEFT = 'l';
    static final char INAPP_POSITION_CENTER = 'c';
    static final String INAPP_NOTIF_DARKEN_SCREEN = "dk";
    static final String INAPP_NOTIF_SHOW_CLOSE = "sc";
    static final String INAPP_JSON_RESPONSE_KEY = "inapp_notifs";
    static final String INBOX_JSON_RESPONSE_KEY = "inbox_notifs";
    static final String INAPP_MAX_DISPLAY_COUNT = "mdc";
    static final String INAPP_MAX_PER_SESSION = "imc";
    static final String INAPP_WINDOW = "w";
    static final String PREFS_INAPP_KEY = "inApp";
    static final int INAPP_CLOSE_IV_WIDTH = 40;
    static final String DEBUG_KEY = "d";
    static final String NOTIFICATION_ID_TAG = "wzrk_id";
    static final String DEEP_LINK_KEY = "wzrk_dl";
    static final String WZRK_PREFIX = "wzrk_";
    static final int NOTIFICATION_ID_TAG_INTERVAL = 5000;
    static final int NOTIFICATION_VIEWED_ID_TAG_INTERVAL = 2000;
    static final String SESSION_ID_LAST = "lastSessionId";
    static final String LAST_SESSION_EPOCH = "sexe";
    static final String LABEL_SENDER_ID = "GCM_SENDER_ID";
    static final int MAX_KEY_LENGTH = 120;
    static final int MAX_VALUE_LENGTH = 512;
    static final int MAX_MULTI_VALUE_ARRAY_LENGTH = 100;
    static final int MAX_MULTI_VALUE_LENGTH = 512;
    static final String WZRK_FROM_KEY = "wzrk_from";
    static final String WZRK_ACCT_ID_KEY = "wzrk_acct_id";
    static final String WZRK_FROM = "CTPushNotificationReceiver";
    static final String NETWORK_INFO = "NetworkInfo";
    static final String PRIMARY_DOMAIN = "wzrkt.com";
    static final String KEY_DOMAIN_NAME = "comms_dmn";
    static final String HEADER_DOMAIN_NAME = "X-WZRK-RD";
    static final String HEADER_MUTE = "X-WZRK-MUTE";
    static final String NAMESPACE_IJ = "IJ";
    static final String KEY_LAST_TS = "comms_last_ts";
    static final String KEY_FIRST_TS = "comms_first_ts";
    static final String KEY_I = "comms_i";
    static final String KEY_J = "comms_j";
    static final String CACHED_GUIDS_KEY = "cachedGUIDsKey";
    static final String MULTI_USER_PREFIX = "mt_";
    static final String NOTIFICATION_TAG = "wzrk_pn";
    static final String CHARGED_EVENT = "Charged";
    static final String GCM_PROPERTY_REG_ID = "registration_id"; // maintain legacy value
    static final String FCM_PROPERTY_REG_ID = "fcm_token";
    static final String KEY_MUTED = "comms_mtd";
    static final int EMPTY_NOTIFICATION_ID = -1000;
    static final String KEY_MAX_PER_DAY = "istmcd_inapp";
    static final String KEY_COUNTS_SHOWN_TODAY = "istc_inapp";
    static final String KEY_COUNTS_PER_INAPP = "counts_per_inapp";
    static final String INAPP_ID_IN_PAYLOAD = "ti";
    static final int LOCATION_PING_INTERVAL_IN_SECONDS = 10;
    static final String[] SYSTEM_EVENTS = {NOTIFICATION_CLICKED_EVENT_NAME};
    static final long DEFAULT_PUSH_TTL = 1000 * 60 * 60 * 24 * 4;
    static final String PF_JOB_ID = "pfjobid";
    static final int PING_FREQUENCY_VALUE = 240;
    static final String PING_FREQUENCY = "pf";
    static final long ONE_MIN_IN_MILLIS = 60 * 1000L;
    static final String COPY_TYPE = "copy";
    static final String DND_START = "22:00";
    static final String DND_STOP = "06:00";
    static final String VIDEO_THUMBNAIL = "ct_video_1";
    static final String AUDIO_THUMBNAIL = "ct_audio";
    static final String IMAGE_PLACEHOLDER = "ct_image";
    static final int LIST_VIEW_WIDTH = 450;
    /**
     * Profile command constants.
     */
    static final String COMMAND_SET = "$set";
    static final String COMMAND_ADD = "$add";
    static final String COMMAND_REMOVE = "$remove";

    static final String COMMAND_DELETE = "$delete";
    static final String GUID_PREFIX_GOOGLE_AD_ID = "__g";

    // valid profile identifier keys
    static final Set<String> PROFILE_IDENTIFIER_KEYS = new HashSet<>(Arrays.asList(
            "Identity", "Email", "FBID", "GPID"));
}
