# `pt_progress` Live Update: copy-paste QA payloads

Every payload below is a **full push** (wrapper + `data`), the same shape the backend sends. Paste it
into the sample app and it goes through the same path as a real FCM push.

## How to send a payload

1. Open the sample app, scroll to **LIVE UPDATES** and tap **Paste Live Update payload (QA)**.
2. Copy one payload, tap **Paste**, then **Send**.
3. Open the notification shade (close it again to see the Android 16 status-bar chip).

From a computer instead, with a debug build of the sample app (the whole JSON in single quotes):

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

## How to read the expected visual

| Symbol | Meaning |
|---|---|
| `===` | Bar or segment, full color |
| `---` | Faded part (half opacity, thinner): only with `pt_styled_by_progress` |
| `...` | Empty part of the Android 15 system bar |
| `##....##` | Indeterminate bar (moving) |
| `o` | Point (milestone dot) |
| `(T)` | Tracker icon (scooter) |
| `[S]` / `[E]` | Start icon (store) / end icon (house) |
| `(i)` | Small app icon |
| a space inside the bar | Small gap: between segments on Android 16, or at the progress when styled with no tracker |
| `colors:` line | The segment colors and lengths, left to right (`accent` = the theme color) |

The drawings are not pixel-exact; they show what appears and where.

---

## Plain determinate bar

### P1. Plain bar, nothing else

**Test name:** P1. Plain bar, nothing else

**Payload:**

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

**Expected result:** No tracker and not styled: Android 16 draws one solid bar and the progress is not visible (it only shows progress through a tracker or styled-by-progress). Below 16 the system bar fills to 40%.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P1 plain bar 40%                                     |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P1 plain bar 40%                                     |
| ==================..........................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### P2. Plain bar + styled by progress

**Test name:** P2. Plain bar + styled by progress

**Payload:**

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

**Expected result:** Styled by progress: Android 16 fades everything after 40%, with a small gap at 40%. Below 16 the flag is not used; the system bar fills to 40%.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P2 plain bar styled 40%                              |
| ================= --------------------------         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P2 plain bar styled 40%                              |
| ==================..........................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### P3. Plain bar + tracker

**Test name:** P3. Plain bar + tracker

**Payload:**

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

**Expected result:** Android 16 puts the tracker on the bar at 40%. Below 16 a system bar cannot hold an image, so the tracker sits next to the title.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P3 plain bar + tracker 40%                           |
| ================(T)=========================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| P3 plain bar + tracker 40%                           |
| ==================..........................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### P4. Plain bar + start/end icons

**Test name:** P4. Plain bar + start/end icons

**Payload:**

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

**Expected result:** Start and end icons sit on both sides of the bar on both tiers.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P4 plain bar + icons                                 |
| [S] ================= -------------------------- [E] |
|     colors: accent                                   |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P4 plain bar + icons                                 |
| [S] ==================.......................... [E] |
|     colors: system bar                               |
+------------------------------------------------------+
```

### P5. Plain bar + pt_progress_max

**Test name:** P5. Plain bar + pt_progress_max

**Payload:**

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

**Expected result:** Android 16 has no max: the scale is 0-100, so 100 is a full bar. Below 16 the plain bar uses pt_progress_max: 100 of 200 is half.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P5 progress 100 max 200                              |
| =========================================(T)         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| P5 progress 100 max 200                              |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### P6. Plain bar at 0, 100, -10, 150 (send one by one)

**Test name:** P6. Plain bar at 0, 100, -10, 150 (send one by one)

**Expected result:** pt_progress is kept inside the track: -10 acts as 0 and 150 acts as 100, on both tiers.

Send the 4 payloads below one by one, in this order.

#### Send 1 of 4

**Payload 1:**

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

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P6 progress 0                                        |
| (T)-----------------------------------------         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| P6 progress 0                                        |
| ............................................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 2 of 4

**Payload 2:**

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

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P6 progress 100                                      |
| =========================================(T)         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| P6 progress 100                                      |
| ============================================         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 3 of 4

**Payload 3:**

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

**Expected visual after payload 3:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P6 progress -10                                      |
| (T)-----------------------------------------         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| P6 progress -10                                      |
| ............................................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 4 of 4

**Payload 4:**

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

**Expected visual after payload 4:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| P6 progress 150                                      |
| =========================================(T)         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| P6 progress 150                                      |
| ============================================         |
| colors: system bar                                   |
+------------------------------------------------------+
```

