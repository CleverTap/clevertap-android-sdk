package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.login.LoginController
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.product_config.CTProductConfigFactory
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache

internal open class CoreState(
    val locationManager: BaseLocationManager,
    val config: CleverTapInstanceConfig,
    val coreMetaData: CoreMetaData,
    val databaseManager: BaseDatabaseManager,
    val deviceInfo: DeviceInfo,
    val eventMediator: EventMediator,
    val localDataStore: LocalDataStore,
    val activityLifeCycleManager: ActivityLifeCycleManager,
    val analyticsManager: AnalyticsManager,
    val baseEventQueueManager: BaseEventQueueManager,
    val cTLockManager: CTLockManager,
    val callbackManager: BaseCallbackManager,
    val controllerManager: ControllerManager,
    val inAppController: InAppController,
    val evaluationManager: EvaluationManager,
    val impressionManager: ImpressionManager,
    val loginController: LoginController,
    val sessionManager: SessionManager,
    val validationResultStack: ValidationResultStack,
    val mainLooperHandler: MainLooperHandler,
    val networkManager: NetworkManager,
    val pushProviders: PushProviders,
    val varCache: VarCache,
    val parser: Parser,
    val cryptHandler: CryptHandler,
    val storeRegistry: StoreRegistry,
    val templatesManager: TemplatesManager,
    val profileValueHandler: ProfileValueHandler,
    var cTVariables: CTVariables
) {
    /**
     *
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     *
     */
    @Deprecated("")
    fun getCtProductConfigController(context: Context?): CTProductConfigController? {
        initProductConfig(context)
        return this.controllerManager!!.getCTProductConfigController()
    }

    /**
     *
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     *
     */
    @Deprecated("")
    private fun initProductConfig(context: Context?) {
        if (this.config.isAnalyticsOnly()) {
            this.config.getLogger()
                .debug(
                    this.config.getAccountId(),
                    "Product Config is not enabled for this instance"
                )
            return
        }
        if (this.controllerManager!!.getCTProductConfigController() == null) {
            this.config.getLogger().verbose(
                config.getAccountId() + ":async_deviceID",
                "Initializing Product Config with device Id = " + this.deviceInfo!!.getDeviceID()
            )
            val ctProductConfigController = CTProductConfigFactory
                .getInstance(
                    context, this.deviceInfo,
                    this.config, analyticsManager, coreMetaData, callbackManager
                )
            this.controllerManager.setCTProductConfigController(ctProductConfigController)
        }
    }
}