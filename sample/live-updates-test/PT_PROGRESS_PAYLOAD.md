# `pt_progress` Live Update — payload reference

Wire shape for a **progress-centric Live Update** rendered by the SDK / Push Templates
(**Mode B** — the `data` object carries a `pt_id`, so the SDK renders it and it takes precedence
over any registered notification factory).

- Top-level keys = the **Live Update wrapper** (routing + identity).
- Nested **`data`** object = the **render blob** (Push Template keys). The SDK surfaces `data` to the
  top level with a *root-wins* merge (wrapper/identity keys are never overwritten).

## Full payload (all features)

```json
{
  "wzrk_la": "true",
  "wzrk_activityId": "order_A1234",
  "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",

  "wzrk_pn": "true",
  "wzrk_rnv": "true",
  "wzrk_id": "<BE-assigned>",
  "wzrk_pid": "<BE-assigned>",
  "wzrk_acct_id": "<your account id>",

  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order #A1234",
    "pt_msg": "Out for delivery",
    "pt_small_icon_clr": "#FF9500",

    "pt_progress": "66",
    "pt_progress_max": "100",
    "pt_styled_by_progress": "true",
    "pt_progress_indeterminate": "false",

    "pt_progress_segments": [
      { "length": 33, "color": "#4CAF50" },
      { "length": 33, "color": "#4CAF50" },
      { "length": 34, "color": "#48484A" }
    ],
    "pt_progress_points": [
      { "position": 0,   "color": "#4CAF50", "title": "Placed" },
      { "position": 33,  "color": "#4CAF50", "title": "Preparing" },
      { "position": 66,  "color": "#FF9500", "title": "Out for delivery" },
      { "position": 100, "color": "#48484A", "title": "Delivered" }
    ],

    "pt_progress_tracker_icon": "https://i.imgur.com/6DavQwg.jpg",
    "pt_progress_start_icon": "https://i.imgur.com/6DavQwg.jpg",
    "pt_progress_end_icon": "https://i.imgur.com/6DavQwg.jpg",

    "pt_chip_type": "text",
    "pt_chip_text": "12 min",

    "pt_promote": "true",

    "wzrk_dl": "https://clevertap.com/order/A1234",
    "wzrk_acts": [
      { "id": "track",   "l": "Track order", "dl": "https://clevertap.com/track",   "ac": false },
      { "id": "support", "l": "Support",     "dl": "https://clevertap.com/support", "ac": true }
    ]
  }
}
```

## ⚠️ Scale rule (read this first)

The native Android 16 `ProgressStyle` has **no separate max** — the **total track length = the sum of
the `pt_progress_segments` lengths**, and `pt_progress_max` is **ignored** on that path. So:

- **`pt_progress` and every point `position` must be on the same scale as the segment-length sum.**
  Above, segments sum to `100`, so `pt_progress: "66"` and points `0 / 33 / 66 / 100` all line up and
  the tracker icon sits on the 3rd point.
- If segments summed to `3` (e.g. three `length: 1`) but `pt_progress` was `66`, the tracker would
  clamp to the **far right** — a common mistake.
- With **no** `pt_progress_segments`, the track defaults to `0..100`, so a percentage `pt_progress`
  works directly (plain-bar case).

## Wrapper keys (top level)

| Key | Meaning | Notes |
|---|---|---|
| `wzrk_la` | Marks a Live Update → routes into the LA pipeline | `"true"` |
| `wzrk_activityId` | Stable activity id; SDK derives the notification id from it | **Keep identical across updates** for in-place rendering |
| `wzrk_la_event` | `start` \| `update` \| `end` | `end` clears the ongoing flag + raises the *Ended* event |
| `wzrk_cid` | Target notification channel id | Must reference an existing high-importance channel |
| `wzrk_pn`, `wzrk_rnv`, `wzrk_id`, `wzrk_pid`, `wzrk_acct_id` | Transport/identity | **BE-owned.** `wzrk_rnv: "true"` enables the *Notification Viewed* event |

## Render keys (`data`)

| Key | Meaning | Notes |
|---|---|---|
| `pt_id` | `"pt_progress"` | Selects the progress template; wins over a factory |
| `pt_title`, `pt_msg` | Title / body | `nt` / `nm` accepted as fallbacks |
| `pt_small_icon_clr` | Accent color | `#RRGGBB` |
| `pt_progress` | Current progress on the track scale | See scale rule above |
| `pt_progress_max` | Max | Used by the **pre-16 fallback** only; ignored by native |
| `pt_styled_by_progress` | Tint bar by progress | 16+ native |
| `pt_progress_indeterminate` | Spinner bar | Honored **only** when there are no segments/points |
| `pt_progress_segments` | `[{length, color?}]` | Weighted connectors; lengths define the track total |
| `pt_progress_points` | `[{position, color?, title?}]` | Milestone dots on the same scale |
| `pt_progress_tracker_icon` / `_start_icon` / `_end_icon` | Image URLs | Square-cropped |
| `pt_chip_type` | `text` \| `timer` \| `countdown` \| `none` | Status-bar chip (16+) |
| `pt_chip_text` | Chip text | For `pt_chip_type=text` |
| `pt_when` + `pt_countdown` | Chip epoch-millis + count-down flag | For `pt_chip_type=timer`/`countdown` |
| `pt_promote` | Request promoted ongoing | `"false"` to opt out; 16+ only, and only a *request* |
| `wzrk_dl` | Tap deep link | |
| `wzrk_acts` | Up to 3 buttons `{id, l, dl, ac}` | `ac` = auto-cancel |

## Notes

- **Either/or — indicator:** send segments/points for the milestone tracker, **or** omit both for a
  plain bar (optionally `pt_progress_indeterminate: "true"` for a spinner). Not both.
- **Either/or — chip:** `text` uses `pt_chip_text`; `timer`/`countdown` use `pt_when` (+ `pt_countdown`).
- **String values on the wire:** FCM data messages are string maps. `pt_progress_segments`,
  `pt_progress_points` and `wzrk_acts` may be sent as real JSON arrays inside `data` (the SDK
  compact-stringifies them during surfacing) or pre-stringified — both work.
- **Version behavior:** native promoted `ProgressStyle` needs **Android 16 (API 36) + androidx.core
  ≥ 1.17.0**. On API 23–35 (or 16 with older core) it renders the ongoing RemoteViews fallback (no
  status-bar chip / promotion).
