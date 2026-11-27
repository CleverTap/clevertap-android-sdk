# detekt baseline burn-down (SDK-6378)

Generated from the per-module baselines (`<module>/detekt-baseline-release.xml`) on the post-migration code (feat/kotlin-migration + SDK-6375 idiomatic pass). Everything below is **ignored by the build today**; new code must not add to it. Regenerate after fixing: `./gradlew :<module>:detektBaselineRelease`, then `python3 config/detekt/burndown.py .` to refresh this file.

**Total baselined findings: 1600**  
core: 1299 · geofence: 101 · hms: 3 · pushtemplates: 197

Baseline files: `clevertap-core/detekt-baseline-release.xml`; `clevertap-geofence/detekt-baseline-release.xml`; `clevertap-hms/detekt-baseline-release.xml`; `clevertap-pushtemplates/detekt-baseline-release.xml`

## Summary by category

| Category | Findings | Rules |
|---|---|---|
| Null safety | 404 | NullableToStringCall (158), UnsafeCallOnNullableType (141), CanBeNonNullable (88), MapGetWithNotNullAssertionOperator (17) |
| Idioms | 3 | UseRequire (1), UseOrEmpty (1), MayBeConst (1) |
| Exceptions | 10 | TooGenericExceptionThrown (5), ThrowingExceptionsWithoutMessageOrCause (5) |
| Library safety | 9 | DoubleMutabilityForCollection (9) |
| Naming | 22 | FunctionParameterNaming (8), PackageNaming (7), VariableNaming (4), FunctionNaming (3) |
| Style | 297 | ReturnCount (83), MagicNumber (75), MaxLineLength (66), ForbiddenMethodCall (54), BracesOnIfStatements (19) |
| Complexity | 784 | LongMethod (246), NestedBlockDepth (186), LongParameterList (124), CyclomaticComplexMethod (121), TooManyFunctions (69), LargeClass (30), ComplexCondition (8) |
| Other | 71 | LoopWithTooManyJumpStatements (22), UnnecessaryAbstractClass (14), UtilityClassWithPublicConstructor (7), NoNameShadowing (6), ThrowsCount (4), UnusedParameter (2), SafeCast (2), MemberNameEqualsClassName (2), InjectDispatcher (2), ArrayPrimitive (2), UselessCallOnNotNull (1), SpreadOperator (1), ObjectLiteralToLambda (1), HasPlatformType (1), FunctionOnlyReturningConstant (1), ForbiddenComment (1), EmptyDefaultConstructor (1), DestructuringDeclarationWithTooManyEntries (1) |

## By rule (with how to fix and where)

### Null safety

- **NullableToStringCall: 158** (core 148, pushtemplates 7, geofence 3). Handle null explicitly instead of printing "null".  
  Top files: `CleverTapAPI.kt` (18), `AnalyticsManager.kt` (13), `CleverTapDisplayUnitContent.kt` (12), `InAppController.kt` (8), `PushProviders.kt` (5), `FileUtils.kt` (4)
- **UnsafeCallOnNullableType: 141** (core 82, geofence 34, pushtemplates 23, hms 2). Single read + `?:`/`?.let`/early return; `requireNotNull(x) { "why" }` where a crash is intended.  
  Top files: `CleverTapAPI.kt` (14), `GifDecoder.kt` (12), `TriggersMatcher.kt` (9), `CTGeofenceAPI.kt` (8), `GifHeaderParser.kt` (5), `Utils.kt` (5)
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

- **FunctionParameterNaming: 8** (core 4, pushtemplates 4). camelCase parameters.  
  Top files: `ContentView.kt` (2), `CTProductConfigController.kt` (1), `CleverTapAPI.kt` (1), `GifDecoder.kt` (1), `SyncListener.kt` (1), `ProductDisplayLinearBigContentView.kt` (1)
- **PackageNaming: 7** (core 7). Lower-case package segments (e.g. `product_config`, `featureFlags`) - public API packages, needs a deprecation plan.  
  Top files: `CTProductConfigConstants.kt` (1), `CTProductConfigController.kt` (1), `CTProductConfigFactory.kt` (1), `CTProductConfigListener.kt` (1), `DefaultXmlParser.kt` (1), `ProductConfigSettings.kt` (1)
