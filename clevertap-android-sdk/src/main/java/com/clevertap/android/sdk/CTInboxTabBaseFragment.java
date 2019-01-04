package com.clevertap.android.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

abstract class CTInboxTabBaseFragment extends Fragment {

    interface InboxListener{
        void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
        void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
    }

    ArrayList<CTInboxMessage> inboxMessageArrayList;
    CleverTapInstanceConfig config;
    ExoPlayerRecyclerView exoPlayerRecyclerView;
    boolean videoPresent = false;
    CTInboxStyleConfig styleConfig;
    private WeakReference<CTInboxTabBaseFragment.InboxListener> listenerWeakReference;
    CleverTapAPI cleverTapAPI;

    void setListener(InboxListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    InboxListener getListener() {
        InboxListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            Logger.v("InboxListener is null for messages");
        }
        return listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null) {
            inboxMessageArrayList = bundle.getParcelableArrayList("inboxMessages");
            Logger.d("Inbox Message List - "+inboxMessageArrayList.toString());
            config = bundle.getParcelable("config");
            styleConfig = bundle.getParcelable("styleConfig");
            cleverTapAPI = CleverTapAPI.instanceWithConfig(getActivity(),config);
            if (((Activity)context) != null && ((Activity)context) instanceof CTInboxActivity) {
                setListener((CTInboxTabBaseFragment.InboxListener) ((Activity)context));
            }
        }
    }

    boolean checkInboxMessagesContainVideo(ArrayList<CTInboxMessage> inboxMessageArrayList){
        boolean videoPresent = false;
        for(CTInboxMessage inboxMessage : inboxMessageArrayList){
            if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()){
                videoPresent = true;
                break;
            }
        }
        return videoPresent;
    }

    @Override
    public void onPause() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(videoPresent)
                    exoPlayerRecyclerView.onPausePlayer();
            }
        });
        super.onPause();
    }

    @Override
    public void onResume() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(videoPresent)
                    exoPlayerRecyclerView.onRestartPlayer();
            }
        });
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if(exoPlayerRecyclerView!=null && videoPresent)
            exoPlayerRecyclerView.onRelease();
        super.onDestroy();
    }

    void didClick(Bundle data, int position) {
        InboxListener listener = getListener();
        if (listener != null) {
            listener.messageDidClick(getActivity().getBaseContext(),inboxMessageArrayList.get(position), data);
        }
    }

    void didShow(Bundle data, int position) {
        InboxListener listener = getListener();
        if (listener != null) {
            listener.messageDidShow(getActivity().getBaseContext(),inboxMessageArrayList.get(position), data);
        }
    }

    void handleClick(int position, String buttonText){
        try {
            Bundle data = new Bundle();

            data.putString(Constants.NOTIFICATION_ID_TAG,inboxMessageArrayList.get(position).getCampaignId());
            if(buttonText != null && !buttonText.isEmpty())
                data.putString("wzrk_c2a", buttonText);
            didClick(data,position);

            String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getActionUrl();
            if (actionUrl != null) {
                fireUrlThroughIntent(actionUrl, data);
                return;
            }
        } catch (Throwable t) {
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
        }
    }

    void handleViewPagerClick(int position, int viewPagerPosition){
        try {
        Bundle data = new Bundle();

        data.putString(Constants.NOTIFICATION_ID_TAG,inboxMessageArrayList.get(position).getCampaignId());
        didClick(data,position);
        String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(viewPagerPosition).getActionUrl();
            fireUrlThroughIntent(actionUrl, data);
            return;
        }catch (Throwable t){
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
        }
    }

    void fireUrlThroughIntent(String url, Bundle formData) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
    }

    void markReadForMessageId(CTInboxMessage inboxMessage){
        Logger.v("Marking " + inboxMessage.getCampaignId() + " as read");
        cleverTapAPI.markReadInboxMessage(inboxMessage);
    }
}
