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
import android.graphics.Bitmap
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

@Suppress("DEPRECATION")
internal class PushTemplateReceiver : BroadcastReceiver() {

    var clicked1: Boolean = true
    var clicked2: Boolean = true
    var clicked3: Boolean = true
    var clicked4: Boolean = true
    var clicked5: Boolean = true
    var close: Boolean = true

    private var cleverTapAPI: CleverTapAPI? = null

    private var contentViewBig: RemoteViews? = null
    private var contentViewSmall: RemoteViews? = null
    private var contentViewRating: RemoteViews? = null
    private var contentViewManualCarousel: RemoteViews? = null

    private var pt_id: String? = null

    private var templateType: TemplateType? = null

    private var pt_title: String? = null

    private var pt_msg: String? = null

    private var pt_msg_summary: String? = null

    private var pt_rating_default_dl: String? = null

    private var imageList: ArrayList<String>? = ArrayList()

    private var deepLinkList: ArrayList<String>? = ArrayList()

    private var bigTextList: ArrayList<String>? = ArrayList()

    private var smallTextList: ArrayList<String>? = ArrayList()

    private var priceList: ArrayList<String>? = ArrayList()

    private var smallIcon = 0

    private var requiresChannelId = false

    private var notificationManager: NotificationManager? = null

    private var pt_product_display_linear: String? = null

    private var pt_big_img_alt: String? = null

    private var pt_big_img_alt_alt_text: String? = null

    private var pt_small_icon_clr: String? = null

    private var pt_big_img: String? = null

    private var pt_meta_clr: String? = null

    private var pt_dismiss_intent = false

    private var pt_rating_toast: String? = null

    private var pt_subtitle: String? = null

    private var config: CleverTapInstanceConfig? = null

