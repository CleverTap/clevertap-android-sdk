package com.clevertap.android.sdk.inapp.customtemplates

import android.content.Context
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateType.FUNCTION
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateType.TEMPLATE
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.ACTION
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.BOOLEAN
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.FILE
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.NUMBER
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.STRING
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Representation of the context around an invocation of a [CustomTemplate]. Use the `get` methods to obtain the
 * current values of the arguments. Use [setPresented] and [setDismissed] to notify the SDK of the current state of
 * this invocation.
 */
sealed class CustomTemplateContext private constructor(
    template: CustomTemplate,
    protected val notification: CTInAppNotification,
    inAppListener: InAppListener,
    private val resourceProvider: FileResourceProvider,
    private var dismissListener: ContextDismissListener?,
    protected val logger: Logger
) {

    internal companion object Factory {

        private const val ARGS_KEY_ACTIONS = "actions"

        internal fun createContext(
            template: CustomTemplate,
            notification: CTInAppNotification,
            inAppListener: InAppListener,
            resourceProvider: FileResourceProvider,
            dismissListener: ContextDismissListener?,
            logger: Logger
        ): CustomTemplateContext {
            return when (template.type) {
                TEMPLATE -> TemplateContext(
                    template = template,
                    notification = notification,
                    inAppListener = inAppListener,
                    resourceProvider = resourceProvider,
                    dismissListener = dismissListener,
                    logger = logger
                )

                FUNCTION -> FunctionContext(
                    template = template,
                    notification = notification,
                    inAppListener = inAppListener,
                    resourceProvider = resourceProvider,
                    dismissListener = dismissListener,
                    logger = logger
                )
            }
        }
    }

    val templateName = template.name
    protected val argumentValues = mergeArguments(template.args, notification.customTemplateData?.getArguments())
    internal val inAppListenerRef = WeakReference(inAppListener)
    private val isAction = notification.customTemplateData?.isAction ?: false
    private val isVisual = template.isVisual

    /**
     * Retrieve a [String] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getString(name: String): String? {
        return getValue(name)
    }

    /**
     * Retrieve a [Boolean] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getBoolean(name: String): Boolean? {
        return getValue(name)
    }

    /**
     * Retrieve a [Byte] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getByte(name: String): Byte? {
        return getValue(name)
    }

    /**
     * Retrieve a [Short] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getShort(name: String): Short? {
        return getValue(name)
    }

    /**
     * Retrieve a [Int] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getInt(name: String): Int? {
        return getValue(name)
    }

    /**
     * Retrieve a [Long] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getLong(name: String): Long? {
        return getValue(name)
    }

    /**
     * Retrieve a [Float] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getFloat(name: String): Float? {
        return getValue(name)
    }

    /**
     * Retrieve a [Double] argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getDouble(name: String): Double? {
        return getValue(name)
    }

    /**
     * Retrieve a map of all arguments under [name]. Map arguments will be combined with dot notation arguments. All
     * values are converted to their defined type in the [CustomTemplate]. Action arguments are mapped to their
     * name as [String]. Returns `null` if no arguments are found for the requested map.
     */
    fun getMap(name: String): Map<String, Any>? {
        val mapPrefix = "$name."
        val mapContent = argumentValues.filterKeys { key ->
            key.startsWith(mapPrefix)
        }
        if (mapContent.isEmpty()) {
            return null
        }

        val map = mutableMapOf<String, Any>()
        for ((key, value) in mapContent) {
            val keyParts = key.removePrefix(mapPrefix).split(".")

            val keyValue: Any = if (value is CTInAppAction) {
                getActionName(value)
            } else {
                value
            }

            var currentMap: MutableMap<String, Any> = map
            for ((index, keyPart) in keyParts.withIndex()) {
                if (index == keyParts.lastIndex) {
                    currentMap[keyPart] = keyValue
                } else {
                    @Suppress("UNCHECKED_CAST") var innerMap = currentMap[keyPart] as? MutableMap<String, Any>
                    if (innerMap == null) {
                        innerMap = mutableMapOf()
                        currentMap[keyPart] = innerMap
                    }
                    currentMap = innerMap
                }
            }
        }

        return map
    }

    /**
     * Retrieve an absolute file path argument by [name].
     *
     * @return The argument value or `null` if no such argument is defined for the [CustomTemplate].
     */
    fun getFile(name: String): String? {
        return getValue<String?>(name)?.let {
            resourceProvider.cachedFilePath(it)
        }
    }

    /**
     * Notify the SDK that the current [CustomTemplate] is presented.
     */
    open fun setPresented() {
        if (isAction) {
            return
        }

        val listener = inAppListenerRef.get()
        if (listener != null) {
            listener.inAppNotificationDidShow(notification, null)
        } else {
            logger.debug("CustomTemplates", "Cannot set template as presented")
        }
    }

    /**
     * Notify the SDK that the current [CustomTemplate] is dismissed. The current [CustomTemplate] is considered to be
     * visible to the user until this method is called. Since the SDK can show only one InApp message at a time, all
     * other messages will be queued until the current one is dismissed.
     */
    fun setDismissed() {
        dismissListener?.onDismissContext(this)
        dismissListener = null

        if (isAction && !isVisual) {
            return
        }

        val listener = inAppListenerRef.get()
        if (listener != null) {
            listener.inAppNotificationDidDismiss(null, notification, null)
        } else {
            logger.debug("CustomTemplates", "Cannot set template as dismissed")
        }
        inAppListenerRef.clear()
    }

    private fun mergeArguments(
        defaults: List<TemplateArgument>, overrides: JSONObject?
    ): Map<String, Any> {
        val mergedArguments = mutableMapOf<String, Any>()
        for (argument in defaults) {
            val value: Any? = getOverrideValue(argument, overrides) ?: argument.defaultValue
            if (value != null) {
                mergedArguments[argument.name] = value
            } else {
                continue
            }
        }
        return mergedArguments
    }

    private fun getOverrideValue(argument: TemplateArgument, overrides: JSONObject?): Any? {
        if (overrides?.has(argument.name) != true) {
            return null
        }

        return try {
            when (argument.type) {
                STRING -> overrides.getString(argument.name)
                BOOLEAN -> overrides.getBoolean(argument.name)
                NUMBER -> {
                    when (argument.defaultValue) {
                        is Byte -> overrides.getInt(argument.name).toByte()
                        is Short -> overrides.getInt(argument.name).toShort()
                        is Int -> overrides.getInt(argument.name)
                        is Long -> overrides.getLong(argument.name)
                        is Float -> overrides.getDouble(argument.name).toFloat()
                        else -> overrides.getDouble(argument.name)
                    }
                }
                FILE -> overrides.getString(argument.name)
                ACTION -> CTInAppAction.createFromJson(
                    overrides.optJSONObject(argument.name)?.optJSONObject(ARGS_KEY_ACTIONS)
                )
            }
        } catch (je: JSONException) {
            logger.debug(
                "CustomTemplates",
                "Received argument with invalid type. Expected type: ${argument.type} for argument: ${argument.name}"
            )
            null
        }
    }

    private inline fun <reified T> getValue(name: String): T? {
        return argumentValues[name] as? T
    }

    override fun toString(): String {
        return "CustomTemplateContext {\ntemplateName = $templateName,\nargs = {\n${
            argumentValues.map {
                "\t${it.key} = ${
                    if (it.value is CTInAppAction) {
                        "Action {${getActionName(it.value as? CTInAppAction)}}"
                    } else {
                        it.value.toString()
                    }
                }"
            }.joinToString(",\n")
        }\n}}"
    }

    private fun getActionName(action: CTInAppAction?): String {
        return action?.customTemplateInAppData?.templateName ?: action?.type?.toString() ?: ""
    }

    /**
     * See [CustomTemplateContext].
     */
    class TemplateContext internal constructor(
        template: CustomTemplate,
        notification: CTInAppNotification,
        inAppListener: InAppListener,
        resourceProvider: FileResourceProvider,
        dismissListener: ContextDismissListener?,
        logger: Logger
    ) : CustomTemplateContext(template, notification, inAppListener, resourceProvider, dismissListener, logger) {

        /**
         * Trigger an action argument by name. Open url actions could require an [activityContext] to be launched
         * from a specific activity, otherwise they would be launched with [android.content.Intent.FLAG_ACTIVITY_NEW_TASK]
         */
        fun triggerActionArgument(actionArgumentName: String, activityContext: Context? = null) {
            val actionValue = argumentValues[actionArgumentName]
            if (actionValue !is CTInAppAction) {
                logger.info(
                    "CustomTemplates",
                    "No argument of type action with name $actionArgumentName exists for template $templateName"
                )
                return
            }

            val listener = inAppListenerRef.get()
            if (listener != null) {
                listener.inAppNotificationActionTriggered(
                    notification,
                    actionValue,
                    actionValue.customTemplateInAppData?.templateName ?: actionArgumentName,
                    null,
                    activityContext
                )
            } else {
                logger.debug("CustomTemplates", "Cannot trigger action")
            }
        }
    }

    /**
     * See [CustomTemplateContext].
     */
    class FunctionContext internal constructor(
        template: CustomTemplate,
        notification: CTInAppNotification,
        inAppListener: InAppListener,
        resourceProvider: FileResourceProvider,
        dismissListener: ContextDismissListener?,
        logger: Logger
    ) : CustomTemplateContext(
        template, notification, inAppListener, resourceProvider, dismissListener, logger
    )

    internal fun interface ContextDismissListener {

        fun onDismissContext(context: CustomTemplateContext)
    }
}
