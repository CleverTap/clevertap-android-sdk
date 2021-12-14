package com.clevertap.android.pushtemplates

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.os.*
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.styles.*
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class TemplateRenderer : INotificationRenderer {

    private var pt_id: String? = null
    private var templateType: TemplateType? = null
    internal var pt_title: String? = null
    internal var pt_msg: String? = null
    internal var pt_msg_summary: String? = null
    internal var pt_large_icon: String? = null
    internal var pt_big_img: String? = null
    internal var pt_title_clr: String? = null
    internal var pt_msg_clr: String? = null
    internal var pt_chrono_title_clr: String? = null
    internal var imageList: ArrayList<String>? = null
    internal var deepLinkList: ArrayList<String>? = null
    internal var bigTextList: ArrayList<String>? = null
    internal var smallTextList: ArrayList<String>? = null
    internal var priceList: ArrayList<String>? = null
    internal var pt_product_display_action: String? = null
    internal var pt_product_display_action_clr: String? = null
    internal var pt_bg: String? = null
    internal var pt_rating_default_dl: String? = null
    internal var pt_small_view: String? = null
    private var contentViewBig: RemoteViews? = null
    private var contentViewSmall: RemoteViews? = null
    private var contentViewCarousel: RemoteViews? = null
    private var contentViewRating: RemoteViews? = null
    private var contentFiveCTAs: RemoteViews? = null
    private var contentViewTimer: RemoteViews? = null
    private var contentViewTimerCollapsed: RemoteViews? = null
    internal var contentViewManualCarousel: RemoteViews? = null
    private var channelId: String? = null
    internal var smallIcon = 0
    internal var pt_dot = 0
    private var requiresChannelId = false
    private var notificationManager: NotificationManager? = null
    private var asyncHelper: AsyncHelper? = null

    //    private DBHelper dbHelper;
    var pt_timer_threshold = 0
    internal var pt_input_label: String? = null
    var pt_input_feedback: String? = null
    internal var pt_input_auto_open: String? = null
    internal var pt_dismiss_on_click: String? = null
    var pt_timer_end = 0
    private var pt_title_alt: String? = null
    private var pt_msg_alt: String? = null
    private var pt_big_img_alt: String? = null
    internal var pt_product_display_linear: String? = null
    internal var pt_meta_clr: String? = null
    internal var pt_product_display_action_text_clr: String? = null
    internal var pt_small_icon_clr: String? = null
    internal var pt_small_icon: Bitmap? = null
    internal var pt_dot_sep: Bitmap? = null
    private var pt_cancel_notif_id: String? = null
    private var pt_cancel_notif_ids: ArrayList<Int>? = null
    var actions: JSONArray? = null
    internal var pt_subtitle: String? = null
    private var pID: String? = null
    internal var pt_flip_interval = 0
    private var pt_collapse_key: Any? = null
    internal var pt_manual_carousel_type: String? = null
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
        return pt_msg // TODO: Check if set properly before caller calls this
    }

    override fun getTitle(extras: Bundle, context: Context): String? {
        return pt_title // TODO: Check if set properly before caller calls this
    }

    override fun renderNotification(
        extras: Bundle, context: Context, nb: NotificationCompat.Builder,
        config: CleverTapInstanceConfig,
        notificationId: Int
    ): NotificationCompat.Builder? {
        if (pt_id == null) {
            PTLog.verbose("Template ID not provided. Cannot create the notification")
            return null
        }
        this.notificationId = notificationId
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
                if (ValidatorFactory.getValidator(TemplateType.FIVE_ICONS, this)?.validate() == true)
                    return FiveIconStyle(this, extras).builderFromStyle(context, extras, notificationId, nb)
                        .setOngoing(true)

            TemplateType.PRODUCT_DISPLAY -> if (ValidatorFactory.getValidator(TemplateType.PRODUCT_DISPLAY, this)
                    ?.validate() == true
            )
                return ProductDisplayStyle(this, extras).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.ZERO_BEZEL ->
                if (ValidatorFactory.getValidator(TemplateType.ZERO_BEZEL, this)?.validate() == true)
                    return ZeroBezelStyle(this).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.TIMER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (ValidatorFactory.getValidator(TemplateType.TIMER, this)?.validate() == true) {
                    val timerEnd = getTimerEnd()
                    timerRunner(context, extras, notificationId, timerEnd)
                    return TimerStyle(this, extras).builderFromStyle(context, extras, notificationId, nb).setTimeoutAfter(
                        timerEnd!!.toLong()
                    )
                }
            } else {
                PTLog.debug("Push Templates SDK supports Timer Notifications only on or above Android Nougat, reverting to basic template")
                if (ValidatorFactory.getValidator(TemplateType.BASIC, this)?.validate() == true) {
                    return BasicStyle(this).builderFromStyle(context, extras, notificationId, nb)
                }
            }

            TemplateType.INPUT_BOX -> if (ValidatorFactory.getValidator(TemplateType.INPUT_BOX, this)
                    ?.validate() == true
            )
                return InputBoxStyle(this).builderFromStyle(context, extras, notificationId, nb)

            TemplateType.CANCEL -> renderCancelNotification(context)
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

    private fun getTimerEnd(): Int?{
        var timer_end: Int? = null
        if (pt_timer_threshold != -1 && pt_timer_threshold >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = pt_timer_threshold * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
        }else if (pt_timer_end >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = pt_timer_end * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
        }else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + PTConstants.PT_TIMER_END)
        }
        return timer_end
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun timerRunner(context: Context, extras: Bundle, notificationId: Int, delay: Int?) {
        val handler = Handler(Looper.getMainLooper())
        extras.remove("wzrk_rnv")
        if (pt_title_alt != null && pt_title_alt!!.isNotEmpty()) {
            pt_title = pt_title_alt
        }
        if (pt_big_img_alt != null && pt_big_img_alt!!.isNotEmpty()) {
            pt_big_img = pt_big_img_alt
        }
        if (pt_msg_alt != null && pt_msg_alt!!.isNotEmpty()) {
            pt_msg = pt_msg_alt
        }

        /*handler.postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                if (Utils.isNotificationInTray(context, notificationId)) {
                    asyncHelper.postAsyncSafely("TemplateRenderer#timerRunner", new Runnable() {
                        @Override
                        public void run() {
                            if (hasAllBasicNotifKeys()) {
                                renderBasicTemplateNotification(context, extras, _root_ide_package_.PTConstants.EMPTY_NOTIFICATION_ID);
                            }
                        }
                    });
                }
            }
        }, delay - 100);*/

        handler.postDelayed({
            if (Utils.isNotificationInTray(context, notificationId) && ValidatorFactory.
                getValidator(TemplateType.BASIC, this)?.validate() == true) {
                val applicationContext = context.applicationContext
                val basicTemplateBundle = extras.clone() as Bundle
                basicTemplateBundle.putString(Constants.WZRK_PUSH_ID, null) // skip dupe check
                basicTemplateBundle.putString(PTConstants.PT_ID, "pt_basic") // set to basic
                // force random id generation
                basicTemplateBundle.putString(PTConstants.PT_COLLAPSE_KEY, null)
                basicTemplateBundle.putString(Constants.WZRK_COLLAPSE, null)
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
        }, (delay!! - 100).toLong())
    }



//    private fun renderZeroBezelNotification(
//        context: Context, extras: Bundle, notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder {
//        var nb = nb
//        PTLog.debug("Rendering Zero Bezel Template Push Notification with extras - $extras")
//        try {
//            contentViewBig = RemoteViews(context.packageName, R.layout.zero_bezel)// ZBBCV
//            setCustomContentViewBasicKeys(contentViewBig!!, context)// ZBBCV
//            val textOnlySmallView = pt_small_view != null && pt_small_view == PTConstants.TEXT_ONLY
//            contentViewSmall = if (textOnlySmallView) {
//                RemoteViews(context.packageName, R.layout.cv_small_text_only)// ZBTOSCV
//            } else {
//                RemoteViews(context.packageName, R.layout.cv_small_zero_bezel)// ZBMSCV
//            }
//            setCustomContentViewBasicKeys(contentViewSmall!!, context)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            setCustomContentViewTitle(contentViewBig!!, pt_title)// ZBBCV init
//            setCustomContentViewTitle(contentViewSmall!!, pt_title)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            setCustomContentViewMessage(contentViewBig!!, pt_msg)// ZBBCV init
//            if (textOnlySmallView) {
//                contentViewSmall!!.setViewVisibility(R.id.msg, View.GONE)//ZBTOSCV init
//            } else {
//                setCustomContentViewMessage(contentViewSmall!!, pt_msg)// ZBMSCV init
//            }
//            setCustomContentViewMessageSummary(contentViewBig!!, pt_msg_summary)// ZBBCV init
//            setCustomContentViewTitleColour(contentViewBig!!, pt_title_clr)// ZBBCV init
//            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)// ZBBCV init
//            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            setCustomContentViewMessageColour(contentViewBig!!, pt_msg_clr)// ZBBCV init
//            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent: PendingIntent = if (deepLinkList != null) {
//                setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList!![0])
//            } else {
//                setPendingIntent(context, notificationId, extras, launchIntent, null)
//            }
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentViewSmall!!,
//                contentViewBig!!,
//                pt_title,
//                pIntent
//            )
//            setCustomContentViewBigImage(contentViewBig!!, pt_big_img)// ZBBCV init
//            if (!textOnlySmallView) {
//                setCustomContentViewBigImage(contentViewSmall!!, pt_big_img)// ZBMSCV init
//            }
//            if (textOnlySmallView) {
//                setCustomContentViewLargeIcon(contentViewSmall!!, pt_large_icon)// ZBTOSCV init
//            }
//            setCustomContentViewSmallIcon(contentViewBig!!)// ZBBCV init
//            setCustomContentViewSmallIcon(contentViewSmall!!)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            setCustomContentViewDotSep(contentViewBig!!)// ZBBCV init
//            setCustomContentViewDotSep(contentViewSmall!!)// ZBSCV init // ZBTOSCV super init->ZBSCV // ZBMSCV super init->ZBSCV
//            if (Utils.getFallback()) {
//                PTLog.debug("Image not fetched, falling back to Basic Template")
//                return renderBasicTemplateNotification(
//                    context,
//                    extras,
//                    notificationId,
//                    nb//where to add
//                ) // will overwrite set values on nb
//            }
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating image only notification", t)
//        }
//        return nb
//    }

//    private fun renderProductDisplayNotification(
//        context: Context, extras: Bundle,
//        notificationId: Int, nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder? {
//        var nb = nb
//        PTLog.debug("Rendering Product Display Template Push Notification with extras - $extras")
//        /** ------------------------------------------------------------
//         * -------IMP: FIRST INIT BIG CONTENT VIEW THEN SMALL-----------
//         * ------------------------------------------------------------*/
//        try {
//            var isLinear = false
//            if (pt_product_display_linear == null || pt_product_display_linear!!.isEmpty()) {
//                contentViewBig = RemoteViews(context.packageName, R.layout.product_display_template)// PDNLBCV
//                contentViewSmall = RemoteViews(context.packageName, R.layout.content_view_small)//NLSCV
//            } else {
//                isLinear = true
//                contentViewBig =
//                    RemoteViews(context.packageName, R.layout.product_display_linear_expanded)// PDLBCV
//                contentViewSmall =
//                    RemoteViews(context.packageName, R.layout.product_display_linear_collapsed)//LSCV
//            }
//            setCustomContentViewBasicKeys(contentViewBig!!, context)// PDLBCV init // PDNLBCV super init->PDLBCV
//            if (!isLinear) {
//                setCustomContentViewBasicKeys(contentViewSmall!!, context)//NLSCV super init -> scv
//            }
//            if (bigTextList!!.isNotEmpty()) {
//                setCustomContentViewText(contentViewBig!!, R.id.product_name, bigTextList!![0])// PDLBCV init // PDNLBCV super init->PDLBCV
//            }
//            if (!isLinear) {
//                if (smallTextList!!.isNotEmpty()) {// PDNLBCV init
//                    setCustomContentViewText(
//                        contentViewBig!!,
//                        R.id.product_description,
//                        smallTextList!![0]
//                    )
//                }
//            }
//            if (priceList!!.isNotEmpty()) {
//                setCustomContentViewText(contentViewBig!!, R.id.product_price, priceList!![0])// PDLBCV init // PDNLBCV super init->PDLBCV
//            }
//            if (!isLinear) {
//                setCustomContentViewTitle(contentViewBig!!, pt_title)// PDNLBCV init
//                setCustomContentViewTitle(contentViewSmall!!, pt_title)//NLSCV super init -> scv
//                setCustomContentViewMessage(contentViewBig!!, pt_msg)// PDNLBCV init
//                setCustomContentViewElementColour(
//                    contentViewBig!!,
//                    R.id.product_description,
//                    pt_msg_clr
//                )// PDNLBCV init
//                setCustomContentViewElementColour(contentViewBig!!, R.id.product_name, pt_title_clr)// PDNLBCV init
//                setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)//NLSCV super init -> scv
//            }
//            setCustomContentViewMessage(contentViewSmall!!, pt_msg)//NLSCV super init -> scv // LSCV init
//            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)//NLSCV super init -> scv // LSCV init
//            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)// PDLBCV init // PDNLBCV super init->PDLBCV
//            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)//NLSCV super init -> scv // LSCV init
//            setCustomContentViewButtonLabel(
//                contentViewBig!!,
//                R.id.product_action,
//                pt_product_display_action
//            )// PDLBCV init // PDNLBCV super init->PDLBCV
//            setCustomContentViewButtonColour(
//                contentViewBig!!,
//                R.id.product_action,
//                pt_product_display_action_clr
//            )// PDLBCV init // PDNLBCV super init->PDLBCV
//            setCustomContentViewButtonText(
//                contentViewBig!!,
//                R.id.product_action,
//                pt_product_display_action_text_clr
//            )// PDLBCV init // PDNLBCV super init->PDLBCV
//            setCustomContentViewLargeIcon(contentViewSmall!!, pt_large_icon)//NLSCV super init -> scv // LSCV init
//            if (!isLinear) {
//                setCustomContentViewSmallIcon(contentViewSmall!!)//NLSCV super init -> scv
//                setCustomContentViewDotSep(contentViewSmall!!)//NLSCV super init -> scv
//            }
//            var imageCounter = 0
//            var isFirstImageOk = false
//            val smallImageLayoutIds = ArrayList<Int>()
//            smallImageLayoutIds.add(R.id.small_image1)
//            smallImageLayoutIds.add(R.id.small_image2)
//            smallImageLayoutIds.add(R.id.small_image3)
//            // --------------LSCV init---- START-------------extract logic
//            val smallCollapsedImageLayoutIds = ArrayList<Int>()
//            smallCollapsedImageLayoutIds.add(R.id.small_image1_collapsed)
//            smallCollapsedImageLayoutIds.add(R.id.small_image2_collapsed)
//            smallCollapsedImageLayoutIds.add(R.id.small_image3_collapsed)
//            // --------------LSCV init---- END-------------extract logic
//            val tempImageList = ArrayList<String>()
//
//            for (index in imageList!!.indices) {
//                if (isLinear) {
//                    // --------------LSCV init---- START-------------extract logic
//                    Utils.loadImageURLIntoRemoteView(
//                        smallCollapsedImageLayoutIds[imageCounter],
//                        imageList!![index],
//                        contentViewSmall
//                    )
//                    contentViewSmall!!.setViewVisibility(
//                        smallCollapsedImageLayoutIds[imageCounter],
//                        View.VISIBLE
//                    )
//                    if (!Utils.getFallback()) {
//                        contentViewSmall!!.setViewVisibility(
//                            smallCollapsedImageLayoutIds[imageCounter],
//                            View.VISIBLE
//                        )
//                    }
//                    // --------------LSCV init---- END-------------extract logic
//                }
//                // --------------PDLBCV init // PDNLBCV super init->PDLBCV---- START-------------extract logic
//                Utils.loadImageURLIntoRemoteView(
//                    smallImageLayoutIds[imageCounter], imageList!![index], contentViewBig
//                )
//                val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view)
//                Utils.loadImageURLIntoRemoteView(R.id.fimg, imageList!![index], tempRemoteView)
//                if (!Utils.getFallback()) {
//                    if (!isFirstImageOk) {
//                        isFirstImageOk = true
//                    }
//                    contentViewBig!!.setViewVisibility(
//                        smallImageLayoutIds[imageCounter],
//                        View.VISIBLE
//                    )
//                    contentViewBig!!.addView(R.id.carousel_image, tempRemoteView)
//                    imageCounter++
//                    tempImageList.add(imageList!![index])
//                } else {
//                    deepLinkList!!.removeAt(index)
//                    bigTextList!!.removeAt(index)
//                    smallTextList!!.removeAt(index)
//                    priceList!!.removeAt(index)
//                }
//                // --------------PDLBCV init // PDNLBCV super init->PDLBCV---- END-------------extract logic
//            }
//            // --------------PDLBCV init // PDNLBCV super init->PDLBCV---- START-------------
//            extras.putStringArrayList(PTConstants.PT_IMAGE_LIST, tempImageList)
//            extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, deepLinkList)
//            extras.putStringArrayList(PTConstants.PT_BIGTEXT_LIST, bigTextList)
//            extras.putStringArrayList(PTConstants.PT_SMALLTEXT_LIST, smallTextList)
//            extras.putStringArrayList(PTConstants.PT_PRICE_LIST, priceList)
//            val requestCode1 = Random().nextInt()
//            val requestCode2 = Random().nextInt()
//            val requestCode3 = Random().nextInt()
//            val notificationIntent1 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent1.putExtra(PTConstants.PT_CURRENT_POSITION, 0)
//            notificationIntent1.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent1.putExtra(PTConstants.PT_BUY_NOW_DL, deepLinkList!![0])
//            notificationIntent1.putExtras(extras)
//            val contentIntent1 =
//                PendingIntent.getBroadcast(context, requestCode1, notificationIntent1, 0)
//            contentViewBig!!.setOnClickPendingIntent(R.id.small_image1, contentIntent1)
//            if (deepLinkList!!.size >= 2) {
//                val notificationIntent2 = Intent(context, PushTemplateReceiver::class.java)
//                notificationIntent2.putExtra(PTConstants.PT_CURRENT_POSITION, 1)
//                notificationIntent2.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//                notificationIntent2.putExtra(PTConstants.PT_BUY_NOW_DL, deepLinkList!![1])
//                notificationIntent2.putExtras(extras)
//                val contentIntent2 =
//                    PendingIntent.getBroadcast(context, requestCode2, notificationIntent2, 0)
//                contentViewBig!!.setOnClickPendingIntent(R.id.small_image2, contentIntent2)
//            }
//            if (deepLinkList!!.size >= 3) {
//                val notificationIntent3 = Intent(context, PushTemplateReceiver::class.java)
//                notificationIntent3.putExtra(PTConstants.PT_CURRENT_POSITION, 2)
//                notificationIntent3.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//                notificationIntent3.putExtra(PTConstants.PT_BUY_NOW_DL, deepLinkList!![2])
//                notificationIntent3.putExtras(extras)
//                val contentIntent3 =
//                    PendingIntent.getBroadcast(context, requestCode3, notificationIntent3, 0)
//                contentViewBig!!.setOnClickPendingIntent(R.id.small_image3, contentIntent3)
//            }
//            val notificationIntent4 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent4.putExtra(PTConstants.PT_IMAGE_1, true)
//            notificationIntent4.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent4.putExtra(PTConstants.PT_BUY_NOW_DL, deepLinkList!![0])
//            notificationIntent4.putExtra(PTConstants.PT_BUY_NOW, true)
//            notificationIntent4.putExtra("config", config)
//            notificationIntent4.putExtras(extras)
//            val contentIntent4 =
//                PendingIntent.getBroadcast(context, Random().nextInt(), notificationIntent4, 0)
//            contentViewBig!!.setOnClickPendingIntent(R.id.product_action, contentIntent4)
//            // --------------PDLBCV init // PDNLBCV super init->PDLBCV---- END-------------
//            if (isLinear) {
//                // --------------LSCV init---- START-------------extract logic
//                val notificationSmallIntent1 =
//                    Intent(context, PTPushNotificationReceiver::class.java)
//                val contentSmallIntent1 = setPendingIntent(
//                    context,
//                    notificationId,
//                    extras,
//                    notificationSmallIntent1,
//                    deepLinkList!![0]
//                )
//                contentViewSmall!!.setOnClickPendingIntent(
//                    R.id.small_image1_collapsed,
//                    contentSmallIntent1
//                )
//                if (deepLinkList!!.size >= 2) {
//                    val notificationSmallIntent2 =
//                        Intent(context, PTPushNotificationReceiver::class.java)
//                    val contentSmallIntent2 = setPendingIntent(
//                        context,
//                        notificationId,
//                        extras,
//                        notificationSmallIntent2,
//                        deepLinkList!![1]
//                    )
//                    contentViewSmall!!.setOnClickPendingIntent(
//                        R.id.small_image2_collapsed,
//                        contentSmallIntent2
//                    )
//                }
//                if (deepLinkList!!.size >= 3) {
//                    val notificationSmallIntent3 =
//                        Intent(context, PTPushNotificationReceiver::class.java)
//                    val contentSmallIntent3 = setPendingIntent(
//                        context,
//                        notificationId,
//                        extras,
//                        notificationSmallIntent3,
//                        deepLinkList!![2]
//                    )
//                    contentViewSmall!!.setOnClickPendingIntent(
//                        R.id.small_image3_collapsed,
//                        contentSmallIntent3
//                    )
//                }
//                // --------------LSCV init---- END-------------extract logic
//            }
//            val dismissIntent = Intent(context, PushTemplateReceiver::class.java)
//            val dIntent: PendingIntent
//            dIntent = setDismissIntent(context, extras, dismissIntent)
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent: PendingIntent
//            pIntent = if (deepLinkList != null) {
//                setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList!![0])
//            } else {
//                setPendingIntent(context, notificationId, extras, launchIntent, null)
//            }
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentViewSmall!!,
//                contentViewBig!!,
//                pt_title,
//                pIntent,
//                dIntent
//            )
//            // --------------PDLBCV init // PDNLBCV super init->PDLBCV---- START-------------
//            setCustomContentViewDotSep(contentViewBig!!)
//            setCustomContentViewSmallIcon(contentViewBig!!)
//            // --------------PDLBCV init // PDNLBCV super init->PDLBCV---- END-------------
//            if (imageCounter <= 1) {
//                PTLog.debug("2 or more images are not retrievable, not displaying the notification.")
//                return null
//            }
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating Product Display Notification ", t)
//        }
//        return nb
//    }
//
//    private fun renderFiveIconNotification(
//        context: Context, extras: Bundle, notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder? {
//        var nb = nb
//        PTLog.debug("Rendering Five Icon Template Push Notification with extras - $extras")
//        try {
//            // --------------FI init---- START-------------
//            if (pt_title == null || pt_title!!.isEmpty()) {
//                pt_title = Utils.getApplicationName(context)
//            }
//            contentFiveCTAs = RemoteViews(context.packageName, R.layout.five_cta)
//            setCustomContentViewExpandedBackgroundColour(contentFiveCTAs!!, pt_bg)
//            // --------------FI init---- END-------------
//            // --------------FACTORY---- START-------------
//            val reqCode1 = Random().nextInt()
//            val reqCode2 = Random().nextInt()
//            val reqCode3 = Random().nextInt()
//            val reqCode4 = Random().nextInt()
//            val reqCode5 = Random().nextInt()
//            val reqCode6 = Random().nextInt()
//            val notificationIntent1 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent1.putExtra("cta1", true)
//            notificationIntent1.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent1.putExtras(extras)
//            val contentIntent1 =
//                PendingIntent.getBroadcast(context, reqCode1, notificationIntent1, 0)
//            // --------------FACTORY---- END-------------
//            contentFiveCTAs!!.setOnClickPendingIntent(R.id.cta1, contentIntent1)// FI init
//            // --------------FACTORY---- START-------------
//            val notificationIntent2 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent2.putExtra("cta2", true)
//            notificationIntent2.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent2.putExtras(extras)
//            val contentIntent2 =
//                PendingIntent.getBroadcast(context, reqCode2, notificationIntent2, 0)
//            // --------------FACTORY---- END-------------
//            contentFiveCTAs!!.setOnClickPendingIntent(R.id.cta2, contentIntent2)// FI init
//            // --------------FACTORY---- START-------------
//            val notificationIntent3 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent3.putExtra("cta3", true)
//            notificationIntent3.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent3.putExtras(extras)
//            val contentIntent3 =
//                PendingIntent.getBroadcast(context, reqCode3, notificationIntent3, 0)
//            // --------------FACTORY---- END-------------
//            contentFiveCTAs!!.setOnClickPendingIntent(R.id.cta3, contentIntent3)// FI init
//            // --------------FACTORY---- START-------------
//            val notificationIntent4 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent4.putExtra("cta4", true)
//            notificationIntent4.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent4.putExtras(extras)
//            val contentIntent4 =
//                PendingIntent.getBroadcast(context, reqCode4, notificationIntent4, 0)
//            // --------------FACTORY---- END-------------
//            contentFiveCTAs!!.setOnClickPendingIntent(R.id.cta4, contentIntent4)// FI init
//            // --------------FACTORY---- START-------------
//            val notificationIntent5 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent5.putExtra("cta5", true)
//            notificationIntent5.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent5.putExtras(extras)
//            val contentIntent5 =
//                PendingIntent.getBroadcast(context, reqCode5, notificationIntent5, 0)
//            // --------------FACTORY---- END-------------
//            contentFiveCTAs!!.setOnClickPendingIntent(R.id.cta5, contentIntent5)// FI init
//            // --------------FACTORY---- START-------------
//            val notificationIntent6 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent6.putExtra("close", true)
//            notificationIntent6.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent6.putExtras(extras)
//            val contentIntent6 =
//                PendingIntent.getBroadcast(context, reqCode6, notificationIntent6, 0)
//            // --------------FACTORY---- END-------------
//            contentFiveCTAs!!.setOnClickPendingIntent(R.id.close, contentIntent6)// FI init
//            // --------------FACTORY---- START-------------
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null)
//            // --------------FACTORY---- END-------------
//            // --------------FI init---- START-------------
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentFiveCTAs!!,
//                contentFiveCTAs!!,
//                pt_title,
//                pIntent
//            )
//            nb.setOngoing(true)
//            var imageCounter = 0
//            for (imageKey in imageList!!.indices) {
//                if (imageKey == 0) {
//                    Utils.loadImageURLIntoRemoteView(
//                        R.id.cta1,
//                        imageList!![imageKey],
//                        contentFiveCTAs
//                    )
//                    if (Utils.getFallback()) {
//                        contentFiveCTAs!!.setViewVisibility(R.id.cta1, View.GONE)
//                        imageCounter++
//                    }
//                } else if (imageKey == 1) {
//                    Utils.loadImageURLIntoRemoteView(
//                        R.id.cta2,
//                        imageList!![imageKey],
//                        contentFiveCTAs
//                    )
//                    if (Utils.getFallback()) {
//                        imageCounter++
//                        contentFiveCTAs!!.setViewVisibility(R.id.cta2, View.GONE)
//                    }
//                } else if (imageKey == 2) {
//                    Utils.loadImageURLIntoRemoteView(
//                        R.id.cta3,
//                        imageList!![imageKey],
//                        contentFiveCTAs
//                    )
//                    if (Utils.getFallback()) {
//                        imageCounter++
//                        contentFiveCTAs!!.setViewVisibility(R.id.cta3, View.GONE)
//                    }
//                } else if (imageKey == 3) {
//                    Utils.loadImageURLIntoRemoteView(
//                        R.id.cta4,
//                        imageList!![imageKey],
//                        contentFiveCTAs
//                    )
//                    if (Utils.getFallback()) {
//                        imageCounter++
//                        contentFiveCTAs!!.setViewVisibility(R.id.cta4, View.GONE)
//                    }
//                } else if (imageKey == 4) {
//                    Utils.loadImageURLIntoRemoteView(
//                        R.id.cta5,
//                        imageList!![imageKey],
//                        contentFiveCTAs
//                    )
//                    if (Utils.getFallback()) {
//                        imageCounter++
//                        contentFiveCTAs!!.setViewVisibility(R.id.cta5, View.GONE)
//                    }
//                }
//            }
//            Utils.loadImageRidIntoRemoteView(R.id.close, R.drawable.pt_close, contentFiveCTAs)
//            if (imageCounter > 2) {
//                PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.")
//                return null
//            }
//            // --------------FI init---- END-------------
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating image only notification", t)
//        }
//        return nb
//    }
//
//    private fun renderRatingNotification(
//        context: Context, extras: Bundle, notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder {
//        var nb = nb
//        PTLog.debug("Rendering Rating Template Push Notification with extras - $extras")
//        try {
//            contentViewRating = RemoteViews(context.packageName, R.layout.rating)
//            setCustomContentViewBasicKeys(contentViewRating!!, context)// rating super init->iob
//            contentViewSmall = RemoteViews(context.packageName, R.layout.content_view_small)
//            setCustomContentViewBasicKeys(contentViewSmall!!, context)// scv
//            setCustomContentViewTitle(contentViewRating!!, pt_title)// rating super init->iob
//            setCustomContentViewTitle(contentViewSmall!!, pt_title)// scv
//            setCustomContentViewMessage(contentViewRating!!, pt_msg)// rating super init->iob
//            setCustomContentViewMessage(contentViewSmall!!, pt_msg)// scv
//            setCustomContentViewMessageSummary(contentViewRating!!, pt_msg_summary)// rating super init->iob
//            setCustomContentViewTitleColour(contentViewRating!!, pt_title_clr)// rating super init->iob
//            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)// scv
//            setCustomContentViewMessageColour(contentViewRating!!, pt_msg_clr)// rating super init->iob
//            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)// scv
//            setCustomContentViewExpandedBackgroundColour(contentViewRating!!, pt_bg)// rating super init->iob
//            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)// scv
//
//
//            // --------------rating init---- START-------------
//            //Set the rating stars
//            contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
//            contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
//            contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
//            contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
//            contentViewRating!!.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)
//            // --------------rating init---- END-------------
//
//            // --------------FACTORY---- START-------------
//            //Set Pending Intents for each star to listen to click
//            val notificationIntent1 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent1.putExtra("click1", true)
//            notificationIntent1.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent1.putExtra("config", config)
//            notificationIntent1.putExtras(extras)
//            val contentIntent1 =
//                PendingIntent.getBroadcast(context, Random().nextInt(), notificationIntent1, 0)
//            // --------------FACTORY---- END-------------
//            contentViewRating!!.setOnClickPendingIntent(R.id.star1, contentIntent1)// rating init
//            // --------------FACTORY---- START-------------
//            val notificationIntent2 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent2.putExtra("click2", true)
//            notificationIntent2.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent2.putExtra("config", config)
//            notificationIntent2.putExtras(extras)
//            val contentIntent2 =
//                PendingIntent.getBroadcast(context, Random().nextInt(), notificationIntent2, 0)
//            // --------------FACTORY---- END-------------
//            contentViewRating!!.setOnClickPendingIntent(R.id.star2, contentIntent2)// rating init
//            // --------------FACTORY---- START-------------
//            val notificationIntent3 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent3.putExtra("click3", true)
//            notificationIntent3.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent3.putExtra("config", config)
//            notificationIntent3.putExtras(extras)
//            val contentIntent3 =
//                PendingIntent.getBroadcast(context, Random().nextInt(), notificationIntent3, 0)
//            // --------------FACTORY---- END-------------
//            contentViewRating!!.setOnClickPendingIntent(R.id.star3, contentIntent3)// rating init
//            // --------------FACTORY---- START-------------
//            val notificationIntent4 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent4.putExtra("click4", true)
//            notificationIntent4.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent4.putExtra("config", config)
//            notificationIntent4.putExtras(extras)
//            val contentIntent4 =
//                PendingIntent.getBroadcast(context, Random().nextInt(), notificationIntent4, 0)
//            // --------------FACTORY---- END-------------
//            contentViewRating!!.setOnClickPendingIntent(R.id.star4, contentIntent4)// rating init
//            // --------------FACTORY---- START-------------
//            val notificationIntent5 = Intent(context, PushTemplateReceiver::class.java)
//            notificationIntent5.putExtra("click5", true)
//            notificationIntent5.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            notificationIntent5.putExtra("config", config)
//            notificationIntent5.putExtras(extras)
//            val contentIntent5 =
//                PendingIntent.getBroadcast(context, Random().nextInt(), notificationIntent5, 0)
//            // --------------FACTORY---- END-------------
//            contentViewRating!!.setOnClickPendingIntent(R.id.star5, contentIntent5)// rating init
//            // --------------FACTORY---- START-------------
//            val launchIntent = Intent(context, PushTemplateReceiver::class.java)
//            val pIntent = setPendingIntent(
//                context,
//                notificationId,
//                extras,
//                launchIntent,
//                pt_rating_default_dl
//            )
//            // --------------FACTORY---- END-------------
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentViewSmall!!,
//                contentViewRating!!,
//                pt_title,
//                pIntent
//            )// rating init
//            setCustomContentViewBigImage(contentViewRating!!, pt_big_img)// rating super init->iob
//            setCustomContentViewLargeIcon(contentViewSmall!!, pt_large_icon)// scv
//            setCustomContentViewLargeIcon(contentViewRating!!, pt_large_icon)// rating super init->iob
//            setCustomContentViewSmallIcon(contentViewRating!!)// rating super init->iob
//            setCustomContentViewSmallIcon(contentViewSmall!!)// scv
//            setCustomContentViewDotSep(contentViewRating!!)// rating super init->iob
//            setCustomContentViewDotSep(contentViewSmall!!)// scv
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating rating notification ", t)
//        }
//        return nb
//    }
//
//    private fun renderManualCarouselNotification(
//        context: Context, extras: Bundle,
//        notificationId: Int, nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder? {
//        var nb = nb
//        PTLog.debug("Rendering Manual Carousel Template Push Notification with extras - $extras")
//        try {
//            contentViewManualCarousel = RemoteViews(context.packageName, R.layout.manual_carousel)
//            setCustomContentViewBasicKeys(contentViewManualCarousel!!, context)// maanual super init->scv
//            contentViewSmall = RemoteViews(context.packageName, R.layout.content_view_small)
//            setCustomContentViewBasicKeys(contentViewSmall!!, context)// scv
//            setCustomContentViewTitle(contentViewManualCarousel!!, pt_title)// maanual super init->scv
//            setCustomContentViewTitle(contentViewSmall!!, pt_title)// scv
//            setCustomContentViewMessage(contentViewManualCarousel!!, pt_msg)// maanual super init->scv
//            setCustomContentViewMessage(contentViewSmall!!, pt_msg)// scv
//            setCustomContentViewExpandedBackgroundColour(contentViewManualCarousel!!, pt_bg)// maanual super init->scv
//            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)// scv
//            setCustomContentViewTitleColour(contentViewManualCarousel!!, pt_title_clr)// maanual super init->scv
//            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)// scv
//            setCustomContentViewMessageColour(contentViewManualCarousel!!, pt_msg_clr)// maanual super init->scv
//            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)// scv
//            setCustomContentViewMessageSummary(contentViewManualCarousel!!, pt_msg_summary)// manual init
//            contentViewManualCarousel!!.setViewVisibility(R.id.leftArrowPos0, View.VISIBLE)// manual init
//            contentViewManualCarousel!!.setViewVisibility(R.id.rightArrowPos0, View.VISIBLE)// manual init
//            // --------------manual init---- START-------------
//            var imageCounter = 0
//            var isFirstImageOk = false
//            val dl = deepLinkList!![0]
//            var currentPosition = 0
//            val tempImageList = ArrayList<String>()
//            for (index in imageList!!.indices) {
//                val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view_rounded)
//                Utils.loadImageURLIntoRemoteView(
//                    R.id.flipper_img,
//                    imageList!![index],
//                    tempRemoteView,
//                    context
//                )
//                if (!Utils.getFallback()) {
//                    if (!isFirstImageOk) {
//                        currentPosition = index
//                        isFirstImageOk = true
//                    }
//                    contentViewManualCarousel!!.addView(R.id.carousel_image, tempRemoteView)
//                    contentViewManualCarousel!!.addView(R.id.carousel_image_right, tempRemoteView)
//                    contentViewManualCarousel!!.addView(R.id.carousel_image_left, tempRemoteView)
//                    imageCounter++
//                    tempImageList.add(imageList!![index])
//                } else {
//                    if (deepLinkList != null && deepLinkList!!.size == imageList!!.size) {
//                        deepLinkList!!.removeAt(index)
//                    }
//                    PTLog.debug("Skipping Image in Manual Carousel.")
//                }
//            }
//            if (pt_manual_carousel_type == null || !pt_manual_carousel_type.equals(
//                    PTConstants.PT_MANUAL_CAROUSEL_FILMSTRIP,
//                    ignoreCase = true
//                )
//            ) {
//                contentViewManualCarousel!!.setViewVisibility(R.id.carousel_image_right, View.GONE)
//                contentViewManualCarousel!!.setViewVisibility(R.id.carousel_image_left, View.GONE)
//            }
//            contentViewManualCarousel!!.setDisplayedChild(R.id.carousel_image_right, 1)
//            contentViewManualCarousel!!.setDisplayedChild(
//                R.id.carousel_image_left,
//                tempImageList.size - 1
//            )
//            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, currentPosition)
//            extras.putStringArrayList(PTConstants.PT_IMAGE_LIST, tempImageList)
//            extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, deepLinkList)
//            // --------------manual init---- END-------------
//            // --------------FACTORY---- START-------------
//            val rightArrowPos0Intent = Intent(context, PushTemplateReceiver::class.java)
//            rightArrowPos0Intent.putExtra(PTConstants.PT_RIGHT_SWIPE, true)
//            rightArrowPos0Intent.putExtra(PTConstants.PT_MANUAL_CAROUSEL_FROM, 0)
//            rightArrowPos0Intent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            rightArrowPos0Intent.putExtras(extras)
//            val contentRightPos0Intent =
//                setPendingIntent(context, notificationId, extras, rightArrowPos0Intent, dl)
//            // --------------FACTORY---- END-------------
//            contentViewManualCarousel!!.setOnClickPendingIntent(
//                R.id.rightArrowPos0,
//                contentRightPos0Intent
//            )// manual init
//            // --------------FACTORY---- START-------------
//            val leftArrowPos0Intent = Intent(context, PushTemplateReceiver::class.java)
//            leftArrowPos0Intent.putExtra(PTConstants.PT_RIGHT_SWIPE, false)
//            leftArrowPos0Intent.putExtra(PTConstants.PT_MANUAL_CAROUSEL_FROM, 0)
//            leftArrowPos0Intent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//            leftArrowPos0Intent.putExtras(extras)
//            val contentLeftPos0Intent =
//                setPendingIntent(context, notificationId, extras, leftArrowPos0Intent, dl)
//            // --------------FACTORY---- END-------------
//            contentViewManualCarousel!!.setOnClickPendingIntent(
//                R.id.leftArrowPos0,
//                contentLeftPos0Intent
//            )// manual init
//            // --------------FACTORY---- START-------------
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)//factory
//            val pIntent = setPendingIntent(context, notificationId, extras, launchIntent, dl)
//            val dismissIntent = Intent(context, PushTemplateReceiver::class.java)//factory
//            val dIntent: PendingIntent
//            dIntent = setDismissIntent(context, extras, dismissIntent)
//            // --------------FACTORY---- END-------------
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentViewSmall!!,
//                contentViewManualCarousel!!,
//                pt_title,
//                pIntent,
//                dIntent
//            )// manual init
//            setCustomContentViewLargeIcon(contentViewSmall!!, pt_large_icon)// scv
//            setCustomContentViewLargeIcon(contentViewManualCarousel!!, pt_large_icon)// maanual super init->scv
//            setCustomContentViewSmallIcon(contentViewManualCarousel!!)// maanual super init->scv
//            setCustomContentViewSmallIcon(contentViewSmall!!)// scv
//            setCustomContentViewDotSep(contentViewManualCarousel!!)// maanual super init->scv
//            setCustomContentViewDotSep(contentViewSmall!!)// scv
//            if (imageCounter < 2) {
//                PTLog.debug("Need at least 2 images to display Manual Carousel, found - $imageCounter, not displaying the notification.")
//                return null
//            }// manual init
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating Manual carousel notification ", t)
//        }
//        return nb
//    }

    override fun setSmallIcon(smallIcon: Int, context: Context) {
        this.smallIcon = smallIcon
        try {
            pt_small_icon = Utils.setBitMapColour(context, smallIcon, pt_small_icon_clr)
        } catch (e: NullPointerException) {
            PTLog.debug("NPE while setting small icon color")
        }
    }

    override fun getActionButtonIconKey(): String {
        return PTConstants.PT_NOTIF_ICON
    }

    override fun getCollapseKey(extras: Bundle): Any? {
        return pt_collapse_key // TODO: Check if set properly before caller calls this
    }

    private fun setUp(context: Context, extras: Bundle, config: CleverTapInstanceConfig?) {
        pt_id = extras.getString(PTConstants.PT_ID)
        val pt_json = extras.getString(PTConstants.PT_JSON)
        if (pt_id != null) {
            templateType = TemplateType.fromString(pt_id)
            var newExtras: Bundle? = null
            try {
                if (pt_json != null && pt_json.isNotEmpty()) {
                    newExtras = Utils.fromJson(JSONObject(pt_json))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (newExtras != null) extras.putAll(newExtras)
        }
        pt_msg = extras.getString(PTConstants.PT_MSG)
        pt_msg_summary = extras.getString(PTConstants.PT_MSG_SUMMARY)
        pt_msg_clr = extras.getString(PTConstants.PT_MSG_COLOR)
        pt_title = extras.getString(PTConstants.PT_TITLE)
        pt_title_clr = extras.getString(PTConstants.PT_TITLE_COLOR)
        pt_meta_clr = extras.getString(PTConstants.PT_META_CLR)
        pt_bg = extras.getString(PTConstants.PT_BG)
        pt_big_img = extras.getString(PTConstants.PT_BIG_IMG)
        pt_large_icon = extras.getString(PTConstants.PT_NOTIF_ICON)
        pt_small_view = extras.getString(PTConstants.PT_SMALL_VIEW)
        imageList = Utils.getImageListFromExtras(extras)
        deepLinkList = Utils.getDeepLinkListFromExtras(extras)
        bigTextList = Utils.getBigTextFromExtras(extras)
        smallTextList = Utils.getSmallTextFromExtras(extras)
        priceList = Utils.getPriceFromExtras(extras)
        pt_rating_default_dl = extras.getString(PTConstants.PT_DEFAULT_DL)
//        asyncHelper = AsyncHelper.instance
        //        dbHelper = new DBHelper(context);
        pt_timer_threshold = Utils.getTimerThreshold(extras)
        pt_input_label = extras.getString(PTConstants.PT_INPUT_LABEL)
        pt_input_feedback = extras.getString(PTConstants.PT_INPUT_FEEDBACK)
        pt_input_auto_open = extras.getString(PTConstants.PT_INPUT_AUTO_OPEN)
        pt_dismiss_on_click = extras.getString(PTConstants.PT_DISMISS_ON_CLICK)
        pt_chrono_title_clr = extras.getString(PTConstants.PT_CHRONO_TITLE_COLOUR)
        pt_product_display_action = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION)
        pt_product_display_action_clr = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION_COLOUR)
        pt_timer_end = Utils.getTimerEnd(extras)
        pt_big_img_alt = extras.getString(PTConstants.PT_BIG_IMG_ALT)
        pt_msg_alt = extras.getString(PTConstants.PT_MSG_ALT)
        pt_title_alt = extras.getString(PTConstants.PT_TITLE_ALT)
        pt_product_display_linear = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_LINEAR)
        pt_product_display_action_text_clr =
            extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR)
        pt_small_icon_clr = extras.getString(PTConstants.PT_SMALL_ICON_COLOUR)
        pt_cancel_notif_id = extras.getString(PTConstants.PT_CANCEL_NOTIF_ID)
        pt_cancel_notif_ids = Utils.getNotificationIds(context)
        actions = Utils.getActionKeys(extras)
        pt_subtitle = extras.getString(PTConstants.PT_SUBTITLE)
        pt_collapse_key = extras[PTConstants.PT_COLLAPSE_KEY]
        pt_flip_interval = Utils.getFlipInterval(extras)
        pID = extras.getString(Constants.WZRK_PUSH_ID)
        pt_manual_carousel_type = extras.getString(PTConstants.PT_MANUAL_CAROUSEL_TYPE)
        if (config != null) {
            this.config = config
        }
        setKeysFromDashboard(extras)
    }

