package com.clevertap.android.sdk.product_config

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Log
import com.clevertap.android.sdk.Constants.LOG_TAG_PRODUCT_CONFIG
import org.xmlpull.v1.XmlPullParser
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class DefaultXmlParser {

    open fun getDefaultsFromXml(context: Context, resourceId: Int): Map<String, String> =
        HashMap<String, String>().also { getDefaultsFromXml(context.resources, resourceId, it) }

    open fun getDefaultsFromXml(resources: Resources?, resourceId: Int, hashMap: MutableMap<String, String>) {
        if (resources == null) {
            Log.e(
                "ProductConfig",
                "Could not find the resources of the current context while trying to set defaults from an XML."
            )
            return
        }
        try {
            getDefaultsFromXmlParser(resources.getXml(resourceId), hashMap)
        } catch (e: Exception) {
            Log.e("ProductConfig", "Encountered an error while parsing the defaults XML file.", e)
        }
    }

    open fun getDefaultsFromXmlParser(xmlParser: XmlResourceParser, defaultsMap: MutableMap<String, String>) {
        var curTag: String? = null
        var key: String? = null
        var value: String? = null

        var eventType = xmlParser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                curTag = xmlParser.name
            } else if (eventType != XmlPullParser.END_TAG) {
                if (eventType == XmlPullParser.TEXT && curTag != null) {
                    when (curTag) {
                        XML_TAG_KEY -> key = xmlParser.text
                        XML_TAG_VALUE -> value = xmlParser.text
                        else -> Log.w(
                            LOG_TAG_PRODUCT_CONFIG,
                            "Encountered an unexpected tag while parsing the defaults XML."
                        )
                    }
                }
            } else {
                if (xmlParser.name == XML_TAG_ENTRY) {
                    if (key != null && value != null) {
                        defaultsMap[key] = value
                    } else {
                        Log.w(
                            LOG_TAG_PRODUCT_CONFIG,
                            "An entry in the defaults XML has an invalid key and/or value tag."
                        )
                    }

                    key = null
                    value = null
                }

                curTag = null
            }
            eventType = xmlParser.next()
        }
    }

    private companion object {
        private const val XML_TAG_ENTRY = "entry"

        private const val XML_TAG_KEY = "key"

        private const val XML_TAG_VALUE = "value"
    }
}
