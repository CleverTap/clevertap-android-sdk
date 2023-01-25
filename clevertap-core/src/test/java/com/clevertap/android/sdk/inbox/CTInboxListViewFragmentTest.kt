package com.clevertap.android.sdk.inbox

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTInboxListViewFragmentTest : BaseTestCase() {

    private lateinit var buttonJsonObj: JSONObject
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
        buttonJsonObj =
            JSONObject(
                """{"type":"url","text":"ClickHere","color":"#000000","bg":"#ffffff","copyText":{"text":"",
                |"replacements":"","og":""},"url":{"android":{"text":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity",
                |"replacements":"ctdemo:\/\/com.clevertap.demo\/WebViewActivity","og":""},
                |"ios":{"text":"","replacements":"","og":""}},"kv":{}}""".trimMargin()
            )
        ctInboxListViewFragment = CTInboxListViewFragment()
        ctInboxListViewFragmentSpy = Mockito.spy(ctInboxListViewFragment)
    }

    @Test
    fun test_handleClick_when_buttonText_is_empty_fires_url_through_intent() {

        val position = 0
        val buttonTxt = ""

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_buttonText_is_null_fires_url_through_intent() {

        val position = 0
        val buttonTxt = null

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_buttonText_is_present_fires_url_through_intent() {

        val position = 0
        val buttonTxt = "ClickHere"

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_buttonJsonObj_is_null_fires_url_through_intent() {

        val position = 0
        val buttonTxt = "ClickHere"

        val buttonJsonObj = null

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_buttonJsonObj_is_present_fires_url_through_intent() {

        val position = 0
        val buttonTxt = "ClickHere"

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_keyValuePayload_is_empty_fires_url_through_intent() {

        val position = 0
        val buttonTxt = "ClickHere"

        val keyValuePayload = HashMap<String, String>()
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_keyValuePayload_is_null_fires_url_through_intent() {

        val position = 0
        val buttonTxt = "ClickHere"

        val keyValuePayload = null
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.atLeastOnce()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }

    @Test
    fun test_handleClick_when_keyValuePayload_is_present_do_nothing() {

        val position = 0
        val buttonTxt = "ClickHere"

        val keyValuePayload = HashMap<String, String>()
        keyValuePayload["id"] = "123456"
        ctInboxListViewFragmentSpy.inboxMessages.add(CTInboxMessage(jsonObj))

        //Act
        ctInboxListViewFragmentSpy.handleClick(position, buttonTxt, buttonJsonObj, keyValuePayload)

        //Assert
        verify(
            ctInboxListViewFragmentSpy,
            Mockito.never()
        ).fireUrlThroughIntent("ctdemo://com.clevertap.demo/WebViewActivity")
    }
}