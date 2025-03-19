package com.clevertap.android.sdk.inapp;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DidClickForHardPermissionListener;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.inapp.images.FileResourceProvider;
import com.clevertap.android.sdk.utils.UriHelper;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CTInAppBaseFragment extends Fragment {

    class CTInAppNativeButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            handleButtonClickAtIndex((int) view.getTag());
        }
    }

    CloseImageView closeImageView = null;

    CleverTapInstanceConfig config;

    Context context;

    int currentOrientation;

    CTInAppNotification inAppNotification;

    AtomicBoolean isCleanedUp = new AtomicBoolean();

    private WeakReference<InAppListener> listenerWeakReference;

    private DidClickForHardPermissionListener didClickForHardPermissionListener;

    private FileResourceProvider provider;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Bundle bundle = getArguments();
        if (bundle != null) {
            inAppNotification = bundle.getParcelable(Constants.INAPP_KEY);
            config = bundle.getParcelable(Constants.KEY_CONFIG);
            Logger logger = null;
            if (config != null) {
                logger = config.getLogger();
            }
            provider = FileResourceProvider.getInstance(context, logger);
            currentOrientation = getResources().getConfiguration().orientation;
            generateListener();
            /*Initialize the below listener only when in app has InAppNotification activity as their host activity
            when requesting permission for notification.*/
            if (context instanceof DidClickForHardPermissionListener) {
                didClickForHardPermissionListener = (DidClickForHardPermissionListener) context;
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        didShow(null);
    }

    abstract void cleanup();

    public void triggerAction(
            @NonNull CTInAppAction action,
            @Nullable String callToAction,
            @Nullable Bundle additionalData) {
        if (action.getType() == InAppActionType.OPEN_URL) {
            //All URL parameters should be tracked as additional data
            final Bundle urlActionData = UriHelper.getAllKeyValuePairs(action.getActionUrl(), false);

            // callToAction is handled as a parameter
            String callToActionUrlParam = urlActionData.getString(Constants.KEY_C2A);
            // no need to keep it in the data bundle
            urlActionData.remove(Constants.KEY_C2A);

            // add all additional params, overriding the url params if there is a collision
            if (additionalData != null) {
                urlActionData.putAll(additionalData);
            }
            // Use the merged data for the action
            additionalData = urlActionData;
            if (callToActionUrlParam != null) {
                // check if there is a deeplink within the callToAction param
                final String[] parts = callToActionUrlParam.split(Constants.URL_PARAM_DL_SEPARATOR);
                if (parts.length == 2) {
                    // Decode it here as it is not decoded by UriHelper
                    try {
                        // Extract the actual callToAction value
                        callToActionUrlParam = URLDecoder.decode(parts[0], "UTF-8");
                    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                        config.getLogger().debug("Error parsing c2a param", e);
                    }
                    // use the url from the callToAction param
                    action = CTInAppAction.createOpenUrlAction(parts[1]);
                }
            }
            if (callToAction == null) {
                // Use the url param value only if no other value is passed
                callToAction = callToActionUrlParam;
            }
        }
        Bundle actionData = notifyActionTriggered(action, callToAction != null ? callToAction : "", additionalData);
        didDismiss(actionData);
    }

    void openActionUrl(String url) {
        triggerAction(CTInAppAction.createOpenUrlAction(url), null, null);
    }

    public void didDismiss(Bundle data) {
        cleanup();
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidDismiss(inAppNotification, data);
        }
    }

    @SuppressWarnings("SameParameterValue")
    void didShow(Bundle data) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidShow(inAppNotification, data);
        }
    }

    abstract void generateListener();

    InAppListener getListener() {
        InAppListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),
                    "InAppListener is null for notification: " + inAppNotification.getJsonDescription());
        }
        return listener;
    }

    void setListener(InAppListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
    }

    void handleButtonClickAtIndex(int index) {
        try {
            CTInAppNotificationButton button = inAppNotification.getButtons().get(index);
            Bundle clickData = didClick(button);

            if (index == 0 && inAppNotification.isLocalInApp() && didClickForHardPermissionListener != null) {
                didClickForHardPermissionListener.didClickForHardPermissionWithFallbackSettings(
                        inAppNotification.fallBackToNotificationSettings());
                return;
            }

            CTInAppAction action = button.getAction();
            if (action != null && InAppActionType.REQUEST_FOR_PERMISSIONS == action.getType()
                    && didClickForHardPermissionListener != null) {
                didClickForHardPermissionListener.
                        didClickForHardPermissionWithFallbackSettings(action.shouldFallbackToSettings());
                return;
            }

            didDismiss(clickData);
        } catch (Throwable t) {
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
            didDismiss(null);
        }
    }

    public FileResourceProvider resourceProvider() {
        return provider;
    }

    private Bundle didClick(CTInAppNotificationButton button) {
        CTInAppAction action = button.getAction();
        if (action == null) {
            action = CTInAppAction.createCloseAction();
        }
        return notifyActionTriggered(action, button.getText(), null);
    }

    private Bundle notifyActionTriggered(
            @NonNull CTInAppAction action,
            @NonNull String callToAction,
            @Nullable Bundle additionalData) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationActionTriggered(
                    inAppNotification,
                    action,
                    callToAction,
                    additionalData,
                    getActivity());
        } else {
            return null;
        }
    }
}
