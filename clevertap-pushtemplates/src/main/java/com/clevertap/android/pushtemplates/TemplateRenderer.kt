package com.clevertap.android.pushtemplates

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat.Builder
import com.clevertap.android.pushtemplates.PTConstants.*
import com.clevertap.android.pushtemplates.TemplateDataFactory.getActions
import com.clevertap.android.pushtemplates.TemplateDataFactory.toBasicTemplateData
import com.clevertap.android.pushtemplates.content.FiveIconBigContentView
import com.clevertap.android.pushtemplates.content.FiveIconSmallContentView
import com.clevertap.android.pushtemplates.handlers.CancelTemplateHandler
import com.clevertap.android.pushtemplates.handlers.TimerTemplateHandler
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import com.clevertap.android.pushtemplates.media.TemplateRepository
import com.clevertap.android.pushtemplates.styles.*
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.NOTIF_MSG
import com.clevertap.android.sdk.Constants.NOTIF_TITLE
import com.clevertap.android.sdk.Constants.WZRK_COLOR
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.interfaces.AudibleNotification
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TemplateRenderer(context: Context, extras: Bundle, internal val config: CleverTapInstanceConfig? = null) : INotificationRenderer, AudibleNotification {
    internal val templateMediaManager: TemplateMediaManager by lazy {
        TemplateMediaManager(templateRepository = TemplateRepository(context, config))
    }
    internal var smallIcon = 0
    internal var pt_small_icon_clr: String? = null

    internal var actionButtons = emptyList<ActionButton>()
    internal var actionButtonPendingIntents = mutableMapOf<String, PendingIntent>()
    internal var notificationId: Int = -1//Creates a instance field for access in ContentViews->PendingIntentFactory

    enum class LogLevel(private val value: Int) {
        OFF(-1), INFO(0), DEBUG(2), VERBOSE(3);

        fun intValue(): Int {
            return value
        }
    }

    init {
        pt_small_icon_clr =
            Utils.getDarkModeAdaptiveColor(extras, Utils.isDarkMode(context), PT_SMALL_ICON_COLOUR)
                ?: extras.getString(WZRK_COLOR)
    }

    override fun getMessage(extras: Bundle): String? {
        return extras.getString(PT_MSG).takeUnless { it.isNullOrEmpty() } ?: extras.getString(NOTIF_MSG)
    }

    override fun getTitle(extras: Bundle, context: Context): String? {
        return extras.getString(PT_TITLE).takeUnless { it.isNullOrEmpty() } ?: extras.getString(NOTIF_TITLE)
    }


    override fun renderNotification(
        extras: Bundle, context: Context, nb: Builder,
        config: CleverTapInstanceConfig,
        notificationId: Int
    ): Builder? {
        this.notificationId = notificationId

        val id = extras.getString(PT_ID)
        if (id == null) {
            PTLog.verbose("Template ID not provided. Cannot create the notification")
            return null
        }
        val templateType = TemplateType.fromString(id)
        val altTextDefault = context.getString(R.string.pt_big_image_alt)

        val templateData = TemplateDataFactory.createTemplateData(
            templateType,
            extras,
            Utils.isDarkMode(context),
            altTextDefault
        ) { Utils.getNotificationIds(context) }

        this.actionButtons = getActionButtons(context, extras, notificationId, templateData?.getActions())

        when (templateData) {
            is BasicTemplateData -> {
                if (ValidatorFactory.getValidator(templateData)?.validate() == true) {
                    return BasicStyle(templateData, this).builderFromStyle(context, extras, notificationId, nb)
                }
            }

            is AutoCarouselTemplateData ->
                if (ValidatorFactory.getValidator(templateData)?.validate() == true)
                    return AutoCarouselStyle(templateData, this).builderFromStyle(context, extras, notificationId, nb)

            is ManualCarouselTemplateData ->
                if (ValidatorFactory.getValidator(templateData)?.validate() == true)
                    return ManualCarouselStyle(templateData, this , extras).builderFromStyle(context, extras, notificationId, nb)

            is RatingTemplateData ->
                if (ValidatorFactory.getValidator(templateData)?.validate() == true)
                    return RatingStyle(templateData, this, extras).builderFromStyle(context, extras, notificationId, nb)

            is FiveIconsTemplateData ->
                if (ValidatorFactory.getValidator(templateData)?.validate() == true) {
                    val fiveIconStyle  = FiveIconStyle(templateData, this, extras)
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

            is ProductTemplateData -> if (ValidatorFactory.getValidator(templateData)
                    ?.validate() == true
            )
                return ProductDisplayStyle(templateData, this, extras).builderFromStyle(context, extras, notificationId, nb)

            is ZeroBezelTemplateData ->
                if (ValidatorFactory.getValidator(templateData)?.validate() == true)
                    return ZeroBezelStyle(templateData, this).builderFromStyle(context, extras, notificationId, nb)

            is TimerTemplateData -> if (VERSION.SDK_INT >= VERSION_CODES.O) {
                ValidatorFactory.getValidator(templateData)
                    ?.takeIf { it.validate() }
                    ?.let {
                        TimerTemplateHandler.getTimerEnd(templateData)?.let { timerEnd ->
                            if (templateData.renderTerminal) {
                                TimerTemplateHandler.scheduleTimer(
                                    context,
                                    extras,
                                    notificationId,
                                    timerEnd,
                                    templateData,
                                    config
                                )
                            }
                            return TimerStyle(templateData, this)
                                .builderFromStyle(context, extras, notificationId, nb)
                                .setTimeoutAfter(timerEnd.toLong())
                        }
                    }
            }
            else {

                val basicTemplateData = templateData.toBasicTemplateData()

                PTLog.debug("Push Templates SDK supports Timer Notifications only on or above Android Oreo, reverting to basic template")
                if (ValidatorFactory.getValidator(basicTemplateData)?.validate() == true) {
                    return BasicStyle(basicTemplateData, this).builderFromStyle(context, extras, notificationId, nb)
                }
            }

            is InputBoxTemplateData -> if (ValidatorFactory.getValidator(templateData)
                    ?.validate() == true
            )
                return InputBoxStyle(templateData, this).builderFromStyle(context, extras, notificationId, nb)

            is CancelTemplateData -> CancelTemplateHandler.renderCancelNotification(context, templateData)
            else -> {
                PTLog.verbose("operation not defined!")
            }
        }
        templateMediaManager.clearCaches()
        return null
    }

    override fun setSmallIcon(smallIcon: Int, context: Context) {
        this.smallIcon = smallIcon
        try {
            Utils.setBitMapColour(context, smallIcon, pt_small_icon_clr, PT_META_CLR_DEFAULTS)
        } catch (_: NullPointerException) {
            PTLog.debug("NPE while setting small icon color")
        }
    }

    override fun getActionButtonIconKey(): String {
        return PT_NOTIF_ICON
    }

    override fun getCollapseKey(extras: Bundle): Any? {
        return extras[PT_COLLAPSE_KEY] ?: extras[Constants.WZRK_COLLAPSE]
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
                } catch (_: ClassNotFoundException) {
                    try {
                        clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
                    } catch (_: ClassNotFoundException) {
                        Logger.d("No Intent Service found")
                    }
                }
            } else {
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
                } catch (_: ClassNotFoundException) {
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
         * @param debugLevel Can be one of the following:  -1 (disables all debugging), 0 (default, shows minimal SDK integration related logging),
         * 2(shows debug output)
         */
        @JvmStatic
        var debugLevel = LogLevel.INFO.intValue()
    }
}