---

## Indeterminate bar

### I1. Indeterminate

**Test name:** I1. Indeterminate

**Payload:**

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

**Expected result:** No segments or points + indeterminate: an animated bar with no fill.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Finding a driver                                     |
| I1 indeterminate                                     |
| ##....##....##....##....##....##....##....##         |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Finding a driver                                     |
| I1 indeterminate                                     |
| ##....##....##....##....##....##....##....##         |
+------------------------------------------------------+
```

### I2. Indeterminate + start/end icons

**Test name:** I2. Indeterminate + start/end icons

**Payload:**

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

**Expected result:** Start and end icons also sit around an indeterminate bar.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Finding a driver                                     |
| I2 indeterminate + icons                             |
| [S] ##....##....##....##....##....##....##....## [E] |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Finding a driver                                     |
| I2 indeterminate + icons                             |
| [S] ##....##....##....##....##....##....##....## [E] |
+------------------------------------------------------+
```

### I3. Indeterminate + tracker

**Test name:** I3. Indeterminate + tracker

**Payload:**

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

**Expected result:** An indeterminate bar has no position, so the tracker is hidden on both tiers.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Finding a driver                                     |
| I3 indeterminate + tracker (hidden)                  |
| ##....##....##....##....##....##....##....##         |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Finding a driver                                     |
| I3 indeterminate + tracker (hidden)                  |
| ##....##....##....##....##....##....##....##         |
+------------------------------------------------------+
```

### I4. Indeterminate + pt_progress, then indeterminate + segments (send one by one)

**Test name:** I4. Indeterminate + pt_progress, then indeterminate + segments (send one by one)

**Expected result:** Indeterminate wins over pt_progress, but segments win over indeterminate: with segments you get the milestone track.

Send the 2 payloads below one by one, in this order.

#### Send 1 of 2

**Payload 1:**

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

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| I4a indeterminate + progress 40                      |
| ##....##....##....##....##....##....##....##         |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| I4a indeterminate + progress 40                      |
| ##....##....##....##....##....##....##....##         |
+------------------------------------------------------+
```

#### Send 2 of 2

**Payload 2:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ]
  }
}
```

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| I4b indeterminate + segments                         |
| ============== ======= ----- ---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| I4b indeterminate + segments                         |
| ====================== ---------------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

---

## Milestone track

### M1. Segments only

**Test name:** M1. Segments only

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ]
  }
}
```

**Expected result:** Segment widths follow their length (33/33/34). Styled by progress fades everything after 50. Android 16 leaves small gaps between segments; the fallback does not.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M1 segments only                                     |
| ============== ======= ----- ---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M1 segments only                                     |
| ====================== ---------------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### M2. Segments + points with titles

**Test name:** M2. Segments + points with titles

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ]
  }
}
```

**Expected result:** Points sit at their position. Point titles show only below 16 (native points have no text).

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M2 segments + points                                 |
| ==============o======= -----o---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M2 segments + points                                 |
| ==============o======= -----o---------------         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### M3. Points only

**Test name:** M3. Points only

**Payload:**

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
      {"position": 25, "title": "Packed"},
      {"position": 75, "title": "Shipped"}
    ]
  }
}
```

