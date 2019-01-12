package com.clevertap.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

abstract class CTInboxTabBaseFragment extends Fragment {

    interface InboxListener{
        void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
        void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
    }

    ArrayList<CTInboxMessage> inboxMessageArrayList =  new ArrayList<>();
    CleverTapInstanceConfig config;
    boolean videoPresent = CleverTapAPI.haveVideoPlayerSupport;
    CTInboxStyleConfig styleConfig;
    private WeakReference<CTInboxTabBaseFragment.InboxListener> listenerWeakReference;

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
            //noinspection ConstantConditions
            config = bundle.getParcelable("config");
            styleConfig = bundle.getParcelable("styleConfig");
            if (context instanceof CTInboxActivity) {
                setListener((CTInboxTabBaseFragment.InboxListener) getActivity());
            }
            int position = bundle.getInt("position");
            CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(getActivity(),config);
            if (cleverTapAPI != null) {
                inboxMessageArrayList = cleverTapAPI.getAllInboxMessages();
                if(position == 1 && !styleConfig.getFirstTab().isEmpty()){
                    inboxMessageArrayList = cleverTapAPI.filterMessages(inboxMessageArrayList,styleConfig.getFirstTab());
                }else if( position == 2 && !styleConfig.getSecondTab().isEmpty()){
                    inboxMessageArrayList = cleverTapAPI.filterMessages(inboxMessageArrayList,styleConfig.getSecondTab());
                }
            }
        }
    }

    void didClick(Bundle data, int position) {
        InboxListener listener = getListener();
        if (listener != null) {
            //noinspection ConstantConditions
            listener.messageDidClick(getActivity().getBaseContext(),inboxMessageArrayList.get(position), data);
        }
    }

    @SuppressWarnings("SameParameterValue")
    void didShow(Bundle data, int position) {
        InboxListener listener = getListener();
        if (listener != null) {
            //noinspection ConstantConditions
            listener.messageDidShow(getActivity().getBaseContext(),inboxMessageArrayList.get(position), data);
        }
    }

    void handleClick(int position, String buttonText, JSONObject jsonObject){
        try {
            Bundle data = new Bundle();

            JSONObject wzrkParams = inboxMessageArrayList.get(position).getWzrkParams();
            Iterator<String> iterator = wzrkParams.keys();
            while(iterator.hasNext()){
                String keyName = iterator.next();
                if(keyName.startsWith(Constants.WZRK_PREFIX))
                    data.putString(keyName,wzrkParams.getString(keyName));
            }

            if (buttonText != null && !buttonText.isEmpty()) {
                data.putString("wzrk_c2a", buttonText);
            }
            didClick(data,position);

            if (jsonObject != null) {
                if(inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getLinktype(jsonObject).equalsIgnoreCase(Constants.COPY_TYPE)){
                    //noinspection UnnecessaryReturnStatement
                    return;
                }else{
                    String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getLinkUrl(jsonObject);
                    if (actionUrl != null) {
                        fireUrlThroughIntent(actionUrl);
                    }
                }
            }else {
                String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getActionUrl();
                if (actionUrl != null) {
                    fireUrlThroughIntent(actionUrl);
                }
            }
        } catch (Throwable t) {
            Logger.d("Error handling notification button click: " + t.getCause());
        }
    }

    void handleViewPagerClick(int position, int viewPagerPosition){
        try {
        Bundle data = new Bundle();
        JSONObject wzrkParams = inboxMessageArrayList.get(position).getWzrkParams();
        Iterator<String> iterator = wzrkParams.keys();
        while(iterator.hasNext()){
            String keyName = iterator.next();
            if(keyName.startsWith(Constants.WZRK_PREFIX))
                data.putString(keyName,wzrkParams.getString(keyName));
        }
        didClick(data,position);
        String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(viewPagerPosition).getActionUrl();
            fireUrlThroughIntent(actionUrl);
        }catch (Throwable t){
            Logger.d("Error handling notification button click: " + t.getCause());
        }
    }

    void fireUrlThroughIntent(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
    }

    ArrayList<CTInboxMessage> filterMessages(ArrayList<CTInboxMessage> inboxMessageArrayList,String tab){
        ArrayList<CTInboxMessage> filteredMessages = new ArrayList<>();
        for(CTInboxMessage inboxMessage : inboxMessageArrayList){
            if(inboxMessage.getTags() != null && inboxMessage.getTags().size() > 0) {
                for (String stringTag : inboxMessage.getTags()) {
                    if (stringTag.equalsIgnoreCase(tab)) {
                        filteredMessages.add(inboxMessage);
                    }
                }
            }
        }
        return filteredMessages;
    }
}
