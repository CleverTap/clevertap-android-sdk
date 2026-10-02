# Content Fetch × In-App Arbitration — In-Repo Testing Plan

> Scope: unit + integration tests inside `clevertap-core`. Real device / network E2E is
> covered in the separate E2E repo and is intentionally out of scope here.
> Status: **plan only — not yet implemented.** Pick up from "Decisions to confirm" below.

---

## 0. Guiding principle — minimum mocking

Push each case to the **cheapest home where the real logic lives**. Measured mock cost
(against real constructors + existing test harnesses in this repo):

| Case family | Where the logic actually lives | Mocks needed | Harness exists? |
|---|---|---|---|
| Winner merge (priority / `ti` across /a1 ∪ /content) | `AppLaunchInAppArbitrator` (`sortByPriority` + `showWinner` lambdas) | **0** (lambdas + `TestDispatchers`) | ✅ `AppLaunchInAppArbitratorTest` |
| Timeout / late-arrival / completion→show | `AppLaunchInAppArbitrator` | **0** | ✅ same |
| Eligibility selection (real sort, suppressed-skip, zero-eligible) | `EvaluationManager.evaluateOnAppLaunchedServerSide` | **5** ctor (use real matchers; mock only `storeRegistry`/`InAppStore`) | ✅ `EvaluationManagerTest` pattern |
| Dry-run non-mutation | `EvaluationManager.evaluateDryRun` | **5** ctor | ✅ same |
| Completion fires on 429/500/empty/cancel | `ContentFetchManager` | **~3** (`ctApiWrapper` + `Response` + fake handler) | ✅ `ContentFetchManagerTest` |
| Recursion guard + CS guard | `InAppResponse.processResponse(source)` | **~6 relaxed** (7-arg ctor) | partial |
| ND merge order + callback | `DisplayUnitResponse` | **3** | ✅ `DisplayUnitResponseTest` |
| S1/S2 short-circuit + fast-path trigger | `InAppController` wiring | **~18** (reuse existing `@Before`) | ✅ `InAppControllerTest` |

Takeaways:
- ~80% of the matrix sits in **0–5 mock** homes (arbitrator, EvaluationManager, ContentFetchManager).
- The only mock-heavy surface is the **pre-existing** `InAppControllerTest` (52 `mockk` in its `@Before`).
  We **reuse** it for S1/S2 + fast-path trigger; we do not add to the mock count.
- **Drop the full-`CleverTap` option:** there is **no `BaseTestCase` in clevertap-core**. Standing one
  up would be the single biggest mocking cost in the plan for little gain over `InAppResponse`-level tests.

---

## 1. Testing strategy — 5 layers

| Layer | Purpose | Harness |
|---|---|---|
| **L1. Component (unit)** | isolate each piece | exists; add `EvaluationManagerDryRunTest` (the one real gap) |
| **L2. Arbitration integration** | real winner selection across /a1 + /content | `InAppController` + real `EvaluationManager` + real arbitrator; mock `InAppQueue` (capture), `MockCTExecutors`, `TestDispatchers` |
| **L3. Response-chain / guards** | recursion + CS guards, S1/S2 short-circuits | `InAppResponse.processResponse(source)` with mocked `InAppController` |
| **L4. Failure injection** | 429 / HTTP error / empty / timeout / cancel / crash | `ContentFetchManager` + mocked `CtApi`; drive completion + test dispatcher |
| **L5. Concurrency** (optional) | thread-safety of the synchronized window | stress: concurrent /a1 route + /content completion |

Harness technique: **drive `onContentFetchComplete()` directly** for selection tests
(deterministic, no real timer). Use `TestDispatchers` + `advanceTimeBy` only where the test is
*about* the timer. Mock the network `/content` as canned JSON (or error) via `CtApi`.

---

## 2. Scenario matrix (the "all cases" centerpiece)

Each row: `(/a1 in-apps, content_fetch, /content outcome, local state) → (count shown, winner)`.

