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
    "pt_ico": "https://i.imgur.com/6DavQwg.jpg",

    "pt_progress": "66",
    "pt_styled_by_progress": "true",
    "pt_progress_indeterminate": "false",

    "pt_progress_segments": [
      { "length": 33, "color": "#4CAF50" },
      { "length": 33, "color": "#4CAF50" },
      { "length": 34, "color": "#48484A" }
    ],
    "pt_progress_points": [
      { "position": 33, "color": "#4CAF50", "title": "Preparing" },
      { "position": 66, "color": "#FF9500", "title": "On way" }
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
  Above, segments sum to `100`, so `pt_progress: "66"` and points `33 / 66` all line up and the
  tracker icon sits on the "On way" point.
- If segments summed to `3` (e.g. three `length: 1`) but `pt_progress` was `66`, the tracker would
  clamp to the **far right** — a common mistake.
- With **no** `pt_progress_segments`, the track defaults to `0..100`, so a percentage `pt_progress`
  works directly (plain-bar case, and points without segments).
- The fallback below Android 16 draws the **same** milestone track as native: same total, same
  limits (see *Limits*), so one payload looks the same on both tiers. `pt_progress_max` is **not** used by
  the milestone track on either tier; it only sets the max of the **plain bar below Android 16**.

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
| `pt_small_icon_clr` | Accent color | `#RRGGBB` (`pt_small_icon_clr_dark` for dark mode; `wzrk_color` as fallback). Below Android 12 it is also the default color of segments/points that have no `color` (see *Colors*) |
| `pt_ico` | Large icon URL | Optional, same key as the other templates. Set on the notification and drawn by the **system** in its own slot on the right, in the collapsed and the expanded view, on both tiers (native ProgressStyle on 16+; standard collapsed view and the expanded frame below 16). Not cropped |
| `pt_progress` | Current progress on the track scale | See scale rule above |
| `pt_progress_max` | Max of the plain bar | Used **only** by the plain determinate bar **below 16**. Ignored by native and by the milestone track on both tiers (its total = segment-length sum, else 100) |
| `pt_styled_by_progress` | Fade everything ahead of the progress | Milestone track on both tiers: segments/points after `pt_progress` are drawn at half opacity and thinner. The SDK default is `false` (the platform's own default is `true`); when `false`, only the tracker shows the progress, as on native. Not applied to the plain bar below 16 |
| `pt_progress_indeterminate` | Animated bar with no fixed fill | Both tiers. Honored **only** when there are no segments/points. The tracker icon is hidden in this mode (both tiers) |
| `pt_progress_segments` | `[{length, color?}]` | Drawn to scale: each segment's width follows its `length`; the lengths define the track total. **Max 10** (see *Limits*) |
| `pt_progress_points` | `[{position, color?, title?}]` | Milestone dots at their `position`, on the same scale. **Max 4**, and points at `0` or at the end are **not shown** (see *Limits*). `title` shows on the pre-16 fallback only |
| `pt_progress_tracker_icon` | Image URL | Square-cropped. Milestone track (both tiers): rides the track at the progress position. Plain bar below 16: fixed next to the title (a RemoteViews ProgressBar cannot carry an image). Hidden for an indeterminate bar |
| `pt_progress_start_icon` / `_end_icon` | Image URLs | Square-cropped. Shown at both ends of the bar for every bar type (milestone, plain, indeterminate), on both tiers |
| `pt_chip_type` | `text` \| `timer` \| `countdown` \| `none` | See *Chip* below |
| `pt_chip_text` | Chip text | Only for `pt_chip_type=text`. Keep it **≤ 7 characters** (e.g. `12 min`) |
| `pt_when` + `pt_countdown` | Epoch-millis + count-down flag | For `pt_chip_type=timer`/`countdown`. See *Timer / countdown* below |
| `pt_promote` | Request promoted ongoing | `"false"` to opt out; 16+ only, and only a *request* |
| `pt_dismiss` | Auto-dismiss after this many **seconds** | Optional, same key as the other templates (e.g. `"3600"`). The SDK passes it to Android's own timeout (`setTimeoutAfter`), so Android removes the card after that time even if the `end` push never arrives. No app code needed. Android 8+ only (no-op on 6–7). Zero, negative or invalid values are ignored. See *Auto-dismiss* below |
| `wzrk_dl` | Tap deep link | |
| `wzrk_acts` | Up to 3 buttons `{id, l, dl, ac}` | `ac` = auto-cancel |

## Chip

| | Android 16+ (native) | Android 6–15 (fallback) |
|---|---|---|
| `text` | Status-bar chip (`setShortCriticalText`), never inside the card. Max width **96dp**: under 7 characters shows in full; if less than half the text fits, the chip shows the icon only | Shown inside the expanded card, next to the title. Capped at **96dp**, one line, ellipsized |
| `timer` / `countdown` | Running timer in the status-bar chip **and** in the card header | Running timer in the notification header (there is no status-bar chip) |
| After `end` | No chip (the update is no longer ongoing/promoted) | No chip |

The status-bar chip only appears while the notification is **promoted** and **not in view** (shade
closed). With the shade open, look at the card instead.

## Timer / countdown

- `pt_when` is the epoch-millis the timer counts **to** (countdown, `pt_countdown: "true"`) or **from**
  (timer). For a countdown it must be in the future.
- **A countdown does not stop at zero.** Android keeps counting into negative time (`−0:01`, `−0:02`…)
  until the notification is posted again. The backend must send the next `update` (with a new
  `pt_when`) or the `end` event **before** the countdown reaches zero. `pt_dismiss` (see below) is a
  safety net that removes the card later, but it does not stop the countdown at zero.
- Android 16's status-bar chip hides itself once the countdown is no longer positive, but the card
  header can still show negative time.
- Android 6 (API 23) has no count-down chronometer, so a countdown counts up there.

## Limits (same on both tiers)

These are Android 16's own rules (`Notification.ProgressStyle`, API 36). The pre-16 fallback applies
the same rules, so a payload that breaks them still looks the same everywhere, just not as intended.

| You send | What is shown |
|---|---|
| A segment with `length` ≤ 0 | That segment is dropped |
| No segments (points only) | One full-width segment of length 100 in the default color |
| 1–10 segments | All of them, each at its own width |
| **More than 10 segments** | **One** segment of the same total. It keeps the color only if every segment had the same color; otherwise the default color |
| A point at `position` ≤ 0 or ≥ the total | Dropped (no dots at the very start or end) |
| **More than 4 points** | Only the **first 4** valid points |
| Two points with the same `position` | The **last** one |
| `pt_progress` below 0 or above the total | Clamped to the track |

**Safe payload:** 1–10 segments and at most 4 points strictly inside the track. So for an order
tracker, put the milestone dots *between* the start and the end (e.g. `33` and `66` on a 100 track),
not at `0` and `100`.

## Colors and light/dark mode

- The card follows the **system theme** on both tiers: light background with dark text in light mode,
  dark in dark mode. The fallback has no background or text colors of its own.
- A segment/point **without** `color` uses the theme accent:

  | Device | Default color |
  |---|---|
  | Android 16 | System primary color (from the wallpaper, Material You) |
  | Android 12–15 | System accent color (`system_accent1_600` light / `_200` dark), close to Android 16 |
  | Android 6–11 | `pt_small_icon_clr` (or `_dark` / `wzrk_color`), else grey `#A6A6A6` (no wallpaper colors on these versions) |

  Send an explicit `color` if every device must show the same color.
- **Contrast fix (both tiers):** every segment/point color is made lighter or darker (same hue) until
  it has at least **3:1 contrast** with the card. So `#FFFFFF` still shows on a light card and
  `#000000` on a dark card (both as a grey). The exact shade can differ slightly between tiers.
- Below 16 the track is an image made when the push is posted. If the user switches the theme while
  the notification is visible, the text updates at once and the track on the next `update`.

## Auto-dismiss (`pt_dismiss`)

- Value in **seconds**, as a string. The SDK multiplies it by 1000 and calls
  `NotificationCompat.Builder.setTimeoutAfter`, the platform's own timeout: Android removes the card.
- **Android 8 – 16:** works on both tiers (native ProgressStyle and the fallback use the same builder).
  **Android 6 – 7:** the platform has no timeout, so it is ignored there.
- Send it with every `start` / `update`, longer than the longest expected gap until the next update
  (for example the ETA plus a buffer). Send a short one with `end` (for example `"300"`) to clear the
  finished card a few minutes later.
- To check on a device (platform behaviour, not SDK code): the timer restarting on every update, an
  ongoing / promoted card being removed, and whether the removal also raises the "Live Activity"
  **Dismissed** event (Android may send the delete intent on a timeout too; the SDK cannot tell the two
  apart).

## Collapsed vs expanded

- **Android 16+, promoted** (`update` events, `pt_promote` not `"false"`, Live Updates allowed by the
  user): the card is **always expanded** and cannot be collapsed (platform rule).
- **Android 16+, not promoted** (`end` event, `pt_promote: "false"`, user turned Live Updates off)
  and **Android 6–15**: the collapsed view is the system's standard template — **small icon, title,
  time, message**. No bar, tracker, chip or buttons; those show only when expanded. Android 7–11 also
  show the app name in the header.

## Notes

- **Either/or — indicator:** send segments/points for the milestone tracker, **or** omit both for a
  plain bar (optionally `pt_progress_indeterminate: "true"` for an animated indeterminate bar). Not both.
- **Either/or — chip:** `text` uses `pt_chip_text`; `timer`/`countdown` use `pt_when` (+ `pt_countdown`).
- **String values on the wire:** FCM data messages are string maps. `pt_progress_segments`,
  `pt_progress_points` and `wzrk_acts` may be sent as real JSON arrays inside `data` (the SDK
  compact-stringifies them during surfacing) or pre-stringified — both work.
- **Version behavior:** native promoted `ProgressStyle` needs **Android 16 (API 36) + androidx.core
  ≥ 1.17.0**. On API 23–35 (or 16 with older core) it renders the ongoing fallback: a custom
  expanded view (RemoteViews) and the system's standard collapsed view. There is no promotion,
  status-bar chip or lock-screen placement on the fallback (16+ OS features).
- **Point titles:** shown under the dots on the pre-16 fallback only (native points have no text).
  Neighbouring titles keep a small gap and are shortened with `…` when there is no room.
- **Remaining fallback gaps:** the plain bar below 16 is the system ProgressBar (no
  `pt_styled_by_progress` fade, tracker next to the title, different height/color); the milestone
  track uses thinner lines without the 4dp gaps between segments; tiny segments are not stretched to
  the native 16dp minimum.