**Expected result:** No segments: both tiers draw one full segment of 100 in the theme accent color, with the dots on it.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M3 points only                                       |
| ===========o====================o===========         |
| colors: one segment, accent                          |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M3 points only                                       |
| ===========o====================o===========         |
|         Packed               Shipped                 |
| colors: one segment, accent                          |
+------------------------------------------------------+
```

### M4. Milestone + tracker

**Test name:** M4. Milestone + tracker

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ]
  }
}
```

**Expected result:** The tracker rides the track at 50 on both tiers. Below 16 it is not also shown next to the title.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M4 milestone + tracker                               |
| ==============o======(T)====o===============         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M4 milestone + tracker                               |
| ==============o======(T)====o===============         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### M5. Milestone + tracker + styled

**Test name:** M5. Milestone + tracker + styled

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ]
  }
}
```

**Expected result:** Tracker at 50 and everything after it faded, including the dot at 66. With a tracker there is no gap.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M5 tracker + styled                                  |
| ==============o======(T)----o---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M5 tracker + styled                                  |
| ==============o======(T)----o---------------         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### M6. Milestone, no tracker, not styled

**Test name:** M6. Milestone, no tracker, not styled

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ]
  }
}
```

**Expected result:** No tracker and not styled: only colors and dots, the progress 50 is not visible. Same on both tiers.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M6 no tracker, not styled                            |
| ==============o=============o===============         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M6 no tracker, not styled                            |
| ==============o=============o===============         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### M7. Unequal segments 10/80/10

**Test name:** M7. Unequal segments 10/80/10

**Payload:**

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
      {"length": 10, "color": "#4CAF50"},
      {"length": 80, "color": "#2196F3"},
      {"length": 10, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 10, "title": "Placed"},
      {"position": 90, "title": "Arriving"}
    ]
  }
}
```

**Expected result:** Widths follow length: 10% green, 80% blue, 10% orange. Dots at 10 and 90.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M7 unequal 10/80/10                                  |
| ====o================(T)===============o====         |
| colors: green 10 | blue 80 | orange 10               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M7 unequal 10/80/10                                  |
| ====o================(T)===============o====         |
|  Placed                            Arriving          |
| colors: green 10 | blue 80 | orange 10               |
+------------------------------------------------------+
```

### M8. No colors

**Test name:** M8. No colors

**Payload:**

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
      {"length": 50},
      {"length": 50}
    ],
    "pt_progress_points": [
      {"position": 50}
    ]
  }
}
```

**Expected result:** Segments and points with no color take the theme accent (Android 12+), or grey on Android 7-11.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M8 no colors                                         |
| =====================(T)====================         |
| colors: accent 50 | accent 50                        |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| M8 no colors                                         |
| =====================(T)====================         |
| colors: accent 50 | accent 50                        |
+------------------------------------------------------+
```

---

## Limits and wrong payloads

### L1. Points at 0 and 100

**Test name:** L1. Points at 0 and 100

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 0, "color": "#E91E63", "title": "Placed"},
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"},
      {"position": 100, "color": "#E91E63", "title": "Delivered"}
    ]
  }
}
```

**Expected result:** Points at 0 and at the end are never drawn (Android 16 rule, copied by the fallback). Only 33 and 66 show.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L1 points at 0 and 100                               |
| ==============o======= -----o---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L1 points at 0 and 100                               |
| ==============o======= -----o---------------         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### L2. 6 points

**Test name:** L2. 6 points

**Payload:**

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
      {"length": 100, "color": "#2196F3"}
    ],
    "pt_progress_points": [
      {"position": 10, "color": "#FF9500"},
      {"position": 20, "color": "#FF9500"},
      {"position": 30, "color": "#FF9500"},
      {"position": 40, "color": "#FF9500"},
      {"position": 50, "color": "#FF9500"},
      {"position": 60, "color": "#FF9500"}
    ]
  }
}
```

