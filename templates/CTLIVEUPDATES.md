# Live Updates by CleverTap

A **Live Update** is a single, ongoing notification that is refreshed in place to show real-time information — an order on its way, a ride approaching, a live match score. It is the Android counterpart of an iOS Live Activity, and CleverTap drives both from the same campaign.

This guide walks through the end-to-end flow, the simplest way to show one on Android, and how to take full control of the notification when you need to.

# Table of contents

- [The end-to-end flow](#the-end-to-end-flow)
- [Showing a live update](#showing-a-live-update)
  - [The progress-centric notification (recommended)](#the-progress-centric-notification-recommended)
  - [Custom handling — build your own notification](#custom-handling--build-your-own-notification)
- [Lifecycle and analytics](#lifecycle-and-analytics)
- [Ending and dismissing](#ending-and-dismissing)
- [Requirements](#requirements)

# The end-to-end flow

A Live Update runs as a short sequence of pushes — a start, any number of updates, and an end — that all drive **one** notification:

1. **You create a Live Update campaign** on the CleverTap dashboard and send it to your users.
2. **CleverTap delivers it as an ordinary FCM push.** The SDK recognises the live-update marker on the push and takes over rendering — there is nothing to register and no token to manage (unlike iOS, Android has no push-to-start step).
3. **The SDK shows the notification** and keeps it ongoing.
4. **Each follow-up push refreshes the same notification in place.** Every push for the activity carries a stable id, so an update replaces the existing notification instead of stacking a new one.
5. **An end push finishes it.** The notification becomes dismissable and the SDK stops treating it as live.
6. **Throughout, the SDK raises a `Live Activity` event** for each transition (Started / Updated / Ended / Dismissed), so the campaign's funnel is tracked automatically.

You control only what the notification looks like — either by letting the SDK render the built-in [progress-centric notification](#the-progress-centric-notification-recommended), or by [building it yourself](#custom-handling--build-your-own-notification).

# Showing a live update

## The progress-centric notification (recommended)

On Android, the natural counterpart to an iOS Live Activity is a **progress-centric notification** — a live, ongoing notification built around a progress indicator (a delivery advancing through stages, a countdown, a score).

On **Android 16 (API 36) and above**, the SDK renders this as a **native, promoted live notification** with essentially no work on your side — you add one manifest permission and `androidx.core` 1.17.0 or above (see [Requirements](#requirements)) and send the content in the campaign. It comes with a progress track, milestone points, a status-bar chip, and automatic promotion to an ongoing "live" notification; the SDK handles all of it.

On **earlier Android versions**, the SDK automatically falls back to a custom notification that mirrors the same look, so the very same campaign works across devices. The only difference is that it cannot be *promoted* (promotion is an Android 16 OS capability); it is still shown as an ongoing live update.

Because the dashboard builds the payload for you, sending one is a content exercise, not a coding one. Under the hood the live update simply carries the progress content; a representative payload looks like this:

```json
{
  "wzrk_la": "true",
  "wzrk_activityId": "order-8842",
  "wzrk_la_event": "update",
  "data": {
    "pt_id": "pt_progress",
    "pt_title": "Order on its way",
    "pt_msg": "Arriving in about 10 minutes",
    "pt_progress": "2",
    "pt_styled_by_progress": "true",
    "pt_progress_segments": "[{\"length\":1},{\"length\":1},{\"length\":1}]",
    "pt_progress_points": "[{\"position\":1,\"title\":\"Packed\"},{\"position\":2,\"title\":\"Shipped\"}]",
    "pt_chip_type": "text",
    "pt_chip_text": "On the way"
  }
}
```

Send the next update with a higher progress value, and a final `end` event when it is done — the same notification advances in place and then closes. The full list of content keys (segments, milestone points, tracker icon, chip, countdown, and so on) is in [Push Templates › Progress Template](CTPUSHTEMPLATES.md#progress-template-1).

To render the progress-centric notification, your app must include the Push Templates SDK — see [Push Templates](CTPUSHTEMPLATES.md) for the one-time setup. If your app has its own `FirebaseMessagingService`, render CleverTap pushes with `CTFcmMessageHandler().createNotification(context, message)`; `CleverTapAPI.createNotification` does not render templates.

## Custom handling — build your own notification

When you want a bespoke layout instead of the built-in progress notification, implement `ICleverTapLiveUpdateNotificationFactory` and register it once in your `Application.onCreate()`, since a push can start the app without any Activity. When a live-update push arrives without a `pt_id`, the SDK calls your factory to build the `Notification`; a push with any `pt_id` is rendered by Push Templates instead, and ordinary pushes are unaffected.

#### Kotlin
```kotlin
CleverTapAPI.setLiveUpdateNotificationFactory(object : ICleverTapLiveUpdateNotificationFactory {
    override fun onCreateNotification(context: Context, extras: Bundle): Notification? {
        // Build and return the Notification for this live update, or null to skip.
        // extras carries all CleverTap keys, including wzrk_activityId and the nested "data".
        val ended = extras.getString("wzrk_la_event") == "end"
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_live)
            .setContentTitle("Order on its way")
            .setOngoing(!ended)
            .build()
    }
})
```

#### Java
```java
CleverTapAPI.setLiveUpdateNotificationFactory((context, extras) -> {
    // Build and return the Notification, or null to skip.
    boolean ended = "end".equals(extras.getString("wzrk_la_event"));
    return new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_live)
            .setContentTitle("Order on its way")
            .setOngoing(!ended)
            .build();
});
```

You build the `Notification`; the SDK still owns display, notification-channel creation, the notification id (so your updates land in place), deduplication, and lifecycle analytics. Pass `null` to `setLiveUpdateNotificationFactory` to revert to default rendering.

- The SDK posts your `Notification` as is, so make it non-ongoing on the `end` push yourself (as above); otherwise the user can't swipe it away.
- `onCreateNotification` runs on a background thread while the SDK renders pushes one at a time, so keep it fast and avoid network calls.
- `context` may be the caller's context (for example your `FirebaseMessagingService`), so don't store it.
- If you set your own `deleteIntent`, the SDK doesn't add its own, so call `CleverTapAPI.handleLiveUpdateDismissed(context, extras)` from it to keep recording the `Dismissed` event.

# Lifecycle and analytics

The SDK raises a single event, **`Live Activity`**, for each transition, with a `state` field:

Transition | `Live Activity` state
---|---
Activity started | `Started`
Activity updated | `Updated`
Activity ended | `Ended`
User swiped it away, or it was auto-dismissed (`pt_dismiss`) | `Dismissed`

Each event is tied back to its campaign and activity automatically, so you get a complete funnel without extra instrumentation.

> **Note:** Each update also raises a standard `Notification Viewed` event, so a long-running live update will accrue more than one view.

# Ending and dismissing

The campaign's final push ends the activity: the notification stops being ongoing, drops any status-bar chip or running timer, and becomes dismissable, and an `Ended` event is raised. Whenever the user swipes the notification away, before or after the end, a `Dismissed` event is raised; before the end, the next update shows it again. For the progress-centric notification you can also set an auto-dismiss (`pt_dismiss`, Android 8 and above) so Android removes the card after a set time even if the final push never arrives.

# Requirements

- Live Updates are delivered over your existing FCM push setup — no extra registration is required.
- As for any push, Android 13 (API 33) and above need the `POST_NOTIFICATIONS` runtime permission (see [Push primer](EXAMPLES.md#push-primer-android-13-notification-runtime-permission)).
- To promote the progress-centric notification on Android 16 (API 36) and above, add `<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />` to your app's `AndroidManifest.xml`.
- The progress-centric notification requires the Push Templates SDK (see [Push Templates](CTPUSHTEMPLATES.md)).
- The native Android 16 style requires `androidx.core:core` 1.17.0 or above in your app; with an older version the SDK uses the fallback layout on Android 16 too.
- The native, promoted progress notification is available on Android 16 (API 36) and above; earlier versions use an automatic fallback that mirrors the look but is not promoted.
