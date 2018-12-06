package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

abstract class CTInboxTabBaseFragment extends Fragment {

    ArrayList<CTInboxMessage> inboxMessageArrayList;
    CleverTapInstanceConfig config;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null) {
            inboxMessageArrayList = bundle.getParcelableArrayList("inboxMessages");
            Logger.d("Inbox Message List - "+inboxMessageArrayList.toString());
            config = bundle.getParcelable("config");
        }
    }
}
