# CleverTap Android SDK — Engineering Guide (CLAUDE.md)

Orientation for working in this repo. **Keep this file lean** — it is loaded into *every* session.
Deep, file-referenced knowledge lives in the specialized agents under `.claude/agents/`; those
bodies load only when the agent is spawned (in its own context), so put detail there, not here.
References use files/classes/methods, not line numbers (which drift) — grep for exact locations.

## Modules (`settings.gradle`)
- `clevertap-core` — the SDK; package root `com.clevertap.android.sdk`. Everything below lives here
  unless noted.
- `clevertap-hms` — Huawei (HMS) push plugin. `clevertap-pushtemplates` — rich push templates.
  `clevertap-geofence` — geofencing. `sample` / `instantapp` — demo apps. `test_shared` — shared
  Robolectric/MockK test utilities.

Versions in `gradle/libs.versions.toml`: **SDK 8.3.0, minSdk 23, compileSdk 36**, Java 8 / Kotlin
JVM 1.8. Codebase is a **Java + Kotlin mix** (older core/public API Java; newer subsystems Kotlin).
Match the language and style of the file you edit.

## Build / test / quality
```bash
./gradlew :clevertap-core:assemble                             # build AAR
./gradlew :clevertap-core:test                                 # unit tests (Robolectric, JVM)
./gradlew :clevertap-core:test --tests "*AnalyticsManagerTest" # single class
./gradlew check                                                # tests + lint + checkstyle + detekt
```
Tests: `clevertap-core/src/test` (JUnit4, Robolectric, MockK, JSONAssert, Awaitility); helpers
(`TestClock`, dispatchers, mock managers) in `test_shared/`. Style: Checkstyle (Java,
`config/checkstyle/`) + Detekt (Kotlin, `config/detekt/`), wired via `gradle-scripts/`.
Work on `develop`; PRs target `develop`. Update the relevant `docs/*CHANGELOG.md` for user-facing changes.

## Architecture in one screen
- `CleverTapAPI` — public facade + static registry of instances (keyed by account id). Default
  instance reads creds from the manifest (`ManifestInfo`); more instances from `CleverTapInstanceConfig`.
- `CleverTapFactory.getCoreState(...)` — the **assembly root**: builds and wires every manager,
  returns an immutable `CoreState`. Start here to trace any dependency.
- `ControllerManager` (optional/lazy controllers), `CoreMetaData` (runtime state; static fields are
  process-global, instance fields per-account), `Constants` (event names, storage keys, `wzrk_*`,
  event-type codes — look up by name, don't hardcode).

### The CleverTap thread (central invariant)
All mutable work for an account is **serialized on one per-account single-threaded executor** — this
is how thread-safety is achieved with minimal locking.
- `task/CTExecutors` (one per account via `CTExecutorFactory`): `postAsyncSafelyTask()` → the **CT
  thread** (state, event DB, session, in-app eval, flush orchestration); `ioTask()` → IO pool
  (parallel I/O, no shared-state mutation); callbacks default to the **main thread**.
- `task/PostAsyncSafelyExecutor` runs synchronously when already on the CT thread (re-entrant).
- **Rules:** mutate state/DB/session → CT thread; touch UI (in-app display, callbacks) → main
  thread; heavy parallel I/O → IO pool. Different accounts run on independent threads — never assume
  cross-instance ordering.
- Lifecycle: `ActivityLifecycleCallback` (one process-wide registration) → `ActivityLifeCycleManager`
  (per-instance reactions, posted to the CT thread).

### Persistence & crypto primitives
- `StorageHelper` — SharedPreferences facade; **keys are account-namespaced** (`rawKey:accountId`).
- `store/` (`StoreProvider`/`StoreRegistry`) — typed per-feature stores. `db/DBAdapter`+`db/dao/*`
  via `DBManager` — SQLite (access through DAOs, never raw tables; PII encrypted at the DB layer).
- `cryption/` — `CryptHandler` (AES / AES-GCM), `EncryptionLevel` (NONE/MEDIUM=PII/FULL), migrations.

### Event → network → response spine
Record (`AnalyticsManager` + `validation/`) → queue (`events/EventQueueManager` → `db/`, under
`CTLockManager.eventLock`, kicks in-app eval + debounced flush) → flush (`network/NetworkManager`:
batches ≤50, `QueueHeaderBuilder`, domain handshake) → respond (`response/ClevertapResponseHandler`
runs an **ordered chain** of per-feature processors) → cleanup (delete sent by max-id; retry on
failure). Full detail in **ct-networking**.

## Product verticals → agents
Invoke the matching agent when a task is scoped to a vertical (its body carries the file-level detail):

| Agent | Area (packages) |
|---|---|
| `ct-architecture` | wiring/`CleverTapFactory`/`CoreState`, threading (`task/`), lifecycle, storage/crypto |
| `ct-analytics` | `AnalyticsManager`, `events/`, profiles/`login/`, sessions, `LocalDataStore`, `usereventlogs/` |
| `ct-networking` | `network/`, `db/`, the `response/` chain |
| `ct-inapps` | `inapp/` — type catalog (HTML/native/image-only/partial/alert/custom-code/PIP), triggers, FC, custom & system templates |
| `ct-variables` | `variables/` — define/parse/fetch/sync, `VarCache`, file vars, callbacks |
| `ct-native-display` | `displayunits/` — controller/cache, element-click, `DisplayUnitResponse` |
| `ct-inbox` | `inbox/` — controller, DB, message types, UI, v2 cross-device sync |
| `ct-push` | `pushnotification/` + `clevertap-hms` + `clevertap-pushtemplates` + `clevertap-geofence` — providers (FCM/HMS), rendering, tokens, push-amp, template types |

Adjacent/legacy: `product_config/`, `featureFlags/` (separate from Variables), `leanplum/` (migration shim).

## Conventions & gotchas
- **Respect the thread model** — decide CT thread vs IO vs main before adding logic; follow the
  nearest existing method's pattern.
- **Multi-instance safety** — namespace every new persisted key by account; scrutinize new static state.
- **Don't bypass layers** — `StorageHelper`/stores for prefs, DAOs via `DBManager` for SQLite,
  `CryptHandler` for PII, the `validation/` pipeline for event data.
- **`wzrk_*` is server-controlled attribution** — on merges, server values win; strip user-supplied
  `wzrk_*` keys. Reuse constants from `Constants.java`; never invent parallel keys.
- **Preserve migrations** — storage formats, encryption levels, in-app CS/SS modes all have upgrade paths.
