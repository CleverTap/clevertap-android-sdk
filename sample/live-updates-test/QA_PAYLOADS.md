# `pt_progress` Live Update: copy-paste QA payloads

Every payload below is a **full push** (wrapper + `data`), the same shape the backend sends. Paste it
into the sample app and it goes through the same path as a real FCM push.

## How to send a payload

1. Open the sample app, scroll to **LIVE UPDATES** and tap **Paste Live Update payload (QA)**.
2. Copy one JSON block below, tap **Paste**, then **Send**.
3. Open the notification shade (close it again to see the Android 16 status-bar chip).

From a computer instead (the whole JSON in single quotes):

```
adb shell am start -n com.clevertap.demo/.LiveUpdatePayloadActivity --es payload '<json>' --ez send true
```

Things the paste screen does for you (QA helpers only, a real backend sends real values):

| In the payload | What the screen does |
|---|---|
| `"wzrk_acct_id": "<your account id>"` | Fills in the sample app's own account id. Without an account id the SDK drops the push |
| `{{now}}`, `{{now+600}}`, `{{now-300}}` | Replaced with the current time in milliseconds, plus or minus that many seconds. Used by `pt_when` |
| `wzrk_cid` | Creates the channel if it does not exist yet |

Rules:

- Each case has its own `wzrk_activityId`, so each case is its own card. Payloads with the **same**
  `wzrk_activityId` update the **same** card; send those in the order shown.
- Do not add a `wzrk_pid` you already sent: the SDK drops a repeated `wzrk_pid` as a duplicate.
- Every payload was sent through the paste screen on an Android 16 emulator (native) and an
  Android 15 emulator (fallback), and checked to show (or, where expected, not show) a notification.
  The expected look comes from the SDK code and Android 16's own source; check it on the device.

## Plain determinate bar

### P1. Plain bar, nothing else

| Android 16 | Android 15 and below |
|---|---|
| One solid bar: the 40% is **not** visible (no tracker, not styled) | System bar, 40% filled |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P1 plain bar 40%",
    "pt_progress": "40"
  }
}
```

### P2. Plain bar + styled by progress

| Android 16 | Android 15 and below |
|---|---|
| 40% full, rest faded | System bar, 40% filled (flag not used below 16) |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P2 plain bar styled 40%",
    "pt_progress": "40",
    "pt_styled_by_progress": "true"
  }
}
```

### P3. Plain bar + tracker

| Android 16 | Android 15 and below |
|---|---|
| Tracker rides the bar at 40% | Tracker next to the title, system bar 40% filled |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P3 plain bar + tracker 40%",
    "pt_progress": "40",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

### P4. Plain bar + start/end icons

| Android 16 | Android 15 and below |
|---|---|
| Store icon left, house icon right, 40% full, rest faded | Store icon left, house icon right, system bar 40% filled |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p4",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P4 plain bar + icons",
    "pt_progress": "40",
    "pt_styled_by_progress": "true",
    "pt_progress_start_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f3ea.png",
    "pt_progress_end_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f3e0.png"
  }
}
```

### P5. Plain bar + pt_progress_max

| Android 16 | Android 15 and below |
|---|---|
| Max ignored: 100 = **full** bar, tracker at the end | 100 of 200 = **half** bar |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p5",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P5 progress 100 max 200",
    "pt_progress": "100",
    "pt_progress_max": "200",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

### P6. Plain bar at 0, 100, -10, 150 (send one by one)

| Android 16 | Android 15 and below |
|---|---|
| 0: tracker far left, all faded. 100: tracker far right. -10 = 0. 150 = 100 | 0: empty. 100: full. -10 = empty. 150 = full |

Send 1 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P6 progress 0",
    "pt_progress": "0",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

Send 2 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P6 progress 100",
    "pt_progress": "100",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

Send 3 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P6 progress -10",
    "pt_progress": "-10",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

Send 4 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_p6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "P6 progress 150",
    "pt_progress": "150",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

## Indeterminate bar

### I1. Indeterminate

| Android 16 | Android 15 and below |
|---|---|
| Moving bar | Moving system bar |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_i1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Finding a driver",
    "pt_msg": "I1 indeterminate",
    "pt_progress_indeterminate": "true"
  }
}
```