**Expected result:** Only the first 4 points are kept: 10, 20, 30, 40.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L2 six points                                        |
| ====o====o===o===o==========================         |
| colors: one segment, blue                            |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L2 six points                                        |
| ====o====o===o===o==========================         |
| colors: one segment, blue                            |
+------------------------------------------------------+
```

### L3. 11 segments: mixed colors, then all red (send one by one)

**Test name:** L3. 11 segments: mixed colors, then all red (send one by one)

**Expected result:** More than 10 segments become ONE segment. It keeps the color only if every segment had the same color.

Send the 2 payloads below one by one, in this order.

#### Send 1 of 2

**Payload 1:**

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
      {"length": 10, "color": "#FF9500"},
      {"length": 10, "color": "#4CAF50"},
      {"length": 10, "color": "#FF9500"},
      {"length": 10, "color": "#4CAF50"},
      {"length": 10, "color": "#FF9500"},
      {"length": 10, "color": "#4CAF50"},
      {"length": 10, "color": "#FF9500"},
      {"length": 10, "color": "#4CAF50"},
      {"length": 10, "color": "#FF9500"},
      {"length": 10, "color": "#4CAF50"},
      {"length": 10, "color": "#FF9500"}
    ]
  }
}
```

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L3a 11 segments mixed                                |
| ===================(T)======================         |
| colors: one segment, accent                          |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L3a 11 segments mixed                                |
| ===================(T)======================         |
| colors: one segment, accent                          |
+------------------------------------------------------+
```

#### Send 2 of 2

**Payload 2:**

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
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"},
      {"length": 10, "color": "#F44336"}
    ]
  }
}
```

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L3b 11 segments all red                              |
| ===================(T)======================         |
| colors: one segment, red                             |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L3b 11 segments all red                              |
| ===================(T)======================         |
| colors: one segment, red                             |
+------------------------------------------------------+
```

### L4. Segment with length 0 and -5

**Test name:** L4. Segment with length 0 and -5

**Payload:**

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
      {"length": 50, "color": "#4CAF50"},
      {"length": 0, "color": "#F44336"},
      {"length": -5, "color": "#F44336"},
      {"length": 50, "color": "#FF9500"}
    ]
  }
}
```

**Expected result:** Segments with length 0 or less are dropped; the other two share the track 50/50.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L4 zero and negative length                          |
| =====================(T)====================         |
| colors: green 50 | orange 50                         |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L4 zero and negative length                          |
| =====================(T)====================         |
| colors: green 50 | orange 50                         |
+------------------------------------------------------+
```

### L5. Two points at the same position

**Test name:** L5. Two points at the same position

**Payload:**

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
      {"length": 100, "color": "#2196F3"}
    ],
    "pt_progress_points": [
      {"position": 50, "color": "#F44336", "title": "A"},
      {"position": 50, "color": "#4CAF50", "title": "B"}
    ]
  }
}
```

**Expected result:** Two points at the same position: the last one wins (green, title B).

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L5 duplicate position                                |
| ======================o=====================         |
| colors: one segment, blue                            |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L5 duplicate position                                |
| ======================o=====================         |
|                       B                              |
| colors: one segment, blue                            |
+------------------------------------------------------+
```

### L6. pt_progress 150, then -10 (send one by one)

**Test name:** L6. pt_progress 150, then -10 (send one by one)

**Expected result:** pt_progress outside the track is kept inside it: 150 goes to the end, -10 to the start.

Send the 2 payloads below one by one, in this order.

#### Send 1 of 2

**Payload 1:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ]
  }
}
```

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L6 progress 150                                      |
| ============== ============= ============(T)         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L6 progress 150                                      |
| =========================================(T)         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

#### Send 2 of 2

**Payload 2:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ]
  }
}
```

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L6 progress -10                                      |
| (T)----------- ------------- ---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L6 progress -10                                      |
| (T)-----------------------------------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### L7. Scale mismatch: segments add to 3, progress 66

**Test name:** L7. Scale mismatch: segments add to 3, progress 66

**Payload:**

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
      {"length": 1, "color": "#4CAF50"},
      {"length": 1, "color": "#2196F3"},
      {"length": 1, "color": "#FF9500"}
    ]
  }
}
```

