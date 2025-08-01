package com.clevertap.android.pushtemplates

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat.Builder
import com.clevertap.android.pushtemplates.PTConstants.*
import com.clevertap.android.pushtemplates.content.FiveIconBigContentView
import com.clevertap.android.pushtemplates.content.FiveIconSmallContentView
import com.clevertap.android.pushtemplates.styles.*
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.interfaces.AudibleNotification
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Data class representing image data with URL and alt text for better readability
 */
data class ImageData(
    val url: String,
    val altText: String
)

class TemplateRenderer : INotificationRenderer, AudibleNotification {
    private var pt_id: String? = null
    private var templateType: TemplateType? = null
    internal var pt_title: String? = null
    internal var pt_msg: String? = null
    internal var pt_msg_summary: String? = null
    internal var pt_large_icon: String? = null
    internal var pt_big_img: String? = null
    internal var pt_big_img_alt_text: String = ""
    internal var pt_title_clr: String? = null
    internal var pt_msg_clr: String? = null
    internal var pt_chrono_title_clr: String? = null
    internal var imageList: ArrayList<ImageData>? = null
    internal var deepLinkList: ArrayList<String>? = null
    internal var bigTextList: ArrayList<String>? = null
    internal var smallTextList: ArrayList<String>? = null
    internal var priceList: ArrayList<String>? = null
    internal var pt_product_display_action: String? = null
    internal var pt_product_display_action_clr: String? = null
    internal var pt_bg: String? = null
    internal var pt_rating_default_dl: String? = null
    internal var pt_small_view: String? = null
    internal var smallIcon = 0
    internal var pt_dot = 0
    internal var pt_render_terminal = true
    var pt_timer_threshold = 0
    internal var pt_input_label: String? = null
    var pt_input_feedback: String? = null
    internal var pt_input_auto_open: String? = null
    internal var pt_dismiss_on_click: String? = null
    var pt_timer_end = 0
    private var pt_title_alt: String? = null
    private var pt_msg_alt: String? = null
    private var pt_msg_summary_alt: String? = null
    private var pt_big_img_alt: String? = null
    private var pt_big_img_alt_alt_text: String? = null
    internal var pt_product_display_linear: String? = null
    internal var pt_meta_clr: String? = null
    internal var pt_product_display_action_text_clr: String? = null
    internal var pt_small_icon_clr: String? = null
    internal var pt_small_icon: Bitmap? = null
    private var pt_cancel_notif_id: String? = null
    private var pt_cancel_notif_ids: ArrayList<Int>? = null
    var actions: JSONArray? = null
    var actionButtons = emptyList<ActionButton>()
    var actionButtonPendingIntents = mutableMapOf<String, PendingIntent>()
    internal var pt_subtitle: String? = null
    private var pID: String? = null
    internal var pt_flip_interval = 0
    private var pt_collapse_key: Any? = null
    internal var pt_manual_carousel_type: String? = null
    internal var pt_scale_type: PTScaleType = PTScaleType.CENTER_CROP
    internal var config: CleverTapInstanceConfig? = null
    internal var notificationId: Int = -1//Creates a instance field for access in ContentViews->PendingIntentFactory

    enum class LogLevel(private val value: Int) {
        OFF(-1), INFO(0), DEBUG(2), VERBOSE(3);

        fun intValue(): Int {
            return value
        }
    }

    internal constructor(context: Context, extras: Bundle) {
        setUp(context, extras, null)
    }

    private constructor(context: Context, extras: Bundle, config: CleverTapInstanceConfig) {
        setUp(context, extras, config)
    }

    override fun getMessage(extras: Bundle): String? {
        return pt_msg
    }

    override fun getTitle(extras: Bundle, context: Context): String? {
        return pt_title
    }

