# CleverTap Android SDK — Engineering Guide (CLAUDE.md)

This file orients Claude Code (and humans) in the CleverTap Android SDK monorepo. It covers
the module layout, the build/test workflow, the core architecture, the **single-threaded
per-instance processing model** ("the CleverTap thread"), and a map of each product vertical.
Deep, vertical-specific knowledge lives in the specialized agents under `.claude/agents/` —
see [Specialized agents](#specialized-agents).

> Line numbers drift; this guide references **files, classes, and methods**, not line numbers.
> When you need exact locations, grep for the symbol.

---

## 1. Repository layout

Gradle modules (see `settings.gradle`):

| Module | Purpose |
|---|---|
| `clevertap-core` | The SDK. Everything below lives here unless noted. Package root: `com.clevertap.android.sdk`. |
| `clevertap-hms` | Huawei (HMS) push provider plugin. |
| `clevertap-pushtemplates` | Rich push notification templates (carousel, rating, timer, etc.). |
| `clevertap-geofence` | Geofencing (Google Play Services location) integration. |
| `sample` | Demo app used for manual testing. |
| `instantapp` | Instant-app sample variant. |
| `test_shared` | Shared Robolectric/MockK test utilities consumed by module `src/test`. |

Docs live in `docs/` (feature guides + per-module changelogs, e.g. `docs/CTCORECHANGELOG.md`,
`docs/Variables.md`, `docs/CTPUSHTEMPLATES.md`). `templates/` holds the source for those docs.

Versions are centralized in `gradle/libs.versions.toml`:
- **SDK version: `8.3.0`** (`clevertap_android_sdk`)
- **minSdk 23, compileSdk 36**
- Java 8 source/target; Kotlin JVM target 1.8. Codebase is a **Java + Kotlin mix**
  (older core/public API in Java; newer subsystems in Kotlin).

---

## 2. Build, test, and quality

```bash
# Build the core AAR
./gradlew :clevertap-core:assemble

# Unit tests (Robolectric — run on the JVM, no device needed; default emulated SDK 29)
./gradlew :clevertap-core:test
./gradlew :clevertap-core:test --tests "*AnalyticsManagerTest"   # single class

# Everything: tests + lint + checkstyle + detekt
./gradlew check
```

- **Tests**: `clevertap-core/src/test/...` (~190+ test classes). Stack: JUnit4, Robolectric,
  MockK, JSONAssert, Awaitility, coroutines-test. Shared helpers (`TestClock`, mock managers,
  test dispatchers) live in `test_shared/`. Robolectric SDK level is set in
  `test_shared/robolectric.properties`.
- **Style/quality**: Checkstyle for Java (`config/checkstyle/`), Detekt for Kotlin
  (`config/detekt/detekt.yml`), wired via `gradle-scripts/{checkstyle,detekt,commons,jacoco_root}.gradle`.
  `gradle-scripts/commons.gradle` is applied to every library module and derives `versionCode`/
  `versionName` and `buildConfigField`s from the catalog version.
- Prefer matching the **language and style of the file you're editing**. Don't rewrite Java to
  Kotlin (or vice versa) opportunistically.

### Branch/PR conventions
Default working branch is `develop`; releases flow to `master`. PRs target `develop`
(`on_pr_from_task_to_develop.yml`) unless it's a release PR. Update the relevant changelog in
`docs/` for user-facing changes.

---

## 3. Core architecture

### 3.1 Instance model
`CleverTapAPI` is the public facade and the static registry of instances (keyed by account ID).
There is a **default instance** (credentials from `AndroidManifest.xml` via `ManifestInfo`) and
**additional instances** created from a `CleverTapInstanceConfig`. Each account is fully isolated:
its own executor, database, and namespaced storage.

- `CleverTapAPI` — public API + static instance map.
- `CleverTapInstanceConfig` — immutable, Parcelable per-instance config (account id/token/region,
  encryption level, analyticsOnly, etc.).
- `CleverTapFactory.getCoreState(...)` — the **assembly root**. Constructs and wires every manager
  and returns an immutable `CoreState`. This is the single place dependencies are graphed; start
  here to understand what talks to what.
- `CoreState` (Kotlin) — the immutable dependency container held by each `CleverTapAPI` instance.
- `ControllerManager` — aggregates optional/lazy subsystem controllers (in-app, inbox, feature
  flags, product config, display unit cache).
- `CoreMetaData` — mutable runtime state. **Static** fields (`appForeground`, `currentActivity`)
  are process-global; instance fields (session id, app-launch-pushed flag, attribution params)
  are per-instance.
- `ManifestInfo` — lazy singleton parse of the manifest (account creds, icons, region/proxy).
- `DeviceInfo` — device characteristics, cached once and treated as immutable.
- `Constants` — SDK-wide constants (event names, storage keys, `wzrk_*` payload keys, event type
  codes). **Look up constants by name here rather than hardcoding values.**

### 3.2 The CleverTap thread (READ THIS)
The SDK's central invariant: **all mutable work for an account is serialized onto a single
per-account thread.** This is how thread-safety is achieved with minimal locking.

- `task/CTExecutorFactory` — returns one cached `CTExecutors` per account id.
- `task/CTExecutors` — exposes the task types:
  - `postAsyncSafelyTask()` → the **CT thread**: a single-threaded executor, one per account.
    Almost all state mutation (event recording, DB writes, session logic, in-app evaluation,
    network flush orchestration) runs here.
  - `ioTask()` → `IOExecutor`, a multi-threaded pool for parallel I/O (disk, downloads) that
    does **not** mutate shared SDK state.
  - callbacks default to `MainThreadExecutor` (main looper).
- `task/PostAsyncSafelyExecutor` — the single-thread executor. Key optimization: if you're
  **already on the CT thread**, tasks run **synchronously** (re-entrant) instead of re-queuing.
- `task/Task` — chainable wrapper: `.execute(tag, Callable)` runs on the task executor;
  `.addOnSuccessListener/.addOnFailureListener` deliver on the **main thread** by default.
- `task/MainLooperHandler` — internal main-thread handler (used e.g. to schedule delayed flushes).

**Rules of thumb**
- Anything mutating SDK state, touching the event DB, or reading/writing session state **must**
  run via `postAsyncSafelyTask()`.
- Anything touching the UI (in-app display, activity launch, user callbacks) **must** end up on
  the **main thread**.
- Heavy parallel I/O with no shared-state mutation → `ioTask()`.
- Multiple accounts run on independent CT threads → they are genuinely parallel; never assume
  cross-instance ordering.

### 3.3 Lifecycle
- `ActivityLifecycleCallback.register(app)` (or extending the provided `Application`) registers a
  single process-wide `ActivityLifecycleCallbacks`; it fans out to static `CleverTapAPI` hooks for
  all instances.
- `ActivityLifeCycleManager` — per-instance reactions (session start/close, app-launched event,
  show pending in-apps, push initial events), all posted to the CT thread.

### 3.4 Persistence primitives
- `StorageHelper` (Kotlin) — SharedPreferences facade. **Keys are namespaced per account**
  (`rawKey:accountId`); default instance falls back to the unsuffixed key.
- `store/` package — typed per-feature stores via `StoreProvider`/`StoreRegistry`
  (`InAppStore`, `ImpressionStore`, `LegacyInAppStore`, assets/files stores). Preference names are
  suffixed with `deviceId`+`accountId`.
- `db/DBAdapter` (+ `db/dao/*`) — SQLite. Facade over per-table DAOs (events, profile events,
  push-notification-viewed, user profiles, inbox, push ids, uninstall ts, user event logs).
  Access via `DBManager`/`DBAdapter`, never raw tables. PII columns are encrypted at the DB layer.
- `cryption/` — `CryptHandler` (AES / AES-GCM), `EncryptionLevel` (NONE / MEDIUM=PII / FULL),
  `CryptMigrator` for level/algorithm upgrades.

---

## 4. The event → network → response loop

This is the spine that most verticals hang off of. Full detail is in the **ct-analytics** and
**ct-networking** agents; the shape:

1. **Record** — `AnalyticsManager` (public event/profile APIs) validates + normalizes via the
   `validation/` pipeline and hands off on the CT thread.
2. **Queue** — `events/EventQueueManager` writes the event to SQLite (`db/`) under
   `CTLockManager.eventLock`, kicks **in-app evaluation**, and schedules a (debounced) flush.
3. **Flush** — `network/NetworkManager.flushDBQueue()` pulls batches (≤50), builds the request
   header (`QueueHeaderBuilder`: device id, `_i`/`_j`, ARP, identity, FC counts…), performs a
   domain **handshake** if needed, and POSTs.
4. **Respond** — `response/ClevertapResponseHandler` runs the parsed response through an ordered
   **chain of response processors**, each owning one feature slice. Current order (from
   `CleverTapFactory`): `InAppResponse` → `MetadataResponse` → `ARPResponse` → `ConsoleResponse`
   → `InboxResponse` → `InboxV2Response` → `PushAmpResponse` → `FetchVariablesResponse` →
   `DisplayUnitResponse` → `FeatureFlagResponse` → `ProductConfigResponse` → `GeofenceResponse` →
   `ContentFetchResponse`.
5. **Cleanup** — sent events are deleted by max-id; on failure the batch stays for retry with
   backoff. Mute (`X-WZRK-MUTE`) and domain-change headers can abort/redirect a flush.

Key cross-cutting gotchas: domain caching + `needsHandshakeForDomain`, region/proxy overrides,
`isUserSwitchFlush` (filters inbox/display-unit/variables processors), and the separate
push-notification-viewed queue/domain (spiky).

---

## 5. Product verticals (map)

Each vertical has a dedicated agent (below) with file-level detail. Quick index:

- **Analytics / events / profiles / sessions / identity** — `AnalyticsManager`, `events/`,
  `LocalDataStore`, `usereventlogs/`, `profile/`, `login/`, `SessionManager`. → **ct-analytics**
- **Networking / queue / DB / response chain** — `network/`, `db/`, `response/`. → **ct-networking**
- **In-app notifications** — `inapp/` (controller, fragments, `evaluation/` triggers & limits,
  `store/`, `customtemplates/`), `InAppFCManager`, `InAppNotificationActivity`. → **ct-inapps**
- **Variables / remote config** — `variables/` (`CTVariables`, `Var`, `VarCache`, `Parser`),
  `FetchVariablesResponse`. → **ct-variables**
- **Native Display (Display Units)** — `displayunits/` (`CTDisplayUnitController`, `DisplayUnitCache`,
  models), `DisplayUnitResponse`. → **ct-native-display**
- **App Inbox** — `inbox/` (`CTInboxController`, `CTInboxActivity`, messages), `InboxResponse`/
  `InboxV2Response` (v2 cross-device sync). → **ct-inbox**
- **Push notifications** — `pushnotification/` (`PushProviders`, renderers, FCM), plus modules
  `clevertap-hms`, `clevertap-pushtemplates`, and `clevertap-geofence`.
  → **ct-push**
- Adjacent/legacy: `product_config/`, `featureFlags/` (separate from Variables), `leanplum/`
  (Leanplum→CleverTap migration shim).

---

## 6. Working conventions & gotchas

- **Respect the thread model.** Before adding logic, decide: CT thread (state), IO pool (I/O), or
  main (UI/callbacks). Wrong thread = races or ANRs. When in doubt, follow the pattern in the
  nearest existing method.
- **Multi-instance safety.** Any new persisted key must be account-namespaced. Any new static
  field is process-global — think twice.
- **Don't bypass the layers.** Use `StorageHelper`/stores for prefs, DAOs via `DBManager` for
  SQLite, `CryptHandler` for PII, the `validation/` pipeline for event data.
- **Constants live in `Constants.java`** (and per-module constants files). Reuse `wzrk_*` keys;
  never invent parallel ones.
- **`wzrk_*` is server-controlled attribution.** When merging user-supplied properties with
  cached campaign fields, server `wzrk_*` values take precedence and user `wzrk_*` keys are
  stripped.
- **Backward compatibility matters.** Storage formats, encryption levels, and in-app CS/SS modes
  all have migration paths — preserve them.

---

## 7. Specialized agents

Fine-grained, file-referenced knowledge per area lives in `.claude/agents/`. Invoke the matching
agent when a task is scoped to that vertical:

| Agent | Use when working on… |
|---|---|
| `ct-architecture` | Core wiring, `CleverTapFactory`/`CoreState`, threading/executors, lifecycle, storage/crypto primitives. |
| `ct-analytics` | Events, charged events, profiles, identity/login, sessions, user event logs, `LocalDataStore`. |
| `ct-networking` | Queue flush, batching, handshake/domain/region, headers/ARP, DB queue tables, the response chain. |
| `ct-inapps` | In-app notifications: types/fragments, triggers & limits evaluation, frequency capping, custom templates, push primer. |
| `ct-variables` | Variables/remote config: define/parse/fetch/sync, `VarCache` merge/diff, file variables, callbacks. |
| `ct-native-display` | Display Units: controller/cache, element-click events, `DisplayUnitResponse`, callbacks. |
| `ct-inbox` | App Inbox: controller, DB persistence, message types, `CTInboxActivity` UI, v2 cross-device sync. |
| `ct-push` | Push across core + HMS + push templates + geofence: provider plugin model, rendering, tokens, push-amp. |

These agents are read/analysis-oriented by default; they still edit code when a task requires it.
