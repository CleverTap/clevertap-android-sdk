package com.clevertap.android.sdk.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Real fragment (safe to attach through a FragmentManager, unlike a MockK spy)
 * that records [refreshList] and [onPageSettled] invocations instead of
 * executing them.
 */
class RecordingInboxListFragment : CTInboxListViewFragment() {

    var refreshListCalls = 0

    var onPageSettledCalls = 0

    override fun refreshList() {
        refreshListCalls++
    }

    override fun onPageSettled() {
        onPageSettledCalls++
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Headless: the real onCreateView needs styleConfig/config intent extras.
        return null
    }
}
