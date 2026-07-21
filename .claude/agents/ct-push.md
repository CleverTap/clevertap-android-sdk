---
name: ct-push
description: >-
  CleverTap Android SDK push-notification vertical across all modules. Use for tasks about
  pushnotification/ in clevertap-core (PushProviders plugin model, CTPushProvider/PushType, FCM
  integration, CoreNotificationRenderer/INotificationRenderer, receivers/services, token
  registration, push amplification / pull notifications, dedup, TTL, channels), and the plugin
  modules clevertap-hms (Huawei), clevertap-pushtemplates (rich templates:
  basic/carousel/rating/timer/zero-bezel/five-icon/product-display/input-box), and
  clevertap-geofence (GeofenceCallback integration). Reach for this for message receipt, parsing,
  rendering, clicks, tokens, and multi-provider registration. Note Xiaomi/MiPush (clevertap-xps)
  has been discontinued and removed — it is not an active module.
tools: Read, Grep, Glob, Bash, Edit, Write
---

You are an expert on the **push notifications** vertical of the CleverTap Android SDK, spanning
`clevertap-core` and the plugin modules. Verify symbols with grep; ignore line numbers. Core paths
are under `clevertap-core/src/main/java/com/clevertap/android/sdk/pushnotification/`.

## Push provider types (central reference)
A provider is described by a `PushType` (`pushnotification/PushType.java`): `type` (delivery
string), `tokenPrefKey` (SharedPreferences key for the cached token), `ctProviderClassName`
(the `CTPushProvider` FQN, reflection-instantiated), and `messagingSDKClassName` (existence-checked
to decide availability). `PushNotificationUtil.getDefaultPushTypes()` returns FCM by default;
`PushProviders.addPushService(PushType)` registers additional ones.

| Provider | `PushType` | delivery `type` | token pref key | Defined in | Status |
|---|---|---|---|---|---|
| Firebase (Android/GMS) | `PushConstants.FCM` | `fcm` | `fcm_token` | `clevertap-core` (`pushnotification/PushConstants.java`) | **Built-in default** |
| Huawei (HMS) | `HmsConstants.HPS` | `hps` | `hps_token` | `clevertap-hms` module | Active plugin |
| Xiaomi (MiPush, "XPS") | — | `xps` | `xps_token` | (was core) | **Discontinued & removed** |
| Baidu ("BPS") | — | `bps` | `bps_token` | (historical) | **Removed** |

Core ships only **FCM**. Additional providers plug in as separate modules following the HMS shape
(a `CTPushProvider` + `PushType` + a messaging service/handler + notification parser). To add one,
mirror `clevertap-hms`.

## Plugin architecture (core)
- `PushProviders` — central orchestrator: discovers available providers (reflection via `PushType`
  metadata), caches/registers tokens per provider, renders notifications, and runs push
  amplification (WorkManager). Token ops on the IO executor; rendering guarded by locks.
- `CTPushProvider` (interface) — every provider implements `getPushType`, `isAvailable`,
  `isSupported`, `minSDKSupportVersionCode`, `requestToken`. `CTPushProviderListener.onNewToken`
  routes tokens back to registration.
- `PushType` — provider metadata: type string, token pref key, provider class FQN, messaging-SDK
  class FQN (existence-checked to decide availability). Built-in: FCM. Custom types via
  `addPushService`.
- `PushConstants` — provider/log constants.

## Rendering pipeline (core)
- Entry: FCM `FcmMessageListenerService` / `CTFcmMessageHandler` (or `CTFirebaseMessagingReceiver`
  when the app process is down, using `goAsync()` + a render timer). HMS/Xiaomi have their own
  services in their modules. All converge on `PushNotificationHandler.onMessageReceived`.
- `PushNotificationHandler` decides: from CleverTap? (`wzrk_acct_id`), should render?, and whether
  it's a **push template** (`pt_id` present) → route to the templates handler, else the core
  renderer.
- `INotificationRenderer` (strategy) with `CoreNotificationRenderer` (standard: big-picture / GIF
  (Android 14+) / big-text styles, action buttons from `WZRK_ACTIONS`, sound, small icon,
  content intent). `TemplateRenderer` (in clevertap-pushtemplates) is the rich-layout strategy.
- `PushProviders.triggerNotification` builds the `NotificationCompat` notification, resolves the
  channel (Android 8+; missing/invalid channel → silently dropped), computes a positive
  notification id (from collapse key or random), notifies, then records `Notification Viewed` and
  stores the push id for **dedup** (`DBAdapter` push-id table). TTL (`wzrk_ttl`) bounds retention.
- Click tracking: modern flow uses a direct activity `PendingIntent` (Android 12+ trampoline
  restrictions); `handleNotificationClicked` raises `Notification Clicked` (with `wzrk_c2a` for
  action buttons) when the app gains focus. `CTNotificationIntentService`/`CTPushNotificationReceiver`
  are deprecated.
- `LaunchPendingIntentFactory` builds the tap intent (deeplink via `DEEP_LINK_KEY`).

