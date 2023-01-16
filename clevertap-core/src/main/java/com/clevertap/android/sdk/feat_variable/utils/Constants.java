package com.clevertap.android.sdk.feat_variable.utils;

public class Constants {
    public static final String LEANPLUM_PACKAGE_IDENTIFIER = "s";

    public static final String SDK_VERSION = "7.0.1";
    public static int NETWORK_TIMEOUT_SECONDS = 10;
    public static int NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS = 10;
    public static String LEANPLUM_VERSION = SDK_VERSION;
    public static String CLIENT = "android";
    public static String LEANPLUM_SUPPORTED_ENCODING = "gzip";

    static final String INVALID_MAC_ADDRESS = "02:00:00:00:00:00";
    static final String INVALID_MAC_ADDRESS_HASH = "0f607264fc6318a92b9e13c65db7cd3c";
    static final String INVALID_UUID = "00000000-0000-0000-0000-000000000000";

    /**
     * From very old versions of the SDK, leading zeros were stripped from the mac address.
     */
    static final String OLD_INVALID_MAC_ADDRESS_HASH = "f607264fc6318a92b9e13c65db7cd3c";

    static final String INVALID_ANDROID_ID = "9774d56d682e549c";
    static final int MAX_DEVICE_ID_LENGTH = 400;
    static final int MAX_USER_ID_LENGTH = 400;

    public static String defaultDeviceId = null;
    public static boolean isDevelopmentModeEnabled = false;
    public static boolean loggingEnabled = false;
    public static boolean isTestMode = false;
    public static boolean enableVerboseLoggingInDevelopmentMode = false;
    public static boolean enableFileUploadingInDevelopmentMode = true;
    static boolean isInPermanentFailureState = false;

    public static boolean isNoop() {
        return isTestMode || isInPermanentFailureState;
    }

    public static class Defaults {
        public static final String LEANPLUM = "__leanplum__";
        public static final String COUNT_KEY = "__leanplum_unsynced";
        public static final String ITEM_KEY = "__leanplum_unsynced_%d";
        public static final String UUID_KEY = "__leanplum_uuid";
        public static final String VARIABLES_KEY = "__leanplum_variables";
        public static final String VARIABLES_JSON_KEY = "__leanplum_variables_json";
        public static final String VARIABLES_SIGN_KEY = "__leanplum_variables_signature";
        public static final String ATTRIBUTES_KEY = "__leanplum_attributes";
        public static final String TOKEN_KEY = "__leanplum_token";
        public static final String API_HOST_KEY = "__leanplum_api_host";
        public static final String API_PATH_KEY = "__leanplum_api_path";
        public static final String API_SSL_KEY = "__leanplum_api_ssl";
        public static final String SOCKET_HOST_KEY = "__leanplum_socket_host";
        public static final String SOCKET_PORT_KEY = "__leanplum_socket_port";
        public static final String MESSAGES_KEY = "__leanplum_messages";
        public static final String REGIONS_KEY = "regions";
        public static final String MESSAGING_PREF_NAME = "__leanplum_messaging__";
        public static final String MESSAGE_TRIGGER_OCCURRENCES_KEY =
                "__leanplum_message_trigger_occurrences_%s";
        public static final String MESSAGE_IMPRESSION_OCCURRENCES_KEY =
                "__leanplum_message_occurrences_%s";
        public static final String MESSAGE_MUTED_KEY = "__leanplum_message_muted_%s";
        public static final String LOCAL_NOTIFICATION_KEY = "__leanplum_local_message_%s";
        public static final String INBOX_KEY = "__leanplum_newsfeed";
        public static final String LEANPLUM_PUSH = "__leanplum_push__";
        public static final String APP_ID = "__app_id";
        public static final String PROPERTY_FCM_TOKEN_ID = "registration_id";
        public static final String PROPERTY_MIPUSH_TOKEN_ID = "mipush_registration_id";
        public static final String PROPERTY_HMS_TOKEN_ID = "hms_registration_id";
        public static final String PROPERTY_SENDER_IDS = "sender_ids";
        public static final String NOTIFICATION_CHANNELS_KEY = "__leanplum_notification_channels";
        public static final String DEFAULT_NOTIFICATION_CHANNEL_KEY = "__leanplum_default_notification_channels";
        public static final String NOTIFICATION_GROUPS_KEY = "__leanplum_notification_groups";
    }

    public static class Params {
        public static final String RESPONSE = "response";

