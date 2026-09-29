package com.clevertap.android.sdk.inbox

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.FetchInboxCallback
import com.clevertap.android.sdk.R
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.robolectric.Robolectric
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTInboxListViewFragmentTest : BaseTestCase() {

    private lateinit var buttonJsonObjUrlType: JSONObject
    private lateinit var buttonJsonObjKVType: JSONObject
    private lateinit var jsonObj: JSONObject
    private lateinit var ctInboxListViewFragment: CTInboxListViewFragment
    private lateinit var ctInboxListViewFragmentSpy: CTInboxListViewFragment

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        jsonObj = JSONObject(
            """{"id":"1674122920_1674457409","msg":{"bg":"#ECEDF2","orientation":"l","content":[{"key":5385896764,
                |"message":{"replacements":"SampleMessage","text":"SampleMessage","color":"#434761"},
                |"title":{"replacements":"SampleTitle","text":"SampleTitle","color":"#434761"},"action":{"hasUrl":true,
                |"hasLinks":true,"url":{"android":{"text":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity",
                |"replacements":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity","og":""},
                |"ios":{"text":"","replacements":"","og":""}},"links":[{"type":"url","text":"ClickHere",
                |"color":"#000000","bg":"#ffffff","copyText":{"text":"","replacements":"","og":""},
                |"url":{"android":{"text":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity",
                |"replacements":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity","og":""},
                |"ios":{"text":"","replacements":"","og":""}},"kv":{}}]},"media":{},"icon":{}}],
                |"type":"simple","tags":[],"enableTags":false},"isRead":true,"date":1674457409,"wzrk_ttl":1675062209,
                |"tags":[""],"wzrk_id":"1674122920_20230123","wzrkParams":{"wzrk_ttl":1675062209,
                |"wzrk_id":"1674122920_20230123","wzrk_pivot":"wzrk_default"}}""".trimMargin()
        )
        buttonJsonObjUrlType =
            JSONObject(
                """{"type":"url","text":"ClickHere","color":"#000000","bg":"#ffffff","copyText":{"text":"",
                |"replacements":"","og":""},"url":{"android":{"text":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity",
                |"replacements":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity","og":""},
                |"ios":{"text":"","replacements":"","og":""}},"kv":{}}""".trimMargin()
            )
        buttonJsonObjKVType =
            JSONObject(
                "{\"type\":\"kv\",\"text\":\"Link\",\"color\":\"#007bff\",\"bg\":\"#ffffff\",\"copyText\":{\"text\":\"\",\"replacements\":\"\",\"og\":\"\"},\"url\":{\"android\":{\"text\":\"\",\"replacements\":\"\",\"og\":\"\"},\"ios\":{\"text\":\"\",\"replacements\":\"\",\"og\":\"\"}},\"kv\":{\"Test\":\"TestValue\"}}".trimMargin()
            )
        ctInboxListViewFragment = CTInboxListViewFragment()
        ctInboxListViewFragmentSpy = spyk(ctInboxListViewFragment)
    }

    @Test
    fun test_handleClick_when_buttonText_is_empty_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = ""
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjUrlType, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_handleClick_when_buttonText_is_null_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = null
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjUrlType, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_handleClick_when_buttonText_is_present_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = "ClickHere"
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjUrlType, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_handleClick_when_buttonJsonObj_is_null_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = "ClickHere"
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val buttonJsonObj = null

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObj, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_handleClick_when_buttonJsonObj_is_present_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = "ClickHere"
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjUrlType, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_handleClick_when_keyValuePayload_is_empty_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = "ClickHere"
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjUrlType, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_handleClick_when_keyValuePayload_is_null_fires_url_through_intent() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = "ClickHere"
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = null
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjUrlType, keyValuePayload, buttonIndex)

        //Assert
        verify(atLeast = 1) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }

    @Test
    fun test_didClick_with_out_of_range_position_is_a_silent_noop() {
        val listener = mockk<CTInboxListViewFragment.InboxListener>(relaxed = true)
        ctInboxListViewFragmentSpy.setListener(listener)
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        // Positions that a stale click could deliver after a list refresh
        ctInboxListViewFragmentSpy.didClick(null, 5, 0, null, Constants.APP_INBOX_ITEM_INDEX)
        ctInboxListViewFragmentSpy.didClick(null, -1, 0, null, Constants.APP_INBOX_ITEM_INDEX)

        verify(exactly = 0) { listener.messageDidClick(any(), any(), any(), any(), any()) }
    }

    @Test
    fun test_didClick_with_empty_list_does_not_throw() {
        val listener = mockk<CTInboxListViewFragment.InboxListener>(relaxed = true)
        ctInboxListViewFragmentSpy.setListener(listener)

        ctInboxListViewFragmentSpy.didClick(null, 0, 0, null, Constants.APP_INBOX_ITEM_INDEX)

        verify(exactly = 0) { listener.messageDidClick(any(), any(), any(), any(), any()) }
    }

    @Test
    fun test_didShow_forwards_the_exact_message_to_the_listener() {
        val listener = mockk<CTInboxListViewFragment.InboxListener>(relaxed = true)
        ctInboxListViewFragmentSpy.setListener(listener)
        val message = CTInboxMessage(jsonObj)

        ctInboxListViewFragmentSpy.didShow(null, message)

        verify(exactly = 1) { listener.messageDidShow(message, null) }
    }

    // ---------------------------------------------------------------------
    // refreshList() and helpers
    // ---------------------------------------------------------------------

    private fun message(id: String, text: String = "SampleMessage", read: Boolean = false): CTInboxMessage {
        val json = JSONObject(jsonObj.toString())
        json.put("id", id).put("isRead", read)
        json.getJSONObject("msg").getJSONArray("content").getJSONObject(0)
            .getJSONObject("message").put("text", text)
        return CTInboxMessage(json)
    }

    /** Fragment spy whose view hierarchy exists (non-video path) and whose data
     *  query is stubbed, mirroring an attached fragment without a FragmentManager. */
    private fun viewReadyFragment(initial: List<CTInboxMessage> = emptyList()): CTInboxListViewFragment {
        val fragment = spyk(CTInboxListViewFragment())
        val fragmentActivity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        every { fragment.activity } returns fragmentActivity
        fragment.styleConfig = CTInboxStyleConfig()
        fragment.haveVideoPlayerSupport = false
        fragment.linearLayout = LinearLayout(fragmentActivity).apply {
            addView(RecyclerView(fragmentActivity).apply {
                id = R.id.list_view_recycler_view
                visibility = View.GONE
            })
        }
        fragment.noMessageView = TextView(fragmentActivity)
        every { fragment.view } returns fragment.linearLayout
        fragment.inboxMessages.addAll(initial)
        return fragment
    }

    @Test
    fun test_mergeReadStateForward_upgrades_unread_to_read_but_never_downgrades() {
        val oldRead = message("A", read = true)
        val freshUnreadA = message("A", read = false)
        val freshReadB = message("B", read = true)

        val upgraded = CTInboxListViewFragment.mergeReadStateForward(listOf(oldRead), listOf(freshUnreadA, freshReadB))

        assertEquals(1, upgraded)
        assertTrue(freshUnreadA.isRead)
        assertTrue(freshReadB.isRead)
    }

    @Test
    fun test_isContentIdentical_matrix() {
        val a = message("A")
        assertTrue(CTInboxListViewFragment.isContentIdentical(listOf(a), listOf(message("A"))))
        assertFalse(CTInboxListViewFragment.isContentIdentical(listOf(a), listOf(message("B"))))
        assertFalse(CTInboxListViewFragment.isContentIdentical(listOf(a), listOf(message("A", read = true))))
        assertFalse(CTInboxListViewFragment.isContentIdentical(listOf(a), listOf(message("A", text = "Edited"))))
        assertFalse(CTInboxListViewFragment.isContentIdentical(listOf(a), listOf(a, message("B"))))
    }

    @Test
    fun test_refreshList_builds_list_lazily_and_never_reassigns_the_shared_list() {
        val fragment = viewReadyFragment()
        val originalListInstance = fragment.inboxMessages
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A"), message("B"))

        fragment.refreshList()

        assertSame(originalListInstance, fragment.inboxMessages)
        assertEquals(2, fragment.inboxMessages.size)
        val recycler = fragment.linearLayout.findViewById<RecyclerView>(R.id.list_view_recycler_view)
        assertEquals(View.VISIBLE, recycler.visibility)
        assertEquals(2, recycler.adapter?.itemCount)
        assertEquals(View.GONE, fragment.noMessageView.visibility)
    }

    @Test
    fun test_refreshList_with_identical_content_touches_nothing() {
        val fragment = viewReadyFragment()
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A"))
        fragment.refreshList()
        val boundMessage = fragment.inboxMessages[0]

        // Identical copy (fresh object, same id/read/content) => no-op, original object kept
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A"))
        fragment.refreshList()

        assertSame(boundMessage, fragment.inboxMessages[0])
    }

    @Test
    fun test_refreshList_treats_read_state_only_difference_as_noop() {
        val fragment = viewReadyFragment()
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A"))
        fragment.refreshList()
        val boundMessage = fragment.inboxMessages[0]
        boundMessage.setRead(true) // what the view holder does after 2s on screen

        // Store still says unread (async write hasn't landed) — must not repaint or downgrade
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A", read = false))
        fragment.refreshList()

        assertSame(boundMessage, fragment.inboxMessages[0])
        assertTrue(fragment.inboxMessages[0].isRead)
    }

    @Test
    fun test_refreshList_from_non_empty_to_empty_shows_no_message_view() {
        val fragment = viewReadyFragment()
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A"))
        fragment.refreshList()

        every { fragment.fetchFreshMessages() } returns arrayListOf()
        fragment.refreshList()

        assertTrue(fragment.inboxMessages.isEmpty())
        val recycler = fragment.linearLayout.findViewById<RecyclerView>(R.id.list_view_recycler_view)
        assertEquals(View.GONE, recycler.visibility)
        assertEquals(0, recycler.adapter?.itemCount)
        assertEquals(View.VISIBLE, fragment.noMessageView.visibility)
    }

    @Test
    fun test_refreshList_without_view_refreshes_data_only() {
        val fragment = spyk(CTInboxListViewFragment())
        every { fragment.activity } returns mockk<FragmentActivity>(relaxed = true)
        val originalListInstance = fragment.inboxMessages
        every { fragment.fetchFreshMessages() } returns arrayListOf(message("A"))

        fragment.refreshList() // getView() == null -> data-only branch

        assertSame(originalListInstance, fragment.inboxMessages)
        assertEquals(1, fragment.inboxMessages.size)
    }

    @Test
    fun test_swipe_refresh_success_triggers_refreshList_and_stops_spinner() {
        mockkStatic(CleverTapAPI::class)
        try {
            val api = mockk<CleverTapAPI>(relaxed = true)
            every { CleverTapAPI.instanceWithConfig(any(), any()) } returns api
            every { api.isInboxFetchDisabledForSession } returns false
            val callbackSlot = slot<FetchInboxCallback>()
            justRun { api.fetchInbox(capture(callbackSlot)) }

            val fragment = viewReadyFragment()
            every { fragment.requireContext() } returns appCtx
            every { fragment.isAdded } returns true
            justRun { fragment.refreshList() }

            val swipeRefreshLayout = SwipeRefreshLayout(appCtx)
            fragment.wireSwipeToRefresh(swipeRefreshLayout)
            swipeRefreshLayout.isRefreshing = true
            onRefreshListenerOf(swipeRefreshLayout).onRefresh()

            callbackSlot.captured.onInboxFetched(true)

            assertFalse(swipeRefreshLayout.isRefreshing)
            verify(exactly = 1) { fragment.refreshList() }
        } finally {
            unmockkStatic(CleverTapAPI::class)
        }
    }

    @Test
    fun test_swipe_refresh_failure_stops_spinner_without_refreshing() {
        mockkStatic(CleverTapAPI::class)
        try {
            val api = mockk<CleverTapAPI>(relaxed = true)
            every { CleverTapAPI.instanceWithConfig(any(), any()) } returns api
            every { api.isInboxFetchDisabledForSession } returns false
            val callbackSlot = slot<FetchInboxCallback>()
            justRun { api.fetchInbox(capture(callbackSlot)) }

            val fragment = viewReadyFragment()
            every { fragment.requireContext() } returns appCtx
            every { fragment.isAdded } returns true
            justRun { fragment.refreshList() }

            val swipeRefreshLayout = SwipeRefreshLayout(appCtx)
            fragment.wireSwipeToRefresh(swipeRefreshLayout)
            swipeRefreshLayout.isRefreshing = true
            onRefreshListenerOf(swipeRefreshLayout).onRefresh()

            callbackSlot.captured.onInboxFetched(false)

            assertFalse(swipeRefreshLayout.isRefreshing)
            assertTrue(swipeRefreshLayout.isEnabled)
            verify(exactly = 0) { fragment.refreshList() }
        } finally {
            unmockkStatic(CleverTapAPI::class)
        }
    }

    private fun onRefreshListenerOf(layout: SwipeRefreshLayout): SwipeRefreshLayout.OnRefreshListener {
        val field = SwipeRefreshLayout::class.java.getDeclaredField("mListener")
        field.isAccessible = true
        return field.get(layout) as SwipeRefreshLayout.OnRefreshListener
    }

    @Test
    fun test_handleClick_when_keyValuePayload_is_present_do_nothing() {

        val itemPosition = 0
        val viewPagerPosition = 0
        val buttonTxt = "ClickHere"
        val buttonIndex = Constants.APP_INBOX_CTA1_INDEX

        val keyValuePayload = HashMap<String, String>()
        keyValuePayload["id"] = "123456"
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(itemPosition, viewPagerPosition, buttonTxt, buttonJsonObjKVType, keyValuePayload, buttonIndex)

        //Assert
        verify(exactly = 0) {
            ctInboxListViewFragmentSpy.fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
        }
    }
}
