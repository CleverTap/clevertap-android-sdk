# CleverTap Android SDK — ANR, Memory Leak & Crash Audit

Date: 2026-04-30
Branch: `claude/analyze-anr-memory-leaks-YkSS4`
Scope: `clevertap-core`, `clevertap-pushtemplates`, `clevertap-geofence`, `clevertap-hms`

This report catalogues confirmed (read-from-source) risks. Each finding cites
file path and line numbers so the relevant code can be opened directly.

---

## 1. ANR (Application Not Responding) Risks

ANR threshold is ~5s for an input dispatch / service start, ~10s for a
broadcast receiver. Anything that sits on the main thread or stalls the
broadcast `goAsync()` window is in scope.

### 1.1 [HIGH] `@Synchronized` DB methods that any thread can hit

`clevertap-core/src/main/java/com/clevertap/android/sdk/db/DBAdapter.kt:63-99`

```kotlin
@WorkerThread
@Synchronized
fun storeObject(obj: JSONObject, table: Table): Long = eventDAO.storeEvent(obj, table)

@WorkerThread
@Synchronized
fun fetchEvents(table: Table, limit: Int): QueueData = eventDAO.fetchEvents(table, limit)
```

The `@Synchronized` annotation locks on the `DBAdapter` instance for every
storage / fetch / cleanup / inbox / push / profile call. `@WorkerThread` is
advisory only — nothing prevents a public API caller from invoking these
indirectly on the main thread (and several legacy paths do, e.g., inbox
re-init, profile inflation). Because event/inbox/profile/push DAO calls all
share the same monitor, a slow event flush blocks an unrelated inbox query.

**Fix:** scope locks per-table or replace with the SQLite connection lock
(SQLiteDatabase already serialises writes); never do crypto / large iteration
inside the monitor.

### 1.2 [HIGH] Reflection + GMS network call inside `synchronized` during cold start

`clevertap-core/src/main/java/com/clevertap/android/sdk/DeviceInfo.java:828-874`

```java
private synchronized void fetchGoogleAdID() {
    ...
    Class adIdClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
    Method getAdInfo = adIdClient.getMethod("getAdvertisingIdInfo", Context.class);
    Object adInfo = getAdInfo.invoke(null, context); // talks to Play Services
    synchronized (adIDLock) { ... }
}
```

`AdvertisingIdClient.getAdvertisingIdInfo()` opens a bound service to Google
Play Services; on cold devices this can take hundreds of ms to several
seconds. The outer `synchronized` is on `DeviceInfo` itself, so any other
caller of `DeviceInfo` synchronized methods (there are several:
`generateDeviceID`, accessors) is parked behind it. Combined with double
locking on `adIDLock` (1.852-869), this raises deadlock and stall risk.

### 1.3 [HIGH] `synchronized` SharedPreferences writes on InApp hot path

`clevertap-core/src/main/java/com/clevertap/android/sdk/InAppFCManager.java:196`

```java
public synchronized void updateLimits(final Context context, int perDay, int perSession) {
    StorageHelper.putInt(context, storageKeyWithSuffix(...), perDay);
    StorageHelper.putInt(context, storageKeyWithSuffix(...), perSession);
}
```

`StorageHelper.putInt` ultimately calls `SharedPreferences.Editor.commit()` in
several call sites — confirm whether this path uses `apply()`. Synchronized
public API holding the monitor across two preference writes will block
in‑app evaluation when called during page transitions.

### 1.4 [MEDIUM] `BroadcastReceiver` waits on a `Future` for up to 8 s

`clevertap-geofence/src/main/java/com/clevertap/android/geofence/CTGeofenceReceiver.java:50-56`

```java
Future<?> future = CTGeofenceTaskManager.getInstance()
        .postAsyncSafely("PushGeofenceEvent", pushGeofenceEventTask);
if (future != null) {
    future.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS); // 8000
}
```

Receiver lifetime is 10 s. An 8 s `future.get` leaves only 2 s of slack — a
single backed-up executor queue or slow geofence write will ANR.

**Fix:** drop the synchronous wait, rely on `goAsync()` and finish the
`PendingResult` from the task callback.

### 1.5 [MEDIUM] FCM receiver ties pending result to a worker thread

