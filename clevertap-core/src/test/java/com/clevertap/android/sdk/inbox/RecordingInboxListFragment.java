package com.clevertap.android.sdk.inbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Real fragment (safe to attach through a FragmentManager, unlike a MockK spy)
 * that records {@link #refreshList()} invocations instead of executing them.
 */
public class RecordingInboxListFragment extends CTInboxListViewFragment {

    public int refreshListCalls = 0;

    @Override
    void refreshList() {
        refreshListCalls++;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Headless: the real onCreateView needs styleConfig/config intent extras.
        return null;
    }
}
