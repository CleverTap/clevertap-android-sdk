package com.clevertap.android.sdk.inbox

import androidx.fragment.app.Fragment
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.assertEquals
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