//    private fun setDotSep(context: Context) {
//        try {
//            pt_dot = context.resources.getIdentifier(
//                PTConstants.PT_DOT_SEP,
//                "drawable",
//                context.packageName
//            )
//            pt_dot_sep = Utils.setBitMapColour(context, pt_dot, pt_meta_clr)
//        } catch (e: NullPointerException) {
//            PTLog.debug("NPE while setting dot sep color")
//        }
//    }

//    private fun renderAutoCarouselNotification(
//        context: Context,
//        extras: Bundle,
//        notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder? {
//        var nb = nb
//        PTLog.debug("Rendering Auto Carousel Template Push Notification with extras - $extras")
//        try {
//            contentViewCarousel = RemoteViews(context.packageName, R.layout.auto_carousel)
//            setCustomContentViewBasicKeys(contentViewCarousel!!, context)// auto super init->scv
//            contentViewSmall = RemoteViews(context.packageName, R.layout.content_view_small)
//            setCustomContentViewBasicKeys(contentViewSmall!!, context) // scv
//            setCustomContentViewTitle(contentViewCarousel!!, pt_title)// auto super init->scv
//            setCustomContentViewTitle(contentViewSmall!!, pt_title) // scv
//            setCustomContentViewMessage(contentViewCarousel!!, pt_msg)// auto super init->scv
//            setCustomContentViewMessage(contentViewSmall!!, pt_msg) // scv
//            setCustomContentViewExpandedBackgroundColour(contentViewCarousel!!, pt_bg)// auto super init->scv
//            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg) // scv
//            setCustomContentViewTitleColour(contentViewCarousel!!, pt_title_clr)// auto super init->scv
//            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr) // scv
//            setCustomContentViewMessageColour(contentViewCarousel!!, pt_msg_clr)// auto super init->scv
//            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr) // scv
//            setCustomContentViewMessageSummary(contentViewCarousel!!, pt_msg_summary)// auto init
//            setCustomContentViewViewFlipperInterval(contentViewCarousel!!, pt_flip_interval)// auto init
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent: PendingIntent
//            pIntent = if (deepLinkList != null) {
//                setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList!![0])
//            } else {
//                setPendingIntent(context, notificationId, extras, launchIntent, null)
//            }
//            nb = setNotificationBuilderBasics(
//                nb, contentViewSmall!!, contentViewCarousel!!, pt_title,
//                pIntent
//            )
//            // -------------auto init START----------------setViewFlipper
//            var imageCounter = 0
//            for (index in imageList!!.indices) {
//                val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view)
//                Utils.loadImageURLIntoRemoteView(R.id.fimg, imageList!![index], tempRemoteView)
//                if (!Utils.getFallback()) {
//                    contentViewCarousel!!.addView(R.id.view_flipper, tempRemoteView)
//                    imageCounter++
//                } else {
//                    PTLog.debug("Skipping Image in Auto Carousel.")
//                }
//            }
//            // -------------auto init END----------------setViewFlipper
//            setCustomContentViewLargeIcon(contentViewSmall!!, pt_large_icon) // scv
//            setCustomContentViewLargeIcon(contentViewCarousel!!, pt_large_icon)// auto super init->scv
//            setCustomContentViewSmallIcon(contentViewCarousel!!)// auto super init->scv
//            setCustomContentViewSmallIcon(contentViewSmall!!) // scv
//            setCustomContentViewDotSep(contentViewCarousel!!)// auto super init->scv
//            setCustomContentViewDotSep(contentViewSmall!!) // scv
//            if (imageCounter < 2) {
//                PTLog.debug(
//                    "Need at least 2 images to display Auto Carousel, found - "
//                            + imageCounter + ", not displaying the notification."
//                )
//                return null
//            }
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating auto carousel notification ", t)
//        }
//        return nb
//    }

