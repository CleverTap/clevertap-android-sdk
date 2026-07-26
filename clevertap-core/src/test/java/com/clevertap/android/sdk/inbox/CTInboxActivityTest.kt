package com.clevertap.android.sdk.inbox

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.R
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTInboxActivityTest : BaseTestCase() {

    private fun activityWithFragments(vararg fragments: Fragment): CTInboxActivity {
        // Without inbox intent extras onCreate returns early, leaving a headless
        // activity whose FragmentManager is fully usable.
        val activity = Robolectric.buildActivity(CTInboxActivity::class.java).setup().get()
        val transaction = activity.supportFragmentManager.beginTransaction()
        fragments.forEachIndexed { index, fragment -> transaction.add(fragment, "tab$index") }
        transaction.commitNow()
        return activity
    }

    @Test
    fun `refreshAllInboxListFragments refreshes every attached inbox list fragment`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()

        val activity = activityWithFragments(tabOne, tabTwo)
        activity.refreshAllInboxListFragments()

        assertEquals(1, tabOne.refreshListCalls)
        assertEquals(1, tabTwo.refreshListCalls)
    }

    @Test
    fun `refreshAllInboxListFragments skips non-inbox fragments without crashing`() {
        val inboxFragment = RecordingInboxListFragment()
        val unrelatedFragment = Fragment()

        val activity = activityWithFragments(unrelatedFragment, inboxFragment)
        activity.refreshAllInboxListFragments()

        assertEquals(1, inboxFragment.refreshListCalls)
    }

    /** Launches CTInboxActivity with real inbox extras (no-tabs style config). */
    private fun launchNoTabsInboxActivity(messageCount: Int): CTInboxActivity {
        every { CleverTapAPI.instanceWithConfig(any(), any()) } returns cleverTapAPI
        every { cleverTapAPI.inboxMessageCount } returns messageCount
        every { cleverTapAPI.allInboxMessages } returns arrayListOf()

        val intent = Intent(appCtx, CTInboxActivity::class.java).apply {
            putExtra("styleConfig", CTInboxStyleConfig())
            putExtra("configBundle", Bundle().apply { putParcelable("config", cleverTapInstanceConfig) })
        }
        val controller = Robolectric.buildActivity(CTInboxActivity::class.java, intent)
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        val activity = controller.setup().get()
        activity.supportFragmentManager.executePendingTransactions()
        return activity
    }

    @Test
    fun `no-tabs empty inbox creates the list fragment so pull-to-refresh exists`() {
        mockkStatic(CleverTapAPI::class)
        try {
            val activity = launchNoTabsInboxActivity(messageCount = 0)

            val fragment = activity.supportFragmentManager.fragments.firstOrNull { it is CTInboxListViewFragment }
            assertTrue(fragment != null)
            assertEquals(View.GONE, activity.findViewById<TextView>(R.id.no_message_view).visibility)
        } finally {
            unmockkStatic(CleverTapAPI::class)
        }
    }

    @Test
    fun `no-tabs non-empty inbox still creates the list fragment`() {
        mockkStatic(CleverTapAPI::class)
        try {
            val activity = launchNoTabsInboxActivity(messageCount = 3)

            val fragment = activity.supportFragmentManager.fragments.firstOrNull { it is CTInboxListViewFragment }
            assertTrue(fragment != null)
        } finally {
            unmockkStatic(CleverTapAPI::class)
        }
    }

    @Test
    fun `refreshAllInboxListFragments skips detached inbox fragments`() {
        val attached = RecordingInboxListFragment()
        val detached = RecordingInboxListFragment()

        val activity = activityWithFragments(attached)
        activity.supportFragmentManager.beginTransaction().add(detached, "detached").commitNow()
        activity.supportFragmentManager.beginTransaction().detach(detached).commitNow()
        activity.refreshAllInboxListFragments()

        assertEquals(1, attached.refreshListCalls)
        assertEquals(0, detached.refreshListCalls)
    }
}
