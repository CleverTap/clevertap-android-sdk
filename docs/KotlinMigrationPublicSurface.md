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

---

## Outcome (migration done, 2026-11-25)

All Java in `clevertap-core`, `-geofence`, `-hms`, `-pushtemplates`, `sample` and every unit test is
Kotlin, except `pushnotification/work/CTWorkManagerAccessor.java`, kept Java on purpose (its own
comment: SDK-6153 WorkManager `$Companion` issue).

**Verification on `feat/kotlin-migration`:** all unit tests pass (core 4027 / 26 skipped,
pushtemplates 661, geofence 121, hms 34 / 2 skipped, 0 failures; baseline was all green),
`apiCheck` green, `:sample` compiles.

### Public API (BCV) diff vs the Java baseline

- **Removed:** 85 non-API classes (buckets B–F: now `internal` or `@RestrictTo`).
- **Removed members of A types:** only the sanctioned ones: `Var(CTVariables)` constructor and the
  two `Var.define(..., CTVariables)` overloads; `CleverTapAPI.get/setGeofenceCallback`.
- **Changed modifiers (binary-compatible):** former statics are `public static final` (`@JvmStatic`
  is always final); 17 Kotlin interface methods went abstract → default (`-jvm-default=enable`).
- **Added:** Kotlin artefacts only: `Companion` fields/classes, `INotificationRenderer$DefaultImpls`,
  enum `getEntries`, and ~190 `synthetic` members (Kotlin-only property accessors, private-ctor
  bridges). Synthetic members are not callable from Java.

### Source-compatibility patterns used for bucket-A types

- Every Java `getX()/setX()/isX()` stays an open function with the exact JVM name, **plus** a
  Kotlin-only property (`@JvmSynthetic @JvmName("ktGetX") inline get() = getX()`), so Kotlin callers
  can keep writing either `obj.x` or `obj.getX()`. Accessors are `inline` so mockk sees one method.
- Single-method listeners are `fun interface` (SAM lambdas keep working).
- Statics: `companion object` + `@JvmStatic` / `@JvmField` / `const val`; classes that were
  non-final stay `open`.
- `-jvm-default=enable` (gradle-scripts/commons.gradle) so interface default methods stay real JVM
  defaults for Java implementors.

### Kotlin source changes customers may hit (for the 9.0.0 changelog)

Java callers and the binary API are unaffected. Kotlin callers may need small edits:
- **Nullable factory returns** (user decision): `CleverTapAPI.getDefaultInstance`,
  `getGlobalInstance`, `instanceWithConfig`, `CleverTapInstanceConfig.createInstance` /
  `getDefaultInstance` → add `?.` / `!!`.
- Getters that returned null in Java are now typed nullable (e.g. `cleverTapID`, `getProperty`,
  `getInboxMessageForId`, `getDisplayUnitForId`, listener getters, `CleverTapDisplayUnit` getters,
  `CTInboxStyleConfig`/`UTMDetail` getters, instance-config region/proxy/domain getters).
- Boxed results are nullable: `CTFeatureFlagsController.get`, product config
  `getBoolean/getDouble/getLong`.
- Listener callback parameter nullability is now fixed by the declaration (Kotlin overrides must
  match): core listeners take nullable params; `VariableCallback.onValueChanged`,
  `DisplayUnitListener.onDisplayUnitsLoaded` and geofence `CTGeofenceEventsListener` take non-null.
- Constants moved into companions (`HmsConstants`, `PTConstants`, `CTGeofenceSettings.ACCURACY_*`,
  `CTGeofenceAPI.GEOFENCE_LOG_TAG`): qualified use (`HmsConstants.HPS`) still works; a Kotlin
  `import ...HmsConstants.HPS` must become `import ...HmsConstants.Companion.HPS`.
- `DisplayUnitCache` and `INotificationRenderer` (interfaces customers implement) have no property
  syntax: use `getAllDisplayUnits()` / `getActionButtonIconKey()`.
- Previously-public internals are `internal` now (Kotlin callers lose them; Java callers still link).

### Java-client interop audit

BCV only compares erased descriptors, so the Java (pre-migration, `df56c52b6`) and Kotlin release
bytecode were also compared member by member for every public class plus the 57 SDK classes the
wrapper SDKs (flutter, react-native, Leanplum, native-ui-kit, segment, rudder) reference, checking
what javac/the JVM care about but BCV can't see: generic signatures, `throws`, varargs, `final`,
compile-time constants (`case X:`), interface methods losing default bodies, Kotlin-only types in
signatures, mangled/removed members on `@RestrictTo` classes, and new Kotlin parameter null-checks.

- **Fixed:** `DisplayUnitCache.updateDisplayUnits` had become `List<? extends CleverTapDisplayUnit>`,
  which breaks Java implementations' `@Override` → `@JvmSuppressWildcards`.
