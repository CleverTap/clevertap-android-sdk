package com.clevertap.android.sdk.product_config

import android.content.res.Resources
import android.content.res.XmlResourceParser
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

internal class DefaultXmlParserTest : BaseTestCase() {

    private lateinit var defaultXmlParser: DefaultXmlParser
    private lateinit var resources: Resources
    private lateinit var xmlResourceParser: XmlResourceParser


    override fun setUp() {
        super.setUp()
        defaultXmlParser = DefaultXmlParser()
        resources = mockk(relaxed = true)
        xmlResourceParser = mockk(relaxed = true)
    }

    @Test
    fun test_getDefaultsFromXml_whenContextResourcesAreNull_ParserMethodShouldNotGetCalled() {
        val defaultXmlParserSpy = spyk(defaultXmlParser)
        val context = spyk(application)
        every { context.resources } returns null
        val resourceID = 1212121
        defaultXmlParserSpy.getDefaultsFromXml(context, resourceID)
        verify(exactly = 0) { defaultXmlParserSpy.getDefaultsFromXmlParser(any(), any()) }
    }
}