### A. Winner selection
| # | /a1 | /content | Expect | Home |
|---|---|---|---|---|
| W1 | A(p1,ti100), B(p1,ti200); no content_fetch | — | **A** (earliest ti), no window | EvalMgr |
| W2 | A(p1,ti100) + content_fetch | C(p1,ti300) | **A** (ti 100<300) — the repro | Arbitrator |
| W3 | A(p1,ti100) + cf | C(p5,ti300) | **C** (higher priority) | Arbitrator |
| W4 | A(p10,ti100) + cf | C(p1,ti300) | **A** | Arbitrator |
| W5 | A(p1,ti300) + cf | C(p1,ti100) | **C** (content earlier ti) | Arbitrator |
| W6 | A(suppressed), B(p1) + cf | C(p1) | winner of {B,C}, A skipped | EvalMgr |
| W7 | 0 eligible + cf | C(p1) | **C** (S4) | EvalMgr + Arbitrator |

### B. Failure / timeout
| # | Setup | Expect | Home |
|---|---|---|---|
| F1 | /content **429** | **A** (/a1 winner) — fail still fires completion | ContentFetchMgr + Arbitrator |
| F2 | /content **HTTP 500** | **A** | ContentFetchMgr + Arbitrator |
| F3 | /content **[]** | **A** | Arbitrator |
| F4 | /content **never arrives** (3s timeout) | **A**, shown at 3s | Arbitrator |
| F5 | /content arrives **@5s** (after timeout) | **A** @3s, C **dropped** (suppressing); still 1 | Arbitrator |
| F6 | /content carries **content_fetch** (nested) | recursion guard: no re-fetch; still 1 | InAppResponse |
| F7 | /content carries **inapp_notifs_cs** | CS guard: store **not** wiped | InAppResponse |

### C. Short-circuits (no window → immediate)
| # | content_fetch | Expect | Home |
|---|---|---|---|
| S1 | responseKey = inbox | A shown immediately, no window | InAppController |
| S2 | eventName = "Product Viewed" | A shown immediately | InAppController |

### D. Option 2 fast path (synthetic `priority` present — dormant today; test the activation path)
| # | Setup | Expect | Home |
|---|---|---|---|
| FP1 | /a1 A(p100), synthetic C(p50) | fast path: A immediately; C dropped | InAppController + Arbitrator |
| FP2 | /a1 A(p10), synthetic C(p90) | wait → **C** | InAppController + Arbitrator |
| FP3 | synthetic C ineligible (limit maxed) | fast path: A | EvalMgr dry-run |
| FP4 | partial synthetics (one item no priority) | all-or-nothing → wait (Option 1) | InAppController |
| FP5 | mispredict | late higher-priority drop logs mispredict | Arbitrator |

### E. User-switch
- US1: login mid-window → previous user's A **not** shown to new user (abandon). Home: Arbitrator + `LoginControllerTest` (ordering already covered).

### F. Crash-safety
- CR1: throwing `showWinner` → no propagation, self-heal. (already covered in arbitrator test)
- CR2: throwing completion callback → contained. Home: ContentFetchManager.

### G. Native Display
- ND merge preserves order; host callback receives full merged set; /a1 still REPLACE. Home: `DisplayUnitResponseTest`.

### H. Dry-run (L1)
- `evaluateDryRun` does **not** increment triggers; offset `+1` flips an `OnEvery/OnExactly` limit;
  template/trigger/limit gates filter correctly; empty candidates → empty.

---

## 3. Proposed new/extended test files
- `EvaluationManagerDryRunTest` (L1) — H + FP3
- `AppLaunchArbitrationIntegrationTest` (L2) — W2–W5, F-series via completion, FP1/FP2
- `InAppContentFetchGuardsTest` (L3) — F6, F7, S1, S2 (or fold S1/S2 into `InAppControllerTest`)
- extend `ContentFetchManagerTest` (L4) — completion-fires-on-{429,500,empty,cancel}, CR2
- extend `DisplayUnitResponseTest` (G) — order + full-set callback
- extend `EvaluationManagerTest` — W1, W6, W7 with real matchers
- (optional) `AppLaunchArbitratorConcurrencyTest` (L5)

Suggested implementation order (cheapest first): **L1 → L4 → EvalMgr rows → L2 → L3 → (L5)**.

---

## 4. Coroutine testing rules (confirmed against `kotlinx-coroutines-test 1.7.3`, not assumed)

These are hard rules derived from official docs + verified empirically (existing suites run green):