- **No issue:** no lost `throws`, varargs or constants; no interface method became abstract; no
  Kotlin `Function`/`Unit` types in signatures; every member the wrappers call still exists under
  the same JVM name (only `Utils`' unused implicit constructor is gone).
- **Harmless by construction:** 12 `CleverTapAPI` parameters now carry `? extends` wildcards
  (`pushEvent`, `pushProfile`, `onUserLogin`, multi-value and inbox-id methods): strictly more
  permissive for Java callers, and `CleverTapAPI` cannot be subclassed outside the SDK (its
  constructors were private/package-private and are now synthetic). Same for the new `final` on
  `getCoreState()` and on `CoreMetaData`/`LocalDataStore` members (wrappers call, never subclass).
- **New null-checks:** almost all are on parameters the Java version dereferenced immediately
  anyway (same NPE, one frame earlier) or that Android always supplies (Parcel, receiver
  Context/Intent). Real differences, both judged acceptable:
  - `CleverTapInstanceConfig.setIdentityKeys((String[]) null)` used to store null; the Kotlin
    `vararg` rejects a null array (normal varargs calls are unaffected; a nullable vararg array is
    not expressible in Kotlin without losing the varargs flag).
  - `PushType(type, prefKey, …)` now rejects null `type`/`prefKey`; both wrappers that build
    `PushType`s (flutter `Utils.mapToPushType`, RN `pushTypeFromReadableMap`) already return early
    on null, Leanplum passes literals.

### Kotlin public surface + explicit API (SDK-6380)

The Kotlin types that existed before the Java migration were curated with the same buckets as the
Java ones. Public API classes in the BCV dumps went from 390 to 140.

- **`internal` is JVM-public.** Every Kotlin `internal` declaration - classes, objects, interfaces
  and top-level functions/properties - also carries `@RestrictTo(LIBRARY_GROUP)`, so lint's
  `RestrictedApi` flags any Java/Kotlin caller outside `com.clevertap.*` (enforced by
  `./gradlew apiInteropCheck`).
- **Explicit API mode** (`kotlin { explicitApi() }`) is on for all SDK modules: a public
  declaration without an explicit visibility/return type is a compile error.
- **Now internal (Kotlin callers lose them; Java still links):** the in-app engine (triggers,
  evaluation, stores, caches, PIP config, video handles, `JsonTemplatesProducer`), network/db/crypto/
  validation/profile-traversal/utils internals, `CTXtensions`, `CTPreferenceCache`, `CTWorkManager`,
  live-update internals, `PushProviders`, `DeviceInfo`, `BaseCallbackManager`, `CTMessageDAO`, push
  templates' `PTScaleType`, checkers and validators.
- **Now public + `@RestrictTo`** (lint warning outside the SDK, still callable):
  `CTNotificationIntentService`, `CTPushNotificationReceiver` (deprecated; manifest declarations are
  unaffected), `ILogger`, the bitmap download pipeline, executors, push-handler interfaces;
  `SCDomainListener`/`NotificationRenderedListener` stay reachable for the Signed Call SDK.
- **Hidden members of wrapper-used types** (no SDK module, released push templates or wrapper calls
  them): `LocalDataStore.processProfileTree` (2), `Utils.getNotificationGifURI`/`cleanupOldGIFs`.
- **Java-friendliness added (additive):** `@JvmStatic` on `CTInAppType.fromString`,
  `InAppActionType.fromString`, `CTInAppNotification.defaultTtl`; `@JvmOverloads` on
  `TemplateContext.triggerActionArgument` and the `TemplateRenderer` constructor.
- **Known, kept:** `UserEventLog` and `CTInboxImageData` are public `data class`es (their `copy`
  default-arg bridges are the 2 findings left in `config/api-interop/baseline.txt`); changing the
  class kind would itself break the API.
- `LeanplumCT` stays public API (Leanplum-migration facade used by customers; documented outside
  this repo).

### Possible latent bugs found during conversion (behaviour deliberately preserved)

- `LocalDataStore` static `EXECUTOR_THREAD_ID` written cross-thread without `volatile`.
- `ControllerManager._initializeInbox` reads `deviceInfo.deviceID` three times (racy).
- `synchronized(this)` inside anonymous Callables (feature flags, product config,
  ProductConfigSettings) locks the Callable, protecting nothing.
- `LoginController.asyncProfileSwitchUser` logs field `cachedGUID` instead of param `cacheGuid`.
- `ValidationResultStack.getPendingValidationResults` returns the live list outside its lock.
- `CTInboxMessageContent.writeToParcel` NPEs for content without an "action" object.
- `CTInboxMessage.getCarouselImagesData()` throws when a content lacks media/alt text.
- Inbox view holders pass `bgColor`/`titleColor` to `Color.parseColor` unguarded; carousel holders
  index `dots[0]` without an empty check; `CTInboxActivity.onDestroy` uses fields `onCreate` may
  not have set.
- pushtemplates `PendingIntentFactory` throws when the app has no launch intent (was an implicit
  platform-type NPE; now an explicit `!!`).
- `CTGeofenceSettings.hashCode()` is identity-based while `equals()` compares values (and NPEs on a
  null id); pushtemplates `Utils.getTimerEnd` parses before checking for "-1".
- `CleverTapAPI.buildPushProvidersFromManifest` has an always-true `splits != null` check.

### Small intentional differences

- `CTHmsMessageHandler.createNotification(ctx, null)` returns `false` without calling the parser
  (its "Invalid Notification Message" debug log no longer appears).
- `Logger` asserts non-null messages before `android.util.Log` (Logcat already NPEs on null on a
  device; under Robolectric this is new).
- Unit tests use `kotlin.test`; hamcrest stays only for `samePropertyValuesAs` (5 sites: no
  `kotlin.test` equivalent, compared types have no value `equals()`).
