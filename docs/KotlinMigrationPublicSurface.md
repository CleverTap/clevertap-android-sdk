# Kotlin migration — curated public surface (Java types)

Branch: `feat/kotlin-migration` (off `release/core_9.0.0` @ `a42229811`). Jira epic SDK-6366.
Scope: the 173 `public` top-level **Java** types in `clevertap-core`, `-geofence`, `-hms`, `-pushtemplates`.

## How this was derived

Mechanically from compiled release bytecode (`javap`), then curated by hand:

1. **Roots** — `CleverTapAPI`, `CTGeofenceAPI`, plus every SDK type named in a code block of the
   customer-facing docs (README, EXAMPLES, CTGEOFENCE, CTHUAWEIPUSH, CTPUSHTEMPLATES*, CTLIVEUPDATES,
   CTV4CHANGES, CustomCodeTemplates, Variables, FAQ, InAppNotificationsJavascriptInterface,
   SystemInAppFunctions). Internal design docs (fcaps/serving/trigger) were excluded.
2. **Closure** — followed every public/protected member signature (params, returns, generics) and
   supertype of reached types. Members annotated `@RestrictTo` were *not* followed.
3. **Other reasons a type must stay JVM-public** — declared in an `AndroidManifest.xml`, loaded by
   name (`Class.forName` / string constant / consumer keep rule), referenced from another SDK module's
   bytecode, or used by our wrapper SDKs (flutter, react-native, Leanplum-Android-SDK,
   native-ui-kit, segment, rudder — scanned from local checkouts).

Result: **64 reachable, 9 platform, 12 cross-module, 4 wrapper-only, 84 accidental** — then adjusted
below. Roughly **half the public Java types should not be public.**

## Target visibility rules

| Bucket | Kotlin visibility after conversion | API check (BCV) |
|---|---|---|
| A. Public API | `public`, ABI frozen (`@JvmStatic`/`@JvmField`/`@JvmOverloads`/`@Throws` as needed) | tracked |
| B. Leaked via a member | `internal` **after** hiding the leaking member with `@RestrictTo(LIBRARY_GROUP)` | excluded |
| C. Platform-instantiated | `internal` is safe (Kotlin `internal` is still JVM-public, so manifest / reflection / inflation keep working) — unless customers declare/extend it (see notes) | excluded |
| D. Cross-module SDK internals | `public` + `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` (all modules publish as `com.clevertap.android`) | excluded via `nonPublicMarkers` |
| E. Used by wrapper SDKs only | **decision needed** — promote to A, or `@RestrictTo(LIBRARY_GROUP_PREFIX)` (covers `com.clevertap.*` groups, not `com.leanplum`) | — |
| F. Accidentally public | `internal` | excluded |

`@RestrictTo` is lint-only: it never breaks a customer build, so moving a type from A to D/E is
source- and binary-compatible. Moving a type to `internal`/F is binary-compatible for Java callers
(still JVM-public) but makes it invisible to Kotlin callers — only do that for types no customer
should be touching.

---

## A. Public API — keep public (≈52)

**Entry points & config**
`CleverTapAPI` (+ nested `LogLevel` etc.), `CleverTapInstanceConfig`, `ActivityLifecycleCallback`,
`Application`, `Logger` (wrappers call `Logger.v/d`), `UTMDetail`, `events.EventDetail`,
`CTInboxStyleConfig`, `CTWebInterface` *(manual: JS bridge customers attach to their WebViews; used by native-ui-kit)*

**Listeners / callbacks customers implement**
`CTInboxListener`, `InboxMessageListener`, `InboxMessageButtonListener`, `FetchInboxCallback`,
`InAppNotificationListener`, `InAppNotificationButtonListener`, `PushPermissionResponseListener`,
`SyncListener` (D), `CTFeatureFlagsListener` (D), `product_config.CTProductConfigListener` (D),
`interfaces.OnInitCleverTapIDListener`, `pushnotification.CTPushNotificationListener`,
`pushnotification.amp.CTPushAmpListener`, `displayunits.DisplayUnitListener`,
`displayunits.DisplayUnitCache`, `variables.callbacks.FetchVariablesCallback`,
`variables.callbacks.VariableCallback`, `variables.callbacks.VariablesChangedCallback`,
`variables.annotations.Variable`

**Models returned to customers**
`inbox.CTInboxMessage`, `inbox.CTInboxMessageContent`, `inbox.CTInboxMessageType`,
`displayunits.model.CleverTapDisplayUnit`, `displayunits.model.CleverTapDisplayUnitContent`,
`displayunits.CTDisplayUnitType`, `variables.Var`, `pushnotification.NotificationInfo`,
`pushnotification.PushType`