    override fun renderNotification(
        extras: Bundle, context: Context, nb: Builder,
        config: CleverTapInstanceConfig,
        notificationId: Int
    ): Builder? {
        if (pt_id == null) {
            PTLog.verbose("Template ID not provided. Cannot create the notification")
            return null
        }
        this.notificationId = notificationId
        this.actionButtons = getActionButtons(context, extras, notificationId, actions)
        when (templateType) {
            TemplateType.BASIC ->
                if (ValidatorFactory.getValidator(TemplateType.BASIC, this)?.validate() == true)
                    return BasicStyle(this).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.AUTO_CAROUSEL ->
                if (ValidatorFactory.getValidator(TemplateType.AUTO_CAROUSEL, this)?.validate() == true)
                    return AutoCarouselStyle(this).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.MANUAL_CAROUSEL ->
                if (ValidatorFactory.getValidator(TemplateType.MANUAL_CAROUSEL, this)?.validate() == true)
                    return ManualCarouselStyle(this, extras).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.RATING ->
                if (ValidatorFactory.getValidator(TemplateType.RATING, this)?.validate() == true)
                    return RatingStyle(this, extras).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.FIVE_ICONS ->
                if (ValidatorFactory.getValidator(TemplateType.FIVE_ICONS, this)?.validate() == true) {
                    val fiveIconStyle  = FiveIconStyle(this, extras)
                    val fiveIconNotificationBuilder = fiveIconStyle.builderFromStyle(
                        context,
                        extras,
                        notificationId,
                        nb
                    )

                    /**
                     * Checks whether the imageUrls are perfect to download icon's bitmap,
                     * if not then do not render notification
                     */
                    return if ((fiveIconStyle.fiveIconSmallContentView as
                                FiveIconSmallContentView).getUnloadedFiveIconsCount() > 2 ||
                        (fiveIconStyle.fiveIconBigContentView as FiveIconBigContentView).getUnloadedFiveIconsCount() > 2){
                        null
                    } else fiveIconNotificationBuilder
                }

            TemplateType.PRODUCT_DISPLAY -> if (ValidatorFactory.getValidator(TemplateType.PRODUCT_DISPLAY, this)
                    ?.validate() == true
            )
                return ProductDisplayStyle(this, extras).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.ZERO_BEZEL ->
                if (ValidatorFactory.getValidator(TemplateType.ZERO_BEZEL, this)?.validate() == true)
                    return ZeroBezelStyle(this).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.TIMER -> if (VERSION.SDK_INT >= VERSION_CODES.O) {
                ValidatorFactory.getValidator(TemplateType.TIMER, this)
                    ?.takeIf { it.validate() }
                    ?.let {
                        getTimerEnd()?.let { timerEnd ->
                            if (pt_render_terminal) {
                                timerRunner(context, extras, notificationId, timerEnd)
                            }
                            return TimerStyle(this, extras)
                                .builderFromStyle(context, extras, notificationId, nb)
                                .setTimeoutAfter(timerEnd.toLong())
                        }
                    }
            }
            else {
                PTLog.debug("Push Templates SDK supports Timer Notifications only on or above Android Oreo, reverting to basic template")
                if (ValidatorFactory.getValidator(TemplateType.BASIC, this)?.validate() == true) {
                    return BasicStyle(this).builderFromStyle(context, extras, notificationId, nb)
                }
            }

            TemplateType.INPUT_BOX -> if (ValidatorFactory.getValidator(TemplateType.INPUT_BOX, this)
                    ?.validate() == true
            )
                return InputBoxStyle(this).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.CANCEL -> renderCancelNotification(context)
            else -> {
                PTLog.verbose("operation not defined!")
            }
        }
        return null
    }

