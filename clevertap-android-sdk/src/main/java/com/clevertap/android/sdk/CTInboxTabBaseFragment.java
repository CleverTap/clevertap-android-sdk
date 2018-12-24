package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

abstract class CTInboxTabBaseFragment extends Fragment {

    ArrayList<CTInboxMessage> inboxMessageArrayList;
    CleverTapInstanceConfig config;
    ExoPlayerRecyclerView exoPlayerRecyclerView;
    boolean videoPresent = false;
    CTInboxStyleConfig styleConfig;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null) {
            inboxMessageArrayList = bundle.getParcelableArrayList("inboxMessages");
            Logger.d("Inbox Message List - "+inboxMessageArrayList.toString());
            config = bundle.getParcelable("config");
            styleConfig = bundle.getParcelable("styleConfig");
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
}
