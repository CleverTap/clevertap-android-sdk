package com.clevertap.android.sdk.inbox

import android.content.Intent
import io.mockk.unmockkObject
import io.mockk.mockkObject
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
        mockkObject(CleverTapAPI.Companion)
        try {
            val activity = launchNoTabsInboxActivity(messageCount = 0)

            val fragment = activity.supportFragmentManager.fragments.firstOrNull { it is CTInboxListViewFragment }
            assertTrue(fragment != null)
            assertEquals(View.GONE, activity.findViewById<TextView>(R.id.no_message_view).visibility)
        } finally {
            unmockkObject(CleverTapAPI.Companion)
        }
    }

    @Test
    fun `activity recreation does not duplicate the list fragment`() {
        mockkObject(CleverTapAPI.Companion)
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
            unmockkObject(CleverTapAPI.Companion)
        }
    }

    private fun inboxFragmentCount(activity: CTInboxActivity): Int =
        activity.supportFragmentManager.fragments.count { it is CTInboxListViewFragment }

    @Test
    fun `no-tabs non-empty inbox still creates the list fragment`() {
        mockkObject(CleverTapAPI.Companion)
        try {
            val activity = launchNoTabsInboxActivity(messageCount = 3)

            val fragment = activity.supportFragmentManager.fragments.firstOrNull { it is CTInboxListViewFragment }
            assertTrue(fragment != null)
        } finally {
            unmockkObject(CleverTapAPI.Companion)
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
    fun `page settled listener notifies the fragment that came to rest`() {
        val tabOne = RecordingInboxListFragment()
        val tabTwo = RecordingInboxListFragment()

        pageSettledListener(tabOne, tabTwo, currentItem = 1)
            .onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)

        assertEquals(0, tabOne.onPageSettledCalls)
        assertEquals(1, tabTwo.onPageSettledCalls)
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
