package com.clevertap.android.pushtemplates;

import java.util.Set;

@SuppressWarnings("WeakerAccess")
//Using common keys from core-sdk constants
public class PTConstants {

    public static final String LOG_TAG = "PTLog";

    public static final String PT_MANUAL_CAROUSEL_CURRENT = "pt_manual_carousel_current";

    public static final String PT_IMAGE_LIST = "pt_image_list";

    public static final String PT_COLLAPSE_KEY = "pt_ck";

    public static final String PT_DEEPLINK_LIST = "pt_deeplink_list";

    public static final String PT_FLIP_INTERVAL = "pt_flip_interval";

    public static final String PT_DIR = "pt_dir";

    public static final String PT_SOUND_FILE_NAME = "pt_silent_sound";

    public static final String PT_RATING_C2A_KEY = "rating_";

    public static final String PT_5CTA_C2A_KEY = "5cta_";

    public static final String PT_VT_C2A_KEY = "vt_btn";

    public static final String PT_VT_C2A_COLLAPSED_KEY = "vt_btn_collapsed";

    public static final String PT_PRICE_LIST = "pt_price_list";

    public static final String PT_SMALLTEXT_LIST = "pt_small_text_list";

    public static final String PT_BIGTEXT_LIST = "pt_big_text_list";

    public static final String PT_MANUAL_CAROUSEL_TYPE = "pt_manual_carousel_type";

    public static final String PT_MANUAL_CAROUSEL_FILMSTRIP = "filmstrip";

    public static final int ONE_SECOND = 1000;

    public static final long ONE_SECOND_LONG = 1000L;

    public static final String DEFAULT_DL = "default_dl";

    public static final String PT_ID = "pt_id";

    public static final String PT_NOTIF_ICON = "pt_ico";

    public static final String PT_TITLE = "pt_title";

    public static final String PT_MSG = "pt_msg";

    public static final String PT_MSG_SUMMARY = "pt_msg_summary";

    public static final String PT_TITLE_COLOR = "pt_title_clr";

    public static final String PT_DARK_MODE_SUFFIX = "_dark";

    public static final String PT_MSG_COLOR = "pt_msg_clr";

    public static final String PT_BG = "pt_bg";

    public static final String ALT_TEXT_SUFFIX = "_alt_text";

    public static final String PT_BIG_IMG = "pt_big_img";

    public static final String PT_GIF = "pt_gif";

    // GIF key for terminal notification of timer template
    public static final String PT_GIF_ALT = "pt_gif_alt";

    public static final String PT_GIF_FRAMES = "pt_gif_frames";

    public static final String PT_GIF_FRAMES_ALT = "pt_gif_frames_alt";

    public static final String PT_SCALE_TYPE_ALT = "pt_scale_type_alt";

    public static final String PT_BIG_IMG_ALT_TEXT = "pt_big_img_alt_text";

    public static final String PT_STICKY = "pt_sticky";

    public static final String PT_DISMISS = "pt_dismiss";

    // Keys for collapsed media in zero bezel
    public static final String PT_GIF_COLLAPSED = "pt_gif_collapsed";
    public static final String PT_GIF_FRAMES_COLLAPSED = "pt_gif_frames_collapsed";
    public static final String PT_SCALE_TYPE_COLLAPSED = "pt_scale_type_collapsed";
    public static final String PT_BIG_IMG_COLLAPSED = "pt_big_img_collapsed";
    public static final String PT_BIG_IMG_COLLAPSED_ALT_TEXT = "pt_big_img_collapsed_alt_text";

    public static final String PT_JSON = "pt_json";

    public static final String PT_BUY_NOW_DL = "pt_buy_now_dl";

    public static final String PT_DEFAULT_DL = "pt_default_dl";

    public static final String PT_SMALL_VIEW = "pt_small_view";

    public static final String PT_TIMER_THRESHOLD = "pt_timer_threshold";

    public static final String PT_RENDER_TERMINAL = "pt_render_terminal";

    public static final String PT_INPUT_LABEL = "pt_input_label";

    public static final String PT_INPUT_KEY = "pt_input_reply";

    public static final String PT_INPUT_FEEDBACK = "pt_input_feedback";

    public static final int PT_INPUT_TIMEOUT = 1300;

    public static final String PT_NOTIF_ID = "notificationId";

    public static final String PT_INPUT_AUTO_OPEN = "pt_input_auto_open";

    public static final String PT_EVENT_NAME_KEY = "pt_event_name";

    public static final String PT_EVENT_PROPERTY_KEY = "pt_event_property";

    public static final String PT_EVENT_PROPERTY_SEPERATOR = "pt_event_property_";

    public static final String PT_DISMISS_ON_CLICK = "pt_dismiss_on_click";

    public static final String PT_CHRONO_TITLE_COLOUR = "pt_chrono_title_clr";

