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
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider;
import com.clevertap.android.sdk.utils.UriHelper;
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

    private InAppResourceProvider provider;

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
            provider = new InAppResourceProvider(context, logger);
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

    Bundle didClick(CTInAppNotificationButton button) {
        return actionTriggered(button.getAction(), button.getText(), null);
    }

    Bundle actionTriggered(CTInAppAction action, String callToAction, @Nullable Bundle additionalData) {
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

    void openActionUrl(String url) {
        try {
            final Bundle formData = UriHelper.getAllKeyValuePairs(url, false);

            String actionParts = formData.getString(Constants.KEY_C2A);
            String callToAction = null;
            if (actionParts != null) {
                final String[] parts = actionParts.split("__dl__");
                if (parts.length == 2) {
                    // Decode it here as wzrk_c2a is not decoded by UriHelper
                    callToAction = URLDecoder.decode(parts[0], "UTF-8");
                    url = parts[1];
                }
            }

            CTInAppAction action = CTInAppAction.createOpenUrlAction(url);
            config.getLogger().debug("Executing call to action for in-app: " + url);
            Bundle actionData = actionTriggered(action, callToAction != null ? callToAction : "", formData);
            didDismiss(actionData);
        } catch (Throwable t) {
            config.getLogger().debug("Error parsing the in-app notification action!", t);
        }
    }

    public void didDismiss(Bundle data) {
        cleanup();
        InAppListener listener = getListener();
        if (listener != null && getActivity() != null && getActivity().getBaseContext() != null) {
            listener.inAppNotificationDidDismiss(getActivity().getBaseContext(), inAppNotification, data);
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

    public InAppResourceProvider resourceProvider() {
        return provider;
    }

}
