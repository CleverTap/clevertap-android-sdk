package com.clevertap.android.sdk.product_config;

import static com.clevertap.android.sdk.Constants.LOG_TAG_PRODUCT_CONFIG;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DefaultXmlParser {

    private static final String XML_TAG_ENTRY = "entry";

    private static final String XML_TAG_KEY = "key";

    private static final String XML_TAG_VALUE = "value";

    private static final int XML_TAG_TYPE_KEY = 0;

    private static final int XML_TAG_TYPE_VALUE = 1;

    public DefaultXmlParser() {
    }

    static HashMap<String, String> getDefaultsFromXml(Context context, int resourceId) {
        HashMap<String, String> defaultsMap = new HashMap<>();

        try {
            Resources resources = context.getResources();
            if (resources == null) {
                Log.e("ProductConfig",
                        "Could not find the resources of the current context while trying to set defaults from an XML.");
                return defaultsMap;
            }

            XmlResourceParser xmlParser = resources.getXml(resourceId);
            String curTag = null;
            String key = null;
            String value = null;

            for (int eventType = xmlParser.getEventType(); eventType != XmlPullParser.END_DOCUMENT;
                    eventType = xmlParser.next()) {
                if (eventType == XmlPullParser.START_TAG) {
                    curTag = xmlParser.getName();
                } else if (eventType != XmlPullParser.END_TAG) {
                    if (eventType == XmlPullParser.TEXT && curTag != null) {
                        byte tagType = -1;
                        switch (curTag) {
                            case XML_TAG_KEY:
                                tagType = XML_TAG_TYPE_KEY;
                                break;
                            case XML_TAG_VALUE:
                                tagType = XML_TAG_TYPE_VALUE;
                        }

                        switch (tagType) {
                            case XML_TAG_TYPE_KEY:
                                key = xmlParser.getText();
                                break;
                            case XML_TAG_TYPE_VALUE:
                                value = xmlParser.getText();
                                break;
                            default:
                                Log.w(LOG_TAG_PRODUCT_CONFIG,
                                        "Encountered an unexpected tag while parsing the defaults XML.");
                        }
                    }
                } else {
                    if (xmlParser.getName().equals(XML_TAG_ENTRY)) {
                        if (key != null && value != null) {
                            defaultsMap.put(key, value);
                        } else {
                            Log.w(LOG_TAG_PRODUCT_CONFIG,
                                    "An entry in the defaults XML has an invalid key and/or value tag.");
                        }

                        key = null;
                        value = null;
                    }

                    curTag = null;
                }
            }
        } catch (IOException | XmlPullParserException var11) {
            Log.e("ProductConfig", "Encountered an error while parsing the defaults XML file.", var11);
        }

        return defaultsMap;
    }
}