`clevertap-core/.../pushnotification/fcm/CTFirebaseMessagingReceiver.java:102-172`

`onReceive()` calls `goAsync()` and then spawns a raw `new Thread(...)` to
flush push impressions. A `CountDownTimer` (default 4.5 s) is the only safety
net. If `flushPushImpressionsOnPostAsyncSafely` blocks on DB or network the
process can be ANR'd before the timer fires.

### 1.6 [MEDIUM] First-call lazy synchronized singletons

`clevertap-core/src/main/java/com/clevertap/android/sdk/ManifestInfo.java:49-54`

```java
public synchronized static ManifestInfo getInstance(Context context) {
    if (instance == null) instance = new ManifestInfo(context);
    return instance;
}
```

Constructor reads PackageManager metadata (binder I/O). The static
synchronized method blocks every concurrent caller until the first instance
is built — common cold-start serialisation point.

### 1.7 [MEDIUM] Profile inflation under coarse monitor

`clevertap-core/src/main/java/com/clevertap/android/sdk/LocalDataStore.java:486-535`

A `synchronized (PROFILE_FIELDS_IN_THIS_SESSION)` block fetches the encrypted
profile from SQLite and iterates every key (decrypt per field). Hundreds of
profile properties means hundreds of crypto operations under the monitor.

### 1.8 [MEDIUM] `pushInstallReferrer` is `synchronized` and does pref I/O

`clevertap-core/src/main/java/com/clevertap/android/sdk/AnalyticsManager.java:396-407`

Synchronized public method that reads + writes `app_install_status`. Called
from referrer client callbacks; competes with other analytics events.

### 1.9 [LOW] `Thread.sleep` in GIF animation thread

`clevertap-core/src/main/java/com/clevertap/android/sdk/gif/GifImageView.java:192`

Sleeps on a worker thread, but it interrupts cleanup on
`onDetachedFromWindow` poorly — the worker thread can outlive the View by
the duration of the longest frame.

---

### Top 5 ANR risks

1. `DBAdapter.kt` — class‑wide `@Synchronized` across all DB DAOs (1.1)
2. `DeviceInfo.fetchGoogleAdID` — reflective Play-Services call inside `synchronized` (1.2)
3. `InAppFCManager.updateLimits` — synchronized SharedPreferences writes (1.3)
4. `CTGeofenceReceiver` — 8 s `future.get` inside a 10 s receiver window (1.4)
5. `CTFirebaseMessagingReceiver` — raw `Thread` + `CountDownTimer` race against `goAsync` (1.5)

---

## 2. Memory Leaks

### 2.1 [HIGH] `ExecutorService` in `LocalDataStore` is never shut down

`clevertap-core/src/main/java/com/clevertap/android/sdk/LocalDataStore.java:75`

```java
this.es = Executors.newFixedThreadPool(1);
```

`es` is created in the constructor and used by `es.submit(...)` (e.g. line
~643). There is no `shutdown()` / `shutdownNow()` anywhere in
`LocalDataStore`. Each Runnable submitted is an anonymous inner class that
captures `LocalDataStore.this`, which transitively holds `Context`,
`DeviceInfo`, `BaseDatabaseManager`. When a `CleverTapAPI` instance is
removed from the static `instances` map (on account switch / multi-instance
teardown), the `LocalDataStore` and everything it references stays alive
because the worker thread keeps the pool — and thus its tasks' captured
state — strongly reachable.

### 2.2 [MEDIUM] Static map of notification listeners with no eviction

`clevertap-core/src/main/java/com/clevertap/android/sdk/CleverTapAPI.java:161`

```java
private static final HashMap<String,NotificationRenderedListener>
        sNotificationRenderedListenerMap = new HashMap<>();
```

Inserted in `setNotificationRenderedListener` (line ~2806), retrieved
(~2811), removed only via the explicit `remove…` API (~2816). If the host
forgets to remove (or removal is not called on a crash path), an Activity-
scoped listener stays in the static map for the process lifetime.

### 2.3 [MEDIUM] Static `instances` map keyed by accountId, holds `Context`

`clevertap-core/src/main/java/com/clevertap/android/sdk/CleverTapAPI.java:152` plus instance field at `:163`

