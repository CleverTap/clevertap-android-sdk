package com.clevertap.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class CTInboxListViewFragment extends Fragment {

    interface InboxListener{
        void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
        void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
    }

    ArrayList<CTInboxMessage> inboxMessages =  new ArrayList<>();
    CleverTapInstanceConfig config;
    boolean haveVideoPlayerSupport = CleverTapAPI.haveVideoPlayerSupport;
    CTInboxStyleConfig styleConfig;
    private WeakReference<CTInboxListViewFragment.InboxListener> listenerWeakReference;
    LinearLayout linearLayout;
    private boolean firstTime = true;
    private int tabPosition;

    MediaPlayerRecyclerView mediaRecyclerView;
    RecyclerView recyclerView;

    private boolean shouldAutoPlayOnFirstLaunch() {
        return tabPosition <= 0;
    }

    void setListener(CTInboxListViewFragment.InboxListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    CTInboxListViewFragment.InboxListener getListener() {
        CTInboxListViewFragment.InboxListener listener = null;
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

    private ArrayList<CTInboxMessage> filterMessages(ArrayList<CTInboxMessage>messages, String filter){
        ArrayList<CTInboxMessage> filteredMessages = new ArrayList<>();
        for(CTInboxMessage inboxMessage : messages){
            if(inboxMessage.getTags() != null && inboxMessage.getTags().size() > 0) {
                for (String stringTag : inboxMessage.getTags()) {
                    if (stringTag.equalsIgnoreCase(filter)) {
                        filteredMessages.add(inboxMessage);
                    }
                }
            }
        }
        return filteredMessages;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null) {
            //noinspection ConstantConditions
            config = bundle.getParcelable("config");
            styleConfig = bundle.getParcelable("styleConfig");
            tabPosition = bundle.getInt("position", -1);
            final String filter = bundle.getString("filter", null);
            if (context instanceof CTInboxActivity) {
                setListener((CTInboxListViewFragment.InboxListener) getActivity());
            }
            CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(getActivity(), config);
            if (cleverTapAPI != null) {
                ArrayList<CTInboxMessage> allMessages = cleverTapAPI.getAllInboxMessages();
                inboxMessages = filter != null ? filterMessages(allMessages, filter) : allMessages;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View allView = inflater.inflate(R.layout.inbox_list_view,container,false);
        linearLayout = allView.findViewById(R.id.list_view_linear_layout);
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig.getInboxBackgroundColor()));
        TextView noMessageView = allView.findViewById(R.id.list_view_no_message_view);

        if (inboxMessages.size() <= 0) {
            noMessageView.setVisibility(View.VISIBLE);
            return allView;
        }

        noMessageView.setVisibility(View.GONE);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        final CTInboxMessageAdapter inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessages, this);

        if (haveVideoPlayerSupport) {
            mediaRecyclerView = new MediaPlayerRecyclerView(getActivity());
            Logger.d("ListView added - "+ mediaRecyclerView.toString());
            setMediaRecyclerView(mediaRecyclerView);
            mediaRecyclerView.setVisibility(View.VISIBLE);
            mediaRecyclerView.setLayoutManager(linearLayoutManager);
            mediaRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            mediaRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mediaRecyclerView.setAdapter(inboxMessageAdapter);
            inboxMessageAdapter.notifyDataSetChanged();

            linearLayout.addView(mediaRecyclerView);

            if (firstTime && shouldAutoPlayOnFirstLaunch()) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mediaRecyclerView.playVideo();
                    }
                }, 1000);
                firstTime = false;
            }

        } else {
            recyclerView = allView.findViewById(R.id.list_view_recycler_view);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(inboxMessageAdapter);
            inboxMessageAdapter.notifyDataSetChanged();
        }
        return allView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mediaRecyclerView != null){
            mediaRecyclerView.onPausePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mediaRecyclerView != null){
            mediaRecyclerView.onRestartPlayer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaRecyclerView != null){
            mediaRecyclerView.release();
        }
    }

    void didClick(Bundle data, int position) {
        CTInboxListViewFragment.InboxListener listener = getListener();
        if (listener != null) {
            //noinspection ConstantConditions
            listener.messageDidClick(getActivity().getBaseContext(), inboxMessages.get(position), data);
        }
    }

    @SuppressWarnings("SameParameterValue")
    void didShow(Bundle data, int position) {
        CTInboxListViewFragment.InboxListener listener = getListener();
        if (listener != null) {
            //noinspection ConstantConditions
            listener.messageDidShow(getActivity().getBaseContext(), inboxMessages.get(position), data);
        }
    }

    void handleClick(int position, String buttonText, JSONObject jsonObject){
        try {
            Bundle data = new Bundle();
            JSONObject wzrkParams = inboxMessages.get(position).getWzrkParams();
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
                if(inboxMessages.get(position).getInboxMessageContents().get(0).getLinktype(jsonObject).equalsIgnoreCase(Constants.COPY_TYPE)){
                    //noinspection UnnecessaryReturnStatement
                    return;
                }else{
                    String actionUrl = inboxMessages.get(position).getInboxMessageContents().get(0).getLinkUrl(jsonObject);
                    if (actionUrl != null) {
                        fireUrlThroughIntent(actionUrl);
                    }
                }
            }else {
                String actionUrl = inboxMessages.get(position).getInboxMessageContents().get(0).getActionUrl();
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
            JSONObject wzrkParams = inboxMessages.get(position).getWzrkParams();
            Iterator<String> iterator = wzrkParams.keys();
            while(iterator.hasNext()){
                String keyName = iterator.next();
                if(keyName.startsWith(Constants.WZRK_PREFIX))
                    data.putString(keyName,wzrkParams.getString(keyName));
            }
            didClick(data,position);
            String actionUrl = inboxMessages.get(position).getInboxMessageContents().get(viewPagerPosition).getActionUrl();
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

    MediaPlayerRecyclerView getMediaRecyclerView() {
        return this.mediaRecyclerView;
    }

    void setMediaRecyclerView(MediaPlayerRecyclerView mediaRecyclerView) {
        this.mediaRecyclerView = mediaRecyclerView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mediaRecyclerView != null) {
            if (mediaRecyclerView.getLayoutManager() != null) {
                outState.putParcelable("recyclerLayoutState", mediaRecyclerView.getLayoutManager().onSaveInstanceState());
            }
        }

        if(recyclerView != null) {
            if (recyclerView.getLayoutManager() != null) {
                outState.putParcelable("recyclerLayoutState", recyclerView.getLayoutManager().onSaveInstanceState());
            }
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable("recyclerLayoutState");
            if (mediaRecyclerView != null) {
                if(mediaRecyclerView.getLayoutManager()!=null) {
                    mediaRecyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
                }
            }
            
            if (recyclerView != null) {
                if(recyclerView.getLayoutManager()!=null) {
                    recyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
                }
            }
        }
    }
}
