package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import java.util.Arrays
import java.util.HashSet

// Bucket D: pushtemplates (and wrapper SDKs) read these as `Constants.X` from Java. Converted from a
// constants-only Java interface to an `object`: its `const val` / `@JvmField` members compile to
// static fields on Constants, and Kotlin callers keep `import com.clevertap.android.sdk.Constants.X`.
// (An interface companion cannot mix `const val` with `@JvmField`.)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Constants {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(TYPE_EMAIL, TYPE_PHONE, TYPE_IDENTITY)
    annotation class IdentityType

    const val TAG_FEATURE_IN_APPS: String = "TAG_FEATURE_IN_APPS"
    const val TYPE_IDENTITY: String = "Identity"
    const val TYPE_EMAIL: String = "Email"
    const val TYPE_PHONE: String = "Phone"

    const val FCM_FALLBACK_NOTIFICATION_CHANNEL_ID: String = "fcm_fallback_notification_channel"
    const val CT_FALLBACK_NOTIFICATION_CHANNEL_ID_LOW: String = "ct_fallback_notification_channel_low_importance"
    const val FCM_FALLBACK_NOTIFICATION_CHANNEL_NAME: String = "Misc"
    const val LOW_IMPORTANCE_FALLBACK_NOTIFICATION_CHANNEL_NAME: String = "Silent Misc"
    const val CLEVERTAP_OPTOUT: String = "ct_optout"
    const val CLEVERTAP_ALLOW_SYSTEM_EVENTS: String = "ct_allow_sys_events_and_communication"
    const val CLEVERTAP_STORAGE_TAG: String = "WizRocket"
    const val CLEVERTAP_LOG_TAG: String = "CleverTap"
    const val SESSION_LENGTH_MINS: Int = 20
    const val DEVICE_ID_TAG: String = "deviceId"
    const val FALLBACK_ID_TAG: String = "fallbackId"
    const val PAGE_EVENT: Int = 1
    const val PING_EVENT: Int = 2
    const val PROFILE_EVENT: Int = 3
    const val RAISED_EVENT: Int = 4
    const val DATA_EVENT: Int = 5
    const val NV_EVENT: Int = 6
    const val FETCH_EVENT: Int = 7
    const val DEFINE_VARS_EVENT: Int = 8
    const val variablePayloadType: String = "varsPayload"
    const val WZRK_FETCH: String = "wzrk_fetch"
    const val WZRK_MID: String = "wzrk_mid"
    const val NOTIFICATION_CLICKED_EVENT_NAME: String = "Notification Clicked"
    const val NOTIFICATION_VIEWED_EVENT_NAME: String = "Notification Viewed"
    const val SC_OUTGOING_EVENT_NAME: String = "SCOutgoing"
    const val SC_INCOMING_EVENT_NAME: String = "SCIncoming"
    const val SC_END_EVENT_NAME: String = "SCEnd"
    const val SC_CAMPAIGN_OPT_OUT_EVENT_NAME: String = "SCCampaignOptOut"
    const val GEOFENCE_ENTERED_EVENT_NAME: String = "Geocluster Entered"
    const val GEOFENCE_EXITED_EVENT_NAME: String = "Geocluster Exited"
    const val APP_LAUNCHED_EVENT: String = "App Launched"
    const val ERROR_KEY: String = "wzrk_error"
    const val WZRK_URL_SCHEMA: String = "wzrk://"
    const val INAPP_PREVIEW_PUSH_PAYLOAD_KEY: String = "wzrk_inapp"
    const val INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY: String = "wzrk_inapp_type"
    const val INAPP_PREVIEW_S3_URL_KEY: String = "wzrk_inapp_s3_url"
    const val INAPP_IMAGE_INTERSTITIAL_TYPE: String = "image-interstitial"
    const val INAPP_ADVANCED_BUILDER_TYPE: String = "advanced-builder"
    const val INAPP_IMAGE_INTERSTITIAL_CONFIG: String = "imageInterstitialConfig"
    const val INAPP_HTML_SPLIT: String = "\"##Vars##\""
    const val INAPP_IMAGE_INTERSTITIAL_HTML_NAME: String = "image_interstitial.html"
    const val INBOX_PREVIEW_PUSH_PAYLOAD_KEY: String = "wzrk_inbox"
    const val DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY: String = "wzrk_adunit"
    const val INAPP_HTML_TAG: String = "html"
    const val INAPP_DATA_TAG: String = "d"
    const val INAPP_X_PERCENT: String = "xp"
    const val INAPP_Y_PERCENT: String = "yp"
    const val INAPP_X_DP: String = "xdp"
    const val INAPP_Y_DP: String = "ydp"
    const val INAPP_POSITION: String = "pos"
    const val INAPP_ASPECT_RATIO: String = "aspectRatio"
    const val INAPP_POSITION_TOP: Char = 't'
    const val INAPP_POSITION_RIGHT: Char = 'r'
    const val INAPP_POSITION_BOTTOM: Char = 'b'
    const val INAPP_POSITION_LEFT: Char = 'l'
    const val INAPP_POSITION_CENTER: Char = 'c'
    const val INAPP_NOTIF_DARKEN_SCREEN: String = "dk"
    const val INAPP_NOTIF_SHOW_CLOSE: String = "sc"
    const val INAPP_JSON_RESPONSE_KEY: String = "inapp_notifs"
    const val INAPP_NOTIFS_META_KEY: String = "inapp_notifs_meta"

    const val INAPP_NOTIFS_STALE_KEY: String = "inapp_stale"
    const val INAPP_NOTIFS_APP_LAUNCHED_KEY: String = "inapp_notifs_applaunched"
    const val INAPP_NOTIFS_APP_LAUNCHED_META_KEY: String = "inapp_notifs_applaunched_meta"

    const val INAPP_NOTIFS_KEY_CS: String = "inapp_notifs_cs"
    const val INAPP_NOTIFS_KEY_SS: String = "inapp_notifs_ss"

    const val INAPP_DELIVERY_MODE_KEY: String = "inapp_delivery_mode"

    const val PREFS_INAPP_KEY_CS: String = INAPP_NOTIFS_KEY_CS
    const val PREFS_INAPP_KEY_SS: String = INAPP_NOTIFS_KEY_SS
    const val PREFS_EVALUATED_INAPP_KEY_SS: String = "evaluated_ss"
    const val PREFS_SUPPRESSED_INAPP_KEY_CS: String = "suppressed_ss"

    // ---- Native Display (ND) frequency caps (SDK-6055) ----
    // ND == Display Units channel. SS + legacy only (no CS mode). Mirrors the in-app keys above.
    // Response keys (LC -> SDK). Campaign content still arrives under DISPLAY_UNIT_JSON_RESPONSE_KEY ("adUnit_notifs").
    const val DISPLAY_UNIT_NOTIFS_SS_KEY: String = "adUnit_notifs_ss"                 // advanced-rule metadata bundle (rules only)
    const val DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY: String = "adUnit_notifs_applaunched" // app-launch content + CG-suppressed stubs
    const val DISPLAY_UNIT_NOTIFS_STALE_KEY: String = "adUnit_stale"                  // dead-target GC ids
    // Request meta keys (SDK -> LC)
    const val ND_SS_EVAL_META: String = "adUnit_eval"                                 // eligible advanced ND campaign votes
    const val ND_SUPPRESSED_META: String = "adUnit_suppressed"                        // CG-suppression acks (app-launch path only)
    const val ND_TARGET_SHOWN_LIST: String = "ndtlc"                                  // [[ti, todayCount, lifetimeCount], ...]
    // Wire keys with directional overload (mirror in-app imc/imp)
    const val ND_MAX_PER_SESSION_KEY: String = "ndmc"                                 // resp: session ceiling; also storage key
    const val ND_MAX_PER_DAY_KEY: String = "ndmp"                                     // req: SDK daily count; resp: daily ceiling
    // ND store prefs namespace + keys
    const val ND_KEY: String = "adUnit"                                               // ND metadata store prefs namespace
    const val PREFS_ND_KEY_SS: String = DISPLAY_UNIT_NOTIFS_SS_KEY                    // metadata bundle pref key
    const val PREFS_EVALUATED_ND_KEY_SS: String = "evaluated_nd_ss"                   // pending adUnit_eval report
    const val PREFS_SUPPRESSED_ND_KEY: String = "suppressed_nd"                       // pending adUnit_suppressed report

    const val INBOX_JSON_RESPONSE_KEY: String = "inbox_notifs"
    const val INBOX_V2_JSON_RESPONSE_KEY: String = "inbox_notifs_v2"
    const val INBOX_V2_ISREAD_KEY: String = "isRead"
    const val INBOX_V2_THROTTLE_WINDOW_MS: Long = 5L * 60L * 1000L
    const val DISPLAY_UNIT_JSON_RESPONSE_KEY: String = "adUnit_notifs"
    const val CONTENT_FETCH_JSON_RESPONSE_KEY: String = "content_fetch"
    // Fields on a single content_fetch directive item (self-describing; see ContentFetchItem).
    const val CONTENT_FETCH_ITEM_EVENT_NAME: String = "eventName"
    const val CONTENT_FETCH_ITEM_RESPONSE_KEY: String = "responseKey"
    const val CONTENT_FETCH_ITEM_TARGET_ID: String = "tgtId"
    const val FEATURE_FLAG_JSON_RESPONSE_KEY: String = "ff_notifs"
    const val REQUEST_VARIABLES_JSON_RESPONSE_KEY: String = "vars"
    const val REQUEST_VARIANTS_JSON_RESPONSE_KEY: String = "abVariantInfo"
    const val REMOTE_CONFIG_FLAG_JSON_RESPONSE_KEY: String = "pc_notifs"
    const val GEOFENCES_JSON_RESPONSE_KEY: String = "geofences"
    const val DISCARDED_EVENT_JSON_KEY: String = "d_e"
    const val INAPP_MAX_DISPLAY_COUNT: String = "mdc"
    const val INAPP_MAX_PER_SESSION_KEY: String = "imc"
    const val INAPP_MAX_PER_DAY_KEY: String = "imp"
    const val INAPP_WINDOW: String = "w"
    const val INAPP_KEY: String = "inApp"
    const val INAPP_ID_KEY: String = "inAppId"
    const val INAPP_WZRK_PIVOT: String = "wzrk_pivot"
    const val INAPP_WZRK_CGID: String = "wzrk_cgId"
    const val INAPP_CLOSE_IV_WIDTH: Int = 40
    const val INAPP_CLOSE_IV_TOUCH_TARGET_WIDTH: Int = 48
    const val INAPP_JS_ENABLED: String = "isJsEnabled"
    const val NOTIFICATION_ID_TAG: String = "wzrk_id"
    const val DEEP_LINK_KEY: String = "wzrk_dl"
    const val WZRK_PREFIX: String = "wzrk_"
    const val NOTIFICATION_ID_TAG_INTERVAL: Int = 5000
    const val NOTIFICATION_VIEWED_ID_TAG_INTERVAL: Int = 2000
    const val SESSION_ID_LAST: String = "lastSessionId"
    const val LAST_SESSION_EPOCH: String = "sexe"
    const val MAX_KEY_LENGTH: Int = 120
    const val MAX_VALUE_LENGTH: Int = 512
    const val WZRK_FROM_KEY: String = "wzrk_from"
    const val WZRK_ACCT_ID_KEY: String = "wzrk_acct_id"
    const val WZRK_FROM: String = "CTPushNotificationReceiver"
    const val NETWORK_INFO: String = "NetworkInfo"
    const val PRIMARY_DOMAIN: String = "clevertap-prod.com"
    const val CACHED_GUIDS_KEY: String = "cachedGUIDsKey"
    const val CACHED_GUIDS_LENGTH_KEY: String = "cachedGUIDsLengthKey"
    const val CACHED_VARIABLES_KEY: String = "variablesKey"
    const val CACHED_VARIANTS_KEY: String = "variantsForAbKey"
    const val MULTI_USER_PREFIX: String = "mt_"
    const val NOTIFICATION_TAG: String = "wzrk_pn"
    const val CHARGED_EVENT: String = "Charged"
    const val PROFILE: String = "profile"
    const val RAISED: String = "raised"
    const val USER_ATTRIBUTE_CHANGE: String = "_CTUserAttributeChange"
    const val KEY_NEW_VALUE: String = "newValue"
    const val KEY_OLD_VALUE: String = "oldValue"
    const val KEY_ITEMS: String = "Items"
    const val KEY_MUTED: String = "comms_mtd"
    const val KEY_MUTE_EXPIRY: String = "comms_mute_expiry_ts"
    const val DEFAULT_MUTE_DURATION_MS: Long = 24 * 60 * 60 * 1000L
    const val EMPTY_NOTIFICATION_ID: Int = -1000
    const val KEY_MAX_PER_DAY: String = "istmcd_inapp"
    const val KEY_COUNTS_SHOWN_TODAY: String = "istc_inapp"
    const val KEY_COUNTS_PER_INAPP: String = "counts_per_inapp"

    const val KEY_TRIGGERS_PER_INAPP: String = "triggers_per_inapp"

    // ---- Native Display (ND) frequency-cap storage keys (SDK-6055), mirror the in-app keys above ----
    const val KEY_ND_MAX_PER_DAY: String = "ndstmcd"                 // stored ND daily ceiling (mirror of istmcd_inapp)
    const val KEY_ND_COUNTS_SHOWN_TODAY: String = "ndstc"            // stored ND global shown-today (mirror of istc_inapp)
    const val KEY_ND_LAST_RESET_DATE: String = "nd_ict_date"         // ddMMyyyy of the last daily reset (mirror of ict_date)
    const val KEY_ND_COUNTS_PER_TARGET: String = "nd_counts_per_target"   // NdCountsStore namespace (per-target counts + globals)
    const val KEY_ND_IMPRESSIONS_PER_TARGET: String = "nd_impressions"    // ND ImpressionStore namespace (whenLimits timestamps)
    const val KEY_ND_TRIGGERS_PER_TARGET: String = "nd_triggers_per_target" // onEvery/onExactly trigger counts
    const val INAPP_ID_IN_PAYLOAD: String = "ti"
    const val LOCATION_PING_INTERVAL_IN_SECONDS: Int = 10
    @JvmField
    val SYSTEM_EVENTS: Array<String> = arrayOf(NOTIFICATION_CLICKED_EVENT_NAME, NOTIFICATION_VIEWED_EVENT_NAME, GEOFENCE_ENTERED_EVENT_NAME, GEOFENCE_EXITED_EVENT_NAME)
    const val DEFAULT_PUSH_TTL_SECONDS: Long = 60 * 60 * 24 * 4// 4 days
    const val ONE_MIN_IN_MILLIS: Long = 60 * 1000L
    const val ONE_DAY_IN_MILLIS: Long = 24 * 60 * 60 * 1000L
    const val COPY_TYPE: String = "copy"
    const val DND_START: String = "22:00"
    const val DND_STOP: String = "06:00"
    const val VIDEO_THUMBNAIL: String = "ct_video_1"
    const val AUDIO_THUMBNAIL: String = "ct_audio"
    const val IMAGE_PLACEHOLDER: String = "ct_image"
    const val KEY_CONFIG: String = "config"
    const val KEY_C2A: String = "wzrk_c2a"

    // Split-of-clicks: per-element identity + action descriptors (must match iOS wire contract)
    const val KEY_WZRK_ELEMENT_ID: String = "wzrk_element_id"
    const val KEY_WZRK_ACTION: String = "wzrk_action"
    const val KEY_WZRK_DATA: String = "wzrk_data"

    // key for parsing
    const val KEY_SWIPE_TO_DISMISS: String = "swipe-dismiss"

    // wzrk_element_id values
    const val INAPP_ELEMENT_ID_IMAGE: String = "image-1"
    const val INAPP_ELEMENT_ID_CLOSE: String = "closeButton"
    const val INAPP_ELEMENT_ID_BUTTON_PREFIX: String = "button-"
    // PIP exposes a single CTA button; its element id is a fixed literal
    const val INAPP_ELEMENT_ID_PIP_CTA: String = "button-cta"

    // wzrk_c2a values for dismiss gestures
    const val INAPP_CTA_DISMISS_BUTTON: String = "Dismiss Button"
    const val INAPP_CTA_SWIPE_DISMISS: String = "Swipe to Dismiss"

    // Programmatic PIP dismiss via the public dismissPipInApp() API.
    const val INAPP_ELEMENT_ID_DISMISS_API: String = "dismissApi"
    const val INAPP_CTA_DISMISS_PIP_API: String = "Dismiss PiP API"

    // wzrk_data literal value for a close action
    const val INAPP_WZRK_DATA_CLOSE: String = "close"

    // Media-error synthetic-close descriptors emitted by the bundled advanced-builder HTML template
    const val INAPP_CTA_IMAGE_ERROR_DISMISS: String = "image-error-dismiss"
    const val INAPP_CTA_VIDEO_ERROR_DISMISS: String = "video-error-dismiss"

    // wzrk_error codes/messages for media preload failures.
    const val INAPP_IMAGE_LOAD_FAILED_ERROR_CODE: Int = 591
    const val INAPP_VIDEO_LOAD_FAILED_ERROR_CODE: Int = 592
    const val INAPP_IMAGE_LOAD_FAILED_ERROR_MSG: String = "InApp image failed to load"
    const val INAPP_VIDEO_LOAD_FAILED_ERROR_MSG: String = "InApp video failed to load"

    // wzrk_error codes/messages for PIP media failures (all URLs failed, PIP never showed).
    // One code per PIP media type, keyed by the campaign's primary media type.
    const val INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_CODE: Int = 593
    const val INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_CODE: Int = 594
    const val INAPP_PIP_GIF_LOAD_FAILED_ERROR_CODE: Int = 595
    const val INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_MSG: String = "InApp PIP image failed to load"
    const val INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_MSG: String = "InApp PIP video failed to load"
    const val INAPP_PIP_GIF_LOAD_FAILED_ERROR_MSG: String = "InApp PIP GIF failed to load"

    // wzrk_error codes/messages for the server-side in-app queue dropped on OutOfMemoryError.
    const val INAPP_QUEUE_SAVE_OOM_ERROR_CODE: Int = 596
    const val INAPP_QUEUE_READ_OOM_ERROR_CODE: Int = 597
    const val INAPP_QUEUE_SAVE_OOM_ERROR_MSG: String = "InApp queue dropped on save (OOM)"
    const val INAPP_QUEUE_READ_OOM_ERROR_MSG: String = "InApp queue dropped on read (OOM)"
    // Android could not load the in-app prefs file (OutOfMemoryError); the queue is kept as is.
    const val INAPP_QUEUE_PREFS_LOAD_OOM_ERROR_CODE: Int = 598
    const val INAPP_QUEUE_PREFS_LOAD_OOM_ERROR_MSG: String = "InApp queue unavailable, prefs load failed (OOM)"

    const val KEY_EFC: String = "efc"
    const val KEY_EXCLUDE_GLOBAL_CAPS: String = "excludeGlobalFCaps"
    // ND opt-in flag (ONC-418). Absent/false => campaign is outside the ND fcap regime; true => honor the
    // fcap fields present. Gates whether a shown ND unit counts toward the global ndmp/ndtlc budget.
    const val KEY_IS_ND_FCAP_ENABLED: String = "isNdFcapEnabled"
    const val KEY_TLC: String = "tlc"
    const val KEY_TDC: String = "tdc"
    const val KEY_KV: String = "kv"
    const val KEY_TYPE: String = "type"
    const val KEY_LIMIT: String = "limit"
    const val KEY_FREQUENCY: String = "frequency"
    const val KEY_T: String = "t"
    const val KEY_EVT_NAME: String = "evtName"
    const val KEY_EVT_DATA: String = "evtData"
    const val KEY_FALLBACK_NOTIFICATION_SETTINGS: String = "fbSettings"
    const val KEY_REQUEST_FOR_NOTIFICATION_PERMISSION: String = "rfp"
    const val NOTIFICATION_PERMISSION_REQUEST_CODE: Int = 102
    const val KEY_IS_TABLET: String = "tablet"
    const val KEY_BG: String = "bg"
    const val KEY_TITLE: String = "title"
    const val KEY_TEXT: String = "text"
    const val KEY_KEY: String = "key"
    const val KEY_VALUE: String = "value"
    const val KEY_COLOR: String = "color"
    const val KEY_MESSAGE: String = "message"
    const val KEY_HIDE_CLOSE: String = "close"
    const val KEY_MEDIA: String = "media"
    const val KEY_MEDIA_LANDSCAPE: String = "mediaLandscape"
    const val KEY_PORTRAIT: String = "hasPortrait"
    const val KEY_LANDSCAPE: String = "hasLandscape"
    const val KEY_CONTENT_TYPE: String = "content_type"
    const val KEY_URL: String = "url"
    const val KEY_ALT_TEXT: String = "alt_text"
    const val KEY_BUTTONS: String = "buttons"
    const val KEY_CUSTOM_HTML: String = "custom-html"
    const val WZRK_PUSH_ID: String = "wzrk_pid"
    const val WZRK_DEDUPE: String = "wzrk_dd"
    const val WZRK_PUSH_SILENT: String = "wzrk_pn_s"
    const val NOTIF_MSG: String = "nm"
    const val NOTIF_TITLE: String = "nt"
    const val NOTIF_ICON: String = "ico"
    const val NOTIF_HIDE_APP_LARGE_ICON: String = "wzrk_hide_large_icon"
    const val WZRK_ACTIONS: String = "wzrk_acts"
    const val WZRK_BIG_PICTURE: String = "wzrk_bp"
    const val WZRK_BIG_PICTURE_ALT_TEXT_KEY: String = "alt_text_wzrk_bp"
    const val WZRK_GIF: String = "wzrk_gif"
    const val PUSH_DIRECTORY_NAME: String = "CleverTap.Push"
    const val WZRK_MSG_SUMMARY: String = "wzrk_nms"
    const val NOTIF_PRIORITY: String = "pr"
    const val PRIORITY_HIGH: String = "high"
    const val PRIORITY_MAX: String = "max"
    const val WZRK_COLLAPSE: String = "wzrk_ck"
    const val WZRK_CHANNEL_ID: String = "wzrk_cid"
    const val WZRK_BADGE_ICON: String = "wzrk_bi"
    const val WZRK_BADGE_COUNT: String = "wzrk_bc"
    const val WZRK_SUBTITLE: String = "wzrk_st"
    const val WZRK_COLOR: String = "wzrk_clr"
    const val WZRK_DISMISS: String = "wzrk_dismiss"

    // --- Live Updates ---
    // Mirrors the iOS Live Activity contract (CLTAP_LIVE_ACTIVITY_* / kCTLA*).
    // On Android there is no ActivityKit token concept — live updates are ordinary
    // FCM data pushes, rendered by the SDK when `data` has a pt_id (Mode B), else routed to an
    // ICleverTapLiveUpdateNotificationFactory (Mode A). These keys mark such
    // pushes and carry the backend-assigned identity used for in-place updates + attribution.
    /** Marker present on a Live Update push. Value is "true". */
    const val WZRK_LIVE_UPDATE: String = "wzrk_la"
    /** Backend-assigned stable activity id. The SDK derives the in-place notification id from it, and
     *  because it is `wzrk_`-prefixed it also flows into the lifecycle event `evtData`
     *  automatically (serves both routing and attribution). */
    const val WZRK_LIVE_UPDATE_ID: String = "wzrk_activityId"
    /** Single nested payload object carried by a Live Update push. Holds the render content for BOTH
     *  modes: if it contains a `pt_id` the SDK renders it (Mode B, e.g. `{"pt_id":"pt_progress", ...}`);
     *  otherwise it is the client factory's custom data (Mode A). Not `wzrk_`-prefixed on purpose
     *  so the render blob never leaks into analytics evtData. */
    const val WZRK_LIVE_UPDATE_DATA: String = "data"
    /** Lifecycle event carried by the push, sent by BE, mapped 1:1 to the analytics state:
     *  [WZRK_LIVE_UPDATE_EVENT_START] → Started, [WZRK_LIVE_UPDATE_EVENT_UPDATE] →
     *  Updated, [WZRK_LIVE_UPDATE_EVENT_END] → Ended. Anything else defaults to Updated. */
    const val WZRK_LIVE_UPDATE_EVENT: String = "wzrk_la_event"
    const val WZRK_LIVE_UPDATE_EVENT_START: String = "start"
    const val WZRK_LIVE_UPDATE_EVENT_UPDATE: String = "update"
    const val WZRK_LIVE_UPDATE_EVENT_END: String = "end"
    /** Single event name raised for every Live Update lifecycle transition (mirrors iOS kCTLAEventName). */
    const val LIVE_UPDATE_EVENT_NAME: String = "Live Activity"
    /** evtData field holding the lifecycle state. */
    const val LIVE_UPDATE_STATE_KEY: String = "state"
    const val LIVE_UPDATE_STATE_STARTED: String = "Started"
    const val LIVE_UPDATE_STATE_UPDATED: String = "Updated"
    const val LIVE_UPDATE_STATE_ENDED: String = "Ended"
    const val LIVE_UPDATE_STATE_DISMISSED: String = "Dismissed"
    // Note: a missing Live Update channel falls back to the shared push fallback channel
    // (FCM_FALLBACK_NOTIFICATION_CHANNEL_ID via CTXtensions.getOrCreateChannel), not a bespoke one.

    const val WZRK_STICKY: String = "wzrk_sticky"
    const val WZRK_SILENCE_IN_FOREGROUND: String = "wzrk_sif"
    const val WZRK_SOUND: String = "wzrk_sound"
    const val WZRK_TIME_TO_LIVE: String = "wzrk_ttl"
    const val WZRK_TIME_TO_LIVE_OFFSET: String = "wzrk_ttl_offset"
    const val INAPP_SUPPRESSED: String = "suppressed"
    const val INAPP_SS_EVAL_META: String = "inapps_eval"
    const val INAPP_SUPPRESSED_META: String = "inapps_suppressed"
    const val INAPP_WHEN_TRIGGERS: String = "whenTriggers"
    const val INAPP_FC_LIMITS: String = "frequencyLimits"
    const val INAPP_OCCURRENCE_LIMITS: String = "occurrenceLimits"

    const val INAPP_PRIORITY: String = "priority"

    const val CLTAP_PROP_CAMPAIGN_ID: String = "Campaign id"
    const val CLTAP_PROP_VARIANT: String = "Variant"
    const val CLTAP_APP_VERSION: String = "Version"
    const val CLTAP_LATITUDE: String = "Latitude"
    const val CLTAP_LONGITUDE: String = "Longitude"
    const val CLTAP_OS_VERSION: String = "OS Version"
    const val CLTAP_SDK_VERSION: String = "SDK Version"
    const val CLTAP_CARRIER: String = "Carrier"
    const val CLTAP_NETWORK_TYPE: String = "Radio"
    const val CLTAP_CONNECTED_TO_WIFI: String = "wifi"
    const val CLTAP_BLUETOOTH_VERSION: String = "BluetoothVersion"
    const val CLTAP_BLUETOOTH_ENABLED: String = "BluetoothEnabled"

    const val WZRK_RNV: String = "wzrk_rnv"
    const val BLACK: String = "#000000"
    const val WHITE: String = "#FFFFFF"
    const val BLUE: String = "#0000FF"
    const val GREEN: String = "#00FF00"
    const val LIGHT_BLUE: String = "#818ce5"
    const val FALLBACK_COLOR: String = "#FFFFFF"
    /**
     * Profile command constants.
     */
    const val COMMAND_SET: String = "\$set"
    const val COMMAND_ADD: String = "\$add"
    const val COMMAND_REMOVE: String = "\$remove"
    const val COMMAND_DELETE: String = "\$delete"
    const val COMMAND_INCREMENT: String = "\$incr"
    const val COMMAND_DECREMENT: String = "\$decr"
    const val DATE_PREFIX: String = "\$D_"
    const val DELETE_MARKER: String = "__CLEVERTAP_DELETE__"
    const val GET_MARKER: String = "__CLEVERTAP_GET__"
    const val GUID_PREFIX_GOOGLE_AD_ID: String = "__g"
    const val CUSTOM_CLEVERTAP_ID_PREFIX: String = "__h"
    const val ERROR_PROFILE_PREFIX: String = "__i"
    const val KEY_ICON: String = "icon"
    const val KEY_POSTER_URL: String = "poster"
    const val KEY_ACTION: String = "action"
    const val KEY_ANDROID: String = "android"
    const val KEY_ORIENTATION: String = "orientation"
    const val KEY_WZRK_PARAMS: String = "wzrkParams"
    const val KEY_CONTENT: String = "content"
    const val KEY_METADATA: String = "metadata"
    const val KEY_CUSTOM_KV: String = "custom_kv"
    const val KEY_BORDER: String = "border"
    const val KEY_RADIUS: String = "radius"
    const val KEY_ACTIONS: String = "actions"
    const val KEY_ID: String = "id"
    const val KEY_DATE: String = "date"
    const val KEY_WZRK_TTL: String = "wzrk_ttl"
    const val KEY_IS_READ: String = "isRead"
    const val KEY_TAGS: String = "tags"
    const val KEY_MSG: String = "msg"
    const val KEY_HAS_URL: String = "hasUrl"
    const val KEY_HAS_LINKS: String = "hasLinks"
    const val KEY_LINKS: String = "links"
    const val KEY_ENCRYPTION_MIGRATION: String = "encryptionmigration"
    const val KEY_ENCRYPTION_CGK: String = "cgk"

    const val KEY_ENCRYPTION_NAME: String = "Name"
    const val KEY_ENCRYPTION_IDENTITY: String = "Identity"
    const val KEY_ENCRYPTION_PHONE: String = "Phone"
    const val KEY_ENCRYPTION_EMAIL: String = "Email"
    const val TEST_IDENTIFIER: String = "0_0"
    const val FEATURE_DISPLAY_UNIT: String = "DisplayUnit : "
    const val FEATURE_FLAG_UNIT: String = "Feature Flag : "
    const val LOG_TAG_PRODUCT_CONFIG: String = "Product Config : "

    const val CRYPTION_SALT: String = "W1ZRCl3>"
    const val CRYPTION_IV: String = "__CL3>3Rt#P__1V_"

    const val AES_PREFIX: String = "["
    const val AES_SUFFIX: String = "]"

    const val AES_GCM_PREFIX: String = "<ct<"
    const val AES_GCM_SUFFIX: String = ">ct>"

    const val FETCH_TYPE_PC: Int = 0
    const val FETCH_TYPE_FF: Int = 1
    const val FETCH_TYPE_VARIABLES: Int = 4
    const val FETCH_TYPE_IN_APPS: Int = 5
    const val FETCH_TYPE_IN_ACTION_IN_APPS: Int = 6
    const val FETCH_TYPE_INBOX_V2: Int = 7
    // Native Display SS metadata refresh — wzrk_fetch t value, agreed with BE.
    const val FETCH_TYPE_ND_META: Int = 8
    const val LOG_TAG_SIGNED_CALL: String = "SignedCall : "
    const val LOG_TAG_GEOFENCES: String = "Geofences : "
    const val LOG_TAG_INAPP: String = "InApp : "
    const val CLEVERTAP_IDENTIFIER: String = "CLEVERTAP_IDENTIFIER"
    const val SEPARATOR_COMMA: String = ","
    const val AUTH: String = "auth"
    const val SP_KEY_PROFILE_IDENTITIES: String = "SP_KEY_PROFILE_IDENTITIES"

    // valid profile identifier keys
    @JvmField
    val LEGACY_IDENTITY_KEYS: HashSet<String> = HashSet(Arrays.asList(TYPE_IDENTITY, TYPE_EMAIL))
    @JvmField
    val ALL_IDENTITY_KEYS: HashSet<String> = HashSet(Arrays.asList(TYPE_IDENTITY, TYPE_EMAIL, TYPE_PHONE))
    @JvmField
    val piiDBKeys: HashSet<String> = HashSet(Arrays.asList(KEY_ENCRYPTION_NAME, KEY_ENCRYPTION_EMAIL, KEY_ENCRYPTION_IDENTITY, KEY_ENCRYPTION_PHONE))

    /**
     * Valid indexes for the App Inbox item and buttons.
     */
    const val APP_INBOX_ITEM_CONTENT_PAGE_INDEX: Int = 0 //used for non-carousel templates as they have only one page of content to display
    const val APP_INBOX_ITEM_INDEX: Int = -1
    const val APP_INBOX_CTA1_INDEX: Int = 0
    const val APP_INBOX_CTA2_INDEX: Int = 1
    const val APP_INBOX_CTA3_INDEX: Int = 2

    @JvmField
    val NULL_STRING_ARRAY: Array<String> = arrayOf()
    const val PT_NOTIF_ID: String = "notificationId"
    const val CLOSE_SYSTEM_DIALOGS: String = "close_system_dialogs"
    const val KEY_CT_TYPE: String = "ct_type"

    // ==========Fallback keys=========
    const val WZRK_TSR_FB: String = "wzrk_tsr_fb"// terminate and stay resident
    const val NOTIFICATION_RENDER_FALLBACK: String = "wzrk_fallback"
    const val OMR_INVOKE_TIME_IN_MILLIS: String = "omr_invoke_time_in_millis"
    const val WZRK_BPDS: String = "wzrk_bpds"
    const val WZRK_BPDS_REASON: String = "wzrk_bpdsr"
    const val WZRK_PN_PRT: String = "wzrk_pn_prt"
    const val PRIORITY_NORMAL: String = "normal"
    const val PRIORITY_UNKNOWN: String = "fcm_unknown"
    const val D_SRC: String = "d_src"// data source for push impressions
    const val D_SRC_PI_R: String = "PI_R"// push impression data source is Receiver
    const val D_SRC_PI_WM: String = "PI_WM"// push impression data source is work manager
    const val META_FRESH_APP_LAUNCH: String = "fl"

    const val REGION_INDIA: String = "in1"
    const val REGION_EUROPE: String = "eu1"

    // ============ notification image download timeout ===================

    const val PN_IMAGE_CONNECTION_TIMEOUT_IN_MILLIS: Int = 1000
    const val PN_IMAGE_READ_TIMEOUT_IN_MILLIS: Int = 5000
    const val PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS: Long = 5000
    const val PN_LARGE_ICON_DOWNLOAD_TIMEOUT_IN_MILLIS: Long = 2000

    //==============

    const val FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME: String = "CTFlushPushImpressionsOneTime"

    const val URL_PARAM_DL_SEPARATOR: String = "__dl__"
}
