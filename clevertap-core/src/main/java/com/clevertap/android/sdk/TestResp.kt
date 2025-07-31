package com.clevertap.android.sdk

@JvmField
val json = """
        {
          "arp": {
            "j_n": "Zw==",
            "i_n": "ZGJk",
            "d_ts": 1750660056,
            "dh": -955998143,
            "v": 2,
            "j_s": "{}",
            "id": "8KK-85K-996Z",
            "r_ts": 1750664227,
            "wdt": 2.67,
            "hgt": 5.5,
            "av": "7.3.1"
          },
          "inapp_stale": [
            
          ],
          "imc": 50,
          "imp": 150,
          "inapp_delivery_mode": "SS",
          "inapp_notifs_ss": [
            {
              "ti": 1701427934,
              "wzrk_id": "1701427934_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "display trigger op",
                  "eventProperties": [
                    {
                      "propertyName": "hopeless",
                      "operator": 3,
                      "propertyValue": [
                        "yes"
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1701440314,
              "wzrk_id": "1701440314_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 8,
              "whenTriggers": [
                {
                  "eventName": "display trigger op",
                  "eventProperties": [
                    {
                      "propertyName": "equals",
                      "operator": 15,
                      "propertyValue": [
                        100
                      ]
                    },
                    {
                      "propertyName": "equals",
                      "operator": 2,
                      "propertyValue": [
                        200
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                {
                  "type": "onEvery",
                  "limit": 3
                }
              ]
            },
            {
              "ti": 1701362488,
              "wzrk_id": "1701362488_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 8,
              "whenTriggers": [
                {
                  "eventName": "display trigger op",
                  "eventProperties": [
                    {
                      "propertyName": "string",
                      "operator": 27,
                      "propertyValue": null
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1701362852,
              "wzrk_id": "1701362852_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 3,
              "whenTriggers": [
                {
                  "eventName": "display trigger op",
                  "eventProperties": [
                    {
                      "propertyName": "contains",
                      "operator": 28,
                      "propertyValue": [
                        "any"
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                {
                  "type": "onEvery",
                  "limit": 2
                }
              ]
            },
            {
              "ti": 1701358334,
              "wzrk_id": "1701358334_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 3,
              "whenTriggers": [
                {
                  "eventName": "display trigger op",
                  "eventProperties": [
                    {
                      "propertyName": "equals",
                      "operator": 1,
                      "propertyValue": [
                        444
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 3
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1701358658,
              "wzrk_id": "1701358658_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 1,
              "whenTriggers": [
                {
                  "eventName": "display trigger op",
                  "eventProperties": [
                    {
                      "propertyName": "equals",
                      "operator": 15,
                      "propertyValue": [
                        444
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "minutes",
                  "limit": 2,
                  "frequency": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                {
                  "type": "onExactly",
                  "limit": 2
                }
              ]
            },
            {
              "ti": 1733462104,
              "wzrk_id": "1733462104_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "Buy",
                  "firstTimeOnly": true
                },
                {
                  "eventName": "Return",
                  "firstTimeOnly": true
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 10
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719295036,
              "wzrk_id": "1719295036_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 5,
              "whenTriggers": [
                {
                  "eventName": "stringAttr1_change",
                  "profileAttrName": "stringAttr1"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "minutes",
                  "limit": 1,
                  "frequency": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719293975,
              "wzrk_id": "1719293975_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 3,
              "whenTriggers": [
                {
                  "eventName": "stringAttr1_change",
                  "profileAttrName": "stringAttr1"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1739197450,
              "wzrk_id": "1739197450_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "Added To Cart"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "ever",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1739195257,
              "wzrk_id": "1739195257_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "Added To Cart"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "ever",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1701442337,
              "wzrk_id": "1701442337_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 6,
              "whenTriggers": [
                {
                  "eventName": "Notification Viewed",
                  "eventProperties": [
                    {
                      "propertyName": "Campaign id",
                      "operator": 3,
                      "propertyValue": [
                        "1701363236"
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1733462661,
              "wzrk_id": "1733462661_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "blood_sugar_first_time",
                  "firstTimeOnly": true,
                  "profileAttrName": "Blood sugar"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 10
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1703164787,
              "wzrk_id": "1703164787_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 6,
              "whenTriggers": [
                {
                  "eventName": "Event with bool",
                  "eventProperties": [
                    {
                      "propertyName": "param1",
                      "operator": 1,
                      "propertyValue": [
                        "true"
                      ]
                    },
                    {
                      "propertyName": "param2",
                      "operator": 1,
                      "propertyValue": [
                        "false"
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1734712768,
              "wzrk_id": "1734712768_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 1,
              "whenTriggers": [
                {
                  "eventName": "test4"
                },
                {
                  "eventName": "favSong_evt",
                  "firstTimeOnly": true,
                  "profileAttrName": "favsong"
                },
                {
                  "eventName": "favColor_newEvt",
                  "profileAttrName": "favcolor"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 3
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                {
                  "type": "onExactly",
                  "limit": 1
                }
              ]
            },
            {
              "ti": 1738767785,
              "wzrk_id": "1738767785_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "check"
                },
                {
                  "eventName": "test1"
                },
                {
                  "eventName": "Charged"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719298275,
              "wzrk_id": "1719298275_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "numberAttr4_change",
                  "profileAttrName": "numberAttr4"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 3
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719296686,
              "wzrk_id": "1719296686_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 6,
              "whenTriggers": [
                {
                  "eventName": "numberAttr2_change",
                  "profileAttrName": "numberAttr2"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "minutes",
                  "limit": 1,
                  "frequency": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719295883,
              "wzrk_id": "1719295883_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 8,
              "whenTriggers": [
                {
                  "eventName": "booleanAttr1_change",
                  "profileAttrName": "booleanAttr1"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719294407,
              "wzrk_id": "1719294407_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 7,
              "whenTriggers": [
                {
                  "eventName": "stringAttr2_change",
                  "profileAttrName": "stringAttr2"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "hours",
                  "limit": 3,
                  "frequency": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1703164556,
              "wzrk_id": "1703164556_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 5,
              "whenTriggers": [
                {
                  "eventName": "Send Event with bool 3",
                  "eventProperties": [
                    {
                      "propertyName": "param2",
                      "operator": 1,
                      "propertyValue": [
                        "false"
                      ]
                    },
                    {
                      "propertyName": "param1",
                      "operator": 1,
                      "propertyValue": [
                        "true"
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1746343695,
              "wzrk_id": "1746343695_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 1,
              "whenTriggers": [
                {
                  "eventName": "LevelUp"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                },
                {
                  "type": "minutes",
                  "limit": 1,
                  "frequency": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                {
                  "type": "onExactly",
                  "limit": 2
                }
              ]
            },
            {
              "ti": 1737467291,
              "wzrk_id": "1737467291_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 7,
              "whenTriggers": [
                {
                  "eventName": "unityiifunc"
                },
                {
                  "eventName": "iin"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1733464407,
              "wzrk_id": "1733464407_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "heart_rate_first_time",
                  "profileAttrName": "Heart rate"
                },
                {
                  "eventName": "calcium_first_time",
                  "firstTimeOnly": true,
                  "profileAttrName": "Calcium"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 10
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1701442536,
              "wzrk_id": "1701442536_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "Notification Clicked",
                  "eventProperties": [
                    {
                      "propertyName": "Campaign id",
                      "operator": 3,
                      "propertyValue": [
                        "1701363236"
                      ]
                    }
                  ]
                }
              ],
              "frequencyLimits": [
                {
                  "type": "seconds",
                  "limit": 2,
                  "frequency": 30
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1733461975,
              "wzrk_id": "1733461975_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "Search",
                  "firstTimeOnly": true
                },
                {
                  "eventName": "Rate"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 10
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1747730977,
              "wzrk_id": "1747730977_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 7,
              "whenTriggers": [
                {
                  "eventName": "sameinapp"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "ever",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1727187722,
              "wzrk_id": "1727187722_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "abc"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 3
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1739176042,
              "wzrk_id": "1739176042_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "Half Interstitial"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "ever",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719297794,
              "wzrk_id": "1719297794_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 7,
              "whenTriggers": [
                {
                  "eventName": "numberAttr3_change",
                  "profileAttrName": "numberAttr3"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719298062,
              "wzrk_id": "1719298062_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 6,
              "whenTriggers": [
                {
                  "eventName": "numberAttr3_change",
                  "profileAttrName": "numberAttr3"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719296127,
              "wzrk_id": "1719296127_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 4,
              "whenTriggers": [
                {
                  "eventName": "numberAttr1_change",
                  "profileAttrName": "numberAttr1"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719296034,
              "wzrk_id": "1719296034_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 4,
              "whenTriggers": [
                {
                  "eventName": "numberAttr1_change",
                  "profileAttrName": "numberAttr1"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                {
                  "type": "onEvery",
                  "limit": 2
                }
              ]
            },
            {
              "ti": 1733906267,
              "wzrk_id": "1733906267_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "stringAttr3_change",
                  "profileAttrName": "stringattr3"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1736758413,
              "wzrk_id": "1736758413_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "mt ios2"
                },
                {
                  "eventName": "mt ios1"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 3
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1737466880,
              "wzrk_id": "1737466880_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 7,
              "whenTriggers": [
                {
                  "eventName": "unity cc"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ],
              "templateName": "Example template"
            },
            {
              "ti": 1719295451,
              "wzrk_id": "1719295451_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 8,
              "whenTriggers": [
                {
                  "eventName": "booleanAttr0_change",
                  "profileAttrName": "booleanAttr0"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719298563,
              "wzrk_id": "1719298563_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 6,
              "whenTriggers": [
                {
                  "eventName": "booleanAttr0_change",
                  "profileAttrName": "booleanAttr0"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 2
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            },
            {
              "ti": 1719295131,
              "wzrk_id": "1719295131_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 6,
              "whenTriggers": [
                {
                  "eventName": "booleanAttr0_change",
                  "profileAttrName": "booleanAttr0"
                }
              ],
              "frequencyLimits": [
                {
                  "type": "session",
                  "limit": 1
                }
              ],
              "excludeGlobalFCaps": -1,
              "occurrenceLimits": [
                
              ]
            }
          ],
          "inapp_notifs": [
            {
              "type": "half-interstitial",
              "bg": "#ffffff",
              "tablet": false,
              "close": true,
              "message": {
                "text": "RegressionTest DefName DefEmail \n15\nPriority 10",
                "color": "#FFFFFF",
                "og": ""
              },
              "title": {
                "text": "Hello DefName",
                "color": "#434761",
                "og": ""
              },
              "buttons": [
                {
                  "text": "OpenURL",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "url",
                    "android": "https:\/\/clevertap.com\/",
                    "ios": "https:\/\/clevertap.com\/",
                    "kv": {
                      
                    }
                  }
                }
              ],
              "media": {
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/659a93266f3941fb99f138e49817af4a.jpeg?v=1745847273",
                "poster": "",
                "key": "659a93266f3941fb99f138e49817af4a",
                "content_type": "image\/jpeg",
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
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/7b43d99dc6a5442fb06ae4ce738714c4.jpeg?v=1745847288",
                "poster": "",
                "key": "7b43d99dc6a5442fb06ae4ce738714c4",
                "content_type": "image\/jpeg",
                "filename": "",
                "processing": false
              },
              "isMediaSourceRecommendedLandscape": false,
              "recommendedTextLandscape": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "hasPortrait": true,
              "hasLandscape": true,
              "rounded-borders": false,
              "rfp": false,
              "is_native": true,
              "ti": 1750659630,
              "wzrk_id": "1750659630_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "App Launched"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ],
              "wzrk_ttl": 1750837027
            },
            {
              "type": "half-interstitial",
              "bg": "#ffffff",
              "tablet": false,
              "close": true,
              "message": {
                "text": "Inapp preview",
                "color": "#FFFFFF",
                "replacements": "Inapp preview",
                "og": ""
              },
              "title": {
                "text": "Prio 10 BOSS",
                "color": "#434761",
                "replacements": "Prio 10 BOSS",
                "og": ""
              },
              "buttons": [
                {
                  "text": "OpenURL",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "url",
                    "android": "https:\/\/clevertap.com\/",
                    "ios": "https:\/\/clevertap.com\/",
                    "kv": {
                      
                    }
                  }
                }
              ],
              "media": {
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/8b435f699635475bac49969fa7df8cae.jpeg?v=1750659839",
                "poster": "",
                "key": "8b435f699635475bac49969fa7df8cae",
                "content_type": "image\/jpeg",
                "filename": "",
                "processing": true
              },
              "isMediaSourceRecommended": false,
              "recommendedText": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "mediaLandscape": {
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/7b43d99dc6a5442fb06ae4ce738714c4.jpeg?v=1745847288",
                "poster": "",
                "key": "7b43d99dc6a5442fb06ae4ce738714c4",
                "content_type": "image\/jpeg",
                "filename": "",
                "processing": false
              },
              "isMediaSourceRecommendedLandscape": false,
              "recommendedTextLandscape": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "hasPortrait": true,
              "hasLandscape": true,
              "rounded-borders": false,
              "rfp": false,
              "is_native": true,
              "ti": 1750659854,
              "wzrk_id": "1750659854_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 10,
              "whenTriggers": [
                {
                  "eventName": "App Launched"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ],
              "wzrk_ttl": 1750837027
            },
            {
              "type": "half-interstitial",
              "bg": "#ffffff",
              "tablet": false,
              "close": true,
              "message": {
                "text": "RegressionTest DefName DefEmail \n15\nPriority 5",
                "color": "#FFFFFF",
                "og": ""
              },
              "title": {
                "text": "Hello DefName",
                "color": "#434761",
                "og": ""
              },
              "buttons": [
                {
                  "text": "OpenURL",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "url",
                    "android": "https:\/\/clevertap.com\/",
                    "ios": "https:\/\/clevertap.com\/",
                    "kv": {
                      
                    }
                  }
                }
              ],
              "media": {
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/bb6b326272394fbbac181716a9c1d93e.jpeg?v=1750659668",
                "poster": "",
                "key": "bb6b326272394fbbac181716a9c1d93e",
                "content_type": "image\/jpeg",
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
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/7b43d99dc6a5442fb06ae4ce738714c4.jpeg?v=1 745847288",
                "poster": "",
                "key": "7b43d99dc6a5442fb06ae4ce738714c4",
                "content_type": "image\/jpeg",
                "filename": "",
                "processing": false
              },
              "isMediaSourceRecommendedLandscape": false,
              "recommendedTextLandscape": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "hasPortrait": true,
              "hasLandscape": true,
              "rounded-borders": false,
              "rfp": false,
              "is_native": true,
              "ti": 1750659700,
              "wzrk_id": "1750659700_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 5,
              "whenTriggers": [
                {
                  "eventName": "App Launched"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ],
              "wzrk_ttl": 1750837027
            },
            {
              "type": "half-interstitial",
              "bg": "#ffffff",
              "tablet": false,
              "close": true,
              "message": {
                "text": "RegressionTest DefName DefEmail \n15\nPriority 1",
                "color": "#000000",
                "og": ""
              },
              "title": {
                "text": "Hello DefName",
                "color": "#434761",
                "og": ""
              },
              "buttons": [
                {
                  "text": "OpenURL",
                  "color": "#000000",
                  "bg": "#1EB858",
                  "border": "#1EB858",
                  "radius": "4",
                  "actions": {
                    "close": true,
                    "type": "url",
                    "android": "https:\/\/clevertap.com\/",
                    "ios": "https:\/\/clevertap.com\/",
                    "kv": {
                      
                    }
                  }
                }
              ],
              "media": {
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/f4f313943a124bd4946fa867f4cc403f.jpeg?v=1750659752",
                "poster": "",
                "key": "f4f313943a124bd4946fa867f4cc403f",
                "content_type": "image\/jpeg",
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
                "url": "https:\/\/d2hduo7492lb76.cloudfront.net\/dist\/1699358338\/i\/7b43d99dc6a5442fb06ae4ce738714c4.jpeg?v=1745847288",
                "poster": "",
                "key": "7b43d99dc6a5442fb06ae4ce738714c4",
                "content_type": "image\/jpeg",
                "filename": "",
                "processing": false
              },
              "isMediaSourceRecommendedLandscape": false,
              "recommendedTextLandscape": {
                "text": "",
                "replacements": "",
                "og": ""
              },
              "hasPortrait": true,
              "hasLandscape": true,
              "rounded-borders": false,
              "rfp": false,
              "is_native": true,
              "ti": 1750659783,
              "wzrk_id": "1750659783_20250623",
              "wzrk_pivot": "wzrk_default",
              "priority": 1,
              "whenTriggers": [
                {
                  "eventName": "App Launched"
                }
              ],
              "frequencyLimits": [
                
              ],
              "excludeGlobalFCaps": 1,
              "occurrenceLimits": [
                
              ],
              "wzrk_ttl": 1750837027
            }
          ],
          "adUnit_notifs": [
            {
              "type": "simple",
              "bg": "#ffffff",
              "content": [
                {
                  "key": 8342242371,
                  "message": {
                    "text": "Message ND",
                    "color": "#434761",
                    "replacements": "Message ND",
                    "og": ""
                  },
                  "title": {
                    "text": "Test ND Title",
                    "color": "#434761",
                    "replacements": "Test ND Title",
                    "og": ""
                  },
                  "action": {
                    "url": {
                      "android": {
                        "text": "",
                        "replacements": "",
                        "og": ""
                      },
                      "ios": {
                        "text": "",
                        "replacements": "",
                        "og": ""
                      }
                    },
                    "hasUrl": false
                  },
                  "media": {
                    
                  },
                  "icon": {
                    
                  },
                  "isMediaSourceRecommended": false,
                  "isIconSourceRecommended": false,
                  "recommendedText": {
                    "text": "",
                    "replacements": "",
                    "og": ""
                  },
                  "recommendedIconText": {
                    "text": "",
                    "replacements": "",
                    "og": ""
                  }
                }
              ],
              "customKVData": [
                
              ],
              "wzrk_id": "1747125327_20250623",
              "wzrk_pivot": "wzrk_default",
              "ti": 1747125327
            }
          ],
          "pushamp_notifs": {
            "list": [
              
            ],
            "ack": true,
            "pf": 240
          },
          "ff_notifs": {
            "kv": [
              
            ],
            "ts": 1750664227
          },
          "vars": {
            "android.samsung.s1": "this is ANDROID 7.2.01",
            "darkTheme": true,
            "var_float": 5.09999,
            "group2.group3.var2_in_group_2_3": 2000,
            "factory_var_map.str": "factory str adnroid",
            "var_double": 6.0999,
            "group.var1_in_group": 10,
            "group.factory_var_in_group": 1300,
            "instance_double": 88.2,
            "stringVariable255555": "test string_updated",
            "var_boolean": false,
            "map_complex.nested_string": "ANDROID nested string",
            "weapon.damage": 10,
            "instance_int": 9105,
            "var_byte": 1,
            "weapon.color": "pink allllllllll",
            "factory_var_map.int": 1200,
            "var3": 3,
            "factory_var_int": 87,
            "factory_var_file": "https:\/\/dsl810bactgru.cloudfront.net\/1699358338\/assets\/a1e6009a83764887ac6287c8b535f229.png",
            "boolean": 10,
            "var_dict_complex.nested_string": "hello, nested1",
            "var_dict.nested_double": 11.5,
            "var_dict.nested_outside": "hello, outsideupdated",
            "group1.var1": 3,
            "group1.group2.var3": 11111,
            "var_int": 30,
            "var_bool": false,
            "var_number": 32,
            "var.hello": "hello, group B",
            "android.samsung.s2": "All",
            "map.nested_string": "nested str",
            "var_dict.nested_string": "hello, nestedUpdated",
            "folder1.fileVariable": "https:\/\/d11gh1sd24upek.cloudfront.net\/1699358338\/assets\/5812e4a6f6a64cd68c845fe427dc9773.pdf",
            "folder1.folder2.fileVariable": "https:\/\/dsl810bactgru.cloudfront.net\/1699358338\/assets\/f99a5abeba8a4a279dc44e10cd296a8a.jpeg",
            "group.factory_var_file_in_group": "https:\/\/d11gh1sd24upek.cloudfront.net\/1699358338\/assets\/5812e4a6f6a64cd68c845fe427dc9773.pdf",
            "PELive.var1": "All"
          }
        }
    """.trimIndent()