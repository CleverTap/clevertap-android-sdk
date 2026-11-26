# detekt baseline burn-down (SDK-6378)

Generated from the per-module baselines (`<module>/detekt-baseline-release.xml`) on the post-migration code (feat/kotlin-migration + SDK-6375 idiomatic pass). Everything below is **ignored by the build today**; new code must not add to it. Regenerate after fixing: `./gradlew :<module>:detektBaselineRelease`, then `python3 config/detekt/burndown.py .` to refresh this file.

**Total baselined findings: 2700**  
core: 2189 · geofence: 158 · hms: 6 · pushtemplates: 347

Baseline files: `clevertap-core/detekt-baseline-release.xml`; `clevertap-geofence/detekt-baseline-release.xml`; `clevertap-hms/detekt-baseline-release.xml`; `clevertap-pushtemplates/detekt-baseline-release.xml`

## Summary by category

| Category | Findings | Rules |
|---|---|---|
| Null safety | 418 | NullableToStringCall (158), UnsafeCallOnNullableType (141), CanBeNonNullable (88), MapGetWithNotNullAssertionOperator (17), UnnecessarySafeCall (13), UnnecessaryNotNullOperator (1) |
| Idioms | 69 | UseOrEmpty (31), UseCheckOrError (11), UseLet (7), VarCouldBeVal (6), UnnecessaryLet (6), UseRequire (3), UseIsNullOrEmpty (3), MayBeConst (1), ForbiddenVoid (1) |
| Exceptions | 10 | TooGenericExceptionThrown (5), ThrowingExceptionsWithoutMessageOrCause (5) |
| Library safety | 21 | DoubleMutabilityForCollection (21) |
| Naming | 38 | FunctionParameterNaming (14), FunctionNaming (9), VariableNaming (8), PackageNaming (7) |
| Style | 801 | BracesOnIfStatements (246), MaxLineLength (228), MagicNumber (190), ReturnCount (83), ForbiddenMethodCall (54) |
| Complexity | 773 | LongMethod (232), NestedBlockDepth (189), LongParameterList (124), CyclomaticComplexMethod (124), TooManyFunctions (69), LargeClass (27), ComplexCondition (8) |
| Public docs | 305 | UndocumentedPublicFunction (224), UndocumentedPublicProperty (47), UndocumentedPublicClass (34) |
| Other | 265 | NewLineAtEndOfFile (168), LoopWithTooManyJumpStatements (22), UnnecessaryAbstractClass (14), EmptyFunctionBlock (8), UtilityClassWithPublicConstructor (7), NoNameShadowing (6), WildcardImport (5), UnusedPrivateProperty (5), UnusedParameter (4), ThrowsCount (4), UnnecessaryApply (3), SafeCast (2), ObjectLiteralToLambda (2), MemberNameEqualsClassName (2), InjectDispatcher (2), ConstructorParameterNaming (2), ArrayPrimitive (2), UselessCallOnNotNull (1), SpreadOperator (1), HasPlatformType (1), FunctionOnlyReturningConstant (1), ForbiddenComment (1), EmptyDefaultConstructor (1), DestructuringDeclarationWithTooManyEntries (1) |

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
- **UnnecessarySafeCall: 13** (core 13). Drop `?.` on a non-null value.  
  Top files: `UserEventLogDAOImpl.kt` (5), `InboxMessageDAOImpl.kt` (2), `PushNotificationDAOImpl.kt` (2), `UserProfileDAOImpl.kt` (2), `InboxDeleteCall.kt` (1), `UninstallTimestampDAOImpl.kt` (1)
- **UnnecessaryNotNullOperator: 1** (core 1). Drop the `!!` on a non-null value.  
  Top files: `EventQueueManager.kt` (1)

### Idioms

- **UseOrEmpty: 31** (core 28, pushtemplates 2, geofence 1). `orEmpty()`.  
  Top files: `PushProviders.kt` (3), `CleverTapDisplayUnitContent.kt` (2), `EventPropertyKeyNormalizer.kt` (2), `PushNotificationUtil.kt` (2), `ProgressStyle.kt` (2), `CTHtmlBannerCallbacksBridge.kt` (1)
- **UseCheckOrError: 11** (geofence 6, core 5). `check(cond)`/`error(msg)` instead of `throw IllegalStateException`.  
  Top files: `CleverTapAPI.kt` (2), `InAppController.kt` (2), `CTGeofenceAPI.kt` (2), `CTGeofenceFactory.kt` (2), `CTLocationFactory.kt` (2), `GifHeaderParser.kt` (1)
- **UseLet: 7** (pushtemplates 5, core 1, hms 1). `x?.let { }` instead of if/else with a null branch.  
  Top files: `TemplateRenderer.kt` (2), `TimerStyle.kt` (2), `CleverTapDisplayUnitContent.kt` (1), `CTHmsMessageHandler.kt` (1), `VerticalImageContentView.kt` (1)