### I2. Indeterminate + start/end icons

| Android 16 | Android 15 and below |
|---|---|
| Icons on both sides of the moving bar | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_i2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Finding a driver",
    "pt_msg": "I2 indeterminate + icons",
    "pt_progress_indeterminate": "true",
    "pt_progress_start_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f3ea.png",
    "pt_progress_end_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f3e0.png"
  }
}
```

### I3. Indeterminate + tracker

| Android 16 | Android 15 and below |
|---|---|
| No tracker anywhere | No tracker anywhere |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_i3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Finding a driver",
    "pt_msg": "I3 indeterminate + tracker (hidden)",
    "pt_progress_indeterminate": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png"
  }
}
```

### I4. Indeterminate + pt_progress, then indeterminate + segments (send one by one)

| Android 16 | Android 15 and below |
|---|---|
| 1st: still moving (pt_progress ignored). 2nd: milestone track (flag ignored) | Same |

Send 1 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_i4",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "I4a indeterminate + progress 40",
    "pt_progress_indeterminate": "true",
    "pt_progress": "40"
  }
}
```

Send 2 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_i4",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "I4b indeterminate + segments",
    "pt_progress_indeterminate": "true",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ]
  }
}
```

## Milestone track

### M1. Segments only

| Android 16 | Android 15 and below |
|---|---|
| Green / blue / orange, faded after 50, small gaps between segments | Same widths and colors, no gaps, thinner line |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M1 segments only",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ]
  }
}
```

### M2. Segments + points with titles

| Android 16 | Android 15 and below |
|---|---|
| 2 dots (33, 66), faded after 50, no titles | Same + titles Cooking / On way |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M2 segments + points",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ]
  }
}
```

### M3. Points only

| Android 16 | Android 15 and below |
|---|---|
| One full default-color bar, dots at 25 and 75 | Same + titles Packed / Shipped |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M3 points only",
    "pt_progress": "50",
    "pt_progress_points": [
      {
        "position": 25,
        "title": "Packed"
      },
      {
        "position": 75,
        "title": "Shipped"
      }
    ]
  }
}
```

### M4. Milestone + tracker

| Android 16 | Android 15 and below |
|---|---|
| Tracker on the track at 50 | Same; tracker NOT next to the title |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m4",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M4 milestone + tracker",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ]
  }
}
```

### M5. Milestone + tracker + styled

| Android 16 | Android 15 and below |
|---|---|
| Tracker at 50, everything after it faded (dot at 66 too) | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m5",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M5 tracker + styled",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ]
  }
}
```

### M6. Milestone, no tracker, not styled

| Android 16 | Android 15 and below |
|---|---|
| Colors and dots only, progress 50 **not** visible | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M6 no tracker, not styled",
    "pt_progress": "50",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ]
  }
}
```

### M7. Unequal segments 10/80/10

| Android 16 | Android 15 and below |
|---|---|
| Widths 10% / 80% / 10%, dots at 10 and 90 | Same + titles |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m7",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M7 unequal 10/80/10",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 10,
        "color": "#4CAF50"
      },
      {
        "length": 80,
        "color": "#2196F3"
      },
      {
        "length": 10,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 10,
        "title": "Placed"
      },
      {
        "position": 90,
        "title": "Arriving"
      }
    ]
  }
}
```

### M8. No colors

| Android 16 | Android 15 and below |
|---|---|
| Segments and dot in the theme accent color | Android 12-15: theme accent. Android 7-11: grey |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_m8",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "M8 no colors",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 50
      },
      {
        "length": 50
      }
    ],
    "pt_progress_points": [
      {
        "position": 50
      }
    ]
  }
}
```

## Limits and wrong payloads

### L1. Points at 0 and 100