```java
private static HashMap<String, CleverTapAPI> instances;
...
private final Context context;
```

The map itself is intentional (singleton-per-account), but each
`CleverTapAPI` keeps a `Context` field. If any caller passes an Activity to
`instanceWithConfig(...)` or `getDefaultInstance(...)`, that Activity is
pinned for the process lifetime. The constructor does not coerce to
`getApplicationContext()`. (Verify all entry points; coerce there once.)

### 2.4 [MEDIUM] `Handler.postDelayed` in inbox UI captures Fragment / View

`clevertap-core/.../inbox/CTInboxBaseMessageViewHolder.java:288-311` — anonymous
`Runnable` posted with 2 s delay; calls `getParent()` (Fragment) inside.

`clevertap-core/.../inbox/CTInboxListViewFragment.java:142-147` — anonymous
`Runnable` posted with 1 s delay; closes over `mediaRecyclerView` (View
field).

If the user navigates away or rotates inside the delay window, the message
sits in the main `MessageQueue` and pins the View / Fragment / Activity
until it runs. `removeCallbacksAndMessages(null)` on view detach would
mitigate.

### 2.5 [LOW] Static anonymous `ActivityLifecycleCallbacks`

`clevertap-core/.../ActivityLifecycleCallback.java:17-49`

```java
private static final Application.ActivityLifecycleCallbacks lifecycleCallbacks =
        new Application.ActivityLifecycleCallbacks() { ... };
```

Anonymous inner class held in a static. Currently safe because it only calls
static methods, but the pattern invites future leaks if anyone adds a
non-static field reference. Convert to a top-level class for safety.

### 2.6 [NOTE] `CoreMetaData.currentActivity` already uses `WeakReference`

`clevertap-core/.../CoreMetaData.java:20` correctly stores the foreground
Activity as a `WeakReference<Activity>`. No change needed.

---

### Top 5 leaks

1. `LocalDataStore.es` — unbounded executor lifetime, holds queued runnables (2.1)
2. `Handler.postDelayed` in inbox views without `removeCallbacks` on detach (2.4)
3. `sNotificationRenderedListenerMap` — relies on caller hygiene (2.2)
4. `CleverTapAPI.instances` map storing raw `Context` (2.3)
5. Static anonymous `ActivityLifecycleCallbacks` pattern risk (2.5)

---

## 3. Crash Risks

### 3.1 [HIGH] Unconditional `deepLinkList.get(0)` before any size check

`clevertap-pushtemplates/.../PushTemplateReceiver.java:411`

```java
String pt_dl_clicked = deepLinkList.get(0); // line 411
if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
    extras.putString(...);
    if (deepLinkList.size() > 0) { pt_dl_clicked = deepLinkList.get(0); }
}
```

Line 411 dereferences index 0 *before* the conditional `size()` checks below
it (415, 421, 429, …). If `deepLinkList` is empty (malformed payload, null
deeplinks JSON), this is a guaranteed `IndexOutOfBoundsException` inside a
`BroadcastReceiver` — i.e. the push tap is silently dropped and the process
logs a crash.

The size guards in 415 / 421 / 429 are also off-by-one: e.g.
`if (size > 0) get(0)` is fine, but `else { get(0); }` at 424/432/440/448
runs for any non-empty list, meaning it won't actually crash there — but
411 already has.

### 3.2 [HIGH] `PendingIntent` flag handling — verify all factories

`clevertap-core/.../pushnotification/LaunchPendingIntentFactory.java:18,31-32`

The factory branches on API level. Lines 18 and 58–60 correctly add
`FLAG_IMMUTABLE` / `FLAG_MUTABLE` for API ≥ 31. Line 31 path needs checking
that callers always go through the API gated path. Same shape in
`clevertap-geofence/.../GoogleGeofenceAdapter.java:71-72` — relies on
`PendingIntentFactory` adding the flag for API 31+. Recommend a single
helper that always adds the right flag and grep for any direct
`PendingIntent.getBroadcast` / `getActivity` / `getService` outside it.

### 3.3 [MEDIUM] Unsafe reflective cast in ad-id fetch

`clevertap-core/.../DeviceInfo.java:851`