**Push integration (customers call / extend / declare)**
`pushnotification.fcm.CTFcmMessageHandler`, `pushnotification.fcm.FcmMessageListenerService`,
`pushnotification.PushNotificationHandler` *(manual: documented for custom FCM services; used by flutter, Leanplum)*,
`pushnotification.INotificationRenderer` (via `CleverTapAPI#renderPushNotification`; implemented by push templates),
`interfaces.NotificationHandler` (via `CleverTapAPI#setNotificationHandler`)

**Deprecated but still public (keep until removal)**
`featureFlags.CTFeatureFlagsController`, `product_config.CTProductConfigController`,
`product_config.ProductConfigSettings`

**Other modules**
- geofence: `CTGeofenceAPI`, `CTGeofenceSettings`, `Logger` (via `CTGeofenceAPI#getLogger`),
  `interfaces.CTGeofenceEventsListener`, `interfaces.CTLocationUpdatesListener`
- pushtemplates: `PushTemplateNotificationHandler`, `PTConstants` *(only `PT_DISMISS_ON_CLICK` etc. are documented — see "Constants" below)*
- hms: `HmsConstants` (`HmsConstants.HPS` is documented), `CTHmsMessageHandler` *(verify: likely in developer.clevertap.com custom-HMS-service docs)*

## B. Public only because a member leaks them — hide the member, then make `internal`

| Type | Leaks through | Fix |
|---|---|---|
| `variables.CTVariables` | `Var` constructor / `Var.define(..., CTVariables)` | `@RestrictTo` those `Var` members |
| `variables.VarCache` | `CTVariables` constructor | follows from above |
| `GeofenceCallback` | `CleverTapAPI#get/setGeofenceCallback` (core↔geofence wiring) | `@RestrictTo(LIBRARY_GROUP)` on both methods; type → D |
| `ILogger` | supertype of `Logger`, ctor params of internal caches | keep as D (Logger is API and implements it) |
| `DidClickForHardPermissionListener` | supertype of `InAppNotificationActivity` | → F once the activity is internal |
| `inbox.CTInboxListViewFragment` (+`InboxListener`) | nested type reached via `CTInboxActivity` | → F with the activity |
| `pushnotification.fcm.IFcmMessageHandler` | supertype of `CTFcmMessageHandler` | keep interface, `@RestrictTo` (already) |
| `interfaces.IPushAmpHandler` | supertype of `CTFcmMessageHandler`; used by hms | → D |
| `interfaces.ActionButtonClickHandler` | supertype of `PushTemplateNotificationHandler` | → D |
| `Constants` (already `@RestrictTo`) | docs show `Constants.BLUE/WHITE/BLACK` | move the documented constants to a small public object; rest → D (pushtemplates uses it) |

## C. Platform-instantiated (manifest / reflection) — must stay JVM-public

`InAppNotificationActivity`, `inbox.CTInboxActivity`, `pushnotification.fcm.CTFirebaseMessagingReceiver`,
`pushnotification.fcm.FcmPushProvider` (reflection), `pushnotification.CTPushNotificationReceiver` (D),
`pushnotification.CTNotificationIntentService` (D, reflection + keep rule),
geofence `CTGeofenceReceiver`, `CTLocationUpdateReceiver`, `CTGeofenceBootReceiver`,
hms `CTHmsMessageService`, `HmsPushProvider` (reflection; Leanplum references it),
pushtemplates `PTPushNotificationReceiver`, `PushTemplateReceiver`.

Notes: `CTNotificationIntentService` and `CTPushNotificationReceiver` are deprecated but older
integration guides told customers to declare them in their own manifest — keep the FQN; Kotlin
`internal` is fine. `FcmMessageListenerService` is in A because customers subclass it.
Layout-inflated views (`customviews.*`, `gif.GifImageView`) are inflated reflectively — Kotlin
`internal` is fine for them too.

## D. Cross-module SDK internals — `public` + `@RestrictTo(LIBRARY_GROUP)`

| Type (core) | Used by |
|---|---|
| `Utils` | geofence, hms, pushtemplates (+ Leanplum) |
| `ManifestInfo` | pushtemplates |
| `interfaces.AudibleNotification` | pushtemplates |
| `interfaces.INotificationParser` | hms |
| `pushnotification.CTPushProvider`, `CTPushProviderListener` | hms |
| `pushnotification.LaunchPendingIntentFactory` | pushtemplates |
| `pushnotification.PushConstants` | hms (+ flutter) |
| `pushnotification.PushNotificationUtil` | pushtemplates |
| `task.CTExecutors`, `task.Task` (+ `OnSuccessListener`, `OnFailureListener`) | pushtemplates (+ Leanplum) |
| plus from B: `GeofenceCallback`, `ILogger`, `IPushAmpHandler`, `ActionButtonClickHandler`, `Constants` | |

