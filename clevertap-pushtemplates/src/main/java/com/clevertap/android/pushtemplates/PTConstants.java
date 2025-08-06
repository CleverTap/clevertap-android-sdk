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

    public static final String PT_PRICE_LIST = "pt_price_list";

    public static final String PT_SMALLTEXT_LIST = "pt_small_text_list";

    public static final String PT_BIGTEXT_LIST = "pt_big_text_list";

    public static final String PT_MANUAL_CAROUSEL_TYPE = "pt_manual_carousel_type";

    public static final String PT_MANUAL_CAROUSEL_FILMSTRIP = "filmstrip";

    public static boolean PT_FALLBACK = false;

    public static String PT_IMAGE_PATH_LIST = "";

    public static final int ONE_SECOND = 1000;

    public static final String DEFAULT_DL = "default_dl";

    public static final String NOTIF_TAG = "wzrk_pn";

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

    public static final String PT_BIG_IMG_ALT_TEXT = "pt_big_img_alt_text";

    // Keys for collapsed media in zero bezel
    public static final String PT_GIF_COLLAPSED = "pt_gif_collapsed";
    public static final String PT_GIF_FRAMES_COLLAPSED = "pt_gif_frames_collapsed";
    public static final String PT_SCALE_TYPE_COLLAPSED = "pt_scale_type_collapsed";
    public static final String PT_BIG_IMG_COLLAPSED = "pt_big_img_collapsed";
    public static final String PT_BIG_IMG_COLLAPSED_ALT_TEXT = "pt_big_img_collapsed_alt_text";

    public static final String PT_SMALL_IMG = "pt_small_img";

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

    public static final String PT_DOT_SEP = "pt_dot_sep";

    public static final String PT_COLOUR_GREY = "#A6A6A6";

    public static final String PT_META_CLR_DEFAULTS = PT_COLOUR_GREY;

    public static final int PT_FLIP_INTERVAL_TIME = 4 * ONE_SECOND;

    public static final String KEY_CLICKED_STAR = "clickedStar";

    public static final String KEY_REQUEST_CODES = "requestCodes";

    public static final Set<String> COLOR_KEYS = Set.of(
            PT_TITLE_COLOR,
            PT_MSG_COLOR,
            PT_BG,
            PT_META_CLR,
            PT_SMALL_ICON_COLOUR,
            PT_CHRONO_TITLE_COLOUR,
            PT_PRODUCT_DISPLAY_ACTION_COLOUR,
            PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR);

}
