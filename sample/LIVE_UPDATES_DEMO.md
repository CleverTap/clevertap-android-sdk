# Live Updates demo (order tracking)

Demonstrates **CleverTap Live Updates** on Android using the notification-factory flow
(SDK-5612), rendering an order-tracking notification that updates **in place** as an order
progresses — the Android counterpart of an iOS Live Activity order tracker.

Jira: [SDK-6039](https://wizrocket.atlassian.net/browse/SDK-6039) · Epic: [SDK-5253](https://wizrocket.atlassian.net/browse/SDK-5253)

## How it works

1. The backend / FCM sends a **Live Update data push**.
2. The SDK sees the `wzrk_la` marker and routes the payload to the app's
   [`CustomNotificationFactory`](src/main/java/com/clevertap/demo/CustomNotificationFactory.kt)
   (an `ICleverTapNotificationFactory`), registered in `MyApplication` via
   `CleverTapAPI.setNotificationFactory(...)`.
3. The factory builds the order-tracking notification (custom `RemoteViews`,
   [`notification_live_order.xml`](src/main/res/layout/notification_live_order.xml)) and returns it.
4. The SDK renders it. It **derives the notification id from `cleverTapActivityId`**, so every
   subsequent push for the same order updates the **same** notification in place (no stacking),
   and raises the "Live Activity" lifecycle events (Started / Updated / Ended / Dismissed).

## Demo payload

| Key                   | Purpose                                                        | Example          |
|-----------------------|---------------------------------------------------------------|------------------|
| `wzrk_la`             | Marks the push as a Live Update (routes to the factory)       | `"true"`         |
| `cleverTapActivityId` | Stable order id — SDK derives the notification id from this   | `order_123456`   |
| `wzrk_la_event`       | `update` (default) or `end` (terminal → Delivered)            | `update`         |
| `la_store`            | Store name (title)                                            | `Pizza place`    |
| `la_items`            | Item summary                                                  | `2 Pizza`        |
| `la_order`            | Order id label                                                | `Order #123456`  |
| `la_eta`              | ETA value                                                     | `50 min`         |
| `la_status`           | Status line                                                   | `Order confirmed`|
| `la_step`             | Progress step: `0`=Placed, `1`=Preparing, `2`=En Route, `3`=Delivered | `0`     |

> The `wzrk_*` keys are CleverTap's; the `la_*` keys are client-defined for this demo. A real
> integration aligns the `la_*` keys with whatever the backend sends.

## Trigger it

Send successive pushes with the **same `cleverTapActivityId`** and an increasing `la_step`; each one
updates the single notification. Example progression (raw FCM data payload):

```jsonc
// 1) Placed
{ "wzrk_la": "true", "cleverTapActivityId": "order_123456", "wzrk_la_event": "update",
  "la_store": "Pizza place", "la_items": "2 Pizza", "la_order": "Order #123456",
  "la_eta": "50 min", "la_status": "Order confirmed", "la_step": "0" }

// 2) Preparing
{ "wzrk_la": "true", "cleverTapActivityId": "order_123456", "wzrk_la_event": "update",
  "la_eta": "40 min", "la_status": "Preparing your order", "la_step": "1", /* ...store/items/order */ }

// 3) En Route
{ "wzrk_la": "true", "cleverTapActivityId": "order_123456", "wzrk_la_event": "update",
  "la_eta": "12 min", "la_status": "Out for delivery", "la_step": "2", /* ... */ }

// 4) Delivered (terminal)
{ "wzrk_la": "true", "cleverTapActivityId": "order_123456", "wzrk_la_event": "end",
  "la_eta": "0 min", "la_status": "Delivered — enjoy!", "la_step": "3", /* ... */ }
```

Each push must also carry the standard CleverTap push keys (`wzrk_pn`, `wzrk_id`, `wzrk_pid`,
`wzrk_acct_id`, …) so the SDK processes it — these are added automatically when the campaign is
sent from the CleverTap dashboard.