    public static final String PT_CHRONO_BG_CLR = "pt_chrono_bg_clr";
    public static final String PT_CHRONO_BORDER_CLR = "pt_chrono_border_clr";
    public static final String PT_CHRONO_STYLE = "pt_chrono_style";
    public static final String PT_CHRONO_GRAD_CLR1 = "pt_chrono_grad_clr1";
    public static final String PT_CHRONO_GRAD_CLR2 = "pt_chrono_grad_clr2";
    public static final String PT_CHRONO_GRAD_DIR = "pt_chrono_grad_dir";
    public static final String PT_CHRONO_BORDER_RADIUS = "pt_chrono_border_radius";
    public static final String PT_CHRONO_BORDER_WIDTH = "pt_chrono_border_width";


    public static final String PT_PRODUCT_DISPLAY_ACTION = "pt_product_display_action";

    public static final String PT_PRODUCT_DISPLAY_ACTION_COLOUR = "pt_product_display_action_clr";

    public static final String PT_TIMER_END = "pt_timer_end";

    public static final String PT_TIMER_SPLIT = "\\$D_";

    public static final int PT_TIMER_MIN_THRESHOLD = 10;

    public static final String PT_BIG_IMG_ALT = "pt_big_img_alt";
    public static final String PT_BIG_IMG_ALT_ALT_TEXT = "pt_big_img_alt_alt_text";

    public static final String PT_TITLE_ALT = "pt_title_alt";

    public static final String PT_MSG_ALT = "pt_msg_alt";

    public static final String PT_MSG_SUMMARY_ALT = "pt_msg_summary_alt";

    public static final String PT_PRODUCT_DISPLAY_LINEAR = "pt_product_display_linear";

    public static final String PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR = "pt_product_display_action_text_clr";

    public static final String PT_SMALL_ICON_COLOUR = "pt_small_icon_clr";

    public static final String PT_META_CLR = "pt_meta_clr";

    public static final String PT_CANCEL_NOTIF_ID = "pt_cancel_notif_id";

    public static final String PT_ACTION_ID = "actionId";

    public static final String PT_RIGHT_SWIPE = "right_swipe";

    public static final String PT_MANUAL_CAROUSEL_FROM = "manual_carousel_from";

    public static final String PT_IMAGE_1 = "img1";

    public static final String PT_CURRENT_POSITION = "pt_current_position";

    public static final String PT_BUY_NOW = "buynow";

    public static final String PT_SCALE_TYPE = "pt_scale_type";

    public static final String TEXT_ONLY = "text_only";

    public static final String PT_SUBTITLE = "pt_subtitle";

    public static final String PT_DISMISS_INTENT = "pt_dismiss_intent";

    public static final String PT_SILENT_CHANNEL_ID = "pt_silent_sound_channel";

    public static final CharSequence PT_SILENT_CHANNEL_NAME = "Silent Channel";

    public static final String PT_SILENT_CHANNEL_DESC = "A channel to silently update notifications";

    public static final String PT_RATING_TOAST = "pt_rating_toast";

    public static final String PT_COLOUR_GREY = "#A6A6A6";

    public static final String PT_META_CLR_DEFAULTS = PT_COLOUR_GREY;

    public static final int PT_FLIP_INTERVAL_TIME = 4 * ONE_SECOND;

    public static final String KEY_CLICKED_STAR = "clickedStar";

    public static final String KEY_REQUEST_CODES = "requestCodes";

    /*
     * Custom Rating template (pt_custom_rating) keys.
     *
     * Separate from the Classic Rating template (pt_rating), whose keys and behaviour are frozen.
     * Key names follow the payload contract in the Rating PRD (section 10.2).
     */

    // Required: "icon" or "text". Anything else renders the Basic template (R-22).
    public static final String PT_RATING_STYLE = "pt_rating_style";

    // Required: number of rating positions, 2-5. Must match the populated positions.
    public static final String PT_RATING_COUNT = "pt_rating_count";

    // Per position, 1..count. Icon style uses pt_rating_icon{n} for the unselected state and
    // pt_rating_icon{n}_sel for the selected state. Text style uses pt_rating_label{n}.
    public static final String PT_RATING_ICON_PREFIX = "pt_rating_icon";
    public static final String PT_RATING_ICON_SELECTED_SUFFIX = "_sel";
    public static final String PT_RATING_LABEL_PREFIX = "pt_rating_label";

    // Tint applied to monochrome icon assets, and to the text chip background, unselected and
    // selected. A chip needs four colours in all; the label's own two live in the keys below.
    public static final String PT_RATING_ICON_CLR = "pt_rating_icon_clr";
    public static final String PT_RATING_ICON_SEL_CLR = "pt_rating_icon_sel_clr";

    // Text style only: the label colour inside the chip, unselected and selected. Absent, the label
    // inherits the notification's message and title colours respectively.
    public static final String PT_RATING_LABEL_CLR = "pt_rating_label_clr";
    public static final String PT_RATING_LABEL_SEL_CLR = "pt_rating_label_sel_clr";

    // Submit button.
    public static final String PT_RATING_CTA_LABEL = "pt_rating_cta_label";
    public static final String PT_RATING_CTA_DL = "pt_rating_cta_dl";
    public static final String PT_RATING_CTA_BG_CLR = "pt_rating_cta_bg_clr";
    public static final String PT_RATING_CTA_BORDER_CLR = "pt_rating_cta_border_clr";
    public static final String PT_RATING_CTA_TXT_CLR = "pt_rating_cta_txt_clr";
    public static final String PT_RATING_CTA_RADIUS = "pt_rating_cta_radius";