- **VarCouldBeVal: 6** (core 4, pushtemplates 2). `val` for never-reassigned locals/properties.  
  Top files: `ProductDisplayLinearBigContentView.kt` (2), `CTLocalInApp.kt` (1), `ExoplayerHandle.kt` (1), `ImpressionManager.kt` (1), `Media3Handle.kt` (1)
- **UnnecessaryLet: 6** (core 6). Remove a `let` that adds nothing.  
  Top files: `NdStoreProvider.kt` (3), `AESCrypt.kt` (1), `CTLocalInApp.kt` (1), `Media3Handle.kt` (1)
- **UseRequire: 3** (core 2, geofence 1). `require(cond)` instead of `throw IllegalArgumentException`.  
  Top files: `GifDecoder.kt` (1), `PushProviders.kt` (1), `Utils.kt` (1)
- **UseIsNullOrEmpty: 3** (geofence 3). `isNullOrEmpty()`.  
  Top files: `GoogleGeofenceAdapter.kt` (2), `CTGeofenceAPI.kt` (1)
- **MayBeConst: 1** (pushtemplates 1). `const val` so Java gets a static final field.  
  Top files: `PTConstants.kt` (1)
- **ForbiddenVoid: 1** (geofence 1). `Unit` instead of `Void` outside Java generic type arguments.  
  Top files: `GoogleGeofenceAdapter.kt` (1)

### Exceptions

- **TooGenericExceptionThrown: 5** (pushtemplates 3, core 2). Throw a specific exception type.  
  Top files: `Utils.kt` (2), `CleverTapFactory.kt` (1), `InputBoxStyle.kt` (1), `PushTemplateReceiver.kt` (1)
- **ThrowingExceptionsWithoutMessageOrCause: 5** (core 4, pushtemplates 1). Add a message/cause.  
  Top files: `CTFcmMessageHandler.kt` (1), `CTInboxActivity.kt` (1), `InAppNotificationActivity.kt` (1), `PushProviders.kt` (1), `PushTemplateReceiver.kt` (1)

### Library safety

- **DoubleMutabilityForCollection: 21** (core 20, pushtemplates 1). `val` + mutable collection, or `var` + read-only collection.  
  Top files: `CTInAppNotification.kt` (2), `CTInboxMessage.kt` (2), `CustomTemplateContext.kt` (2), `EvaluationManager.kt` (2), `NdEvaluationManager.kt` (2), `CTInAppAction.kt` (1)

### Naming

- **FunctionParameterNaming: 14** (pushtemplates 9, core 5). camelCase parameters.  
  Top files: `ContentView.kt` (5), `TimerSmallContentView.kt` (2), `CTProductConfigController.kt` (1), `CleverTapAPI.kt` (1), `GifDecoder.kt` (1), `IdentitySet.kt` (1)
- **FunctionNaming: 9** (core 9). Kotlin camelCase function names (not for public API / reflection names).  
  Top files: `CTInboxController.kt` (4), `BaseCallbackManager.kt` (2), `InAppController.kt` (1), `InboxV2Response.kt` (1), `PushProviders.kt` (1)
- **VariableNaming: 8** (core 6, geofence 1, pushtemplates 1). camelCase, no `m`/`_` prefixes (not for keys/reflection names).  
  Top files: `CTExecutors.kt` (4), `InAppTimerManager.kt` (1), `ValidationResult.kt` (1), `CTGeofenceTaskManager.kt` (1), `TemplateRenderer.kt` (1)
- **PackageNaming: 7** (core 7). Lower-case package segments (e.g. `product_config`, `featureFlags`) - public API packages, needs a deprecation plan.  
  Top files: `CTProductConfigConstants.kt` (1), `CTProductConfigController.kt` (1), `CTProductConfigFactory.kt` (1), `CTProductConfigListener.kt` (1), `DefaultXmlParser.kt` (1), `ProductConfigSettings.kt` (1)

### Style

- **BracesOnIfStatements: 246** (core 209, pushtemplates 33, geofence 2, hms 2). Braces on every if/else (repo convention).  
  Top files: `DeviceInfo.kt` (6), `PushPreviewKey.kt` (5), `CTInAppBaseFullFragment.kt` (4), `CTMessageDAO.kt` (4), `CleverTapDisplayUnit.kt` (4), `CleverTapDisplayUnitContent.kt` (4)
- **MaxLineLength: 228** (core 185, pushtemplates 40, geofence 3). Wrap lines > 120.  
  Top files: `CleverTapAPI.kt` (17), `Constants.kt` (11), `TemplateRenderer.kt` (9), `DisplayUnitResponse.kt` (7), `EventDataNormalizer.kt` (7), `ManifestValidator.kt` (7)