    override fun onReceive(context: Context, intent: Intent) {
        Utils.createSilentNotificationChannel(context)

        if (intent.extras != null) {
            val extras = intent.extras!!
            cleverTapAPI = CleverTapAPI
                .getGlobalInstance(context, extras.getString(Constants.WZRK_ACCT_ID_KEY))
            pt_id = intent.getStringExtra(PTConstants.PT_ID)
            pt_msg = extras.getString(PTConstants.PT_MSG)
            pt_msg_summary = extras.getString(PTConstants.PT_MSG_SUMMARY)
            pt_title = extras.getString(PTConstants.PT_TITLE)
            pt_rating_default_dl = extras.getString(PTConstants.PT_DEFAULT_DL)
            deepLinkList = Utils.getDeepLinkListFromExtras(extras)
            bigTextList = Utils.getBigTextFromExtras(extras)
            smallTextList = Utils.getSmallTextFromExtras(extras)
            priceList = Utils.getPriceFromExtras(extras)
            pt_product_display_linear = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_LINEAR)
            notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            pt_big_img_alt = extras.getString(PTConstants.PT_BIG_IMG_ALT)
            pt_big_img_alt_alt_text = extras.getString(
                PTConstants.PT_BIG_IMG_ALT_ALT_TEXT,
                context.getString(R.string.pt_big_image_alt)
            )
            pt_small_icon_clr = extras.getString(PTConstants.PT_SMALL_ICON_COLOUR)
            requiresChannelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            pt_dismiss_intent = extras.getBoolean(PTConstants.PT_DISMISS_INTENT, false)
            pt_rating_toast = extras.getString(PTConstants.PT_RATING_TOAST)
            pt_subtitle = extras.getString(PTConstants.PT_SUBTITLE)
            setKeysFromDashboard(extras)

            if (pt_id != null) {
                templateType = TemplateType.fromString(pt_id)
            }

            if (cleverTapAPI != null) {
                try {
                    // TODO(SDK-6368): use cleverTapAPI.getInstanceConfig() once SDK-6372 lands
                    // (CoreState is Kotlin-internal in core; Java reached it through the @RestrictTo getter.)
                    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
                    val ctConfig = cleverTapAPI!!.coreState.config
                    this.config = ctConfig
                    val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
                    task.execute("PushTemplateReceiver#renderNotification") {
                        try {
                            if (pt_dismiss_intent) {
                                Utils.deleteSilentNotificationChannel(context)
                                Utils.deleteImageFromStorage(context, intent)
                                return@execute null
                            }
                            if (templateType != null) {
                                when (templateType) {
                                    TemplateType.RATING -> handleRatingNotification(context, extras, intent)
                                    TemplateType.FIVE_ICONS, TemplateType.ICONS -> handleFiveCTANotification(
                                        context,
                                        extras
                                    )

                                    TemplateType.PRODUCT_DISPLAY -> handleProductDisplayNotification(context, extras)
                                    TemplateType.INPUT_BOX -> handleInputBoxNotification(context, extras, intent)
                                    TemplateType.MANUAL_CAROUSEL -> handleManualCarouselNotification(context, extras)
                                    TemplateType.VERTICAL_IMAGE -> handleVerticalImageButtonClick(context, extras)
                                    else -> {}
                                }
                            }
                        } catch (t: Throwable) {
                            PTLog.verbose("Couldn't render notification: " + t.localizedMessage)
                        }
                        null
                    }
                } catch (e: Exception) {
                    PTLog.verbose("Couldn't render notification: " + e.localizedMessage)
                }
            } else {
                PTLog.verbose("clevertap instance is null, not running PushTemplateReceiver#renderNotification")
            }
        }
    }

    private fun handleManualCarouselNotification(context: Context, extras: Bundle) {
        try {
            /**
             * Requires to recreate remote views to update notification
             * but recreating remote views creates performance issue and notification lag,
             * hence using below method to get remote views from existing notification
             */
            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
            var notification = Utils.getNotificationById(context, notificationId)
            if (notification == null) {
                PTLog.verbose("Manual Carousel Notification is null, returning")
                return
            }
            contentViewManualCarousel = notification.bigContentView
            contentViewSmall = notification.contentView

            setCustomContentViewBasicKeys(contentViewManualCarousel!!, context)

            val rightSwipe = extras.getBoolean(PTConstants.PT_RIGHT_SWIPE)

            imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST)
            deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST)

            val imageList = imageList
            if (imageList == null || imageList.isEmpty()) {
                PTLog.verbose("Manual Carousel image list is null or empty, skipping swipe")
                return
            }

            val currPosition = extras.getInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT)
            val newPosition: Int
            if (rightSwipe) {
                contentViewManualCarousel!!.showNext(R.id.carousel_image)
                contentViewManualCarousel!!.showNext(R.id.carousel_image_right)
                contentViewManualCarousel!!.showNext(R.id.carousel_image_left)
                newPosition = if (currPosition == imageList.size - 1) {
                    0
                } else {
                    currPosition + 1
                }
            } else {
                contentViewManualCarousel!!.showPrevious(R.id.carousel_image)
                contentViewManualCarousel!!.showPrevious(R.id.carousel_image_right)
                contentViewManualCarousel!!.showPrevious(R.id.carousel_image_left)
                newPosition = if (currPosition == 0) {
                    imageList.size - 1
                } else {
                    currPosition - 1
                }
            }
            var dl: String? = ""

            val deepLinkList = deepLinkList
            if (deepLinkList != null && !deepLinkList.isEmpty()) {
                dl = if (deepLinkList.size == imageList.size) {
                    deepLinkList[newPosition]
                } else if (deepLinkList.size == 1) {
                    deepLinkList[0]
                } else if (newPosition < deepLinkList.size) {
                    deepLinkList[newPosition]
                } else {
                    deepLinkList[0]
                }
            }

            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, newPosition)
            extras.remove(PTConstants.PT_RIGHT_SWIPE)
            extras.putString(Constants.DEEP_LINK_KEY, dl)
            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition)

            contentViewManualCarousel!!.setOnClickPendingIntent(
                R.id.rightArrowPos0, PendingIntentFactory
                    .getPendingIntent(
                        context, notificationId, extras, false,
                        MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT
                    )
            )

            contentViewManualCarousel!!.setOnClickPendingIntent(
                R.id.leftArrowPos0, PendingIntentFactory
                    .getPendingIntent(
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

            setSmallIcon(context)

            setNotificationBuilderBasics(
                notificationBuilder, contentViewSmall, contentViewManualCarousel,
                pt_title, pIntent, dIntent
            )

            notification = notificationBuilder.build()

            notificationManager!!.notify(notificationId, notification)
        } catch (t: Throwable) {
            PTLog.verbose("Error creating manual carousel notification ", t)
        }
    }

    private fun handleInputBoxNotification(context: Context, extras: Bundle, intent: Intent) {
        //Fetch Remote Input
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
        val dIntent: PendingIntent = PendingIntentFactory.setDismissIntent(context, extras, dismissIntent)
        config = extras.getParcelable("config")

        if (remoteInput != null) {
            //Fetch Reply
            val reply = remoteInput.getCharSequence(
                PTConstants.PT_INPUT_KEY
            )

            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)

            if (reply != null) {
                PTLog.verbose("Processing Input from Input Template")
                extras.putString(PTConstants.PT_INPUT_KEY, reply.toString())
                Utils.raiseCleverTapEvent(context, config, extras, PTConstants.PT_INPUT_KEY)
                //Update the notification to show that the reply was received.
                val repliedNotification: NotificationCompat.Builder = if (requiresChannelId) {
                    NotificationCompat.Builder(context, PTConstants.PT_SILENT_CHANNEL_ID)
                } else {
                    NotificationCompat.Builder(context)
                }
                setSmallIcon(context)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    repliedNotification.setSubText(pt_subtitle)
                }

                repliedNotification.setSmallIcon(smallIcon)
                    .setContentTitle(pt_title)
                    .setContentText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                    .setTimeoutAfter(PTConstants.PT_INPUT_TIMEOUT.toLong())
                    .setDeleteIntent(dIntent)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)

                setStandardViewBigImageStyle(
                    pt_big_img_alt,
                    extras,
                    context,
                    repliedNotification,
                    pt_big_img_alt_alt_text
                )

                val notification = repliedNotification.build()
                notificationManager!!.notify(notificationId, notification)

                /* Check if Auto Open key is present and not empty, if not present then show feedback and
                auto kill in 3 secs. If present, then launch the App with Dl or Launcher activity.
                The launcher activity will get the reply in extras under the key "pt_reply" */
                if (VERSION.SDK_INT < VERSION_CODES.S && (extras.getString(PTConstants.PT_INPUT_AUTO_OPEN) != null
                            || extras.getBoolean(PTConstants.PT_INPUT_AUTO_OPEN))
                ) {
                    //adding delay for launcher
                    try {
                        Thread.sleep(PTConstants.PT_INPUT_TIMEOUT.toLong())
                    } catch (e: InterruptedException) {
                        PTLog.verbose("Thread interrupted while waiting for input timeout", e)
                    }

                    val launchIntent: Intent

                    if (extras.containsKey(Constants.DEEP_LINK_KEY)
                        && extras.getString(Constants.DEEP_LINK_KEY) != null
                    ) {
                        launchIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(intent.getStringExtra(Constants.DEEP_LINK_KEY))
                        )
                        Utils.setPackageNameFromResolveInfoList(context, launchIntent)
                    } else {
                        launchIntent = context.packageManager
                            .getLaunchIntentForPackage(context.packageName) ?: return
                    }

                    launchIntent.putExtras(extras)

                    //adding reply to extra
                    launchIntent.putExtra("pt_reply", reply)

                    launchIntent.removeExtra(Constants.WZRK_ACTIONS)
                    launchIntent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )

                    context.startActivity(launchIntent)
                }
            } else {
                PTLog.verbose("PushTemplateReceiver: Input is Empty")
            }
        }
    }

    private fun handleRatingNotification(context: Context, extras: Bundle, intent: Intent) {
        try {
            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)

            if (extras.getBoolean(PTConstants.DEFAULT_DL, false)) {
                this.config = extras.getParcelable("config")
                notificationManager!!.cancel(notificationId)
                val launchIntent: Intent
                var clazz: Class<*>? = null
                try {
                    clazz = Class
                        .forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
                } catch (ex: ClassNotFoundException) {
                    PTLog.debug("No Intent Service found")
                }

                val isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils
                    .isServiceAvailable(context, clazz)
                if (isPTIntentServiceAvailable) {
                    launchIntent = Intent(CTNotificationIntentService.MAIN_ACTION)
                    launchIntent.setPackage(context.packageName)
                    launchIntent.putExtra(Constants.KEY_CT_TYPE, TYPE_BUTTON_CLICK)
                    launchIntent.putExtras(extras)
                    launchIntent.putExtra("dl", pt_rating_default_dl)
                    context.startService(launchIntent)
                } else {
                    launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pt_rating_default_dl))
                    launchIntent.removeExtra(Constants.WZRK_ACTIONS)
                    launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
                    launchIntent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                or Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                    Utils.raiseNotificationClicked(context, extras, this.config)
                    launchIntent.putExtras(extras)
                    launchIntent.putExtra(Constants.DEEP_LINK_KEY, pt_rating_default_dl)
                    context.startActivity(launchIntent)
                }

                return
            }

            val deepLinkList = deepLinkList!!
            var pt_dl_clicked: String? = deepLinkList[0]

            if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 1)
                if (deepLinkList.size > 0) {
                    pt_dl_clicked = deepLinkList[0]
                }
            }
            if (2 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 2)
                pt_dl_clicked = if (deepLinkList.size > 1) {
                    deepLinkList[1]
                } else {
                    deepLinkList[0]
                }
            }
            if (3 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 3)
                pt_dl_clicked = if (deepLinkList.size > 2) {
                    deepLinkList[2]
                } else {
                    deepLinkList[0]
                }
            }
            if (4 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 4)
                pt_dl_clicked = if (deepLinkList.size > 3) {
                    deepLinkList[3]
                } else {
                    deepLinkList[0]
                }
            }
            if (5 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 5)
                pt_dl_clicked = if (deepLinkList.size > 4) {
                    deepLinkList[4]
                } else {
                    deepLinkList[0]
                }
            }

            var notification = Utils.getNotificationById(context, notificationId)
            if (notification == null) {
                PTLog.verbose("Rating Notification is null, returning")
                return
            }
            contentViewRating = notification.bigContentView
            contentViewSmall = notification.contentView
            val contentViewRating = contentViewRating!!

            if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                clicked1 = false
            } else {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
            }
            if (2 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                clicked2 = false
            } else {
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
            }
            if (3 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled)
                clicked3 = false
            } else {
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
            }
            if (4 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_filled)
                clicked4 = false
            } else {
                contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
            }
            if (5 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_filled)
                contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_filled)
                clicked5 = false
            } else {
                contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)
            }
            cancelRatingClickIntents(context, intent)
            extras.putString(Constants.DEEP_LINK_KEY, pt_dl_clicked)
            contentViewRating.setOnClickPendingIntent(
                R.id.tVRatingConfirmation,
                LaunchPendingIntentFactory.getActivityIntent(extras, context)
            )

            setSmallIcon(context)

            val notificationBuilder = NotificationCompat.Builder(context, notification)
            val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
            val dIntent: PendingIntent = PendingIntentFactory.setDismissIntent(context, extras, dismissIntent)

            if (notificationManager != null) {
                //Use the Builder to build notification
                notificationBuilder.setSmallIcon(smallIcon)
                    .setCustomContentView(contentViewSmall)
                    .setCustomBigContentView(contentViewRating)
                    .setContentTitle(pt_title)
                    .setDeleteIntent(dIntent)
                    .setAutoCancel(true)

                notification = notificationBuilder.build()

                notificationManager!!.notify(notificationId, notification)
            }

            Utils.raiseCleverTapEvent(
                context, config, "Rating Submitted",
                Utils.convertRatingBundleObjectToHashMap(extras)
            )
            if (VERSION.SDK_INT < VERSION_CODES.S) {
                handleRatingDeepLink(context, extras, notificationId, pt_dl_clicked, this.config)
            }
        } catch (t: Throwable) {
            PTLog.verbose("Error creating rating notification ", t)
        }
    }

    /**
     * This method cancels all pending intents fired on click of rating. Allows the user to
     * click only once for Android 11+ devices and prevents collisions for future rating push-templates
     * @param context Context required for cancelling pending intents
     * @param intent Intent required for cancelling pending intents
     */
    private fun cancelRatingClickIntents(context: Context, intent: Intent) {
        var flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT
        flagsLaunchPendingIntent = flagsLaunchPendingIntent or PendingIntent.FLAG_IMMUTABLE
        val requestCodes = intent.getIntArrayExtra(PTConstants.KEY_REQUEST_CODES)

        for (requestCode in requestCodes!!) {
            PendingIntent.getBroadcast(
                context,
                requestCode, intent, flagsLaunchPendingIntent
            ).cancel()
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(InterruptedException::class)
    private fun handleRatingDeepLink(
        context: Context, extras: Bundle, notificationId: Int,
        pt_dl_clicked: String?, config: CleverTapInstanceConfig?
    ) {
        Thread.sleep(1000)
        notificationManager!!.cancel(notificationId)

        setToast(context, pt_rating_toast, config)

        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)

        val launchIntent: Intent
        if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
            launchIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(extras.getString(Constants.DEEP_LINK_KEY))
            )
            com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, launchIntent)
        } else {
            launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        }
        launchIntent.putExtras(extras)
        launchIntent.putExtra(Constants.DEEP_LINK_KEY, pt_dl_clicked)
        launchIntent.removeExtra(Constants.WZRK_ACTIONS)
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
        launchIntent.setFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        )
        context.startActivity(launchIntent)
    }

    @SuppressLint("MissingPermission")
    private fun handleVerticalImageButtonClick(context: Context, extras: Bundle) {
        CleverTapAPI.handleNotificationClicked(context, extras)

        val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID, -1)
        if (notificationId > -1) {
            notificationManager!!.cancel(notificationId)
        }

        context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        val deepLink = extras.getString(Constants.DEEP_LINK_KEY)
        val launchIntent: Intent
        if (deepLink != null && !deepLink.isEmpty()) {
            launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            com.clevertap.android.sdk.Utils.setPackageNameFromResolveInfoList(context, launchIntent)
        } else {
            launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        }
        launchIntent.putExtras(extras)
        launchIntent.removeExtra(Constants.WZRK_ACTIONS)
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
        launchIntent.setFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        )
        context.startActivity(launchIntent)
    }

    private fun handleProductDisplayNotification(context: Context, extras: Bundle) {
        try {
            val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
            var notification = Utils.getNotificationById(context, notificationId)
            if (notification == null) {
                PTLog.verbose("Product Display Notification is null, returning")
                return
            }
            contentViewBig = notification.bigContentView
            contentViewSmall = notification.contentView

            var isLinear = false
            if (pt_product_display_linear == null || pt_product_display_linear!!.isEmpty()) {
            } else {
                isLinear = true
            }

            setCustomContentViewBasicKeys(contentViewBig!!, context)
            if (!isLinear) {
                setCustomContentViewBasicKeys(contentViewSmall!!, context)
            }

            val currentPosition = extras.getInt(PTConstants.PT_CURRENT_POSITION)

            contentViewBig!!.setDisplayedChild(R.id.carousel_image, currentPosition)
            imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST)
            deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST)
            bigTextList = extras.getStringArrayList(PTConstants.PT_BIGTEXT_LIST)
            smallTextList = extras.getStringArrayList(PTConstants.PT_SMALLTEXT_LIST)
            priceList = extras.getStringArrayList(PTConstants.PT_PRICE_LIST)

            val dl = deepLinkList!![currentPosition]
            if (!isLinear) {
                contentViewBig!!.setTextViewText(R.id.title, bigTextList!![currentPosition])
            } else {
                contentViewBig!!.setTextViewText(R.id.product_name, bigTextList!![currentPosition])
            }
            contentViewBig!!.setTextViewText(R.id.msg, smallTextList!![currentPosition])
            contentViewBig!!.setTextViewText(R.id.product_price, priceList!![currentPosition])
            extras.remove(PTConstants.PT_CURRENT_POSITION)

            val bundleBuyNow = extras.clone() as Bundle
            bundleBuyNow.putBoolean(PTConstants.PT_IMAGE_1, true)
            bundleBuyNow.putInt(PTConstants.PT_NOTIF_ID, notificationId)
            bundleBuyNow.putString(PTConstants.PT_BUY_NOW_DL, dl)
            bundleBuyNow.putBoolean(PTConstants.PT_BUY_NOW, true)

            contentViewBig!!.setOnClickPendingIntent(
                R.id.product_action,
                PendingIntentFactory.getCtaLaunchPendingIntent(
                    context,
                    bundleBuyNow, dl, notificationId
                )
            )

            val notificationBuilder = NotificationCompat.Builder(context, notification)

            val bundleLaunchIntent = extras.clone() as Bundle
            bundleLaunchIntent.putString(Constants.DEEP_LINK_KEY, dl)
            val pIntent: PendingIntent? = PendingIntentFactory.getPendingIntent(
                context, notificationId, bundleLaunchIntent, true,
                PRODUCT_DISPLAY_CONTENT_PENDING_INTENT, null
            )

            if (notificationManager != null) {
                //Use the Builder to build notification
                val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
                val dIntent: PendingIntent = PendingIntentFactory.setDismissIntent(context, extras, dismissIntent)
                setSmallIcon(context)

                setNotificationBuilderBasics(
                    notificationBuilder, contentViewSmall, contentViewBig, pt_title,
                    pIntent,
                    dIntent
                )

                notification = notificationBuilder.build()

                notificationManager!!.notify(notificationId, notification)
            }
        } catch (t: Throwable) {
            PTLog.verbose("Error creating product display notification ", t)
        }
    }

    private fun handleFiveCTANotification(context: Context, extras: Bundle) {
        val dl: String? = null
        val notificationId = extras.getInt(PTConstants.PT_NOTIF_ID)
        extras.putString(Constants.DEEP_LINK_KEY, dl)

        if (close == extras.getBoolean("close")) {
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + "close")
            notificationManager!!.cancel(notificationId)
        }
        Utils.raiseNotificationClicked(context, extras, config)
    }

    private fun setNotificationBuilderBasics(
        notificationBuilder: NotificationCompat.Builder,
        contentViewSmall: RemoteViews?, contentViewBig: RemoteViews?, pt_title: String?, pIntent: PendingIntent?,
        dIntent: PendingIntent?
    ) {
        notificationBuilder.setSmallIcon(smallIcon)
            .setCustomContentView(contentViewSmall)
            .setCustomBigContentView(contentViewBig)
            .setContentTitle(Html.fromHtml(pt_title))
            .setDeleteIntent(dIntent)
            .setContentIntent(pIntent).setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
    }

    private fun setCustomContentViewBasicKeys(contentView: RemoteViews, context: Context) {
        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
        contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context, System.currentTimeMillis()))
        val pt_subtitle = pt_subtitle
        if (pt_subtitle != null && !pt_subtitle.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle, Html.FROM_HTML_MODE_LEGACY))
            } else {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle))
            }
        } else {
            contentView.setViewVisibility(R.id.subtitle, View.GONE)
            contentView.setViewVisibility(R.id.sep_subtitle, View.GONE)
        }
        val pt_meta_clr = pt_meta_clr
        if (pt_meta_clr != null && !pt_meta_clr.isEmpty()) {
            contentView.setTextColor(R.id.app_name, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS))
            contentView.setTextColor(R.id.timestamp, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS))
            contentView.setTextColor(R.id.subtitle, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS))
        }
    }

    private fun setStandardViewBigImageStyle(
        imgUrl: String?, extras: Bundle, context: Context,
        notificationBuilder: NotificationCompat.Builder, altText: String?
    ) {
        if (imgUrl != null && imgUrl.startsWith("http")) {
            try {
                val templateMediaManager =
                    TemplateMediaManager(TemplateRepository(context, config), GifDecoderImpl())
                val bpMap: Bitmap = templateMediaManager.getNotificationBitmap(imgUrl, false, context)
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

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
        notificationBuilder.setStyle(bigTextStyle)
    }

    private fun setSmallIcon(context: Context) {
        val metaData: Bundle?
        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            metaData = ai.metaData
            val x = Utils._getManifestStringValueForKey(metaData, ManifestInfo.LABEL_NOTIFICATION_ICON)
                ?: throw IllegalArgumentException()
            smallIcon = context.resources.getIdentifier(x, "drawable", context.packageName)
            if (smallIcon == 0) {
                throw IllegalArgumentException()
            }
        } catch (t: Throwable) {
            smallIcon = Utils.getAppIconAsIntId(context)
        }
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
    }

    private fun setToast(
        context: Context, message: String?,
        config: CleverTapInstanceConfig?
    ) {
        if (message != null && !message.isEmpty()) {
            Utils.showToast(context, message, config)
        }
    }
}
