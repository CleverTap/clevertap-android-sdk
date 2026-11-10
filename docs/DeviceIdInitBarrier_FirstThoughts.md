# Device ID init barrier — first thoughts (SDK-6254)

> Status: **thinking out loud, not a design.** This is a working note capturing the
> problem, what the code actually does today, and a *candidate* direction. Nothing here
> is decided. Open questions are left open on purpose. Expect this to be wrong in places
> until the consumer/queue audit (below) is done.

Branch: `feat/SDK-6254/device-id-duplicate-instance-sync`
Epic: SDK Infra Q4'26 (SDK-6211)

---

## 1. The itch

Two complaints:

- **"SDK instances are not synchronised."**
- **"The `deviceIdCreated` callback is a code smell — it blocks correct init and spreads nullables."**

Initial hunch was "one root cause." After the init-path survey (§8) it looks like **two
distinct problems that happen to collide**:

1. **The init barrier problem** — device ID is resolved *out of band* from the rest of init,
   so device-scoped components are built empty and patched later (§2–§7).
2. **The duplicate-instance problem** — the static instance registry + factory are **not
   thread-safe**, and instances get created from several threads at once, so the same
   accountId can be constructed twice (§8). This is almost certainly the literal "device id
   duplicate instance" in the ticket title.

They collide because the device-ID callback re-fetches its instance through the same
unsynchronised factory (`DeviceInfo.java:734` → `instanceWithConfig`), so a duplicate means
the callback can fire on a *different* object than the one the app holds. **Problem 2
probably has to be fixed first** — the §4 barrier assumes one instance (one serial queue) per
account; duplicates give you two serial queues and the barrier guarantees nothing.

---

## 2. What actually happens today (grounded, not from memory)

- `CleverTapAPI` constructor → `CleverTapFactory.getCoreState()` runs **synchronously on the
  caller thread** (usually the app main thread in `onCreate`) and must return a `CoreState`
  right away.
- Inside it, device ID resolution is kicked off async: `DeviceInfo.onInitDeviceInfo()`
  posts an `ioTask()` whose success listener defaults to **`MAIN_EXECUTOR`**
  (`CTExecutors.java:25`). So the completion — `CleverTapAPI.deviceIDCreated()` — runs
  **on the main thread**, reached via a static `instanceWithConfig(ctx, config)` back-ref
  (`DeviceInfo.java:734`).
- `deviceIDCreated()` (`CleverTapAPI.java:3258-3354`) then *builds* the device-scoped
  components: InAppStore, ImpressionStore, ND stores, InAppFCManager, NdFCManager, and sets
  FeatureFlags / ProductConfig guids.
- But `CleverTapFactory` *also* builds the same stores/FC managers in `taskInitStores` /
  `taskInitFCManager` (`CleverTapFactory.kt:279-339`), guarded `if (deviceID != null)`,
  while `deviceIDCreated` builds them guarded `if (== null)`. **Two build paths, opposite
  guards, whoever-wins race.**

### Why it's async at all (the legitimate part)

Not a mistake. Resolution must stay off the caller thread because it does:
- SharedPreferences disk I/O (StrictMode — the `CleverTapFactory.kt:309` comment says so), and
- on **first launch only**, a blocking GAID fetch (`fetchGoogleAdID()`, reflection + Play
  Services round-trip) when `useGoogleAdId` is on.

So "async" is right. "On the main thread, via a second queue, built twice" is the smell.

### Why it's only *accidentally* on the main thread