- **MagicNumber: 190** (core 161, pushtemplates 25, geofence 4). Named constants.  
  Top files: `CTInboxBaseMessageViewHolder.kt` (18), `GifHeaderParser.kt` (18), `GifDecoder.kt` (11), `CTInAppBaseFullFragment.kt` (7), `PIPConfigFactory.kt` (6), `Utils.kt` (6)
- **ReturnCount: 83** (core 76, pushtemplates 4, geofence 3). Guard clauses / `when` expressions; max 2 non-guard returns.  
  Top files: `InAppController.kt` (4), `TriggersMatcher.kt` (4), `CleverTapAPI.kt` (3), `NetworkManager.kt` (3), `PushProviders.kt` (3), `Utils.kt` (3)
- **ForbiddenMethodCall: 54** (core 32, geofence 14, pushtemplates 8). Log through `Logger`, no `Thread.sleep`/`println`.  
  Top files: `Logger.kt` (38), `PTLog.kt` (6), `DefaultXmlParser.kt` (4), `PushTemplateReceiver.kt` (2), `CTDisplayUnitType.kt` (1), `CleverTapResponse.kt` (1)

### Complexity

- **LongMethod: 232** (core 185, pushtemplates 25, geofence 22). Extract private functions (threshold 30 lines).  
  Top files: `InAppController.kt` (8), `AnalyticsManager.kt` (7), `PushProviders.kt` (7), `CTInAppNotification.kt` (6), `EventQueueManager.kt` (5), `Utils.kt` (5)
- **NestedBlockDepth: 189** (core 168, pushtemplates 15, geofence 6). Guard clauses, extract functions (threshold 3).  
  Top files: `UserEventLogDAOImpl.kt` (7), `Utils.kt` (7), `NetworkManager.kt` (6), `InAppController.kt` (5), `AnalyticsManager.kt` (4), `CTInAppNotification.kt` (4)
- **LongParameterList: 124** (core 102, pushtemplates 22). Parameter objects / default args.  
  Top files: `CleverTapAPI.kt` (9), `EventDataNormalizer.kt` (5), `NotificationBitmapUtils.kt` (5), `ArrayOperationHandler.kt` (4), `CustomTemplateContext.kt` (4), `OperationHandler.kt` (4)
- **CyclomaticComplexMethod: 124** (core 100, pushtemplates 21, geofence 3). Split / `when` tables (threshold 10).  
  Top files: `CTInAppNotification.kt` (5), `TriggersMatcher.kt` (5), `InAppController.kt` (4), `PushProviders.kt` (4), `TemplateRenderer.kt` (4), `AnalyticsManager.kt` (3)
- **TooManyFunctions: 69** (core 63, pushtemplates 4, geofence 2). Split the type/file (threshold 15).  
  Top files: `Utils.kt` (3), `CleverTapAPI.kt` (2), `InboxPendingActionsDAO.kt` (2), `AnalyticsManager.kt` (1), `BaseAnalyticsManager.kt` (1), `CTInAppBaseFragment.kt` (1)
- **LargeClass: 27** (core 22, pushtemplates 4, geofence 1). Split responsibilities (threshold 300 lines).  
  Top files: `CleverTapAPI.kt` (2), `Utils.kt` (2), `AnalyticsManager.kt` (1), `CTInAppNotification.kt` (1), `CTInboxBaseMessageViewHolder.kt` (1), `CTInboxController.kt` (1)
- **ComplexCondition: 8** (core 5, pushtemplates 3). Name the sub-conditions.  
  Top files: `TemplateRenderer.kt` (2), `CTVariableUtils.kt` (1), `CleverTapAPI.kt` (1), `CleverTapDisplayUnitContent.kt` (1), `EventQueueManager.kt` (1), `InAppResponse.kt` (1)

### Public docs

- **UndocumentedPublicFunction: 224** (core 195, geofence 28, pushtemplates 1). KDoc on public functions.  
  Top files: `CleverTapInstanceConfig.kt` (31), `CleverTapAPI.kt` (25), `LeanplumCT.kt` (22), `Logger.kt` (20), `CTLocalInApp.kt` (18), `CTInboxStyleConfig.kt` (14)
- **UndocumentedPublicProperty: 47** (core 41, geofence 6). KDoc on public properties.  
  Top files: `CTInAppNotification.kt` (21), `Logger.kt` (4), `CTLocalInApp.kt` (3), `CTInAppNotificationButton.kt` (2), `CTInboxImageData.kt` (2), `CleverTapInstanceConfig.kt` (2)
- **UndocumentedPublicClass: 34** (core 28, geofence 3, pushtemplates 3). KDoc on public types (start with bucket-A API; @RestrictTo types can stay brief).  
  Top files: `CTLocalInApp.kt` (8), `Logger.kt` (3), `TemplateRenderer.kt` (2), `Application.kt` (1), `CTInAppAction.kt` (1), `CTInAppNotification.kt` (1)