//    private fun setCustomContentViewViewFlipperInterval(contentView: RemoteViews, interval: Int) {
//        contentView.setInt(R.id.view_flipper, "setFlipInterval", interval)
//    }

//    private fun renderBasicTemplateNotification(
//        context: Context,
//        extras: Bundle,
//        notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder {
//        var nb = nb
//        PTLog.debug("Rendering Basic Template Push Notification with extras - $extras")
//        try {
//            contentViewBig = RemoteViews(context.packageName, R.layout.image_only_big)
//            setCustomContentViewBasicKeys(contentViewBig!!, context)// iob super init
//            contentViewSmall = RemoteViews(context.packageName, R.layout.content_view_small)
//            setCustomContentViewBasicKeys(contentViewSmall!!, context)//cv
//            setCustomContentViewTitle(contentViewBig!!, pt_title)// iob super init
//            setCustomContentViewTitle(contentViewSmall!!, pt_title)//cv
//            setCustomContentViewMessage(contentViewBig!!, pt_msg)// iob super init
//            setCustomContentViewMessage(contentViewSmall!!, pt_msg)//cv
//            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)// iob super init
//            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)//cv
//            setCustomContentViewTitleColour(contentViewBig!!, pt_title_clr)// iob super init
//            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)//cv
//            setCustomContentViewMessageColour(contentViewBig!!, pt_msg_clr)// iob super init
//            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)//cv
//            setCustomContentViewMessageSummary(contentViewBig!!, pt_msg_summary)// iob
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent: PendingIntent
//            pIntent = if (deepLinkList != null && deepLinkList!!.size > 0) {
//                setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList!![0])
//            } else {
//                setPendingIntent(context, notificationId, extras, launchIntent, null)
//            }
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentViewSmall!!,
//                contentViewBig!!,
//                pt_title,
//                pIntent
//            )
//            setCustomContentViewSmallIcon(contentViewBig!!)// iob super init
//            setCustomContentViewSmallIcon(contentViewSmall!!)// cv
//            setCustomContentViewDotSep(contentViewBig!!)// iob super init
//            setCustomContentViewDotSep(contentViewSmall!!)// cv
//            setCustomContentViewBigImage(contentViewBig!!, pt_big_img)// iob
//            setCustomContentViewLargeIcon(contentViewBig!!, pt_large_icon)// iob super init
//            setCustomContentViewLargeIcon(contentViewSmall!!, pt_large_icon) // cv
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating image only notification", t)
//        }
//        return nb
//    }

