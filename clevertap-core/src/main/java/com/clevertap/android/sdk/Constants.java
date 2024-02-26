package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashSet;

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
    String LABEL_PROXY_DOMAIN = "CLEVERTAP_PROXY_DOMAIN";
    String LABEL_SPIKY_PROXY_DOMAIN = "CLEVERTAP_SPIKY_PROXY_DOMAIN";
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
    String LABEL_ENCRYPTION_LEVEL = "CLEVERTAP_ENCRYPTION_LEVEL";
    String LABEL_DEFAULT_CHANNEL_ID = "CLEVERTAP_DEFAULT_CHANNEL_ID";
    String FCM_FALLBACK_NOTIFICATION_CHANNEL_ID = "fcm_fallback_notification_channel";
    String FCM_FALLBACK_NOTIFICATION_CHANNEL_NAME = "Misc";
    String CLEVERTAP_OPTOUT = "ct_optout";
    String CLEVERTAP_STORAGE_TAG = "WizRocket";
    String CLEVERTAP_LOG_TAG = "CleverTap";
    int SESSION_LENGTH_MINS = 20;
    String DEVICE_ID_TAG = "deviceId";
    String FALLBACK_ID_TAG = "fallbackId";
    int PAGE_EVENT = 1;
    int PING_EVENT = 2;
    int PROFILE_EVENT = 3;
    int RAISED_EVENT = 4;
    int DATA_EVENT = 5;
    int NV_EVENT = 6;
    int FETCH_EVENT = 7;
    int DEFINE_VARS_EVENT = 8;
    String variablePayloadType = "varsPayload";
    String WZRK_FETCH = "wzrk_fetch";
    String ICON_BASE_URL = "http://static.wizrocket.com/android/ico/";
    String NOTIFICATION_CLICKED_EVENT_NAME = "Notification Clicked";
    String NOTIFICATION_VIEWED_EVENT_NAME = "Notification Viewed";
    String SC_OUTGOING_EVENT_NAME = "SCOutgoing";
    String SC_INCOMING_EVENT_NAME = "SCIncoming";
    String SC_END_EVENT_NAME = "SCEnd";
    String SC_CAMPAIGN_OPT_OUT_EVENT_NAME = "SCCampaignOptOut";
    String GEOFENCE_ENTERED_EVENT_NAME = "Geocluster Entered";
    String GEOFENCE_EXITED_EVENT_NAME = "Geocluster Exited";
    String APP_LAUNCHED_EVENT = "App Launched";
    String ERROR_KEY = "wzrk_error";
    int PUSH_DELAY_MS = 1000;
    String INAPP_PREVIEW_PUSH_PAYLOAD_KEY = "wzrk_inapp";
    String INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY = "wzrk_inapp_type";
    String INAPP_IMAGE_INTERSTITIAL_TYPE = "image-interstitial";
    String INAPP_IMAGE_INTERSTITIAL_CONFIG = "imageInterstitialConfig";
    String INAPP_HTML_SPLIT = "\"##Vars##\"";
    String INAPP_IMAGE_INTERSTITIAL_HTML_NAME = "image_interstitial.html";
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

    String INAPP_NOTIFS_STALE_KEY = "inapp_stale";
    String INAPP_NOTIFS_APP_LAUNCHED_KEY = "inapp_notifs_applaunched";

    String INAPP_NOTIFS_KEY_CS = "inapp_notifs_cs";
    String INAPP_NOTIFS_KEY_SS = "inapp_notifs_ss";

    String INAPP_DELIVERY_MODE_KEY = "inapp_delivery_mode";

    String PREFS_INAPP_KEY_CS = INAPP_NOTIFS_KEY_CS;
    String PREFS_INAPP_KEY_SS = INAPP_NOTIFS_KEY_SS;
    String PREFS_EVALUATED_INAPP_KEY_SS = "evaluated_ss";
    String PREFS_SUPPRESSED_INAPP_KEY_CS = "suppressed_ss";

    String INBOX_JSON_RESPONSE_KEY = "inbox_notifs";
    String DISPLAY_UNIT_JSON_RESPONSE_KEY = "adUnit_notifs";
    String FEATURE_FLAG_JSON_RESPONSE_KEY = "ff_notifs";
    String REQUEST_VARIABLES_JSON_RESPONSE_KEY = "vars";
    String REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY = "pc_notifs";
    String GEOFENCES_JSON_RESPONSE_KEY = "geofences";
    String DISCARDED_EVENT_JSON_KEY = "d_e";
    String INAPP_MAX_DISPLAY_COUNT = "mdc";
    String INAPP_MAX_PER_SESSION_KEY = "imc";
    String INAPP_MAX_PER_DAY_KEY = "imp";
    String INAPP_WINDOW = "w";
    String INAPP_KEY = "inApp";
    String INAPP_WZRK_PIVOT = "wzrk_pivot";
    String INAPP_WZRK_CGID = "wzrk_cgId";
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
    String PRIMARY_DOMAIN = "clevertap-prod.com";
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
    String CACHED_VARIABLES_KEY = "variablesKey";
    String MULTI_USER_PREFIX = "mt_";
    String NOTIFICATION_TAG = "wzrk_pn";
    String CHARGED_EVENT = "Charged";
    String KEY_ITEMS = "Items";
    String KEY_MUTED = "comms_mtd";
    int EMPTY_NOTIFICATION_ID = -1000;
    String KEY_MAX_PER_DAY = "istmcd_inapp";
    String KEY_COUNTS_SHOWN_TODAY = "istc_inapp";
    String KEY_COUNTS_PER_INAPP = "counts_per_inapp";

    String KEY_TRIGGERS_PER_INAPP = "triggers_per_inapp";
    String INAPP_ID_IN_PAYLOAD = "ti";
    int LOCATION_PING_INTERVAL_IN_SECONDS = 10;
    String[] SYSTEM_EVENTS = {NOTIFICATION_CLICKED_EVENT_NAME,
            NOTIFICATION_VIEWED_EVENT_NAME, GEOFENCE_ENTERED_EVENT_NAME,
            GEOFENCE_EXITED_EVENT_NAME};
    long DEFAULT_PUSH_TTL = 1000L * 60 * 60 * 24 * 4;// 4 days
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
    String KEY_PROXY_DOMAIN = "proxyDomain";
    String KEY_SPIKY_PROXY_DOMAIN = "spikyProxyDomain";
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
    String KEY_EXCLUDE_GLOBAL_CAPS = "excludeGlobalFCaps";
    String KEY_TLC = "tlc";
    String KEY_TDC = "tdc";
    String KEY_KV = "kv";
    String KEY_TYPE = "type";
    String KEY_LIMIT = "limit";
    String KEY_FREQUENCY = "frequency";
    String KEY_T = "t";
    String KEY_EVT_NAME = "evtName";
    String KEY_EVT_DATA = "evtData";
    String KEY_FALLBACK_NOTIFICATION_SETTINGS = "fbSettings";
    String KEY_REQUEST_FOR_NOTIFICATION_PERMISSION = "rfp";
    int NOTIFICATION_PERMISSION_REQUEST_CODE = 102;
    String KEY_IS_TABLET = "tablet";
    String KEY_BG = "bg";
    String KEY_TITLE = "title";
    String KEY_TEXT = "text";
    String KEY_KEY = "key";
    String KEY_VALUE = "value";
    String KEY_EVENT_NAME = "eventName";
    String KEY_EVENT_PROPERTIES = "eventProperties";
    String KEY_ITEM_PROPERTIES = "itemProperties";
    String KEY_GEO_RADIUS_PROPERTIES = "geoRadius";
    String KEY_PROPERTY_VALUE = "propertyValue";
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
    String KEY_ENCRYPTION_LEVEL = "encryptionLevel";
    String KEY_ENCRYPTION_FLAG_STATUS = "encryptionFlagStatus";
    String WZRK_PUSH_ID = "wzrk_pid";
    String WZRK_PUSH_SILENT = "wzrk_pn_s";
    String EXTRAS_FROM = "extras_from";
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
    String WZRK_TIME_TO_LIVE_OFFSET = "wzrk_ttl_offset";
    String INAPP_SUPPRESSED = "suppressed";
    String INAPP_SS_EVAL_META = "inapps_eval";
    String INAPP_SUPPRESSED_META = "inapps_suppressed";
    String INAPP_OPERATOR = "operator";
    String INAPP_PROPERTYNAME = "propertyName";
    String INAPP_WHEN_TRIGGERS = "whenTriggers";
    String INAPP_WHEN_LIMITS = "whenLimit";
    String INAPP_FC_LIMITS = "frequencyLimits";
    String INAPP_OCCURRENCE_LIMITS = "occurrenceLimits";

    String INAPP_PRIORITY = "priority";

    String CLTAP_PROP_CAMPAIGN_ID = "Campaign id";
    String CLTAP_PROP_VARIANT = "Variant";
    String CLTAP_APP_VERSION = "Version";
    String CLTAP_LATITUDE = "Latitude";
    String CLTAP_LONGITUDE = "Longitude";
    String CLTAP_OS_VERSION = "OS Version";
    String CLTAP_SDK_VERSION = "SDK Version";
    String CLTAP_CARRIER = "Carrier";
    String CLTAP_NETWORK_TYPE = "Radio";
    String CLTAP_CONNECTED_TO_WIFI = "wifi";
    String CLTAP_BLUETOOTH_VERSION = "BluetoothVersion";
    String CLTAP_BLUETOOTH_ENABLED = "BluetoothEnabled";

    String WZRK_RNV = "wzrk_rnv";
    String BLACK = "#000000";
    String WHITE = "#FFFFFF";
    String BLUE = "#0000FF";
    String GREEN = "#00FF00";
    String LIGHT_BLUE = "#818ce5";
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
    String KEY_ENCRYPTION_MIGRATION = "encryptionmigration";
    String KEY_ENCRYPTION_CGK = "cgk";
    String KEY_ENCRYPTION_INAPP_CS = "cs";

    String KEY_ENCRYPTION_INAPP_SS = "ss";

    String KEY_ENCRYPTION_NAME = "Name";
    String KEY_ENCRYPTION_IDENTITY = "Identity";
    String KEY_ENCRYPTION_PHONE = "Phone";
    String KEY_ENCRYPTION_EMAIL = "Email";
    String TEST_IDENTIFIER = "0_0";
    String FEATURE_DISPLAY_UNIT = "DisplayUnit : ";
    String FEATURE_FLAG_UNIT = "Feature Flag : ";
    String LOG_TAG_PRODUCT_CONFIG = "Product Config : ";

    String CRYPTION_SALT = "W1ZRCl3>";
    String CRYPTION_IV = "__CL3>3Rt#P__1V_";

    int FETCH_TYPE_PC = 0;
    int FETCH_TYPE_FF = 1;
    int FETCH_TYPE_VARIABLES = 4;
    int FETCH_TYPE_IN_APPS = 5;
    String LOG_TAG_SIGNED_CALL = "SignedCall : ";
    String LOG_TAG_GEOFENCES = "Geofences : ";
    String LOG_TAG_INAPP = "InApp : ";
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
    int ENCRYPTION_FLAG_FAIL = 0b00;
    int ENCRYPTION_FLAG_CGK_SUCCESS = 0b01;
    int ENCRYPTION_FLAG_DB_SUCCESS = 0b10;
    int ENCRYPTION_FLAG_ALL_SUCCESS = 0b11;

    String CLEVERTAP_IDENTIFIER = "CLEVERTAP_IDENTIFIER";
    String SEPARATOR_COMMA = ",";
    String EMPTY_STRING = "";
    String AUTH = "auth";
    String SP_KEY_PROFILE_IDENTITIES = "SP_KEY_PROFILE_IDENTITIES";

    // valid profile identifier keys
    HashSet<String> LEGACY_IDENTITY_KEYS = new HashSet<>(Arrays.asList(TYPE_IDENTITY, TYPE_EMAIL));
    HashSet<String> ALL_IDENTITY_KEYS = new HashSet<>(Arrays.asList(TYPE_IDENTITY, TYPE_EMAIL, TYPE_PHONE));
    HashSet<String> MEDIUM_CRYPT_KEYS = new HashSet<>(Arrays.asList(KEY_ENCRYPTION_CGK, KEY_ENCRYPTION_MIGRATION, KEY_ENCRYPTION_EMAIL, KEY_ENCRYPTION_PHONE, KEY_ENCRYPTION_IDENTITY, KEY_ENCRYPTION_NAME));
    HashSet<String> NONE_CRYPT_KEYS = new HashSet<>(Arrays.asList(KEY_ENCRYPTION_MIGRATION));
    HashSet<String> piiDBKeys = new HashSet<>(Arrays.asList(KEY_ENCRYPTION_NAME, KEY_ENCRYPTION_EMAIL, KEY_ENCRYPTION_IDENTITY, KEY_ENCRYPTION_PHONE));


    int MAX_DELAY_FREQUENCY = 1000 * 60 * 10;

    /**
     * Valid indexes for the App Inbox item and buttons.
     */
    int APP_INBOX_ITEM_CONTENT_PAGE_INDEX = 0; //used for non-carousel templates as they have only one page of content to display
    int APP_INBOX_ITEM_INDEX = -1;
    int APP_INBOX_CTA1_INDEX = 0;
    int APP_INBOX_CTA2_INDEX = 1;
    int APP_INBOX_CTA3_INDEX = 2;

    String[] NULL_STRING_ARRAY = new String[0];
    String PT_NOTIF_ID = "notificationId";
    String CLOSE_SYSTEM_DIALOGS = "close_system_dialogs";
    String KEY_CT_TYPE = "ct_type";
    String PT_INPUT_KEY = "pt_input_reply";

    // ==========Fallback keys=========
    String WZRK_TSR_FB = "wzrk_tsr_fb";// terminate and stay resident
    String NOTIFICATION_RENDER_FALLBACK = "wzrk_fallback";
    String OMR_INVOKE_TIME_IN_MILLIS = "omr_invoke_time_in_millis";
    String WZRK_BPDS = "wzrk_bpds";
    String WZRK_PN_PRT = "wzrk_pn_prt";
    String PRIORITY_NORMAL = "normal";
    String PRIORITY_UNKNOWN = "fcm_unknown";
    String D_SRC = "d_src";// data source for push impressions
    String D_SRC_PI_R = "PI_R";// push impression data source is Receiver
    String D_SRC_PI_WM = "PI_WM";// push impression data source is work manager

    String REGION_INDIA = "in1";
    String REGION_EUROPE = "eu1";

    // ============ notification image download timeout ===================

    int PN_IMAGE_CONNECTION_TIMEOUT_IN_MILLIS =  1000;
    int PN_IMAGE_READ_TIMEOUT_IN_MILLIS =  5000;
    long PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS =  5000;
    long PN_LARGE_ICON_DOWNLOAD_TIMEOUT_IN_MILLIS =  2000;

    //==============

   String FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME = "CTFlushPushImpressionsOneTime";

}