**Expected result:** Segments add up to 3, so 66 is far past the end: the tracker sits at the far right and nothing is faded.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L7 scale mismatch                                    |
| ============== ============== ===========(T)         |
| colors: green 1 | blue 1 | orange 1                  |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L7 scale mismatch                                    |
| =========================================(T)         |
| colors: green 1 | blue 1 | orange 1                  |
+------------------------------------------------------+
```

### L8. Broken segments JSON, with pt_progress

**Test name:** L8. Broken segments JSON, with pt_progress

**Payload:**

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

**Expected result:** Broken segments JSON is ignored. pt_progress is still there, so a plain bar shows.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| L8 broken JSON + progress 40                         |
| ================(T)=========================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| L8 broken JSON + progress 40                         |
| ==================..........................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### L8b. Broken segments JSON, nothing else

**Test name:** L8b. Broken segments JSON, nothing else

**Payload:**

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

**Expected result:** Broken segments JSON is ignored and nothing else is left, so the push is not shown.

**Expected visual:**

```
Android 16
(no notification)

Android 15 and below
(no notification)
```

---

## Chip

### C1. No chip

**Test name:** C1. No chip

**Payload:**

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

**Expected result:** pt_chip_type none: no chip anywhere.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C1 no chip                                           |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                                     |
| C1 no chip                                           |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### C2. Text chip

**Test name:** C2. Text chip

**Payload:**

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

**Expected result:** Android 16: the text chip is in the status bar (promoted, shade closed), not in the card. Below 16 it is shown right of the title.

**Expected visual:**

```
Android 16
Status bar (shade closed): [(i) 12 min]
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C2 text chip                                         |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                              12 min |
| C2 text chip                                         |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### C3. Text chip, long text

**Test name:** C3. Text chip, long text

**Payload:**

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

**Expected result:** The chip is at most 96dp. Android 16 shows only the icon when less than half the text fits; below 16 the text is cut with ...

**Expected visual:**

```
Android 16
Status bar (shade closed): [(i)]
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C3 long chip text                                    |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                        Arriving ... |
| C3 long chip text                                    |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### C4. Timer (counts up from 5 min ago)

**Test name:** C4. Timer (counts up from 5 min ago)

**Payload:**

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

**Expected result:** A timer counts up from pt_when. Android 16: in the chip and the header. Below 16: in the header.

**Expected visual:**

```
Android 16
Status bar (shade closed): [(i) 05:00]
+------------------------------------------------------+
| (i) App · 05:00                                      |
| Order #A1234                                         |
| C4 timer                                             |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · 05:00                                      |
| (T) Order #A1234                                     |
| C4 timer                                             |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### C5. Countdown (10 min)

**Test name:** C5. Countdown (10 min)

**Payload:**

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

**Expected result:** A countdown counts down to pt_when. Android 16: in the chip and the header. Below 16: in the header. It does not stop at zero.

**Expected visual:**

```
Android 16
Status bar (shade closed): [(i) 10:00]
+------------------------------------------------------+
| (i) App · 10:00 (counting down)                      |
| Order #A1234                                         |
| C5 countdown 10 min                                  |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · 10:00 (counting down)                      |
| (T) Order #A1234                                     |
| C5 countdown 10 min                                  |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### C6. Mixed-up chip keys (send one by one)

**Test name:** C6. Mixed-up chip keys (send one by one)

**Expected result:** pt_chip_text is used only for pt_chip_type text; a timer needs pt_when. Anything else: no chip.

Send the 4 payloads below one by one, in this order.

#### Send 1 of 4

**Payload 1:**

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

**Expected visual after payload 1:**

```
Android 16
Status bar (shade closed): [(i) 05:00]
+------------------------------------------------------+
| (i) App · 05:00                                      |
| Order #A1234                                         |
| C6a timer + text                                     |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · 05:00                                      |
| Order #A1234                                         |
| C6a timer + text                                     |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 2 of 4