//    private fun setCustomContentViewText(contentView: RemoteViews, resourceId: Int, s: String) {
//        if (s.isNotEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                contentView.setTextViewText(
//                    resourceId,
//                    Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY)
//                )
//            } else {
//                contentView.setTextViewText(resourceId, Html.fromHtml(s))
//            }
//        }
//    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private fun renderTimerNotification(
//        context: Context,
//        extras: Bundle,
//        notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder? {
//        var nb = nb
//        PTLog.debug("Rendering Timer Template Push Notification with extras - $extras")
//        try {
//            contentViewTimer = RemoteViews(context.packageName, R.layout.timer)// TBCV
//            contentViewTimerCollapsed = RemoteViews(context.packageName, R.layout.timer_collapsed)// TSCV
//            // renderer call first -------START--------
//            val timer_end: Int
//            timer_end =
//                if (pt_timer_threshold != -1 && pt_timer_threshold >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
//                    pt_timer_threshold * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
//                } else if (pt_timer_end >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
//                    pt_timer_end * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
//                } else {
//                    PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + PTConstants.PT_TIMER_END)
//                    return null//TODO Check this
//                }
//            // renderer call first -------END--------
//            setCustomContentViewBasicKeys(contentViewTimer!!, context)// TBCV super init -> TSCV
//            setCustomContentViewBasicKeys(contentViewTimerCollapsed!!, context) // TSCV init
//            setCustomContentViewTitle(contentViewTimer!!, pt_title)// TBCV super init -> TSCV
//            setCustomContentViewTitle(contentViewTimerCollapsed!!, pt_title)// TSCV init
//            setCustomContentViewMessage(contentViewTimer!!, pt_msg)// TBCV super init -> TSCV
//            setCustomContentViewMessage(contentViewTimerCollapsed!!, pt_msg)// TSCV init
//            setCustomContentViewExpandedBackgroundColour(contentViewTimer!!, pt_bg)// TBCV super init -> TSCV
//            setCustomContentViewCollapsedBackgroundColour(contentViewTimerCollapsed!!, pt_bg)// TSCV init
//            setCustomContentViewChronometerBackgroundColour(contentViewTimer!!, pt_bg)// TBCV super init -> TSCV
//            setCustomContentViewChronometerBackgroundColour(contentViewTimerCollapsed!!, pt_bg)// TSCV init
//            setCustomContentViewTitleColour(contentViewTimer!!, pt_title_clr)// TBCV super init -> TSCV
//            setCustomContentViewTitleColour(contentViewTimerCollapsed!!, pt_title_clr)// TSCV init
//            setCustomContentViewChronometerTitleColour(
//                contentViewTimer!!,
//                pt_chrono_title_clr,
//                pt_title_clr
//            )// TBCV super init -> TSCV
//            setCustomContentViewChronometerTitleColour(
//                contentViewTimerCollapsed!!,
//                pt_chrono_title_clr,
//                pt_title_clr
//            )// TSCV init
//            setCustomContentViewMessageColour(contentViewTimer!!, pt_msg_clr)// TBCV super init -> TSCV
//            setCustomContentViewMessageColour(contentViewTimerCollapsed!!, pt_msg_clr)// TSCV init
//            setCustomContentViewMessageSummary(contentViewTimer!!, pt_msg_summary)// TBCV init
//            contentViewTimer!!.setChronometer(
//                R.id.chronometer,
//                SystemClock.elapsedRealtime() + timer_end,
//                null,
//                true
//            )// TBCV init
//            contentViewTimer!!.setChronometerCountDown(R.id.chronometer, true)// TBCV init
//            contentViewTimerCollapsed!!.setChronometer(
//                R.id.chronometer,
//                SystemClock.elapsedRealtime() + timer_end,
//                null,
//                true
//            )// TSCV init
//            contentViewTimerCollapsed!!.setChronometerCountDown(R.id.chronometer, true)// TSCV init
//            // --------------FACTORY---- START-------------
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent: PendingIntent
//            pIntent = if (deepLinkList != null) {
//                setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList!![0])
//            } else {
//                setPendingIntent(context, notificationId, extras, launchIntent, null)
//            }
//            // --------------FACTORY---- END-------------
//            nb = setNotificationBuilderBasics(
//                nb,
//                contentViewTimerCollapsed!!,
//                contentViewTimer!!,
//                pt_title,
//                pIntent
//            )
//            nb.setTimeoutAfter(timer_end.toLong())
//            setCustomContentViewBigImage(contentViewTimer!!, pt_big_img)// TBCV init
//            setCustomContentViewSmallIcon(contentViewTimer!!)// TBCV super init -> TSCV
//            setCustomContentViewSmallIcon(contentViewTimerCollapsed!!)// TSCV init
//            setCustomContentViewDotSep(contentViewTimer!!)// TBCV super init -> TSCV
//            setCustomContentViewDotSep(contentViewTimerCollapsed!!)// TSCV init
//
//            // it's ok to run Timer before notifying notification using nb since we have min 10 secs which is enough.
//            timerRunner(context, extras, notificationId, timer_end)
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating Timer notification ", t)
//        }
//        return nb
//    }

