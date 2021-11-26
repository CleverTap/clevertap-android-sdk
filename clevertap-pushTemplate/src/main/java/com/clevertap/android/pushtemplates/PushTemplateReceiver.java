package com.clevertap.android.pushtemplates;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;

import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.clevertap.android.sdk.pushnotification.CTNotificationIntentService.TYPE_BUTTON_CLICK;

public class PushTemplateReceiver extends BroadcastReceiver {
    boolean clicked1 = true, clicked2 = true, clicked3 = true, clicked4 = true, clicked5 = true, img1 = false, img2 = false, img3 = false, buynow = true, bigimage = true, cta1 = true, cta2 = true, cta3 = true, cta4 = true, cta5 = true, close = true;
    private RemoteViews contentViewBig, contentViewSmall, contentViewRating, contentViewManualCarousel;
    private String pt_id;
    private TemplateType templateType;
    private String pt_title;
    private String pt_msg;
    private String pt_msg_summary;
    private String pt_img_small;
    private String pt_large_icon;
    private String pt_rating_default_dl;
    private String pt_title_clr, pt_msg_clr;
    private ArrayList<String> imageList = new ArrayList<>();
    private ArrayList<String> deepLinkList = new ArrayList<>();
    private ArrayList<String> bigTextList = new ArrayList<>();
    private ArrayList<String> smallTextList = new ArrayList<>();
    private ArrayList<String> priceList = new ArrayList<>();
    private String pt_bg;
    private String channelId;
    private int smallIcon = 0;
    private int pt_dot = 0;
    private boolean requiresChannelId;
    private NotificationManager notificationManager;
    private String pt_product_display_action;
    private String pt_product_display_action_clr;
    private String pt_product_display_linear;
    private String pt_big_img_alt;
    private Bitmap pt_small_icon;
    private Bitmap pt_dot_sep;
    private String pt_small_icon_clr;
    private String pt_product_display_action_text_clr;
    private String pt_big_img;
    private String pt_meta_clr;
    private AsyncHelper asyncHelper;
    private boolean pt_dismiss_intent;
    private String pt_rating_toast;
    private String pt_subtitle;
    private String pID;
    private CleverTapInstanceConfig config;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        Utils.createSilentNotificationChannel(context);

