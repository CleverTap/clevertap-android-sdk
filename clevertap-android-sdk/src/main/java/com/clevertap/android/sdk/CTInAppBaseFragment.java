package com.clevertap.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CTInAppBaseFragment extends Fragment {

    void didClick(Bundle data, HashMap<String, String> keyValueMap) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidClick(inAppNotification, data, keyValueMap);
        }
    }

    CTInAppNotification inAppNotification;
    CleverTapInstanceConfig config;
    private WeakReference<CTInAppBaseFragment.InAppListener> listenerWeakReference;
    CloseImageView closeImageView = null;
    int currentOrientation;

    Activity parent;
    AtomicBoolean isCleanedUp = new AtomicBoolean();

    void setListener(InAppListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    InAppListener getListener() {
        InAppListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(), "InAppListener is null for notification: " + inAppNotification.getJsonDescription());
        }
        return listener;
    }

    abstract void cleanup();
    abstract void generateListener();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parent = activity;
        Bundle bundle = getArguments();
        inAppNotification = bundle.getParcelable(Constants.INAPP_KEY);
        config = bundle.getParcelable(Constants.KEY_CONFIG);
        currentOrientation = getResources().getConfiguration().orientation;
        generateListener();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        didShow(null);
    }

    void handleButtonClickAtIndex(int index) {
        try {
            CTInAppNotificationButton button = inAppNotification.getButtons().get(index);
            Bundle data = new Bundle();

            data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
            data.putString(Constants.KEY_C2A, button.getText());

            didClick(data, button.getKeyValues());

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

    @SuppressWarnings("SameParameterValue")
    void didShow(Bundle data) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidShow(inAppNotification, data);
        }
    }

    void didDismiss(Bundle data) {
        cleanup();
        InAppListener listener = getListener();
        if (listener != null && getActivity() != null && getActivity().getBaseContext() != null) {
            listener.inAppNotificationDidDismiss(getActivity().getBaseContext(), inAppNotification, data);
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
            Utils.setPackageNameFromResolveInfoList(getActivity(),intent);
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
        didDismiss(formData);
    }

    int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
    }

    interface InAppListener {
        void inAppNotificationDidShow(CTInAppNotification inAppNotification, Bundle formData);

        void inAppNotificationDidClick(CTInAppNotification inAppNotification, Bundle formData, HashMap<String, String> keyValueMap);

        void inAppNotificationDidDismiss(Context context, CTInAppNotification inAppNotification, Bundle formData);
    }

    class CTInAppNativeButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            handleButtonClickAtIndex((int) view.getTag());
        }
    }

}