    private fun renderCancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (pt_cancel_notif_id != null && pt_cancel_notif_id!!.isNotEmpty()) {
            val notificationId = pt_cancel_notif_id!!.toInt()
            notificationManager.cancel(notificationId)
        } else {
            if (pt_cancel_notif_ids!!.size > 0) {
                for (i in 0..pt_cancel_notif_ids!!.size) {
                    notificationManager.cancel(pt_cancel_notif_ids!![i])
                }
            }
        }
    }

    private fun getTimerEnd(): Int? {
        var timer_end: Int? = null
        if (pt_timer_threshold != -1 && pt_timer_threshold >= PT_TIMER_MIN_THRESHOLD) {
            timer_end = pt_timer_threshold * ONE_SECOND + ONE_SECOND
        } else if (pt_timer_end >= PT_TIMER_MIN_THRESHOLD) {
            timer_end = pt_timer_end * ONE_SECOND + ONE_SECOND
        } else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + PT_TIMER_END)
        }
        return timer_end
    }

    @RequiresApi(VERSION_CODES.M)
    private fun timerRunner(context: Context, extras: Bundle, notificationId: Int, delay: Int?) {
        val handler = Handler(Looper.getMainLooper())


        if (delay != null) {
            handler.postDelayed({
                if (Utils.isNotificationInTray(
                        context,
                        notificationId
                    ) && ValidatorFactory.getValidator(TemplateType.BASIC, this)?.validate() == true
                ) {
                    val applicationContext = context.applicationContext
                    val basicTemplateBundle = extras.clone() as Bundle
                    basicTemplateBundle.remove("wzrk_rnv")
                    basicTemplateBundle.putString(Constants.WZRK_PUSH_ID, null) // skip dupe check
                    basicTemplateBundle.putString(PT_ID, "pt_basic") // set to basic


                    /**
                     *  Update existing payload bundle with new title,msg,img for Basic template
                     */
                    val ptJsonStr  = basicTemplateBundle.getString(PT_JSON)
                    var ptJsonObj: JSONObject? = null
                    if (ptJsonStr != null) {
                        try {
                            ptJsonObj = JSONObject(ptJsonStr)
                        } catch (e: Exception) {
                            Logger.v("Unable to convert JSON to String")
                        }
                    }

                    if (pt_title_alt != null && pt_title_alt!!.isNotEmpty()) {
                        ptJsonObj?.put(PT_TITLE,pt_title_alt) ?: basicTemplateBundle.putString(
                            PT_TITLE,
                            pt_title_alt
                        )
                    }
                    if (pt_big_img_alt != null && pt_big_img_alt!!.isNotEmpty()) {
                        ptJsonObj?.put(PT_BIG_IMG, pt_big_img_alt) ?: basicTemplateBundle.putString(
                            PT_BIG_IMG,
                            pt_big_img_alt
                        )

                        ptJsonObj?.put(PT_BIG_IMG_ALT_TEXT, pt_big_img_alt_alt_text) ?: basicTemplateBundle.putString(
                            PT_BIG_IMG_ALT_TEXT,
                            pt_big_img_alt_alt_text
                        )
                    }
                    if (pt_msg_alt != null && pt_msg_alt!!.isNotEmpty()) {
                        ptJsonObj?.put(PT_MSG, pt_msg_alt) ?: basicTemplateBundle.putString(
                            PT_MSG,
                            pt_msg_alt
                        )
                    }

                    if (pt_msg_summary_alt != null && pt_msg_summary_alt!!.isNotEmpty()) {
                        ptJsonObj?.put(PT_MSG_SUMMARY, pt_msg_summary_alt) ?: basicTemplateBundle.putString(
                            PT_MSG_SUMMARY,
                            pt_msg_summary_alt
                        )
                    }


                    if (ptJsonObj != null) {
                        basicTemplateBundle.putString(
                            PT_JSON,
                            ptJsonObj.toString()
                        )
                    }
                    // force random id generation
                    basicTemplateBundle.putString(PT_COLLAPSE_KEY, null)
                    basicTemplateBundle.putString(Constants.WZRK_COLLAPSE, null)
                    basicTemplateBundle.remove(Constants.PT_NOTIF_ID)
                    val templateRenderer: INotificationRenderer =
                        TemplateRenderer(applicationContext, basicTemplateBundle)
                    val cleverTapAPI = CleverTapAPI
                        .getGlobalInstance(
                            applicationContext,
                            PushNotificationUtil.getAccountIdFromNotificationBundle(basicTemplateBundle)
                        )
                    cleverTapAPI?.renderPushNotification(
                        templateRenderer,
                        applicationContext,
                        basicTemplateBundle
                    )
                }
            }, (delay - 100).toLong())
        }
    }

    override fun setSmallIcon(smallIcon: Int, context: Context) {
        this.smallIcon = smallIcon
        try {
            pt_small_icon = Utils.setBitMapColour(context, smallIcon, pt_small_icon_clr, PT_META_CLR_DEFAULTS)
        } catch (e: NullPointerException) {
            PTLog.debug("NPE while setting small icon color")
        }
    }

    override fun getActionButtonIconKey(): String {
        return PT_NOTIF_ICON
    }

    override fun getCollapseKey(extras: Bundle): Any? {
        return pt_collapse_key
    }

    override fun setSound(
        context: Context, extras: Bundle, nb: Builder, config: CleverTapInstanceConfig
    ): Builder {
        try {
            if (extras.containsKey(Constants.WZRK_SOUND)) {
                var soundUri: Uri? = null
                val o = extras[Constants.WZRK_SOUND]
                if (o is Boolean && o) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else if (o is String) {
                    var s = o
                    if (s == "true") {
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } else if (s.isNotEmpty()) {
                        if (s.contains(".mp3") || s.contains(".ogg") || s.contains(".wav")) {
                            s = s.substring(0, s.length - 4)
                        }
                        soundUri = Uri
                            .parse(
                                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName
                                        + "/raw/" + s
                            )
                    }
                }
                if (soundUri != null) {
                    nb.setSound(soundUri)
                }
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Could not process sound parameter", t)
        }
        return nb
    }

    private fun setUp(context: Context, extras: Bundle, config: CleverTapInstanceConfig?) {
        val isDarkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        pt_id = extras.getString(PT_ID)
        val pt_json = extras.getString(PT_JSON)
        if (pt_id != null) {
            templateType = TemplateType.fromString(pt_id)
            var newExtras: Bundle? = null
            try {
                if (pt_json.isNotNullAndEmpty()) {
                    newExtras = Utils.fromJson(JSONObject(pt_json))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (newExtras != null) extras.putAll(newExtras)
        }

        val darkModeAdaptiveColors = Utils.createColorMap(extras, isDarkMode)
        val altTextDefault = context.getString(R.string.pt_big_image_alt)
        pt_msg = extras.getString(PT_MSG)
        pt_msg_summary = extras.getString(PT_MSG_SUMMARY)
        pt_msg_clr = darkModeAdaptiveColors[PT_MSG_COLOR]
        pt_title = extras.getString(PT_TITLE)
        pt_title_clr = darkModeAdaptiveColors[PT_TITLE_COLOR]
        pt_meta_clr = darkModeAdaptiveColors[PT_META_CLR]
        pt_bg = darkModeAdaptiveColors[PT_BG]
        pt_big_img = extras.getString(PT_BIG_IMG)
        pt_big_img_alt_text = extras.getString(PT_BIG_IMG_ALT_TEXT, altTextDefault)
        pt_large_icon = extras.getString(PT_NOTIF_ICON)
        pt_small_view = extras.getString(PT_SMALL_VIEW)
        imageList = Utils.getImageDataListFromExtras(extras, altTextDefault)
        deepLinkList = Utils.getDeepLinkListFromExtras(extras)
        bigTextList = Utils.getBigTextFromExtras(extras)
        smallTextList = Utils.getSmallTextFromExtras(extras)
        priceList = Utils.getPriceFromExtras(extras)
        pt_rating_default_dl = extras.getString(PT_DEFAULT_DL)
        pt_timer_threshold = Utils.getTimerThreshold(extras)

        // true by default if not present in extras
        pt_render_terminal = extras.getString(PT_RENDER_TERMINAL)?.equals("true", ignoreCase = true) ?: true
        pt_input_label = extras.getString(PT_INPUT_LABEL)
        pt_input_feedback = extras.getString(PT_INPUT_FEEDBACK)
        pt_input_auto_open = extras.getString(PT_INPUT_AUTO_OPEN)
        pt_dismiss_on_click = extras.getString(PT_DISMISS_ON_CLICK)
        pt_chrono_title_clr = darkModeAdaptiveColors[PT_CHRONO_TITLE_COLOUR]
        pt_product_display_action = extras.getString(PT_PRODUCT_DISPLAY_ACTION)
        pt_product_display_action_clr = darkModeAdaptiveColors[PT_PRODUCT_DISPLAY_ACTION_COLOUR]
        pt_timer_end = Utils.getTimerEnd(extras, System.currentTimeMillis())
        pt_big_img_alt = extras.getString(PT_BIG_IMG_ALT)
        pt_big_img_alt_alt_text = extras.getString(PT_BIG_IMG_ALT_ALT_TEXT, altTextDefault)
        pt_msg_alt = extras.getString(PT_MSG_ALT)
        pt_msg_summary_alt = extras.getString(PT_MSG_SUMMARY_ALT)
        pt_title_alt = extras.getString(PT_TITLE_ALT)
        pt_product_display_linear = extras.getString(PT_PRODUCT_DISPLAY_LINEAR)
        pt_product_display_action_text_clr = darkModeAdaptiveColors[PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR]
        pt_small_icon_clr = darkModeAdaptiveColors[PT_SMALL_ICON_COLOUR]
        pt_cancel_notif_id = extras.getString(PT_CANCEL_NOTIF_ID)
        pt_cancel_notif_ids = Utils.getNotificationIds(context)
        actions = Utils.getActionKeys(extras)
        pt_subtitle = extras.getString(PT_SUBTITLE)
        pt_collapse_key = extras[PT_COLLAPSE_KEY]
        pt_flip_interval = Utils.getFlipInterval(extras)
        pt_scale_type = PTScaleType.fromString(extras.getString(PT_SCALE_TYPE))
        pID = extras.getString(Constants.WZRK_PUSH_ID)
        pt_manual_carousel_type = extras.getString(PT_MANUAL_CAROUSEL_TYPE)
        if (config != null) {
            this.config = config
        }
        setKeysFromDashboard(extras)
    }

    private fun setKeysFromDashboard(extras: Bundle) {
        if (pt_title == null || pt_title!!.isEmpty()) {
            pt_title = extras.getString(Constants.NOTIF_TITLE)
        }
        if (pt_msg == null || pt_msg!!.isEmpty()) {
            pt_msg = extras.getString(Constants.NOTIF_MSG)
        }
        if (pt_msg_summary == null || pt_msg_summary!!.isEmpty()) {
            pt_msg_summary = extras.getString(Constants.WZRK_MSG_SUMMARY)
        }
        if (pt_big_img == null || pt_big_img!!.isEmpty()) {
            pt_big_img = extras.getString(Constants.WZRK_BIG_PICTURE)
        }
        if (pt_rating_default_dl == null || pt_rating_default_dl!!.isEmpty()) {
            pt_rating_default_dl = extras.getString(Constants.DEEP_LINK_KEY)
        }
        if (pt_meta_clr == null || pt_meta_clr!!.isEmpty()) {
            pt_meta_clr = extras.getString(Constants.WZRK_COLOR)
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr!!.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_COLOR)
        }
        if (pt_subtitle == null || pt_subtitle!!.isEmpty()) {
            pt_subtitle = extras.getString(Constants.WZRK_SUBTITLE)
        }
        if (pt_collapse_key == null) {
            pt_collapse_key = extras[Constants.WZRK_COLLAPSE]
        }
    }

    override fun setActionButtons(
        context: Context?,
        extras: Bundle?,
        notificationId: Int,
        nb: Builder,
        actions: JSONArray?
    ): Builder {
        actionButtons.forEach { button ->
            val pendingIntent = actionButtonPendingIntents[button.id]
            if (pendingIntent != null) {
                nb.addAction(button.icon, button.label, pendingIntent)
            }
        }
        return nb
    }

    internal fun getActionButtons(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        actions: JSONArray?
    ): List<ActionButton> {
        val actionButtons = mutableListOf<ActionButton>()
        if (actions != null && actions.length() > 0) {
            for (i in 0 until actions.length()) {
                try {
                    val action = actions.getJSONObject(i)
                    val label = action.optString("l")
                    val ico = action.optString(actionButtonIconKey)
                    val id = action.optString("id")

                    if (label.isEmpty() || id.isEmpty()) {
                        Logger.d("not adding push notification action: action label or id missing")
                        continue
                    }
                    var icon = 0
                    if (ico.isNotEmpty()) {
                        try {
                            icon = context.resources.getIdentifier(ico, "drawable", context.packageName)
                        } catch (t: Throwable) {
                            Logger.d("unable to add notification action icon: " + t.localizedMessage)
                        }
                    }

                    // Create the button object
                    val button = ActionButton(id, label, icon)
                    actionButtons.add(button)

                    // Create and store the pendingIntent
                    val pendingIntent = createActionButtonPendingIntent(context, action, extras, notificationId)
                    if (pendingIntent != null) {
                        actionButtonPendingIntents[id] = pendingIntent
                    }
                } catch (t: Throwable) {
                    Logger.d("error adding notification action : " + t.localizedMessage)
                }
            }
        }
        return actionButtons
    }

    private fun createActionButtonPendingIntent(
        context: Context,
        action: JSONObject,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        try {
            // Extract necessary properties from the action
            val dl = action.optString("dl")
            val id = action.optString("id")
            val autoCancel = action.optBoolean("ac", true)

            // Determine if the intent should be sent to CTIntentService
            val intentServiceName = ManifestInfo.getInstance(context).intentServiceName
            var clazz: Class<*>? = null
            if (intentServiceName != null) {
                try {
                    clazz = Class.forName(intentServiceName)
                } catch (e: ClassNotFoundException) {
                    try {
                        clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
                    } catch (ex: ClassNotFoundException) {
                        Logger.d("No Intent Service found")
                    }
                }
            } else {
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
                } catch (ex: ClassNotFoundException) {
                    Logger.d("No Intent Service found")
                }
            }
            val isCTIntentServiceAvailable = com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz)

            var sendToCTIntentService = (VERSION.SDK_INT < VERSION_CODES.S && autoCancel
                    && isCTIntentServiceAvailable)
            val dismissOnClick = extras.getString("pt_dismiss_on_click")

            /**
             * Send to CTIntentService in case (OS >= S) and notif is for Push templates with remind action
             */
            if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                && id.contains("remind") && dismissOnClick != null &&
                dismissOnClick.equals("true", ignoreCase = true) && autoCancel &&
                isCTIntentServiceAvailable
            ) {
                sendToCTIntentService = true
            }

            /**
             * Send to CTIntentService in case (OS >= S) and notif is for Push templates with pt_dismiss_on_click
             * true
             */
            if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                && dismissOnClick != null && dismissOnClick.equals("true", ignoreCase = true)
                && autoCancel && isCTIntentServiceAvailable
            ) {
                sendToCTIntentService = true
            }

            // Create the appropriate intent
            var actionLaunchIntent: Intent? = null

            if (sendToCTIntentService) {
                actionLaunchIntent = Intent(CTNotificationIntentService.MAIN_ACTION)
                actionLaunchIntent.setPackage(context.packageName)
                actionLaunchIntent.putExtra(
                    Constants.KEY_CT_TYPE,
                    CTNotificationIntentService.TYPE_BUTTON_CLICK
                )
                if (dl.isNotEmpty()) {
                    actionLaunchIntent.putExtra("dl", dl)
                }
            } else {
                if (dl.isNotEmpty()) {
                    actionLaunchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(dl))
                    Utils.setPackageNameFromResolveInfoList(context, actionLaunchIntent)
                } else {
                    actionLaunchIntent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                }
            }

            // Configure intent extras
            if (actionLaunchIntent != null) {
                actionLaunchIntent.putExtras(extras)
                actionLaunchIntent.removeExtra(Constants.WZRK_ACTIONS)
                actionLaunchIntent.putExtra("actionId", id)
                actionLaunchIntent.putExtra("autoCancel", autoCancel)
                actionLaunchIntent.putExtra("wzrk_c2a", id)
                actionLaunchIntent.putExtra("notificationId", notificationId)
                actionLaunchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            } else {
                return null
            }

            // Create and return the PendingIntent
            val requestCode = Random().nextInt()
            var flagsActionLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                flagsActionLaunchPendingIntent =
                    flagsActionLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE
            }

            return if (sendToCTIntentService) {
                PendingIntent.getService(
                    context, requestCode,
                    actionLaunchIntent, flagsActionLaunchPendingIntent
                )
            } else {
                PendingIntent.getActivity(
                    context, requestCode,
                    actionLaunchIntent, flagsActionLaunchPendingIntent, null
                )
            }
        } catch (t: Throwable) {
            Logger.d("error creating pending intent for action button: " + t.localizedMessage)
            return null
        }
    }

    companion object {
        /**
         * Returns the log level set for PushTemplates
         *
         * @return The int value
         */
        /**
         * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
         * Debug messages are tagged as PTLog.
         *
         * @param level Can be one of the following:  -1 (disables all debugging), 0 (default, shows minimal SDK integration related logging),
         * 2(shows debug output)
         */
        @JvmStatic
        var debugLevel = LogLevel.INFO.intValue()
    }
}