### Other

- **NewLineAtEndOfFile: 168** (core 119, pushtemplates 49). See https://detekt.dev/docs/rules  
  Top files: `ExoplayerHandle.kt` (2), `Extensions.kt` (2), `Media3Handle.kt` (2), `AESCrypt.kt` (1), `AESGCMCrypt.kt` (1), `AnalyticsManagerBundler.kt` (1)
- **LoopWithTooManyJumpStatements: 22** (core 21, pushtemplates 1). See https://detekt.dev/docs/rules  
  Top files: `EventDataNormalizer.kt` (2), `CTInboxController.kt` (1), `CTProductConfigController.kt` (1), `CleverTapAPI.kt` (1), `EventDAOImpl.kt` (1), `GifDecoder.kt` (1)
- **UnnecessaryAbstractClass: 14** (core 12, pushtemplates 2). See https://detekt.dev/docs/rules  
  Top files: `BaseAnalyticsManager.kt` (1), `BaseEventQueueManager.kt` (1), `BaseLocationManager.kt` (1), `BaseSessionManager.kt` (1), `CTInAppBaseFullFragment.kt` (1), `CTInAppBaseFullHtmlFragment.kt` (1)
- **EmptyFunctionBlock: 8** (core 8). See https://detekt.dev/docs/rules  
  Top files: `ActivityLifecycleCallback.kt` (1), `CTInAppBasePartialNativeFragment.kt` (1), `CTInAppHtmlBannerOverlay.kt` (1), `ExoplayerPlayerListener.kt` (1), `Media3PlayerListener.kt` (1), `MediaPlayerRecyclerView.kt` (1)
- **UtilityClassWithPublicConstructor: 7** (core 4, pushtemplates 2, hms 1). See https://detekt.dev/docs/rules  
  Top files: `ActivityLifecycleCallback.kt` (1), `CTVariableUtils.kt` (1), `FileResourcesRepoFactory.kt` (1), `MemoryCreator.kt` (1), `HmsConstants.kt` (1), `PTConstants.kt` (1)
- **NoNameShadowing: 6** (core 5, geofence 1). See https://detekt.dev/docs/rules  
  Top files: `CTInboxController.kt` (2), `GifDecoder.kt` (1), `MemoryAccessObject.kt` (1), `PlayStoreReviewHandler.kt` (1), `PendingIntentFactory.kt` (1)
- **WildcardImport: 5** (core 3, pushtemplates 2). See https://detekt.dev/docs/rules  
  Top files: `ValidatorFactory.kt` (2), `DBAdapter.kt` (1), `ExoplayerPlayerListener.kt` (1), `QueueHeaderBuilder.kt` (1)
- **UnusedPrivateProperty: 5** (pushtemplates 3, core 2). See https://detekt.dev/docs/rules  
  Top files: `SizeChecker.kt` (3), `InActionStorageStrategy.kt` (2)
- **UnusedParameter: 4** (core 4). See https://detekt.dev/docs/rules  
  Top files: `ActivityLifeCycleManager.kt` (1), `EventQueueManager.kt` (1), `PIPExpandedView.kt` (1), `Utils.kt` (1)
- **ThrowsCount: 4** (core 4). See https://detekt.dev/docs/rules  
  Top files: `JsonTemplatesProducer.kt` (2), `CustomTemplate.kt` (1), `TemplatesManager.kt` (1)
- **UnnecessaryApply: 3** (core 3). See https://detekt.dev/docs/rules  
  Top files: `InAppStore.kt` (3)
- **SafeCast: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CTVariableUtils.kt` (2)
- **ObjectLiteralToLambda: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CTInAppBaseFullFragment.kt` (1), `CleverTapAPI.kt` (1)
- **MemberNameEqualsClassName: 2** (core 1, geofence 1). See https://detekt.dev/docs/rules  
  Top files: `CTCaches.kt` (1), `CTGeofenceAPI.kt` (1)
- **InjectDispatcher: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CtDefaultDispatchers.kt` (2)
- **ConstructorParameterNaming: 2** (pushtemplates 2). See https://detekt.dev/docs/rules  
  Top files: `TimerBigContentView.kt` (1), `TimerSmallContentView.kt` (1)
- **ArrayPrimitive: 2** (core 2). See https://detekt.dev/docs/rules  
  Top files: `CTLocalInApp.kt` (1), `UserEventLogDAOImpl.kt` (1)
- **UselessCallOnNotNull: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `CTProductConfigController.kt` (1)
- **SpreadOperator: 1** (core 1). See https://detekt.dev/docs/rules  
  Top files: `DeviceInfo.kt` (1)
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

