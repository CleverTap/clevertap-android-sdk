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
| `ManifestInfo` | pushtemplates, **Signed Call** (`getInstance(Context)` must stay `static`: SDK-6395) |
| `DeviceInfo` (only the statics `getAppIconAsIntId(Context)`, `getDeviceType(Context)`) | **Signed Call** (`invokestatic`; the class itself stays `internal`, SDK-6395) |
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
| `CTXtensions.isNotNullAndBlank` (top-level function) | flutter (`import com.clevertap.android.sdk.isNotNullAndBlank`) | public + `@RestrictTo(LIBRARY_GROUP_PREFIX)` (SDK-6395; the first wrapper scan matched class names only) |

## F. Accidentally public — become `internal` on conversion (≈75)

- **core, root package:** `AnalyticsManager`, `BaseAnalyticsManager`, `BaseCallbackManager`,
  `CTLockManager`, `CallbackManager`, `ControllerManager`, `DeviceInfo` (except its two
  Signed Call statics, see D), `FailureFlushListener`,
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
Kotlin, except:
- `pushnotification/work/CTWorkManagerAccessor.java`, kept Java on purpose (its own comment: SDK-6153
  WorkManager `$Companion` issue);
- the 16 client-implemented callback types restored as Java by SDK-6395 (see "Java and wrapper
  compatibility fixes" below).

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
- ~~Listener callback parameter nullability is now fixed by the declaration~~ Not any more (SDK-6395):
  the callback interfaces customers and wrappers implement are Java again, so Kotlin sees platform
  types and both the nullable and the non-null override spellings compile, exactly as before 9.0.0.
- Constants moved into companions (`HmsConstants`, `PTConstants`, `CTGeofenceSettings.ACCURACY_*`,
  `CTGeofenceAPI.GEOFENCE_LOG_TAG`): qualified use (`HmsConstants.HPS`) still works; a Kotlin
  `import ...HmsConstants.HPS` must become `import ...HmsConstants.Companion.HPS`.
- `DisplayUnitCache` and `INotificationRenderer` are Java interfaces again (SDK-6395), so Kotlin
  callers can use either `getAllDisplayUnits()` or `allDisplayUnits`, as before 9.0.0.
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
  - `CleverTapAPI.parseVariables((Object[]) null)` and `parseVariablesForClasses((Class[]) null)` now
    throw `NullPointerException`. The Java `Parser` caught the NPE from iterating a null array and only
    logged it. Normal varargs calls (including null *elements*) are unaffected; a nullable vararg
    array is not expressible in Kotlin without losing the varargs flag.
  - `CleverTapAPI.getUserEventLog(null)` / `getUserEventLogCount(null)` now throw
    `IllegalStateException` ("Required value was null.") instead of `NullPointerException`. Both threw
    before; only the exception type changed.
  - `INotificationRenderer.setActionButtons(..., nb = null, ...)`: the 9.0.0 Kotlin interface rejected
    a null builder. Fixed by SDK-6395: the default method is the old Java body again, so a null builder
    behaves as in 8.x (returned when there are no actions; otherwise each `addAction` failure is
    caught and logged).
  - `Var.addFileReadyHandler(null)` now throws `NullPointerException` at the call. The Java `Var`
    stored the null and threw later, when the file became ready, so it failed either way; only the
    point of failure moved.
- **`Constants` is a class, no longer an interface.** Its constant values, field types and static
  flags are identical to 8.x (restored by SDK-6395), so reading `Constants.X` from Java or Kotlin is
  unaffected. Only `implements Constants` (inheriting the constants by implementing the interface)
  no longer compiles or links. `Constants` was already `@RestrictTo` in Java, and no shipped consumer
  implements it (consumer link check). Converting it back to an interface would mean restructuring
  359 fields for no known caller, so the change is intentional.

### Java and wrapper compatibility fixes (SDK-6395)

A pre-release audit (JVM linkage of the real shipped consumers, a Java source probe of every A/C/D/E
class, real compiles of every wrapper) found these regressions; all are fixed:

- **Statics restored (`@JvmStatic`).** Every member that was `public static` in Java and had become a
  `Companion`/`object` instance method is static again: `ManifestInfo.getInstance(Context)` (still
  locks `ManifestInfo.class`; Signed Call and push-templates <= 2.7.0 call it), `DeviceInfo.getAppIconAsIntId`
  / `getDeviceType` (Signed Call), `InAppNotificationActivity.launchForPushPermissionPrompt` /
  `launchForInAppNotification`, `CTMessageDAO.initWithJSON`, and the bucket-F `object`s
  (`CTJsonConverter`, `JsonUtil`, `PackageUtils`, `UriHelper`, `ManifestValidator`,
  `CTFeatureFlagsFactory`, `CTProductConfigFactory`, `IdentityRepoFactory`, push templates `Utils`/`PTLog`).
  Not restored (deliberate): `Utils.getNotificationGifURI` / `cleanupOldGIFs` (still `internal`; they
  take the internal `Clock`, and no shipped consumer calls them) and `CTJsonConverter.toJsonArray` /
  `toJsonString` / `toList` (deleted as unused, bucket F).
- **Subclassable again.** `CTHmsMessageService`, `HmsPushProvider`, `PTPushNotificationReceiver`,
  `PushTemplateReceiver`, `CTGeofenceBootReceiver`, `CTLocationUpdateReceiver`, `CTGeofenceReceiver`
  are `internal open` (their overrides are open); `LaunchPendingIntentFactory` (D) is an open class with
  a public constructor again (statics in a `@JvmStatic` companion). Bucket-F classes that stay final
  are listed in the SDK-6395 PR.
- **Old JVM shapes.** `Constants.SYSTEM_EVENTS` (`String[]`), `LEGACY_IDENTITY_KEYS` /
  `ALL_IDENTITY_KEYS` / `piiDBKeys` (`HashSet<String>`) and `NULL_STRING_ARRAY` are public static fields
  again (`@JvmField`); `CTInboxActivity.orientation` is a public static field again;
  `CTInboxMessage(Parcel)` and `CTInboxStyleConfig(CTInboxStyleConfig)` are private again (a public
  overload made `new CTInboxMessage(null)` / `super(null)` ambiguous in Java).
- **Null tolerance restored** (the Java versions returned null or did nothing):
  `getDevicePushToken(null)` / `getPushToken(null)` return null; `removeCleverTapIDListener(null)`,
  `removeVariablesChangedCallback(null)`, `removeOneTimeVariablesChangedCallback(null)` and
  `setRequestDevicePushTokenListener(null)` are no-ops; `CTFcmMessageHandler.processPushAmp(ctx, null)`
  is a no-op and `CTHmsMessageHandler.processPushAmp(ctx, null)` logs, as before.
- **Wrapper source compatibility (option A).** The callback types clients implement are Java again, so
  Kotlin implementations compile with either nullability: `InAppNotificationListener`,
  `InboxMessageListener`, `DisplayUnitListener`, `InAppNotificationButtonListener`,
  `InboxMessageButtonListener`, `SyncListener`, `OnInitCleverTapIDListener`,
  `CTPushNotificationListener`, `CTPushAmpListener`, `DisplayUnitCache`, `NotificationHandler`,
  `SCDomainListener`, `INotificationRenderer`, `VariableCallback`, and geofence
  `CTGeofenceEventsListener` / `CTLocationUpdatesListener`. Their JVM shape is unchanged (the only
  `.api` change is the removal of the Kotlin-only `INotificationRenderer$DefaultImpls`). `Var`'s
  callback methods take `VariableCallback<in T>` (Java: `VariableCallback<? super T>`, same
  descriptor), so a `VariableCallback<Any?>` can be registered on a `Var<Any>` again.
  `isNotNullAndBlank` is public + `@RestrictTo(LIBRARY_GROUP_PREFIX)` again (Flutter imports it).

#### Nullable returns that were platform types in Java (option A 2d)

Kotlin cannot declare a platform type, so every return that Java left un-annotated had to become
either `T` or `T?`. Each one was re-checked: it is declared non-null only when **every** path returns a
non-null value and the compiler proves it (no `!!`, `checkNotNull`, `?: throw` or `error()` added),
counting all branches, catch blocks, async/lazy init windows, config variants (analytics-only,
personalization off) and later reassignment. Everything else stays nullable, and the wrappers
(Flutter, Leanplum) are patched with `?.` / `!!` instead.

Scope: public and `@RestrictTo`-public methods/properties of the bucket A/D/E, wrapper-used and
Signed Call classes whose new return is `@Nullable` while the old Java return had no `@Nullable`
(found from the bytecode annotations of both builds). `DeviceInfo` (18 getters) is left out: it is a
Kotlin `internal` class, so Kotlin callers cannot see its nullability, and Signed Call (pure Java)
uses only its two `int` statics. `Var.value()` returns `T`, not `T?`; a `Var<*>` receiver gives
`Any?` in Kotlin and no declaration-side change fixes that (`T : Any` breaks `Var<Any?>` users), so
Flutter `:946` needs a wrapper `!!`.

Result: 149 returns checked, **2 made non-null** (`Var.name()`, `Var.nameComponents()`), 147 stay
nullable. Four of those (`CTFeatureFlagsController.get`, product config `getBoolean` / `getDouble` /
`getLong`) never return null, but a non-null `Boolean`/`Double`/`Long` return compiles to a
primitive, which would change the JVM descriptor.

| Method | Verdict | Evidence |
|---|---|---|
| `CTGeofenceSettings.getId` (CTGeofenceSettings.kt:281) | stays nullable | builder field is nullable (no id set) |
| `CTInboxStyleConfig.getBackButtonColor` (CTInboxStyleConfig.kt:72) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getFirstTabTitle` (CTInboxStyleConfig.kt:93) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getInboxBackgroundColor` (CTInboxStyleConfig.kt:114) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getNavBarColor` (CTInboxStyleConfig.kt:135) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getNavBarTitle` (CTInboxStyleConfig.kt:156) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getNavBarTitleColor` (CTInboxStyleConfig.kt:177) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getNoMessageViewText` (CTInboxStyleConfig.kt:198) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getNoMessageViewTextColor` (CTInboxStyleConfig.kt:220) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getSelectedTabColor` (CTInboxStyleConfig.kt:241) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getSelectedTabIndicatorColor` (CTInboxStyleConfig.kt:262) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getTabBackgroundColor` (CTInboxStyleConfig.kt:283) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CTInboxStyleConfig.getUnselectedTabColor` (CTInboxStyleConfig.kt:332) | stays nullable | the setter accepts null and the Parcel constructor uses `readString()` (CTInboxStyleConfig.kt:49-61) |
| `CleverTapAPI.featureFlag` (CleverTapAPI.kt:406) | stays nullable | null for `isAnalyticsOnly` (CleverTapFactory.kt:851) and until the async `initFeatureFlags` task runs (CleverTapFactory.kt:598); field starts null (ControllerManager.kt:51) |
| `CleverTapAPI.getCTNotificationInboxListener` (CleverTapAPI.kt:494) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.getCTPushAmpListener` (CleverTapAPI.kt:519) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.getCTPushNotificationListener` (CleverTapAPI.kt:541) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.getCleverTapAttributionIdentifier` (CleverTapAPI.kt:577) | stays nullable | same as `getCleverTapID` (DeviceInfo.kt:438) |
| `CleverTapAPI.getCleverTapID` (CleverTapAPI.kt:593) | stays nullable | `DeviceInfo.deviceID` = stored id ?: fallback id, both nullable before the id barrier (DeviceInfo.kt:340) |
| `CleverTapAPI.getDetails` (CleverTapAPI.kt:662) | stays nullable | `LocalDataStore.getEventDetail` returns null when personalization is off / no event / on error (LocalDataStore.kt:68-75) |
| `CleverTapAPI.getUserEventLog` (CleverTapAPI.kt:680) | stays nullable | returns null when personalization is disabled; DAO read returns null for an unknown event |
| `CleverTapAPI.getDevicePushToken` (CleverTapAPI.kt:698) | stays nullable | `PushProviders.getCachedToken` returns null for null type / no token |
| `CleverTapAPI.getDevicePushTokenRefreshListener` (CleverTapAPI.kt:705) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.getHistory` (CleverTapAPI.kt:819) | stays nullable | `getEventHistory` returns null from its catch (LocalDataStore.kt:83-94) |
| `CleverTapAPI.getInAppNotificationListener` (CleverTapAPI.kt:855) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.getInboxMessageForId` (CleverTapAPI.kt:921) | stays nullable | null when the inbox is not initialised or the id is unknown (`inboxControllerOrLog()?`, CTInboxController.kt:119) |
| `CleverTapAPI.getLocation` (CleverTapAPI.kt:963) | stays nullable | `findBestLocation` returns null without a LocationManager/permission (LocationManager.kt:27-30) |
| `CleverTapAPI.getProperty` (CleverTapAPI.kt:1032) | stays nullable | returns null when personalization is disabled, or the property is unknown |
| `CleverTapAPI.getProfile` (CleverTapAPI.kt:1061) | stays nullable | returns null when personalization is disabled |
| `CleverTapAPI.getPushToken` (CleverTapAPI.kt:1076) | stays nullable | same as `getDevicePushToken` |
| `CleverTapAPI.getSyncListener` (CleverTapAPI.kt:1095) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.productConfig` (CleverTapAPI.kt:1406) | stays nullable | CoreState.kt:89-95 `initProductConfig` returns early for `isAnalyticsOnly`, so the controller stays null (CoreState.kt:78) |
| `CleverTapAPI.pushSignedCallEvent` (CleverTapAPI.kt:1593) | stays nullable | `raiseEventForSignedCall` returns null from its catch (AnalyticsManager.kt:875-885) |
| `CleverTapAPI.pushGeoFenceExitedEvent` (CleverTapAPI.kt:1613) | stays nullable | `raiseEventForGeofences` returns null from its catch (AnalyticsManager.kt:888) |
| `CleverTapAPI.pushGeofenceEnteredEvent` (CleverTapAPI.kt:1623) | stays nullable | same as `pushGeoFenceExitedEvent` |
| `CleverTapAPI.setLocationForGeofences` (CleverTapAPI.kt:1878) | stays nullable | `LocationManager.updateLocation` returns null for a null location and on two early returns (LocationManager.kt:52-70) |
| `CleverTapAPI.renderPushNotification` (CleverTapAPI.kt:2164) | stays nullable | returns null from its catch |
| `CleverTapAPI.defineVariable` (CleverTapAPI.kt:2241) | stays nullable | `Var.define` returns null for an invalid name or a null non-file default (Var.kt:348-356) |
| `CleverTapAPI.defineFileVariable` (CleverTapAPI.kt:2252) | stays nullable | same as `defineVariable` |
| `CleverTapAPI.getVariableValue` (CleverTapAPI.kt:2279) | stays nullable | null for a null name or an unknown variable |
| `CleverTapAPI.getVariable` (CleverTapAPI.kt:2287) | stays nullable | null for a null name or an unknown variable |
| `CleverTapAPI.getLocale` (CleverTapAPI.kt:2464) | stays nullable | `DeviceInfo.customLocale` is null unless set (DeviceInfo.kt:264) |
| `CleverTapAPI.getDefaultInstance` (CleverTapAPI.kt:3088) | stays nullable | null when no default config can be built (no account id/token in the manifest) |
| `CleverTapAPI.instanceWithConfig` (CleverTapAPI.kt:3208) | stays nullable | returns null for a null config |
| `CleverTapAPI.getNotificationHandler` (CleverTapAPI.kt:3455) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapAPI.getSignedCallNotificationHandler` (CleverTapAPI.kt:3463) | stays nullable | listener/handler getter: null until the app registers one |
| `CleverTapInstanceConfig.getAccountRegion` (CleverTapInstanceConfig.kt:290) | stays nullable | unset by default (`= null`) and restored from a Parcel/JSON |
| `CleverTapInstanceConfig.getProxyDomain` (CleverTapInstanceConfig.kt:356) | stays nullable | unset by default (`= null`) and restored from a Parcel/JSON |
| `CleverTapInstanceConfig.getSpikyProxyDomain` (CleverTapInstanceConfig.kt:377) | stays nullable | unset by default (`= null`) and restored from a Parcel/JSON |
| `CleverTapInstanceConfig.getCustomHandshakeDomain` (CleverTapInstanceConfig.kt:399) | stays nullable | unset by default (`= null`) and restored from a Parcel/JSON |
| `CleverTapInstanceConfig.getFcmSenderId` (CleverTapInstanceConfig.kt:430) | stays nullable | unset by default (`= null`) and restored from a Parcel/JSON |
| `CleverTapInstanceConfig.getPackageName` (CleverTapInstanceConfig.kt:452) | stays nullable | unset by default (`= null`) and restored from a Parcel/JSON |
| `CleverTapInstanceConfig.getIdentityKeys` (CleverTapInstanceConfig.kt:461) | stays nullable | assigned from a Parcel `createStringArray()` (nullable) and from the manifest |
| `CleverTapInstanceConfig.getDefaultInstance` (CleverTapInstanceConfig.kt:809) | stays nullable | null when the manifest has no account id/token |
| `CleverTapInstanceConfig.createInstance` (CleverTapInstanceConfig.kt:832) | stays nullable | returns null for a null account id or token (CleverTapInstanceConfig.kt:850-852) |
| `CoreMetaData.getLocationFromUser` (CoreMetaData.kt:50) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getAppInboxActivity` (CoreMetaData.kt:84) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getScreenName` (CoreMetaData.kt:90) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getCampaign` (CoreMetaData.kt:120) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getMedium` (CoreMetaData.kt:142) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getSource` (CoreMetaData.kt:154) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getWzrkParams` (CoreMetaData.kt:165) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getCurrentActivity` (CoreMetaData.kt:222) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `CoreMetaData.getCurrentActivityName` (CoreMetaData.kt:237) | stays nullable | state that is null until set (activity, UTM, location, screen) |
| `LocalDataStore.readUserEventLog` (LocalDataStore.kt:219) | stays nullable | DB/profile read: null for unknown keys or when the device id is not yet known |
| `LocalDataStore.getProfileProperty` (LocalDataStore.kt:294) | stays nullable | DB/profile read: null for unknown keys or when the device id is not yet known |
| `ManifestInfo.getAccountId` (ManifestInfo.kt:18) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getNotificationIcon` (ManifestInfo.kt:26) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getExcludedActivities` (ManifestInfo.kt:27) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getIntentServiceName` (ManifestInfo.kt:35) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getDevDefaultPushChannelId` (ManifestInfo.kt:36) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getProfileKeys` (ManifestInfo.kt:37) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getVendorOneProvider` (ManifestInfo.kt:39) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getVendorTwoProvider` (ManifestInfo.kt:40) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `ManifestInfo.getEncryptionInTransit` (ManifestInfo.kt:41) | stays nullable | public constructor takes it nullable, and manifest meta-data may be absent |
| `UTMDetail.getCampaign` (UTMDetail.kt:21) | stays nullable | settable to null; null when the URI has no such parameter |
| `UTMDetail.getMedium` (UTMDetail.kt:42) | stays nullable | settable to null; null when the URI has no such parameter |
| `UTMDetail.getSource` (UTMDetail.kt:63) | stays nullable | settable to null; null when the URI has no such parameter |
| `Utils.convertToTitleCase` (Utils.kt) | stays nullable | returns the input for null/empty input |
| `Utils.getBitmapFromURL` (Utils.kt) | stays nullable | `DownloadedBitmap.bitmap` is null on a failed download |
| `Utils.optionalStringKey` (Utils.kt) | stays nullable | returns null when the key is absent |
| `Utils.readAssetFile` (Utils.kt) | stays nullable | `Scanner.next()` is an un-annotated platform value; non-null only by assertion |
| `Utils.getNormalizedName` (Utils.kt) | stays nullable | returns null for a null name |
| `CleverTapDisplayUnit.getBgColor` (CleverTapDisplayUnit.kt:69) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getContents` (CleverTapDisplayUnit.kt:80) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getCustomExtras` (CleverTapDisplayUnit.kt:91) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getError` (CleverTapDisplayUnit.kt:125) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getJsonObject` (CleverTapDisplayUnit.kt:136) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getType` (CleverTapDisplayUnit.kt:147) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getUnitID` (CleverTapDisplayUnit.kt:158) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnit.getWZRKFields` (CleverTapDisplayUnit.kt:169) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getActionUrl` (CleverTapDisplayUnitContent.kt:59) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getContentType` (CleverTapDisplayUnitContent.kt:72) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getError` (CleverTapDisplayUnitContent.kt:88) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getIcon` (CleverTapDisplayUnitContent.kt:99) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getMedia` (CleverTapDisplayUnitContent.kt:110) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getMessage` (CleverTapDisplayUnitContent.kt:133) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getMessageColor` (CleverTapDisplayUnitContent.kt:144) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getPosterUrl` (CleverTapDisplayUnitContent.kt:155) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getTitle` (CleverTapDisplayUnitContent.kt:166) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `CleverTapDisplayUnitContent.getTitleColor` (CleverTapDisplayUnitContent.kt:177) | stays nullable | constructor/Parcel field is nullable (bad or partial payload) |
| `EventDetail.getName` (EventDetail.kt:51) | stays nullable | constructor parameter is nullable |
| `CTFeatureFlagsController.getGuid` (CTFeatureFlagsController.kt:43) | stays nullable | constructor parameter is nullable |
| `CTFeatureFlagsController.get` (CTFeatureFlagsController.kt:84) | stays nullable | never null, but `Boolean`/`Double`/`Long` non-null compiles to a primitive and would change the JVM descriptor |
| `CTInboxMessage.getActionUrl` (CTInboxMessage.kt:26) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getBgColor` (CTInboxMessage.kt:41) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getBody` (CTInboxMessage.kt:57) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getCampaignId` (CTInboxMessage.kt:70) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getImageUrl` (CTInboxMessage.kt:140) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getOrientation` (CTInboxMessage.kt:182) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getTitle` (CTInboxMessage.kt:211) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessage.getType` (CTInboxMessage.kt:224) | stays nullable | field starts null and stays null when the payload lacks it (some are Parcel-only) |
| `CTInboxMessageContent.getActionUrl` (CTInboxMessageContent.kt:21) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getContentType` (CTInboxMessageContent.kt:32) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getIcon` (CTInboxMessageContent.kt:47) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getIconContentDescription` (CTInboxMessageContent.kt:58) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinks` (CTInboxMessageContent.kt:69) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getMedia` (CTInboxMessageContent.kt:80) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getMediaContentDescription` (CTInboxMessageContent.kt:91) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getMessage` (CTInboxMessageContent.kt:102) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getMessageColor` (CTInboxMessageContent.kt:113) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getPosterUrl` (CTInboxMessageContent.kt:124) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getTitle` (CTInboxMessageContent.kt:145) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getTitleColor` (CTInboxMessageContent.kt:156) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinkBGColor` (CTInboxMessageContent.kt:193) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinkColor` (CTInboxMessageContent.kt:215) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinkKeyValue` (CTInboxMessageContent.kt:262) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinkText` (CTInboxMessageContent.kt:288) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinkUrl` (CTInboxMessageContent.kt:311) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `CTInboxMessageContent.getLinktype` (CTInboxMessageContent.kt:348) | stays nullable | field starts null, or the method returns null for null/missing JSON or from its catch |
| `INotificationParser.toBundle` (INotificationParser.kt:18) | stays nullable | interface contract: implementations return null for non-CleverTap messages |
| `CTProductConfigController.getBoolean` (CTProductConfigController.kt:173) | stays nullable | never null, but `Boolean`/`Double`/`Long` non-null compiles to a primitive and would change the JVM descriptor |
| `CTProductConfigController.getDouble` (CTProductConfigController.kt:187) | stays nullable | never null, but `Boolean`/`Double`/`Long` non-null compiles to a primitive and would change the JVM descriptor |
| `CTProductConfigController.getLong` (CTProductConfigController.kt:216) | stays nullable | never null, but `Boolean`/`Double`/`Long` non-null compiles to a primitive and would change the JVM descriptor |
| `ProductConfigSettings.getGuid` (ProductConfigSettings.kt:71) | stays nullable | constructor parameter is nullable |
| `LaunchPendingIntentFactory.getLaunchPendingIntent` (LaunchPendingIntentFactory.kt:34) | stays nullable | returns null without a launch intent and deep link (documented) |
| `LaunchPendingIntentFactory.getActivityIntent` (LaunchPendingIntentFactory.kt:50) | stays nullable | returns null without a launch intent and deep link (documented) |
| `PushType.getCtProviderClassName` (PushType.kt:28) | stays nullable | constructor parameter is nullable |
| `PushType.getMessagingSDKClassName` (PushType.kt:42) | stays nullable | constructor parameter is nullable |
| `PushType.toJSONObject` (PushType.kt:84) | stays nullable | returns null from its catch |
| `PushType.fromJSONObject` (PushType.kt:120) | stays nullable | returns null for null input / on error |
| `CTVariableUtils.mergeHelper` (CTVariableUtils.kt:140) | stays nullable | returns null by design for unknown kinds / missing keys |
| `CTVariableUtils.traverse` (CTVariableUtils.kt:203) | stays nullable | returns null by design for unknown kinds / missing keys |
| `CTVariableUtils.kindFromValue` (CTVariableUtils.kt:224) | stays nullable | returns null by design for unknown kinds / missing keys |
| `Var.name` (Var.kt:160) | **non-null** | backing `private val name: String` (Var.kt:17) |
| `Var.nameComponents` (Var.kt:167) | **non-null** | backing `private val nameComponents: Array<String>` (Var.kt:18) |
| `Var.kind` (Var.kt:174) | stays nullable | `kind` is null when `kindFromValue` does not recognise the default (CTVariableUtils `else -> null`) |
| `Var.numberValue` (Var.kt:259) | stays nullable | null for non-numeric variables |
| `Var.stringValue` (Var.kt:270) | stays nullable | null for a map or null value, and for a file that is not cached |

#### Consumer link check (CI guard)

`apiCheck` compares erased descriptors of the public API, so it cannot see a `static` method moving
into a `Companion` of a `@RestrictTo` class, a class becoming `final`, or a restricted field changing
type. Those still crash separately published artifacts that were compiled against an older SDK.
`config/api-interop/consumer_link_check.py` closes that gap:

- **Inputs:** `config/api-interop/consumers.txt` lists the shipped artifacts (Signed Call 0.0.7.3 -
  0.0.9.0, push-templates 2.0.0 - 2.7.0, hms 1.3.4 - 1.5.1, geofence 1.0.0 - 1.4.0, leanplum-core
  7.8.0). clevertap-native-ui-kit is not on Maven Central and is not listed. An older release of an
  SDK module is checked with that module's fresh classes left out (`replaces=`).
- **Check:** every class reference and every `Fieldref` / `Methodref` / `InterfaceMethodref` from a
  consumer into an SDK package is resolved against the freshly built release classes the way the JVM
  does (JVMS 5.4.3), together with the opcode that uses it: missing class or member, static vs
  instance (`invokestatic` / `getstatic` on an instance member and the reverse), Methodref vs
  InterfaceMethodref owner kind, private / package-private / protected access, writes to final
  fields, extending a final class, overriding a final method, and abstract methods a concrete
  consumer class leaves unimplemented.
- **Baseline:** `config/api-interop/consumer_link_baseline.txt` holds only findings that also fail
  against the pre-migration Java SDK (old hms / Signed Call releases built against the removed
  `PushConstants$PushType` API, and Leanplum's `LocalDataStore.updateProfileFields`). Any other
  finding fails the check.
- **Run:** CI (`.github/mini_flows/codechecks_detekt/action.yml`) downloads the artifacts with
  `config/api-interop/fetch_consumers.sh` (curl, Maven Central) and runs
  `./gradlew consumerLinkCheck -PconsumerJarsDir=build/consumer-jars`. Locally,
  `./gradlew consumerLinkCheck` downloads them itself; where Maven Central is not reachable, pass
  `-PconsumerJarsDir=<dir>` with pre-downloaded `<artifact>-<version>.aar` files (add
  `-PconsumerAllowMissing` to skip absent ones), or call the script directly:
  `python3 config/api-interop/consumer_link_check.py --repo . --jars-dir <dir> [--allow-missing]`.
- **Proof it works:** against the 9.0.0 build before SDK-6395 it reports the `ManifestInfo.getInstance`
  (Signed Call, every push-templates 2.x) and `DeviceInfo.getAppIconAsIntId` (Signed Call) breaks;
  after SDK-6395 it reports none beyond the baseline.

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
- Found while writing the customer KDoc (SDK-6388), documented as-is:
  - `CTInboxMessage.getTitle/getBody/getImageUrl/getActionUrl` and `CTInAppNotification.actionExtras` /
    `landscapeImageUrl` are only restored from a Parcel and never set by the JSON parser, so they are
    always `null` on messages the SDK builds.
  - The SDK never calls `SyncListener.profileDataUpdated`.
  - `CTInboxMessage.getExpires()` falls back to now + 24h **in milliseconds** when `wzrk_ttl` is
    absent, while the payload value is in seconds.
  - `TemplateRenderer.debugLevel` has two stacked KDoc blocks.

### Small intentional differences

- `CTHmsMessageHandler.createNotification(ctx, null)` returns `false` without calling the parser
  (its "Invalid Notification Message" debug log no longer appears).
- `Logger` asserts non-null messages before `android.util.Log` (Logcat already NPEs on null on a
  device; under Robolectric this is new).
- Unit tests use `kotlin.test`; hamcrest stays only for `samePropertyValuesAs` (5 sites: no
  `kotlin.test` equivalent, compared types have no value `equals()`).