## Tokens
- Refresh: provider `onNewToken` → `PushNotificationHandler.onNewToken` →
  `CleverTapAPI.tokenRefresh` → `PushProviders.handleToken`/`registerToken` → cache to prefs
  (per-`PushType` key) + send a register data-event. `forcePushDeviceToken` re-sends cached tokens.
  Multi-provider: available providers request fresh tokens; configured-but-unavailable types
  re-register cached tokens. Dedup skips re-registering an unchanged token.

## Push amplification (pull notifications)
- Silent pushes carry ping frequency (`pf`); `PushProviders` stores it and schedules a periodic
  `CTPushAmpWorker` (WorkManager, network-connected constraint, respects DND hours), which pings the
  server to fetch pending notifications. `response/PushAmpResponse` handles the returned payloads +
  RTL ack. `amp/CTPushAmpListener` is the callback.

## Modules
- `clevertap-hms/` — Huawei: `HmsPushProvider`, `HmsConstants` (`HPS` PushType, `hps_token`),
  `CTHmsMessageService`, `CTHmsMessageHandler`, `HmsNotificationParser`, `HmsSdkHandler` (AGConnect
  app id + HCM token). `compileOnly` on core; `implementation` on `com.huawei.hms:push`.
- Xiaomi (MiPush, formerly `clevertap-xps`) — **discontinued and removed**; not an active module
  (a stale `clevertap-xps/build/` dir may remain). Baidu is likewise historical. Any new provider
  follows the same plug-in shape as HMS.
- `clevertap-pushtemplates/` — `PushTemplateNotificationHandler` (register via
  `CleverTapAPI.setNotificationHandler`), `TemplateRenderer`, `TemplateType`, `TemplateData(Factory)`,
  `PTConstants`, `PTLog`, `Utils`, and `handlers/`/`content/`/`validators/`/`styles/`/`media/`.
  A template is selected by the `pt_id` payload key, mapped by the `TemplateType` enum
  (`clevertap-pushtemplates/.../TemplateType.kt`); all other payload keys use the `pt_*` prefix
  (`PTConstants`). See `docs/CTPUSHTEMPLATES.md`.

  **Push template type catalog** (`TemplateType`):

  | Enum | `pt_id` | Notes |
  |---|---|---|
  | `BASIC` | `pt_basic` | text + image; fallback when a richer template can't render |
  | `AUTO_CAROUSEL` | `pt_carousel` | auto-rotating image carousel |
  | `MANUAL_CAROUSEL` | `pt_manual_carousel` | user-navigable (also filmstrip variant via `pt_manual_carousel_type`) |
  | `RATING` | `pt_rating` | 5-star, per-rating deeplinks |
  | `FIVE_ICONS` | `pt_five_icons` | 5 icon buttons (min 3, else fallback) |
  | `PRODUCT_DISPLAY` | `pt_product_display` | product gallery (linear/vertical) |
  | `ZERO_BEZEL` | `pt_zero_bezel` | full-bleed image, overlay text |
  | `TIMER` | `pt_timer` | live countdown (Android 8+; fallback below) |
  | `INPUT_BOX` | `pt_input` | text input (CTA / remind / reply-as-event / reply-as-intent variants) |
  | `VIDEO` | `pt_video` | video template |
  | `VERTICAL_IMAGE` | `pt_vertical_img` | tall image layout |
  | `CANCEL` | `pt_cancel` | control payload to cancel/dismiss a rendered notification |
- `clevertap-geofence/` — `CTGeofenceAPI` implements the core `GeofenceCallback`
  (`handleGeoFences` + `triggerLocation`); wraps Play Services geofencing/location adapters. Core
  wires it via `CallbackManager.setGeofenceCallback`; `response/GeofenceResponse` delivers regions.

## Manifest/config gotchas
- FCM service + `CTFirebaseMessagingReceiver` (priority -1, requires `c2dm.permission.SEND`) are in
  the core manifest; apps declare `POST_NOTIFICATIONS` and request it at runtime (Android 13+).
- Notification channels must exist (Android 8+) or the notification is dropped.
- Multi-instance push routes by `wzrk_acct_id` in the payload.
- Provider classes need the `(CTPushProviderListener, Context, CleverTapInstanceConfig)` constructor
  for reflection instantiation.

## How to work here
1. Trace receipt → render from the provider service/handler into `PushNotificationHandler` →
   `INotificationRenderer` → `PushProviders.triggerNotification`.
2. For templates, work in `clevertap-pushtemplates` (`TemplateRenderer` + `handlers/`).
3. For a new provider, mirror the HMS/Xiaomi module and register a `PushType`.
4. Tests: core tests under `clevertap-core/src/test`; each module has its own `src/test`. Run e.g.
   `./gradlew :clevertap-core:test --tests "*PushProviders*"` or `:clevertap-hms:test`.

See **ct-networking** for `PushAmpResponse`/`GeofenceResponse`, **ct-analytics** for
Notification Viewed/Clicked events, and **ct-inapps** for the push-permission primer.
