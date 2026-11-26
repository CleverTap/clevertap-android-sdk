package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

// Bucket D: shared with the other SDK modules. `const val` members compile to static fields on
// Constants; the few non-constant values are read-only collections.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object Constants {

    // Nested types don't inherit the outer @RestrictTo in BCV's dump - restrict it explicitly.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(TYPE_EMAIL, TYPE_PHONE, TYPE_IDENTITY)
    public annotation class IdentityType

    public const val TAG_FEATURE_IN_APPS: String = "TAG_FEATURE_IN_APPS"
    public const val TYPE_IDENTITY: String = "Identity"
    public const val TYPE_EMAIL: String = "Email"
    public const val TYPE_PHONE: String = "Phone"

    public const val FCM_FALLBACK_NOTIFICATION_CHANNEL_ID: String = "fcm_fallback_notification_channel"
    public const val CT_FALLBACK_NOTIFICATION_CHANNEL_ID_LOW: String = "ct_fallback_notification_channel_low_importance"
    public const val FCM_FALLBACK_NOTIFICATION_CHANNEL_NAME: String = "Misc"
    public const val LOW_IMPORTANCE_FALLBACK_NOTIFICATION_CHANNEL_NAME: String = "Silent Misc"
    public const val CLEVERTAP_OPTOUT: String = "ct_optout"
    public const val CLEVERTAP_ALLOW_SYSTEM_EVENTS: String = "ct_allow_sys_events_and_communication"
    public const val CLEVERTAP_STORAGE_TAG: String = "WizRocket"
    public const val CLEVERTAP_LOG_TAG: String = "CleverTap"
    public const val SESSION_LENGTH_MINS: Int = 20
    public const val DEVICE_ID_TAG: String = "deviceId"
    public const val FALLBACK_ID_TAG: String = "fallbackId"
    public const val PAGE_EVENT: Int = 1
    public const val PING_EVENT: Int = 2
    public const val PROFILE_EVENT: Int = 3
    public const val RAISED_EVENT: Int = 4
    public const val DATA_EVENT: Int = 5
    public const val NV_EVENT: Int = 6
    public const val FETCH_EVENT: Int = 7
    public const val DEFINE_VARS_EVENT: Int = 8
    public const val variablePayloadType: String = "varsPayload"
    public const val WZRK_FETCH: String = "wzrk_fetch"
    public const val WZRK_MID: String = "wzrk_mid"
    public const val NOTIFICATION_CLICKED_EVENT_NAME: String = "Notification Clicked"
    public const val NOTIFICATION_VIEWED_EVENT_NAME: String = "Notification Viewed"
    public const val SC_OUTGOING_EVENT_NAME: String = "SCOutgoing"
    public const val SC_INCOMING_EVENT_NAME: String = "SCIncoming"
    public const val SC_END_EVENT_NAME: String = "SCEnd"
    public const val SC_CAMPAIGN_OPT_OUT_EVENT_NAME: String = "SCCampaignOptOut"
    public const val GEOFENCE_ENTERED_EVENT_NAME: String = "Geocluster Entered"
    public const val GEOFENCE_EXITED_EVENT_NAME: String = "Geocluster Exited"
    public const val APP_LAUNCHED_EVENT: String = "App Launched"
    public const val ERROR_KEY: String = "wzrk_error"
    public const val WZRK_URL_SCHEMA: String = "wzrk://"
    public const val INAPP_PREVIEW_PUSH_PAYLOAD_KEY: String = "wzrk_inapp"
    public const val INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY: String = "wzrk_inapp_type"
    public const val INAPP_PREVIEW_S3_URL_KEY: String = "wzrk_inapp_s3_url"
    public const val INAPP_IMAGE_INTERSTITIAL_TYPE: String = "image-interstitial"
    public const val INAPP_ADVANCED_BUILDER_TYPE: String = "advanced-builder"
    public const val INAPP_IMAGE_INTERSTITIAL_CONFIG: String = "imageInterstitialConfig"
    public const val INAPP_HTML_SPLIT: String = "\"##Vars##\""
    public const val INAPP_IMAGE_INTERSTITIAL_HTML_NAME: String = "image_interstitial.html"
    public const val INBOX_PREVIEW_PUSH_PAYLOAD_KEY: String = "wzrk_inbox"
    public const val DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY: String = "wzrk_adunit"
    public const val INAPP_HTML_TAG: String = "html"
    public const val INAPP_DATA_TAG: String = "d"
    public const val INAPP_X_PERCENT: String = "xp"
    public const val INAPP_Y_PERCENT: String = "yp"
    public const val INAPP_X_DP: String = "xdp"
    public const val INAPP_Y_DP: String = "ydp"
    public const val INAPP_POSITION: String = "pos"
    public const val INAPP_ASPECT_RATIO: String = "aspectRatio"
    public const val INAPP_POSITION_TOP: Char = 't'
    public const val INAPP_POSITION_RIGHT: Char = 'r'
    public const val INAPP_POSITION_BOTTOM: Char = 'b'
    public const val INAPP_POSITION_LEFT: Char = 'l'
    public const val INAPP_POSITION_CENTER: Char = 'c'
    public const val INAPP_NOTIF_DARKEN_SCREEN: String = "dk"
    public const val INAPP_NOTIF_SHOW_CLOSE: String = "sc"
    public const val INAPP_JSON_RESPONSE_KEY: String = "inapp_notifs"
    public const val INAPP_NOTIFS_META_KEY: String = "inapp_notifs_meta"

    public const val INAPP_NOTIFS_STALE_KEY: String = "inapp_stale"
    public const val INAPP_NOTIFS_APP_LAUNCHED_KEY: String = "inapp_notifs_applaunched"
    public const val INAPP_NOTIFS_APP_LAUNCHED_META_KEY: String = "inapp_notifs_applaunched_meta"

    public const val INAPP_NOTIFS_KEY_CS: String = "inapp_notifs_cs"
    public const val INAPP_NOTIFS_KEY_SS: String = "inapp_notifs_ss"

    public const val INAPP_DELIVERY_MODE_KEY: String = "inapp_delivery_mode"

    public const val PREFS_INAPP_KEY_CS: String = INAPP_NOTIFS_KEY_CS
    public const val PREFS_INAPP_KEY_SS: String = INAPP_NOTIFS_KEY_SS
    public const val PREFS_EVALUATED_INAPP_KEY_SS: String = "evaluated_ss"
    public const val PREFS_SUPPRESSED_INAPP_KEY_CS: String = "suppressed_ss"

    // ---- Native Display (ND) frequency caps (SDK-6055) ----
    // ND == Display Units channel. SS + legacy only (no CS mode). Mirrors the in-app keys above.
    // Response keys (LC -> SDK). Campaign content still arrives under DISPLAY_UNIT_JSON_RESPONSE_KEY ("adUnit_notifs").
    public const val DISPLAY_UNIT_NOTIFS_SS_KEY: String = "adUnit_notifs_ss"                 // advanced-rule metadata bundle (rules only)
    public const val DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY: String = "adUnit_notifs_applaunched" // app-launch content + CG-suppressed stubs
    public const val DISPLAY_UNIT_NOTIFS_STALE_KEY: String = "adUnit_stale"                  // dead-target GC ids
    // Request meta keys (SDK -> LC)
    public const val ND_SS_EVAL_META: String = "adUnit_eval"                                 // eligible advanced ND campaign votes
    public const val ND_SUPPRESSED_META: String = "adUnit_suppressed"                        // CG-suppression acks (app-launch path only)
    public const val ND_TARGET_SHOWN_LIST: String = "ndtlc"                                  // [[ti, todayCount, lifetimeCount], ...]
    // Wire keys with directional overload (mirror in-app imc/imp)
    public const val ND_MAX_PER_SESSION_KEY: String = "ndmc"                                 // resp: session ceiling; also storage key
    public const val ND_MAX_PER_DAY_KEY: String = "ndmp"                                     // req: SDK daily count; resp: daily ceiling
    // ND store prefs namespace + keys
    public const val ND_KEY: String = "adUnit"                                               // ND metadata store prefs namespace
    public const val PREFS_ND_KEY_SS: String = DISPLAY_UNIT_NOTIFS_SS_KEY                    // metadata bundle pref key
    public const val PREFS_EVALUATED_ND_KEY_SS: String = "evaluated_nd_ss"                   // pending adUnit_eval report
    public const val PREFS_SUPPRESSED_ND_KEY: String = "suppressed_nd"                       // pending adUnit_suppressed report

    public const val INBOX_JSON_RESPONSE_KEY: String = "inbox_notifs"
    public const val INBOX_V2_JSON_RESPONSE_KEY: String = "inbox_notifs_v2"
    public const val INBOX_V2_ISREAD_KEY: String = "isRead"
    public const val INBOX_V2_THROTTLE_WINDOW_MS: Long = 5L * 60L * 1000L
    public const val DISPLAY_UNIT_JSON_RESPONSE_KEY: String = "adUnit_notifs"
    public const val CONTENT_FETCH_JSON_RESPONSE_KEY: String = "content_fetch"
    // Fields on a single content_fetch directive item (self-describing; see ContentFetchItem).
    public const val CONTENT_FETCH_ITEM_EVENT_NAME: String = "eventName"
    public const val CONTENT_FETCH_ITEM_RESPONSE_KEY: String = "responseKey"
    public const val CONTENT_FETCH_ITEM_TARGET_ID: String = "tgtId"
    public const val FEATURE_FLAG_JSON_RESPONSE_KEY: String = "ff_notifs"
    public const val REQUEST_VARIABLES_JSON_RESPONSE_KEY: String = "vars"
    public const val REQUEST_VARIANTS_JSON_RESPONSE_KEY: String = "abVariantInfo"
    public const val REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY: String = "pc_notifs"
    public const val GEOFENCES_JSON_RESPONSE_KEY: String = "geofences"
    public const val DISCARDED_EVENT_JSON_KEY: String = "d_e"
    public const val INAPP_MAX_DISPLAY_COUNT: String = "mdc"
    public const val INAPP_MAX_PER_SESSION_KEY: String = "imc"
    public const val INAPP_MAX_PER_DAY_KEY: String = "imp"
    public const val INAPP_WINDOW: String = "w"
    public const val INAPP_KEY: String = "inApp"
    public const val INAPP_ID_KEY: String = "inAppId"
    public const val INAPP_WZRK_PIVOT: String = "wzrk_pivot"
    public const val INAPP_WZRK_CGID: String = "wzrk_cgId"
    public const val INAPP_CLOSE_IV_WIDTH: Int = 40
    public const val INAPP_CLOSE_IV_TOUCH_TARGET_WIDTH: Int = 48
    public const val INAPP_JS_ENABLED: String = "isJsEnabled"
    public const val NOTIFICATION_ID_TAG: String = "wzrk_id"
    public const val DEEP_LINK_KEY: String = "wzrk_dl"
    public const val WZRK_PREFIX: String = "wzrk_"
    public const val NOTIFICATION_ID_TAG_INTERVAL: Int = 5000
    public const val NOTIFICATION_VIEWED_ID_TAG_INTERVAL: Int = 2000
    public const val SESSION_ID_LAST: String = "lastSessionId"
    public const val LAST_SESSION_EPOCH: String = "sexe"
    public const val MAX_KEY_LENGTH: Int = 120
    public const val MAX_VALUE_LENGTH: Int = 512
    public const val WZRK_FROM_KEY: String = "wzrk_from"
    public const val WZRK_ACCT_ID_KEY: String = "wzrk_acct_id"
    public const val WZRK_FROM: String = "CTPushNotificationReceiver"
    public const val NETWORK_INFO: String = "NetworkInfo"
    public const val PRIMARY_DOMAIN: String = "clevertap-prod.com"
    public const val CACHED_GUIDS_KEY: String = "cachedGUIDsKey"
    public const val CACHED_GUIDS_LENGTH_KEY: String = "cachedGUIDsLengthKey"
    public const val CACHED_VARIABLES_KEY: String = "variablesKey"
    public const val CACHED_VARIANTS_KEY: String = "variantsForAbKey"
    public const val MULTI_USER_PREFIX: String = "mt_"
    public const val NOTIFICATION_TAG: String = "wzrk_pn"
    public const val CHARGED_EVENT: String = "Charged"
    public const val PROFILE: String = "profile"
    public const val RAISED: String = "raised"
    public const val USER_ATTRIBUTE_CHANGE: String = "_CTUserAttributeChange"
    public const val KEY_NEW_VALUE: String = "newValue"
    public const val KEY_OLD_VALUE: String = "oldValue"
    public const val KEY_ITEMS: String = "Items"
    public const val KEY_MUTED: String = "comms_mtd"
    public const val KEY_MUTE_EXPIRY: String = "comms_mute_expiry_ts"
    public const val DEFAULT_MUTE_DURATION_MS: Long = 24 * 60 * 60 * 1000L
    public const val EMPTY_NOTIFICATION_ID: Int = -1000
    public const val KEY_MAX_PER_DAY: String = "istmcd_inapp"
    public const val KEY_COUNTS_SHOWN_TODAY: String = "istc_inapp"
    public const val KEY_COUNTS_PER_INAPP: String = "counts_per_inapp"

    public const val KEY_TRIGGERS_PER_INAPP: String = "triggers_per_inapp"

    // ---- Native Display (ND) frequency-cap storage keys (SDK-6055), mirror the in-app keys above ----
    public const val KEY_ND_MAX_PER_DAY: String = "ndstmcd"                 // stored ND daily ceiling (mirror of istmcd_inapp)
    public const val KEY_ND_COUNTS_SHOWN_TODAY: String = "ndstc"            // stored ND global shown-today (mirror of istc_inapp)
    public const val KEY_ND_LAST_RESET_DATE: String = "nd_ict_date"         // ddMMyyyy of the last daily reset (mirror of ict_date)
    public const val KEY_ND_COUNTS_PER_TARGET: String = "nd_counts_per_target"   // NdCountsStore namespace (per-target counts + globals)
    public const val KEY_ND_IMPRESSIONS_PER_TARGET: String = "nd_impressions"    // ND ImpressionStore namespace (whenLimits timestamps)
    public const val KEY_ND_TRIGGERS_PER_TARGET: String = "nd_triggers_per_target" // onEvery/onExactly trigger counts
    public const val INAPP_ID_IN_PAYLOAD: String = "ti"
    public const val LOCATION_PING_INTERVAL_IN_SECONDS: Int = 10
    public val SYSTEM_EVENTS: List<String> = listOf(
        NOTIFICATION_CLICKED_EVENT_NAME,
        NOTIFICATION_VIEWED_EVENT_NAME,
        GEOFENCE_ENTERED_EVENT_NAME,
        GEOFENCE_EXITED_EVENT_NAME
    )
    public const val DEFAULT_PUSH_TTL_SECONDS: Long = 60 * 60 * 24 * 4// 4 days
    public const val ONE_MIN_IN_MILLIS: Long = 60 * 1000L
    public const val ONE_DAY_IN_MILLIS: Long = 24 * 60 * 60 * 1000L
    public const val COPY_TYPE: String = "copy"
    public const val DND_START: String = "22:00"
    public const val DND_STOP: String = "06:00"
    public const val VIDEO_THUMBNAIL: String = "ct_video_1"
    public const val AUDIO_THUMBNAIL: String = "ct_audio"
    public const val IMAGE_PLACEHOLDER: String = "ct_image"
    public const val KEY_CONFIG: String = "config"
    public const val KEY_C2A: String = "wzrk_c2a"

    // Split-of-clicks: per-element identity + action descriptors (must match iOS wire contract)
    public const val KEY_WZRK_ELEMENT_ID: String = "wzrk_element_id"
    public const val KEY_WZRK_ACTION: String = "wzrk_action"
    public const val KEY_WZRK_DATA: String = "wzrk_data"

    // key for parsing
    public const val KEY_SWIPE_TO_DISMISS: String = "swipe-dismiss"

    // wzrk_element_id values
    public const val INAPP_ELEMENT_ID_IMAGE: String = "image-1"
    public const val INAPP_ELEMENT_ID_CLOSE: String = "closeButton"
    public const val INAPP_ELEMENT_ID_BUTTON_PREFIX: String = "button-"
    // PIP exposes a single CTA button; its element id is a fixed literal
    public const val INAPP_ELEMENT_ID_PIP_CTA: String = "button-cta"

    // wzrk_c2a values for dismiss gestures
    public const val INAPP_CTA_DISMISS_BUTTON: String = "Dismiss Button"
    public const val INAPP_CTA_SWIPE_DISMISS: String = "Swipe to Dismiss"

    // Programmatic PIP dismiss via the public dismissPipInApp() API.
    public const val INAPP_ELEMENT_ID_DISMISS_API: String = "dismissApi"
    public const val INAPP_CTA_DISMISS_PIP_API: String = "Dismiss PiP API"

    // wzrk_data literal value for a close action
    public const val INAPP_WZRK_DATA_CLOSE: String = "close"

    // Media-error synthetic-close descriptors emitted by the bundled advanced-builder HTML template
    public const val INAPP_CTA_IMAGE_ERROR_DISMISS: String = "image-error-dismiss"
    public const val INAPP_CTA_VIDEO_ERROR_DISMISS: String = "video-error-dismiss"

    // wzrk_error codes/messages for media preload failures.
    public const val INAPP_IMAGE_LOAD_FAILED_ERROR_CODE: Int = 591
    public const val INAPP_VIDEO_LOAD_FAILED_ERROR_CODE: Int = 592
    public const val INAPP_IMAGE_LOAD_FAILED_ERROR_MSG: String = "InApp image failed to load"
    public const val INAPP_VIDEO_LOAD_FAILED_ERROR_MSG: String = "InApp video failed to load"

    // wzrk_error codes/messages for PIP media failures (all URLs failed, PIP never showed).
    // One code per PIP media type, keyed by the campaign's primary media type.
    public const val INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_CODE: Int = 593
    public const val INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_CODE: Int = 594
    public const val INAPP_PIP_GIF_LOAD_FAILED_ERROR_CODE: Int = 595
    public const val INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_MSG: String = "InApp PIP image failed to load"
    public const val INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_MSG: String = "InApp PIP video failed to load"
    public const val INAPP_PIP_GIF_LOAD_FAILED_ERROR_MSG: String = "InApp PIP GIF failed to load"

    // wzrk_error codes/messages for the server-side in-app queue dropped on OutOfMemoryError.
    public const val INAPP_QUEUE_SAVE_OOM_ERROR_CODE: Int = 596
    public const val INAPP_QUEUE_READ_OOM_ERROR_CODE: Int = 597
    public const val INAPP_QUEUE_SAVE_OOM_ERROR_MSG: String = "InApp queue dropped on save (OOM)"
    public const val INAPP_QUEUE_READ_OOM_ERROR_MSG: String = "InApp queue dropped on read (OOM)"
    // Android could not load the in-app prefs file (OutOfMemoryError); the queue is kept as is.
    public const val INAPP_QUEUE_PREFS_LOAD_OOM_ERROR_CODE: Int = 598
    public const val INAPP_QUEUE_PREFS_LOAD_OOM_ERROR_MSG: String = "InApp queue unavailable, prefs load failed (OOM)"

    public const val KEY_EFC: String = "efc"
    public const val KEY_EXCLUDE_GLOBAL_CAPS: String = "excludeGlobalFCaps"
    // ND opt-in flag (ONC-418). Absent/false => campaign is outside the ND fcap regime; true => honor the
    // fcap fields present. Gates whether a shown ND unit counts toward the global ndmp/ndtlc budget.
    public const val KEY_IS_ND_FCAP_ENABLED: String = "isNdFcapEnabled"
    public const val KEY_TLC: String = "tlc"
    public const val KEY_TDC: String = "tdc"
    public const val KEY_KV: String = "kv"
    public const val KEY_TYPE: String = "type"
    public const val KEY_LIMIT: String = "limit"
    public const val KEY_FREQUENCY: String = "frequency"
    public const val KEY_T: String = "t"
    public const val KEY_EVT_NAME: String = "evtName"
    public const val KEY_EVT_DATA: String = "evtData"
    public const val KEY_FALLBACK_NOTIFICATION_SETTINGS: String = "fbSettings"
    public const val KEY_REQUEST_FOR_NOTIFICATION_PERMISSION: String = "rfp"
    public const val NOTIFICATION_PERMISSION_REQUEST_CODE: Int = 102
    public const val KEY_IS_TABLET: String = "tablet"
    public const val KEY_BG: String = "bg"
    public const val KEY_TITLE: String = "title"
    public const val KEY_TEXT: String = "text"
    public const val KEY_KEY: String = "key"
    public const val KEY_VALUE: String = "value"
    public const val KEY_COLOR: String = "color"
    public const val KEY_MESSAGE: String = "message"
    public const val KEY_HIDE_CLOSE: String = "close"
    public const val KEY_MEDIA: String = "media"
    public const val KEY_MEDIA_LANDSCAPE: String = "mediaLandscape"
    public const val KEY_PORTRAIT: String = "hasPortrait"
    public const val KEY_LANDSCAPE: String = "hasLandscape"
    public const val KEY_CONTENT_TYPE: String = "content_type"
    public const val KEY_URL: String = "url"
    public const val KEY_ALT_TEXT: String = "alt_text"
    public const val KEY_BUTTONS: String = "buttons"
    public const val KEY_CUSTOM_HTML: String = "custom-html"
    public const val WZRK_PUSH_ID: String = "wzrk_pid"
    public const val WZRK_DEDUPE: String = "wzrk_dd"
    public const val WZRK_PUSH_SILENT: String = "wzrk_pn_s"
    public const val NOTIF_MSG: String = "nm"
    public const val NOTIF_TITLE: String = "nt"
    public const val NOTIF_ICON: String = "ico"
    public const val NOTIF_HIDE_APP_LARGE_ICON: String = "wzrk_hide_large_icon"
    public const val WZRK_ACTIONS: String = "wzrk_acts"
    public const val WZRK_BIG_PICTURE: String = "wzrk_bp"
    public const val WZRK_BIG_PICTURE_ALT_TEXT_KEY: String = "alt_text_wzrk_bp"
    public const val WZRK_GIF: String = "wzrk_gif"
    public const val PUSH_DIRECTORY_NAME: String = "CleverTap.Push"
    public const val WZRK_MSG_SUMMARY: String = "wzrk_nms"
    public const val NOTIF_PRIORITY: String = "pr"
    public const val PRIORITY_HIGH: String = "high"
    public const val PRIORITY_MAX: String = "max"
    public const val WZRK_COLLAPSE: String = "wzrk_ck"
    public const val WZRK_CHANNEL_ID: String = "wzrk_cid"
    public const val WZRK_BADGE_ICON: String = "wzrk_bi"
    public const val WZRK_BADGE_COUNT: String = "wzrk_bc"
    public const val WZRK_SUBTITLE: String = "wzrk_st"
    public const val WZRK_COLOR: String = "wzrk_clr"
    public const val WZRK_DISMISS: String = "wzrk_dismiss"

    // --- Live Updates ---
    // Mirrors the iOS Live Activity contract (CLTAP_LIVE_ACTIVITY_* / kCTLA*).
    // On Android there is no ActivityKit token concept — live updates are ordinary
    // FCM data pushes, rendered by the SDK when `data` has a pt_id (Mode B), else routed to an
    // ICleverTapLiveUpdateNotificationFactory (Mode A). These keys mark such
    // pushes and carry the backend-assigned identity used for in-place updates + attribution.
    /** Marker present on a Live Update push. Value is "true". */
    public const val WZRK_LIVE_UPDATE: String = "wzrk_la"
    /** Backend-assigned stable activity id. The SDK derives the in-place notification id from it, and
     *  because it is `wzrk_`-prefixed it also flows into the lifecycle event `evtData`
     *  automatically (serves both routing and attribution). */
    public const val WZRK_LIVE_UPDATE_ID: String = "wzrk_activityId"
    /** Single nested payload object carried by a Live Update push. Holds the render content for BOTH
     *  modes: if it contains a `pt_id` the SDK renders it (Mode B, e.g. `{"pt_id":"pt_progress", ...}`);
     *  otherwise it is the client factory's custom data (Mode A). Not `wzrk_`-prefixed on purpose
     *  so the render blob never leaks into analytics evtData. */
    public const val WZRK_LIVE_UPDATE_DATA: String = "data"
    /** Lifecycle event carried by the push, sent by BE, mapped 1:1 to the analytics state:
     *  [WZRK_LIVE_UPDATE_EVENT_START] → Started, [WZRK_LIVE_UPDATE_EVENT_UPDATE] →
     *  Updated, [WZRK_LIVE_UPDATE_EVENT_END] → Ended. Anything else defaults to Updated. */
    public const val WZRK_LIVE_UPDATE_EVENT: String = "wzrk_la_event"
    public const val WZRK_LIVE_UPDATE_EVENT_START: String = "start"
    public const val WZRK_LIVE_UPDATE_EVENT_UPDATE: String = "update"
    public const val WZRK_LIVE_UPDATE_EVENT_END: String = "end"
    /** Single event name raised for every Live Update lifecycle transition (mirrors iOS kCTLAEventName). */
    public const val LIVE_UPDATE_EVENT_NAME: String = "Live Activity"
    /** evtData field holding the lifecycle state. */
    public const val LIVE_UPDATE_STATE_KEY: String = "state"
    public const val LIVE_UPDATE_STATE_STARTED: String = "Started"
    public const val LIVE_UPDATE_STATE_UPDATED: String = "Updated"
    public const val LIVE_UPDATE_STATE_ENDED: String = "Ended"
    public const val LIVE_UPDATE_STATE_DISMISSED: String = "Dismissed"
    // Note: a missing Live Update channel falls back to the shared push fallback channel
    // (FCM_FALLBACK_NOTIFICATION_CHANNEL_ID via CTXtensions.getOrCreateChannel), not a bespoke one.

    public const val WZRK_STICKY: String = "wzrk_sticky"
    public const val WZRK_SILENCE_IN_FOREGROUND: String = "wzrk_sif"
    public const val WZRK_SOUND: String = "wzrk_sound"
    public const val WZRK_TIME_TO_LIVE: String = "wzrk_ttl"
    public const val WZRK_TIME_TO_LIVE_OFFSET: String = "wzrk_ttl_offset"
    public const val INAPP_SUPPRESSED: String = "suppressed"
    public const val INAPP_SS_EVAL_META: String = "inapps_eval"
    public const val INAPP_SUPPRESSED_META: String = "inapps_suppressed"
    public const val INAPP_WHEN_TRIGGERS: String = "whenTriggers"
    public const val INAPP_FC_LIMITS: String = "frequencyLimits"
    public const val INAPP_OCCURRENCE_LIMITS: String = "occurrenceLimits"

    public const val INAPP_PRIORITY: String = "priority"

    public const val CLTAP_PROP_CAMPAIGN_ID: String = "Campaign id"
    public const val CLTAP_PROP_VARIANT: String = "Variant"
    public const val CLTAP_APP_VERSION: String = "Version"
    public const val CLTAP_LATITUDE: String = "Latitude"
    public const val CLTAP_LONGITUDE: String = "Longitude"
    public const val CLTAP_OS_VERSION: String = "OS Version"
    public const val CLTAP_SDK_VERSION: String = "SDK Version"
    public const val CLTAP_CARRIER: String = "Carrier"
    public const val CLTAP_NETWORK_TYPE: String = "Radio"
    public const val CLTAP_CONNECTED_TO_WIFI: String = "wifi"
    public const val CLTAP_BLUETOOTH_VERSION: String = "BluetoothVersion"
    public const val CLTAP_BLUETOOTH_ENABLED: String = "BluetoothEnabled"

    public const val WZRK_RNV: String = "wzrk_rnv"
    public const val BLACK: String = "#000000"
    public const val WHITE: String = "#FFFFFF"
    public const val BLUE: String = "#0000FF"
    public const val GREEN: String = "#00FF00"
    public const val LIGHT_BLUE: String = "#818ce5"
    public const val FALLBACK_COLOR: String = "#FFFFFF"
    /**
     * Profile command constants.
     */
    public const val COMMAND_SET: String = "\$set"
    public const val COMMAND_ADD: String = "\$add"
    public const val COMMAND_REMOVE: String = "\$remove"
    public const val COMMAND_DELETE: String = "\$delete"
    public const val COMMAND_INCREMENT: String = "\$incr"
    public const val COMMAND_DECREMENT: String = "\$decr"
    public const val DATE_PREFIX: String = "\$D_"
    public const val DELETE_MARKER: String = "__CLEVERTAP_DELETE__"
    public const val GET_MARKER: String = "__CLEVERTAP_GET__"
    public const val GUID_PREFIX_GOOGLE_AD_ID: String = "__g"
    public const val CUSTOM_CLEVERTAP_ID_PREFIX: String = "__h"
    public const val ERROR_PROFILE_PREFIX: String = "__i"
    public const val KEY_ICON: String = "icon"
    public const val KEY_POSTER_URL: String = "poster"
    public const val KEY_ACTION: String = "action"
    public const val KEY_ANDROID: String = "android"
    public const val KEY_ORIENTATION: String = "orientation"
    public const val KEY_WZRK_PARAMS: String = "wzrkParams"
    public const val KEY_CONTENT: String = "content"
    public const val KEY_METADATA: String = "metadata"
    public const val KEY_CUSTOM_KV: String = "custom_kv"
    public const val KEY_BORDER: String = "border"
    public const val KEY_RADIUS: String = "radius"
    public const val KEY_ACTIONS: String = "actions"
    public const val KEY_ID: String = "id"
    public const val KEY_DATE: String = "date"
    public const val KEY_WZRK_TTL: String = "wzrk_ttl"
    public const val KEY_IS_READ: String = "isRead"
    public const val KEY_TAGS: String = "tags"
    public const val KEY_MSG: String = "msg"
    public const val KEY_HAS_URL: String = "hasUrl"
    public const val KEY_HAS_LINKS: String = "hasLinks"
    public const val KEY_LINKS: String = "links"
    public const val KEY_ENCRYPTION_MIGRATION: String = "encryptionmigration"
    public const val KEY_ENCRYPTION_CGK: String = "cgk"

    public const val KEY_ENCRYPTION_NAME: String = "Name"
    public const val KEY_ENCRYPTION_IDENTITY: String = "Identity"
    public const val KEY_ENCRYPTION_PHONE: String = "Phone"
    public const val KEY_ENCRYPTION_EMAIL: String = "Email"
    public const val TEST_IDENTIFIER: String = "0_0"
    public const val FEATURE_DISPLAY_UNIT: String = "DisplayUnit : "
    public const val FEATURE_FLAG_UNIT: String = "Feature Flag : "
    public const val LOG_TAG_PRODUCT_CONFIG: String = "Product Config : "

    public const val CRYPTION_SALT: String = "W1ZRCl3>"
    public const val CRYPTION_IV: String = "__CL3>3Rt#P__1V_"

    public const val AES_PREFIX: String = "["
    public const val AES_SUFFIX: String = "]"

    public const val AES_GCM_PREFIX: String = "<ct<"
    public const val AES_GCM_SUFFIX: String = ">ct>"

    public const val FETCH_TYPE_PC: Int = 0
    public const val FETCH_TYPE_FF: Int = 1
    public const val FETCH_TYPE_VARIABLES: Int = 4
    public const val FETCH_TYPE_IN_APPS: Int = 5
    public const val FETCH_TYPE_IN_ACTION_IN_APPS: Int = 6
    public const val FETCH_TYPE_INBOX_V2: Int = 7
    // Native Display SS metadata refresh — wzrk_fetch t value, agreed with BE.
    public const val FETCH_TYPE_ND_META: Int = 8
    public const val LOG_TAG_SIGNED_CALL: String = "SignedCall : "
    public const val LOG_TAG_GEOFENCES: String = "Geofences : "
    public const val LOG_TAG_INAPP: String = "InApp : "
    public const val CLEVERTAP_IDENTIFIER: String = "CLEVERTAP_IDENTIFIER"
    public const val SEPARATOR_COMMA: String = ","
    public const val AUTH: String = "auth"
    public const val SP_KEY_PROFILE_IDENTITIES: String = "SP_KEY_PROFILE_IDENTITIES"

    // valid profile identifier keys (hash sets: callers iterate them in the former HashSet order)
    public val LEGACY_IDENTITY_KEYS: Set<String> = hashSetOf(TYPE_IDENTITY, TYPE_EMAIL)
    public val ALL_IDENTITY_KEYS: Set<String> = hashSetOf(TYPE_IDENTITY, TYPE_EMAIL, TYPE_PHONE)
    public val piiDBKeys: Set<String> =
        hashSetOf(KEY_ENCRYPTION_NAME, KEY_ENCRYPTION_EMAIL, KEY_ENCRYPTION_IDENTITY, KEY_ENCRYPTION_PHONE)

    /**
     * Valid indexes for the App Inbox item and buttons.
     */
    public const val APP_INBOX_ITEM_CONTENT_PAGE_INDEX: Int = 0 //used for non-carousel templates as they have only one page of content to display
    public const val APP_INBOX_ITEM_INDEX: Int = -1
    public const val APP_INBOX_CTA1_INDEX: Int = 0
    public const val APP_INBOX_CTA2_INDEX: Int = 1
    public const val APP_INBOX_CTA3_INDEX: Int = 2

    public val NULL_STRING_ARRAY: Array<String> = arrayOf()
    public const val PT_NOTIF_ID: String = "notificationId"
    public const val CLOSE_SYSTEM_DIALOGS: String = "close_system_dialogs"
    public const val KEY_CT_TYPE: String = "ct_type"

    // ==========Fallback keys=========
    public const val WZRK_TSR_FB: String = "wzrk_tsr_fb"// terminate and stay resident
    public const val NOTIFICATION_RENDER_FALLBACK: String = "wzrk_fallback"
    public const val OMR_INVOKE_TIME_IN_MILLIS: String = "omr_invoke_time_in_millis"
    public const val WZRK_BPDS: String = "wzrk_bpds"
    public const val WZRK_BPDS_REASON: String = "wzrk_bpdsr"
    public const val WZRK_PN_PRT: String = "wzrk_pn_prt"
    public const val PRIORITY_NORMAL: String = "normal"
    public const val PRIORITY_UNKNOWN: String = "fcm_unknown"
    public const val D_SRC: String = "d_src"// data source for push impressions
    public const val D_SRC_PI_R: String = "PI_R"// push impression data source is Receiver
    public const val D_SRC_PI_WM: String = "PI_WM"// push impression data source is work manager
    public const val META_FRESH_APP_LAUNCH: String = "fl"

    public const val REGION_INDIA: String = "in1"
    public const val REGION_EUROPE: String = "eu1"

    // ============ notification image download timeout ===================

    public const val PN_IMAGE_CONNECTION_TIMEOUT_IN_MILLIS: Int = 1000
    public const val PN_IMAGE_READ_TIMEOUT_IN_MILLIS: Int = 5000
    public const val PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS: Long = 5000
    public const val PN_LARGE_ICON_DOWNLOAD_TIMEOUT_IN_MILLIS: Long = 2000

    //==============

    public const val FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME: String = "CTFlushPushImpressionsOneTime"

    public const val URL_PARAM_DL_SEPARATOR: String = "__dl__"
}