| Android 16 | Android 15 and below |
|---|---|
| Only dots at 33 and 66 | Same; no Placed / Delivered titles |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L1 points at 0 and 100",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 0,
        "color": "#E91E63",
        "title": "Placed"
      },
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      },
      {
        "position": 100,
        "color": "#E91E63",
        "title": "Delivered"
      }
    ]
  }
}
```

### L2. 6 points

| Android 16 | Android 15 and below |
|---|---|
| Only the first 4: dots at 10, 20, 30, 40 | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L2 six points",
    "pt_progress": "50",
    "pt_progress_segments": [
      {
        "length": 100,
        "color": "#2196F3"
      }
    ],
    "pt_progress_points": [
      {
        "position": 10,
        "color": "#FF9500"
      },
      {
        "position": 20,
        "color": "#FF9500"
      },
      {
        "position": 30,
        "color": "#FF9500"
      },
      {
        "position": 40,
        "color": "#FF9500"
      },
      {
        "position": 50,
        "color": "#FF9500"
      },
      {
        "position": 60,
        "color": "#FF9500"
      }
    ]
  }
}
```

### L3. 11 segments: mixed colors, then all red (send one by one)

| Android 16 | Android 15 and below |
|---|---|
| 1st: ONE segment in the default color. 2nd: ONE red segment | Same |

Send 1 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L3a 11 segments mixed",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 10,
        "color": "#FF9500"
      },
      {
        "length": 10,
        "color": "#4CAF50"
      },
      {
        "length": 10,
        "color": "#FF9500"
      },
      {
        "length": 10,
        "color": "#4CAF50"
      },
      {
        "length": 10,
        "color": "#FF9500"
      },
      {
        "length": 10,
        "color": "#4CAF50"
      },
      {
        "length": 10,
        "color": "#FF9500"
      },
      {
        "length": 10,
        "color": "#4CAF50"
      },
      {
        "length": 10,
        "color": "#FF9500"
      },
      {
        "length": 10,
        "color": "#4CAF50"
      },
      {
        "length": 10,
        "color": "#FF9500"
      }
    ]
  }
}
```

Send 2 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L3b 11 segments all red",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      },
      {
        "length": 10,
        "color": "#F44336"
      }
    ]
  }
}
```

### L4. Segment with length 0 and -5

| Android 16 | Android 15 and below |
|---|---|
| Those two dropped; green and orange fill the track 50/50 | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l4",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L4 zero and negative length",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 50,
        "color": "#4CAF50"
      },
      {
        "length": 0,
        "color": "#F44336"
      },
      {
        "length": -5,
        "color": "#F44336"
      },
      {
        "length": 50,
        "color": "#FF9500"
      }
    ]
  }
}
```

### L5. Two points at the same position

| Android 16 | Android 15 and below |
|---|---|
| One **green** dot at 50 (the last one wins) | Same, title B |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l5",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L5 duplicate position",
    "pt_progress": "30",
    "pt_progress_segments": [
      {
        "length": 100,
        "color": "#2196F3"
      }
    ],
    "pt_progress_points": [
      {
        "position": 50,
        "color": "#F44336",
        "title": "A"
      },
      {
        "position": 50,
        "color": "#4CAF50",
        "title": "B"
      }
    ]
  }
}
```

### L6. pt_progress 150, then -10 (send one by one)

| Android 16 | Android 15 and below |
|---|---|
| 150: tracker at the end. -10: tracker at the start | Same |

Send 1 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L6 progress 150",
    "pt_progress": "150",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ]
  }
}
```

Send 2 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L6 progress -10",
    "pt_progress": "-10",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ]
  }
}
```

### L7. Scale mismatch: segments add to 3, progress 66

| Android 16 | Android 15 and below |
|---|---|
| Tracker stuck at the far right, nothing faded | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l7",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L7 scale mismatch",
    "pt_progress": "66",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 1,
        "color": "#4CAF50"
      },
      {
        "length": 1,
        "color": "#2196F3"
      },
      {
        "length": 1,
        "color": "#FF9500"
      }
    ]
  }
}
```

### L8. Broken segments JSON, with pt_progress

| Android 16 | Android 15 and below |
|---|---|
| Segments ignored: plain bar at 40 | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l8",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L8 broken JSON + progress 40",
    "pt_progress": "40",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": "[{length:"
  }
}
```