        public static final String ACTION = "action";
        public static final String ACTION_DEFINITIONS = "actionDefinitions";
        public static final String APP_ID = "appId";
        public static final String BACKGROUND = "background";
        public static final String CLIENT = "client";
        public static final String CLIENT_KEY = "clientKey";
        public static final String DATA = "data";
        public static final String DEV_MODE = "devMode";
        public static final String DEVICE_ID = "deviceId";
        public static final String DEVICE_MODEL = "deviceModel";
        public static final String DEVICE_NAME = "deviceName";
        public static final String DEVICE_FCM_PUSH_TOKEN = "gcmRegistrationId";
        public static final String DEVICE_MIPUSH_TOKEN = "miPushRegId";
        public static final String DEVICE_HMS_TOKEN = "huaweiPushRegId";
        public static final String DEVICE_SYSTEM_NAME = "systemName";
        public static final String DEVICE_SYSTEM_VERSION = "systemVersion";
        public static final String EMAIL = "email";
        public static final String EVENT = "event";
        public static final String FILE = "file";
        public static final String FILE_ATTRIBUTES = "fileAttributes";
        public static final String GOOGLE_PLAY_PURCHASE_DATA = "googlePlayPurchaseData";
        public static final String GOOGLE_PLAY_PURCHASE_DATA_SIGNATURE =
                "googlePlayPurchaseDataSignature";
        public static final String IAP_CURRENCY_CODE = "currencyCode";
        public static final String IAP_ITEM = "item";
        public static final String INCLUDE_DEFAULTS = "includeDefaults";
        public static final String INCLUDE_VARIANT_DEBUG_INFO = "includeVariantDebugInfo";
        public static final String INCLUDE_MESSAGE_ID = "includeMessageId";
        public static final String INFO = "info";
        public static final String INSTALL_DATE = "installDate";
        public static final String KINDS = "kinds";
        public static final String LIMIT_TRACKING = "limitTracking";
        public static final String MESSAGE = "message";
        public static final String NAME = "name";
        public static final String COUNT = "count";
        public static final String MESSAGE_ID = "messageId";
        public static final String NEW_USER_ID = "newUserId";
        public static final String INBOX_MESSAGE_ID = "newsfeedMessageId";
        public static final String INBOX_MESSAGES = "newsfeedMessages";
        public static final String PARAMS = "params";
        public static final String SDK_VERSION = "sdkVersion";
        public static final String STATE = "state";
        public static final String TIME = "time";
        public static final String TYPE = "type";
        public static final String TOKEN = "token";
        public static final String TRAFFIC_SOURCE = "trafficSource";
        public static final String UPDATE_DATE = "updateDate";
        public static final String USER_ID = "userId";
        public static final String USER_ATTRIBUTES = "userAttributes";
        public static final String UUID = "uuid";
        public static final String VALUE = "value";
        public static final String VARS = "vars";
        public static final String VERSION_NAME = "versionName";
        public static final String REQUEST_ID = "reqId";
        public static final String STACK_TRACE = "stackTrace";
        public static final String API_HOST = "apiHost";
        public static final String API_PATH = "apiPath";
        public static final String DEV_SERVER_HOST = "devServerHost";
        public static final String MIGRATE_STATE = "migrateState";
        public static final String MIGRATE_STATE_HASH = "sha256";
        public static final String SDK = "sdk";
        public static final String CT_DUPLICATE = "ct";
        public static final String CLEVERTAP = "ct";
        public static final String CT_ACCOUNT_ID = "accountId";
        public static final String CT_TOKEN = "token";
        public static final String CT_REGION_CODE = "regionCode";
        public static final String CT_ATTRIBUTE_MAPPINGS = "attributeMappings";
        public static final String API_EVENTS_STATE = "events";
    }

