package com.clevertap.android.pushtemplates;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.RemoteInput;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

import static android.content.Context.NOTIFICATION_SERVICE;

@SuppressWarnings({"FieldCanBeLocal", "rawtypes"})
public class TemplateRenderer implements INotificationRenderer{

    private static int debugLevel = TemplateRenderer.LogLevel.INFO.intValue();
    private String pt_id;
    private TemplateType templateType;
    private String pt_title;
    private String pt_msg;
    private String pt_msg_summary;
    private String pt_large_icon;
    private String pt_big_img;
    private String pt_title_clr, pt_msg_clr, pt_chrono_title_clr;
    private ArrayList<String> imageList;
    private ArrayList<String> deepLinkList;
    private ArrayList<String> bigTextList;
    private ArrayList<String> smallTextList;
    private ArrayList<String> priceList;
    private String pt_product_display_action;
    private String pt_product_display_action_clr;
    private String pt_bg;
    private String pt_rating_default_dl;
    private String pt_small_view;
    private RemoteViews contentViewBig, contentViewSmall, contentViewCarousel, contentViewRating,
            contentFiveCTAs, contentViewTimer, contentViewTimerCollapsed, contentViewManualCarousel;
    private String channelId;
    private int smallIcon = 0;
    private int pt_dot = 0;
    private boolean requiresChannelId;
    private NotificationManager notificationManager;
    private AsyncHelper asyncHelper;
//    private DBHelper dbHelper;
    private int pt_timer_threshold;
    private String pt_input_label;
    private String pt_input_feedback;
    private String pt_input_auto_open;
    private String pt_dismiss_on_click;
    private int pt_timer_end;
    private String pt_title_alt;
    private String pt_msg_alt;
    private String pt_big_img_alt;
    private String pt_product_display_linear;
    private String pt_meta_clr;
    private String pt_product_display_action_text_clr;
    private String pt_small_icon_clr;
    private Bitmap pt_small_icon;
    private Bitmap pt_dot_sep;
    private String pt_cancel_notif_id;
    private ArrayList<Integer> pt_cancel_notif_ids;
    private JSONArray actions;
    private String pt_subtitle;
    private String pID;
    private int pt_flip_interval;
    private Object pt_collapse_key;
    private String pt_manual_carousel_type;
    private CleverTapInstanceConfig config;
    public final static String MAIN_ACTION = "com.clevertap.PT_PUSH_EVENT";
    public final static String TYPE_BUTTON_CLICK = "com.clevertap.ACTION_BUTTON_CLICK";

    @SuppressWarnings({"unused"})
    public enum LogLevel {
        OFF(-1),
        INFO(0),
        DEBUG(2),
        VERBOSE(3);

        private final int value;

        LogLevel(final int newValue) {
            value = newValue;
        }

        public int intValue() {
            return value;
        }
    }

    /**
     * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
     * Debug messages are tagged as PTLog.
     *
     * @param level Can be one of the following:  -1 (disables all debugging), 0 (default, shows minimal SDK integration related logging),
     *              2(shows debug output)
     */
    public static void setDebugLevel(int level) {
        debugLevel = level;
    }

    /**
     * Returns the log level set for PushTemplates
     *
     * @return The int value
     */
    @SuppressWarnings("WeakerAccess")
    public static int getDebugLevel() {
        return debugLevel;
    }

    TemplateRenderer(Context context, Bundle extras) {
        setUp(context, extras, null);
    }

    private TemplateRenderer(Context context, Bundle extras, CleverTapInstanceConfig config) {
        setUp(context, extras, config);
    }

    @Override
    public String getMessage(final Bundle extras) {
        return pt_msg;// TODO: Check if set properly before caller calls this
    }

    @Override
    public String getTitle(final Bundle extras, Context context) {
        return pt_title;// TODO: Check if set properly before caller calls this
    }

    @Override
    public Builder renderNotification(final Bundle extras, final Context context, final Builder nb,
            final CleverTapInstanceConfig config,
            final int notificationId) {

        if (pt_id == null) {
            PTLog.verbose("Template ID not provided. Cannot create the notification");
            return null;
        }

        switch (templateType) {
            case BASIC:
                if (hasAllBasicNotifKeys())
                    return renderBasicTemplateNotification(context, extras, notificationId,nb);
                break;
            case AUTO_CAROUSEL:
                if (hasAllCarouselNotifKeys())
                    return renderAutoCarouselNotification(context, extras, notificationId,nb);
                break;
            case MANUAL_CAROUSEL:
                if (hasAllManualCarouselNotifKeys())
                    return renderManualCarouselNotification(context, extras, notificationId,nb);
                break;
            case RATING:
                if (hasAllRatingNotifKeys())
                    return renderRatingNotification(context, extras, notificationId,nb);
                break;
            case FIVE_ICONS:
                if (hasAll5IconNotifKeys())
                    return renderFiveIconNotification(context, extras, notificationId,nb);
                break;
            case PRODUCT_DISPLAY:
                if (hasAllProdDispNotifKeys())
                    return renderProductDisplayNotification(context, extras, notificationId,nb);
                break;
            case ZERO_BEZEL:
                if (hasAllZeroBezelNotifKeys())
                    return renderZeroBezelNotification(context, extras, notificationId,nb);
                break;
            case TIMER: // TODO PENDING : design issue
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (hasAllTimerKeys()) {
                        renderTimerNotification(context, extras, notificationId,nb);
                    }
                } else {
                    PTLog.debug("Push Templates SDK supports Timer Notifications only on or above Android Nougat, reverting to basic template");
                    if (hasAllBasicNotifKeys()) {
                        return renderBasicTemplateNotification(context, extras, notificationId,nb);
                    }
                }
                break;
            case INPUT_BOX:
                if (hasAllInputBoxKeys())
                    return renderInputBoxNotification(context, extras, notificationId,nb);
                break;
            case CANCEL:// TODO: PENDING, diff use case not fitting into design, design issue
                renderCancelNotification();
                break;
        }

