package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;

import com.clevertap.android.sdk.inapp.CTInAppAction;
import com.clevertap.android.sdk.inapp.InAppDisplayListener;
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFullFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlCoverFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlHalfInterstitialFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlInterstitialFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeCoverFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeCoverImageFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHalfInterstitialFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHalfInterstitialImageFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeInterstitialFragment;
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeInterstitialImageFragment;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton;
import com.clevertap.android.sdk.inapp.CTInAppType;
import com.clevertap.android.sdk.inapp.InAppActionType;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inapp.InAppListener;

import java.lang.ref.WeakReference;
import java.util.List;

public final class InAppNotificationActivity extends FragmentActivity implements InAppListener,
        DidClickForHardPermissionListener, PushPermissionHandler.PushPermissionResultCallback, InAppDisplayListener,
        InAppController.RestoredInAppListener {

    private final static String INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT = "displayPushPermissionPrompt";
    private final static String INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS = "shouldShowFallbackSettings";
    private final static String INTENT_EXTRA_CT_CONFIG = "config";

    private static boolean isAlertVisible = false;

    private CleverTapInstanceConfig config;

    private CTInAppNotification inAppNotification;

    private WeakReference<InAppListener> listenerWeakReference;

    private PushPermissionHandler pushPermissionHandler;

    private boolean invokedCallbacks = false;

    // Restoring the in-app after a process kill: the activity has no content until it is loaded.
    private static final long RESTORE_TIMEOUT_MS = 4000;

    private final Handler restoreTimeoutHandler = new Handler(Looper.getMainLooper());

    private boolean restoreFinished = false;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void launchForPushPermissionPrompt(
            Activity activity,
            CleverTapInstanceConfig config,
            boolean showFallbackSettings) {
        if (!activity.getClass().equals(InAppNotificationActivity.class)) {
            Intent intent = new Intent(activity, InAppNotificationActivity.class);
            intent.putExtra(INTENT_EXTRA_CT_CONFIG, config);
            intent.putExtra(INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT, true);
            intent.putExtra(INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS, showFallbackSettings);
            activity.startActivity(intent);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void launchForInAppNotification(
            Context context,
            CTInAppNotification inAppNotification,
            CleverTapInstanceConfig config) {
        Intent intent = new Intent(context, InAppNotificationActivity.class);
        // Only the id is sent; the in-app is read from InAppController. Sending the whole object
        // can exceed the Binder limit for large HTML (TransactionTooLargeException, SDK-6299).
        intent.putExtra(Constants.INAPP_ID_KEY, inAppNotification.getCampaignId());
        intent.putExtra(INTENT_EXTRA_CT_CONFIG, config);
        config.getLogger().verbose(config.getAccountId(),
                "Launching InAppNotificationActivity with in-app id: " + inAppNotification.getCampaignId());
        context.startActivity(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isInAppMissing()) {
            // The in-app is no longer in memory, e.g. Android recreated this activity after the app
            // process was killed. A restored in-app fragment removes itself. Try to show the in-app
            // again from storage; close if that cannot start.
            if (!restoreInApp()) {
                Logger.v("In-app " + getIntent().getStringExtra(Constants.INAPP_ID_KEY)
                        + " is no longer available, closing InAppNotificationActivity");
                finish();
            }
            return;
        }
        setUp(savedInstanceState);
    }

    private void setUp(@Nullable Bundle savedInstanceState) {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                didDismiss(null);
            }
        });

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
            }
        }
        try {
            Bundle intentExtras = getIntent().getExtras();
            if (intentExtras == null) {
                throw new IllegalArgumentException();
            }


            config = intentExtras.getParcelable(INTENT_EXTRA_CT_CONFIG);
            if (config == null) {
                throw new IllegalArgumentException();
            }

            CoreState ctState = CleverTapAPI.instanceWithConfig(this, config).getCoreState();

            pushPermissionHandler = new PushPermissionHandler(
                    config,
                    ctState.getCallbackManager().getPushPermissionResponseListenerList(),
                    this);

            boolean showHardNotificationPermission = intentExtras.getBoolean(
                    INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT,
                    false);
            if (showHardNotificationPermission) {
                boolean shouldShowFallbackSettings = intentExtras.getBoolean(
                        INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS,
                        false);
                showPushPermissionPrompt(shouldShowFallbackSettings);
                return;
            }

            setListener(ctState.getInAppController());
            ctState.getInAppController().registerInAppDisplayListener(this);

            String inAppId = intentExtras.getString(Constants.INAPP_ID_KEY);
            inAppNotification = InAppController.getDisplayingInApp(inAppId);
            if (inAppNotification == null) {
                // Not in memory (e.g. the app process was killed): close without tracking.
                config.getLogger().verbose(config.getAccountId(),
                        "In-app " + inAppId + " is no longer available, closing InAppNotificationActivity");
                finish();
                return;
            }
            config.getLogger().verbose(config.getAccountId(),
                    "InAppNotificationActivity loaded in-app " + inAppId + " from memory");
        } catch (Throwable t) {
            Logger.v("Cannot find a valid notification bundle to show!", t);
            finish();
            return;
        }

        //Allow rotation for all InApps but respect the flags sent from dashboard
        if (inAppNotification.isPortrait() && !inAppNotification.isLandscape()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Logger.d("App in Landscape, dismissing portrait InApp Notification");
                finish();
                didDismiss(null);
                return;
            } else {
                Logger.d("App in Portrait, displaying InApp Notification anyway");
            }
        }

        if (!inAppNotification.isPortrait() && inAppNotification.isLandscape()) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Logger.d("App in Portrait, dismissing landscape InApp Notification");
                finish();
                didDismiss(null);
                return;
            } else {
                Logger.d("App in Landscape, displaying InApp Notification anyway");
            }
        }

        CTInAppBaseFullFragment contentFragment;
        if (savedInstanceState == null) {
            contentFragment = createContentFragment();
            if (contentFragment != null) {
                contentFragment.setArguments(inAppNotification, config);
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .add(android.R.id.content, contentFragment, getFragmentTag())
                        .commitNow();
            }
        } else if (isAlertVisible) {
            createContentFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pushPermissionHandler != null) { // null while an in-app is being restored
            pushPermissionHandler.onActivityResume(this);
        }
    }

    /**
     * Starts showing the in-app again from storage after the app process was killed.
     *
     * @return false when it cannot start, so the activity should close.
     */
    private boolean restoreInApp() {
        // After a process kill this is often the call that creates the SDK instance for the new
        // process. As in setUp, any error closes the screen instead of crashing the app.
        try {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                return false;
            }
            String inAppId = extras.getString(Constants.INAPP_ID_KEY);
            CleverTapInstanceConfig restoreConfig = extras.getParcelable(INTENT_EXTRA_CT_CONFIG);
            if (inAppId == null || restoreConfig == null) {
                return false;
            }
            CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(this, restoreConfig);
            if (cleverTapAPI == null) {
                return false;
            }
            Logger.v("In-app " + inAppId + " is not in memory, restoring it from storage");
            // This window is transparent and has no content yet, but it still covers the app and
            // would take every tap. Let taps through to the app until the in-app is ready.
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            restoreTimeoutHandler.postDelayed(this::onRestoreTimeout, RESTORE_TIMEOUT_MS);
            cleverTapAPI.getCoreState().getInAppController().restoreDisplayedInApp(inAppId, this);
            return true;
        } catch (Throwable t) {
            Logger.v("Could not start restoring the in-app, closing InAppNotificationActivity", t);
            return false;
        }
    }

    /**
     * Result of {@link InAppController#restoreDisplayedInApp}. The activity is the listener itself,
     * so the controller can hold it weakly.
     */
    @Override
    public boolean onRestoreResult(@Nullable CTInAppNotification restoredInApp) {
        if (restoreFinished || isFinishing() || isDestroyed()) {
            return false;
        }
        restoreFinished = true;
        restoreTimeoutHandler.removeCallbacksAndMessages(null);
        if (restoredInApp == null || getSupportFragmentManager().isStateSaved()) {
            Logger.v("In-app " + getIntent().getStringExtra(Constants.INAPP_ID_KEY)
                    + " could not be restored, closing InAppNotificationActivity");
            finish();
            return false;
        }
        // The restored in-app is now the one on screen, so setUp finds it in memory. Pass no saved
        // state so a new fragment is added; the one Android restored removed itself.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        setUp(null);
        // setUp closes the activity in some cases (e.g. a portrait-only in-app in landscape). When it
        // did so through didDismiss, the controller already frees the in-app slot on that path, so
        // true keeps it from doing it twice. Otherwise false makes the controller free the slot.
        return !isFinishing() || invokedCallbacks;
    }

    private void onRestoreTimeout() {
        if (restoreFinished) {
            return;
        }
        restoreFinished = true;
        Logger.v("Restoring in-app " + getIntent().getStringExtra(Constants.INAPP_ID_KEY)
                + " took too long, closing InAppNotificationActivity");
        finish();
    }

    @SuppressLint("WrongConstant")
    @Override
    public void finish() {
        super.finish();
        if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Nullable
    @Override
    public Bundle inAppNotificationDidClick(
            @NonNull final CTInAppNotification inAppNotification,
            @NonNull final CTInAppNotificationButton button,
            final int buttonIndex,
            @Nullable final Context activityContext) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationDidClick(inAppNotification, button, buttonIndex, this);
        } else {
            return null;
        }
    }

    @Override
    public void inAppNotificationDidDismiss(
            @NonNull final CTInAppNotification inAppNotification,
            @Nullable Bundle formData) {
        didDismiss(formData);
    }

    @Override
    public void inAppNotificationDidShow(@NonNull CTInAppNotification inAppNotification, @Nullable Bundle formData) {
        didShow(formData);
    }

    @Nullable
    @Override
    public Bundle inAppNotificationActionTriggered(
            @NonNull final CTInAppNotification inAppNotification,
            @NonNull final CTInAppAction action,
            @NonNull final String callToAction,
            @Nullable final Bundle additionalData,
            @Nullable final Context activityContext) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationActionTriggered(
                    inAppNotification,
                    action,
                    callToAction,
                    additionalData,
                    this);
        } else {
            return null;
        }
    }

    @Override
    public void didClickForHardPermissionWithFallbackSettings(boolean fallbackToSettings) {
        showPushPermissionPrompt(fallbackToSettings);
    }

    @Override
    public void didCancelPermissionRequest() {
        pushPermissionHandler.notifyPushPermissionExternalListeners(this);
    }

    public void showPushPermissionPrompt(boolean isFallbackSettingsEnabled) {
        pushPermissionHandler.requestPermission(this, isFallbackSettingsEnabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        pushPermissionHandler.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onPushPermissionResult(boolean isGranted) {
        Bundle data = null;
        if (inAppNotification != null && inAppNotification.isLocalInApp()) {
            data = new Bundle();
            data.putString(Constants.KEY_C2A, inAppNotification.getButtons().get(0).getText());
            data.putString(Constants.NOTIFICATION_ID_TAG, "");
        }
        didDismiss(data);
    }

    void didDismiss(Bundle data) {
        didDismiss(data, true);
    }

    void didDismiss(Bundle data, boolean killActivity) {
        if (isAlertVisible) {
            isAlertVisible = false;
        }

        if (!invokedCallbacks) {
            InAppListener listener = getListener();
            if (listener != null && inAppNotification != null) {
                listener.inAppNotificationDidDismiss(inAppNotification, data);
            }
            invokedCallbacks = true;
        }
        if (killActivity) {
            finish();
        }
    }

    void didShow(Bundle data) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidShow(inAppNotification, data);
        }
    }

    InAppListener getListener() {
        InAppListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null && inAppNotification != null) {
            config.getLogger().verbose(config.getAccountId(),
                    "InAppActivityListener is null for notification: " + inAppNotification.getJsonDescription());
        }
        return listener;
    }

    void setListener(InAppListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    @Nullable
    private Bundle didClick(CTInAppNotificationButton button, int buttonIndex) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationDidClick(inAppNotification, button, buttonIndex, this);
        } else {
            return null;
        }
    }

    private CTInAppBaseFullFragment createContentFragment() {
        CTInAppType type = inAppNotification.getInAppType();
        CTInAppBaseFullFragment viewFragment = null;
        switch (type) {
            case CTInAppTypeCoverHTML: {
                viewFragment = new CTInAppHtmlCoverFragment();
                break;
            }
            case CTInAppTypeInterstitialHTML: {
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
                showAlertDialogForInApp();
                break;
            }
            default: {
                config.getLogger().verbose("InAppNotificationActivity: Unhandled InApp Type: " + type);
                break;
            }
        }
        return viewFragment;
    }

    private String getFragmentTag() {
        return config.getAccountId() + ":CT_INAPP_CONTENT_FRAGMENT";
    }

    /**
     * True when this activity was opened for an in-app that is no longer in memory, which happens
     * when Android recreates it after the app process was killed.
     */
    private boolean isInAppMissing() {
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(Constants.INAPP_ID_KEY)) {
            return false;
        }
        return InAppController.getDisplayingInApp(extras.getString(Constants.INAPP_ID_KEY)) == null;
    }

    private void showAlertDialogForInApp() {
        List<CTInAppNotificationButton> buttons = inAppNotification.getButtons();
        if (buttons.isEmpty()) {
            config.getLogger()
                    .debug("InAppNotificationActivity: Notification has no buttons, not showing Alert InApp");
            return;
        }
        AlertDialog alertDialog;
        CTInAppNotificationButton positiveButton = buttons.get(0);


            alertDialog = new AlertDialog.Builder(InAppNotificationActivity.this,
                    android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setCancelable(false)
                    .setTitle(inAppNotification.getTitle())
                    .setMessage(inAppNotification.getMessage())
                    .setPositiveButton(positiveButton.getText(),
                            (dialogInterface, i) -> onAlertButtonClick(positiveButton, 0, true))
                    .create();

            if (inAppNotification.getButtons().size() == 2) {
                CTInAppNotificationButton negativeButton = buttons.get(1);
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                        negativeButton.getText(),
                        (dialog, which) -> onAlertButtonClick(negativeButton, 1, false));
            }

        //By default, we will allow 2 button alerts and set a third button if it is configured
        if (buttons.size() > 2) {
            CTInAppNotificationButton button = buttons.get(2);
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                    button.getText(),
                    (dialogInterface, i) -> onAlertButtonClickLegacy(button, 2));
        }

        alertDialog.show();
        isAlertVisible = true;
        didShow(null);
    }

    private void onAlertButtonClickLegacy(final CTInAppNotificationButton button, int buttonIndex) {
        Bundle clickData = didClick(button, buttonIndex);
        didDismiss(clickData);
    }

    private void onAlertButtonClick(CTInAppNotificationButton button, int buttonIndex, boolean isPositive) {
        Bundle clickData = didClick(button, buttonIndex);

        if (inAppNotification.isLocalInApp()) {
            if(isPositive) {
                showPushPermissionPrompt(inAppNotification.getFallBackToNotificationSettings());
                return;
            } else {
                didCancelPermissionRequest();
            }
        }

        CTInAppAction action = button.action;
        if (action != null && InAppActionType.REQUEST_FOR_PERMISSIONS == action.getType()) {
            showPushPermissionPrompt(action.shouldFallbackToSettings());
            return;
        }

        didDismiss(clickData);
    }

    @Override
    public void hideInApp() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restoreTimeoutHandler.removeCallbacksAndMessages(null);

        if (config != null) {
            CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(this, config);
            if (cleverTapAPI != null) {
                CoreState ctState = cleverTapAPI.getCoreState();
                if (ctState != null) {
                    ctState.getInAppController().unregisterInAppDisplayListener();
                }
            }
        }
        if (!isChangingConfigurations()) {
            didDismiss(null, false);
        }
    }
}