- **VariableNaming: 4** (core 4). camelCase, no `m`/`_` prefixes (not for keys/reflection names).  
  Top files: `CTExecutors.kt` (4)
- **FunctionNaming: 3** (core 3). Kotlin camelCase function names (not for public API / reflection names).  
  Top files: `CTInboxController.kt` (1), `InAppController.kt` (1), `PushProviders.kt` (1)

### Style

- **ReturnCount: 83** (core 76, pushtemplates 4, geofence 3). Guard clauses / `when` expressions; max 2 non-guard returns.  
  Top files: `InAppController.kt` (4), `TriggersMatcher.kt` (4), `CleverTapAPI.kt` (3), `NetworkManager.kt` (3), `PushProviders.kt` (3), `Utils.kt` (3)
- **MagicNumber: 75** (core 62, pushtemplates 12, geofence 1). Named constants.  
  Top files: `GifHeaderParser.kt` (18), `GifDecoder.kt` (11), `CtDatabase.kt` (5), `ValidationConfig.kt` (3), `FiveIconBigContentView.kt` (3), `FiveIconSmallContentView.kt` (3)
- **MaxLineLength: 66** (core 60, pushtemplates 5, geofence 1). Wrap lines > 120.  
  Top files: `CleverTapAPI.kt` (8), `InAppController.kt` (5), `AnalyticsManager.kt` (4), `InAppActionHandler.kt` (4), `ManifestValidator.kt` (4), `Utils.kt` (4)
- **ForbiddenMethodCall: 54** (core 32, geofence 14, pushtemplates 8). Log through `Logger`, no `Thread.sleep`/`println`.  
  Top files: `Logger.kt` (38), `PTLog.kt` (6), `DefaultXmlParser.kt` (4), `PushTemplateReceiver.kt` (2), `CTDisplayUnitType.kt` (1), `CleverTapResponse.kt` (1)
- **BracesOnIfStatements: 19** (core 13, pushtemplates 6). Braces on every if/else (repo convention).  
  Top files: `CTInboxMessage.kt` (2), `CleverTapDisplayUnitContent.kt` (2), `DeviceInfo.kt` (2), `EvalRules.kt` (2), `TriggerValue.kt` (2), `VarCache.kt` (2)

### Complexity

- **LongMethod: 246** (core 197, pushtemplates 27, geofence 22). Extract private functions (threshold 30 lines).  
  Top files: `InAppController.kt` (8), `AnalyticsManager.kt` (7), `PushProviders.kt` (7), `CTInAppNotification.kt` (6), `EventDataNormalizer.kt` (5), `EventQueueManager.kt` (5)
- **NestedBlockDepth: 186** (core 165, pushtemplates 15, geofence 6). Guard clauses, extract functions (threshold 3).  
  Top files: `UserEventLogDAOImpl.kt` (7), `Utils.kt` (7), `NetworkManager.kt` (6), `InAppController.kt` (5), `AnalyticsManager.kt` (4), `CTInAppNotification.kt` (4)
- **LongParameterList: 124** (core 102, pushtemplates 22). Parameter objects / default args.  
  Top files: `CleverTapAPI.kt` (9), `EventDataNormalizer.kt` (5), `NotificationBitmapUtils.kt` (5), `ArrayOperationHandler.kt` (4), `CustomTemplateContext.kt` (4), `OperationHandler.kt` (4)
- **CyclomaticComplexMethod: 121** (core 98, pushtemplates 20, geofence 3). Split / `when` tables (threshold 10).  
  Top files: `CTInAppNotification.kt` (5), `TriggersMatcher.kt` (5), `InAppController.kt` (4), `TemplateRenderer.kt` (4), `AnalyticsManager.kt` (3), `DisplayUnitResponse.kt` (3)
- **TooManyFunctions: 69** (core 63, pushtemplates 4, geofence 2). Split the type/file (threshold 15).  
  Top files: `Utils.kt` (3), `CleverTapAPI.kt` (2), `InboxPendingActionsDAO.kt` (2), `AnalyticsManager.kt` (1), `BaseAnalyticsManager.kt` (1), `CTInAppBaseFragment.kt` (1)
