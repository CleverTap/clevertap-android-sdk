# detekt baseline burn-down (SDK-6378)

Generated from the per-module baselines (`<module>/detekt-baseline-release.xml`) on the post-migration code (feat/kotlin-migration + SDK-6375 idiomatic pass). Everything below is **ignored by the build today**; new code must not add to it. Regenerate after fixing: `./gradlew :<module>:detektBaselineRelease`, then `python3 config/detekt/burndown.py .` to refresh this file.

**Total baselined findings: 651**  
core: 479 · geofence: 51 · hms: 2 · pushtemplates: 119

Baseline files: `clevertap-core/detekt-baseline-release.xml`; `clevertap-geofence/detekt-baseline-release.xml`; `clevertap-hms/detekt-baseline-release.xml`; `clevertap-pushtemplates/detekt-baseline-release.xml`

## Summary by category

| Category | Findings | Rules |
|---|---|---|
| Null safety | 229 | UnsafeCallOnNullableType (124), CanBeNonNullable (88), MapGetWithNotNullAssertionOperator (17) |
| Idioms | 3 | UseRequire (1), UseOrEmpty (1), MayBeConst (1) |
| Exceptions | 10 | TooGenericExceptionThrown (5), ThrowingExceptionsWithoutMessageOrCause (5) |
| Library safety | 9 | DoubleMutabilityForCollection (9) |
| Naming | 2 | FunctionNaming (2) |
| Style | 66 | MagicNumber (25), ReturnCount (17), MaxLineLength (17), ForbiddenMethodCall (7) |
| Complexity | 311 | LongParameterList (72), NestedBlockDepth (70), CyclomaticComplexMethod (62), TooManyFunctions (37), LongMethod (37), LargeClass (28), ComplexCondition (5) |
| Other | 21 | LoopWithTooManyJumpStatements (11), ThrowsCount (2), ArrayPrimitive (2), UselessCallOnNotNull (1), SpreadOperator (1), ObjectLiteralToLambda (1), MemberNameEqualsClassName (1), FunctionOnlyReturningConstant (1), ForbiddenComment (1) |

## By rule (with how to fix and where)

### Null safety

- **UnsafeCallOnNullableType: 124** (core 65, geofence 34, pushtemplates 23, hms 2). Single read + `?:`/`?.let`/early return; `requireNotNull(x) { "why" }` where a crash is intended.  
  Top files: `CleverTapAPI.kt` (14), `TriggersMatcher.kt` (9), `CTGeofenceAPI.kt` (8), `Utils.kt` (5), `ProductDisplayTemplateValidator.kt` (5), `EventQueueManager.kt` (4)
- **CanBeNonNullable: 88** (core 73, geofence 9, pushtemplates 6). Make the declaration non-null (often `lateinit` or a constructor `val`).  
  Top files: `CleverTapAPI.kt` (13), `LocalDataStore.kt` (6), `AnalyticsManager.kt` (5), `Utils.kt` (5), `CTProductConfigFactory.kt` (4), `CTFeatureFlagsFactory.kt` (3)
- **MapGetWithNotNullAssertionOperator: 17** (pushtemplates 17). `map.getValue(k)` or explicit null handling.  
  Top files: `ProductDisplayTemplateValidator.kt` (5), `CarouselTemplateValidator.kt` (2), `ContentValidator.kt` (2), `FiveIconsTemplateValidator.kt` (2), `InputBoxTemplateValidator.kt` (2), `RatingTemplateValidator.kt` (2)

### Idioms

- **UseRequire: 1** (core 1). `require(cond)` instead of `throw IllegalArgumentException`.  
  Top files: `PushProviders.kt` (1)
- **UseOrEmpty: 1** (core 1). `orEmpty()`.  
  Top files: `CTInboxStyleConfig.kt` (1)
- **MayBeConst: 1** (pushtemplates 1). `const val` so Java gets a static final field.  
  Top files: `PTConstants.kt` (1)

### Exceptions

- **TooGenericExceptionThrown: 5** (pushtemplates 3, core 2). Throw a specific exception type.  
  Top files: `Utils.kt` (2), `CleverTapFactory.kt` (1), `InputBoxStyle.kt` (1), `PushTemplateReceiver.kt` (1)
- **ThrowingExceptionsWithoutMessageOrCause: 5** (core 4, pushtemplates 1). Add a message/cause.  
  Top files: `CTFcmMessageHandler.kt` (1), `CTInboxActivity.kt` (1), `InAppNotificationActivity.kt` (1), `PushProviders.kt` (1), `PushTemplateReceiver.kt` (1)

### Library safety

- **DoubleMutabilityForCollection: 9** (core 9). `val` + mutable collection, or `var` + read-only collection.  
  Top files: `EvaluationManager.kt` (2), `NdEvaluationManager.kt` (2), `CTInAppAction.kt` (1), `CTInboxController.kt` (1), `CTInboxStyleConfig.kt` (1), `CleverTapDisplayUnit.kt` (1)

### Naming

- **FunctionNaming: 2** (core 2). Kotlin camelCase function names (not for public API / reflection names).  
  Top files: `InAppController.kt` (1), `PushProviders.kt` (1)

### Style

- **MagicNumber: 25** (core 13, pushtemplates 11, geofence 1). Named constants.  
  Top files: `CtDatabase.kt` (5), `ValidationConfig.kt` (3), `FiveIconBigContentView.kt` (3), `FiveIconSmallContentView.kt` (3), `PendingIntentFactory.kt` (3), `CTXtensions.kt` (2)