    // Optional confirmation message. When present, submitting swaps the notification to a
    // confirmation state instead of dismissing it.
    public static final String PT_RATING_CONFIRM_MSG = "pt_rating_confirm_msg";

    public static final int PT_RATING_COUNT_MIN = 2;
    public static final int PT_RATING_COUNT_MAX = 5;
    public static final int PT_RATING_CTA_RADIUS_DEFAULT = 8;
    public static final int PT_RATING_RADIUS_MAX = 32;
    public static final int PT_RATING_CTA_LABEL_MAX_LEN = 25;

    /*
     * Rating Submitted event properties (FR-EVT-01). wzrk_c2a carries the tapped position for
     * continuity with the Classic template; these three are new and typed.
     */
    public static final String PT_RATING_EVENT_NAME = "Rating Submitted";
    public static final String PT_RATING_EVENT_VALUE = "rating_value";
    public static final String PT_RATING_EVENT_SCALE = "rating_scale";
    public static final String PT_RATING_EVENT_STYLE = "rating_style";

    /*
     * Internal intent extras for the custom rating selection/submit flow. These are never part of
     * the campaign payload.
     */

    // Marks a broadcast as the submit tap rather than a position tap.
    public static final String PT_RATING_SUBMIT = "ptRatingSubmit";

    // Selected position (1..count) carried between the position tap and the submit tap.
    public static final String PT_RATING_SELECTED_POSITION = "ptRatingSelectedPosition";

    // Vertical Image Template Keys
    public static final String PT_TEXT1 = "pt_text1";
    public static final String PT_TEXT2 = "pt_text2";
    public static final String PT_TEXT1_COLOR = "pt_text1_clr";
    public static final String PT_TEXT2_COLOR = "pt_text2_clr";

    // Vertical Image Template - Button configuration
    public static final String PT_BTN_NAME = "pt_btn_name";
    public static final String PT_BTN_DL = "pt_btn_dl";
    public static final String PT_BTN_STYLE = "pt_btn_style";
    public static final String PT_BTN_CLR = "pt_btn_clr";
    public static final String PT_BTN_BORDER_CLR = "pt_btn_border_clr";
    public static final String PT_BTN_TEXT_CLR = "pt_btn_text_clr";
    public static final String PT_BTN_GRAD_CLR1 = "pt_btn_grad_clr1";
    public static final String PT_BTN_GRAD_CLR2 = "pt_btn_grad_clr2";
    public static final String PT_BTN_GRAD_DIR = "pt_btn_grad_dir";
    public static final String PT_BTN_BORDER_RADIUS = "pt_btn_border_radius";
    public static final String PT_BTN_BORDER_WIDTH = "pt_btn_border_width";

    public static final double PT_BTN_GRAD_DIR_DEFAULT = 90.0;
    public static final float PT_BTN_BORDER_RADIUS_DEFAULT = 4f;
    public static final float PT_BTN_BORDER_WIDTH_DEFAULT = 1f;

    /*
     * Expanded media (image or GIF) border configuration, shared by every template that renders one.
     *
     * Both size values are in dp, matching pt_btn_border_*, pt_chrono_border_* and
     * pt_rating_cta_radius. They are converted into the bitmap's pixel space at draw time.
     */
    public static final String PT_MEDIA_BORDER_CLR = "pt_media_border_clr";
    public static final String PT_MEDIA_RADIUS = "pt_media_radius";
    public static final String PT_MEDIA_BORDER_WIDTH = "pt_media_border_width";

    public static final int PT_MEDIA_RADIUS_MAX = 32;
    public static final int PT_MEDIA_BORDER_WIDTH_MAX = 16;
    public static final int PT_MEDIA_BORDER_WIDTH_DEFAULT = 1;

    // Vertical Image Template - Collapsed button configuration
    public static final String PT_BTN_CLR_COLLAPSED = "pt_btn_clr_collapsed";
    public static final String PT_BTN_BORDER_CLR_COLLAPSED = "pt_btn_border_clr_collapsed";
    public static final String PT_BTN_TEXT_CLR_COLLAPSED = "pt_btn_text_clr_collapsed";
    public static final String PT_BTN_GRAD_CLR1_COLLAPSED = "pt_btn_grad_clr1_collapsed";
    public static final String PT_BTN_GRAD_CLR2_COLLAPSED = "pt_btn_grad_clr2_collapsed";

    public static final Set<String> COLOR_KEYS = Set.of(
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
            PT_MEDIA_BORDER_CLR,
            PT_RATING_ICON_CLR,
            PT_RATING_ICON_SEL_CLR,
            PT_RATING_LABEL_CLR,
            PT_RATING_LABEL_SEL_CLR,
            PT_RATING_CTA_BG_CLR,
            PT_RATING_CTA_BORDER_CLR,
            PT_RATING_CTA_TXT_CLR);

}
