package com.clevertap.android.sdk.inbox

import androidx.fragment.app.FragmentActivity
import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
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

        verify(exactly = 0) { listener.messageDidClick(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun test_didClick_with_empty_list_does_not_throw() {
        val listener = mockk<CTInboxListViewFragment.InboxListener>(relaxed = true)
        ctInboxListViewFragmentSpy.setListener(listener)

        ctInboxListViewFragmentSpy.didClick(null, 0, 0, null, Constants.APP_INBOX_ITEM_INDEX)

        verify(exactly = 0) { listener.messageDidClick(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun test_didShow_forwards_the_exact_message_to_the_listener() {
        val listener = mockk<CTInboxListViewFragment.InboxListener>(relaxed = true)
        val activity = mockk<FragmentActivity>(relaxed = true)
        every { ctInboxListViewFragmentSpy.activity } returns activity
        ctInboxListViewFragmentSpy.setListener(listener)
        val message = CTInboxMessage(jsonObj)

        ctInboxListViewFragmentSpy.didShow(null, message)

        verify(exactly = 1) { listener.messageDidShow(any(), message, null) }
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
