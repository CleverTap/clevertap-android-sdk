# Live Updates demo (order tracking)

Demonstrates **CleverTap Live Updates** on Android using the notification-factory flow
(SDK-5612) — **Mode A**, where the client fully renders the notification. It shows a
progress-centric order tracker that updates **in place** as an order progresses, the Android
counterpart of an iOS Live Activity order tracker.

> For the **SDK-rendered** progress template (**Mode B**, `pt_id: pt_progress`), see
> [`live-updates-test/PT_PROGRESS_PAYLOAD.md`](live-updates-test/PT_PROGRESS_PAYLOAD.md).

Jira: [SDK-6039](https://wizrocket.atlassian.net/browse/SDK-6039) · Epic: [SDK-5253](https://wizrocket.atlassian.net/browse/SDK-5253)

## How it works

1. The backend / FCM sends a **Live Update data push** carrying `wzrk_la` and a nested `data`
   object with the client's render keys (and **no** `pt_id`).
2. The SDK sees the `wzrk_la` marker, surfaces the nested `data` to the top level, and — because
   there is no `pt_id` and a factory is registered — routes to the app's
   [`CustomNotificationFactory`](src/main/java/com/clevertap/demo/CustomNotificationFactory.kt)
   (an `ICleverTapNotificationFactory`), registered in `MyApplication` via
   `CleverTapAPI.setNotificationFactory(...)`.
3. The factory builds the notification: on **Android 16+** a native `NotificationCompat.ProgressStyle`
   (promoted, status-bar chip); below 16 a classic determinate progress-bar notification. It also
   wires a content intent so taps are click-tracked (see the factory's `contentIntent`).
4. The SDK **derives the notification id from `wzrk_activityId`**, so every subsequent push for the
   same order updates the **same** notification in place (no stacking), owns the impression, and
   raises the "Live Activity" lifecycle events (Started / Updated / Ended / Dismissed).

## Demo payload

| Key               | Purpose                                                       | Example           |
|-------------------|--------------------------------------------------------------|-------------------|
| `wzrk_la`         | Marks the push as a Live Update (routes to the factory)      | `"true"`          |
| `wzrk_activityId` | Stable order id — SDK derives the notification id from this  | `order_A1234`     |
| `wzrk_la_event`   | `start` \| `update` \| `end` (terminal → Delivered)          | `update`          |
| `wzrk_cid`        | Target notification channel id                               | `live_updates_channel` |
| `nt`              | Title                                                        | `Your order`      |
| `la_status`       | Status line                                                  | `Out for delivery`|
| `la_eta`          | ETA text (status-bar chip on 16+)                            | `12 min`          |
| `la_progress`     | Current progress, `0..la_progress_max`                       | `66`              |
| `la_progress_max` | Max (default `100`)                                          | `100`             |

> The `wzrk_*` keys are CleverTap's; the `nt` / `la_*` keys are client-defined for this demo and are
> read by `CustomNotificationFactory`. A real integration aligns them with whatever the backend nests
> inside `data`.

## Trigger it

Send successive pushes with the **same `wzrk_activityId`** and an increasing `la_progress`; each one
updates the single notification. The client render keys go inside the `data` object (the SDK surfaces
them to the top level):

```jsonc
// 1) Confirmed
{ "wzrk_la": "true", "wzrk_activityId": "order_A1234", "wzrk_la_event": "start",
  "wzrk_cid": "live_updates_channel",
  "data": { "nt": "Order #A1234", "la_status": "Order confirmed", "la_eta": "50 min",
            "la_progress": "0", "la_progress_max": "100" } }

// 2) Preparing
{ "wzrk_la": "true", "wzrk_activityId": "order_A1234", "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "data": { "nt": "Order #A1234", "la_status": "Preparing your order", "la_eta": "40 min",
            "la_progress": "33", "la_progress_max": "100" } }

// 3) Out for delivery
{ "wzrk_la": "true", "wzrk_activityId": "order_A1234", "wzrk_la_event": "update",
  "wzrk_cid": "live_updates_channel",
  "data": { "nt": "Order #A1234", "la_status": "Out for delivery", "la_eta": "12 min",
            "la_progress": "66", "la_progress_max": "100" } }

// 4) Delivered (terminal)
{ "wzrk_la": "true", "wzrk_activityId": "order_A1234", "wzrk_la_event": "end",
  "wzrk_cid": "live_updates_channel",
  "data": { "nt": "Order #A1234", "la_status": "Delivered — enjoy!", "la_eta": "0 min",
            "la_progress": "100", "la_progress_max": "100" } }
```

Each push must also carry the standard CleverTap push keys (`wzrk_pn`, `wzrk_id`, `wzrk_pid`,
`wzrk_acct_id`, `wzrk_rnv`, …) so the SDK processes it — these are added automatically when the
campaign is sent from the CleverTap dashboard.
