# Live Updates (Progress) — QA Test Cases

Exhaustive manual test plan for the **Live Updates / `pt_progress`** feature as exercised by the
sample app's **LIVE UPDATES** menu section. Hand-off ready.

---

## 0. Scope & concepts

Two render modes:

| Mode | What renders | How the sample triggers it |
|------|--------------|----------------------------|
| **Mode B — SDK renders** | SDK's `pt_progress` Push Template (`ProgressStyle`) | Rows 19-0 … 19-14 (`ProgressLiveUpdateDemo`) |
| **Mode A — client renders** | App's `ICleverTapNotificationFactory` (`CustomNotificationFactory`) | Row 19-15 (`CustomLiveUpdateDemo`) |

Two rendering tiers (chosen automatically by the OS version — **not** a menu option):

| Tier | Android | Presentation |
|------|---------|--------------|
| **Native** | **16+ (API 36, "Baklava")** | `Notification.ProgressStyle` — status-bar **chip**, promotable; always expanded while promoted |
| **Fallback** | **< 16 (API 23–35)** | Custom expanded view (**RemoteViews**: a milestone track drawn as an image with native's rules, or a plain bar) via `DecoratedCustomViewStyle`; follows the system light/dark theme; the **collapsed view is the system's standard template** |

> **Every test case below must be run on BOTH tiers** — one device/emulator on **Android 16+** and one on **Android 15 or lower**. Expected results are given per tier where they differ.

**Demo mechanics:** each row runs a 4-step order sequence — *Placed → Cooking → On way → Delivered* — one step every **6 s** (~18 s total). All steps update the **same notification in place** (fixed id `778899` for Mode B; `wzrk_activityId` for Mode A). The final "Delivered" step sends `wzrk_la_event=end` → clears ongoing + enables auto-cancel.

---

## 1. Preconditions / setup

| # | Precondition |
|---|--------------|
| P1 | Sample app installed from this branch; a valid CleverTap account id/token configured. |
| P2 | **Notification permission granted** (Android 13+ runtime `POST_NOTIFICATIONS`). |
| P3 | On Android 16+: **"Live Updates / promoted notifications" enabled** for the app in system settings (the app declares `POST_PROMOTED_NOTIFICATIONS`). |
| P4 | Test devices: **(A)** Android 16+ ; **(B)** Android 12–15 and **(C)** Android 7–11 for the fallback tier (7–11 have a different collapsed header). |
| P5 | Mode A factory is registered at startup (`MyApplication` → `setNotificationFactory(CustomNotificationFactory())`). No action needed; just don't remove it. |
| P6 | Channel "Live Updates" (`live_updates_channel`) is created by the demos on first run; it must **not** be blocked by the user (except in the negative test that explicitly blocks it). |
| P7 | For analytics verification: access to the CleverTap dashboard (Events) for this account to confirm funnel events. |

**How to run a case:** open the app → scroll to **LIVE UPDATES** → tap the listed row → observe for ~20 s. Re-tap a row to restart (fresh ids each run).

**Cases marked [code]** (and every other payload combination) can be run without code changes: tap **Paste Live Update payload (QA)** (row 19-16), paste a payload from [`QA_PAYLOADS.md`](QA_PAYLOADS.md) and tap **Send**. It goes through the same path as a real FCM push (`wzrk_la` routing, `data` surfacing, in-place updates by `wzrk_activityId`).

---

## 2. Coverage matrix (menu row → features)

| Row | Menu label | Indicator | Chip | Promotion | Icons | Actions | Milestone labels* |
|-----|-----------|-----------|------|-----------|-------|---------|-------------------|
| 19-0 | Progress: Order Tracker | Segmented | Text (ETA) | Promoted (default) | Tracker | – | Yes |
| 19-1 | Progress: Actions + Deep Link | Segmented | Text | Promoted | Tracker | 2 buttons + tap DL | Yes |
| 19-2 | Progress: Countdown Chip + Promoted | Segmented | **Countdown/timer** | **Promoted (explicit)** | Tracker | – | Yes |
| 19-3 | Progress: No Promotion | Segmented | Text | **Non-promoted** | Tracker | – | Yes |
| 19-4 | Progress: Start/End Icons | Segmented | Text | Promoted | Tracker + **Start/End** + styled-by-progress | – | Yes |
| 19-5 | Progress: Plain Bar – determinate | **Plain determinate bar** | Text | Promoted | Tracker | – | n/a |
| 19-6 | Progress: Plain Bar + Start/End Icons | **Plain determinate bar** | Text | Promoted | Tracker + **Start/End** | – | n/a |
| 19-7 | Progress: Indeterminate Bar | **Indeterminate bar** | Text | Promoted | Tracker | – | n/a |
| 19-8 | Progress: Indeterminate Bar + Start/End Icons | **Indeterminate bar** | Text | Promoted | **Start/End** (tracker hidden) | – | n/a |
| 19-9 | Progress: Unequal Segments 10/80/10 | Segmented, **unequal lengths** | Text | Promoted | Tracker | – | Yes |
| 19-10 | Progress: On Track - tracker + styled by progress | Segmented, progress **mid-segment** | Text | Promoted | Tracker + **styled-by-progress** | – | Yes |
| 19-11 | Progress: On Track - no tracker, fade only | Segmented, progress mid-segment | Text | Promoted | **No tracker** + styled-by-progress | – | Yes |
| 19-12 | Progress: Light/Dark Colors - default, white, black | Segmented, **uncolored / white / black** | Text | Promoted | Tracker + styled-by-progress | – | Yes |
| 19-13 | Progress: Edge Points 0/100 - ends hidden | Segmented, points at **0 / 33 / 66 / 100** | – | Promoted | Tracker + styled-by-progress | – | Yes (middle 2 only) |
| 19-14 | Progress: Edge Points 5/95 - control, all 4 dots | Segmented, points at **5 / 33 / 66 / 95** | – | Promoted | Tracker + styled-by-progress | – | Yes (all 4) |
| 19-15 | Custom Live Update – Mode A factory | Segmented (client) | Text (ETA, 16+) | Promoted while active | – | – | n/a |
| 19-16 | Paste Live Update payload (QA) | Any (from the pasted payload) | Any | Any | Any | Any | Any |

\* Milestone labels (point titles) render only in the **pre-16 expanded fallback**; they are a no-op on native 16+ (native points carry no text). Points at the very start or end of the track are **not drawn on either tier** (native rule), so rows 19-0 … 19-4 show only *Cooking* and *On way*, and row 19-9 only the points at 10 and 90.

---

## 3. Functional test cases — Mode B (`pt_progress`)

For each case: **Result-16+** = native tier, **Result-<16** = fallback tier.

### TC-B01 — Baseline order tracker (Row 19-0)
- **Steps:** tap "Progress: Order Tracker". Watch 4 steps.
- **Result-16+:** native ProgressStyle notification, always expanded while promoted; with the shade **closed**, a **status-bar chip** shows the ETA (`50 min` → `40 min` → `12 min`); a segmented track with **2 dots** (33 and 66; the points at 0 and 100 are dropped by the platform); segments/dots recolor green as steps complete; the active dot is orange; the content title "Order #A1234" and body update each step; the tracker icon rides the track at the progress. Notification updates **in place** (no stacking).
- **Result-<16:** expanded: custom RemoteViews card in the system theme; the **same track as 16+**: 3 segments, 2 dots at the same positions, **milestone labels** *Cooking* / *On way* under the dots (not overlapping); the **tracker rides the track** at the progress (not next to the title); ETA chip text next to the title (never cuts the title); progress recolors per step; updates in place.
- **Terminal:** after "Delivered" the notification is **no longer ongoing** and can be **swiped away**; the chip is gone on both tiers.

### TC-B02 — Actions + deep link (Row 19-1)
- **Steps:** tap "Progress: Actions + Deep Link". Observe buttons; tap "Track order"; restart and tap "Support"; restart and tap the notification body.
- **Result (both tiers):** two action buttons **"Track order"** and **"Support"** are shown (verify they appear on **< 16** too, not just 16+). Tapping **body** opens `https://clevertap.com` (deep link). Tapping **Track order** opens `.../track` (auto-cancel = false → notification stays). Tapping **Support** opens `.../support` (auto-cancel = true → notification dismisses).
- **Analytics:** a **Notification Clicked** event is recorded on body/button taps.

### TC-B03 — Countdown chip + promotion (Row 19-2)
- **Steps:** tap "Progress: Countdown Chip + Promoted".
- **Result-16+:** the status-bar **chip counts down** (live timer) toward the ETA, and the card header shows the same running timer; notification is **promoted** (elevated/among live updates).
- **Result-<16:** no status-bar chip (fallback has none); the **notification header shows the running countdown** instead of the post time (collapsed and expanded). On Android 6 the timer counts up (no count-down chronometer on API 23).

### TC-B04 — Non-promoted (Row 19-3)
- **Steps:** tap "Progress: No Promotion". Compare against TC-B01/TC-B03.
- **Result-16+:** same tracker content but **not promoted** — **no status-bar chip**, not on the lock screen; appears as an ordinary ongoing notification that **can be collapsed** (collapsed = icon, title, time, message). This is the promoted-vs-non-promoted A/B.
- **Result-<16:** visually same as baseline fallback (promotion is a 16+-only concept).

### TC-B05 — Start/End icons + styled-by-progress (Row 19-4)
- **Steps:** tap "Progress: Start/End Icons".
- **Result-16+:** native track shows a **start icon** and **end icon** at the track ends; `styled_by_progress` fades everything ahead of the progress.
- **Result-<16:** expanded fallback shows the **start icon (store)** and **end icon (house)** flanking the track; the tracker rides the track; everything ahead of the progress is **faded** (half opacity, thinner), like 16+.

### TC-B06 — Plain determinate bar (Row 19-5)
- **Steps:** tap "Progress: Plain Bar – determinate".
- **Result-16+ & <16:** a **single determinate progress bar** (no dots/segments) that fills 0→100 across the 4 steps. No start/end icons. ETA chip: status bar on 16+, inside the card on <16.

### TC-B06B — Plain bar + start/end icons (Row 19-6)
- **Steps:** tap "Progress: Plain Bar + Start/End Icons".
- **Result-16+ & <16:** the plain determinate bar has the **start icon (store)** on its left and the **end icon (house)** on its right. On 16+ the tracker rides the bar; on <16 the tracker sits next to the title.

### TC-B07 — Indeterminate bar (Row 19-7)
- **Steps:** tap "Progress: Indeterminate Bar".
- **Result-16+ & <16:** an **animated indeterminate bar** (no fixed fill); status text updates each step; the bar never shows a determinate fill and does not jump between steps. The **tracker icon is hidden** on both tiers.

### TC-B07B — Indeterminate bar + start/end icons (Row 19-8)
- **Steps:** tap "Progress: Indeterminate Bar + Start/End Icons".
- **Result-16+ & <16:** the animated indeterminate bar has the **start icon (store)** on its left and the **end icon (house)** on its right. The **tracker is hidden** on both (there is no progress position to place it at).

### TC-B07C — Unequal segments (Row 19-9)
- **Steps:** tap "Progress: Unequal Segments 10/80/10".
- **Result-16+ & <16:** the 3 segments are drawn **10% / 80% / 10%** of the track width (not equal); dots at **10** and **90** (the points at 0 and 100 are not drawn); the tracker jumps 0 → 10 → 90 → 100. Below 16 the labels under the two dots never overlap.

### TC-B07D — Progress on the track, with tracker (Row 19-10)
- **Steps:** tap "Progress: On Track - tracker + styled by progress". Segments are fixed (blue / orange / green); only `pt_progress` moves: **10 → 45 → 80 → 100**.
- **Result-16+ & <16:** the tracker sits **on the track** at the progress, inside a segment (not on a dot); everything **after** the tracker is faded (half opacity, thinner); dots after the progress are faded too; at 100 nothing is faded. Below 16 the tracker is **not** also shown next to the title.

### TC-B07E — Progress on the track, no tracker (Row 19-11)
- **Steps:** tap "Progress: On Track - no tracker, fade only".
- **Result-16+ & <16:** no tracker; the progress is visible as the point where the track turns faded, with a **small gap** just before it.

### TC-B07F — Light / dark colors (Row 19-12)
- **Steps:** run the row in **light mode**, then switch the system to **dark mode** and run it again. (Android 10+; earlier versions have no system dark mode.)
- **Result (both tiers, both modes):** the card background and text follow the system theme (no dark card in light mode below 16). The **uncolored** first segment and "Default" dot use the theme accent: wallpaper/accent color on Android 12+, grey on Android 7–11 (different from 16+; expected). The **white** segment/dot is still visible on a light card and the **black** one on a dark card (both shown as grey: contrast fix).
- **Note (<16):** if the theme is switched while the notification is on screen, the text changes at once and the track on the next step.

### TC-B07G — Edge points at 0 and 100 are not drawn (Row 19-13)
- **Steps:** tap "Progress: Edge Points 0/100 - ends hidden". Fixed 3-color track (33 / 33 / 34), `pt_progress` stays at **50**. Points: **0** "Placed" (pink), **33** "Cooking", **66** "On way", **100** "Delivered" (pink).
- **Result-16+:** only **2 dots**, at 33 and 66. **No pink dot** at either end of the track. No titles (native points have no text).
- **Result-<16:** the same 2 dots, with only **"Cooking"** and **"On way"** under them. "Placed" and "Delivered" are not shown.
- **Fail if:** a pink dot or the "Placed" / "Delivered" title shows on any tier (the fallback no longer matches native, `Notification.java` drops points at `<= 0` and `>= total`).

### TC-B07H — Edge points control at 5 and 95 (Row 19-14)
- **Steps:** tap "Progress: Edge Points 5/95 - control, all 4 dots". Same track and progress as TC-B07G; only the two pink points move to **5** and **95**.
- **Result-16+:** **4 dots**: pink at 5, then 33, 66, pink at 95.
- **Result-<16:** the same 4 dots with all 4 titles; "Placed" and "Delivered" do not overlap their neighbours.
- **Why:** confirms that TC-B07G hides the ends only because of their position, not because of the color or title.

### TC-B08 — In-place update (all Mode B rows)
- **Verify:** across all rows, the 4 steps replace **one** notification — the shade never accumulates 4 separate notifications for a single run.

### TC-B09 — Terminal lifecycle / dismissible (all Mode B rows)
- **Verify:** while stepping, the notification is **ongoing** (not swipeable on < Android 14). After "Delivered" (end), it becomes **swipeable/auto-cancel** on all versions.

### TC-B10 — Tap does NOT cancel an ongoing tracker (regression)
- **Steps:** on **Android 12+**, start Row 19-0; before "Delivered", **tap the notification body**.
- **Result:** the app opens, but the **ongoing tracker is NOT dismissed**; the next step still updates the same notification. (Guards the `NotificationUtils` action-scope fix.)

### TC-B11 — Collapsed view (all Mode B rows)
- **Steps:** run any row; collapse the notification (on 16+ use Row 19-3 "No Promotion", or wait for "Delivered" — a promoted card cannot be collapsed).
- **Result (all versions):** the collapsed view is the system's standard notification — **small icon, title, time, message** — with no bar, tracker, chip or buttons. Android 7–11 also show the app name in the header. Nothing is cut off and it follows the system light/dark theme.

---

## 4. Functional test cases — Mode A (custom factory)

### TC-A01 — Mode A order tracker (Row 19-15)
- **Steps:** tap "Custom Live Update – Mode A factory".
- **Result-16+:** a **client-built** native `ProgressStyle` notification (green done / gray pending segments computed from progress); **chip** shows the ETA (`setShortCriticalText`); **promoted** while active; updates in place on the same `wzrk_activityId`.
- **Result-<16:** the factory's **classic determinate progress-bar** notification (sub-text = status); updates in place.
- **Terminal:** "Delivered" step (`wzrk_la_event=end`) → not ongoing, swipeable.

### TC-A02 — Mode A click attribution
- **Steps:** tap the Mode A notification body.
- **Result:** app opens; **Notification Clicked** recorded (factory sets a content intent + `MyApplication` lifecycle callback attributes it).

### TC-A03 — Mode A in-place identity
- **Verify:** the 4 Mode A steps update **one** notification (not 4), keyed off `wzrk_activityId`.

---

## 5. Interaction & lifecycle test cases (both modes)

### TC-I01 — Swipe-dismiss raises "Dismissed"
- **Steps:** start any row; before end, **swipe the notification away** (on Android 14+ where ongoing is swipeable, or after it becomes dismissible).
- **Result:** a **Live Activity → Dismissed** lifecycle event is recorded (SDK delete intent).

### TC-I02 — Lifecycle funnel (Started / Updated / Ended)
- **Steps:** run a full sequence for a row.
- **Result (dashboard):** exactly **one Started** (first step), **Updated** for middle steps, **one Ended** (final step). Plus **Notification Viewed** per rendered step.

### TC-I03 — Re-tap restarts cleanly
- **Steps:** tap the same row twice in quick succession.
- **Result:** no crash; the notification reflects the newest run (fresh `wzrk_id`/`wzrk_pid` per run). *(Known: Mode B uses a fixed notif id, so a second run replaces the first.)*

---

## 6. Negative / edge cases

Some require a modified payload (not a menu row) — marked **[code]**. Menu-runnable ones are marked **[menu]**.

### TC-N01 — Malformed `pt_progress_max` negative **[code]**
- **Setup:** send a `pt_progress` push with `pt_progress_max="-5"` and no segments/points.
- **Result:** notification still renders (degrades to a clamped/empty bar); **no crash**, push is **not dropped**. (Guards the negative-max `coerceAtLeast(1)` fix.)

### TC-N02 — Action buttons on API 23–30 **[menu]**
- **Steps:** run Row 19-1 on an **Android 10/11** device.
- **Result:** the **"Track order" / "Support" buttons ARE present** (regression guard — they used to be dropped below API 31).

### TC-N03 — Title-only Live Update (no message) **[code]**
- **Setup:** `pt_progress` push with `nt` + `pt_progress` but **no `nm`/`pt_msg`**, routed through the SDK's FCM/template handler.
- **Result:** it **renders** (title + progress) and is **not** dropped as a silent push.

### TC-N04 — Mode A with **no factory registered** **[code]**
- **Setup:** temporarily remove `setNotificationFactory(...)` from `MyApplication`; send a Mode A push (`wzrk_la=true`, no `pt_id`, no `nm`).
- **Result:** the push is **dropped** (silent) — **no blank/app-name notification**, and **no** false Started/Viewed impression. (Guards the renderer-gated empty-message fix.)

### TC-N05 — User-blocked channel **[menu+settings]**
- **Setup:** block the "Live Updates" channel in system settings; run any row.
- **Result:** nothing is posted; no lifecycle/Viewed event for the blocked render.

### TC-N06 — Mode A missing `wzrk_activityId` **[code]**
- **Setup:** Mode A push without `wzrk_activityId`.
- **Result:** notification still shows (id falls back to `wzrk_pid`, then a time-based id); in-place updates won't coalesce — that's expected/logged.

### TC-N07 — androidx.core < 1.17.0 at runtime **[env]**
- **Setup:** a host build shipping androidx.core **< 1.17.0** on an Android 16+ device.
- **Result:** the native path degrades to the RemoteViews fallback (no crash) — the 16+ APIs are reflective and R8-kept.

### TC-N08 — Chip text with a non-text chip type **[code]**
- **Setup:** send `pt_chip_type: "none"` (then `"countdown"`, then no `pt_chip_type`) together with `pt_chip_text: "12 min"`.
- **Result:** the chip text is **not** shown on either tier — `pt_chip_text` is used only for `pt_chip_type=text`.

### TC-N09 — Long chip text **[code]**
- **Setup:** `pt_chip_type: "text"`, `pt_chip_text: "Out for delivery"`.
- **Result-16+:** the status-bar chip shows the **icon only** (text too long for 96dp).
- **Result-<16:** the chip is capped at 96dp and ellipsized; the **title is never cut**.

### TC-N11 — Segment / point limits **[code]**
Send a milestone payload (`pt_progress_segments` / `pt_progress_points`) for each case; ready-made payloads are cases L1 to L8 in `QA_PAYLOADS.md`. Expected on **both tiers**:

| Setup | Result |
|---|---|
| 11 segments, different colors | **One** full-width segment in the default color |
| 11 segments, all `#FF0000` | **One** full-width red segment |
| 10 segments | All 10, each at its own width |
| 6 points at 10, 20, 30, 40, 50, 60 | Only the dots at **10, 20, 30, 40** |
| Points at 0, 50, 100 | Only the dot at **50** |
| Two points at 50 (red, then green) | One **green** dot |
| Points only, no segments | A full default-colored track (total 100), dots at their positions |
| A segment with `length: 0` | Dropped; the other segments fill the track |

### TC-N12 — Large icon (`pt_ico`) **[code]**
- **Setup:** add `"pt_ico": "<image URL>"` to any milestone or plain-bar payload; run it once with `pt_promote` `"true"` and once with `"false"`.
- **Result (both tiers):** the image shows on the **right** of the card, drawn by the system, in the collapsed view and in the expanded view (with the bar). Below 16 the expanded card's title, chip and track end before the icon (no overlap). Without `pt_ico` there is no large icon.

### TC-N13 — Auto-dismiss (`pt_dismiss`) **[code]**
- **Setup:** send an ongoing update with `"pt_dismiss": "60"` and send nothing after it. Then repeat, sending a second update (same `wzrk_activityId`, `"pt_dismiss": "60"`) after 40 s.
- **Result (Android 8+, both tiers):** with no further push the card disappears about 60 s after the update, even though it is ongoing (and promoted on 16+). With the second update it disappears about 60 s after the **second** update (the timer restarts). Without `pt_dismiss` it never disappears on its own.
- **Result (Android 6–7):** the card stays; the platform has no timeout.
- **Also note:** whether a "Live Activity" **Dismissed** event is recorded when the card times out.

### TC-N10 — Countdown past zero **[code]**
- **Setup:** `pt_chip_type: "countdown"`, `pt_countdown: "true"`, `pt_when` = now + 10 s; send no further update.
- **Result (both tiers):** the timer counts down to 0:00 and then **continues into negative time** (`−0:01`…). Expected platform behavior — the backend must send the next update or `end` before zero. On 16+ the status-bar chip hides once the countdown is no longer positive.

---

## 7. Analytics verification checklist

For each mode, confirm on the dashboard:

| Event | When | Notes |
|-------|------|-------|
| **Live Activity** (state=Started) | First step of a run | Exactly once. |
| **Live Activity** (state=Updated) | Each middle step | |
| **Live Activity** (state=Ended) | Final "Delivered" step | Exactly once. |
| **Live Activity** (state=Dismissed) | Swipe-away | Via delete intent (TC-I01). |
| **Notification Viewed** | Each rendered step | Both modes. |
| **Notification Clicked** | Body / action-button tap | TC-B02, TC-A02. |

---

## 8. Device/tier sign-off grid

Run sections 3–6 on each device and tick:

| Case | Android 16+ (native) | Android 12–15 (fallback) | Android 7–11 (fallback) |
|------|:---:|:---:|:---:|
| TC-B01 … TC-B11 | ☐ | ☐ | ☐ |
| TC-A01 … TC-A03 | ☐ | ☐ | ☐ |
| TC-I01 … TC-I03 | ☐ | ☐ | ☐ |
| TC-N02 (buttons < 31) | n/a | n/a | ☐ |
| TC-N08 … TC-N13 | ☐ | ☐ | ☐ |

---

## 9. Known limitations to note (not bugs)

- **Milestone labels (point titles)** show only on the pre-16 fallback; native 16+ points have no text.
- **Native limits apply on both tiers:** max 10 segments (more collapse into one), max 4 points, no points at the very start or end.
- **Status-bar chip, promotion and lock-screen placement** are 16+-only. On the fallback a text chip shows inside the card and a timer/countdown shows in the header.
- **Promoted 16+ cards are always expanded**; a collapsed view exists only when not promoted.
- **Tracker icon** rides the track on both tiers for the milestone track. For the **plain bar below 16** it still sits next to the title (a RemoteViews ProgressBar cannot carry an image).
- **`pt_styled_by_progress`** works on the fallback milestone track, but not on the fallback plain bar.
- **Default color** of an uncolored segment/point differs by version: wallpaper color on 16+ (and close on 12–15), small-icon color or grey on 7–11.
- **Fallback track look:** thinner lines, no 4dp gaps between segments, round dots; tiny segments are not stretched to native's 16dp minimum.
- **A countdown keeps counting into negative time** after zero on both tiers until the next update arrives.
- **Mode A** is fully client-drawn — its exact look is defined by `CustomNotificationFactory`, not the SDK; it demonstrates one representative configuration (promoted + ETA chip).
