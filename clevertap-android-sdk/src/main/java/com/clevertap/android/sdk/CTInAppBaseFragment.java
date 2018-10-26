package com.clevertap.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CTInAppBaseFragment extends Fragment {

    interface InAppListener {
        void inAppNotificationDidShow(Context context, CTInAppNotification inAppNotification, Bundle formData);
        void inAppNotificationDidClick(Context context, CTInAppNotification inAppNotification, Bundle formData);
        void inAppNotificationDidDismiss(Context context, CTInAppNotification inAppNotification, Bundle formData);
    }

    CTInAppNotification inAppNotification;
    CleverTapInstanceConfig config;
    private WeakReference<CTInAppBaseFragment.InAppListener> listenerWeakReference;
    CloseImageView closeImageView = null;

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
            config.getLogger().verbose(config.getAccountId(),"InAppListener is null for notification: " + inAppNotification.getJsonDescription());
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
        inAppNotification = bundle.getParcelable("inApp");
        config = bundle.getParcelable("config");
        generateListener();
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O)
            parent.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        didShow(null);
    }

    void didClick(Bundle data) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidClick(getActivity().getBaseContext(),inAppNotification, data);
        }
    }

    void didShow(Bundle data) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidShow(getActivity().getBaseContext(),inAppNotification, data);
        }
    }

    void didDismiss(Bundle data) {
        cleanup();
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidDismiss(getActivity().getBaseContext(),inAppNotification, data);
        }
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

    int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
    }

    void handleButtonClickAtIndex(int index) {
        try {
            CTInAppNotificationButton button = inAppNotification.getButtons().get(index);
            Bundle data = new Bundle();

            data.putString(Constants.NOTIFICATION_ID_TAG,inAppNotification.getCampaignId());
            data.putString("wzrk_c2a", button.getText());
            didClick(data);

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

    class CTInAppNativeButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            handleButtonClickAtIndex((int)view.getTag());
        }
    }

}