//    private fun renderInputBoxNotification(
//        context: Context,
//        extras: Bundle,
//        notificationId: Int,
//        nb: NotificationCompat.Builder
//    ): NotificationCompat.Builder {
//        var nb = nb
//        PTLog.debug("Rendering Input Box Template Push Notification with extras - $extras")
//        try {
//
//            //Set launchIntent to receiver
//            val launchIntent = Intent(context, PTPushNotificationReceiver::class.java)
//            val pIntent: PendingIntent
//            pIntent = if (deepLinkList != null && deepLinkList!!.size > 0) {
//                setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList!![0])
//            } else {
//                setPendingIntent(context, notificationId, extras, launchIntent, null)
//            }
//            nb.setSmallIcon(smallIcon)
//                .setContentTitle(pt_title)
//                .setContentText(pt_msg)
//                .setContentIntent(pIntent)
//                .setVibrate(longArrayOf(0L))
//                .setWhen(System.currentTimeMillis())
//                .setAutoCancel(true)
//
//            // Assign big picture notification
//            nb = setStandardViewBigImageStyle(pt_big_img, extras, context, nb)
//            if (pt_input_label != null && pt_input_label!!.isNotEmpty()) {
//                //Initialise RemoteInput
//                val remoteInput = RemoteInput.Builder(PTConstants.PT_INPUT_KEY)
//                    .setLabel(pt_input_label)
//                    .build()
//
//                //Set launchIntent to receiver
//                val replyIntent = Intent(context, PushTemplateReceiver::class.java)
//                replyIntent.putExtra(PTConstants.PT_INPUT_FEEDBACK, pt_input_feedback)
//                replyIntent.putExtra(PTConstants.PT_INPUT_AUTO_OPEN, pt_input_auto_open)
//                replyIntent.putExtra("config", config)
//                val replyPendingIntent: PendingIntent
//                replyPendingIntent = if (deepLinkList != null) {
//                    setPendingIntent(
//                        context,
//                        notificationId,
//                        extras,
//                        replyIntent,
//                        deepLinkList!![0]
//                    )
//                } else {
//                    setPendingIntent(context, notificationId, extras, replyIntent, null)
//                }
//
//                //Notification Action with RemoteInput instance added.
//                val replyAction = NotificationCompat.Action.Builder(
//                    android.R.drawable.sym_action_chat, pt_input_label, replyPendingIntent
//                )
//                    .addRemoteInput(remoteInput)
//                    .setAllowGeneratedReplies(true)
//                    .build()
//
//
//                //Notification.Action instance added to Notification Builder.
//                nb.addAction(replyAction)
//            }
//            if (pt_dismiss_on_click != null) if (pt_dismiss_on_click!!.isNotEmpty()) extras.putString(
//                PTConstants.PT_DISMISS_ON_CLICK, pt_dismiss_on_click
//            )
//            setActionButtons(context, extras, notificationId, nb)
//        } catch (t: Throwable) {
//            PTLog.verbose("Error creating Input Box notification ", t)
//        }
//        return nb
//    }

