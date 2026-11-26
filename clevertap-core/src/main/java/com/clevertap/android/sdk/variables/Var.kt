package com.clevertap.android.sdk.variables

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.variables.callbacks.VariableCallback

/**
 * CleverTap variable.
 *
 * @param T Type of the variable. Can be Boolean, Byte, Short, Integer, Long, Float, Double,
 * Character, String, or Map. You may nest maps arbitrarily.
 * @author Ansh Sachdeva
 */
public open class Var<T> internal constructor(
    private val ctVariables: CTVariables,
    private val name: String,
    private val nameComponents: Array<String>,
    private val defaultValue: T?,
    private val kind: String?
) {

    @JvmField
    public var stringValue: String? = null

    private var numberValue: Double? = null

    private var value: T? = defaultValue

    private var hadStarted = false // flag to indicate whether the callbacks were invoked for the starting value of the variable

    private val valueChangedHandlers: MutableList<VariableCallback<T>> = ArrayList()

    private val fileReadyHandlers: MutableList<VariableCallback<T>> = ArrayList()

    /**
     * updates the value in [value] by getting the final merged value from
     * [VarCache.getMergedValueFromComponentArray]
     * optionally calls [triggerValueChanged]
     */
    @Synchronized
    public open fun update() {
        val oldValue = value
        value = ctVariables.varCache.getMergedValueFromComponentArray<T>(nameComponents)
        if (value == null && oldValue == null) {
            return
        }
        if (value != null && value == oldValue && hadStarted) {
            return
        }
        cacheComputedValues()
        if (ctVariables.hasVarsRequestCompleted) {
            hadStarted = true
            triggerValueChanged()

            // trigger file ready, start download if not.
            if (isFileKind) {
                @Suppress("UNCHECKED_CAST")
                ctVariables.varCache.fileVarUpdated(this as Var<String>)
            }
        }
    }

    private val isFileKind: Boolean
        get() = CTVariableUtils.FILE == kind

    private fun cacheComputedValues() {
        when (val value = value) {
            is String -> {
                stringValue = value
                modifyNumberValue(value)
                modifyValue(numberValue)
            }

            is Number -> {
                stringValue = value.toString()
                numberValue = value.toDouble()
                modifyValue(value)
            }

            null, is Iterable<*>, is Map<*, *> -> {
                stringValue = null
                numberValue = null
            }

            else -> {
                stringValue = value.toString()
                numberValue = null
            }
        }
    }

    /**
     * values come from the server as Number type. so we type cast in here to the actual
     * java type accordingly  using the defaultValue's type
     */
    @Suppress("UNCHECKED_CAST")
    private fun modifyValue(src: Number?) {
        if (src == null) {
            return
        }

        value = when (defaultValue) {
            is Byte -> src.toByte() as T
            is Short -> src.toShort() as T
            is Int -> src.toInt() as T
            is Long -> src.toLong() as T
            is Float -> src.toFloat() as T
            is Double -> src.toDouble() as T
            is Char -> src.toInt().toChar() as T
            else -> return
        }
    }

    private fun modifyNumberValue(src: String) {
        numberValue = try {
            src.toDouble()
        } catch (e: NumberFormatException) {
            (defaultValue as? Number)?.toDouble()
        }
    }

    //triggers the user's callbacks of variable changing (VariableCallback)
    private fun triggerValueChanged() {
        synchronized(valueChangedHandlers) {
            for (callback in valueChangedHandlers) {
                callback.setVariable(this)
                Utils.runOnUiThread(callback)
            }
        }
    }

    override fun toString(): String = if (isFileKind) {
        "Var($name,${ctVariables.varCache.filePathFromDisk(stringValue)})"
    } else {
        "Var($name,$value)"
    }

    internal fun warnIfNotStarted() {
        if (!ctVariables.hasVarsRequestCompleted && !printedCallbackWarning) {
            log(
                "CleverTap hasn't finished retrieving values from the server. You should use a callback to make sure the value for " +
                    "$name is ready. Otherwise, your app may not use the most up-to-date value."
            )
            printedCallbackWarning = true
        }
    }

    public open fun name(): String? = name

    public open fun nameComponents(): Array<String>? = nameComponents

    public open fun kind(): String? = kind

    @Suppress("UNCHECKED_CAST")
    public open fun defaultValue(): T = defaultValue as T

    @Suppress("UNCHECKED_CAST")
    public open fun value(): T {
        warnIfNotStarted()
        return if (isFileKind) {
            ctVariables.varCache.filePathFromDisk(stringValue) as T
        } else {
            value as T
        }
    }

    internal fun rawFileValue(): String? = if (isFileKind) stringValue else null

    public open fun addValueChangedCallback(callback: VariableCallback<T>?) {
        if (callback == null) {
            log("Invalid callback parameter provided.")
            return
        }

        synchronized(valueChangedHandlers) {
            valueChangedHandlers.add(callback)
        }

        if (ctVariables.hasVarsRequestCompleted) {
            callback.onValueChanged(this)
        }
    }

    public open fun removeValueChangedHandler(handler: VariableCallback<T>?) {
        synchronized(valueChangedHandlers) {
            valueChangedHandlers.remove(handler)
        }
    }

    public open fun triggerFileIsReady() {
        synchronized(fileReadyHandlers) {
            for (callback in fileReadyHandlers) {
                callback.setVariable(this)
                Utils.runOnUiThread(callback)
            }
        }
    }

    public open fun numberValue(): Number? {
        warnIfNotStarted()
        return numberValue
    }

    public open fun stringValue(): String? {
        warnIfNotStarted()
        return if (isFileKind) {
            ctVariables.varCache.filePathFromDisk(stringValue)
        } else {
            stringValue
        }
    }

    internal fun clearStartFlag() {
        hadStarted = false
    }

    public open fun addFileReadyHandler(handler: VariableCallback<T>) {
        synchronized(fileReadyHandlers) {
            fileReadyHandlers.add(handler)
        }
    }

    /**
     * Removes file ready handler for a given variable.
     *
     * @param handler Handler to be removed.
     */
    public open fun removeFileReadyHandler(handler: VariableCallback<T>) {
        synchronized(fileReadyHandlers) {
            fileReadyHandlers.remove(handler)
        }
    }

    public companion object {

        private const val TAG = "variable"

        private var printedCallbackWarning = false

        private fun log(msg: String) {
            Logger.v(TAG, msg)
        }

        /**
         * SDK-internal: takes the internal [CTVariables]; customers use `CleverTapAPI#defineVariable`.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @JvmName("define")
        internal fun <T> define(name: String?, defaultValue: T, ctVariables: CTVariables): Var<T>? {
            val type = CTVariableUtils.kindFromValue(defaultValue)
            return define(name, defaultValue, type, ctVariables)
        }

        /**
         * creates a  [Var] object from given params and calls [VarCache.registerVariable]
         *
         * @param name         name of variable
         * @param defaultValue value
         * @param kind         datatype as string
         * @param T          Type of the variable.
         * @return instance of a [Var] class
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @JvmName("define")
        internal fun <T> define(name: String?, defaultValue: T, kind: String?, ctVariables: CTVariables): Var<T>? {
            if (name.isNullOrEmpty()) {
                log("Empty name parameter provided.")
                return null
            }
            if (name.startsWith(".") || name.endsWith(".")) {
                log("Variable name starts or ends with a `.` which is not allowed: $name")
                return null
            }
            if (CTVariableUtils.FILE != kind && defaultValue == null) {
                Logger.d("Invalid Operation! Null values are not allowed as default values when defining the variable '$name'.")
                return null
            }

            ctVariables.varCache.getVariable<T>(name)?.let { return it }

            val variable = Var(ctVariables, name, CTVariableUtils.getNameComponents(name), defaultValue, kind)
            try {
                variable.cacheComputedValues()
                ctVariables.varCache.registerVariable(variable)
                variable.update()
            } catch (t: Throwable) {
                Logger.v(TAG, "Error defining variable: $name", t)
            }
            return variable
        }
    }
}
