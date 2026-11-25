package com.clevertap.android.sdk.inbox

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.InboxVideoStates
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
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
    fun `activity recreation does not duplicate the list fragment`() {
        mockkStatic(CleverTapAPI::class)
        try {
            every { CleverTapAPI.instanceWithConfig(any(), any()) } returns cleverTapAPI
            every { cleverTapAPI.inboxMessageCount } returns 1
            every { cleverTapAPI.allInboxMessages } returns arrayListOf()
            // Theme must survive recreation: applicationInfo.theme is what the recreated
            // instance resolves (per-instance setTheme would be lost on recreate()).
            application.applicationInfo.theme = androidx.appcompat.R.style.Theme_AppCompat

            val intent = Intent(appCtx, CTInboxActivity::class.java).apply {
                putExtra("styleConfig", CTInboxStyleConfig())
                putExtra("configBundle", Bundle().apply { putParcelable("config", cleverTapInstanceConfig) })
            }
            val controller = Robolectric.buildActivity(CTInboxActivity::class.java, intent).setup()
            controller.get().supportFragmentManager.executePendingTransactions()
            assertEquals(1, inboxFragmentCount(controller.get()))

            // Rotation / process-restore path: FragmentManager restores the tagged
            // fragment, then onCreate runs again and must NOT add a second one.
            controller.recreate()

            val recreated = controller.get()
            recreated.supportFragmentManager.executePendingTransactions()
            assertEquals(1, inboxFragmentCount(recreated))
        } finally {
            unmockkStatic(CleverTapAPI::class)
        }
    }

    private fun inboxFragmentCount(activity: CTInboxActivity): Int =
        activity.supportFragmentManager.fragments.count { it is CTInboxListViewFragment }

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

    /** Page-settled listener over a tab adapter, with the pager parked on [currentItem]. */
    private fun pageSettledListener(
        vararg fragments: Fragment,
        currentItem: Int
    ): ViewPager.OnPageChangeListener {
        val activity = Robolectric.buildActivity(CTInboxActivity::class.java).setup().get()
        val adapter = CTInboxTabAdapter(activity.supportFragmentManager, fragments.size)
        fragments.forEachIndexed { index, fragment -> adapter.addFragment(fragment, "tab$index", index) }
        val pager = mockk<ViewPager>()
        every { pager.currentItem } returns currentItem
        return CTInboxActivity.createPageSettledListener(pager, adapter)
    }

    @Test
    fun `page selected hides the other tabs at once`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()
        val tabThree = RecordingInboxListFragment()

        pageSettledListener(tabOne, tabTwo, tabThree, currentItem = 1).onPageSelected(1)

        assertEquals(1, tabOne.onPageHiddenCalls)
        assertEquals(0, tabTwo.onPageHiddenCalls)
        assertEquals(1, tabThree.onPageHiddenCalls)
    }

    @Test
    fun `page selected does not start the video of the new tab`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()

        pageSettledListener(tabOne, tabTwo, currentItem = 1).onPageSelected(1)

        assertEquals(0, tabTwo.onPageSettledCalls)
    }

    @Test
    fun `page settled listener notifies the fragment that came to rest`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()

        pageSettledListener(tabOne, tabTwo, currentItem = 1)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(0, tabOne.onPageSettledCalls)
        assertEquals(1, tabTwo.onPageSettledCalls)
    }

    @Test
    fun `page settled listener tells the other tabs they are hidden`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()
        val tabThree = RecordingInboxListFragment()

        pageSettledListener(tabOne, tabTwo, tabThree, currentItem = 1)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(1, tabOne.onPageHiddenCalls)
        assertEquals(0, tabTwo.onPageHiddenCalls)
        assertEquals(1, tabThree.onPageHiddenCalls)
    }

    @Test
    fun `page settled listener does not hide the tab that came to rest`() {
        val tab = RecordingInboxListFragment()

        pageSettledListener(tab, currentItem = 0)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(0, tab.onPageHiddenCalls)
        assertEquals(1, tab.onPageSettledCalls)
    }

    @Test
    fun `page settled listener does not hide any tab while the pager is moving`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()
        val listener = pageSettledListener(tabOne, tabTwo, currentItem = 1)

        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING)
        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_SETTLING)

        assertEquals(0, tabOne.onPageHiddenCalls)
        assertEquals(0, tabTwo.onPageHiddenCalls)
    }

    @Test
    fun `page listener tolerates a tab that has no fragment yet`() {
        val activity = Robolectric.buildActivity(CTInboxActivity::class.java).setup().get()
        val adapter = CTInboxTabAdapter(activity.supportFragmentManager, 2)
        val inboxTab = RecordingInboxListFragment()
        adapter.addFragment(inboxTab, "tab1", 1) // slot 0 stays empty
        val pager = mockk<ViewPager>()
        every { pager.currentItem } returns 1
        val listener = CTInboxActivity.createPageSettledListener(pager, adapter)

        listener.onPageSelected(1)
        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(1, inboxTab.onPageSettledCalls)
        assertEquals(0, inboxTab.onPageHiddenCalls)
    }

    @Test
    fun `page settled listener tolerates a non-inbox fragment in another tab`() {
        val inboxTab = RecordingInboxListFragment()

        pageSettledListener(Fragment(), inboxTab, currentItem = 1)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(1, inboxTab.onPageSettledCalls)
        assertEquals(0, inboxTab.onPageHiddenCalls)
    }

    @Test
    fun `page settled listener hides the other tabs before it starts the video of the current one`() {
        val order = mutableListOf<String>()
        val hidden = object : RecordingInboxListFragment() {
            override fun onPageHidden() { order += "hidden" }
        }
        val current = object : RecordingInboxListFragment() {
            override fun onPageSettled() { order += "settled" }
        }

        pageSettledListener(hidden, current, currentItem = 1)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(listOf("hidden", "settled"), order)
    }

    @Test
    fun `page settled listener ignores states other than idle`() {
        val tab = RecordingInboxListFragment()
        val listener = pageSettledListener(tab, currentItem = 0)

        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING)
        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_SETTLING)

        assertEquals(0, tab.onPageSettledCalls)
    }

    @Test
    fun `page settled listener tolerates a non-inbox fragment at the current position`() {
        val inboxTab = RecordingInboxListFragment()

        pageSettledListener(Fragment(), inboxTab, currentItem = 0)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(0, inboxTab.onPageSettledCalls)
    }

    private fun statesWith(messageId: String, positionMs: Long): Bundle = Bundle().apply {
        putBundle(
            messageId,
            Bundle().apply {
                putString("mediaUrl", "url-$messageId")
                putLong("positionMs", positionMs)
                putFloat("volume", 1f)
            }
        )
    }

    @Test
    fun `the screen keeps one set of video states for all its tabs`() {
        val activity = Robolectric.buildActivity(CTInboxActivity::class.java).setup().get()

        assertSame(activity.videoStates, activity.videoStates)
    }

    @Test
    fun `the video states are put in the saved state of the screen`() {
        val controller = Robolectric.buildActivity(CTInboxActivity::class.java).setup()
        controller.get().videoStates.restore(statesWith("m1", 6_000L))

        val out = Bundle()
        controller.saveInstanceState(out)

        assertEquals(6_000L, out.getBundle("videoStates")!!.getBundle("m1")!!.getLong("positionMs"))
    }

    @Test
    fun `a screen where no video played saves no video states`() {
        val controller = Robolectric.buildActivity(CTInboxActivity::class.java).setup()

        val out = Bundle()
        controller.saveInstanceState(out)

        assertNull(out.getBundle("videoStates"))
    }

    @Test
    fun `the video states come back in the new screen after a rotation`() {
        val first = Robolectric.buildActivity(CTInboxActivity::class.java).setup()
        first.get().videoStates.restore(statesWith("m1", 6_000L))
        val out = Bundle()
        first.saveInstanceState(out)

        val second = Robolectric.buildActivity(CTInboxActivity::class.java).create(out).get()

        assertEquals(6_000L, second.videoStates.stateOf("m1", "url-m1")!!.positionMs)
    }

    @Test
    fun `a new screen without saved state starts with no video states`() {
        val activity = Robolectric.buildActivity(CTInboxActivity::class.java).create(null).get()

        assertNull(activity.videoStates.stateOf("m1", "url-m1"))
        assertEquals(true, activity.videoStates.save().isEmpty)
    }

    @Test
    fun `two screens do not share video states`() {
        val first = Robolectric.buildActivity(CTInboxActivity::class.java).setup().get()
        val second = Robolectric.buildActivity(CTInboxActivity::class.java).setup().get()
        first.videoStates.restore(statesWith("m1", 6_000L))

        assertNull(second.videoStates.stateOf("m1", "url-m1"))
    }
}
