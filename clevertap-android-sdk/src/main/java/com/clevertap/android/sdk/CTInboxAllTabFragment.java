package com.clevertap.android.sdk;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CTInboxAllTabFragment extends CTInboxTabBaseFragment {
    private RecyclerView recyclerView;
    private CTInboxMessageAdapter inboxMessageAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View allView = inflater.inflate(R.layout.inbox_all_tab,container,false);
        recyclerView = allView.findViewById(R.id.all_tab_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, getActivity());
        recyclerView.setAdapter(inboxMessageAdapter);
        return allView;
    }
}