        if (intent.getExtras() != null) {
            final Bundle extras = intent.getExtras();
            pt_id = intent.getStringExtra(PTConstants.PT_ID);
            pID = extras.getString(Constants.WZRK_PUSH_ID);
            pt_msg = extras.getString(PTConstants.PT_MSG);
            pt_msg_summary = extras.getString(PTConstants.PT_MSG_SUMMARY);
            pt_msg_clr = extras.getString(PTConstants.PT_MSG_COLOR);
            pt_title = extras.getString(PTConstants.PT_TITLE);
            pt_title_clr = extras.getString(PTConstants.PT_TITLE_COLOR);
            pt_img_small = extras.getString(PTConstants.PT_SMALL_IMG);
            pt_large_icon = extras.getString(PTConstants.PT_NOTIF_ICON);
            pt_bg = extras.getString(PTConstants.PT_BG);
            pt_rating_default_dl = extras.getString(PTConstants.PT_DEFAULT_DL);
            imageList = Utils.getImageListFromExtras(extras);
            deepLinkList = Utils.getDeepLinkListFromExtras(extras);
            bigTextList = Utils.getBigTextFromExtras(extras);
            smallTextList = Utils.getSmallTextFromExtras(extras);
            priceList = Utils.getPriceFromExtras(extras);
            pt_product_display_action = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION);
            pt_product_display_action_clr = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION_COLOUR);
            pt_product_display_linear = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_LINEAR);
            notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            channelId = extras.getString(Constants.WZRK_CHANNEL_ID, "");
            pt_big_img_alt = extras.getString(PTConstants.PT_BIG_IMG_ALT);
            pt_small_icon_clr = extras.getString(PTConstants.PT_SMALL_ICON_COLOUR);
            pt_product_display_action_text_clr = extras.getString(PTConstants.PT_PRODUCT_DISPLAY_ACTION_TEXT_COLOUR);
            requiresChannelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
            asyncHelper = AsyncHelper.getInstance();
            pt_dismiss_intent = extras.getBoolean(PTConstants.PT_DISMISS_INTENT, false);
            pt_rating_toast = extras.getString(PTConstants.PT_RATING_TOAST);
            pt_subtitle = extras.getString(PTConstants.PT_SUBTITLE);
            setKeysFromDashboard(extras);

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

            if (pt_id != null) {
                templateType = TemplateType.fromString(pt_id);
            }
            asyncHelper.postAsyncSafely("PushTemplateReceiver#renderTemplate", new Runnable() {
                @Override
                public void run() {
                    try {
                        if (pt_dismiss_intent) {
                            Utils.deleteSilentNotificationChannel(context);
                            Utils.deleteImageFromStorage(context, intent);
                            return;
                        }
                        if (templateType != null) {
                            switch (templateType) {
                                case RATING:
                                    handleRatingNotification(context, extras);
                                    break;
                                case FIVE_ICONS:
                                    handleFiveCTANotification(context, extras);
                                    break;
                                case PRODUCT_DISPLAY:
                                    handleProductDisplayNotification(context, extras);
                                    break;
                                case INPUT_BOX:
                                    handleInputBoxNotification(context, extras, intent);
                                    break;
                                case MANUAL_CAROUSEL:
                                    handleManualCarouselNotification(context, extras);
                                    break;
                            }
                        }
                    } catch (Throwable t) {
                        PTLog.verbose("Couldn't render notification: " + t.getLocalizedMessage());
                    }
                }
            });

        }
    }

    private void handleManualCarouselNotification(Context context, Bundle extras) {
        try {
            contentViewManualCarousel = new RemoteViews(context.getPackageName(), R.layout.manual_carousel);
            setCustomContentViewBasicKeys(contentViewManualCarousel, context);

            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);

            final boolean rightSwipe = extras.getBoolean(PTConstants.PT_RIGHT_SWIPE);

            imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST);
            deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST);

            int currPosition = extras.getInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT);
            int newPosition;
            if (rightSwipe) {
                contentViewManualCarousel.showNext(R.id.carousel_image);
                contentViewManualCarousel.showNext(R.id.carousel_image_right);
                contentViewManualCarousel.showNext(R.id.carousel_image_left);
                if (currPosition == imageList.size() - 1) {
                    newPosition = 0;
                } else {
                    newPosition = currPosition + 1;
                }
            } else {
                contentViewManualCarousel.showPrevious(R.id.carousel_image);
                contentViewManualCarousel.showPrevious(R.id.carousel_image_right);
                contentViewManualCarousel.showPrevious(R.id.carousel_image_left);
                if (currPosition == 0) {
                    newPosition = imageList.size() - 1;
                } else {
                    newPosition = currPosition - 1;
                }
            }
            String dl = "";

            if (deepLinkList != null && deepLinkList.size() == imageList.size()) {
                dl = deepLinkList.get(newPosition);
            } else if (deepLinkList != null && deepLinkList.size() == 1) {
                dl = deepLinkList.get(0);
            } else if (deepLinkList != null && deepLinkList.size() > newPosition) {
                dl = deepLinkList.get(newPosition);
            } else if (deepLinkList != null && deepLinkList.size() < newPosition) {
                dl = deepLinkList.get(0);
            }

            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, newPosition);
            extras.remove(PTConstants.PT_RIGHT_SWIPE);
            extras.putString(Constants.DEEP_LINK_KEY,dl);

            Intent rightArrowPos0Intent = new Intent(context, PushTemplateReceiver.class);
            rightArrowPos0Intent.putExtra(PTConstants.PT_RIGHT_SWIPE, true);
            rightArrowPos0Intent.putExtra(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition);
            rightArrowPos0Intent.putExtra(PTConstants.PT_NOTIF_ID, notificationId);
            rightArrowPos0Intent.putExtras(extras);
            PendingIntent contentRightPos0Intent = setPendingIntent(context, notificationId, extras, rightArrowPos0Intent, dl);
            contentViewManualCarousel.setOnClickPendingIntent(R.id.rightArrowPos0, contentRightPos0Intent);

            Intent leftArrowPos0Intent = new Intent(context, PushTemplateReceiver.class);
            leftArrowPos0Intent.putExtra(PTConstants.PT_RIGHT_SWIPE, false);
            leftArrowPos0Intent.putExtra(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition);
            leftArrowPos0Intent.putExtra(PTConstants.PT_NOTIF_ID, notificationId);
            leftArrowPos0Intent.putExtras(extras);
            PendingIntent contentLeftPos0Intent = setPendingIntent(context, notificationId, extras, leftArrowPos0Intent, dl);
            contentViewManualCarousel.setOnClickPendingIntent(R.id.leftArrowPos0, contentLeftPos0Intent);


            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);
            PendingIntent pIntent = setPendingIntent(context, notificationId, extras, launchIntent, dl);

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, PTConstants.PT_SILENT_CHANNEL_ID, context);
            Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);

            setSmallIcon(context);

            setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewManualCarousel, pt_title, pIntent, dIntent);

            Notification notification = notificationBuilder.build();

            notificationManager.notify(notificationId, notification);

        } catch (Throwable t) {
            PTLog.verbose("Error creating manual carousel notification ", t);
        }
    }

    private void handleInputBoxNotification(Context context, Bundle extras, Intent intent) {

        //Fetch Remote Input
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
        PendingIntent dIntent;
        dIntent = setDismissIntent(context, extras, dismissIntent);
        config = extras.getParcelable("config");

        if (remoteInput != null) {
            //Fetch Reply
            CharSequence reply = remoteInput.getCharSequence(
                    PTConstants.PT_INPUT_KEY);

            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);

            if (reply != null) {

                PTLog.verbose("Processing Input from Input Template");
                extras.putString(PTConstants.PT_INPUT_KEY, reply.toString());
                Utils.raiseCleverTapEvent(context, config, extras, PTConstants.PT_INPUT_KEY);
                //Update the notification to show that the reply was received.
                final NotificationCompat.Builder repliedNotification;
                if (requiresChannelId) {
                    repliedNotification = new NotificationCompat.Builder(context, PTConstants.PT_SILENT_CHANNEL_ID);
                } else {
                    repliedNotification = new NotificationCompat.Builder(context);
                }
                setSmallIcon(context);
                repliedNotification.setSmallIcon(smallIcon)
                        .setContentTitle(pt_title)
                        .setContentText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                        .setTimeoutAfter(PTConstants.PT_INPUT_TIMEOUT)
                        .setDeleteIntent(dIntent)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true);

                setStandardViewBigImageStyle(pt_big_img_alt, extras, context, repliedNotification);

                Notification notification = repliedNotification.build();
                notificationManager.notify(notificationId, notification);

                /* Check if Auto Open key is present and not empty, if not present then show feedback and
                auto kill in 3 secs. If present, then launch the App with Dl or Launcher activity.
                The launcher activity will get the reply in extras under the key "pt_reply" */
                if (extras.getString(PTConstants.PT_INPUT_AUTO_OPEN) != null || extras.getBoolean(PTConstants.PT_INPUT_AUTO_OPEN)) {
                    //adding delay for launcher
                    try {
                        Thread.sleep(PTConstants.PT_INPUT_TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Intent launchIntent;

                    if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                        launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra(Constants.DEEP_LINK_KEY)));
                        Utils.setPackageNameFromResolveInfoList(context, launchIntent);
                    } else {
                        launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        if (launchIntent == null) {
                            return;
                        }
                    }

                    launchIntent.putExtras(extras);

                    //adding reply to extra
                    launchIntent.putExtra("pt_reply", reply);

                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    context.startActivity(launchIntent);
                }

            } else {
                PTLog.verbose("PushTemplateReceiver: Input is Empty");
            }
        }

    }

    private void handleRatingNotification(Context context, Bundle extras) {
        try {
            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);
            if (extras.getBoolean(PTConstants.DEFAULT_DL, false)) {
                config = extras.getParcelable("config");
                notificationManager.cancel(notificationId);
                Intent launchIntent;
                Class clazz = null;
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService");
                } catch (ClassNotFoundException ex) {
                    PTLog.debug("No Intent Service found");
                }

                boolean isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz);
                if (isPTIntentServiceAvailable) {
                    launchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                    launchIntent.setPackage(context.getPackageName());
                    launchIntent.putExtra("pt_type", TYPE_BUTTON_CLICK);
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra("dl", pt_rating_default_dl);
                    context.startService(launchIntent);
                } else {
                    launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pt_rating_default_dl));
                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    Utils.raiseNotificationClicked(context, extras, config);
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra(Constants.DEEP_LINK_KEY, pt_rating_default_dl);
                    context.startActivity(launchIntent);
                }

                return;
            }
            //Set RemoteViews again
            contentViewRating = new RemoteViews(context.getPackageName(), R.layout.rating);
            setCustomContentViewBasicKeys(contentViewRating, context);

            contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small_single_line_msg);

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

            String pt_dl_clicked = deepLinkList.get(0);

            HashMap<String, Object> map = new HashMap<String, Object>();
            if (clicked1 == extras.getBoolean("click1", false)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 1);
                Utils.raiseNotificationClicked(context, extras, config);
                clicked1 = false;

                if (deepLinkList.size() > 0) {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            } else {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_outline);
            }
            if (clicked2 == extras.getBoolean("click2", false)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 2);
                Utils.raiseNotificationClicked(context, extras, config);
                clicked2 = false;
                if (deepLinkList.size() > 1) {
                    pt_dl_clicked = deepLinkList.get(1);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            } else {
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_outline);
            }
            if (clicked3 == extras.getBoolean("click3", false)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled);
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 3);
                Utils.raiseNotificationClicked(context, extras, config);
                clicked3 = false;
                if (deepLinkList.size() > 2) {
                    pt_dl_clicked = deepLinkList.get(2);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            } else {
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_outline);
            }
            if (clicked4 == extras.getBoolean("click4", false)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_filled);
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 4);
                Utils.raiseNotificationClicked(context, extras, config);
                clicked4 = false;
                if (deepLinkList.size() > 3) {
                    pt_dl_clicked = deepLinkList.get(3);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            } else {
                contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_outline);
            }
            if (clicked5 == extras.getBoolean("click5", false)) {
                contentViewRating.setImageViewResource(R.id.star1, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star2, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star3, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star4, R.drawable.pt_star_filled);
                contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_filled);
                extras.putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + 5);
                Utils.raiseNotificationClicked(context, extras, config);
                clicked5 = false;
                if (deepLinkList.size() > 4) {
                    pt_dl_clicked = deepLinkList.get(4);
                } else {
                    pt_dl_clicked = deepLinkList.get(0);
                }
            } else {
                contentViewRating.setImageViewResource(R.id.star5, R.drawable.pt_star_outline);
            }

            setSmallIcon(context);

            NotificationCompat.Builder notificationBuilder;
            if (requiresChannelId) {
                notificationBuilder = new NotificationCompat.Builder(context, PTConstants.PT_SILENT_CHANNEL_ID);
            } else {
                notificationBuilder = new NotificationCompat.Builder(context);
            }
            Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
            PendingIntent dIntent;
            dIntent = setDismissIntent(context, extras, dismissIntent);

            if (notificationManager != null) {
                //Use the Builder to build notification
                notificationBuilder.setSmallIcon(smallIcon)
                        .setCustomContentView(contentViewSmall)
                        .setCustomBigContentView(contentViewRating)
                        .setContentTitle(pt_title)
                        .setDeleteIntent(dIntent)
                        .setAutoCancel(true);

                Notification notification = notificationBuilder.build();

                notificationManager.notify(notificationId, notification);

                Thread.sleep(1000);
                notificationManager.cancel(notificationId);

                setToast(context, pt_rating_toast);

                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(it);

                Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pt_dl_clicked));
                launchIntent.putExtras(extras);
                launchIntent.putExtra(Constants.DEEP_LINK_KEY, pt_dl_clicked);
                launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }

        } catch (Throwable t) {
            PTLog.verbose("Error creating rating notification ", t);
        }
    }

    private void handleProductDisplayNotification(Context context, Bundle extras) {
        try {
            int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);
            if (buynow == extras.getBoolean(PTConstants.PT_BUY_NOW, false)) {
                notificationManager.cancel(notificationId);
                String dl = extras.getString(PTConstants.PT_BUY_NOW_DL, deepLinkList.get(0));
                config = extras.getParcelable("config");
                notificationManager.cancel(notificationId);
                Intent launchIntent;

                Class clazz = null;
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService");
                } catch (ClassNotFoundException ex) {
                    PTLog.debug("No Intent Service found");
                }

                boolean isPTIntentServiceAvailable = com.clevertap.android.sdk.Utils.isServiceAvailable(context, clazz);
                if (isPTIntentServiceAvailable) {
                    launchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra("dl", dl);
                    launchIntent.setPackage(context.getPackageName());
                    launchIntent.putExtra(PTConstants.PT_TYPE, TYPE_BUTTON_CLICK);
                    context.startService(launchIntent);
                } else {
                    launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                    launchIntent.putExtras(extras);
                    launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl);
                    launchIntent.removeExtra(Constants.WZRK_ACTIONS);
                    launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    Utils.raiseNotificationClicked(context, extras, config);
                    context.startActivity(launchIntent);
                }
                return;
            }

            boolean isLinear = false;
            if (pt_product_display_linear == null || pt_product_display_linear.isEmpty()) {
                contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_template);
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.content_view_small_single_line_msg);
            } else {
                isLinear = true;
                contentViewBig = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_expanded);
                contentViewSmall = new RemoteViews(context.getPackageName(), R.layout.product_display_linear_collapsed);
            }

            setCustomContentViewBasicKeys(contentViewBig, context);
            if (!isLinear) {
                setCustomContentViewBasicKeys(contentViewSmall, context);
            }

            int currentPosition = extras.getInt(PTConstants.PT_CURRENT_POSITION);

            contentViewBig.setDisplayedChild(R.id.carousel_image, currentPosition);
            imageList = extras.getStringArrayList(PTConstants.PT_IMAGE_LIST);
            deepLinkList = extras.getStringArrayList(PTConstants.PT_DEEPLINK_LIST);
            bigTextList = extras.getStringArrayList(PTConstants.PT_BIGTEXT_LIST);
            smallTextList = extras.getStringArrayList(PTConstants.PT_SMALLTEXT_LIST);
            priceList = extras.getStringArrayList(PTConstants.PT_PRICE_LIST);

            String dl = deepLinkList.get(currentPosition);
            contentViewBig.setTextViewText(R.id.product_name, bigTextList.get(currentPosition));
            contentViewBig.setTextViewText(R.id.product_description, smallTextList.get(currentPosition));
            contentViewBig.setTextViewText(R.id.product_price, priceList.get(currentPosition));
            extras.remove(PTConstants.PT_CURRENT_POSITION);

            Intent launchIntent = new Intent(context, PTPushNotificationReceiver.class);

            PendingIntent pIntent;

            Intent notificationIntent4 = new Intent(context, PushTemplateReceiver.class);
            notificationIntent4.putExtra(PTConstants.PT_IMAGE_1, true);
            notificationIntent4.putExtra(PTConstants.PT_NOTIF_ID, notificationId);
            notificationIntent4.putExtra(PTConstants.PT_BUY_NOW_DL, dl);
            notificationIntent4.putExtra(PTConstants.PT_BUY_NOW, true);
            notificationIntent4.putExtras(extras);
            PendingIntent contentIntent4 = PendingIntent.getBroadcast(context, new Random().nextInt(), notificationIntent4, 0);
            contentViewBig.setOnClickPendingIntent(R.id.product_action, contentIntent4);

            NotificationCompat.Builder notificationBuilder = setBuilderWithChannelIDCheck(requiresChannelId, PTConstants.PT_SILENT_CHANNEL_ID, context);

            if (deepLinkList != null && !deepLinkList.isEmpty()) {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, dl);
            } else {
                pIntent = setPendingIntent(context, notificationId, extras, launchIntent, null);
            }

            if (notificationManager != null) {
                //Use the Builder to build notification
                Intent dismissIntent = new Intent(context, PushTemplateReceiver.class);
                PendingIntent dIntent;
                dIntent = setDismissIntent(context, extras, dismissIntent);
                setSmallIcon(context);

                setNotificationBuilderBasics(notificationBuilder, contentViewSmall, contentViewBig, pt_title, pIntent, dIntent);

                Notification notification = notificationBuilder.build();

                notificationManager.notify(notificationId, notification);
            }

        } catch (Throwable t) {
            PTLog.verbose("Error creating product display notification ", t);
        }
    }

    private void handleFiveCTANotification(Context context, Bundle extras) {
        String dl = null;


        int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID);
        if (cta1 == extras.getBoolean("cta1")) {
            dl = deepLinkList.get(0);
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 1 + "_" + dl);
        }
        if (cta2 == extras.getBoolean("cta2")) {
            dl = deepLinkList.get(1);
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 2 + "_" + dl);
        }
        if (cta3 == extras.getBoolean("cta3")) {
            dl = deepLinkList.get(2);
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 3 + "_" + dl);
        }
        if (cta4 == extras.getBoolean("cta4")) {
            dl = deepLinkList.get(3);
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 4 + "_" + dl);
        }
        if (cta5 == extras.getBoolean("cta5")) {
            dl = deepLinkList.get(4);
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 5 + "_" + dl);
        }
        extras.putString(Constants.DEEP_LINK_KEY, dl);

        if (close == extras.getBoolean("close")) {
            extras.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + "close");
            notificationManager.cancel(notificationId);
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }

        Utils.raiseNotificationClicked(context, extras, config);


        if (dl != null) {
            Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
            launchIntent.putExtras(extras);
            launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl);
            launchIntent.removeExtra(Constants.WZRK_ACTIONS);
            launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
    }

    private PendingIntent setPendingIntent(Context context, int notificationId, Bundle extras, Intent launchIntent, String dl) {
        launchIntent.putExtras(extras);
        launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId);
        if (dl != null) {
            launchIntent.putExtra(PTConstants.DEFAULT_DL, true);
            launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl);
        }
        launchIntent.removeExtra(Constants.WZRK_ACTIONS);
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flagsLaunchPendingIntent |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                launchIntent, flagsLaunchPendingIntent);
    }

    private PendingIntent setDismissIntent(Context context, Bundle extras, Intent intent) {
        intent.putExtras(extras);
        intent.putExtra(PTConstants.PT_DISMISS_INTENT, true);
        return PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void setNotificationBuilderBasics(NotificationCompat.Builder notificationBuilder, RemoteViews contentViewSmall, RemoteViews contentViewBig, String pt_title, PendingIntent pIntent, PendingIntent dIntent) {
        notificationBuilder.setSmallIcon(smallIcon)
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setContentTitle(Html.fromHtml(pt_title))
                .setDeleteIntent(dIntent)
                .setContentIntent(pIntent).setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
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
            contentView.setTextColor(R.id.app_name, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS));
            contentView.setTextColor(R.id.timestamp, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS));
            contentView.setTextColor(R.id.subtitle, Utils.getColour(pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS));
            setDotSep(context);
        }
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
            contentView.setTextColor(R.id.msg, Utils.getColour(pt_msg_clr, PTConstants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewTitleColour(RemoteViews contentView, String pt_title_clr) {
        if (pt_title_clr != null && !pt_title_clr.isEmpty()) {
            contentView.setTextColor(R.id.title, Utils.getColour(pt_title_clr, PTConstants.PT_COLOUR_BLACK));
        }
    }

    private void setCustomContentViewExpandedBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.content_view_big, "setBackgroundColor", Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE));
        }
    }

    private void setCustomContentViewCollapsedBackgroundColour(RemoteViews contentView, String pt_bg) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(R.id.content_view_small, "setBackgroundColor", Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE));
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

    private void setStandardViewBigImageStyle(String imgUrl, Bundle extras, Context context, NotificationCompat.Builder notificationBuilder) {
        NotificationCompat.Style bigPictureStyle;
        if (imgUrl != null && imgUrl.startsWith("http")) {
            try {
                Bitmap bpMap = Utils.getNotificationBitmap(imgUrl, false, context);

                if (bpMap == null)
                    throw new Exception("Failed to fetch big picture!");


                bigPictureStyle = new NotificationCompat.BigPictureStyle()
                        .setSummaryText(extras.getString(PTConstants.PT_INPUT_FEEDBACK))
                        .bigPicture(bpMap);

            } catch (Throwable t) {
                bigPictureStyle = new NotificationCompat.BigTextStyle()
                        .bigText(extras.getString(PTConstants.PT_INPUT_FEEDBACK));
                PTLog.verbose("Falling back to big text notification, couldn't fetch big picture", t);
            }
        } else {
            bigPictureStyle = new NotificationCompat.BigTextStyle()
                    .bigText(extras.getString(PTConstants.PT_INPUT_FEEDBACK));
        }

        notificationBuilder.setStyle(bigPictureStyle);

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
            pt_rating_default_dl = extras.getString(Constants.DEEP_LINK_KEY);
        }
        if (pt_meta_clr == null || pt_meta_clr.isEmpty()) {
            pt_meta_clr = extras.getString(Constants.WZRK_COLOR);
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_COLOR);
        }
        if (pt_subtitle == null || pt_subtitle.isEmpty()) {
            pt_subtitle = extras.getString(Constants.WZRK_SUBTITLE);
        }
        if (pt_small_icon_clr == null || pt_small_icon_clr.isEmpty()) {
            pt_small_icon_clr = extras.getString(Constants.WZRK_COLOR);
        }
    }

    private void setToast(Context context, String message) {
        if (message != null && !message.isEmpty()) {
            Utils.showToast(context, message);
        }
    }

    private void setDotSep(Context context) {
        try {
            pt_dot = context.getResources().getIdentifier(PTConstants.PT_DOT_SEP, "drawable", context.getPackageName());
            pt_dot_sep = Utils.setBitMapColour(context, pt_dot, pt_meta_clr);
        } catch (NullPointerException e) {
            PTLog.debug("NPE while setting dot sep color");
        }
    }

}