        return null;
    }

    private Builder renderZeroBezelNotification(final Context context, final Bundle extras, final int notificationId,
            Builder nb) {
        PTLog.debug("Rendering Zero Bezel Template Push Notification with extras - " + extras.toString());
        try {
            contentViewBig = new RemoteViews(context.getPackageName(), R.layout.zero_bezel);
            setCustomContentViewBasicKeys(contentViewBig, context);

            boolean textOnlySmallView = pt_small_view != null && pt_small_view.equals(Constants.TEXT_ONLY);

            if (textOnlySmallView) {
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.cv_small_text_only);
            } else {
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.cv_small_zero_bezel);
            }
            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewBig, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewBig, pt_msg);

            if (textOnlySmallView) {
                contentViewSmall.setViewVisibility(R.id.msg, View.GONE);
            } else {
                setCustomContentViewMessage(contentViewSmall, pt_msg);
            }

            setCustomContentViewMessageSummary(contentViewBig, pt_msg_summary);

            setCustomContentViewTitleColour(contentViewBig, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewExpandedBackgroundColour(contentViewBig, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewMessageColour(contentViewBig, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            nb = setNotificationBuilderBasics(nb, contentViewSmall, contentViewBig, pt_title, pIntent);

            setCustomContentViewBigImage(contentViewBig, pt_big_img);

            if (!textOnlySmallView) {
                setCustomContentViewBigImage(contentViewSmall, pt_big_img);
            }

            if (textOnlySmallView) {
                setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            }

            setCustomContentViewSmallIcon(contentViewBig);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewBig);
            setCustomContentViewDotSep(contentViewSmall);


            if (Utils.getFallback()) {
                PTLog.debug("Image not fetched, falling back to Basic Template");
                return renderBasicTemplateNotification(context, extras, notificationId,nb);// will overwrite set values on nb
            }
        } catch (Throwable t) {
            PTLog.verbose("Error creating image only notification", t);
        }
        return nb;
    }

    private Builder renderProductDisplayNotification(final Context context, final Bundle extras,
            final int notificationId, Builder nb) {
        PTLog.debug("Rendering Product Display Template Push Notification with extras - " + extras.toString());
        try {
            boolean isLinear = false;

            if (pt_product_display_linear == null || pt_product_display_linear.isEmpty()) {
                contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_template);
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);
            } else {
                isLinear = true;
                contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_expanded);
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_collapsed);
            }

            setCustomContentViewBasicKeys(contentViewBig, context);
            if (!isLinear) {
                setCustomContentViewBasicKeys(contentViewSmall, context);
            }
            if (!bigTextList.isEmpty()) {
                setCustomContentViewText(contentViewBig, R.id.product_name, bigTextList.get(0));
            }

            if (!isLinear) {
                if (!smallTextList.isEmpty()) {
                    setCustomContentViewText(contentViewBig, R.id.product_description, smallTextList.get(0));
                }
            }

            if (!priceList.isEmpty()) {
                setCustomContentViewText(contentViewBig, R.id.product_price, priceList.get(0));
            }

            if (!isLinear) {
                setCustomContentViewTitle(contentViewBig, pt_title);
                setCustomContentViewTitle(contentViewSmall, pt_title);
                setCustomContentViewMessage(contentViewBig, pt_msg);
                setCustomContentViewElementColour(contentViewBig, R.id.product_description, pt_msg_clr);
                setCustomContentViewElementColour(contentViewBig, R.id.product_name, pt_title_clr);
                setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);
            }

            setCustomContentViewMessage(contentViewSmall, pt_msg);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewExpandedBackgroundColour(contentViewBig, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewButtonLabel(contentViewBig, R.id.product_action, pt_product_display_action);
            setCustomContentViewButtonColour(contentViewBig, R.id.product_action, pt_product_display_action_clr);
            setCustomContentViewButtonText(contentViewBig, R.id.product_action, pt_product_display_action_text_clr);

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);

            if (!isLinear) {
                setCustomContentViewSmallIcon(contentViewSmall);
                setCustomContentViewDotSep(contentViewSmall);
            }

            int imageCounter = 0;
            boolean isFirstImageOk = false;

            ArrayList<Integer> smallImageLayoutIds = new ArrayList<>();
            smallImageLayoutIds.add(R.id.small_image1);
            smallImageLayoutIds.add(R.id.small_image2);
            smallImageLayoutIds.add(R.id.small_image3);
            ArrayList<Integer> smallCollapsedImageLayoutIds = new ArrayList<>();
            smallCollapsedImageLayoutIds.add(R.id.small_image1_collapsed);
            smallCollapsedImageLayoutIds.add(R.id.small_image2_collapsed);
            smallCollapsedImageLayoutIds.add(R.id.small_image3_collapsed);
            ArrayList<String> tempImageList = new ArrayList<>();

            for (int index = 0; index < imageList.size(); index++) {
                if (isLinear) {
                    Utils.loadImageURLIntoRemoteView(smallCollapsedImageLayoutIds.get(imageCounter), imageList.get(index), contentViewSmall);
                    contentViewSmall.setViewVisibility(smallCollapsedImageLayoutIds.get(imageCounter), View.VISIBLE);
                    if (!Utils.getFallback()) {
                        contentViewSmall.setViewVisibility(smallCollapsedImageLayoutIds.get(imageCounter), View.VISIBLE);
                    }
                }
                Utils.loadImageURLIntoRemoteView(smallImageLayoutIds.get(imageCounter), imageList.get(index), contentViewBig);
                RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.image_view);
                Utils.loadImageURLIntoRemoteView(R.id.fimg, imageList.get(index), tempRemoteView);
                if (!Utils.getFallback()) {
                    if (!isFirstImageOk) {
                        isFirstImageOk = true;
                    }
                    contentViewBig.setViewVisibility(smallImageLayoutIds.get(imageCounter), View.VISIBLE);
                    contentViewBig.addView(R.id.carousel_image, tempRemoteView);
                    imageCounter++;
                    tempImageList.add(imageList.get(index));
                } else {
                    deepLinkList.remove(index);
                    bigTextList.remove(index);
                    smallTextList.remove(index);
                    priceList.remove(index);
                }
            }

            extras.putStringArrayList(Constants.PT_IMAGE_LIST, tempImageList);
            extras.putStringArrayList(Constants.PT_DEEPLINK_LIST, deepLinkList);
            extras.putStringArrayList(Constants.PT_BIGTEXT_LIST, bigTextList);
            extras.putStringArrayList(Constants.PT_SMALLTEXT_LIST, smallTextList);
            extras.putStringArrayList(Constants.PT_PRICE_LIST, priceList);


            int requestCode1 = new Random().nextInt();
            int requestCode2 = new Random().nextInt();
            int requestCode3 = new Random().nextInt();

            Intent notificationIntent1 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent1.putExtra(Constants.PT_CURRENT_POSITION, 0);
            notificationIntent1.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent1.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(0));
            notificationIntent1.putExtras(extras);
            PendingIntent contentIntent1 = PendingIntent.getBroadcast(context, requestCode1, notificationIntent1, 0);
            contentViewBig.setOnClickPendingIntent(R.id.small_image1, contentIntent1);

            if (deepLinkList.size() >= 2) {
                Intent notificationIntent2 = new Intent(context, PushTemplateReceiver.class);
                notificationIntent2.putExtra(Constants.PT_CURRENT_POSITION, 1);
                notificationIntent2.putExtra(Constants.PT_NOTIF_ID, notificationId);
                notificationIntent2.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(1));
                notificationIntent2.putExtras(extras);
                PendingIntent contentIntent2 = PendingIntent.getBroadcast(context, requestCode2, notificationIntent2, 0);
                contentViewBig.setOnClickPendingIntent(R.id.small_image2, contentIntent2);
            }

            if (deepLinkList.size() >= 3) {
                Intent notificationIntent3 = new Intent(context, PushTemplateReceiver.class);
                notificationIntent3.putExtra(Constants.PT_CURRENT_POSITION, 2);
                notificationIntent3.putExtra(Constants.PT_NOTIF_ID, notificationId);
                notificationIntent3.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(2));
                notificationIntent3.putExtras(extras);
                PendingIntent contentIntent3 = PendingIntent.getBroadcast(context, requestCode3, notificationIntent3, 0);
                contentViewBig.setOnClickPendingIntent(R.id.small_image3, contentIntent3);
            }
            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra(Constants.PT_IMAGE_1, true);
            notificationIntent4.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(0));
            notificationIntent4.putExtra(Constants.PT_BUY_NOW, true);
            notificationIntent4.putExtra("config", config);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent4, 0);
            contentViewBig.setOnClickPendingIntent(R.id.product_action, contentIntent4);

            if (isLinear) {
                Intent notificationSmallIntent1 = new Intent(context, PTPushNotificationReceiver.class);
                PendingIntent contentSmallIntent1 = setPendingIntent(context, notificationId, extras, notificationSmallIntent1, deepLinkList.get(0));
                contentViewSmall.setOnClickPendingIntent(R.id.small_image1_collapsed, contentSmallIntent1);
                if (deepLinkList.size() >= 2) {
                    Intent notificationSmallIntent2 = new Intent(context, PTPushNotificationReceiver.class);
                    PendingIntent contentSmallIntent2 = setPendingIntent(context, notificationId, extras, notificationSmallIntent2, deepLinkList.get(1));
                    contentViewSmall.setOnClickPendingIntent(R.id.small_image2_collapsed, contentSmallIntent2);
                }
                if (deepLinkList.size() >= 3) {
                    Intent notificationSmallIntent3 = new Intent(context, PTPushNotificationReceiver.class);
                    PendingIntent contentSmallIntent3 = setPendingIntent(context, notificationId, extras, notificationSmallIntent3, deepLinkList.get(2));
                    contentViewSmall.setOnClickPendingIntent(R.id.small_image3_collapsed, contentSmallIntent3);
                }
            }

            Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            nb = setNotificationBuilderBasics(nb, contentViewSmall, contentViewBig, pt_title, pIntent, dIntent);

            setCustomContentViewDotSep(contentViewBig);

            setCustomContentViewSmallIcon(contentViewBig);

            if (imageCounter <= 1) {
                PTLog.debug("2 or more images are not retrievable, not displaying the notification.");
                return null;
            }

        } catch (Throwable t) {
            PTLog.verbose("Error creating Product Display Notification ", t);
        }
        return nb;
    }

    private Builder renderFiveIconNotification(final Context context, final Bundle extras, final int notificationId,
            Builder nb) {

        PTLog.debug("Rendering Five Icon Template Push Notification with extras - " + extras.toString());
        try {

            if (pt_title == null || pt_title.isEmpty()) {
                pt_title = Utils.getApplicationName(context);
            }
            contentFiveCTAs = new RemoteViews(context.getPackageName(), R.layout.five_cta);

            setCustomContentViewExpandedBackgroundColour(contentFiveCTAs, pt_bg);

            int reqCode1 = new Random().nextInt();
            int reqCode2 = new Random().nextInt();
            int reqCode3 = new Random().nextInt();
            int reqCode4 = new Random().nextInt();
            int reqCode5 = new Random().nextInt();
            int reqCode6 = new Random().nextInt();

            Intent notificationIntent1 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent1.putExtra("cta1", true);
            notificationIntent1.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent1.putExtras(extras);
            PendingIntent contentIntent1 = PendingIntent.getBroadcast(context, reqCode1, notificationIntent1, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta1, contentIntent1);

            Intent notificationIntent2 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent2.putExtra("cta2", true);
            notificationIntent2.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent2.putExtras(extras);
            PendingIntent contentIntent2 = PendingIntent.getBroadcast(context, reqCode2, notificationIntent2, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta2, contentIntent2);

            Intent notificationIntent3 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent3.putExtra("cta3", true);
            notificationIntent3.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent3.putExtras(extras);
            PendingIntent contentIntent3 = PendingIntent.getBroadcast(context, reqCode3, notificationIntent3, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta3, contentIntent3);

            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra("cta4", true);
            notificationIntent4.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, reqCode4, notificationIntent4, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta4, contentIntent4);

            Intent notificationIntent5 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent5.putExtra("cta5", true);
            notificationIntent5.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent5.putExtras(extras);
            PendingIntent contentIntent5 = PendingIntent.getBroadcast(context, reqCode5, notificationIntent5, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta5, contentIntent5);

            Intent notificationIntent6 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent6.putExtra("close", true);
            notificationIntent6.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent6.putExtras(extras);
            PendingIntent contentIntent6 = PendingIntent.getBroadcast(context, reqCode6, notificationIntent6, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.close, contentIntent6);


            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);

            nb = setNotificationBuilderBasics(nb, contentFiveCTAs, contentFiveCTAs, pt_title, pIntent);

            nb.setOngoing(true);

            int imageCounter = 0;
            for (int imageKey = 0; imageKey < imageList.size(); imageKey++) {
                if (imageKey == 0) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta1, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        contentFiveCTAs.setViewVisibility(R.id.cta1, View.GONE);
                        imageCounter++;
                    }
                } else if (imageKey == 1) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta2, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta2, View.GONE);
                    }
                } else if (imageKey == 2) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta3, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta3, View.GONE);
                    }
                } else if (imageKey == 3) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta4, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta4, View.GONE);
                    }
                } else if (imageKey == 4) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta5, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta5, View.GONE);
                    }
                }

            }
            Utils.loadImageRidIntoRemoteView(R.id.close, R.drawable.pt_close, contentFiveCTAs);

            if (imageCounter > 2) {
                PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.");
                return null;
            }
        } catch (Throwable t) {
            PTLog.verbose("Error creating image only notification", t);
        }

        return nb;
    }

    private Builder renderRatingNotification(final Context context, final Bundle extras, final int notificationId,
             Builder nb) {
        PTLog.debug("Rendering Rating Template Push Notification with extras - " + extras.toString());
        try {
            contentViewRating = new RemoteViews(context.getPackageName(), R.layout.rating);
            setCustomContentViewBasicKeys(contentViewRating, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);

            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewRating, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewRating, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewMessageSummary(contentViewRating, pt_msg_summary);

            setCustomContentViewTitleColour(contentViewRating, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewRating, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewExpandedBackgroundColour(contentViewRating, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            //Set the rating stars
            contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);

            //Set Pending Intents for each star to listen to click

            Intent notificationIntent1 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent1.putExtra("click1", true);
            notificationIntent1.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent1.putExtra("config", config);
            notificationIntent1.putExtras(extras);
            PendingIntent contentIntent1 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent1, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star1, contentIntent1);

            Intent notificationIntent2 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent2.putExtra("click2", true);
            notificationIntent2.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent2.putExtra("config", config);
            notificationIntent2.putExtras(extras);
            PendingIntent contentIntent2 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent2, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star2, contentIntent2);

            Intent notificationIntent3 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent3.putExtra("click3", true);
            notificationIntent3.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent3.putExtra("config", config);
            notificationIntent3.putExtras(extras);
            PendingIntent contentIntent3 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent3, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star3, contentIntent3);

            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra("click4", true);
            notificationIntent4.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtra("config", config);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent4, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star4, contentIntent4);

            Intent notificationIntent5 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent5.putExtra("click5", true);
            notificationIntent5.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent5.putExtra("config", config);
            notificationIntent5.putExtras(extras);
            PendingIntent contentIntent5 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent5, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star5, contentIntent5);

            Intent launchIntent = new Intent(context, PushTemplateReceiver.class);

            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, pt_rating_default_dl);

            nb = setNotificationBuilderBasics(nb, contentViewSmall, contentViewRating, pt_title, pIntent);

            setCustomContentViewBigImage(contentViewRating, pt_big_img);

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewRating, pt_large_icon);

            setCustomContentViewSmallIcon(contentViewRating);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewRating);
            setCustomContentViewDotSep(contentViewSmall);

        } catch (Throwable t) {
            PTLog.verbose("Error creating rating notification ", t);
        }
        return nb;
    }

    private Builder renderManualCarouselNotification(final Context context, final Bundle extras,
            final int notificationId, Builder nb) {

        PTLog.debug("Rendering Manual Carousel Template Push Notification with extras - " + extras.toString());
        try {

            contentViewManualCarousel = new RemoteViews(context.getPackageName(), R.layout.manual_carousel);
            setCustomContentViewBasicKeys(contentViewManualCarousel, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);
            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewManualCarousel, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewManualCarousel, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewManualCarousel, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewTitleColour(contentViewManualCarousel, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewManualCarousel, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewManualCarousel, pt_msg_summary);

            contentViewManualCarousel.setViewVisibility(R.id.leftArrowPos0, View.VISIBLE);
            contentViewManualCarousel.setViewVisibility(R.id.rightArrowPos0, View.VISIBLE);

            int imageCounter = 0;
            boolean isFirstImageOk = false;
            String dl = deepLinkList.get(0);
            int currentPosition = 0;
            ArrayList<String> tempImageList = new ArrayList<>();

            for (int index = 0; index < imageList.size(); index++) {
                RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.image_view_rounded);
                Utils.loadImageURLIntoRemoteView(R.id.flipper_img, imageList.get(index), tempRemoteView, context);
                if (!Utils.getFallback()) {
                    if (!isFirstImageOk) {
                        currentPosition = index;
                        isFirstImageOk = true;
                    }
                    contentViewManualCarousel.addView(R.id.carousel_image, tempRemoteView);
                    contentViewManualCarousel.addView(R.id.carousel_image_right, tempRemoteView);
                    contentViewManualCarousel.addView(R.id.carousel_image_left, tempRemoteView);
                    imageCounter++;
                    tempImageList.add(imageList.get(index));
                } else {
                    if (deepLinkList != null && deepLinkList.size() == imageList.size()) {
                        deepLinkList.remove(index);
                    }
                    PTLog.debug("Skipping Image in Manual Carousel.");
                }
            }
            if (pt_manual_carousel_type == null || !pt_manual_carousel_type.equalsIgnoreCase(Constants.PT_MANUAL_CAROUSEL_FILMSTRIP)) {
                contentViewManualCarousel.setViewVisibility(R.id.carousel_image_right, View.GONE);
                contentViewManualCarousel.setViewVisibility(R.id.carousel_image_left, View.GONE);
            }

            contentViewManualCarousel.setDisplayedChild(R.id.carousel_image_right, 1);
            contentViewManualCarousel.setDisplayedChild(R.id.carousel_image_left, tempImageList.size() - 1);

            extras.putInt(Constants.PT_MANUAL_CAROUSEL_CURRENT, currentPosition);
            extras.putStringArrayList(Constants.PT_IMAGE_LIST, tempImageList);
            extras.putStringArrayList(Constants.PT_DEEPLINK_LIST, deepLinkList);

            Intent rightArrowPos0Intent = new Intent(context, PushTemplateReceiver.class);
            rightArrowPos0Intent.putExtra(Constants.PT_RIGHT_SWIPE, true);
            rightArrowPos0Intent.putExtra(Constants.PT_MANUAL_CAROUSEL_FROM, 0);
            rightArrowPos0Intent.putExtra(Constants.PT_NOTIF_ID, notificationId);
            rightArrowPos0Intent.putExtras(extras);
            PendingIntent contentRightPos0Intent = setPendingIntent(context, notificationId, extras, rightArrowPos0Intent, dl);
            contentViewManualCarousel.setOnClickPendingIntent(R.id.rightArrowPos0, contentRightPos0Intent);

            Intent leftArrowPos0Intent = new Intent(context, PushTemplateReceiver.class);
            leftArrowPos0Intent.putExtra(Constants.PT_RIGHT_SWIPE, false);
            leftArrowPos0Intent.putExtra(Constants.PT_MANUAL_CAROUSEL_FROM, 0);
            leftArrowPos0Intent.putExtra(Constants.PT_NOTIF_ID, notificationId);
            leftArrowPos0Intent.putExtras(extras);
            PendingIntent contentLeftPos0Intent = setPendingIntent(context, notificationId, extras, leftArrowPos0Intent, dl);
            contentViewManualCarousel.setOnClickPendingIntent(R.id.leftArrowPos0, contentLeftPos0Intent);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, dl);


            Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);

            nb = setNotificationBuilderBasics(nb, contentViewSmall, contentViewManualCarousel, pt_title, pIntent, dIntent);

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewManualCarousel, pt_large_icon);

            setCustomContentViewSmallIcon(contentViewManualCarousel);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewManualCarousel);
            setCustomContentViewDotSep(contentViewSmall);

            if (imageCounter < 2) {
                PTLog.debug("Need at least 2 images to display Manual Carousel, found - " + imageCounter + ", not displaying the notification.");
                return null;
            }
        } catch (Throwable t) {
            PTLog.verbose("Error creating Manual carousel notification ", t);
        }

        return nb;
    }

    @Override
    public void setSmallIcon(final int smallIcon, final Context context) {
        this.smallIcon = smallIcon;
        try {
            pt_small_icon = Utils.setBitMapColour(context, smallIcon, pt_small_icon_clr);
        } catch (NullPointerException e) {
            PTLog.debug("NPE while setting small icon color");
        }
    }

    @Override
    public Object getCollapseKey(final Bundle extras) {
        return pt_collapse_key;// TODO: Check if set properly before caller calls this
    }

    private void setUp(Context context, Bundle extras, CleverTapInstanceConfig config) {
        pt_id = extras.getString(Constants.PT_ID);
        String pt_json = extras.getString(Constants.PT_JSON);
        if (pt_id != null) {
            templateType = TemplateType.fromString(pt_id);
            Bundle newExtras = null;
            try {
                if (pt_json != null && !pt_json.isEmpty()) {
                    newExtras = Utils.fromJson(new JSONObject(pt_json));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (newExtras != null) extras.putAll(newExtras);
        }
        pt_msg = extras.getString(Constants.PT_MSG);
        pt_msg_summary = extras.getString(Constants.PT_MSG_SUMMARY);
        pt_msg_clr = extras.getString(Constants.PT_MSG_COLOR);
        pt_title = extras.getString(Constants.PT_TITLE);
        pt_title_clr = extras.getString(Constants.PT_TITLE_COLOR);
        pt_meta_clr = extras.getString(Constants.PT_META_CLR);
        pt_bg = extras.getString(Constants.PT_BG);
        pt_big_img = extras.getString(Constants.PT_BIG_IMG);
        pt_large_icon = extras.getString(Constants.PT_NOTIF_ICON);
        pt_small_view = extras.getString(Constants.PT_SMALL_VIEW);
        imageList = Utils.getImageListFromExtras(extras);
        deepLinkList = Utils.getDeepLinkListFromExtras(extras);
        bigTextList = Utils.getBigTextFromExtras(extras);
        smallTextList = Utils.getSmallTextFromExtras(extras);
        priceList = Utils.getPriceFromExtras(extras);
        pt_rating_default_dl = extras.getString(Constants.PT_DEFAULT_DL);
        asyncHelper = AsyncHelper.getInstance();
//        dbHelper = new DBHelper(context);
        pt_timer_threshold = Utils.getTimerThreshold(extras);
        pt_input_label = extras.getString(Constants.PT_INPUT_LABEL);
        pt_input_feedback = extras.getString(Constants.PT_INPUT_FEEDBACK);
        pt_input_auto_open = extras.getString(Constants.PT_INPUT_AUTO_OPEN);
        pt_dismiss_on_click = extras.getString(Constants.PT_DISMISS_ON_CLICK);
        pt_chrono_title_clr = extras.getString(Constants.PT_CHRONO_TITLE_COLOUR);
        pt_product_display_action = extras.getString(Constants.PT_PRODUCT_DISPLAY_ACTION);
        pt_product_display_action_clr = extras.getString(Constants.PT_PRODUCT_DISPLAY_ACTION_COLOUR);
        pt_timer_end = Utils.getTimerEnd(extras);
        pt_big_img_alt = extras.getString(Constants.PT_BIG_IMG_ALT);
        pt_msg_alt = extras.getString(Constants.PT_MSG_ALT);
        pt_title_alt = extras.getString(Constants.PT_TITLE_ALT);
        pt_product_display_linear = extras.getString(Constants.PT_PRODUCT_DISPLAY_LINEAR);
        pt_product_display_action_text_clr = extras.getString(Constants.PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR);
        pt_small_icon_clr = extras.getString(Constants.PT_SMALL_ICON_COLOUR);
        pt_cancel_notif_id = extras.getString(Constants.PT_CANCEL_NOTIF_ID);
        pt_cancel_notif_ids = Utils.getNotificationIds(context);
        actions = Utils.getActionKeys(extras);
        pt_subtitle = extras.getString(Constants.PT_SUBTITLE);
        pt_collapse_key = extras.get(Constants.PT_COLLAPSE_KEY);
        pt_flip_interval = Utils.getFlipInterval(extras);
        pID = extras.getString(Constants.WZRK_PUSH_ID);
        pt_manual_carousel_type = extras.getString(Constants.PT_MANUAL_CAROUSEL_TYPE);
        if (config != null) {
            this.config = config;
        }
        setKeysFromDashboard(extras);
    }


    @SuppressWarnings("WeakerAccess")
    @SuppressLint("NewApi")
    public static void createNotification(Context context, Bundle extras) {
        PTLog.verbose("Creating notification...");
        TemplateRenderer templateRenderer = new TemplateRenderer(context, extras);
        templateRenderer.dupeCheck(context, extras);
    }

    @SuppressWarnings("unused")
    public static void createNotification(Context context, Bundle extras, CleverTapInstanceConfig config) {
        PTLog.verbose("Creating notification with config...");
        TemplateRenderer templateRenderer = new TemplateRenderer(context, extras, config);
        templateRenderer.dupeCheck(context, extras);
    }

    @SuppressWarnings("SameParameterValue")
    private synchronized void dupeCheck(final Context context, final Bundle extras) {
        try {
            asyncHelper.postAsyncSafely("TemplateRenderer#_createNotification", new Runnable() {
                @SuppressWarnings("ConstantConditions")
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    try {
                        //Not needed here, does dupecheck in core lib
//                        if (extras.getString(Constants.WZRK_PUSH_ID) != null) {
//                            if (!extras.getString(Constants.WZRK_PUSH_ID).isEmpty()) {
//                                String ptID = extras.getString(Constants.WZRK_PUSH_ID);
//                                if (!dbHelper.isNotificationPresentInDB(ptID)) {
//                                    _createNotification(context, extras);
//                                    dbHelper.savePT(ptID, Utils.bundleToJSON(extras));
//                                } else {
//                                    PTLog.debug("Notification already Rendered. skipping this payload");
//                                }
//                            }
//                        } else {
//                            _createNotification(context, extras);
//                        }

                    } catch (Throwable t) {
                        PTLog.verbose("Couldn't render notification: " + t.getLocalizedMessage());
                    }
                }
            });
        } catch (Throwable t) {
            PTLog.verbose("Failed to process push notification: " + t.getLocalizedMessage());
        }
    }


    @SuppressWarnings("SameParameterValue")
    private void _createNotification(Context context, Bundle extras) {
        if (pt_id == null) {
            PTLog.verbose("Template ID not provided. Cannot create the notification");
            return;
        }

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        channelId = extras.getString(Constants.WZRK_CHANNEL_ID, "");
        requiresChannelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelIdError = null;
            if (channelId.isEmpty()) {
                channelIdError = "Unable to render notification, channelId is required but not provided in the notification payload: " + extras.toString();
            } else if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                channelIdError = "Unable to render notification, channelId: " + channelId + " not registered by the app.";
            }
            if (channelIdError != null) {
                PTLog.verbose(channelIdError);
                return;
            }
        }

        setSmallIcon(context);
        int notificationId = setCollapseKey(pt_collapse_key);

        switch (templateType) {
            case BASIC:
                if (hasAllBasicNotifKeys())
                    renderBasicTemplateNotification(context, extras, notificationId);
                break;
            case AUTO_CAROUSEL:
                if (hasAllCarouselNotifKeys())
                    renderAutoCarouselNotification(context, extras, notificationId);
                break;
            case MANUAL_CAROUSEL:
                if (hasAllManualCarouselNotifKeys())
                    renderManualCarouselNotification(context, extras, notificationId);
                break;
            case RATING:
                if (hasAllRatingNotifKeys())
                    renderRatingNotification(context, extras, notificationId);
                break;
            case FIVE_ICONS:
                if (hasAll5IconNotifKeys())
                    renderFiveIconNotification(context, extras, notificationId);
                break;
            case PRODUCT_DISPLAY:
                if (hasAllProdDispNotifKeys())
                    renderProductDisplayNotification(context, extras, notificationId);
                break;
            case ZERO_BEZEL:
                if (hasAllZeroBezelNotifKeys())
                    renderZeroBezelNotification(context, extras, notificationId);
                break;
            case TIMER:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (hasAllTimerKeys()) {
                        renderTimerNotification(context, extras, notificationId);
                    }
                } else {
                    PTLog.debug("Push Templates SDK supports Timer Notifications only on or above Android Nougat, reverting to basic template");
                    if (hasAllBasicNotifKeys()) {
                        renderBasicTemplateNotification(context, extras, notificationId);
                    }
                }
                break;
            case INPUT_BOX:
                if (hasAllInputBoxKeys())
                    renderInputBoxNotification(context, extras, notificationId);
                break;
            case CANCEL:
                renderCancelNotification();
                break;
        }
    }

    private void setSmallIcon(Context context) {
        Bundle metaData;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
            String x = Utils._getManifestStringValueForKey(metaData, Constants.LABEL_NOTIFICATION_ICON);
            if (x == null) throw new IllegalArgumentException();
            smallIcon = context.getResources().getIdentifier(x, "drawable", context.getPackageName());
            if (smallIcon == 0) throw new IllegalArgumentException();
        } catch (Throwable t) {
            smallIcon = Utils.getAppIconAsIntId(context);
        }

        try {
            pt_small_icon = Utils.setBitMapColour(context, smallIcon, pt_small_icon_clr);
        } catch (NullPointerException e) {
            PTLog.debug("NPE while setting small icon color");
        }
    }

    private void setDotSep(Context context) {
        try {
            pt_dot = context.getResources().getIdentifier(Constants.PT_DOT_SEP, "drawable", context.getPackageName());
            pt_dot_sep = Utils.setBitMapColour(context, pt_dot, pt_meta_clr);
        } catch (NullPointerException e) {
            PTLog.debug("NPE while setting dot sep color");
        }
    }

    private boolean hasAllBasicNotifKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllZeroBezelNotifKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (deepLinkList == null || deepLinkList.size() == 0) {
            PTLog.verbose("Deeplink is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_big_img == null || pt_big_img.isEmpty()) {
            PTLog.verbose("Display Image is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllCarouselNotifKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (deepLinkList == null || deepLinkList.size() == 0) {
            PTLog.verbose("Deeplink is missing or empty. Not showing notification");
            result = false;
        }
        if (imageList == null || imageList.size() < 3) {
            PTLog.verbose("Three required images not present. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllManualCarouselNotifKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (deepLinkList == null || deepLinkList.size() == 0) {
            PTLog.verbose("Deeplink is missing or empty. Not showing notification");
            result = false;
        }
        if (imageList == null || imageList.size() < 3) {
            PTLog.verbose("Three required images not present. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllRatingNotifKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_rating_default_dl == null || pt_rating_default_dl.isEmpty()) {
            PTLog.verbose("Default deeplink is missing or empty. Not showing notification");
            result = false;
        }

        if (deepLinkList == null || deepLinkList.size() == 0) {
            PTLog.verbose("At least one deeplink is required. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAll5IconNotifKeys() {
        boolean result = true;
        if (deepLinkList == null || deepLinkList.size() < 5) {
            PTLog.verbose("Five required deeplinks not present. Not showing notification");
            result = false;
        }
        if (imageList == null || imageList.size() < 5) {
            PTLog.verbose("Five required images not present. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllProdDispNotifKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (bigTextList == null || bigTextList.size() < 3) {
            PTLog.verbose("Three required product titles not present. Not showing notification");
            result = false;
        }
        if (smallTextList == null || smallTextList.size() < 3) {
            PTLog.verbose("Three required product descriptions not present. Not showing notification");
            result = false;
        }
        if (deepLinkList == null || deepLinkList.size() < 3) {
            PTLog.verbose("Three required deeplinks not present. Not showing notification");
            result = false;
        }
        if (imageList == null || imageList.size() < 3) {
            PTLog.verbose("Three required images not present. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_product_display_action == null || pt_product_display_action.isEmpty()) {
            PTLog.verbose("Button label is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_product_display_action_clr == null || pt_product_display_action_clr.isEmpty()) {
            PTLog.verbose("Button colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllTimerKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_timer_threshold == -1 && pt_timer_end == -1) {
            PTLog.verbose("Timer Threshold or End time not defined. Not showing notification");
            result = false;
        }
        if (pt_bg == null || pt_bg.isEmpty()) {
            PTLog.verbose("Background colour is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private boolean hasAllInputBoxKeys() {
        boolean result = true;
        if (pt_title == null || pt_title.isEmpty()) {
            PTLog.verbose("Title is missing or empty. Not showing notification");
            result = false;
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            PTLog.verbose("Message is missing or empty. Not showing notification");
            result = false;
        }
        if ((pt_input_feedback == null || pt_input_feedback.isEmpty()) && actions == null) {
            PTLog.verbose("Feedback Text or Actions is missing or empty. Not showing notification");
            result = false;
        }
        return result;
    }

    private void renderCancelNotification() {
        if (pt_cancel_notif_id != null && !pt_cancel_notif_id.isEmpty()) {
            int notificationId = Integer.parseInt(pt_cancel_notif_id);
            notificationManager.cancel(notificationId);
        } else {
            if (pt_cancel_notif_ids.size() > 0) {
                for (int i = 0; i <= pt_cancel_notif_ids.size(); i++) {
                    notificationManager.cancel(pt_cancel_notif_ids.get(i));
                }
            }
        }
    }

    private void renderRatingNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Rating Template Push Notification with extras - " + extras.toString());
        try {
            contentViewRating = new RemoteViews(context.getPackageName(), R.layout.rating);
            setCustomContentViewBasicKeys(contentViewRating, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);

            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewRating, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewRating, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewMessageSummary(contentViewRating, pt_msg_summary);

            setCustomContentViewTitleColour(contentViewRating, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewRating, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewExpandedBackgroundColour(contentViewRating, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            //Set the rating stars
            contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
            contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);

            notificationId = setNotificationId(notificationId);

            //Set Pending Intents for each star to listen to click

            Intent notificationIntent1 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent1.putExtra("click1", true);
            notificationIntent1.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent1.putExtra("config", config);
            notificationIntent1.putExtras(extras);
            PendingIntent contentIntent1 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent1, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star1, contentIntent1);

            Intent notificationIntent2 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent2.putExtra("click2", true);
            notificationIntent2.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent2.putExtra("config", config);
            notificationIntent2.putExtras(extras);
            PendingIntent contentIntent2 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent2, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star2, contentIntent2);

            Intent notificationIntent3 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent3.putExtra("click3", true);
            notificationIntent3.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent3.putExtra("config", config);
            notificationIntent3.putExtras(extras);
            PendingIntent contentIntent3 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent3, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star3, contentIntent3);

            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra("click4", true);
            notificationIntent4.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtra("config", config);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent4, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star4, contentIntent4);

            Intent notificationIntent5 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent5.putExtra("click5", true);
            notificationIntent5.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent5.putExtra("config", config);
            notificationIntent5.putExtras(extras);
            PendingIntent contentIntent5 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent5, 0);
            contentViewRating.setOnClickPendingIntent(R.id.star5, contentIntent5);

            Intent launchIntent = new Intent(context, PushTemplateReceiver.class);

            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, pt_rating_default_dl);

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewRating, pt_title, pIntent);

            Notification notification = notificationBuilder.build();

            setCustomContentViewBigImage(contentViewRating, pt_big_img);

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewRating, pt_large_icon);

            setCustomContentViewSmallIcon(contentViewRating);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewRating);
            setCustomContentViewDotSep(contentViewSmall);

            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);

        } catch (Throwable t) {
            PTLog.verbose("Error creating rating notification ", t);
        }
    }

    private Builder renderAutoCarouselNotification(Context context, Bundle extras, int notificationId,Builder nb) {
        PTLog.debug("Rendering Auto Carousel Template Push Notification with extras - " + extras.toString());
        try {

            contentViewCarousel = new RemoteViews(context.getPackageName(), R.layout.auto_carousel);
            setCustomContentViewBasicKeys(contentViewCarousel, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);

            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewCarousel, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewCarousel, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewCarousel, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewTitleColour(contentViewCarousel, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewCarousel, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewCarousel, pt_msg_summary);

            setCustomContentViewViewFlipperInterval(contentViewCarousel, pt_flip_interval);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            nb = setNotificationBuilderBasics(nb, contentViewSmall, contentViewCarousel, pt_title,
                    pIntent);

            int imageCounter = 0;
            for (int index = 0; index < imageList.size(); index++) {
                RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.image_view);
                Utils.loadImageURLIntoRemoteView(R.id.fimg, imageList.get(index), tempRemoteView);
                if (!Utils.getFallback()) {
                    contentViewCarousel.addView(R.id.view_flipper, tempRemoteView);
                    imageCounter++;
                } else {
                    PTLog.debug("Skipping Image in Auto Carousel.");
                }
            }

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewCarousel, pt_large_icon);

            setCustomContentViewSmallIcon(contentViewCarousel);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewCarousel);
            setCustomContentViewDotSep(contentViewSmall);

            if (imageCounter < 2) {
                PTLog.debug("Need at least 2 images to display Auto Carousel, found - "
                        + imageCounter + ", not displaying the notification.");
                return null;
            }

        } catch (Throwable t) {
            PTLog.verbose("Error creating auto carousel notification ", t);
        }

        return nb;
    }

    private void renderAutoCarouselNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Auto Carousel Template Push Notification with extras - " + extras.toString());
        try {
            notificationId = setNotificationId(notificationId);

            contentViewCarousel = new RemoteViews(context.getPackageName(), R.layout.auto_carousel);
            setCustomContentViewBasicKeys(contentViewCarousel, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);

            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewCarousel, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewCarousel, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewCarousel, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewTitleColour(contentViewCarousel, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewCarousel, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewCarousel, pt_msg_summary);

            setCustomContentViewViewFlipperInterval(contentViewCarousel, pt_flip_interval);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewCarousel, pt_title, pIntent);

            Notification notification = notificationBuilder.build();

            int imageCounter = 0;
            for (int index = 0; index < imageList.size(); index++) {
                RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.image_view);
                Utils.loadImageURLIntoRemoteView(R.id.fimg, imageList.get(index), tempRemoteView);
                if (!Utils.getFallback()) {
                    contentViewCarousel.addView(R.id.view_flipper, tempRemoteView);
                    imageCounter++;
                } else {
                    PTLog.debug("Skipping Image in Auto Carousel.");
                }
            }

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewCarousel, pt_large_icon);

            setCustomContentViewSmallIcon(contentViewCarousel);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewCarousel);
            setCustomContentViewDotSep(contentViewSmall);

            if (imageCounter < 2) {
                PTLog.debug("Need at least 2 images to display Auto Carousel, found - "
                        + imageCounter + ", not displaying the notification.");
                return;
            }

            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);
        } catch (Throwable t) {
            PTLog.verbose("Error creating auto carousel notification ", t);
        }
    }

    private void setCustomContentViewViewFlipperInterval(RemoteViews contentView, int interval) {
        contentView.setInt(R.id.view_flipper, "setFlipInterval", interval);
    }

    private void renderManualCarouselNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Manual Carousel Template Push Notification with extras - " + extras.toString());
        try {
            notificationId = setNotificationId(notificationId);

            contentViewManualCarousel = new RemoteViews(context.getPackageName(), R.layout.manual_carousel);
            setCustomContentViewBasicKeys(contentViewManualCarousel, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);
            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewManualCarousel, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewManualCarousel, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewManualCarousel, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewTitleColour(contentViewManualCarousel, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewManualCarousel, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewManualCarousel, pt_msg_summary);

            contentViewManualCarousel.setViewVisibility(R.id.leftArrowPos0, View.VISIBLE);
            contentViewManualCarousel.setViewVisibility(R.id.rightArrowPos0, View.VISIBLE);

            int imageCounter = 0;
            boolean isFirstImageOk = false;
            String dl = deepLinkList.get(0);
            int currentPosition = 0;
            ArrayList<String> tempImageList = new ArrayList<>();

            for (int index = 0; index < imageList.size(); index++) {
                RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.image_view_rounded);
                Utils.loadImageURLIntoRemoteView(R.id.flipper_img, imageList.get(index), tempRemoteView, context);
                if (!Utils.getFallback()) {
                    if (!isFirstImageOk) {
                        currentPosition = index;
                        isFirstImageOk = true;
                    }
                    contentViewManualCarousel.addView(R.id.carousel_image, tempRemoteView);
                    contentViewManualCarousel.addView(R.id.carousel_image_right, tempRemoteView);
                    contentViewManualCarousel.addView(R.id.carousel_image_left, tempRemoteView);
                    imageCounter++;
                    tempImageList.add(imageList.get(index));
                } else {
                    if (deepLinkList != null && deepLinkList.size() == imageList.size()) {
                        deepLinkList.remove(index);
                    }
                    PTLog.debug("Skipping Image in Manual Carousel.");
                }
            }
            if (pt_manual_carousel_type == null || !pt_manual_carousel_type.equalsIgnoreCase(Constants.PT_MANUAL_CAROUSEL_FILMSTRIP)) {
                contentViewManualCarousel.setViewVisibility(R.id.carousel_image_right, View.GONE);
                contentViewManualCarousel.setViewVisibility(R.id.carousel_image_left, View.GONE);
            }

            contentViewManualCarousel.setDisplayedChild(R.id.carousel_image_right, 1);
            contentViewManualCarousel.setDisplayedChild(R.id.carousel_image_left, tempImageList.size() - 1);

            extras.putInt(Constants.PT_MANUAL_CAROUSEL_CURRENT, currentPosition);
            extras.putStringArrayList(Constants.PT_IMAGE_LIST, tempImageList);
            extras.putStringArrayList(Constants.PT_DEEPLINK_LIST, deepLinkList);

            Intent rightArrowPos0Intent = new Intent(context, PushTemplateReceiver.class);
            rightArrowPos0Intent.putExtra(Constants.PT_RIGHT_SWIPE, true);
            rightArrowPos0Intent.putExtra(Constants.PT_MANUAL_CAROUSEL_FROM, 0);
            rightArrowPos0Intent.putExtra(Constants.PT_NOTIF_ID, notificationId);
            rightArrowPos0Intent.putExtras(extras);
            PendingIntent contentRightPos0Intent = setPendingIntent(context, notificationId, extras, rightArrowPos0Intent, dl);
            contentViewManualCarousel.setOnClickPendingIntent(R.id.rightArrowPos0, contentRightPos0Intent);

            Intent leftArrowPos0Intent = new Intent(context, PushTemplateReceiver.class);
            leftArrowPos0Intent.putExtra(Constants.PT_RIGHT_SWIPE, false);
            leftArrowPos0Intent.putExtra(Constants.PT_MANUAL_CAROUSEL_FROM, 0);
            leftArrowPos0Intent.putExtra(Constants.PT_NOTIF_ID, notificationId);
            leftArrowPos0Intent.putExtras(extras);
            PendingIntent contentLeftPos0Intent = setPendingIntent(context, notificationId, extras, leftArrowPos0Intent, dl);
            contentViewManualCarousel.setOnClickPendingIntent(R.id.leftArrowPos0, contentLeftPos0Intent);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, dl);

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewManualCarousel, pt_title, pIntent, dIntent);

            Notification notification = notificationBuilder.build();

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewManualCarousel, pt_large_icon);

            setCustomContentViewSmallIcon(contentViewManualCarousel);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewManualCarousel);
            setCustomContentViewDotSep(contentViewSmall);

            if (imageCounter < 2) {
                PTLog.debug("Need at least 2 images to display Manual Carousel, found - " + imageCounter + ", not displaying the notification.");
                return;
            }
            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);
        } catch (Throwable t) {
            PTLog.verbose("Error creating Manual carousel notification ", t);
        }
    }

    private Builder renderBasicTemplateNotification(Context context, Bundle extras, int notificationId, Builder nb) {
        PTLog.debug("Rendering Basic Template Push Notification with extras - " + extras.toString());
        try {
            contentViewBig = new RemoteViews(context.getPackageName(), R.layout.image_only_big);
            setCustomContentViewBasicKeys(contentViewBig, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);

            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewBig, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewBig, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewBig, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewTitleColour(contentViewBig, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewBig, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewBig, pt_msg_summary);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null && deepLinkList.size() > 0) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            nb = setNotificationBuilderBasics(nb, contentViewSmall, contentViewBig, pt_title, pIntent);

            setCustomContentViewSmallIcon(contentViewBig);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewBig);
            setCustomContentViewDotSep(contentViewSmall);

            setCustomContentViewBigImage(contentViewBig, pt_big_img);

            setCustomContentViewLargeIcon(contentViewBig, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);

        } catch (Throwable t) {
            PTLog.verbose("Error creating image only notification", t);
        }

        return nb;
    }

    private void renderBasicTemplateNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Basic Template Push Notification with extras - " + extras.toString());
        try {
            contentViewBig = new RemoteViews(context.getPackageName(), R.layout.image_only_big);
            setCustomContentViewBasicKeys(contentViewBig, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);

            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewBig, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewBig, pt_msg);
            setCustomContentViewMessage(contentViewSmall, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewBig, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewTitleColour(contentViewBig, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewMessageColour(contentViewBig, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewBig, pt_msg_summary);

            notificationId = setNotificationId(notificationId);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null && deepLinkList.size() > 0) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewBig, pt_title, pIntent);

            Notification notification = notificationBuilder.build();

            setCustomContentViewSmallIcon(contentViewBig);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewBig);
            setCustomContentViewDotSep(contentViewSmall);

            setCustomContentViewBigImage(contentViewBig, pt_big_img);

            setCustomContentViewLargeIcon(contentViewBig, pt_large_icon);
            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);

            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);
        } catch (Throwable t) {
            PTLog.verbose("Error creating image only notification", t);
        }
    }

    private void renderProductDisplayNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Product Display Template Push Notification with extras - " + extras.toString());
        try {
            boolean isLinear = false;

            if (pt_product_display_linear == null || pt_product_display_linear.isEmpty()) {
                contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_template);
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small);
            } else {
                isLinear = true;
                contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_expanded);
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_collapsed);
            }

            setCustomContentViewBasicKeys(contentViewBig, context);
            if (!isLinear) {
                setCustomContentViewBasicKeys(contentViewSmall, context);
            }
            if (!bigTextList.isEmpty()) {
                setCustomContentViewText(contentViewBig, R.id.product_name, bigTextList.get(0));
            }

            if (!isLinear) {
                if (!smallTextList.isEmpty()) {
                    setCustomContentViewText(contentViewBig, R.id.product_description, smallTextList.get(0));
                }
            }

            if (!priceList.isEmpty()) {
                setCustomContentViewText(contentViewBig, R.id.product_price, priceList.get(0));
            }

            if (!isLinear) {
                setCustomContentViewTitle(contentViewBig, pt_title);
                setCustomContentViewTitle(contentViewSmall, pt_title);
                setCustomContentViewMessage(contentViewBig, pt_msg);
                setCustomContentViewElementColour(contentViewBig, R.id.product_description, pt_msg_clr);
                setCustomContentViewElementColour(contentViewBig, R.id.product_name, pt_title_clr);
                setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);
            }

            setCustomContentViewMessage(contentViewSmall, pt_msg);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            setCustomContentViewExpandedBackgroundColour(contentViewBig, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewButtonLabel(contentViewBig, R.id.product_action, pt_product_display_action);
            setCustomContentViewButtonColour(contentViewBig, R.id.product_action, pt_product_display_action_clr);
            setCustomContentViewButtonText(contentViewBig, R.id.product_action, pt_product_display_action_text_clr);

            notificationId = setNotificationId(notificationId);

            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);

            if (!isLinear) {
                setCustomContentViewSmallIcon(contentViewSmall);
                setCustomContentViewDotSep(contentViewSmall);
            }

            int imageCounter = 0;
            boolean isFirstImageOk = false;

            ArrayList<Integer> smallImageLayoutIds = new ArrayList<>();
            smallImageLayoutIds.add(R.id.small_image1);
            smallImageLayoutIds.add(R.id.small_image2);
            smallImageLayoutIds.add(R.id.small_image3);
            ArrayList<Integer> smallCollapsedImageLayoutIds = new ArrayList<>();
            smallCollapsedImageLayoutIds.add(R.id.small_image1_collapsed);
            smallCollapsedImageLayoutIds.add(R.id.small_image2_collapsed);
            smallCollapsedImageLayoutIds.add(R.id.small_image3_collapsed);
            ArrayList<String> tempImageList = new ArrayList<>();

            for (int index = 0; index < imageList.size(); index++) {
                if (isLinear) {
                    Utils.loadImageURLIntoRemoteView(smallCollapsedImageLayoutIds.get(imageCounter), imageList.get(index), contentViewSmall);
                    contentViewSmall.setViewVisibility(smallCollapsedImageLayoutIds.get(imageCounter), View.VISIBLE);
                    if (!Utils.getFallback()) {
                        contentViewSmall.setViewVisibility(smallCollapsedImageLayoutIds.get(imageCounter), View.VISIBLE);
                    }
                }
                Utils.loadImageURLIntoRemoteView(smallImageLayoutIds.get(imageCounter), imageList.get(index), contentViewBig);
                RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.image_view);
                Utils.loadImageURLIntoRemoteView(R.id.fimg, imageList.get(index), tempRemoteView);
                if (!Utils.getFallback()) {
                    if (!isFirstImageOk) {
                        isFirstImageOk = true;
                    }
                    contentViewBig.setViewVisibility(smallImageLayoutIds.get(imageCounter), View.VISIBLE);
                    contentViewBig.addView(R.id.carousel_image, tempRemoteView);
                    imageCounter++;
                    tempImageList.add(imageList.get(index));
                } else {
                    deepLinkList.remove(index);
                    bigTextList.remove(index);
                    smallTextList.remove(index);
                    priceList.remove(index);
                }
            }

            extras.putStringArrayList(Constants.PT_IMAGE_LIST, tempImageList);
            extras.putStringArrayList(Constants.PT_DEEPLINK_LIST, deepLinkList);
            extras.putStringArrayList(Constants.PT_BIGTEXT_LIST, bigTextList);
            extras.putStringArrayList(Constants.PT_SMALLTEXT_LIST, smallTextList);
            extras.putStringArrayList(Constants.PT_PRICE_LIST, priceList);


            int requestCode1 = new Random().nextInt();
            int requestCode2 = new Random().nextInt();
            int requestCode3 = new Random().nextInt();

            Intent notificationIntent1 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent1.putExtra(Constants.PT_CURRENT_POSITION, 0);
            notificationIntent1.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent1.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(0));
            notificationIntent1.putExtras(extras);
            PendingIntent contentIntent1 = PendingIntent.getBroadcast(context, requestCode1, notificationIntent1, 0);
            contentViewBig.setOnClickPendingIntent(R.id.small_image1, contentIntent1);

            if (deepLinkList.size() >= 2) {
                Intent notificationIntent2 = new Intent(context, PushTemplateReceiver.class);
                notificationIntent2.putExtra(Constants.PT_CURRENT_POSITION, 1);
                notificationIntent2.putExtra(Constants.PT_NOTIF_ID, notificationId);
                notificationIntent2.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(1));
                notificationIntent2.putExtras(extras);
                PendingIntent contentIntent2 = PendingIntent.getBroadcast(context, requestCode2, notificationIntent2, 0);
                contentViewBig.setOnClickPendingIntent(R.id.small_image2, contentIntent2);
            }

            if (deepLinkList.size() >= 3) {
                Intent notificationIntent3 = new Intent(context, PushTemplateReceiver.class);
                notificationIntent3.putExtra(Constants.PT_CURRENT_POSITION, 2);
                notificationIntent3.putExtra(Constants.PT_NOTIF_ID, notificationId);
                notificationIntent3.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(2));
                notificationIntent3.putExtras(extras);
                PendingIntent contentIntent3 = PendingIntent.getBroadcast(context, requestCode3, notificationIntent3, 0);
                contentViewBig.setOnClickPendingIntent(R.id.small_image3, contentIntent3);
            }
            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra(Constants.PT_IMAGE_1, true);
            notificationIntent4.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtra(Constants.PT_BUY_NOW_DL, deepLinkList.get(0));
            notificationIntent4.putExtra(Constants.PT_BUY_NOW, true);
            notificationIntent4.putExtra("config", config);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent4, 0);
            contentViewBig.setOnClickPendingIntent(R.id.product_action, contentIntent4);

            if (isLinear) {
                Intent notificationSmallIntent1 = new Intent(context, PTPushNotificationReceiver.class);
                PendingIntent contentSmallIntent1 = setPendingIntent(context, notificationId, extras, notificationSmallIntent1, deepLinkList.get(0));
                contentViewSmall.setOnClickPendingIntent(R.id.small_image1_collapsed, contentSmallIntent1);
                if (deepLinkList.size() >= 2) {
                    Intent notificationSmallIntent2 = new Intent(context, PTPushNotificationReceiver.class);
                    PendingIntent contentSmallIntent2 = setPendingIntent(context, notificationId, extras, notificationSmallIntent2, deepLinkList.get(1));
                    contentViewSmall.setOnClickPendingIntent(R.id.small_image2_collapsed, contentSmallIntent2);
                }
                if (deepLinkList.size() >= 3) {
                    Intent notificationSmallIntent3 = new Intent(context, PTPushNotificationReceiver.class);
                    PendingIntent contentSmallIntent3 = setPendingIntent(context, notificationId, extras, notificationSmallIntent3, deepLinkList.get(2));
                    contentViewSmall.setOnClickPendingIntent(R.id.small_image3_collapsed, contentSmallIntent3);
                }
            }

            Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewBig, pt_title, pIntent, dIntent);

            Notification notification = notificationBuilder.build();

            setCustomContentViewDotSep(contentViewBig);

            setCustomContentViewSmallIcon(contentViewBig);

            if (imageCounter <= 1) {
                PTLog.debug("2 or more images are not retrievable, not displaying the notification.");
                return;
            }

            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);
        } catch (Throwable t) {
            PTLog.verbose("Error creating Product Display Notification ", t);
        }

    }

    private void setCustomContentViewText(RemoteViews contentView, int resourceId, String s) {
        if (!s.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(resourceId, Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(resourceId, Html.fromHtml(s));
            }
        }
    }

    private void renderFiveIconNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Five Icon Template Push Notification with extras - " + extras.toString());
        try {

            if (pt_title == null || pt_title.isEmpty()) {
                pt_title = Utils.getApplicationName(context);
            }
            contentFiveCTAs = new RemoteViews(context.getPackageName(), R.layout.five_cta);

            setCustomContentViewExpandedBackgroundColour(contentFiveCTAs, pt_bg);

            notificationId = setNotificationId(notificationId);

            int reqCode1 = new Random().nextInt();
            int reqCode2 = new Random().nextInt();
            int reqCode3 = new Random().nextInt();
            int reqCode4 = new Random().nextInt();
            int reqCode5 = new Random().nextInt();
            int reqCode6 = new Random().nextInt();

            Intent notificationIntent1 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent1.putExtra("cta1", true);
            notificationIntent1.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent1.putExtras(extras);
            PendingIntent contentIntent1 = PendingIntent.getBroadcast(context, reqCode1, notificationIntent1, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta1, contentIntent1);

            Intent notificationIntent2 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent2.putExtra("cta2", true);
            notificationIntent2.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent2.putExtras(extras);
            PendingIntent contentIntent2 = PendingIntent.getBroadcast(context, reqCode2, notificationIntent2, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta2, contentIntent2);

            Intent notificationIntent3 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent3.putExtra("cta3", true);
            notificationIntent3.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent3.putExtras(extras);
            PendingIntent contentIntent3 = PendingIntent.getBroadcast(context, reqCode3, notificationIntent3, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta3, contentIntent3);

            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra("cta4", true);
            notificationIntent4.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, reqCode4, notificationIntent4, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta4, contentIntent4);

            Intent notificationIntent5 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent5.putExtra("cta5", true);
            notificationIntent5.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent5.putExtras(extras);
            PendingIntent contentIntent5 = PendingIntent.getBroadcast(context, reqCode5, notificationIntent5, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.cta5, contentIntent5);

            Intent notificationIntent6 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent6.putExtra("close", true);
            notificationIntent6.putExtra(Constants.PT_NOTIF_ID, notificationId);
            notificationIntent6.putExtras(extras);
            PendingIntent contentIntent6 = PendingIntent.getBroadcast(context, reqCode6, notificationIntent6, 0);
            contentFiveCTAs.setOnClickPendingIntent(R.id.close, contentIntent6);


            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentFiveCTAs, contentFiveCTAs, pt_title, pIntent);

            notificationBuilder.setOngoing(true);

            Notification notification = notificationBuilder.build();
            int imageCounter = 0;
            for (int imageKey = 0; imageKey < imageList.size(); imageKey++) {
                if (imageKey == 0) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta1, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        contentFiveCTAs.setViewVisibility(R.id.cta1, View.GONE);
                        imageCounter++;
                    }
                } else if (imageKey == 1) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta2, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta2, View.GONE);
                    }
                } else if (imageKey == 2) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta3, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta3, View.GONE);
                    }
                } else if (imageKey == 3) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta4, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta4, View.GONE);
                    }
                } else if (imageKey == 4) {
                    Utils.loadImageURLIntoRemoteView(R.id.cta5, imageList.get(imageKey), contentFiveCTAs);
                    if (Utils.getFallback()) {
                        imageCounter++;
                        contentFiveCTAs.setViewVisibility(R.id.cta5, View.GONE);
                    }
                }

            }
            Utils.loadImageRidIntoRemoteView(R.id.close, R.drawable.pt_close, contentFiveCTAs);

            if (imageCounter > 2) {
                PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.");
                return;
            }
            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);
        } catch (Throwable t) {
            PTLog.verbose("Error creating image only notification", t);
        }

    }

    private void renderZeroBezelNotification(Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Zero Bezel Template Push Notification with extras - " + extras.toString());
        try {
            contentViewBig = new RemoteViews(context.getPackageName(), R.layout.zero_bezel);
            setCustomContentViewBasicKeys(contentViewBig, context);

            boolean textOnlySmallView = pt_small_view != null && pt_small_view.equals(Constants.TEXT_ONLY);

            if (textOnlySmallView) {
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.cv_small_text_only);
            } else {
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.cv_small_zero_bezel);
            }
            setCustomContentViewBasicKeys(contentViewSmall, context);

            setCustomContentViewTitle(contentViewBig, pt_title);
            setCustomContentViewTitle(contentViewSmall, pt_title);

            setCustomContentViewMessage(contentViewBig, pt_msg);

            if (textOnlySmallView) {
                contentViewSmall.setViewVisibility(R.id.msg, View.GONE);
            } else {
                setCustomContentViewMessage(contentViewSmall, pt_msg);
            }

            setCustomContentViewMessageSummary(contentViewBig, pt_msg_summary);

            setCustomContentViewTitleColour(contentViewBig, pt_title_clr);
            setCustomContentViewTitleColour(contentViewSmall, pt_title_clr);

            setCustomContentViewExpandedBackgroundColour(contentViewBig, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall, pt_bg);

            setCustomContentViewMessageColour(contentViewBig, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewSmall, pt_msg_clr);

            notificationId = setNotificationId(notificationId);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewBig, pt_title, pIntent);

            Notification notification = notificationBuilder.build();

            setCustomContentViewBigImage(contentViewBig, pt_big_img);

            if (!textOnlySmallView) {
                setCustomContentViewBigImage(contentViewSmall, pt_big_img);
            }

            if (textOnlySmallView) {
                setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            }

            setCustomContentViewSmallIcon(contentViewBig);
            setCustomContentViewSmallIcon(contentViewSmall);

            setCustomContentViewDotSep(contentViewBig);
            setCustomContentViewDotSep(contentViewSmall);


            if (Utils.getFallback()) {
                PTLog.debug("Image not fetched, falling back to Basic Template");
                renderBasicTemplateNotification(context, extras, notificationId);
            } else {
                notificationManager.notify(notificationId, notification);

                Utils.raiseNotificationViewed(context, extras, config);
            }
        } catch (Throwable t) {
            PTLog.verbose("Error creating image only notification", t);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void renderTimerNotification(final Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Timer Template Push Notification with extras - " + extras.toString());
        try {

            contentViewTimer = new RemoteViews(context.getPackageName(), R.layout.timer);
            contentViewTimerCollapsed = new RemoteViews(context.getPackageName(), R.layout.timer_collapsed);

            int timer_end;

            if (pt_timer_threshold != -1 && pt_timer_threshold >= Constants.PT_TIMER_MIN_THRESHOLD) {
                timer_end = (pt_timer_threshold * Constants.ONE_SECOND) + Constants.ONE_SECOND;
            } else if (pt_timer_end >= Constants.PT_TIMER_MIN_THRESHOLD) {
                timer_end = (pt_timer_end * Constants.ONE_SECOND) + Constants.ONE_SECOND;
            } else {
                PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + Constants.PT_TIMER_END);
                return;
            }

            setCustomContentViewBasicKeys(contentViewTimer, context);
            setCustomContentViewBasicKeys(contentViewTimerCollapsed, context);

            setCustomContentViewTitle(contentViewTimer, pt_title);
            setCustomContentViewTitle(contentViewTimerCollapsed, pt_title);

            setCustomContentViewMessage(contentViewTimer, pt_msg);
            setCustomContentViewMessage(contentViewTimerCollapsed, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewTimer, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewTimerCollapsed, pt_bg);

            setCustomContentViewChronometerBackgroundColour(contentViewTimer, pt_bg);
            setCustomContentViewChronometerBackgroundColour(contentViewTimerCollapsed, pt_bg);

            setCustomContentViewTitleColour(contentViewTimer, pt_title_clr);
            setCustomContentViewTitleColour(contentViewTimerCollapsed, pt_title_clr);

            setCustomContentViewChronometerTitleColour(contentViewTimer, pt_chrono_title_clr, pt_title_clr);
            setCustomContentViewChronometerTitleColour(contentViewTimerCollapsed, pt_chrono_title_clr, pt_title_clr);

            setCustomContentViewMessageColour(contentViewTimer, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewTimerCollapsed, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewTimer, pt_msg_summary);

            contentViewTimer.setChronometer(R.id.chronometer, SystemClock.elapsedRealtime() + (timer_end), null, true);

            contentViewTimer.setChronometerCountDown(R.id.chronometer, true);


            contentViewTimerCollapsed.setChronometer(R.id.chronometer, SystemClock.elapsedRealtime() + (timer_end), null, true);

            contentViewTimerCollapsed.setChronometerCountDown(R.id.chronometer, true);


            notificationId = setNotificationId(notificationId);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            setNotificationBuilderBasics(notificationBuilder, contentViewTimerCollapsed, contentViewTimer, pt_title, pIntent);

            notificationBuilder.setTimeoutAfter(timer_end);

            Notification notification = notificationBuilder.build();

            setCustomContentViewBigImage(contentViewTimer, pt_big_img);

            setCustomContentViewSmallIcon(contentViewTimer);
            setCustomContentViewSmallIcon(contentViewTimerCollapsed);

            setCustomContentViewDotSep(contentViewTimer);
            setCustomContentViewDotSep(contentViewTimerCollapsed);
            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);

            timerRunner(context, extras, notificationId, timer_end);

        } catch (Throwable t) {
            PTLog.verbose("Error creating Timer notification ", t);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    // TODO: Design issue
    private Builder renderTimerNotification(final Context context, Bundle extras, int notificationId,Builder nb) {
        PTLog.debug("Rendering Timer Template Push Notification with extras - " + extras.toString());
        try {

            contentViewTimer = new RemoteViews(context.getPackageName(), R.layout.timer);
            contentViewTimerCollapsed = new RemoteViews(context.getPackageName(), R.layout.timer_collapsed);

            int timer_end;

            if (pt_timer_threshold != -1 && pt_timer_threshold >= Constants.PT_TIMER_MIN_THRESHOLD) {
                timer_end = (pt_timer_threshold * Constants.ONE_SECOND) + Constants.ONE_SECOND;
            } else if (pt_timer_end >= Constants.PT_TIMER_MIN_THRESHOLD) {
                timer_end = (pt_timer_end * Constants.ONE_SECOND) + Constants.ONE_SECOND;
            } else {
                PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + Constants.PT_TIMER_END);
                return null;
            }

            setCustomContentViewBasicKeys(contentViewTimer, context);
            setCustomContentViewBasicKeys(contentViewTimerCollapsed, context);

            setCustomContentViewTitle(contentViewTimer, pt_title);
            setCustomContentViewTitle(contentViewTimerCollapsed, pt_title);

            setCustomContentViewMessage(contentViewTimer, pt_msg);
            setCustomContentViewMessage(contentViewTimerCollapsed, pt_msg);

            setCustomContentViewExpandedBackgroundColour(contentViewTimer, pt_bg);
            setCustomContentViewCollapsedBackgroundColour(contentViewTimerCollapsed, pt_bg);

            setCustomContentViewChronometerBackgroundColour(contentViewTimer, pt_bg);
            setCustomContentViewChronometerBackgroundColour(contentViewTimerCollapsed, pt_bg);

            setCustomContentViewTitleColour(contentViewTimer, pt_title_clr);
            setCustomContentViewTitleColour(contentViewTimerCollapsed, pt_title_clr);

            setCustomContentViewChronometerTitleColour(contentViewTimer, pt_chrono_title_clr, pt_title_clr);
            setCustomContentViewChronometerTitleColour(contentViewTimerCollapsed, pt_chrono_title_clr, pt_title_clr);

            setCustomContentViewMessageColour(contentViewTimer, pt_msg_clr);
            setCustomContentViewMessageColour(contentViewTimerCollapsed, pt_msg_clr);

            setCustomContentViewMessageSummary(contentViewTimer, pt_msg_summary);

            contentViewTimer.setChronometer(R.id.chronometer, SystemClock.elapsedRealtime() + (timer_end), null, true);

            contentViewTimer.setChronometerCountDown(R.id.chronometer, true);


            contentViewTimerCollapsed.setChronometer(R.id.chronometer, SystemClock.elapsedRealtime() + (timer_end), null, true);

            contentViewTimerCollapsed.setChronometerCountDown(R.id.chronometer, true);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            nb = setNotificationBuilderBasics(nb, contentViewTimerCollapsed, contentViewTimer, pt_title, pIntent);

            nb.setTimeoutAfter(timer_end);

            setCustomContentViewBigImage(contentViewTimer, pt_big_img);

            setCustomContentViewSmallIcon(contentViewTimer);
            setCustomContentViewSmallIcon(contentViewTimerCollapsed);

            setCustomContentViewDotSep(contentViewTimer);
            setCustomContentViewDotSep(contentViewTimerCollapsed);

            timerRunner(context, extras, notificationId, timer_end);

        } catch (Throwable t) {
            PTLog.verbose("Error creating Timer notification ", t);
        }

        return nb;
    }

    private void renderInputBoxNotification(final Context context, Bundle extras, int notificationId) {
        PTLog.debug("Rendering Input Box Template Push Notification with extras - " + extras.toString());
        try {
            //Fetch Notif ID
            notificationId = setNotificationId(notificationId);

            //Set launchIntent to receiver
            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null && deepLinkList.size() > 0) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, channelId, context);

            notificationBuilder.setSmallIcon(smallIcon)
                    .setContentTitle(pt_title)
                    .setContentText(pt_msg)
                    .setContentIntent(pIntent)
                    .setVibrate(new long[]{0L})
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true);

            // Assign big picture notification
            setStandardViewBigImageStyle(pt_big_img, extras, context, notificationBuilder);

            if (pt_input_label != null && !pt_input_label.isEmpty()) {
                //Initialise RemoteInput
                RemoteInput remoteInput = new RemoteInput.Builder(Constants.PT_INPUT_KEY)
                        .setLabel(pt_input_label)
                        .build();

                //Set launchIntent to receiver
                Intent replyIntent = new Intent(context, PushTemplateReceiver.class);
                replyIntent.putExtra(Constants.PT_INPUT_FEEDBACK, pt_input_feedback);
                replyIntent.putExtra(Constants.PT_INPUT_AUTO_OPEN, pt_input_auto_open);
                replyIntent.putExtra("config", config);

                PendingIntent replyPendingIntent;
                if (deepLinkList != null) {
                    replyPendingIntent = setPendingIntent(context, notificationId, extras, replyIntent, deepLinkList.get(0));
                } else {
                    replyPendingIntent = setPendingIntent(context, notificationId, extras, replyIntent, null);
                }

                //Notification Action with RemoteInput instance added.
                NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                        android.R.drawable.sym_action_chat, pt_input_label, replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();


                //Notification.Action instance added to Notification Builder.
                notificationBuilder.addAction(replyAction);
            }
            if (pt_dismiss_on_click != null)
                if (!pt_dismiss_on_click.isEmpty())
                    extras.putString(Constants.PT_DISMISS_ON_CLICK, pt_dismiss_on_click);

            setActionButtons(context, extras, notificationId, notificationBuilder);

            Notification notification = notificationBuilder.build();
            notificationManager.notify(notificationId, notification);

            Utils.raiseNotificationViewed(context, extras, config);

        } catch (Throwable t) {
            PTLog.verbose("Error creating Input Box notification ", t);
        }
    }

    private Builder renderInputBoxNotification(final Context context, Bundle extras, int notificationId, Builder nb) {
        PTLog.debug("Rendering Input Box Template Push Notification with extras - " + extras.toString());
        try {

            //Set launchIntent to receiver
            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            if (deepLinkList != null && deepLinkList.size() > 0) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, deepLinkList.get(0));
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            nb.setSmallIcon(smallIcon)
                    .setContentTitle(pt_title)
                    .setContentText(pt_msg)
                    .setContentIntent(pIntent)
                    .setVibrate(new long[]{0L})
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true);

            // Assign big picture notification
            nb = setStandardViewBigImageStyle(pt_big_img, extras, context, nb);

            if (pt_input_label != null && !pt_input_label.isEmpty()) {
                //Initialise RemoteInput
                RemoteInput remoteInput = new RemoteInput.Builder(Constants.PT_INPUT_KEY)
                        .setLabel(pt_input_label)
                        .build();

                //Set launchIntent to receiver
                Intent replyIntent = new Intent(context, PushTemplateReceiver.class);
                replyIntent.putExtra(Constants.PT_INPUT_FEEDBACK, pt_input_feedback);
                replyIntent.putExtra(Constants.PT_INPUT_AUTO_OPEN, pt_input_auto_open);
                replyIntent.putExtra("config", config);

                PendingIntent replyPendingIntent;
                if (deepLinkList != null) {
                    replyPendingIntent = setPendingIntent(context, notificationId, extras, replyIntent, deepLinkList.get(0));
                } else {
                    replyPendingIntent = setPendingIntent(context, notificationId, extras, replyIntent, null);
                }

                //Notification Action with RemoteInput instance added.
                NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                        android.R.drawable.sym_action_chat, pt_input_label, replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();


                //Notification.Action instance added to Notification Builder.
                nb.addAction(replyAction);
            }
            if (pt_dismiss_on_click != null)
                if (!pt_dismiss_on_click.isEmpty())
                    extras.putString(Constants.PT_DISMISS_ON_CLICK, pt_dismiss_on_click);

            setActionButtons(context, extras, notificationId, nb);

        } catch (Throwable t) {
            PTLog.verbose("Error creating Input Box notification ", t);
        }

        return nb;
    }

    private PendingIntent setPendingIntent(Context context, int notificationId, Bundle extras, Intent launchIntent, String dl) {
        launchIntent.putExtras(extras);
        launchIntent.putExtra(Constants.PT_NOTIF_ID, notificationId);
        if (dl != null) {
            launchIntent.putExtra(Constants.DEFAULT_DL, true);
            launchIntent.putExtra(Constants.WZRK_DL, dl);
        }
        launchIntent.removeExtra(Constants.WZRK_ACTIONS);
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Builder setNotificationBuilderBasics(NotificationCompat.Builder notificationBuilder, RemoteViews contentViewSmall, RemoteViews contentViewBig, String pt_title, PendingIntent pIntent) {
        return notificationBuilder.setSmallIcon(smallIcon)
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setContentTitle(Html.fromHtml(pt_title))
                .setContentIntent(pIntent)
                .setVibrate(new long[]{0L})
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
    }

    private Builder setStandardViewBigImageStyle(String pt_big_img, Bundle extras, Context context, NotificationCompat.Builder notificationBuilder) {
        NotificationCompat.Style bigPictureStyle;
        if (pt_big_img != null && pt_big_img.startsWith("http")) {
            try {
                Bitmap bpMap = Utils.getNotificationBitmap(pt_big_img, false, context);

                if (bpMap == null)
                    throw new Exception("Failed to fetch big picture!");

                if (extras.containsKey(Constants.PT_MSG_SUMMARY)) {
                    String summaryText = pt_msg_summary;
                    bigPictureStyle = new NotificationCompat.BigPictureStyle()
                            .setSummaryText(summaryText)
                            .bigPicture(bpMap);
                } else {
                    bigPictureStyle = new NotificationCompat.BigPictureStyle()
                            .setSummaryText(pt_msg)
                            .bigPicture(bpMap);
                }
            } catch (Throwable t) {
                bigPictureStyle = new NotificationCompat.BigTextStyle()
                        .bigText(pt_msg);
                PTLog.verbose("Falling back to big text notification, couldn't fetch big picture", t);
            }
        } else {
            bigPictureStyle = new NotificationCompat.BigTextStyle()
                    .bigText(pt_msg);
        }

        notificationBuilder.setStyle(bigPictureStyle);
        return notificationBuilder;

    }

    private void setCustomContentViewLargeIcon(RemoteViews contentView, String pt_large_icon) {
        if (pt_large_icon != null && !pt_large_icon.isEmpty()) {
            Utils.loadImageURLIntoRemoteView(R.id.large_icon, pt_large_icon, contentView);
        } else {
            contentView.setViewVisibility(R.id.large_icon, View.GONE);
        }
    }

    private NotificationCompat.Builder setBuilderWithChannelIDCheck(boolean requiresChannelId, String channelId, Context context) {
        if (requiresChannelId) {
            return new NotificationCompat.Builder(context, channelId);
        } else {
            return new NotificationCompat.Builder(context);
        }
    }

    private void setCustomContentViewBasicKeys(RemoteViews contentView, Context context) {
        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context));
        contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context));
        if (pt_subtitle != null && !pt_subtitle.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle));
            }
        } else {
            contentView.setViewVisibility(R.id.subtitle, View.GONE);
            contentView.setViewVisibility(R.id.sep_subtitle, View.GONE);
        }
        if (pt_meta_clr != null && !pt_meta_clr.isEmpty()) {
            contentView.setTextColor(R.id.app_name, Utils.getColour(pt_meta_clr, Constants.PT_META_CLR_DEFAULTS));
            contentView.setTextColor(R.id.timestamp, Utils.getColour(pt_meta_clr, Constants.PT_META_CLR_DEFAULTS));
            contentView.setTextColor(R.id.subtitle, Utils.getColour(pt_meta_clr, Constants.PT_META_CLR_DEFAULTS));
            setDotSep(context);
        }
    }

    private void setCustomContentViewButtonColour(RemoteViews contentView, int resourceID, String pt_product_display_action_clr) {
        if (pt_product_display_action_clr != null && !pt_product_display_action_clr.isEmpty()) {
            contentView.setInt(resourceID, "setBackgroundColor", Utils.getColour(pt_product_display_action_clr, Constants.PT_PRODUCT_DISPLAY_ACTION_CLR_DEFAULTS));
        }
    }

    private void setCustomContentViewButtonLabel(RemoteViews contentView, int resourceID, String pt_product_display_action) {
        if (pt_product_display_action != null && !pt_product_display_action.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(resourceID, Html.fromHtml(pt_product_display_action, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(resourceID, Html.fromHtml(pt_product_display_action));
            }
        }
    }

    private void setCustomContentViewButtonText(RemoteViews contentView, int resourceID, String pt_product_display_action_text_clr) {
        if (pt_product_display_action_text_clr != null && !pt_product_display_action_text_clr.isEmpty()) {
            contentView.setTextColor(resourceID, Utils.getColour(pt_product_display_action_text_clr, Constants.PT_PRODUCT_DISPLAY_ACTION_TEXT_CLR_DEFAULT));
        }
    }

    private void setCustomContentViewBigImage(RemoteViews contentView, String pt_big_img) {
        if (pt_big_img != null && !pt_big_img.isEmpty()) {
            Utils.loadImageURLIntoRemoteView(R.id.big_image, pt_big_img, contentView);
            if (Utils.getFallback()) {
                contentView.setViewVisibility(R.id.big_image, View.GONE);
            }
        } else {
            contentView.setViewVisibility(R.id.big_image, View.GONE);
        }
    }

    private int setCollapseKey(Object collapse_key) {
        int notificationId = Constants.EMPTY_NOTIFICATION_ID;
        try {
            if (collapse_key != null) {

                if (collapse_key instanceof Number) {
                    notificationId = ((Number) collapse_key).intValue();
                } else if (collapse_key instanceof String) {
                    try {
                        notificationId = Integer.parseInt(collapse_key.toString());
                        PTLog.debug("Converting collapse_key: " + collapse_key + " to notificationId int: " + notificationId);
                    } catch (NumberFormatException e) {
                        notificationId = (collapse_key.toString().hashCode());
                        PTLog.debug("Converting collapse_key: " + collapse_key + " to notificationId int: " + notificationId);
                    }
                }
            }
        } catch (NumberFormatException e) {
            //no-op
        }
        return notificationId;
    }

    private int setNotificationId(int notificationId) {
        if (notificationId == Constants.EMPTY_NOTIFICATION_ID) {
            notificationId = (int) (Math.random() * 100);
        }
        return notificationId;
    }

    private void setCustomContentViewMessageSummary(RemoteViews contentView, String pt_msg_summary) {
        if (pt_msg_summary != null && !pt_msg_summary.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary));
            }
        }
    }

    private void setCustomContentViewMessageColour(RemoteViews contentView, String pt_msg_clr) {
        if (pt_msg_clr != null && !pt_msg_clr.isEmpty()) {
            contentView.setTextColor(R.id.msg, Utils.getColour(pt_msg_clr, Constants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewTitleColour(RemoteViews contentView, String pt_title_clr) {
        if (pt_title_clr != null && !pt_title_clr.isEmpty()) {
            contentView.setTextColor(R.id.title, Utils.getColour(pt_title_clr, Constants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewElementColour(RemoteViews contentView, int rId, String colour) {
        if (colour != null && !colour.isEmpty()) {
            contentView.setTextColor(rId, Utils.getColour(colour, Constants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewChronometerTitleColour(RemoteViews contentView, String pt_chrono_title_clr, String pt_title_clr) {
        if (pt_chrono_title_clr != null && !pt_chrono_title_clr.isEmpty()) {
            contentView.setTextColor(R.id.chronometer, Utils.getColour(pt_chrono_title_clr, Constants.PT_COLOUR_BLACK));
        } else {
            if (pt_title_clr != null && !pt_title_clr.isEmpty()) {
                contentView.setTextColor(R.id.chronometer, Utils.getColour(pt_title_clr, Constants.PT_COLOUR_BLACK));
            }
        }

    }

    private void setCustomContentViewExpandedBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.content_view_big, "setBackgroundColor", Utils.getColour(pt_bg, Constants.PT_COLOUR_WHITE));
        }
    }

    private void setCustomContentViewCollapsedBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.content_view_small, "setBackgroundColor", Utils.getColour(pt_bg, Constants.PT_COLOUR_WHITE));
        }
    }

    private void setCustomContentViewChronometerBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.chronometer, "setBackgroundColor", Utils.getColour(pt_bg, Constants.PT_COLOUR_WHITE));

        }
    }

    private void setCustomContentViewMessage(RemoteViews contentView, String pt_msg) {
        if (pt_msg != null && !pt_msg.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg));
            }
        }
    }

    private void setCustomContentViewTitle(RemoteViews contentView, String pt_title) {
        if (pt_title != null && !pt_title.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(R.id.title, Html.fromHtml(pt_title, Html.FROM_HTML_MODE_LEGACY));
            } else {
                contentView.setTextViewText(R.id.title, Html.fromHtml(pt_title));
            }
        }
    }

    private void setActionButtons(Context context, Bundle extras, int notificationId, NotificationCompat.Builder nb) {

        Class clazz = null;
        try {
            clazz = Class.forName("com.clevertap.pushtemplates.PTNotificationIntentService");
        } catch (ClassNotFoundException ex) {
            PTLog.debug("No Intent Service found");
        }

        boolean isPTIntentServiceAvailable = Utils.isServiceAvailable(context, clazz);

        if (actions != null && actions.length() > 0) {
            for (int i = 0; i < actions.length(); i++) {
                try {
                    JSONObject action = actions.getJSONObject(i);
                    String label = action.optString("l");
                    String dl = action.optString("dl");
                    String ico = action.optString(Constants.PT_NOTIF_ICON);
                    String id = action.optString("id");
                    boolean autoCancel = action.optBoolean("ac", true);
                    if (label.isEmpty() || id.isEmpty()) {
                        PTLog.debug("not adding push notification action: action label or id missing");
                        continue;
                    }
                    int icon = 0;
                    if (!ico.isEmpty()) {
                        try {
                            icon = context.getResources().getIdentifier(ico, "drawable", context.getPackageName());
                        } catch (Throwable t) {
                            PTLog.debug("unable to add notification action icon: " + t.getLocalizedMessage());
                        }
                    }

                    boolean sendToPTIntentService = (autoCancel && isPTIntentServiceAvailable);

                    Intent actionLaunchIntent;
                    if (sendToPTIntentService) {
                        actionLaunchIntent = new Intent(MAIN_ACTION);
                        actionLaunchIntent.setPackage(context.getPackageName());
                        actionLaunchIntent.putExtra(Constants.PT_TYPE, TYPE_BUTTON_CLICK);
                        if (!dl.isEmpty()) {
                            actionLaunchIntent.putExtra("dl", dl);
                        }
                    } else {
                        if (!dl.isEmpty()) {
                            actionLaunchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                        } else {
                            actionLaunchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        }
                    }

                    if (actionLaunchIntent != null) {
                        actionLaunchIntent.putExtras(extras);
                        actionLaunchIntent.removeExtra(Constants.WZRK_ACTIONS);
                        actionLaunchIntent.putExtra(Constants.PT_ACTION_ID, id);
                        actionLaunchIntent.putExtra("autoCancel", autoCancel);
                        actionLaunchIntent.putExtra("wzrk_c2a", id);
                        actionLaunchIntent.putExtra("notificationId", notificationId);
                        actionLaunchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }

                    PendingIntent actionIntent = null;
                    int requestCode = ((int) System.currentTimeMillis()) + i;
                    if (sendToPTIntentService) {
                        actionIntent = PendingIntent.getService(context, requestCode,
                                actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        actionIntent = PendingIntent.getActivity(context, requestCode,
                                actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    nb.addAction(icon, label, actionIntent);

                } catch (Throwable t) {
                    PTLog.debug("error adding notification action : " + t.getLocalizedMessage());
                }
            }
        }
    }


    private void timerRunner(final Context context, final Bundle extras, final int notificationId, final int delay) {
        final Handler handler = new Handler(Looper.getMainLooper());

        extras.remove("wzrk_rnv");

        if (pt_title_alt != null && !pt_title_alt.isEmpty()) {
            pt_title = pt_title_alt;
        }

        if (pt_big_img_alt != null && !pt_big_img_alt.isEmpty()) {
            pt_big_img = pt_big_img_alt;
        }

        if (pt_msg_alt != null && !pt_msg_alt.isEmpty()) {
            pt_msg = pt_msg_alt;
        }

        handler.postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                if (Utils.isNotificationInTray(context, notificationId)) {
                    asyncHelper.postAsyncSafely("TemplateRenderer#timerRunner", new Runnable() {
                        @Override
                        public void run() {
                            if (hasAllBasicNotifKeys()) {
                                renderBasicTemplateNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID);
                            }
                        }
                    });
                }
            }
        }, delay - 100);

    }

    private void setCustomContentViewSmallIcon(RemoteViews contentView) {
        if (pt_small_icon != null) {
            Utils.loadImageBitmapIntoRemoteView(R.id.small_icon, pt_small_icon, contentView);
        } else {
            Utils.loadImageRidIntoRemoteView(R.id.small_icon, smallIcon, contentView);
        }
    }

    private void setCustomContentViewDotSep(RemoteViews contentView) {
        if (pt_dot_sep != null) {
            Utils.loadImageBitmapIntoRemoteView(R.id.sep, pt_dot_sep, contentView);
            Utils.loadImageBitmapIntoRemoteView(R.id.sep_subtitle, pt_dot_sep, contentView);
        }
    }

    private Builder setNotificationBuilderBasics(NotificationCompat.Builder notificationBuilder, RemoteViews contentViewSmall, RemoteViews contentViewBig, String pt_title, PendingIntent pIntent, PendingIntent dIntent) {
        return notificationBuilder.setSmallIcon(smallIcon)
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setContentTitle(pt_title)
                .setDeleteIntent(dIntent)
                .setContentIntent(pIntent).setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
    }

    private void setKeysFromDashboard(Bundle extras) {
        if (pt_title == null || pt_title.isEmpty()) {
            pt_title = extras.getString(Constants.NOTIF_TITLE);
        }
        if (pt_msg == null || pt_msg.isEmpty()) {
            pt_msg = extras.getString(Constants.NOTIF_MSG);
        }
        if (pt_msg_summary == null || pt_msg_summary.isEmpty()) {
            pt_msg_summary = extras.getString(Constants.WZRK_MSG_SUMMARY);
        }
        if (pt_big_img == null || pt_big_img.isEmpty()) {
            pt_big_img = extras.getString(Constants.WZRK_BIG_PICTURE);
        }
        if (pt_rating_default_dl == null || pt_rating_default_dl.isEmpty()) {
            pt_rating_default_dl = extras.getString(Constants.WZRK_DL);
        }
        if (pt_meta_clr == null || pt_meta_clr.isEmpty()) {
            pt_meta_clr = extras.getString(Constants.WZRK_CLR);
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_CLR);
        }
        if (pt_subtitle == null || pt_subtitle.isEmpty()) {
            pt_subtitle = extras.getString(Constants.WZRK_SUBTITLE);
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_CLR);
        }
        if (pt_collapse_key == null) {
            pt_collapse_key = extras.get(Constants.WZRK_COLLAPSE);
        }
    }

    private PendingIntent setDismissIntent(Context context, Bundle extras, Intent intent) {
        intent.putExtras(extras);
        intent.putExtra(Constants.PT_DISMISS_INTENT, true);
        return PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void setCustomCTA(Context context, Bundle extras, RemoteViews contentView) {
        ArrayList<String> customCTAList = Utils.getCustomCTAListFromExtras(extras);
        if (customCTAList.size() != 0) {
            boolean isCustomCTASet = false;
            RemoteViews tempRemoteView = new RemoteViews(context.getPackageName(), R.layout.pt_custom_cta);
            ArrayList<Integer> customCTAIds = new ArrayList<>();
            customCTAIds.add(R.id.pt_custom_cta1);
            customCTAIds.add(R.id.pt_custom_cta2);
            customCTAIds.add(R.id.pt_custom_cta3);

            for (int index = 0; index < customCTAIds.size(); index++) {
                try {
                    JSONObject cta = Utils.toJsonObject(customCTAList.get(index));
                    String bgClr = cta.getString(Constants.PT_CUSTOM_CTA_BG_CLR);
                    String textClr = cta.getString(Constants.PT_CUSTOM_CTA_TEXT_CLR);
                    String text = cta.getString(Constants.PT_CUSTOM_CTA_TEXT);
                    String dl = cta.getString(Constants.PT_CUSTOM_CTA_DL);

                    setCustomContentViewText(tempRemoteView, customCTAIds.get(index), text);
                    tempRemoteView.setInt(customCTAIds.get(index), "setTextColor", Utils.getColour(textClr, "black"));
                    tempRemoteView.setInt(customCTAIds.get(index), "setBackgroundColor", Utils.getColour(bgClr, "white"));

                    Intent buttonIntent = new Intent(context, PushTemplateReceiver.class);
                    buttonIntent.putExtras(extras);
                    PendingIntent contentRightPos0Intent = setPendingIntent(context, extras.getInt(Constants.PT_NOTIF_ID), extras, buttonIntent, dl);
                    tempRemoteView.setOnClickPendingIntent(customCTAIds.get(index), contentRightPos0Intent);
                    isCustomCTASet = true;

                } catch (JSONException e) {
                    tempRemoteView.setViewVisibility(customCTAIds.get(index), View.GONE);
                    PTLog.debug("Unable to add Custom CTA with payload: " + customCTAList.get(index), e);
                }
            }
            if (isCustomCTASet) {
                contentView.addView(R.id.content_view_big, tempRemoteView);
            }
        }

    }

}
