# Live Updates (Progress) — QA Test Cases

Exhaustive manual test plan for the **Live Updates / `pt_progress`** feature as exercised by the
sample app's **LIVE UPDATES** menu section. Hand-off ready.

---

## 0. Scope & concepts

Two render modes:

| Mode | What renders | How the sample triggers it |
|------|--------------|----------------------------|
| **Mode B — SDK renders** | SDK's `pt_progress` Push Template (`ProgressStyle`) | Rows 1–7 (`ProgressLiveUpdateDemo`) |
| **Mode A — client renders** | App's `ICleverTapNotificationFactory` (`CustomNotificationFactory`) | Row 8 (`CustomLiveUpdateDemo`) |

Two rendering tiers (chosen automatically by the OS version — **not** a menu option):

| Tier | Android | Presentation |
|------|---------|--------------|
| **Native** | **16+ (API 36, "Baklava")** | `Notification.ProgressStyle` — always-expanded, status-bar **chip**, promotable |
| **Fallback** | **< 16 (API 23–34)** | Segmented **RemoteViews** (dots + connectors) via `DecoratedCustomViewStyle` |

> **Every test case below must be run on BOTH tiers** — one device/emulator on **Android 16+** and one on **Android 13/14 (or any < 16)**. Expected results are given per tier where they differ.

**Demo mechanics:** each row runs a 4-step order sequence — *Placed → Cooking → On way → Delivered* — one step every **6 s** (~18 s total). All steps update the **same notification in place** (fixed id `778899` for Mode B; `wzrk_activityId` for Mode A). The final "Delivered" step sends `wzrk_la_event=end` → clears ongoing + enables auto-cancel.

---

## 1. Preconditions / setup

| # | Precondition |
|---|--------------|
| P1 | Sample app installed from this branch; a valid CleverTap account id/token configured. |
| P2 | **Notification permission granted** (Android 13+ runtime `POST_NOTIFICATIONS`). |
| P3 | On Android 16+: **"Live Updates / promoted notifications" enabled** for the app in system settings (the app declares `POST_PROMOTED_NOTIFICATIONS`). |
| P4 | Two test devices: **(A)** Android 16+ ; **(B)** Android 13/14 (or 11) for the fallback tier. |
| P5 | Mode A factory is registered at startup (`MyApplication` → `setNotificationFactory(CustomNotificationFactory())`). No action needed; just don't remove it. |
| P6 | Channel "Live Updates" (`live_updates_channel`) is created by the demos on first run; it must **not** be blocked by the user (except in the negative test that explicitly blocks it). |
| P7 | For analytics verification: access to the CleverTap dashboard (Events) for this account to confirm funnel events. |

**How to run a case:** open the app → scroll to **LIVE UPDATES** → tap the listed row → observe for ~20 s. Re-tap a row to restart (fresh ids each run).

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
| 19-6 | Progress: Indeterminate Bar | **Indeterminate bar** | Text | Promoted | Tracker | – | n/a |
| 19-7 | Custom Live Update – Mode A factory | Segmented (client) | Text (ETA, 16+) | Promoted while active | – | – | n/a |

\* Milestone labels (point titles: *Placed / Cooking / On way / Delivered*) render only in the **pre-16 expanded fallback**; they are a no-op on native 16+ (native points carry no text).

---

## 3. Functional test cases — Mode B (`pt_progress`)

For each case: **Result-16+** = native tier, **Result-<16** = fallback tier.

### TC-B01 — Baseline order tracker (Row 19-0)
- **Steps:** tap "Progress: Order Tracker". Watch 4 steps.
- **Result-16+:** native ProgressStyle notification, always expanded; a **status-bar chip** shows the ETA; a segmented track with 4 dots; dots/segments recolor green as steps complete; the active dot is orange; the content title "Order #A1234" and body update each step; tracker icon visible. Notification updates **in place** (no stacking).
- **Result-<16:** custom RemoteViews notification; segmented row of dots + connectors; **milestone labels** (Placed/Cooking/On way/Delivered) under the dots in the expanded view; progress recolors per step; updates in place.
- **Terminal:** after "Delivered" the notification is **no longer ongoing** and can be **swiped away**.

