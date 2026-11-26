package com.clevertap.android.pushtemplates

//Using common keys from core-sdk constants
public open class PTConstants {

    public companion object {

        public const val LOG_TAG: String = "PTLog"

        public const val PT_MANUAL_CAROUSEL_CURRENT: String = "pt_manual_carousel_current"

        public const val PT_IMAGE_LIST: String = "pt_image_list"

        public const val PT_COLLAPSE_KEY: String = "pt_ck"

        public const val PT_DEEPLINK_LIST: String = "pt_deeplink_list"

        public const val PT_FLIP_INTERVAL: String = "pt_flip_interval"

        public const val PT_DIR: String = "pt_dir"

        public const val PT_SOUND_FILE_NAME: String = "pt_silent_sound"

        public const val PT_RATING_C2A_KEY: String = "rating_"

        public const val PT_5CTA_C2A_KEY: String = "5cta_"

        public const val PT_VT_C2A_KEY: String = "vt_btn"

        public const val PT_VT_C2A_COLLAPSED_KEY: String = "vt_btn_collapsed"

        public const val PT_PRICE_LIST: String = "pt_price_list"

        public const val PT_SMALLTEXT_LIST: String = "pt_small_text_list"

        public const val PT_BIGTEXT_LIST: String = "pt_big_text_list"

        public const val PT_MANUAL_CAROUSEL_TYPE: String = "pt_manual_carousel_type"

        public const val PT_MANUAL_CAROUSEL_FILMSTRIP: String = "filmstrip"

        public const val ONE_SECOND: Int = 1000

        public const val ONE_SECOND_LONG: Long = 1000L

        public const val DEFAULT_DL: String = "default_dl"

        public const val PT_ID: String = "pt_id"

        // --- Progress-centric template (pt_progress) — Android 16 Notification.ProgressStyle ---
        public const val PT_PROGRESS: String = "pt_progress"
        public const val PT_PROGRESS_MAX: String = "pt_progress_max"
        public const val PT_PROGRESS_INDETERMINATE: String = "pt_progress_indeterminate"
        public const val PT_STYLED_BY_PROGRESS: String = "pt_styled_by_progress"
        public const val PT_PROGRESS_SEGMENTS: String = "pt_progress_segments" // JSON array
        public const val PT_PROGRESS_POINTS: String = "pt_progress_points"     // JSON array
        public const val PT_PROGRESS_TRACKER_ICON: String = "pt_progress_tracker_icon"
        public const val PT_PROGRESS_START_ICON: String = "pt_progress_start_icon"
        public const val PT_PROGRESS_END_ICON: String = "pt_progress_end_icon"
        public const val PT_CHIP_TYPE: String = "pt_chip_type" // text | timer | countdown | none
        public const val PT_CHIP_TEXT: String = "pt_chip_text"
        public const val PT_WHEN: String = "pt_when"
        public const val PT_COUNTDOWN: String = "pt_countdown"
        public const val PT_PROMOTE: String = "pt_promote"
        public const val PT_LA_EVENT: String = "wzrk_la_event" // update | end (from LA wrapper)

        public const val PT_NOTIF_ICON: String = "pt_ico"

        public const val PT_TITLE: String = "pt_title"

        public const val PT_MSG: String = "pt_msg"

        public const val PT_MSG_SUMMARY: String = "pt_msg_summary"

        public const val PT_TITLE_COLOR: String = "pt_title_clr"

        public const val PT_DARK_MODE_SUFFIX: String = "_dark"

        public const val PT_MSG_COLOR: String = "pt_msg_clr"

        public const val PT_BG: String = "pt_bg"

        public const val ALT_TEXT_SUFFIX: String = "_alt_text"

        public const val PT_BIG_IMG: String = "pt_big_img"

        public const val PT_GIF: String = "pt_gif"

        // GIF key for terminal notification of timer template
        public const val PT_GIF_ALT: String = "pt_gif_alt"

        public const val PT_GIF_FRAMES: String = "pt_gif_frames"

        public const val PT_GIF_FRAMES_ALT: String = "pt_gif_frames_alt"

        public const val PT_SCALE_TYPE_ALT: String = "pt_scale_type_alt"

        public const val PT_BIG_IMG_ALT_TEXT: String = "pt_big_img_alt_text"

        public const val PT_STICKY: String = "pt_sticky"

        public const val PT_DISMISS: String = "pt_dismiss"

        // Keys for collapsed media in zero bezel
        public const val PT_GIF_COLLAPSED: String = "pt_gif_collapsed"
        public const val PT_GIF_FRAMES_COLLAPSED: String = "pt_gif_frames_collapsed"
        public const val PT_SCALE_TYPE_COLLAPSED: String = "pt_scale_type_collapsed"
        public const val PT_BIG_IMG_COLLAPSED: String = "pt_big_img_collapsed"
        public const val PT_BIG_IMG_COLLAPSED_ALT_TEXT: String = "pt_big_img_collapsed_alt_text"

        public const val PT_JSON: String = "pt_json"

        public const val PT_BUY_NOW_DL: String = "pt_buy_now_dl"

        public const val PT_DEFAULT_DL: String = "pt_default_dl"

        public const val PT_SMALL_VIEW: String = "pt_small_view"

        public const val PT_TIMER_THRESHOLD: String = "pt_timer_threshold"

        public const val PT_RENDER_TERMINAL: String = "pt_render_terminal"

        public const val PT_INPUT_LABEL: String = "pt_input_label"

        public const val PT_INPUT_KEY: String = "pt_input_reply"

        public const val PT_INPUT_FEEDBACK: String = "pt_input_feedback"

        public const val PT_INPUT_TIMEOUT: Int = 1300

        public const val PT_NOTIF_ID: String = "notificationId"

        public const val PT_INPUT_AUTO_OPEN: String = "pt_input_auto_open"

        public const val PT_EVENT_NAME_KEY: String = "pt_event_name"

        public const val PT_EVENT_PROPERTY_KEY: String = "pt_event_property"

        public const val PT_EVENT_PROPERTY_SEPERATOR: String = "pt_event_property_"

        public const val PT_DISMISS_ON_CLICK: String = "pt_dismiss_on_click"

        public const val PT_CHRONO_TITLE_COLOUR: String = "pt_chrono_title_clr"

        public const val PT_CHRONO_BG_CLR: String = "pt_chrono_bg_clr"
        public const val PT_CHRONO_BORDER_CLR: String = "pt_chrono_border_clr"
        public const val PT_CHRONO_STYLE: String = "pt_chrono_style"
        public const val PT_CHRONO_GRAD_CLR1: String = "pt_chrono_grad_clr1"
        public const val PT_CHRONO_GRAD_CLR2: String = "pt_chrono_grad_clr2"
        public const val PT_CHRONO_GRAD_DIR: String = "pt_chrono_grad_dir"
        public const val PT_CHRONO_BORDER_RADIUS: String = "pt_chrono_border_radius"
        public const val PT_CHRONO_BORDER_WIDTH: String = "pt_chrono_border_width"


        public const val PT_PRODUCT_DISPLAY_ACTION: String = "pt_product_display_action"

        public const val PT_PRODUCT_DISPLAY_ACTION_COLOUR: String = "pt_product_display_action_clr"

        public const val PT_TIMER_END: String = "pt_timer_end"

        public const val PT_TIMER_SPLIT: String = "\\\$D_"

        public const val PT_TIMER_MIN_THRESHOLD: Int = 10

        public const val PT_BIG_IMG_ALT: String = "pt_big_img_alt"
        public const val PT_BIG_IMG_ALT_ALT_TEXT: String = "pt_big_img_alt_alt_text"

        public const val PT_TITLE_ALT: String = "pt_title_alt"

        public const val PT_MSG_ALT: String = "pt_msg_alt"

        public const val PT_MSG_SUMMARY_ALT: String = "pt_msg_summary_alt"

        public const val PT_PRODUCT_DISPLAY_LINEAR: String = "pt_product_display_linear"

        public const val PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR: String = "pt_product_display_action_text_clr"

        public const val PT_SMALL_ICON_COLOUR: String = "pt_small_icon_clr"

        public const val PT_META_CLR: String = "pt_meta_clr"

        public const val PT_CANCEL_NOTIF_ID: String = "pt_cancel_notif_id"

        public const val PT_ACTION_ID: String = "actionId"

        // Same key core action buttons use, read by the app's dismiss handler on icon taps
        public const val PT_AUTO_CANCEL: String = "autoCancel"

        public const val PT_RIGHT_SWIPE: String = "right_swipe"

        public const val PT_MANUAL_CAROUSEL_FROM: String = "manual_carousel_from"

        public const val PT_IMAGE_1: String = "img1"

        public const val PT_CURRENT_POSITION: String = "pt_current_position"

        public const val PT_BUY_NOW: String = "buynow"

        public const val PT_SCALE_TYPE: String = "pt_scale_type"

        public const val TEXT_ONLY: String = "text_only"

        public const val PT_SUBTITLE: String = "pt_subtitle"

        public const val PT_DISMISS_INTENT: String = "pt_dismiss_intent"

        public const val PT_SILENT_CHANNEL_ID: String = "pt_silent_sound_channel"

        @JvmField
        public val PT_SILENT_CHANNEL_NAME: CharSequence = "Silent Channel"

        public const val PT_SILENT_CHANNEL_DESC: String = "A channel to silently update notifications"

        public const val PT_RATING_TOAST: String = "pt_rating_toast"

        public const val PT_COLOUR_GREY: String = "#A6A6A6"

        public const val PT_META_CLR_DEFAULTS: String = PT_COLOUR_GREY

        public const val PT_FLIP_INTERVAL_TIME: Int = 4 * ONE_SECOND

        public const val KEY_CLICKED_STAR: String = "clickedStar"

        public const val KEY_REQUEST_CODES: String = "requestCodes"

        // Vertical Image Template Keys
        public const val PT_TEXT1: String = "pt_text1"
        public const val PT_TEXT2: String = "pt_text2"
        public const val PT_TEXT1_COLOR: String = "pt_text1_clr"
        public const val PT_TEXT2_COLOR: String = "pt_text2_clr"

        // Vertical Image Template - Button configuration
        public const val PT_BTN_NAME: String = "pt_btn_name"
        public const val PT_BTN_DL: String = "pt_btn_dl"
        public const val PT_BTN_STYLE: String = "pt_btn_style"
        public const val PT_BTN_CLR: String = "pt_btn_clr"
        public const val PT_BTN_BORDER_CLR: String = "pt_btn_border_clr"
        public const val PT_BTN_TEXT_CLR: String = "pt_btn_text_clr"
        public const val PT_BTN_GRAD_CLR1: String = "pt_btn_grad_clr1"
        public const val PT_BTN_GRAD_CLR2: String = "pt_btn_grad_clr2"
        public const val PT_BTN_GRAD_DIR: String = "pt_btn_grad_dir"
        public const val PT_BTN_BORDER_RADIUS: String = "pt_btn_border_radius"
        public const val PT_BTN_BORDER_WIDTH: String = "pt_btn_border_width"

        public const val PT_BTN_GRAD_DIR_DEFAULT: Double = 90.0
        public const val PT_BTN_BORDER_RADIUS_DEFAULT: Float = 4f
        public const val PT_BTN_BORDER_WIDTH_DEFAULT: Float = 1f

        // Image border configuration (applies to the main notification image across templates)
        public const val PT_IMG_BORDER_CLR: String = "pt_img_border_clr"
        public const val PT_IMG_CORNER_RADIUS: String = "pt_img_corner_radius"
        public const val PT_IMG_BORDER_WIDTH: String = "pt_img_border_width"

        // Vertical Image Template - Collapsed button configuration
        public const val PT_BTN_CLR_COLLAPSED: String = "pt_btn_clr_collapsed"
        public const val PT_BTN_BORDER_CLR_COLLAPSED: String = "pt_btn_border_clr_collapsed"
        public const val PT_BTN_TEXT_CLR_COLLAPSED: String = "pt_btn_text_clr_collapsed"
        public const val PT_BTN_GRAD_CLR1_COLLAPSED: String = "pt_btn_grad_clr1_collapsed"
        public const val PT_BTN_GRAD_CLR2_COLLAPSED: String = "pt_btn_grad_clr2_collapsed"

        @JvmField
        public val COLOR_KEYS: Set<String> = java.util.Set.of(
            PT_TITLE_COLOR,
            PT_MSG_COLOR,
            PT_BG,
            PT_META_CLR,
            PT_CHRONO_TITLE_COLOUR,
            PT_CHRONO_BG_CLR,
            PT_CHRONO_BORDER_CLR,
            PT_PRODUCT_DISPLAY_ACTION_COLOUR,
            PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR,
            PT_CHRONO_GRAD_CLR1,
            PT_CHRONO_GRAD_CLR2,
            PT_TEXT1_COLOR,
            PT_TEXT2_COLOR,
            PT_BTN_CLR,
            PT_BTN_BORDER_CLR,
            PT_BTN_TEXT_CLR,
            PT_BTN_GRAD_CLR1,
            PT_BTN_GRAD_CLR2,
            PT_BTN_CLR_COLLAPSED,
            PT_BTN_BORDER_CLR_COLLAPSED,
            PT_BTN_TEXT_CLR_COLLAPSED,
            PT_BTN_GRAD_CLR1_COLLAPSED,
            PT_BTN_GRAD_CLR2_COLLAPSED,
            PT_IMG_BORDER_CLR
        )
    }
}
