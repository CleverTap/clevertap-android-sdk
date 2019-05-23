package com.clevertap.android.sdk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public final class InAppNotificationActivity extends FragmentActivity implements CTInAppBaseFragment.InAppListener {

    interface InAppActivityListener {
        void inAppNotificationDidShow(Context context, CTInAppNotification inAppNotification, Bundle formData);
        void inAppNotificationDidClick(Context context, CTInAppNotification inAppNotification, Bundle formData);
        void inAppNotificationDidDismiss(Context context, CTInAppNotification inAppNotification, Bundle formData);
    }

    private CTInAppNotification inAppNotification;
    private CleverTapInstanceConfig config;
    private WeakReference<InAppActivityListener> listenerWeakReference;
    private static boolean isAlertVisible = false;

    void setListener(InAppActivityListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    InAppActivityListener getListener() {
        InAppActivityListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),"InAppActivityListener is null for notification: " + inAppNotification.getJsonDescription());
        }
        return listener;
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(android.R.style.Theme_Translucent_NoTitleBar);
    }

    private String getFragmentTag() {
        return config.getAccountId() +":CT_INAPP_CONTENT_FRAGMENT";
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            Bundle notif = getIntent().getExtras();
            if (notif == null) throw new IllegalArgumentException();
            inAppNotification = notif.getParcelable("inApp");
            config = notif.getParcelable("config");
            setListener(CleverTapAPI.instanceWithConfig(getApplicationContext(),config));
        } catch (Throwable t) {
            Logger.v("Cannot find a valid notification bundle to show!", t);
            return;
        }

        //Allow rotation for all InApps but respect the flags sent from dashboard
        if (inAppNotification.isPortrait() && !inAppNotification.isLandscape()) {
            try {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } catch (Throwable t) {
                Logger.d("Error displaying InAppNotification", t);
                int orientation = this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Logger.d("App in Landscape, dismissing portrait InApp Notification");
                    finish();
                    didDismiss(null);
                    return;
                } else {
                    Logger.d("App in Portrait, displaying InApp Notification anyway");
                }
            }
        }

        if (!inAppNotification.isPortrait() && inAppNotification.isLandscape()) {
            try {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } catch (Throwable t) {
                Logger.d("Error displaying InAppNotification", t);
                int orientation = this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Logger.d("App in Portrait, dismissing landscape InApp Notification");
                    finish();
                    didDismiss(null);
                    return;
                } else {
                    Logger.d("App in Landscape, displaying InApp Notification anyway");
                }
            }
        }

        CTInAppBaseFullFragment contentFragment;
        if (savedInstanceState == null) {
            contentFragment = createContentFragment();
            if(contentFragment!=null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("inApp", inAppNotification);
                bundle.putParcelable("config", config);
                contentFragment.setArguments(bundle);
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .add(android.R.id.content, contentFragment, getFragmentTag())
                        .commit();
            }
        }else if(isAlertVisible){
            createContentFragment();
        }
    }

    private CTInAppBaseFullFragment createContentFragment(){
        CTInAppType type = inAppNotification.getInAppType();
        CTInAppBaseFullFragment viewFragment = null;
        switch(type) {
            case CTInAppTypeCoverHTML: {
                viewFragment = new CTInAppHtmlCoverFragment();
                break;
            }
            case CTInAppTypeInterstitialHTML:{
                viewFragment = new CTInAppHtmlInterstitialFragment();
                break;
            }
            case CTInAppTypeHalfInterstitialHTML: {
                viewFragment = new CTInAppHtmlHalfInterstitialFragment();
                break;
            }
            case CTInAppTypeCover: {
                viewFragment = new CTInAppNativeCoverFragment();
                break;
            }
            case CTInAppTypeInterstitial: {
                viewFragment = new CTInAppNativeInterstitialFragment();
                break;
            }
            case CTInAppTypeHalfInterstitial: {
                viewFragment = new CTInAppNativeHalfInterstitialFragment();
                break;
            }
            case CTInAppTypeCoverImageOnly: {
                viewFragment = new CTInAppNativeCoverImageFragment();
                break;
            }
            case CTInAppTypeInterstitialImageOnly: {
                viewFragment = new CTInAppNativeInterstitialImageFragment();
                break;
            }
            case CTInAppTypeHalfInterstitialImageOnly: {
                viewFragment = new CTInAppNativeHalfInterstitialImageFragment();
                break;
            }
            case CTInAppTypeAlert: {
                AlertDialog alertDialog = null;
                if(inAppNotification.getButtons().size()>0) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        alertDialog = new AlertDialog.Builder(InAppNotificationActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert)
                                .setCancelable(false)
                                .setTitle(inAppNotification.getTitle())
                                .setMessage(inAppNotification.getMessage())
                                .setPositiveButton(inAppNotification.getButtons().get(0).getText(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Bundle data = new Bundle();
                                        data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
                                        data.putString("wzrk_c2a", inAppNotification.getButtons().get(0).getText());
                                        didClick(data);
                                        String actionUrl = inAppNotification.getButtons().get(0).getActionUrl();
                                        if (actionUrl != null) {
                                            fireUrlThroughIntent(actionUrl, data);
                                            return;
                                        }
                                        didDismiss(data);
                                    }
                                })
                                .create();
                        if (inAppNotification.getButtons().size() == 2) {
                            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, inAppNotification.getButtons().get(1).getText(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Bundle data = new Bundle();
                                    data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
                                    data.putString("wzrk_c2a", inAppNotification.getButtons().get(1).getText());
                                    didClick(data);
                                    String actionUrl = inAppNotification.getButtons().get(1).getActionUrl();
                                    if (actionUrl != null) {
                                        fireUrlThroughIntent(actionUrl, data);
                                        return;
                                    }
                                    didDismiss(data);
                                }
                            });
                        }
                    } else {
                        alertDialog = new AlertDialog.Builder(InAppNotificationActivity.this)
                                .setCancelable(false)
                                .setTitle(inAppNotification.getTitle())
                                .setMessage(inAppNotification.getMessage())
                                .setPositiveButton(inAppNotification.getButtons().get(0).getText(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Bundle data = new Bundle();
                                        data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
                                        data.putString("wzrk_c2a", inAppNotification.getButtons().get(0).getText());
                                        didClick(data);
                                        String actionUrl = inAppNotification.getButtons().get(0).getActionUrl();
                                        if (actionUrl != null) {
                                            fireUrlThroughIntent(actionUrl, data);
                                            return;
                                        }
                                        didDismiss(data);
                                    }
                                }).create();
                        if (inAppNotification.getButtons().size() == 2) {
                            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, inAppNotification.getButtons().get(1).getText(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Bundle data = new Bundle();
                                    data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
                                    data.putString("wzrk_c2a", inAppNotification.getButtons().get(1).getText());
                                    didClick(data);
                                    String actionUrl = inAppNotification.getButtons().get(1).getActionUrl();
                                    if (actionUrl != null) {
                                        fireUrlThroughIntent(actionUrl, data);
                                        return;
                                    }
                                    didDismiss(data);
                                }
                            });
                        }
                    }
                    //By default, we will allow 2 button alerts and set a third button if it is configured
                    if (inAppNotification.getButtons().size() > 2) {
                        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, inAppNotification.getButtons().get(2).getText(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Bundle data = new Bundle();
                                data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
                                data.putString("wzrk_c2a", inAppNotification.getButtons().get(2).getText());
                                didClick(data);
                                String actionUrl = inAppNotification.getButtons().get(2).getActionUrl();
                                if (actionUrl != null) {
                                    fireUrlThroughIntent(actionUrl, data);
                                    return;
                                }
                                didDismiss(data);
                            }
                        });
                    }
                }
                //noinspection ConstantConditions
                alertDialog.show();
                isAlertVisible = true;
                didShow(null);
                break;
            }
            default: {
                config.getLogger().verbose("InAppNotificationActivity: Unhandled InApp Type: " + type);
                break;
            }
        }
        return viewFragment;
    }

    void didClick(Bundle data) {
        InAppActivityListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidClick(getBaseContext(),inAppNotification, data);
        }
    }

    void didShow(Bundle data) {
        InAppActivityListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidShow(getBaseContext(),inAppNotification, data);
        }
    }

    void didDismiss(Bundle data) {
        if(isAlertVisible){
            isAlertVisible  = false;
        }
        finish();
        InAppActivityListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidDismiss(getBaseContext(),inAppNotification, data);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
        didDismiss(null);
    }

    @Override
    public void inAppNotificationDidShow(Context context, CTInAppNotification inAppNotification, Bundle formData) {
        didShow(formData);
    }

    @Override
    public void inAppNotificationDidClick(Context context, CTInAppNotification inAppNotification, Bundle formData) {
       didClick(formData);
    }

    @Override
    public void inAppNotificationDidDismiss(final Context context, final CTInAppNotification inAppNotification, Bundle formData) {
        didDismiss(formData);
    }

    void fireUrlThroughIntent(String url, Bundle formData) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
        didDismiss(formData);
    }
}