//    private fun setPendingIntent(
//        context: Context,
//        notificationId: Int,
//        extras: Bundle,
//        launchIntent: Intent,
//        dl: String?
//    ): PendingIntent {
//        launchIntent.putExtras(extras)
//        launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
//        if (dl != null) {
//            launchIntent.putExtra(PTConstants.DEFAULT_DL, true)
//            launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl)
//        }
//        launchIntent.removeExtra(Constants.WZRK_ACTIONS)
//        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
//        launchIntent.flags =
//            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//        return PendingIntent.getBroadcast(
//            context, System.currentTimeMillis().toInt(),
//            launchIntent, PendingIntent.FLAG_UPDATE_CURRENT
//        )
//    }

    //    private fun setNotificationBuilderBasics(
//        notificationBuilder: NotificationCompat.Builder,
//        contentViewSmall: RemoteViews,
//        contentViewBig: RemoteViews,
//        pt_title: String?,
//        pIntent: PendingIntent
//    ): NotificationCompat.Builder {
//        return notificationBuilder.setSmallIcon(smallIcon)
//            .setCustomContentView(contentViewSmall)
//            .setCustomBigContentView(contentViewBig)
//            .setContentTitle(Html.fromHtml(pt_title))
//            .setContentIntent(pIntent)
//            .setVibrate(longArrayOf(0L))
//            .setWhen(System.currentTimeMillis())
//            .setAutoCancel(true)
//    }
//
//    private fun setStandardViewBigImageStyle(
//        pt_big_img: String?,
//        extras: Bundle,
//        context: Context,
//        notificationBuilder: NotificationCompat.Builder
//    ): NotificationCompat.Builder {
//        var bigPictureStyle: NotificationCompat.Style
//        if (pt_big_img != null && pt_big_img.startsWith("http")) {
//            try {
//                val bpMap = Utils.getNotificationBitmap(pt_big_img, false, context)
//                    ?: throw Exception("Failed to fetch big picture!")
//                bigPictureStyle = if (extras.containsKey(PTConstants.PT_MSG_SUMMARY)) {
//                    val summaryText = pt_msg_summary
//                    NotificationCompat.BigPictureStyle()
//                        .setSummaryText(summaryText)
//                        .bigPicture(bpMap)
//                } else {
//                    NotificationCompat.BigPictureStyle()
//                        .setSummaryText(pt_msg)
//                        .bigPicture(bpMap)
//                }
//            } catch (t: Throwable) {
//                bigPictureStyle = NotificationCompat.BigTextStyle()
//                    .bigText(pt_msg)
//                PTLog.verbose(
//                    "Falling back to big text notification, couldn't fetch big picture",
//                    t
//                )
//            }
//        } else {
//            bigPictureStyle = NotificationCompat.BigTextStyle()
//                .bigText(pt_msg)
//        }
//        notificationBuilder.setStyle(bigPictureStyle)
//        return notificationBuilder
//    }
//
//    private fun setCustomContentViewLargeIcon(contentView: RemoteViews, pt_large_icon: String?) {
//        if (pt_large_icon != null && pt_large_icon.isNotEmpty()) {
//            Utils.loadImageURLIntoRemoteView(R.id.large_icon, pt_large_icon, contentView)
//        } else {
//            contentView.setViewVisibility(R.id.large_icon, View.GONE)
//        }
//    }
//
//    private fun setCustomContentViewBasicKeys(contentView: RemoteViews, context: Context) {
//        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
//        contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context))
//        if (pt_subtitle != null && pt_subtitle!!.isNotEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                contentView.setTextViewText(
//                    R.id.subtitle,
//                    Html.fromHtml(pt_subtitle, Html.FROM_HTML_MODE_LEGACY)
//                )
//            } else {
//                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle))
//            }
//        } else {
//            contentView.setViewVisibility(R.id.subtitle, View.GONE)
//            contentView.setViewVisibility(R.id.sep_subtitle, View.GONE)
//        }
//        if (pt_meta_clr != null && pt_meta_clr!!.isNotEmpty()) {
//            contentView.setTextColor(
//                R.id.app_name,
//                Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
//            )
//            contentView.setTextColor(
//                R.id.timestamp,
//                Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
//            )
//            contentView.setTextColor(
//                R.id.subtitle,
//                Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
//            )
//            setDotSep(context)
//        }
//    }
//
//    private fun setCustomContentViewButtonColour(
//        contentView: RemoteViews,
//        resourceID: Int,
//        pt_product_display_action_clr: String?
//    ) {
//        if (pt_product_display_action_clr != null && pt_product_display_action_clr.isNotEmpty()) {
//            contentView.setInt(
//                resourceID,
//                "setBackgroundColor",
//                Utils.getColour(
//                    pt_product_display_action_clr,
//                    PTConstants.PT_PRODUCT_DISPLAY_ACTION_CLR_DEFAULTS
//                )
//            )
//        }
//    }
//
//    private fun setCustomContentViewButtonLabel(
//        contentView: RemoteViews,
//        resourceID: Int,
//        pt_product_display_action: String?
//    ) {
//        if (pt_product_display_action != null && pt_product_display_action.isNotEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                contentView.setTextViewText(
//                    resourceID,
//                    Html.fromHtml(pt_product_display_action, Html.FROM_HTML_MODE_LEGACY)
//                )
//            } else {
//                contentView.setTextViewText(resourceID, Html.fromHtml(pt_product_display_action))
//            }
//        }
//    }
//
//    private fun setCustomContentViewButtonText(
//        contentView: RemoteViews,
//        resourceID: Int,
//        pt_product_display_action_text_clr: String?
//    ) {
//        if (pt_product_display_action_text_clr != null && pt_product_display_action_text_clr.isNotEmpty()) {
//            contentView.setTextColor(
//                resourceID,
//                Utils.getColour(
//                    pt_product_display_action_text_clr,
//                    PTConstants.PT_PRODUCT_DISPLAY_ACTION_TEXT_CLR_DEFAULT
//                )
//            )
//        }
//    }
//
//    private fun setCustomContentViewBigImage(contentView: RemoteViews, pt_big_img: String?) {
//        if (pt_big_img != null && pt_big_img.isNotEmpty()) {
//            Utils.loadImageURLIntoRemoteView(R.id.big_image, pt_big_img, contentView)
//            if (Utils.getFallback()) {
//                contentView.setViewVisibility(R.id.big_image, View.GONE)
//            }
//        } else {
//            contentView.setViewVisibility(R.id.big_image, View.GONE)
//        }
//    }
//    private fun setCustomContentViewMessageSummary(
//        contentView: RemoteViews,
//        pt_msg_summary: String?
//    ) {
//        if (pt_msg_summary != null && pt_msg_summary.isNotEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                contentView.setTextViewText(
//                    R.id.msg,
//                    Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY)
//                )
//            } else {
//                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary))
//            }
//        }
//    }
//
//    private fun setCustomContentViewMessageColour(contentView: RemoteViews, pt_msg_clr: String?) {
//        if (pt_msg_clr != null && pt_msg_clr.isNotEmpty()) {
//            contentView.setTextColor(
//                R.id.msg,
//                Utils.getColour(pt_msg_clr, PTConstants.PT_COLOUR_BLACK)
//            )
//        }
//    }
//
//    private fun setCustomContentViewTitleColour(contentView: RemoteViews, pt_title_clr: String?) {
//        if (pt_title_clr != null && pt_title_clr.isNotEmpty()) {
//            contentView.setTextColor(
//                R.id.title,
//                Utils.getColour(pt_title_clr, PTConstants.PT_COLOUR_BLACK)
//            )
//        }
//    }
//
//    private fun setCustomContentViewElementColour(
//        contentView: RemoteViews,
//        rId: Int,
//        colour: String?
//    ) {
//        if (colour != null && colour.isNotEmpty()) {
//            contentView.setTextColor(rId, Utils.getColour(colour, PTConstants.PT_COLOUR_BLACK))
//        }
//    }
//
//    private fun setCustomContentViewChronometerTitleColour(
//        contentView: RemoteViews,
//        pt_chrono_title_clr: String?,
//        pt_title_clr: String?
//    ) {
//        if (pt_chrono_title_clr != null && pt_chrono_title_clr.isNotEmpty()) {
//            contentView.setTextColor(
//                R.id.chronometer,
//                Utils.getColour(pt_chrono_title_clr, PTConstants.PT_COLOUR_BLACK)
//            )
//        } else {
//            if (pt_title_clr != null && pt_title_clr.isNotEmpty()) {
//                contentView.setTextColor(
//                    R.id.chronometer,
//                    Utils.getColour(pt_title_clr, PTConstants.PT_COLOUR_BLACK)
//                )
//            }
//        }
//    }
//
//    private fun setCustomContentViewExpandedBackgroundColour(
//        contentView: RemoteViews,
//        pt_bg: String?
//    ) {
//        if (pt_bg != null && pt_bg.isNotEmpty()) {
//            contentView.setInt(
//                R.id.content_view_big,
//                "setBackgroundColor",
//                Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE)
//            )
//        }
//    }
//
//    private fun setCustomContentViewCollapsedBackgroundColour(
//        contentView: RemoteViews,
//        pt_bg: String?
//    ) {
//        if (pt_bg != null && pt_bg.isNotEmpty()) {
//            contentView.setInt(
//                R.id.content_view_small,
//                "setBackgroundColor",
//                Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE)
//            )
//        }
//    }
//
//    private fun setCustomContentViewChronometerBackgroundColour(
//        contentView: RemoteViews,
//        pt_bg: String?
//    ) {
//        if (pt_bg != null && pt_bg.isNotEmpty()) {
//            contentView.setInt(
//                R.id.chronometer,
//                "setBackgroundColor",
//                Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE)
//            )
//        }
//    }
//
//    private fun setCustomContentViewMessage(contentView: RemoteViews, pt_msg: String?) {
//        if (pt_msg != null && pt_msg.isNotEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                contentView.setTextViewText(
//                    R.id.msg,
//                    Html.fromHtml(pt_msg, Html.FROM_HTML_MODE_LEGACY)
//                )
//            } else {
//                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg))
//            }
//        }
//    }
//
//    private fun setCustomContentViewTitle(contentView: RemoteViews, pt_title: String?) {
//        if (pt_title != null && pt_title.isNotEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                contentView.setTextViewText(
//                    R.id.title,
//                    Html.fromHtml(pt_title, Html.FROM_HTML_MODE_LEGACY)
//                )
//            } else {
//                contentView.setTextViewText(R.id.title, Html.fromHtml(pt_title))
//            }
//        }
//    }
//
//    private fun setActionButtons(
//        context: Context,
//        extras: Bundle,
//        notificationId: Int,
//        nb: NotificationCompat.Builder
//    ) {
//        var clazz: Class<*>? = null
//        try {
//            clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
//        } catch (ex: ClassNotFoundException) {
//            PTLog.debug("No Intent Service found")
//        }
//        val isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz)
//        if (actions != null && actions!!.length() > 0) {
//            for (i in 0 until actions!!.length()) {
//                try {
//                    val action = actions!!.getJSONObject(i)
//                    val label = action.optString("l")
//                    val dl = action.optString("dl")
//                    val ico = action.optString(PTConstants.PT_NOTIF_ICON)
//                    val id = action.optString("id")
//                    val autoCancel = action.optBoolean("ac", true)
//                    if (label.isEmpty() || id.isEmpty()) {
//                        PTLog.debug("not adding push notification action: action label or id missing")
//                        continue
//                    }
//                    var icon = 0
//                    if (ico.isNotEmpty()) {
//                        try {
//                            icon = context.resources.getIdentifier(
//                                ico,
//                                "drawable",
//                                context.packageName
//                            )
//                        } catch (t: Throwable) {
//                            PTLog.debug("unable to add notification action icon: " + t.localizedMessage)
//                        }
//                    }
//                    val sendToPTIntentService = autoCancel && isPTIntentServiceAvailable
//                    var actionLaunchIntent: Intent?
//                    if (sendToPTIntentService) {
//                        actionLaunchIntent = Intent(CTNotificationIntentService.MAIN_ACTION)
//                        actionLaunchIntent.setPackage(context.packageName)
//                        actionLaunchIntent.putExtra(
//                            PTConstants.PT_TYPE,
//                            CTNotificationIntentService.TYPE_BUTTON_CLICK
//                        )
//                        if (dl.isNotEmpty()) {
//                            actionLaunchIntent.putExtra("dl", dl)
//                        }
//                    } else {
//                        actionLaunchIntent = if (dl.isNotEmpty()) {
//                            Intent(Intent.ACTION_VIEW, Uri.parse(dl))
//                        } else {
//                            context.packageManager.getLaunchIntentForPackage(context.packageName)
//                        }
//                    }
//                    if (actionLaunchIntent != null) {
//                        actionLaunchIntent.putExtras(extras)
//                        actionLaunchIntent.removeExtra(Constants.WZRK_ACTIONS)
//                        actionLaunchIntent.putExtra(PTConstants.PT_ACTION_ID, id)
//                        actionLaunchIntent.putExtra("autoCancel", autoCancel)
//                        actionLaunchIntent.putExtra("wzrk_c2a", id)
//                        actionLaunchIntent.putExtra("notificationId", notificationId)
//                        actionLaunchIntent.flags =
//                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//                    }
//                    var actionIntent: PendingIntent? = null
//                    val requestCode = System.currentTimeMillis().toInt() + i
//                    actionIntent = if (sendToPTIntentService) {
//                        PendingIntent.getService(
//                            context, requestCode,
//                            actionLaunchIntent!!, PendingIntent.FLAG_UPDATE_CURRENT
//                        )
//                    } else {
//                        PendingIntent.getActivity(
//                            context, requestCode,
//                            actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT
//                        )
//                    }
//                    nb.addAction(icon, label, actionIntent)
//                } catch (t: Throwable) {
//                    PTLog.debug("error adding notification action : " + t.localizedMessage)
//                }
//            }
//        }
//    }
//
//
//    private fun setCustomContentViewSmallIcon(contentView: RemoteViews) {
//        if (pt_small_icon != null) {
//            Utils.loadImageBitmapIntoRemoteView(R.id.small_icon, pt_small_icon, contentView)
//        } else {
//            Utils.loadImageRidIntoRemoteView(R.id.small_icon, smallIcon, contentView)
//        }
//    }
//
//    private fun setCustomContentViewDotSep(contentView: RemoteViews) {
//        if (pt_dot_sep != null) {
//            Utils.loadImageBitmapIntoRemoteView(R.id.sep, pt_dot_sep, contentView)
//            Utils.loadImageBitmapIntoRemoteView(R.id.sep_subtitle, pt_dot_sep, contentView)
//        }
//    }
//
//    private fun setNotificationBuilderBasics(
//        notificationBuilder: NotificationCompat.Builder,
//        contentViewSmall: RemoteViews,
//        contentViewBig: RemoteViews,
//        pt_title: String?,
//        pIntent: PendingIntent,
//        dIntent: PendingIntent
//    ): NotificationCompat.Builder {
//        return notificationBuilder.setSmallIcon(smallIcon)
//            .setCustomContentView(contentViewSmall)
//            .setCustomBigContentView(contentViewBig)
//            .setContentTitle(pt_title)
//            .setDeleteIntent(dIntent)
//            .setContentIntent(pIntent)
//            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
//            .setWhen(System.currentTimeMillis())
//            .setAutoCancel(true)
//    }

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
        if (pt_small_icon_clr == null || pt_small_icon_clr!!.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_COLOR)
        }
        if (pt_collapse_key == null) {
            pt_collapse_key = extras[Constants.WZRK_COLLAPSE]
        }
    }