### L8b. Broken segments JSON, nothing else

| Android 16 | Android 15 and below |
|---|---|
| **No notification** | **No notification** |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_l8b",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "L8b broken JSON only",
    "pt_progress_segments": "[{length:"
  }
}
```

## Chip

### C1. No chip

| Android 16 | Android 15 and below |
|---|---|
| No chip | No chip |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C1 no chip",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "none"
  }
}
```

### C2. Text chip

| Android 16 | Android 15 and below |
|---|---|
| "12 min" in the status bar (shade closed) | "12 min" right of the title in the card |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C2 text chip",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "text",
    "pt_chip_text": "12 min"
  }
}
```

### C3. Text chip, long text

| Android 16 | Android 15 and below |
|---|---|
| Chip shows only the icon (too long for 96dp) | Cut at 96dp with ... |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C3 long chip text",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "text",
    "pt_chip_text": "Arriving in about 12 minutes"
  }
}
```

### C4. Timer (counts up from 5 min ago)

| Android 16 | Android 15 and below |
|---|---|
| Running time in the chip and the header | Running time in the header |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c4",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C4 timer",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "timer",
    "pt_when": "{{now-300}}"
  }
}
```

### C5. Countdown (10 min)

| Android 16 | Android 15 and below |
|---|---|
| Counts down in the chip and the header | Counts down in the header |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c5",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C5 countdown 10 min",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "countdown",
    "pt_when": "{{now+600}}",
    "pt_countdown": "true"
  }
}
```

### C6. Mixed-up chip keys (send one by one)

| Android 16 | Android 15 and below |
|---|---|
| 1: timer, no text. 2: no chip. 3: no chip. 4: no timer | Same |

Send 1 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C6a timer + text",
    "pt_progress": "50",
    "pt_chip_type": "timer",
    "pt_when": "{{now-300}}",
    "pt_chip_text": "12 min"
  }
}
```

Send 2 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C6b text type, no text",
    "pt_progress": "50",
    "pt_chip_type": "text"
  }
}
```

Send 3 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C6c text, no type",
    "pt_progress": "50",
    "pt_chip_text": "12 min"
  }
}
```

Send 4 of 4:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_c6",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "C6d timer, no pt_when",
    "pt_progress": "50",
    "pt_chip_type": "timer"
  }
}
```

## Promotion

### R1. Promoted (default)

| Android 16 | Android 15 and below |
|---|---|
| Top of the shade, lock screen, status-bar chip, cannot collapse | Normal ongoing notification, can collapse |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_r1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "R1 promoted",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "text",
    "pt_chip_text": "12 min",
    "pt_promote": "true"
  }
}
```

### R2. Not promoted

| Android 16 | Android 15 and below |
|---|---|
| Normal ongoing notification, no chip, can collapse | Same as R1 |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_r2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "R2 not promoted",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_chip_type": "text",
    "pt_chip_text": "12 min",
    "pt_promote": "false"
  }
}
```

## Lifecycle

### E1. start, then update (send one by one)

| Android 16 | Android 15 and below |
|---|---|
| One card that changes from 10 to 60 in place, no new sound | Same |

Send 1 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_e1",
  "wzrk_la_event": "start",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "E1 start 10",
    "pt_progress": "10",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ]
  }
}
```

Send 2 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_e1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "E1 update 60",
    "pt_progress": "60",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ]
  }
}
```

### E2. end (send after E1)

