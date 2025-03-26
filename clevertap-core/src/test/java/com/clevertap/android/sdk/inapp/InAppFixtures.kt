package com.clevertap.android.sdk.inapp

object InAppFixtures {

    val TYPE_HALF_INTERSTITIAL = """
            {
              "type": "half-interstitial",
              "bg": "#ffffff",
              "tablet": false,
              "close": true,
              "message": {
                "text": "message from half inter",
                "color": "#434761",
                "replacements": "message from half inter",
                "og": ""
              },
              "title": {
                "text": "Hi from half inter",
                "color": "#434761",
                "replacements": "Hi from half inter",
                "og": ""
              },
              "buttons": [
                {
                  "text": "ButtonOne",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "close",
                    "android": "",
                    "ios": "",
                    "kv": {
                      
                    }
                  }
                },
                {
                  "text": "ButtonTwo",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "kv",
                    "android": "",
                    "ios": "",
                    "kv": {
                      "a": "aaa"
                    }
                  }
                }
              ],
              "media": {
                "url": "https://d2hduo7492lb76.cloudfront.net/dist/1631012864/i/06e2bfb81b9749cb8cc6b2a6712c68f0.jpeg?v=1742963502",
                "poster": "",
                "key": "06e2bfb81b9749cb8cc6b2a6712c68f0",
                "content_type": "image/jpeg",
                "filename": "",
                "processing": false
              },
              "isMediaSourceRecommended": false,
              "recommendedText": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "mediaLandscape": {
                
              },
              "isMediaSourceRecommendedLandscape": false,
              "recommendedTextLandscape": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "hasPortrait": true,
              "hasLandscape": false,
              "rounded-borders": false,
              "is_native": true,
              "ti": 1742963524,
              "wzrk_id": "1742963524_20250326",
              "wzrk_pivot": "wzrk_default",
              "priority": 5,
              "whenTriggers": [
                {
                  "eventName": "lalit"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ],
              "wzrk_ttl": 1743136393
            }
        """.trimIndent()

    val TYPE_ADVANCED_BUILDER = """
        {
          "type": "custom-html",
          "deviceSettings": {
            "orientation": "portrait",
            "size": {
              "height": 926,
              "width": 428
            }
          },
          "w": {
            "dk": false,
            "sc": false,
            "pos": "c",
            "xp": 100,
            "yp": 100
          },
          "isJsEnabled": true,
          "is_native": true,
          "ti": 1742969683,
          "wzrk_id": "1742969683_20250326",
          "wzrk_pivot": "wzrk_default",
          "priority": 1,
          "whenTriggers": [
            {
              "eventName": "BlockBRTesting"
            }
          ],
          "frequencyLimits": [
            
          ],
          "excludeGlobalFCaps": -1,
          "occurrenceLimits": [
            
          ],
          "wzrk_ttl": 1743142761,
          "d": {
            "html": "some-html-block"
          }
        }
    """.trimIndent()
}