- **LargeClass: 30** (core 25, pushtemplates 4, geofence 1). Split responsibilities (threshold 300 lines).  
  Top files: `CleverTapAPI.kt` (2), `Utils.kt` (2), `AnalyticsManager.kt` (1), `CTInAppNotification.kt` (1), `CTInboxBaseMessageViewHolder.kt` (1), `CTInboxController.kt` (1)
- **ComplexCondition: 8** (core 5, pushtemplates 3). Name the sub-conditions.  
  Top files: `TemplateRenderer.kt` (2), `CTVariableUtils.kt` (1), `CleverTapAPI.kt` (1), `CleverTapDisplayUnitContent.kt` (1), `EventQueueManager.kt` (1), `InAppResponse.kt` (1)

### Other

- **LoopWithTooManyJumpStatements: 22** (core 21, pushtemplates 1). See https://detekt.dev/docs/rules  
  Top files: `EventDataNormalizer.kt` (2), `CTInboxController.kt` (1), `CTProductConfigController.kt` (1), `CleverTapAPI.kt` (1), `EventDAOImpl.kt` (1), `GifDecoder.kt` (1)
- **UnnecessaryAbstractClass: 14** (core 12, pushtemplates 2). See https://detekt.dev/docs/rules  
  Top files: `BaseAnalyticsManager.kt` (1), `BaseEventQueueManager.kt` (1), `BaseLocationManager.kt` (1), `BaseSessionManager.kt` (1), `CTInAppBaseFullFragment.kt` (1), `CTInAppBaseFullHtmlFragment.kt` (1)
- **UtilityClassWithPublicConstructor: 7** (core 4, pushtemplates 2, hms 1). See https://detekt.dev/docs/rules  
  Top files: `ActivityLifecycleCallback.kt` (1), `CTVariableUtils.kt` (1), `FileResourcesRepoFactory.kt` (1), `MemoryCreator.kt` (1), `HmsConstants.kt` (1), `PTConstants.kt` (1)
- **NoNameShadowing: 6** (core 5, geofence 1). See https://detekt.dev/docs/rules  
  Top files: `CTInboxController.kt` (2), `GifDecoder.kt` (1), `MemoryAccessObject.kt` (1), `PlayStoreReviewHandler.kt` (1), `PendingIntentFactory.kt` (1)
- **ThrowsCount: 4** (core 4). See https://detekt.dev/docs/rules  
  Top files: `JsonTemplatesProducer.kt` (2), `CustomTemplate.kt` (1), `TemplatesManager.kt` (1)
- **UnusedParameter: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `EventQueueManager.kt` (1), `Utils.kt` (1)
- **SafeCast: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CTVariableUtils.kt` (2)
- **MemberNameEqualsClassName: 2** (core 1, geofence 1). See https://detekt.dev/docs/rules  
  Top files: `CTCaches.kt` (1), `CTGeofenceAPI.kt` (1)
- **InjectDispatcher: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CtDefaultDispatchers.kt` (2)
- **ArrayPrimitive: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CTLocalInApp.kt` (1), `UserEventLogDAOImpl.kt` (1)
- **UselessCallOnNotNull: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CTProductConfigController.kt` (1)
- **SpreadOperator: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `DeviceInfo.kt` (1)
- **ObjectLiteralToLambda: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CleverTapAPI.kt` (1)
- **HasPlatformType: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `NetworkEncryptionManager.kt` (1)
- **FunctionOnlyReturningConstant: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `LeanplumCT.kt` (1)
- **ForbiddenComment: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CleverTapAPI.kt` (1)
- **EmptyDefaultConstructor: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CTMessageDAO.kt` (1)
- **DestructuringDeclarationWithTooManyEntries: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `BitmapDownloadRequestHandlerWithTimeLimit.kt` (1)

## Suggested order for the burn-down pass

1. **Null safety + idioms** (mechanical, highest value, follows the SDK-6375 patterns).
2. **Exceptions**: catching broadly and swallowing is SDK policy (those rules are off); only fix printStackTrace, raw Exception throws and unnamed empty catches.
3. **Library safety**: public data classes and explicit return types (or turn on `explicitApi()`).
4. **Naming/style** in internal code (not public API, keys, reflection-referenced names).
5. **Complexity**: opportunistically when touching a method; don't refactor for the metric alone.
6. **Public docs**: KDoc on bucket-A API first (see docs/KotlinMigrationPublicSurface.md).

