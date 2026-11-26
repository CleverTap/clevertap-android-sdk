package com.clevertap.android.pushtemplates

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_DISMISS_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PRODUCT_DISPLAY_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.media.GifDecoderImpl
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import com.clevertap.android.pushtemplates.media.TemplateRepository
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService.Companion.TYPE_BUTTON_CLICK
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory
import com.clevertap.android.sdk.task.CTExecutorFactory
import androidx.annotation.RestrictTo

/**
 * Handles the clicks on interactive push templates (rating, product display, input box, manual
 * carousel, five icons, vertical image) and the dismissal of template notifications.
 */
@Suppress("DEPRECATION")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PushTemplateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Utils.createSilentNotificationChannel(context)

        val extras = intent.extras ?: return
        val cleverTapAPI = CleverTapAPI.getGlobalInstance(context, extras.getString(Constants.WZRK_ACCT_ID_KEY))
        val click = TemplateClick(context, intent, extras)

        if (cleverTapAPI == null) {
            PTLog.verbose("clevertap instance is null, not running PushTemplateReceiver#renderNotification")
            return
        }
        try {
            val config = cleverTapAPI.getInstanceConfig()
            val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
            task.execute("PushTemplateReceiver#renderNotification") {
                try {
                    click.handle(config)
                } catch (t: Throwable) {
                    PTLog.verbose("Couldn't render notification: ${t.localizedMessage}")
                }
                null
            }
        } catch (e: Exception) {
            PTLog.verbose("Couldn't render notification: ${e.localizedMessage}")
        }
    }

    /** One broadcast's payload, read on the receiving thread, and the template handlers acting on it. */
    private class TemplateClick(
        private val context: Context,
        private val intent: Intent,
        private val extras: Bundle
    ) {

        private val templateType: TemplateType? =
            intent.getStringExtra(PTConstants.PT_ID)?.let { TemplateType.fromString(it) }

        private val title = extras.stringOrFallback(PTConstants.PT_TITLE, Constants.NOTIF_TITLE)

        private val ratingDefaultDl = extras.stringOrFallback(PTConstants.PT_DEFAULT_DL, Constants.DEEP_LINK_KEY)

        private val deepLinkList: List<String> = Utils.getDeepLinkListFromExtras(extras)

        private val productDisplayLinear = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_LINEAR)

        private val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        private val bigImgAlt = extras.getString(PTConstants.PT_BIG_IMG_ALT)

        private val bigImgAltAltText = extras.getString(
            PTConstants.PT_BIG_IMG_ALT_ALT_TEXT,
            context.getString(R.string.pt_big_image_alt)
        )

        private val isDismissIntent = extras.getBoolean(PTConstants.PT_DISMISS_INTENT, false)

        private val ratingToast = extras.getString(PTConstants.PT_RATING_TOAST)

        private val subtitle = extras.stringOrFallback(PTConstants.PT_SUBTITLE, Constants.WZRK_SUBTITLE)

        private val metaColor = extras.getString(Constants.WZRK_COLOR)

        fun handle(instanceConfig: CleverTapInstanceConfig) {
            if (isDismissIntent) {
                Utils.deleteSilentNotificationChannel(context)
                Utils.deleteImageFromStorage(context, intent)
                return
            }
            when (templateType) {
                TemplateType.RATING -> handleRatingNotification(instanceConfig)
                TemplateType.FIVE_ICONS, TemplateType.ICONS -> handleFiveCTANotification(instanceConfig)
                TemplateType.PRODUCT_DISPLAY -> handleProductDisplayNotification()
                TemplateType.INPUT_BOX -> handleInputBoxNotification()
                TemplateType.MANUAL_CAROUSEL -> handleManualCarouselNotification()
                TemplateType.VERTICAL_IMAGE -> handleVerticalImageButtonClick()
                else -> {}
            }
        }

        /** The system always has one; Java dereferenced it unchecked (the handlers' catch blocks log a failure). */
        private fun requireNotificationManager(): NotificationManager =
            checkNotNull(notificationManager) { "No NotificationManager" }

        private fun dismissPendingIntent(): PendingIntent =
            PendingIntentFactory.setDismissIntent(context, extras, Intent(context, PushTemplateReceiver::class.java))

        private fun handleManualCarouselNotification() {
            try {
                /**
                 * Requires to recreate remote views to update notification
                 * but recreating remote views creates performance issue and notification lag,
                 * hence using below method to get remote views from existing notification
                 */
                val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
                val notification = Utils.getNotificationById(context, notificationId)
                if (notification == null) {
                    PTLog.verbose("Manual Carousel Notification is null, returning")
                    return
                }
                val contentViewManualCarousel = checkNotNull(notification.bigContentView)
                val contentViewSmall = notification.contentView

                applyBasicKeys(contentViewManualCarousel)

                val rightSwipe = extras.getBoolean(PTConstants.PT_RIGHT_SWIPE)

                val imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST)
                val deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST)

                if (imageList.isNullOrEmpty()) {
                    PTLog.verbose("Manual Carousel image list is null or empty, skipping swipe")
                    return
                }

                val currPosition = extras.getInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT)
                val newPosition = if (rightSwipe) {
                    contentViewManualCarousel.showNext(R.id.carousel_image)
                    contentViewManualCarousel.showNext(R.id.carousel_image_right)
                    contentViewManualCarousel.showNext(R.id.carousel_image_left)
                    if (currPosition == imageList.size - 1) { 0 } else { currPosition + 1 }
                } else {
                    contentViewManualCarousel.showPrevious(R.id.carousel_image)
                    contentViewManualCarousel.showPrevious(R.id.carousel_image_right)
                    contentViewManualCarousel.showPrevious(R.id.carousel_image_left)
                    if (currPosition == 0) { imageList.size - 1 } else { currPosition - 1 }
                }
                val dl: String? = when {
                    deepLinkList.isNullOrEmpty() -> ""
                    deepLinkList.size == imageList.size -> deepLinkList[newPosition]
                    deepLinkList.size == 1 -> deepLinkList[0]
                    newPosition < deepLinkList.size -> deepLinkList[newPosition]
                    else -> deepLinkList[0]
                }

                extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, newPosition)
                extras.remove(PTConstants.PT_RIGHT_SWIPE)
                extras.putString(Constants.DEEP_LINK_KEY, dl)
                extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition)

                contentViewManualCarousel.setOnClickPendingIntent(
                    R.id.rightArrowPos0,
                    PendingIntentFactory.getPendingIntent(
                        context, notificationId, extras, false,
                        MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT
                    )
                )

                contentViewManualCarousel.setOnClickPendingIntent(
                    R.id.leftArrowPos0,
                    PendingIntentFactory.getPendingIntent(
                        context, notificationId, extras, false,
                        MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT
                    )
                )

                val pIntent = PendingIntentFactory.getPendingIntent(
                    context, notificationId, extras, true,
                    MANUAL_CAROUSEL_CONTENT_PENDING_INTENT
                )

                val notificationBuilder = NotificationCompat.Builder(context, notification)

                val dIntent = PendingIntentFactory.getPendingIntent(
                    context, notificationId, extras, false,
                    MANUAL_CAROUSEL_DISMISS_PENDING_INTENT, null
                )

                applyBuilderBasics(
                    notificationBuilder, smallIcon(), contentViewSmall, contentViewManualCarousel, pIntent, dIntent
                )

                requireNotificationManager().notify(notificationId, notificationBuilder.build())
            } catch (t: Throwable) {
                PTLog.verbose("Error creating manual carousel notification ", t)
            }
        }

        private fun handleInputBoxNotification() {
            //Fetch Remote Input
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            val dIntent = dismissPendingIntent()
            val config: CleverTapInstanceConfig? = extras.getParcelable("config")

            remoteInput ?: return
            //Fetch Reply
            val reply = remoteInput.getCharSequence(PTConstants.PT_INPUT_KEY)
            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)

            if (reply == null) {
                PTLog.verbose("PushTemplateReceiver: Input is Empty")
                return
            }
            PTLog.verbose("Processing Input from Input Template")
            extras.putString(PTConstants.PT_INPUT_KEY, reply.toString())
            Utils.raiseCleverTapEvent(context, config, extras, PTConstants.PT_INPUT_KEY)
            //Update the notification to show that the reply was received.
            val repliedNotification = if (VERSION.SDK_INT >= VERSION_CODES.O) {
                NotificationCompat.Builder(context, PTConstants.PT_SILENT_CHANNEL_ID)
            } else {
                NotificationCompat.Builder(context)
            }
            val smallIcon = smallIcon()

            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                repliedNotification.setSubText(subtitle)
            }

            repliedNotification.setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                .setTimeoutAfter(PTConstants.PT_INPUT_TIMEOUT.toLong())
                .setDeleteIntent(dIntent)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)

            applyBigImageStyle(bigImgAlt, repliedNotification, bigImgAltAltText, config)

            requireNotificationManager().notify(notificationId, repliedNotification.build())

            /* Check if Auto Open key is present and not empty, if not present then show feedback and
            auto kill in 3 secs. If present, then launch the App with Dl or Launcher activity.
            The launcher activity will get the reply in extras under the key "pt_reply" */
            if (VERSION.SDK_INT >= VERSION_CODES.S || (extras.getString(PTConstants.PT_INPUT_AUTO_OPEN) == null &&
                        !extras.getBoolean(PTConstants.PT_INPUT_AUTO_OPEN))
            ) {
                return
            }
            //adding delay for launcher
            try {
                Thread.sleep(PTConstants.PT_INPUT_TIMEOUT.toLong())
            } catch (e: InterruptedException) {
                PTLog.verbose("Thread interrupted while waiting for input timeout", e)
            }

            val launchIntent = if (extras.containsKey(Constants.DEEP_LINK_KEY) &&
                extras.getString(Constants.DEEP_LINK_KEY) != null
            ) {
                Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra(Constants.DEEP_LINK_KEY))).also {
                    Utils.setPackageNameFromResolveInfoList(context, it)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            }

            launchIntent.apply {
                putExtras(this@TemplateClick.extras)
                //adding reply to extra
                putExtra("pt_reply", reply)
                removeExtra(Constants.WZRK_ACTIONS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            context.startActivity(launchIntent)
        }

        private fun handleRatingNotification(instanceConfig: CleverTapInstanceConfig) {
            try {
                val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)

                if (extras.getBoolean(PTConstants.DEFAULT_DL, false)) {
                    openRatingDefaultDeepLink(notificationId)
                    return
                }

                val clickedStar = extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)
                var ptDlClicked: String? = deepLinkList[0]
                if (clickedStar in 1..STAR_IDS.size) {
                    extras.putString(Constants.KEY_C2A, "${PTConstants.PT_RATING_C2A_KEY}$clickedStar")
                    ptDlClicked = if (deepLinkList.size > clickedStar - 1) {
                        deepLinkList[clickedStar - 1]
                    } else {
                        deepLinkList[0]
                    }
                }

                val notification = Utils.getNotificationById(context, notificationId)
                if (notification == null) {
                    PTLog.verbose("Rating Notification is null, returning")
                    return
                }
                val contentViewRating = checkNotNull(notification.bigContentView)
                val contentViewSmall = notification.contentView

                // The clicked star fills itself and the ones before it; every other star is outlined.
                STAR_IDS.forEachIndexed { index, starId ->
                    val star = index + 1
                    if (clickedStar == star) {
                        STAR_IDS.take(star).forEach { contentViewRating.setImageViewResource(it, R.drawable.pt_star_filled) }
                    } else {
                        contentViewRating.setImageViewResource(starId, R.drawable.pt_star_outline)
                    }
                }
                cancelRatingClickIntents()
                extras.putString(Constants.DEEP_LINK_KEY, ptDlClicked)
                contentViewRating.setOnClickPendingIntent(
                    R.id.tVRatingConfirmation,
                    LaunchPendingIntentFactory.getActivityIntent(extras, context)
                )

                val smallIcon = smallIcon()

                val notificationBuilder = NotificationCompat.Builder(context, notification)
                val dIntent = dismissPendingIntent()

                notificationManager?.let { notificationManager ->
                    //Use the Builder to build notification
                    notificationBuilder.setSmallIcon(smallIcon)
                        .setCustomContentView(contentViewSmall)
                        .setCustomBigContentView(contentViewRating)
                        .setContentTitle(title)
                        .setDeleteIntent(dIntent)
                        .setAutoCancel(true)

                    notificationManager.notify(notificationId, notificationBuilder.build())
                }

                Utils.raiseCleverTapEvent(
                    context, instanceConfig, "Rating Submitted",
                    Utils.convertRatingBundleObjectToHashMap(extras)
                )
                if (VERSION.SDK_INT < VERSION_CODES.S) {
                    handleRatingDeepLink(notificationId, ptDlClicked, instanceConfig)
                }
            } catch (t: Throwable) {
                PTLog.verbose("Error creating rating notification ", t)
            }
        }

        private fun openRatingDefaultDeepLink(notificationId: Int) {
            val config: CleverTapInstanceConfig? = extras.getParcelable("config")
            requireNotificationManager().cancel(notificationId)
            val clazz: Class<*>? = try {
                Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
            } catch (ex: ClassNotFoundException) {
                PTLog.debug("No Intent Service found")
                null
            }

            if (com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz)) {
                val launchIntent = Intent(CTNotificationIntentService.MAIN_ACTION).apply {
                    setPackage(context.packageName)
                    putExtra(Constants.KEY_CT_TYPE, TYPE_BUTTON_CLICK)
                    putExtras(this@TemplateClick.extras)
                    putExtra("dl", ratingDefaultDl)
                }
                context.startService(launchIntent)
            } else {
                val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ratingDefaultDl)).apply {
                    removeExtra(Constants.WZRK_ACTIONS)
                    putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                }
                Utils.raiseNotificationClicked(context, extras, config)
                launchIntent.putExtras(extras)
                launchIntent.putExtra(Constants.DEEP_LINK_KEY, ratingDefaultDl)
                context.startActivity(launchIntent)
            }
        }

        /**
         * This method cancels all pending intents fired on click of rating. Allows the user to
         * click only once for Android 11+ devices and prevents collisions for future rating push-templates
         */
        private fun cancelRatingClickIntents() {
            val flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            // A rating click always carries its request codes; Java dereferenced them unchecked.
            val requestCodes = checkNotNull(intent.getIntArrayExtra(PTConstants.KEY_REQUEST_CODES))

            for (requestCode in requestCodes) {
                PendingIntent.getBroadcast(context, requestCode, intent, flagsLaunchPendingIntent).cancel()
            }
        }

        @SuppressLint("MissingPermission")
        private fun handleRatingDeepLink(
            notificationId: Int,
            ptDlClicked: String?,
            config: CleverTapInstanceConfig?
        ) {
            Thread.sleep(PTConstants.ONE_SECOND_LONG)
            requireNotificationManager().cancel(notificationId)

            if (!ratingToast.isNullOrEmpty()) {
                Utils.showToast(context, ratingToast, config)
            }

            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

            val launchIntent = if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                Intent(Intent.ACTION_VIEW, Uri.parse(extras.getString(Constants.DEEP_LINK_KEY))).also {
                    com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, it)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            }
            launchIntent.apply {
                putExtras(this@TemplateClick.extras)
                putExtra(Constants.DEEP_LINK_KEY, ptDlClicked)
                removeExtra(Constants.WZRK_ACTIONS)
                putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(launchIntent)
        }

        @SuppressLint("MissingPermission")
        private fun handleVerticalImageButtonClick() {
            CleverTapAPI.handleNotificationClicked(context, extras)

            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID, -1)
            if (notificationId > -1) {
                requireNotificationManager().cancel(notificationId)
            }

            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

            val deepLink = extras.getString(Constants.DEEP_LINK_KEY)
            val launchIntent = if (!deepLink.isNullOrEmpty()) {
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).also {
                    com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, it)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            }
            launchIntent.apply {
                putExtras(this@TemplateClick.extras)
                removeExtra(Constants.WZRK_ACTIONS)
                putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(launchIntent)
        }

        private fun handleProductDisplayNotification() {
            try {
                val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
                val notification = Utils.getNotificationById(context, notificationId)
                if (notification == null) {
                    PTLog.verbose("Product Display Notification is null, returning")
                    return
                }
                // Java dereferenced the views of the notification on screen unchecked as well.
                val contentViewBig = checkNotNull(notification.bigContentView)
                val contentViewSmall = notification.contentView

                val isLinear = !productDisplayLinear.isNullOrEmpty()

                applyBasicKeys(contentViewBig)
                if (!isLinear) {
                    applyBasicKeys(checkNotNull(contentViewSmall))
                }

                val currentPosition = extras.getInt(PTConstants.PT_CURRENT_POSITION)

                contentViewBig.setDisplayedChild(R.id.carousel_image, currentPosition)
                val deepLinkList = checkNotNull(extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST))
                val bigTextList = extras.getStringArrayList(PTConstants.PT_BIGTEXT_LIST)
                val smallTextList = extras.getStringArrayList(PTConstants.PT_SMALLTEXT_LIST)
                val priceList = extras.getStringArrayList(PTConstants.PT_PRICE_LIST)

                val dl = deepLinkList[currentPosition]
                contentViewBig.setTextViewText(
                    if (isLinear) { R.id.product_name } else { R.id.title },
                    checkNotNull(bigTextList)[currentPosition]
                )
                contentViewBig.setTextViewText(R.id.msg, checkNotNull(smallTextList)[currentPosition])
                contentViewBig.setTextViewText(R.id.product_price, checkNotNull(priceList)[currentPosition])
                extras.remove(PTConstants.PT_CURRENT_POSITION)

                val bundleBuyNow = (extras.clone() as Bundle).apply {
                    putBoolean(PTConstants.PT_IMAGE_1, true)
                    putInt(PTConstants.PT_NOTIF_ID, notificationId)
                    putString(PTConstants.PT_BUY_NOW_DL, dl)
                    putBoolean(PTConstants.PT_BUY_NOW, true)
                }

                contentViewBig.setOnClickPendingIntent(
                    R.id.product_action,
                    PendingIntentFactory.getCtaLaunchPendingIntent(context, bundleBuyNow, dl, notificationId)
                )

                val notificationBuilder = NotificationCompat.Builder(context, notification)

                val bundleLaunchIntent = (extras.clone() as Bundle).apply { putString(Constants.DEEP_LINK_KEY, dl) }
                val pIntent: PendingIntent? = PendingIntentFactory.getPendingIntent(
                    context, notificationId, bundleLaunchIntent, true,
                    PRODUCT_DISPLAY_CONTENT_PENDING_INTENT, null
                )

                notificationManager?.let { notificationManager ->
                    //Use the Builder to build notification
                    val dIntent = dismissPendingIntent()
                    applyBuilderBasics(
                        notificationBuilder, smallIcon(), contentViewSmall, contentViewBig, pIntent, dIntent
                    )

                    notificationManager.notify(notificationId, notificationBuilder.build())
                }
            } catch (t: Throwable) {
                PTLog.verbose("Error creating product display notification ", t)
            }
        }

        private fun handleFiveCTANotification(instanceConfig: CleverTapInstanceConfig) {
            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
            extras.putString(Constants.DEEP_LINK_KEY, null)

            if (extras.getBoolean("close")) {
                extras.putString(Constants.KEY_C2A, "${PTConstants.PT_5CTA_C2A_KEY}close")
                requireNotificationManager().cancel(notificationId)
            }
            Utils.raiseNotificationClicked(context, extras, instanceConfig)
        }

        private fun applyBuilderBasics(
            notificationBuilder: NotificationCompat.Builder,
            smallIcon: Int,
            contentViewSmall: RemoteViews?,
            contentViewBig: RemoteViews?,
            pIntent: PendingIntent?,
            dIntent: PendingIntent?
        ) {
            notificationBuilder.setSmallIcon(smallIcon)
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setContentTitle(Html.fromHtml(title))
                .setDeleteIntent(dIntent)
                .setContentIntent(pIntent).setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
        }

        private fun applyBasicKeys(contentView: RemoteViews) {
            contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
            contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context, System.currentTimeMillis()))
            if (!subtitle.isNullOrEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    contentView.setTextViewText(R.id.subtitle, Html.fromHtml(subtitle, Html.FROM_HTML_MODE_LEGACY))
                } else {
                    contentView.setTextViewText(R.id.subtitle, Html.fromHtml(subtitle))
                }
            } else {
                contentView.setViewVisibility(R.id.subtitle, View.GONE)
                contentView.setViewVisibility(R.id.sep_subtitle, View.GONE)
            }
            if (!metaColor.isNullOrEmpty()) {
                contentView.setTextColor(R.id.app_name, Utils.getColour(metaColor, PTConstants.PT_META_CLR_DEFAULTS))
                contentView.setTextColor(R.id.timestamp, Utils.getColour(metaColor, PTConstants.PT_META_CLR_DEFAULTS))
                contentView.setTextColor(R.id.subtitle, Utils.getColour(metaColor, PTConstants.PT_META_CLR_DEFAULTS))
            }
        }

        private fun applyBigImageStyle(
            imgUrl: String?,
            notificationBuilder: NotificationCompat.Builder,
            altText: String?,
            config: CleverTapInstanceConfig?
        ) {
            if (imgUrl != null && imgUrl.startsWith("http")) {
                try {
                    val templateMediaManager =
                        TemplateMediaManager(TemplateRepository(context, config), GifDecoderImpl())
                    val bpMap = templateMediaManager.getNotificationBitmap(imgUrl, false, context)
                        ?: throw Exception("Failed to fetch big picture!")

                    val bigPictureStyle = NotificationCompat.BigPictureStyle()
                        .setSummaryText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                        .bigPicture(bpMap)

                    if (VERSION.SDK_INT >= VERSION_CODES.S) {
                        bigPictureStyle.setContentDescription(altText)
                    }

                    notificationBuilder.setStyle(bigPictureStyle)
                    return
                } catch (t: Throwable) {
                    PTLog.verbose("Falling back to big text notification, couldn't fetch big picture", t)
                }
            }

            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle().bigText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
            )
        }

        /** The manifest's notification icon, or the app icon. */
        private fun smallIcon(): Int = try {
            val metaData = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
            val iconName = Utils.getManifestStringValueForKey(metaData, ManifestInfo.LABEL_NOTIFICATION_ICON)
                ?: throw IllegalArgumentException()
            context.resources.getIdentifier(iconName, "drawable", context.packageName)
                .takeIf { it != 0 }
                ?: throw IllegalArgumentException()
        } catch (t: Throwable) {
            Utils.getAppIconAsIntId(context)
        }
    }

    private companion object {

        private val STAR_IDS = intArrayOf(R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5)

        /** The value under [key], or the one under [fallbackKey] when it is missing or empty. */
        private fun Bundle.stringOrFallback(key: String, fallbackKey: String): String? =
            getString(key).takeUnless { it.isNullOrEmpty() } ?: getString(fallbackKey)
    }
}