Longer term, `Utils` should be split: the few helpers other modules need move to a small
`@RestrictTo` util; the rest becomes `internal`.

## E. Used only by wrapper SDKs — kept JVM-public + `@RestrictTo` (decided for this migration)

Decision: don't break any wrapper. These stay `public` in Kotlin, annotated
`@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)` (lint-only), so they drop out of the API
baseline but every wrapper keeps compiling. Proper accessors can replace them later.

| Type | Consumer | Suggestion |
|---|---|---|
| `CoreMetaData` | Leanplum-Android-SDK | add a narrow public/restricted accessor on `CleverTapAPI` for what Leanplum reads, then internalise |
| `LocalDataStore` | Leanplum-Android-SDK | same |
| `task.CTExecutorFactory` / `CTExecutors` | Leanplum-Android-SDK | D already covers `CTExecutors`; Leanplum's group is `com.leanplum` so `@RestrictTo` will lint-warn there — acceptable, or expose a minimal API |
| `variables.CTVariableUtils` | react-native | `@RestrictTo(LIBRARY_GROUP_PREFIX)` (RN plugin group is `com.clevertap.*`) |

## F. Accidentally public — become `internal` on conversion (≈75)

- **core, root package:** `AnalyticsManager`, `BaseAnalyticsManager`, `BaseCallbackManager`,
  `CTLockManager`, `CallbackManager`, `ControllerManager`, `DeviceInfo`, `FailureFlushListener`,
  `InAppFCManager`, `SessionManager`, `DidClickForHardPermissionListener`
- **customviews / gif:** `CloseImageView`, `HorizontalSquareImageView`, `RectangleImageView`,
  `SquareImageView`, `VerticalSpaceItemDecoration`, `GifImageView`, `SimpleBitmapProvider`
- **displayunits:** `CTDisplayUnitController`
- **events:** `BaseEventQueueManager`, `EventGroup`, `EventMediator`
- **featureFlags / product_config:** `CTFeatureFlagConstants`, `CTFeatureFlagsFactory`,
  `CTProductConfigConstants`, `CTProductConfigFactory`
- **inbox:** `CTCarouselViewPager`, `CTCarouselViewPagerAdapter`, `CTInboxBaseMessageViewHolder`,
  `CTInboxController`, `CTInboxTabAdapter`, `CTMessageDAO`, `CTInboxListViewFragment`
- **interfaces:** `NotificationRenderedListener`, `SCDomainListener`
- **login:** `ConfigurableIdentityRepo`, `IdentityRepo`, `IdentityRepoFactory`, `IdentitySet`,
  `LegacyIdentityRepo`, `LoginConstants`, `LoginController`, `LoginInfoProvider`
- **pushnotification:** `CoreNotificationRenderer`, `fcm.FcmSdkHandlerImpl`, `fcm.IFcmSdkHandler`,
  `work.CTWorkManagerAccessor`
- **response:** all 11 (`ARPResponse`, `CleverTapResponse`, `ConsoleResponse`, `FeatureFlagResponse`,
  `FetchVariablesResponse`, `GeofenceResponse`, `InAppResponse`, `InboxResponse`,
  `MetadataResponse`, `ProductConfigResponse`, `PushAmpResponse`)
- **task:** `MainThreadExecutor`
- **utils / validation / variables:** `CTJsonConverter`, `FileUtils`, `PackageUtils`, `UriHelper`,
  `ManifestValidator`, `ValidationResult`, `ValidationResultStack`, `JsonUtil`, `Parser`,
  `CTVariables`, `VarCache`
- **geofence:** `BackgroundLocationWork`, `CTGeofenceConstants`, `FileUtils`, `GeofenceStorageHelper`,
  `interfaces.CTGeofenceAdapter`, `CTGeofenceTask`, `CTLocationAdapter`, `CTLocationCallback`,
  `model.CTGeofence`
- **hms:** `HmsNotificationParser`, `IHmsMessageHandler`, `IHmsSdkHandler`
- **pushtemplates:** `PTLog`, `Utils`

## Caveats

- **External docs not scanned.** developer.clevertap.com is the real customer contract; items marked
  *manual*/*verify* came from memory of those docs and need a check before they're frozen or hidden.
- **Customers we can't see** may call anything that's public today. Moving a type to `internal` is
  still binary-compatible for Java callers, but Kotlin callers lose it. Call out the
  narrowing in the 9.x release notes / CTV migration doc.
- **Kotlin side has the same problem**: 182 already-Kotlin public top-level types are reachable from
  no API root, and some public Kotlin API classes leak internal caches/stores through constructors
  (e.g. `CTCaches`, `FileResourcesRepoImpl`, `InAppAssetsStore`). Out of scope for the Java
  conversion but the same BCV baseline will surface them.