//    private fun setDismissIntent(context: Context, extras: Bundle, intent: Intent): PendingIntent {
//        intent.putExtras(extras)
//        intent.putExtra(PTConstants.PT_DISMISS_INTENT, true)
//        return PendingIntent.getBroadcast(
//            context, System.currentTimeMillis().toInt(),
//            intent, PendingIntent.FLAG_CANCEL_CURRENT
//        )
//    }
//
//    private fun setCustomCTA(context: Context, extras: Bundle, contentView: RemoteViews) {
//        val customCTAList = Utils.getCustomCTAListFromExtras(extras)
//        if (customCTAList.size != 0) {
//            var isCustomCTASet = false
//            val tempRemoteView = RemoteViews(context.packageName, R.layout.pt_custom_cta)
//            val customCTAIds = ArrayList<Int>()
//            customCTAIds.add(R.id.pt_custom_cta1)
//            customCTAIds.add(R.id.pt_custom_cta2)
//            customCTAIds.add(R.id.pt_custom_cta3)
//            for (index in customCTAIds.indices) {
//                try {
//                    val cta = Utils.toJsonObject(
//                        customCTAList[index]
//                    )
//                    val bgClr = cta.getString(PTConstants.PT_CUSTOM_CTA_BG_CLR)
//                    val textClr = cta.getString(PTConstants.PT_CUSTOM_CTA_TEXT_CLR)
//                    val text = cta.getString(PTConstants.PT_CUSTOM_CTA_TEXT)
//                    val dl = cta.getString(PTConstants.PT_CUSTOM_CTA_DL)
//                    setCustomContentViewText(tempRemoteView, customCTAIds[index], text)
//                    tempRemoteView.setInt(
//                        customCTAIds[index],
//                        "setTextColor",
//                        Utils.getColour(textClr, "black")
//                    )
//                    tempRemoteView.setInt(
//                        customCTAIds[index],
//                        "setBackgroundColor",
//                        Utils.getColour(bgClr, "white")
//                    )
//                    val buttonIntent = Intent(context, PushTemplateReceiver::class.java)
//                    buttonIntent.putExtras(extras)
//                    val contentRightPos0Intent = setPendingIntent(
//                        context, extras.getInt(
//                            PTConstants.PT_NOTIF_ID
//                        ), extras, buttonIntent, dl
//                    )
//                    tempRemoteView.setOnClickPendingIntent(
//                        customCTAIds[index],
//                        contentRightPos0Intent
//                    )
//                    isCustomCTASet = true
//                } catch (e: JSONException) {
//                    tempRemoteView.setViewVisibility(customCTAIds[index], View.GONE)
//                    PTLog.debug("Unable to add Custom CTA with payload: " + customCTAList[index], e)
//                }
//            }
//            if (isCustomCTASet) {
//                contentView.addView(R.id.content_view_big, tempRemoteView)
//            }
//        }
//    }
//
//    private fun setNotificationId(notificationId: Int): Int {//TODO Check this
//        var notificationId = notificationId
//        if (notificationId == Constants.EMPTY_NOTIFICATION_ID) {
//            notificationId = (Math.random() * 100).toInt()
//        }
//        return notificationId
//    }

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