    public static class Keys {
        public static final String CITY = "city";
        public static final String COUNTRY = "country";
        public static final String DELIVERY_TIMESTAMP = "deliveryTimestamp";
        public static final String EXPIRATION_TIMESTAMP = "expirationTimestamp";
        public static final String FILENAME = "filename";
        public static final String HASH = "hash";
        public static final String INSTALL_TIME_INITIALIZED = "installTimeInitialized";
        public static final String IS_READ = "isRead";
        public static final String IS_REGISTERED = "isRegistered";
        public static final String IS_REGISTERED_FROM_OTHER_APP = "isRegisteredFromOtherApp";
        public static final String LATEST_VERSION = "latestVersion";
        public static final String LOCALE = "locale";
        public static final String LOCATION = "location";
        public static final String LOCATION_ACCURACY_TYPE = "locationAccuracyType";
        public static final String MESSAGE_DATA = "messageData";
        public static final String MESSAGES = "messages";
        public static final String INBOX_MESSAGES = "newsfeedMessages";
        public static final String PUSH_MESSAGE_ACTION = "_lpx";
        public static final String PUSH_MESSAGE_ID_NO_MUTE_WITH_ACTION = "_lpm";
        public static final String PUSH_MESSAGE_ID_MUTE_WITH_ACTION = "_lpu";
        public static final String PUSH_MESSAGE_ID_NO_MUTE = "_lpn";
        public static final String PUSH_MESSAGE_ID_MUTE = "_lpv";
        public static final String PUSH_MESSAGE_ID = "lp_messageId";
        public static final String PUSH_MESSAGE_NOTIFICATION_ID = "lp_notificationId";
        public static final String PUSH_MESSAGE_TEXT = "lp_message";
        public static final String PUSH_VERSION = "lp_version";
        public static final String PUSH_NOTIFICATION_CHANNEL = "lp_channel";
        public static final String PUSH_MESSAGE_IMAGE_URL = "lp_imageUrl";
        public static final String PUSH_METRIC_SENT_TIME = "sentTime";
        public static final String PUSH_METRIC_OCCURRENCE_ID = "occurrenceId";
        public static final String PUSH_METRIC_CHANNEL = "channel";
        public static final String PUSH_METRIC_MESSAGE_ID = "messageID";
        public static final String PUSH_METRIC_NOTIFICATIONS_ENABLED = "notificationsEnabled";
        public static final String CHANNEL_INTERNAL_KEY = "_channel_internal";
        public static final String PUSH_MESSAGE_SILENT_TRACK = "lp_silent_track";
        public static final String PUSH_SENT_TIME = "lp_sent_time";
        public static final String PUSH_OCCURRENCE_ID = "lp_occurrence_id";
        public static final String LOCAL_PUSH_OCCURRENCE_ID = "internal_occurrence_id";
        public static final String REGION = "region";
        public static final String REGION_STATE = "regionState";
        public static final String REGIONS = "regions";
        public static final String VARIANT_DEBUG_INFO = "variantDebugInfo";
        public static final String SIZE = "size";
        public static final String SUBTITLE = "Subtitle";
        public static final String SYNC_INBOX = "syncNewsfeed";
        public static final String LOGGING_ENABLED = "loggingEnabled";
        public static final String ENABLED_COUNTERS = "enabledSdkCounters";
        public static final String ENABLED_FEATURE_FLAGS = "enabledFeatureFlags";
        public static final String FILES = "files";
        public static final String TIMEZONE = "timezone";
        public static final String TIMEZONE_OFFSET_SECONDS = "timezoneOffsetSeconds";
        public static final String TITLE = "Title";
        public static final String INBOX_IMAGE = "Image";
        public static final String DATA = "Data";
        public static final String TOKEN = "token";
        public static final String VARIANTS = "variants";
        public static final String LOCAL_CAPS = "localCaps";
        public static final String VARS = "vars";
        public static final String VARS_SIGNATURE = "varsSignature";
        public static final String VARS_FROM_CODE = "varsFromCode";
        public static final String NOTIFICATION_CHANNELS = "notificationChannels";
        public static final String DEFAULT_NOTIFICATION_CHANNEL = "defaultNotificationChannel";
        public static final String NOTIFICATION_GROUPS = "notificationChannelGroups";
    }

    public static class Kinds {
        public static final String INT = "integer";
        public static final String FLOAT = "float";
        public static final String STRING = "string";
        public static final String BOOLEAN = "bool";
        public static final String FILE = "file";//
        public static final String DICTIONARY = "group";
        public static final String ARRAY = "list";
        public static final String ACTION = "action";
        public static final String COLOR = "color";//
    }

    public static class Files {
        public static final int MAX_UPLOAD_BATCH_SIZES = (25 * (1 << 20));
        public static final int MAX_UPLOAD_BATCH_FILES = 16;
    }

    public static final String PUSH_DELIVERED_EVENT_NAME = "Push Delivered";
    public static final String PUSH_OPENED_EVENT_NAME = "Push Opened";
    public static final String HELD_BACK_EVENT_NAME = "Held Back";
    public static final String HELD_BACK_MESSAGE_PREFIX = "__held_back__";

    public static class Values {
        public static final String DETECT = "(detect)";
        public static final String RESOURCES_VARIABLE = "__Android Resources";//
        public static final String ACTION_ARG = "__name__";
        public static final String CHAIN_MESSAGE_ARG = "Chained message";
        public static final String DEFAULT_PUSH_ACTION = "Open action";
        public static final String CHAIN_MESSAGE_ACTION_NAME = "Chain to Existing Message";
        public static final String DEFAULT_PUSH_MESSAGE = "Push message goes here.";
        public static final String SDK_LOG = "sdkLog";
        public static final String SDK_COUNT = "sdkCount";
        public static final String FILE_PREFIX = "__file__";
    }

    public static class Crypt {
        public static final int ITER_COUNT = 1000;
        public static final int KEY_LENGTH = 256;
        public static final String SALT = "L3@nP1Vm"; // Must have 8 bytes
        public static final String IV = "__l3anplum__iv__"; // Must have 16 bytes
    }

    public static class Messaging {
        public static final int MAX_STORED_OCCURRENCES_PER_MESSAGE = 100;
        public static final int DEFAULT_PRIORITY = 1000;
    }
}