- **ReturnCount: 17** (core 14, pushtemplates 3). Guard clauses / `when` expressions; max 2 non-guard returns.  
  Top files: `InAppController.kt` (3), `PushProviders.kt` (3), `AnalyticsManager.kt` (1), `BitmapDownloader.kt` (1), `CTFirebaseMessagingReceiver.kt` (1), `CTInAppNotification.kt` (1)
- **MaxLineLength: 17** (core 16, pushtemplates 1). Wrap lines > 120.  
  Top files: `ValidationResultFactory.kt` (3), `VideoLibChecker.kt` (3), `CTInboxController.kt` (2), `CleverTapAPI.kt` (1), `DeviceInfo.kt` (1), `ExoplayerHandle.kt` (1)
- **ForbiddenMethodCall: 7** (core 7). Log through `Logger`, no `Thread.sleep`/`println`.  
  Top files: `DefaultXmlParser.kt` (4), `CTDisplayUnitType.kt` (1), `CleverTapResponse.kt` (1), `UrlConnectionHttpClient.kt` (1)

### Complexity

- **LongParameterList: 72** (core 59, pushtemplates 13). Parameter objects / default args.  
  Top files: `CleverTapAPI.kt` (6), `EventDataNormalizer.kt` (5), `OperationHandler.kt` (4), `ArrayOperationHandler.kt` (3), `CTInboxBaseMessageViewHolder.kt` (3), `ProgressStyle.kt` (3)
- **NestedBlockDepth: 70** (core 62, pushtemplates 8). Guard clauses, extract functions (threshold 3).  
  Top files: `NetworkManager.kt` (6), `InAppController.kt` (5), `AnalyticsManager.kt` (4), `CTInAppNotification.kt` (4), `CTInboxMessageContent.kt` (4), `TemplateRenderer.kt` (4)
- **CyclomaticComplexMethod: 62** (core 44, pushtemplates 16, geofence 2). Split / `when` tables (threshold 10).  
  Top files: `CTInAppNotification.kt` (5), `InAppController.kt` (4), `TemplateRenderer.kt` (4), `AnalyticsManager.kt` (3), `PushProviders.kt` (3), `PushTemplateReceiver.kt` (3)
- **TooManyFunctions: 37** (core 35, geofence 1, pushtemplates 1). Split the type/file (threshold 15).  
  Top files: `Utils.kt` (3), `CleverTapAPI.kt` (2), `BaseAnalyticsManager.kt` (1), `CTInAppBaseFullFragment.kt` (1), `CTInboxBaseMessageViewHolder.kt` (1), `CTInboxMessage.kt` (1)
- **LongMethod: 37** (core 27, pushtemplates 8, geofence 2). Extract private functions (threshold 30 lines).  
  Top files: `PushTemplateReceiver.kt` (4), `CTInAppNotification.kt` (3), `PushProviders.kt` (3), `EventDataNormalizer.kt` (2), `InAppController.kt` (2), `TemplateRenderer.kt` (2)
- **LargeClass: 28** (core 23, pushtemplates 4, geofence 1). Split responsibilities (threshold 300 lines).  
  Top files: `CleverTapAPI.kt` (2), `Utils.kt` (2), `AnalyticsManager.kt` (1), `CTInAppNotification.kt` (1), `CTInboxBaseMessageViewHolder.kt` (1), `CTInboxController.kt` (1)
- **ComplexCondition: 5** (core 3, pushtemplates 2). Name the sub-conditions.  
  Top files: `TemplateRenderer.kt` (2), `CleverTapAPI.kt` (1), `CleverTapDisplayUnitContent.kt` (1), `InAppResponse.kt` (1)

### Other

- **LoopWithTooManyJumpStatements: 11** (core 10, pushtemplates 1). See https://detekt.dev/docs/rules  
  Top files: `EventDataNormalizer.kt` (2), `CTProductConfigController.kt` (1), `CleverTapAPI.kt` (1), `GifImageView.kt` (1), `LocalDataStore.kt` (1), `LoginController.kt` (1)
- **ThrowsCount: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CustomTemplate.kt` (1), `JsonTemplatesProducer.kt` (1)
- **ArrayPrimitive: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CTLocalInApp.kt` (1), `UserEventLogDAOImpl.kt` (1)
- **UselessCallOnNotNull: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CTProductConfigController.kt` (1)
- **SpreadOperator: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `DeviceInfo.kt` (1)
- **ObjectLiteralToLambda: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CleverTapAPI.kt` (1)
- **MemberNameEqualsClassName: 1** (geofence 1). See https://detekt.dev/docs/rules  
  Top files: `CTGeofenceAPI.kt` (1)
- **FunctionOnlyReturningConstant: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `LeanplumCT.kt` (1)
- **ForbiddenComment: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CleverTapAPI.kt` (1)

## Suggested order for the burn-down pass

1. **Null safety + idioms** (mechanical, highest value, follows the SDK-6375 patterns).
2. **Exceptions**: catching broadly and swallowing is SDK policy (those rules are off); only fix printStackTrace, raw Exception throws and unnamed empty catches.
3. **Library safety**: public data classes and explicit return types (or turn on `explicitApi()`).
4. **Naming/style** in internal code (not public API, keys, reflection-referenced names).
5. **Complexity**: opportunistically when touching a method; don't refactor for the metric alone.
6. **Public docs**: KDoc on bucket-A API first (see docs/KotlinMigrationPublicSurface.md).