1. **`advanceTimeBy(n)` does NOT run tasks scheduled at *exactly* now+n** (1.6+ semantics).
   Our arbitrator fires at **exactly 3000ms** → every timeout test must do
   `advanceTimeBy(3000L); runCurrent()` (or `advanceTimeBy(3001L)`). Bare `advanceTimeBy(3000)`
   silently will not fire it. (See `AppLaunchInAppArbitratorTest:53-54` for the correct pattern.)
2. **One `TestCoroutineScheduler` per test, shared across all dispatchers.** Always pass the
   shared scheduler into `TestDispatchers(scheduler)`. Never use the no-arg `TestDispatchers()`
   in a timing test (it mints a fresh scheduler per `io()` call → virtual time not shared).
   For ContentFetchManager + arbitrator integration, both must get the **same** scheduler.
3. **`StandardTestDispatcher` queues; it does not run eagerly.** Use `advanceUntilIdle()`
   ("drain everything"), `advanceTimeBy()` ("move the clock"), or `runCurrent()` ("run only
   what's due now"). Repo uses Standard (correct — we rely on /a1-vs-/content ordering).
4. **`limitedParallelism` on a `StandardTestDispatcher`** preserves virtual time in 1.7.3 —
   *not* documented, but empirically confirmed: `ContentFetchManager` does
   `dispatchers.io().limitedParallelism(n)` and `ContentFetchManagerTest` drives it with
   `advanceUntilIdle()`, green. Do not assume beyond 1.7.x without re-checking.
5. **`runTest` auto-times-out at 60s** if any coroutine parks on a non-test dispatcher. Keep all
   coroutines on injected test dispatchers. Use `backgroundScope` only for long-lived coroutines
   that must be auto-cancelled at test end (we won't need it; `advanceUntilIdle()` drains the backstop).
6. Avoid `UnconfinedTestDispatcher` for concurrency/ordering tests (eager execution hides real
   scheduling; still yields at suspension).

### Production-code conformance (docs vs our code)
- ✅ Inject dispatchers (`DispatcherProvider`), cooperative cancellation
  (`currentCoroutineContext().isActive`, `parentJob.cancel()+join`), host-crash containment
  (`finally { try…catch(Throwable) }` around `onFetchBatchComplete`), arbitrator timer rethrows
  `CancellationException`.
- ⚠️ **One decision:** `ContentFetchManager.handleContentFetch` catches `CancellationException`
  **without rethrow**. It's at the root frame of the `launch` (nothing above to propagate to) and
  the `finally` still fires completion, so it is benign — but it reads against the documented
  "never swallow CancellationException" rule. Decide: rethrow for idiom-correctness, or keep +
  add a one-line comment explaining the intentional root-swallow. Not a blocker.

Sources: developer.android.com/kotlin/coroutines/test · developer.android.com/kotlin/coroutines/coroutines-best-practices ·
github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-test/README.md · kt.academy/article/cc-testing

---

## 5. Decisions to confirm (before implementing)
1. **S1/S2 + fast-path trigger:** ride the existing `InAppControllerTest` harness, or drop from
   in-repo and cover in the E2E repo (saves touching the 18-mock setup)?
2. **Guards (recursion/CS, F6/F7):** test at `InAppResponse` level (~6 relaxed mocks) — in or out?
3. **Winner matrix home:** confirm it lives primarily in **arbitrator + EvaluationManager**
   (mock-light), reframing a few rows from "end-to-end" to "at the decision point". OK?
4. **`CancellationException` rethrow** in `ContentFetchManager` — rethrow, or keep + comment?
5. Anything to cut as "E2E-repo's job" (real HTTP/CloudFront timing, real display rendering)?

---

## 6. Current state (for pickup)
- Branch: `feat/content-fetch-inapp-arbitration`; latest commit `db70f1e86` (docs cleanup). Clean tree.
- Only `#1095` (feat → develop) open. Merge to develop pending user go-ahead.
- Existing feature tests green: `AppLaunchInAppArbitratorTest`, `OffsetTriggerCounterTest`,
  `ContentFetchItemTest`, `DisplayUnitResponseTest`, `ContentFetchManagerTest`,
  `ClevertapResponseHandlerTest`, `LoginControllerTest`.
- No code changes have been made for this test plan yet.
