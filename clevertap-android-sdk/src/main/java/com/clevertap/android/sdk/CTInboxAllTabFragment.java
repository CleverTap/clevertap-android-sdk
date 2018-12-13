package com.clevertap.android.sdk;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CTInboxAllTabFragment extends CTInboxTabBaseFragment {
    private ExoPlayerRecyclerView recyclerView;
    private CTInboxMessageAdapter inboxMessageAdapter;
    private boolean firstTime = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View allView = inflater.inflate(R.layout.inbox_all_tab,container,false);
        recyclerView = allView.findViewById(R.id.all_tab_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setVideoInfoList(inboxMessageArrayList);
        inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, getActivity(),recyclerView);
        recyclerView.setAdapter(inboxMessageAdapter);
        inboxMessageAdapter.notifyDataSetChanged();
        if (firstTime) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    recyclerView.playVideo();
                }
            },1000);
            firstTime = false;
        }
        return allView;
    }

    @Override
    public void onPause() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                recyclerView.onPausePlayer();
            }
        });
        super.onPause();
    }

    @Override
    public void onResume() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                recyclerView
                        .onRestartPlayer();
            }
        });
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if(recyclerView!=null)
            recyclerView.onRelease();
        super.onDestroy();
    }
}