**Payload 2:**

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

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C6b text type, no text                               |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C6b text type, no text                               |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 3 of 4

**Payload 3:**

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

**Expected visual after payload 3:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C6c text, no type                                    |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C6c text, no type                                    |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 4 of 4

**Payload 4:**

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

**Expected visual after payload 4:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C6d timer, no pt_when                                |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| C6d timer, no pt_when                                |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

---

## Promotion

### R1. Promoted (default)

**Test name:** R1. Promoted (default)

**Payload:**

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

**Expected result:** Promoted (default): on Android 16 at the top of the shade and on the lock screen, chip in the status bar, cannot collapse. Below 16 promotion does not exist.

**Expected visual:**

```
Android 16
Status bar (shade closed): [(i) 12 min]
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| R1 promoted                                          |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                              12 min |
| R1 promoted                                          |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### R2. Not promoted

**Test name:** R2. Not promoted

**Payload:**

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

**Expected result:** pt_promote false: on Android 16 a normal ongoing notification, no status-bar chip, can collapse.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| R2 not promoted                                      |
| =====================(T)====================         |
| colors: accent                                       |
+------------------------------------------------------+
(not promoted: can collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| (T) Order #A1234                              12 min |
| R2 not promoted                                      |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

---

## Lifecycle

### E1. start, then update (send one by one)

**Test name:** E1. start, then update (send one by one)

**Expected result:** Same wzrk_activityId: the second push changes the same card in place (10 -> 60), no new sound.

Send the 2 payloads below one by one, in this order.

#### Send 1 of 2

**Payload 1:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ]
  }
}
```

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E1 start 10                                          |
| ===(T)--------o-------------o---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E1 start 10                                          |
| ===(T)--------o-------------o---------------         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

#### Send 2 of 2

**Payload 2:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ]
  }
}
```

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E1 update 60                                         |
| ==============o==========(T)o---------------         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E1 update 60                                         |
| ==============o==========(T)o---------------         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
```

### E2. end (send after E1)

**Test name:** E2. end (send after E1)

**Payload:**

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
      {"length": 33, "color": "#4CAF50"},
      {"length": 33, "color": "#2196F3"},
      {"length": 34, "color": "#FF9500"}
    ],
    "pt_progress_points": [
      {"position": 33, "color": "#4CAF50", "title": "Cooking"},
      {"position": 66, "color": "#2196F3", "title": "On way"}
    ],
    "pt_chip_type": "text",
    "pt_chip_text": "0 min"
  }
}
```

**Expected result:** end: the same card stops being ongoing (swipe or tap removes it), the chip goes, and on Android 16 it is no longer promoted.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| Delivered                                            |
| ==============o=============o============(T)         |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(ended: not ongoing, can collapse, swipe or tap removes it)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| Delivered                                            |
| ==============o=============o============(T)         |
|            Cooking       On way                      |
| colors: green 33 | blue 33 | orange 34               |
+------------------------------------------------------+
(ended: not ongoing, swipe or tap removes it)
```

### E3. No wzrk_activityId (send twice)

**Test name:** E3. No wzrk_activityId (send twice)

**Expected result:** No wzrk_activityId: every push makes a new card.

Send the 2 payloads below one by one, in this order.

#### Send 1 of 2

**Payload 1:**

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

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E3 no activity id                                    |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E3 no activity id                                    |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 2 of 2

**Payload 2:**

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

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E3 no activity id (2nd)                              |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E3 no activity id (2nd)                              |
| ===============================.............         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### E4. Two different wzrk_activityId

**Test name:** E4. Two different wzrk_activityId

**Expected result:** Two different wzrk_activityId values: two separate cards.

Send the 2 payloads below one by one, in this order.

#### Send 1 of 2

**Payload 1:**

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

**Expected visual after payload 1:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E4 order A                                           |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E4 order A                                           |
| =============...............................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