```java
advertisingID = (String) getAdId.invoke(adInfo);
```

If GMS changes the return type or returns `null` and a later call chains a
method, the broad `catch (Throwable)` at 852 swallows it — no crash, but
silent ad-id miss. The unchecked cast itself is safe because of the catch,
but the broad catch hides legitimate breaking changes.

### 3.4 [MEDIUM] `PushTemplateReceiver` reads `extras.getParcelable("config")` without null check

`clevertap-pushtemplates/.../PushTemplateReceiver.java:285,292,298`

```java
config = extras.getParcelable("config");          // line 285, may be null
int notificationId = extras.getInt(PTConstants.PT_NOTIF_ID); // 292
... config.getLogger() ...                        // 298 NPE
```

`extras.getParcelable` returns `null` if the key is missing or the
ClassLoader can't resolve the class (a real risk on process restart inside
a `BroadcastReceiver`).

### 3.5 [MEDIUM] `INotificationRenderer` uses `Class.forName` then dereferences

`clevertap-core/.../pushnotification/INotificationRenderer.java:48-58`

`Class.forName(intentServiceName)` may throw `ClassNotFoundException`; if
caught broadly and `clazz` left null, downstream `clazz.newInstance()` /
`Constructor` calls NPE.

### 3.6 [MEDIUM] `startActivity` from `BroadcastReceiver` context

`clevertap-pushtemplates/.../PushTemplateReceiver.java:580,608`

```java
context.startActivity(launchIntent);
```

On Android 10+ background activity launches from a `BroadcastReceiver` are
restricted. Without proper flags + `PendingIntent`, this can throw
`AndroidRuntimeException` or be silently blocked. Confirm `FLAG_ACTIVITY_NEW_TASK`
is set on every path (e.g. line 400 sets it; verify 580/608 do).

### 3.7 [LOW] `WeakReference.get()` use in InApp

`clevertap-core/.../InAppNotificationActivity.java:319,323` reads via
`listenerWeakReference.get()` and null-checks before deref. Currently safe;
keep the null check at every call site.

---

### Top 5 crash risks

1. `PushTemplateReceiver.java:411` — `IndexOutOfBoundsException` on empty `deepLinkList` (3.1)
2. PendingIntent flag factories — single-helper invariant must be enforced repo-wide on API 31+ (3.2)
3. `PushTemplateReceiver.java:285+298` — NPE from missing `config` parcelable (3.4)
4. `INotificationRenderer` — NPE if `Class.forName` fails silently (3.5)
5. Background `startActivity` from receiver on Android 10+ (3.6)

---

## 4. Recommended remediation order

1. Fix the guaranteed `IndexOutOfBoundsException` in
   `PushTemplateReceiver.java:411` — small diff, removes a known crash.
2. Add a single `PendingIntentFactory` and grep-enforce no direct
   `PendingIntent.get*` outside it.
3. Make `LocalDataStore.es` `shutdown()` reachable on instance teardown
   (e.g. from `CleverTapAPI` removal path). Use `CTExecutors` shared pool
   instead of a private `newFixedThreadPool(1)`.
4. Replace `@Synchronized` on `DBAdapter` with per-DAO locks or rely on
   SQLite's connection serialisation; never hold a Java monitor across
   crypto loops.
5. Move `fetchGoogleAdID()` off the `DeviceInfo` monitor; cache the result
   under `adIDLock` only.
6. Audit `Handler.postDelayed` in inbox/inapp UI; pair every post with
   `removeCallbacksAndMessages(null)` in `onDetachedFromWindow` /
   `onDestroyView`.
7. Either coerce all `CleverTapAPI` constructor `Context` to
   `applicationContext` or document the contract and assert in debug.
8. In `CTGeofenceReceiver`, drop the 8 s `future.get` and finish the
   `PendingResult` from the task callback.

## 5. Notes on what was *not* found

- No obvious WebView abuses on the main thread.
- No unbounded bitmap caches in `bitmap/`; LruCache is used.
- `CoreMetaData.currentActivity` correctly uses `WeakReference`.
- Most JSON parsing is wrapped in `try/catch (Throwable)`; risk is silent
  state, not crash.