### TC-B02 — Actions + deep link (Row 19-1)
- **Steps:** tap "Progress: Actions + Deep Link". Observe buttons; tap "Track order"; restart and tap "Support"; restart and tap the notification body.
- **Result (both tiers):** two action buttons **"Track order"** and **"Support"** are shown (verify they appear on **< 16** too, not just 16+). Tapping **body** opens `https://clevertap.com` (deep link). Tapping **Track order** opens `.../track` (auto-cancel = false → notification stays). Tapping **Support** opens `.../support` (auto-cancel = true → notification dismisses).
- **Analytics:** a **Notification Clicked** event is recorded on body/button taps.

### TC-B03 — Countdown chip + promotion (Row 19-2)
- **Steps:** tap "Progress: Countdown Chip + Promoted".
- **Result-16+:** the status-bar **chip counts down** (live timer) toward the ETA; notification is **promoted** (elevated/among live updates).
- **Result-<16:** no status-bar chip (fallback has none); the notification still renders the tracker; timer behavior not applicable to the fallback chip.

### TC-B04 — Non-promoted (Row 19-3)
- **Steps:** tap "Progress: No Promotion". Compare against TC-B01/TC-B03.
- **Result-16+:** same tracker content but **not promoted** — **no status-bar chip**; appears as an ordinary ongoing notification. This is the promoted-vs-non-promoted A/B.
- **Result-<16:** visually same as baseline fallback (promotion is a 16+-only concept).

### TC-B05 — Start/End icons + styled-by-progress (Row 19-4)
- **Steps:** tap "Progress: Start/End Icons".
- **Result-16+:** native track shows a **start icon** and **end icon** at the track ends; `styled_by_progress` tints the track by progress.
- **Result-<16:** expanded fallback shows the **start/end icons** flanking the segmented row.

### TC-B06 — Plain determinate bar (Row 19-5)
- **Steps:** tap "Progress: Plain Bar – determinate".
- **Result-16+ & <16:** a **single determinate progress bar** (no dots/segments) that fills 0→100 across the 4 steps. Chip shows ETA on 16+.

### TC-B07 — Indeterminate bar (Row 19-6)
- **Steps:** tap "Progress: Indeterminate Bar".
- **Result-16+ & <16:** a **spinner-style indeterminate bar** (animated, no fixed fill); status text updates each step; the bar never shows a determinate fill.

### TC-B08 — In-place update (all Mode B rows)
- **Verify:** across all rows, the 4 steps replace **one** notification — the shade never accumulates 4 separate notifications for a single run.

### TC-B09 — Terminal lifecycle / dismissible (all Mode B rows)
- **Verify:** while stepping, the notification is **ongoing** (not swipeable on < Android 14). After "Delivered" (end), it becomes **swipeable/auto-cancel** on all versions.

### TC-B10 — Tap does NOT cancel an ongoing tracker (regression)
- **Steps:** on **Android 12+**, start Row 19-0; before "Delivered", **tap the notification body**.
- **Result:** the app opens, but the **ongoing tracker is NOT dismissed**; the next step still updates the same notification. (Guards the `NotificationUtils` action-scope fix.)

---

## 4. Functional test cases — Mode A (custom factory)

### TC-A01 — Mode A order tracker (Row 19-7)
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

Run the full section 3–5 on each device and tick:

| Case | Android 16+ (native) | Android 13/14 (fallback) | Android 10/11 (fallback) |
|------|:---:|:---:|:---:|
| TC-B01 … TC-B10 | ☐ | ☐ | ☐ |
| TC-A01 … TC-A03 | ☐ | ☐ | ☐ |
| TC-I01 … TC-I03 | ☐ | ☐ | ☐ |
| TC-N02 (buttons < 31) | n/a | n/a | ☐ |

---

## 9. Known limitations to note (not bugs)

- **Milestone labels (point titles)** show only on the pre-16 fallback; native 16+ points have no text.
- **Countdown chip** and **promotion** are 16+-only; on the fallback tier there is no status-bar chip.
- **Mode A** is fully client-drawn — its exact look is defined by `CustomNotificationFactory`, not the SDK; it demonstrates one representative configuration (promoted + ETA chip).
