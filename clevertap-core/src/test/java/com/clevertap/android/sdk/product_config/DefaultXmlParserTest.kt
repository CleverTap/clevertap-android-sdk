package com.clevertap.android.sdk.product_config

import android.content.res.Resources
import android.content.res.XmlResourceParser
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.mockito.Mockito.*
import java.util.HashMap

internal class DefaultXmlParserTest : BaseTestCase() {

    private lateinit var defaultXmlParser: DefaultXmlParser
    private lateinit var resources: Resources
    private lateinit var xmlResourceParser: XmlResourceParser


    override fun setUp() {
        super.setUp()
        defaultXmlParser = DefaultXmlParser()
        resources = mock(Resources::class.java)
        xmlResourceParser = mock(XmlResourceParser::class.java)
    }

    @Test
    fun test_getDefaultsFromXml() {
        val defaultXmlParser = spy(defaultXmlParser)
        val context = spy(application)
        `when`(context.resources).thenReturn(resources)
        val resourceID = 1212121
        defaultXmlParser.getDefaultsFromXml(context, resourceID)
        val map = HashMap<String, String>()
        verify(defaultXmlParser).getDefaultsFromXml(resources, resourceID, map)
    }

    @Test
    fun test_getDefaultsFromXml_whenContextResourcesAreNull_ParserMethodShouldNotGetCalled() {
        val defaultXmlParser = spy(defaultXmlParser)
        val context = spy(application)
        `when`(context.resources).thenReturn(null)
        val resourceID = 1212121
        defaultXmlParser.getDefaultsFromXml(context, resourceID)
        val map = HashMap<String, String>()
        verify(defaultXmlParser,never()).getDefaultsFromXml(resources, resourceID, map)
    }
}