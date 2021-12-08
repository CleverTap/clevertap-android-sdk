package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Constants {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_EMAIL, TYPE_PHONE, TYPE_IDENTITY})
    @interface IdentityType {

    }
    String TAG_FEATURE_IN_APPS = "TAG_FEATURE_IN_APPS";
    @NonNull
    String TYPE_IDENTITY = "Identity";
    @NonNull
    String TYPE_EMAIL = "Email";
    @NonNull
    String TYPE_PHONE = "Phone";


    String LABEL_ACCOUNT_ID = "CLEVERTAP_ACCOUNT_ID";
    String LABEL_TOKEN = "CLEVERTAP_TOKEN";
    String LABEL_NOTIFICATION_ICON = "CLEVERTAP_NOTIFICATION_ICON";
    String LABEL_INAPP_EXCLUDE = "CLEVERTAP_INAPP_EXCLUDE";
    String LABEL_REGION = "CLEVERTAP_REGION";
    String LABEL_DISABLE_APP_LAUNCH = "CLEVERTAP_DISABLE_APP_LAUNCHED";
    String LABEL_SSL_PINNING = "CLEVERTAP_SSL_PINNING";
    String LABEL_BACKGROUND_SYNC = "CLEVERTAP_BACKGROUND_SYNC";
    String LABEL_CUSTOM_ID = "CLEVERTAP_USE_CUSTOM_ID";
    String LABEL_USE_GOOGLE_AD_ID = "CLEVERTAP_USE_GOOGLE_AD_ID";
    String LABEL_FCM_SENDER_ID = "FCM_SENDER_ID";
    String LABEL_PACKAGE_NAME = "CLEVERTAP_APP_PACKAGE";
    String LABEL_BETA = "CLEVERTAP_BETA";
    String LABEL_INTENT_SERVICE = "CLEVERTAP_INTENT_SERVICE";
    String LABEL_XIAOMI_APP_KEY = "CLEVERTAP_XIAOMI_APP_KEY";
    String LABEL_XIAOMI_APP_ID = "CLEVERTAP_XIAOMI_APP_ID";
    String CLEVERTAP_OPTOUT = "ct_optout";
    String CLEVERTAP_STORAGE_TAG = "WizRocket";
    String CLEVERTAP_LOG_TAG = "CleverTap";
    int SESSION_LENGTH_MINS = 20;
    String DEVICE_ID_TAG = "deviceId";
    String FALLBACK_ID_TAG = "fallbackId";
    SimpleDateFormat FB_DOB_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    int PAGE_EVENT = 1;
    int PING_EVENT = 2;
    int PROFILE_EVENT = 3;
    int RAISED_EVENT = 4;
    int DATA_EVENT = 5;
    int NV_EVENT = 6;
    int FETCH_EVENT = 7;
    String WZRK_FETCH = "wzrk_fetch";
    String ICON_BASE_URL = "http://static.wizrocket.com/android/ico/";
    String NOTIFICATION_CLICKED_EVENT_NAME = "Notification Clicked";
    String NOTIFICATION_VIEWED_EVENT_NAME = "Notification Viewed";
    String GEOFENCE_ENTERED_EVENT_NAME = "Geocluster Entered";
    String GEOFENCE_EXITED_EVENT_NAME = "Geocluster Exited";
    String APP_LAUNCHED_EVENT = "App Launched";
    String ERROR_KEY = "wzrk_error";
    int PUSH_DELAY_MS = 1000;
    String INAPP_PREVIEW_PUSH_PAYLOAD_KEY = "wzrk_inapp";
    String INBOX_PREVIEW_PUSH_PAYLOAD_KEY = "wzrk_inbox";
    String DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY = "wzrk_adunit";
    String INAPP_HTML_TAG = "html";
    String INAPP_DATA_TAG = "d";
    String INAPP_X_PERCENT = "xp";
    String INAPP_Y_PERCENT = "yp";
    String INAPP_X_DP = "xdp";
    String INAPP_Y_DP = "ydp";
    String INAPP_POSITION = "pos";
    char INAPP_POSITION_TOP = 't';
    char INAPP_POSITION_RIGHT = 'r';
    char INAPP_POSITION_BOTTOM = 'b';
    char INAPP_POSITION_LEFT = 'l';
    char INAPP_POSITION_CENTER = 'c';
    String INAPP_NOTIF_DARKEN_SCREEN = "dk";
    String INAPP_NOTIF_SHOW_CLOSE = "sc";
    String INAPP_JSON_RESPONSE_KEY = "inapp_notifs";
    String INBOX_JSON_RESPONSE_KEY = "inbox_notifs";
    String DISPLAY_UNIT_JSON_RESPONSE_KEY = "adUnit_notifs";
    String FEATURE_FLAG_JSON_RESPONSE_KEY = "ff_notifs";
    String REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY = "pc_notifs";
    String GEOFENCES_JSON_RESPONSE_KEY = "geofences";
    String DISCARDED_EVENT_JSON_KEY = "d_e";
    String INAPP_MAX_DISPLAY_COUNT = "mdc";
    String INAPP_MAX_PER_SESSION = "imc";
    String INAPP_WINDOW = "w";
    String INAPP_KEY = "inApp";
    int INAPP_CLOSE_IV_WIDTH = 40;
    String INAPP_JS_ENABLED = "isJsEnabled";
    String NOTIFICATION_ID_TAG = "wzrk_id";
    String DEEP_LINK_KEY = "wzrk_dl";
    String WZRK_PREFIX = "wzrk_";
    int NOTIFICATION_ID_TAG_INTERVAL = 5000;
    int NOTIFICATION_VIEWED_ID_TAG_INTERVAL = 2000;
    String SESSION_ID_LAST = "lastSessionId";
    String LAST_SESSION_EPOCH = "sexe";
    int MAX_KEY_LENGTH = 120;
    int MAX_VALUE_LENGTH = 512;
    int MAX_MULTI_VALUE_ARRAY_LENGTH = 100;
    int MAX_MULTI_VALUE_LENGTH = 512;
    String WZRK_FROM_KEY = "wzrk_from";
    String WZRK_ACCT_ID_KEY = "wzrk_acct_id";
    String WZRK_FROM = "CTPushNotificationReceiver";
    String NETWORK_INFO = "NetworkInfo";
    String PRIMARY_DOMAIN = "wzrkt.com";
    String KEY_DOMAIN_NAME = "comms_dmn";
    String SPIKY_KEY_DOMAIN_NAME = "comms_dmn_spiky";
    String HEADER_DOMAIN_NAME = "X-WZRK-RD";
    String SPIKY_HEADER_DOMAIN_NAME = "X-WZRK-SPIKY-RD";
    String HEADER_MUTE = "X-WZRK-MUTE";
    String NAMESPACE_IJ = "IJ";
    String KEY_LAST_TS = "comms_last_ts";
    String KEY_FIRST_TS = "comms_first_ts";
    String KEY_I = "comms_i";
    String KEY_J = "comms_j";
    String CACHED_GUIDS_KEY = "cachedGUIDsKey";
    String MULTI_USER_PREFIX = "mt_";
    String NOTIFICATION_TAG = "wzrk_pn";
    String CHARGED_EVENT = "Charged";
    String KEY_MUTED = "comms_mtd";
    int EMPTY_NOTIFICATION_ID = -1000;
    String KEY_MAX_PER_DAY = "istmcd_inapp";
    String KEY_COUNTS_SHOWN_TODAY = "istc_inapp";
    String KEY_COUNTS_PER_INAPP = "counts_per_inapp";
    String INAPP_ID_IN_PAYLOAD = "ti";
    int LOCATION_PING_INTERVAL_IN_SECONDS = 10;
    String[] SYSTEM_EVENTS = {NOTIFICATION_CLICKED_EVENT_NAME,
            NOTIFICATION_VIEWED_EVENT_NAME, GEOFENCE_ENTERED_EVENT_NAME,
            GEOFENCE_EXITED_EVENT_NAME};
    long DEFAULT_PUSH_TTL = 1000L * 60 * 60 * 24 * 4;// 4 days
    String PF_JOB_ID = "pfjobid";
    int PING_FREQUENCY_VALUE = 240;
    String PING_FREQUENCY = "pf";
    long ONE_MIN_IN_MILLIS = 60 * 1000L;
    long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;
    String COPY_TYPE = "copy";
    String DND_START = "22:00";
    String DND_STOP = "06:00";
    String VIDEO_THUMBNAIL = "ct_video_1";
    String AUDIO_THUMBNAIL = "ct_audio";
    String IMAGE_PLACEHOLDER = "ct_image";
    //Keys used by the SDK
    String KEY_ACCOUNT_ID = "accountId";
    String KEY_ACCOUNT_TOKEN = "accountToken";
    String KEY_ACCOUNT_REGION = "accountRegion";
    String KEY_ANALYTICS_ONLY = "analyticsOnly";
    String KEY_DEFAULT_INSTANCE = "isDefaultInstance";
    String KEY_USE_GOOGLE_AD_ID = "useGoogleAdId";
    String KEY_DISABLE_APP_LAUNCHED = "disableAppLaunchedEvent";
    String KEY_PERSONALIZATION = "personalization";
    String KEY_DEBUG_LEVEL = "debugLevel";
    String KEY_CREATED_POST_APP_LAUNCH = "createdPostAppLaunch";
    String KEY_SSL_PINNING = "sslPinning";
    String KEY_BACKGROUND_SYNC = "backgroundSync";
    String KEY_FCM_SENDER_ID = "fcmSenderId";
    String KEY_CONFIG = "config";
    String KEY_C2A = "wzrk_c2a";
    String KEY_EFC = "efc";
    String KEY_TLC = "tlc";
    String KEY_TDC = "tdc";
    String KEY_KV = "kv";
    String KEY_TYPE = "type";
    String KEY_IS_TABLET = "tablet";
    String KEY_BG = "bg";
    String KEY_TITLE = "title";
    String KEY_TEXT = "text";
    String KEY_COLOR = "color";
    String KEY_MESSAGE = "message";
    String KEY_HIDE_CLOSE = "close";
    String KEY_MEDIA = "media";
    String KEY_MEDIA_LANDSCAPE = "mediaLandscape";
    String KEY_PORTRAIT = "hasPortrait";
    String KEY_LANDSCAPE = "hasLandscape";
    String KEY_CONTENT_TYPE = "content_type";
    String KEY_URL = "url";
    String KEY_BUTTONS = "buttons";
    String KEY_CUSTOM_HTML = "custom-html";
    String KEY_ENABLE_CUSTOM_CT_ID = "getEnableCustomCleverTapId";
    String KEY_BETA = "beta";
    String KEY_PACKAGE_NAME = "packageName";
    String KEY_ALLOWED_PUSH_TYPES = "allowedPushTypes";
    String KEY_IDENTITY_TYPES = "identityTypes";
    String WZRK_PUSH_ID = "wzrk_pid";
    //String EXTRAS_FROM = "extras_from";
    String NOTIF_MSG = "nm";
    String NOTIF_TITLE = "nt";
    String NOTIF_ICON = "ico";
    String WZRK_ACTIONS = "wzrk_acts";
    String WZRK_BIG_PICTURE = "wzrk_bp";
    String WZRK_MSG_SUMMARY = "wzrk_nms";
    String NOTIF_PRIORITY = "pr";
    String PRIORITY_HIGH = "high";
    String PRIORITY_MAX = "max";
    String WZRK_COLLAPSE = "wzrk_ck";
    String WZRK_CHANNEL_ID = "wzrk_cid";
    String WZRK_BADGE_ICON = "wzrk_bi";
    String WZRK_BADGE_COUNT = "wzrk_bc";
    String WZRK_SUBTITLE = "wzrk_st";
    String WZRK_COLOR = "wzrk_clr";
    String WZRK_SOUND = "wzrk_sound";
    String WZRK_TIME_TO_LIVE = "wzrk_ttl";
    String WZRK_RNV = "wzrk_rnv";
    String BLACK = "#000000";
    String WHITE = "#FFFFFF";
    String BLUE = "#0000FF";
    /**
     * Profile command constants.
     */
    String COMMAND_SET = "$set";
    String COMMAND_ADD = "$add";
    String COMMAND_REMOVE = "$remove";
    String COMMAND_DELETE = "$delete";
    String COMMAND_INCREMENT = "$incr";
    String COMMAND_DECREMENT = "$decr";
    String GUID_PREFIX_GOOGLE_AD_ID = "__g";
    String CUSTOM_CLEVERTAP_ID_PREFIX = "__h";
    String ERROR_PROFILE_PREFIX = "__i";
    String KEY_ICON = "icon";
    String KEY_POSTER_URL = "poster";
    String KEY_ACTION = "action";
    String KEY_ANDROID = "android";
    String KEY_ORIENTATION = "orientation";
    String KEY_WZRK_PARAMS = "wzrkParams";
    String KEY_CONTENT = "content";
    String KEY_CUSTOM_KV = "custom_kv";
    String KEY_BORDER = "border";
    String KEY_RADIUS = "radius";
    String KEY_ACTIONS = "actions";
    String KEY_ID = "id";
    String KEY_DATE = "date";
    String KEY_WZRK_TTL = "wzrk_ttl";
    String KEY_IS_READ = "isRead";
    String KEY_TAGS = "tags";
    String KEY_MSG = "msg";
    String KEY_HAS_URL = "hasUrl";
    String KEY_HAS_LINKS = "hasLinks";
    String KEY_LINKS = "links";
    String TEST_IDENTIFIER = "0_0";
    String FEATURE_DISPLAY_UNIT = "DisplayUnit : ";
    String FEATURE_FLAG_UNIT = "Feature Flag : ";
    String LOG_TAG_PRODUCT_CONFIG = "Product Config : ";
    int FETCH_TYPE_PC = 0;
    int FETCH_TYPE_FF = 1;
    String LOG_TAG_GEOFENCES = "Geofences : ";
    // error message codes
    int INVALID_MULTI_VALUE = 1;
    int PUSH_KEY_EMPTY = 2;
    int OBJECT_VALUE_NOT_PRIMITIVE_PROFILE = 3;
    int INVALID_COUNTRY_CODE = 4;
    int INVALID_PHONE = 5;
    int KEY_EMPTY = 6;
    int PROP_VALUE_NOT_PRIMITIVE = 7;
    int CHANNEL_ID_MISSING_IN_PAYLOAD = 8;
    int CHANNEL_ID_NOT_REGISTERED = 9;
    int NOTIFICATION_VIEWED_DISABLED = 10;
    int VALUE_CHARS_LIMIT_EXCEEDED = 11;
    int MULTI_VALUE_CHARS_LIMIT_EXCEEDED = 12;
    int INVALID_PROFILE_PROP_ARRAY_COUNT = 13;
    int EVENT_NAME_NULL = 14;
    int OBJECT_VALUE_NOT_PRIMITIVE = 15;
    int RESTRICTED_EVENT_NAME = 16;
    int DISCARDED_EVENT_NAME = 17;
    int USE_CUSTOM_ID_FALLBACK = 18;
    int USE_CUSTOM_ID_MISSING_IN_MANIFEST = 19;
    int UNABLE_TO_SET_CT_CUSTOM_ID = 20;
    int INVALID_CT_CUSTOM_ID = 21;
    int INVALID_MULTI_VALUE_KEY = 23;
    int RESTRICTED_MULTI_VALUE_KEY = 24;
    int INVALID_INCREMENT_DECREMENT_VALUE = 25;
    String CLEVERTAP_IDENTIFIER = "CLEVERTAP_IDENTIFIER";
    String SEPARATOR_COMMA = ",";
    String EMPTY_STRING = "";
    String SP_KEY_PROFILE_IDENTITIES = "SP_KEY_PROFILE_IDENTITIES";

    // valid profile identifier keys
    HashSet<String> LEGACY_IDENTITY_KEYS = new HashSet<>(Arrays.asList(TYPE_IDENTITY, TYPE_EMAIL));
    HashSet<String> ALL_IDENTITY_KEYS = new HashSet<>(Arrays.asList(TYPE_IDENTITY, TYPE_EMAIL, TYPE_PHONE));

    int MAX_DELAY_FREQUENCY = 1000 * 60 * 10;

    String[] NULL_STRING_ARRAY = new String[0];
    //public static final String PT_NOTIF_ID = "notificationId";
    public static final String CLOSE_SYSTEM_DIALOGS = "close_system_dialogs";

}