| Android 16 | Android 15 and below |
|---|---|
| Same card, not ongoing, no chip, not promoted (can collapse), swipe or tap removes it | Same card, not ongoing, swipe or tap removes it |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_e1",
  "wzrk_la_event": "end",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "Delivered",
    "pt_progress": "100",
    "pt_styled_by_progress": "true",
    "pt_progress_tracker_icon": "https://cdn.jsdelivr.net/gh/jdecked/twemoji@16.0.1/assets/72x72/1f6f5.png",
    "pt_progress_segments": [
      {
        "length": 33,
        "color": "#4CAF50"
      },
      {
        "length": 33,
        "color": "#2196F3"
      },
      {
        "length": 34,
        "color": "#FF9500"
      }
    ],
    "pt_progress_points": [
      {
        "position": 33,
        "color": "#4CAF50",
        "title": "Cooking"
      },
      {
        "position": 66,
        "color": "#2196F3",
        "title": "On way"
      }
    ],
    "pt_chip_type": "text",
    "pt_chip_text": "0 min"
  }
}
```

### E3. No wzrk_activityId (send twice)

| Android 16 | Android 15 and below |
|---|---|
| Two separate cards | Same |

Send 1 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "E3 no activity id",
    "pt_progress": "50"
  }
}
```

Send 2 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "E3 no activity id (2nd)",
    "pt_progress": "70"
  }
}
```

### E4. Two different wzrk_activityId

| Android 16 | Android 15 and below |
|---|---|
| Two separate cards | Same |

Send 1 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_e4_a",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "E4 order A",
    "pt_progress": "30"
  }
}
```

Send 2 of 2:

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_e4_b",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "E4 order B",
    "pt_progress": "70"
  }
}
```

## Tap and buttons

### A1. Tap deep link

| Android 16 | Android 15 and below |
|---|---|
| Tap opens clevertap.com | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_a1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "A1 tap opens a link",
    "pt_progress": "50",
    "wzrk_dl": "https://clevertap.com/order/A1234"
  }
}
```

### A2. Two buttons

| Android 16 | Android 15 and below |
|---|---|
| Track order (stays), Support (removes the card) | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_a2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "A2 buttons",
    "pt_progress": "50",
    "wzrk_acts": [
      {
        "id": "track",
        "l": "Track order",
        "dl": "https://clevertap.com/track",
        "ac": false
      },
      {
        "id": "support",
        "l": "Support",
        "dl": "https://clevertap.com/support",
        "ac": true
      }
    ]
  }
}
```

## Light and dark mode

### D1. No colors (run in light, then dark)

| Android 16 | Android 15 and below |
|---|---|
| Theme accent color | Android 12-15: theme accent. Android 7-11: grey |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_d1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "D1 default colors",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": [
      {
        "length": 50
      },
      {
        "length": 50
      }
    ],
    "pt_progress_points": [
      {
        "position": 50,
        "title": "Half"
      }
    ]
  }
}
```

### D2. White and black (run in light, then dark)

| Android 16 | Android 15 and below |
|---|---|
| White still visible on light, black on dark (as grey) | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_d2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "D2 white and black",
    "pt_progress": "50",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": [
      {
        "length": 34
      },
      {
        "length": 33,
        "color": "#FFFFFF"
      },
      {
        "length": 33,
        "color": "#000000"
      }
    ],
    "pt_progress_points": [
      {
        "position": 34,
        "title": "Default"
      },
      {
        "position": 67,
        "color": "#FFFFFF",
        "title": "White"
      },
      {
        "position": 90,
        "color": "#000000",
        "title": "Black"
      }
    ]
  }
}
```

## Failures

### F1. No title

| Android 16 | Android 15 and below |
|---|---|
| **No notification** | **No notification** |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_f1",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_msg": "F1 no title",
    "pt_progress": "50"
  }
}
```

### F2. No indicator

| Android 16 | Android 15 and below |
|---|---|
| **No notification** | **No notification** |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_f2",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "F2 no indicator"
  }
}
```

### F3. Broken icon URLs

| Android 16 | Android 15 and below |
|---|---|
| Icons missing, the rest shows | Same |

```json
{
  "wzrk_pn": "true",
  "wzrk_la": "true",
  "wzrk_activityId": "qa_f3",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "wzrk_acct_id": "<your account id>",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "F3 broken icons",
    "pt_progress": "50",
    "pt_progress_tracker_icon": "https://example.invalid/bike.png",
    "pt_progress_start_icon": "https://example.invalid/store.png",
    "pt_progress_end_icon": "https://example.invalid/home.png"
  }
}
```