`deviceIDCreated()` touches no UI — it builds stores. It's on main purely because `ioTask()`'s
default callback executor is `MAIN_EXECUTOR`. A sibling `ioTaskNonUi()` already exists that
keeps the callback on the IO thread. The code even re-posts to `ioTask()` *again* inside the
main callback to dodge `addChangeUserCallback` ANRs ("`// can cause ANR if called from main
thread`").

---

## 3. The harms this actually causes (not hypothetical)

- **In-apps silently dropped.** `/a1` in-apps arriving before stores exist →
  `InAppResponse.java:111` logs *"Stores are not initialised, ignoring inapps!!!!"* and
  `return`s. No queue, no retry.
- **Login NPE / silent no-op, from a cross-queue race.** Login runs on the per-account
  **serial queue** (`postAsyncSafelyTask`), device-id init runs on **IO + main**. Different
  queues ⇒ no ordering. `LoginController.java:180` calls
  `getInAppFCManager().changeUser(...)` with **no null guard** (NPE if init hasn't built it);
  `notifyChangeUserCallback()` only repoints stores already registered (stale prefs otherwise).
- **Early `getDeviceID()` returns the *fallback* id** (`__i…`, `DeviceInfo.java:562-565`),
  not the real one — any early reader sees a mismatched identity.
- **~10 null-tolerant guards** exist only to survive this window (InAppResponse,
  EvaluationManager `inAppStore?.let`, QueueHeaderBuilder elvis, AnalyticsManager ND,
  FeatureFlags/ProductConfig `isEmpty(guid)`, the factory's `deviceId != null` tasks).

---

## 4. Candidate direction — an "init barrier" on the serial queue

The user's framing (confirmed sound): **resolve device ID synchronously on a background
thread as the first job, then let the rest of init and all feature work follow it.**

Precision that matters: *synchronous on a background thread*, **never** on the caller thread
(blocking the constructor = first-launch GAID ANR).

Shape:

1. **Tier 1 — device-ID-independent** (caller thread, as today): network, DB, executors,
   session, lifecycle, callback manager, templates. None need the ID.
2. **Tier 2 — device-scoped**, as the **head job of the per-account serial queue**
   (`postAsyncSafelyTask` — the same queue login + `asyncStartup` already use). Inside one
   straight-line job: resolve the ID **inline** (call `initDeviceID()` directly — it already
   returns a `String`; drop the `addOnSuccessListener` callback), then build InAppStore,
   ImpressionStore, ND stores, FC managers, set the guids. No callback, no re-entry, no main
   thread.
3. **Everything else is ordered behind that barrier** on the same serial queue. Response
   processing, login, event flush already run there ⇒ they observe non-null, fully-wired
   state. **Serial ordering replaces the null-guards.**

### Structural upgrade worth considering

Make Tier 2 an explicit set-once holder — a `DeviceScopedState` (stores + FC managers +
guids), built once by the barrier job — rather than nulling-then-patching fields on
`CoreState`. Payoff: **login collapses into the same path.** Today `deviceIDCreated()` and
`asyncProfileSwitchUser()` are two divergent re-wiring paths (hence the login race);
"device-id-ready" and "change-user" both become "(re)build/repoint the device-scoped state"
— one mechanism. That *is* the "synchronise instances" goal, falling out of the structure.

### What would get deleted if this holds

- `deviceIDCreated()` and the static `instanceWithConfig(...)` reach-back.
- The duplicate `taskInitStores` / `taskInitFCManager` in `CleverTapFactory`.
- The ~10 null guards + the deferred `{ storeRegistry.impressionStore }` lambdas.

---

## 5. The thing that could sink this — consumer/queue audit (TODO, not done)

The barrier only orders work on *that one* serial queue. Anything reading device-scoped state
from a **different** thread still races and must either sit behind the barrier or keep a guard.
Need to classify every consumer before committing:

- [ ] InAppResponse (response processing) — which queue? (believed serial — confirm)
- [ ] LoginController / onUserLogin — serial (confirmed `postAsyncSafelyTask`)
- [ ] EventQueueManager flush — which queue?
- [ ] QueueHeaderBuilder — runs within flush? confirm
- [ ] In-app **display** (`InAppController`) — main thread; reads stores late — safe or not?
- [ ] AnalyticsManager ND didShow — which thread?
- [ ] Push token / lifecycle paths — must NOT get serialized *behind* a slow first-launch
      GAID fetch (deadlock/stall risk). Confirm they're independent.
- [ ] Any synchronous public getter the app may call on main that needs device-scoped state.

---

## 6. Open questions (deliberately unresolved)

- **Does the caller thread ever legitimately need a resolved device ID before the barrier
  completes?** If yes, the whole model needs a rethink.
- **GAID-as-identity (option B).** `useGoogleAdId` defaults **false**; GAID is already sent as
  a standalone `GoogleAdID` attribute (`CTJsonConverter.java:80-86`), so decoupling it from
  identity is *feasible* — but it changes the `__g` device id for existing GAID installs
  (profile-continuity / attribution risk). Leaning: **out of scope here, separate ticket with
  product sign-off.** The barrier design works under current (A) semantics; B only removes the
  first-launch head-of-line stall.
- **Set-once vs truly non-null.** Can `CoreState` hold these as non-null `val`s, or is a
  set-once holder the most we can get given the caller-thread return? (Leaning holder.)
- **Multi-account.** Separate accountIds = separate serial queues = independent barriers.
  Believed fine; confirm nothing shares device-scoped state across instances.
- **Migration/back-compat.** Public `OnInitCleverTapIDListener` must keep working — rewire it
  to fire on barrier completion.

---

## 7. Possible PR staging (if the direction survives the audit)

1. Device-id callback off main → onto the serial queue (`ioTask`→serial / inline). Behavior-
   preserving; kills the main-thread hop + the cross-queue login race.
2. Collapse to a single build path (delete the factory's duplicate tasks).
3. Device-scoped state as set-once; delete the dead null guards.
4. Delete `deviceIDCreated()` + the static reach-back; unify login onto the same path; rewire
   the public ID listener.

Each independently shippable. Step 1 alone is a real bug fix (login race) even if the rest stalls.

---

## 8. Init-path survey (SDK-wide) — the duplicate-instance problem

Surveyed every `CleverTapAPI` creation path across all modules. Two findings reframe the work.

### 8a. Instance *creation* is already synchronous — but not thread-safe

Every path funnels through `instanceWithConfig(context, config, cleverTapID)`
(`CleverTapAPI.java:970-995`), which does a get-or-construct against a **plain, unsynchronised**
static map:

```java
private static HashMap<String, CleverTapAPI> instances;      // line 156 — NOT synchronized
...
if (instances == null) { instances = new HashMap<>(); }      // line 980-981
CleverTapAPI instance = instances.get(config.getAccountId());// line 984
if (instance == null) {
    instance = new CleverTapAPI(context, config, cleverTapID);// line 986 — construct
    instances.put(config.getAccountId(), instance);          // line 987
}
```

The constructor itself returns fast (field assignments + `asyncStartup()` which only *posts*
tasks; §2). So "make creation synchronous" is mostly a non-goal — **it already is.** The real
defect is that the get→construct→put sequence has **no lock**, and callers hit it from
multiple threads (§8b). `getInstances()` / `setInstances()` are even public
(`CleverTapAPI.java:845-851`).

### 8b. Creation happens from several threads at once

| Site | File:line | Method | Thread |
|---|---|---|---|
| App init | `getDefaultInstance` | `instanceWithConfig` | **Main** |
| InApp activity | `InAppNotificationActivity.java:120` | `instanceWithConfig` | **Main** |
| Inbox activity / fragment | `CTInboxActivity.java:81`, `CTInboxListViewFragment.java:117` | `instanceWithConfig` | **Main** |
| PT receivers | `PTPushNotificationReceiver.java:29`, `PushTemplateReceiver.java:111` | `getGlobalInstance` | **Main (receiver)** |
| FCM receiver | `CTFirebaseMessagingReceiver.java:158` | `getGlobalInstance` | **background** (`new Thread()`) |
| Push handler | `PushNotificationHandler.java:78` | `getGlobalInstance` | **caller/service** |
| Geofence | `geofence/Utils.java:86`, `CTGeofenceBootReceiver.java:69` | `getGlobalInstance` | **background** (`@WorkerThread`) |
| Device-id callback | `DeviceInfo.java:734` | `instanceWithConfig` | **main** (re-fetch of self) |

`getGlobalInstance → fromAccountId → createInstanceIfAvailable` (reads config from
`"instance:"+accountId` SharedPrefs, `CleverTapAPI.java:3444`) can **construct** a fresh
instance, not just look one up. `fromAccountId` *also* checks `instances == null` without a
lock (`line 824`).

### 8c. The race (this is the "duplicate instance")

Cold start: app `onCreate` calls `getDefaultInstance` on **main** while a push arrives and
FCM's background thread calls `getGlobalInstance(accountIdFromBundle)`. Both:
- read `instances == null` (or `.get(accountId) == null`) before either `put`s, then
- each `new CleverTapAPI(...)` for the **same accountId**.

Result: **two live instances per account** — two CoreStates, two DBAdapters, **two serial
queues**, two device-id resolutions. The second `put` wins the map; the app may be holding the
loser. Everything downstream that assumes "one serial queue per account" (including the §4
barrier, and login) is built on sand until this is fixed.

No `ContentProvider` / `androidx.startup` auto-init exists (checked all manifests), so there's
no hidden ordering that saves us — it's purely first-caller-wins, unsynchronised.

### 8d. Implication for sequencing

- **Fix 2 (thread-safe singleton registry) before Fix 1 (barrier).** Options to weigh:
  `ConcurrentHashMap` + `computeIfAbsent` (careful: constructor is heavy-ish and re-entrant via
  `DeviceInfo.java:734` → risk of `computeIfAbsent` recursive-update throw), or a plain
  `synchronized` block / per-accountId lock around get-construct-put. Re-entrancy during
  construction is the thing to prove out.
- Audit `getInstances()`/`setInstances()` callers before changing the map type.
- Confirm whether any caller *depends* on today's lazy `createInstanceIfAvailable` from
  SharedPrefs config (push cold-start multi-instance) — that must keep working.

---

## 9. Revised problem list (post-survey)

| # | Problem | Root cause | Likely fix | Order |
|---|---|---|---|---|
| A | Duplicate instances per account | Unsynchronised static registry + multi-thread callers (§8) | Thread-safe singleton factory | **1st** |
| B | `deviceIdCreated` smell / nullables | Device-id resolved out of band; dependent components built twice, patched late (§2–§3) | Init barrier on serial queue (§4) | 2nd |
| C | Login NPE / stale-prefs race | Login (serial queue) races device-id init (main/IO) — different queues | Falls out of B (same queue) | 2nd |
| D | In-apps dropped before stores ready | `InAppResponse.java:111` early-return | Falls out of B (ordering) | 2nd |
| E | Main-thread callback hop | `ioTask()` default callback = `MAIN_EXECUTOR` | Trivial once B lands | 2nd |

Note: A is independently shippable and independently valuable (it's a real concurrency bug),
and it de-risks B/C/D by guaranteeing one serial queue per account.

---

## 10. Threading & locking improvements the device-id flow unlocks

Inventoried every concurrency primitive in the init + device-scoped paths. The thesis:

> **The redesign lets us replace locks with queue affinity.** Today the SDK locks state
> because it's touched from an unpredictable set of threads (main, IO pool, FCM, receivers)
> at an unpredictable time (before/after async device-id). If device-scoped state is (1) built
> once behind the barrier and (2) only ever read/written on the per-account serial queue, the
> queue *is* the mutual exclusion — most of these locks become dead code. Single-writer /
> thread-confinement instead of shared-state-plus-locks. Fewer locks ⇒ less deadlock surface,
> no nested-lock overhead, simpler reasoning.

### 10a. Locks that become dead once device-id init is a single serial barrier (delete)

| Primitive | File:line | Why it's there today | After barrier |
|---|---|---|---|
| `adIDLock` | `DeviceInfo.java:402` | GAID fetched on IO pool, read from main/event threads *while init still running* | Set-once at queue head before any read → **drop lock, publish safely** (final/volatile) |
| `deviceIDLock` | `DeviceInfo.java:412` | Guards UUID gen + device-id storage writes vs concurrent callers | Generation + `forceUpdateDeviceId` all happen on the one serial queue (init **and** login) → **mutual exclusion by ordering, lock gone** |
| `synchronized fetchGoogleAdID()` / `generateDeviceID()` / `generateFallbackDeviceID()` | `DeviceInfo.java:828,876,976` | Method-level re-entrancy guard, **nested** with the two locks above (double-locking) | Each runs once per account during init → **remove the redundant `synchronized`** |

That's the whole GAID/device-id lock nest (5 primitives, double-locked) collapsing to
"set-once on one thread."

### 10b. Data races the barrier closes *for free* (no lock, just ordering)

These are currently **unguarded** nullable `var`s written on one thread and read on another
(classified as current data races in the inventory). Under the barrier they're written once at
the queue head before any reader runs — so they become effectively immutable-after-init
(candidates for `val` / safe publication), **no lock added**:

- `StoreRegistry.inAppStore / impressionStore / ndStoreProvider` (`StoreRegistry.kt:16-21`)
- `ControllerManager.inAppFCManager / ndFCManager` (`ControllerManager.java:29-31`)
- `InAppFCManager.deviceId` (`:41`) and `CTPreference.prefName` (`:22`) repoint races — *iff*
  init + login + the store/FC readers are all confined to the serial queue (see §10d caveat)

### 10c. The registry fix (A) and the device-id fix (B) are synergistic

- `instances` HashMap (`CleverTapAPI.java:156`) wants to become thread-safe. The cleanest form
  is `ConcurrentHashMap.computeIfAbsent` — mirroring the already-correct `executorMap`
  (`CTExecutorFactory.kt:17`). **But** `computeIfAbsent` throws on re-entrant mutation, and the
  constructor re-enters the registry today via `DeviceInfo.java:734`
  (`instanceWithConfig(...).deviceIDCreated(...)`). **Killing that reach-back (part of B) is
  exactly what makes `computeIfAbsent` safe for A.** So B removes the obstacle to the nicest
  version of A.
- `defaultConfig` (`CleverTapAPI.java:154`, static, non-volatile) and the public
  `getInstances()/setInstances()` mutators should be tightened in the same pass as A.

### 10d. Races the barrier does NOT fix — flag, don't assume

Confinement only helps state that's *actually* confined to the serial queue. These are touched
from other threads and need a deliberate decision (move onto the queue, or keep a minimal
atomic/volatile guard) — they're independent of device-id timing:

- ~~`EvaluationManager` eval/suppress lists — `ArrayList` read from the network thread.~~
  **CORRECTED by the §11 audit:** network send runs on the *same* per-account serial queue as
  event-eval, so these lists are serial-confined in steady state (not a race). The only real
  exposure is `loadSuppressedCSAndEvaluatedSSInAppsIds()` reassigning the refs on the IO pool
  during init — a startup-window race the barrier *does* close. Moved out of this list.
- `NdCountsStore.shownToday += 1` (`NdCountsStore.kt:74`) — non-atomic read-modify-write; two
  concurrent `didShow()` lose an increment unless FC mutation is serialized.
- **Keep (inherent, not timing-related):** `InAppFCManager.updateLimits` synchronized,
  `ControllerManager.displayUnitCache` volatile+synchronized lazy-init, `CTPreferenceCache`
  double-checked singleton, `CTLockManager` event/inbox locks,
  `CallbackManager` synchronized listener lists (still invoked across threads).

### 10e. The one thing that gates all of 10b/10d

Everything in §10b/§10d hinges on the **"who runs on which queue" audit from §5.** Lock
removal is only safe where the readers/writers are provably confined to the per-account serial
queue. So the audit isn't just for the barrier — it's the precondition for *any* of the lock
deletions. Order of operations stays: (A) thread-safe registry → (§5 audit) → (B) barrier +
confinement → then delete the §10a/§10b locks as they're proven dead.

---

## 11. §5 queue audit — results (verified in code)

Four subsystem audits + a direct source check to resolve one contradiction. The verb
"confined" below means "provably runs only on the per-account serial queue (Q1)."

### 11a. The actual executor topology

| Tag | Executor | What runs here |
|---|---|---|
| **Q1** | `postAsyncSafelyTask()` — key = **accountId** (`CTExecutors.java:113`) | events (`queueEvent`/`processEvent`/`flush`/`flushQueueSync`), **network send** (`flushDBQueue`→`sendQueue`→`onAttachHeaders`/`onSentHeaders`, verified `NetworkManager.kt:332,341`), `InAppResponse`/`DisplayUnitResponse` processing, `NdFCManager.didShow`, login (`asyncProfileSwitchUser`), push-token refresh from launch/login |
| **Q2** | `postAsyncSafelyTask(TAG_FEATURE_IN_APPS)` — key = feature tag | in-app display loop (`showNotificationIfAvailable`→`_showNotificationIfAvailable`), dismiss chain. **Per-feature, shared across accounts** |
| **Q3** | FC-manager `changeUser` nested dispatch (own tags) | `InAppFCManager.changeUser` / `NdFCManager.changeUser` |
| **IO** | `ioTask()` — concurrent pool | **device-id init** (`initDeviceID`/`fetchGoogleAdID`/`generateDeviceID`), **`InAppFCManager.didShow`**, `checkLimitsBeforeShowing`, store-init tasks, `getDeviceCachedInfo` |
| **MAIN** | main looper | in-app UI (`showInApp`/`displayNotification`), **`deviceIDCreated` callback (today)**, `onActivityResumed` CoreMetaData writes, public getters when app calls on main |
| **G1** | LocalDataStore's own fixed single-thread pool (**not per-account**) | profile inflate/persist |
| **CALLER** | whatever thread the app calls on | public synchronous getters |

**The contradiction resolved:** two audits called the header-listener path a separate "network
thread." Direct read disproves it — `sendQueue` (hence `onAttachHeaders`/`onSentHeaders`) runs on
Q1, the same queue as event-eval. So EvaluationManager's lists are **serial-confined in steady
state** (the flagged ArrayList race was a false positive). See §10d correction.

### 11b. Consolidated verdict per device-scoped state

| State | Written on | Read on | Confined (steady)? | Barrier helps? | Residual action |
|---|---|---|---|---|---|
| Store/FC **references** (`StoreRegistry.inAppStore/impressionStore`, `ControllerManager.inAppFCManager/ndFCManager`) | IO (`deviceIDCreated`) today | Q1, Q2, MAIN, CALLER | **NO** (built off-queue, read everywhere) | **YES** — built once before any reader → set-once refs, publish via `final`/`volatile`, drop null-guards | core of Fix B |
| EvaluationManager eval/suppress lists | Q1 (eval) + **IO** (init reassignment) | Q1 (headers) | steady: **YES**; init window: NO | **YES** — barrier moves the init load onto Q1 → fully confined | then ArrayList is fine; no sync needed |
| `InAppFCManager` counters (`shownToday`/per-inapp) | **IO pool** (`didShow`) | Q1 (header build), CALLER (getters) | **NO** — cross-executor, pre-existing | **NO** (ongoing mutable state, device-id-independent) | **independent fix:** move `didShow` to Q1 to match `NdFCManager.didShow` |
| `NdCountsStore` counters | Q1 (`NdFCManager.didShow`) | MAIN/display (`canShow` gate) | partial (write Q1, read off-queue) | NO | minor; advisory gate — consider snapshot read |
| `InAppFCManager.deviceId` / `CTPreference.prefName` (login repoint) | Q1 (login) | Q1/Q2/MAIN readers | depends on reader queue | partial — barrier unifies init+login path | repoint + readers same queue, or `volatile` |
| LocalDataStore profile | **G1** (global pool) | CALLER (`getProfile`), Q1 | **NO** (global pool + caller reads) | partial | **independent fix:** make per-account; local lock today |
| Device ID (`getDeviceID()`) | IO (init), Q1 (login) | everywhere | treated immutable-after-init | **YES** — truly ready-before-use | good |

### 11c. Head-of-line blocking — confirmed real, bounded

Push `onNewToken` arrives on the FCM/**Binder** thread (`PushProviders.onNewToken:305`, not
confined) and queues its register event onto **Q1** via `sendDataEvent`. Under the barrier, Q1's
head is the device-id job, so on **first launch** a token event waits behind the GAID fetch
(~100-200ms). Acceptable but must be conscious: keep the barrier job minimal, and note that
decoupling GAID (option B) removes this stall entirely. No deadlock (different threads), just a
one-time first-launch delay.

### 11d. What the audit changes about the plan

- **Fix B (barrier) confirmed sound for references** — the store/FC *reference* publication race
  (§10b) is real and the barrier closes it. Proceed.
- **Two races are NOT device-id's to fix** (split into their own tickets, independently
  shippable): `InAppFCManager.didShow` on IO pool (should be Q1), and LocalDataStore's global
  executor. Both are pre-existing concurrency bugs surfaced by the audit.
- **Public getters on CALLER thread** (`getProfile`, `getCleverTapID`, `getInAppsCount`,
  `getInboxMessageCount`, `getPushToken`, `getAllDisplayUnits`) read device-scoped state inline —
  the barrier doesn't change that they're on the caller thread; it only guarantees the state is
  *built* by the time steady-state calls happen. API-shape concern, out of scope.
- **§10a lock deletions** (adIDLock/deviceIDLock/synchronized device-id methods) remain valid —
  device-id generation is IO-then-Q1 and single-writer under the barrier.
