package com.clevertap.android.sdk.inapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DidClickForHardPermissionListener;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Set;
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
    public void onAttach(Context context) {
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        didShow(null);
    }

    abstract void cleanup();

    void didClick(Bundle data, HashMap<String, String> keyValueMap) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidClick(inAppNotification, data, keyValueMap);
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

    void fireUrlThroughIntent(String url, Bundle formData) {
        try {
            Uri uri = Uri.parse(url.replace("\n", "").replace("\r", ""));
            Set<String> queryParamSet = uri.getQueryParameterNames();
            Bundle queryBundle = new Bundle();
            if (queryParamSet != null && !queryParamSet.isEmpty()) {
                for (String queryName : queryParamSet) {
                    queryBundle.putString(queryName, uri.getQueryParameter(queryName));
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (!queryBundle.isEmpty()) {
                intent.putExtras(queryBundle);
            }
            Utils.setPackageNameFromResolveInfoList(getActivity(), intent);
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
        didDismiss(formData);
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
            Bundle data = new Bundle();

            data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
            data.putString(Constants.KEY_C2A, button.getText());

            didClick(data, button.getKeyValues());

            if (index == 0 && inAppNotification.isLocalInApp() &&
                    didClickForHardPermissionListener != null) {
                didClickForHardPermissionListener.didClickForHardPermissionWithFallbackSettings(
                        inAppNotification.fallBackToNotificationSettings());
                return;
            }else if (index == 1 && inAppNotification.isLocalInApp()){
                didDismiss(data);
                return;
            }

            if (button.getType() != null && button.getType().contains(
                    Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION)
                    && didClickForHardPermissionListener != null){
                didClickForHardPermissionListener.
                        didClickForHardPermissionWithFallbackSettings(button.isFallbackToSettings());
                return;
            }
            String actionUrl = button.getActionUrl();
            if (actionUrl != null) {
                fireUrlThroughIntent(actionUrl, data);
                return;
            }
            didDismiss(data);

        } catch (Throwable t) {
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
            didDismiss(null);
        }
    }

    public InAppResourceProvider resourceProvider() {
        return provider;
    }

}