#### Send 2 of 2

**Payload 2:**

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

**Expected visual after payload 2:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E4 order B                                           |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| E4 order B                                           |
| ===============================.............         |
| colors: system bar                                   |
+------------------------------------------------------+
```

---

## Tap and buttons

### A1. Tap deep link

**Test name:** A1. Tap deep link

**Payload:**

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

**Expected result:** Tapping the card opens the deep link.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| A1 tap opens a link                                  |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| A1 tap opens a link                                  |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

### A2. Two buttons

**Test name:** A2. Two buttons

**Payload:**

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
      {"id": "track", "l": "Track order", "dl": "https://clevertap.com/track", "ac": false},
      {"id": "support", "l": "Support", "dl": "https://clevertap.com/support", "ac": true}
    ]
  }
}
```

**Expected result:** Buttons show under the card. Track order keeps the card (ac false); Support removes it (ac true).

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| A2 buttons                                           |
| ============================================         |
| colors: accent                                       |
| [ Track order ]   [ Support ]                        |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| A2 buttons                                           |
| ======================......................         |
| colors: system bar                                   |
| [ Track order ]   [ Support ]                        |
+------------------------------------------------------+
```

---

## Light and dark mode

### D1. No colors (run in light, then dark)

**Test name:** D1. No colors (run in light, then dark)

**Payload:**

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
      {"length": 50},
      {"length": 50}
    ],
    "pt_progress_points": [
      {"position": 50, "title": "Half"}
    ]
  }
}
```

**Expected result:** No colors: the theme accent. Run in light and dark mode: the card and text follow the phone theme.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| D1 default colors                                    |
| ======================o---------------------         |
| colors: accent 50 | accent 50                        |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| D1 default colors                                    |
| ======================o---------------------         |
|                     Half                             |
| colors: accent 50 | accent 50                        |
+------------------------------------------------------+
```

### D2. White and black (run in light, then dark)

**Test name:** D2. White and black (run in light, then dark)

**Payload:**

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
      {"length": 34},
      {"length": 33, "color": "#FFFFFF"},
      {"length": 33, "color": "#000000"}
    ],
    "pt_progress_points": [
      {"position": 34, "title": "Default"},
      {"position": 67, "color": "#FFFFFF", "title": "White"},
      {"position": 90, "color": "#000000", "title": "Black"}
    ]
  }
}
```

**Expected result:** Each color is made lighter or darker (same hue) until it has 3:1 contrast with the card, so white shows on a light card and black on a dark card.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| D2 white and black                                   |
| ===============o====== ------o---------o----         |
| colors: accent 34 | white 33 | black 33              |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| D2 white and black                                   |
| ===============o====== ------o---------o----         |
|             Default        White     Black           |
| colors: accent 34 | white 33 | black 33              |
+------------------------------------------------------+
```

---

## Failures

### F1. No title

**Test name:** F1. No title

**Payload:**

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

**Expected result:** No title: the push is not shown.

**Expected visual:**

```
Android 16
(no notification)

Android 15 and below
(no notification)
```

### F2. No indicator

**Test name:** F2. No indicator

**Payload:**

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

**Expected result:** No indicator at all (no progress, segments, points or indeterminate): the push is not shown.

**Expected visual:**

```
Android 16
(no notification)

Android 15 and below
(no notification)
```

### F3. Broken icon URLs

**Test name:** F3. Broken icon URLs

**Payload:**

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

**Expected result:** Icons that fail to download are left out; the rest of the card shows.

**Expected visual:**

```
Android 16
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| F3 broken icons                                      |
| ============================================         |
| colors: accent                                       |
+------------------------------------------------------+
(promoted: top of shade, cannot collapse)

Android 15 and below
+------------------------------------------------------+
| (i) App · now                                        |
| Order #A1234                                         |
| F3 broken icons                                      |
| ======================......................         |
| colors: system bar                                   |
+------------------------------------------------------+
```

