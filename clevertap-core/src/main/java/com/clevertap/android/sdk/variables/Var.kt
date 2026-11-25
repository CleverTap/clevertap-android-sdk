package com.clevertap.android.sdk.variables

import android.text.TextUtils
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
open class Var<T> internal constructor(private val ctVariables: CTVariables) {

    private var name: String? = null

    private var nameComponents: Array<String>? = null

    @JvmField
    var stringValue: String? = null

    private var numberValue: Double? = null

    private var defaultValue: T? = null

    private var value: T? = null

    private var kind: String? = null

    private var hadStarted = false // flag to indicate whether the callbacks were invoked for the starting value of the variable

    private val valueChangedHandlers: MutableList<VariableCallback<T>> = ArrayList()

    private val fileReadyHandlers: MutableList<VariableCallback<T>> = ArrayList()

    /**
     * updates the value in [value] by getting the final merged value from
     * [VarCache.getMergedValueFromComponentArray]
     * optionally calls [triggerValueChanged]
     */
    @Synchronized
    open fun update() {
        val oldValue = value
        value = ctVariables.getVarCache().getMergedValueFromComponentArray<T>(nameComponents!!)
        if (value == null && oldValue == null) {
            return
        }
        if (value != null && value == oldValue && hadStarted) {
            return
        }
        cacheComputedValues()
        if (ctVariables.hasVarsRequestCompleted()) {
            hadStarted = true
            triggerValueChanged()

            // trigger file ready, start download if not.
            if (CTVariableUtils.FILE == kind) {
                @Suppress("UNCHECKED_CAST")
                ctVariables.getVarCache().fileVarUpdated(this as Var<String>)
            }
        }
    }

    private fun cacheComputedValues() {
        val value = value
        if (value is String) {
            stringValue = value
            modifyNumberValue(value)
            modifyValue(numberValue)
        } else if (value is Number) {
            stringValue = "" + value
            numberValue = value.toDouble()
            modifyValue(value)
        } else if (value != null && value !is Iterable<*> && value !is Map<*, *>) {
            stringValue = value.toString()
            numberValue = null
        } else {
            stringValue = null
            numberValue = null
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

        val defaultValue = defaultValue
        if (defaultValue is Byte) {
            value = src.toByte() as T
        } else if (defaultValue is Short) {
            value = src.toShort() as T
        } else if (defaultValue is Int) {
            value = src.toInt() as T
        } else if (defaultValue is Long) {
            value = src.toLong() as T
        } else if (defaultValue is Float) {
            value = src.toFloat() as T
        } else if (defaultValue is Double) {
            value = src.toDouble() as T
        } else if (defaultValue is Char) {
            value = src.toInt().toChar() as T
        }
    }

    private fun modifyNumberValue(src: String) {
        try {
            numberValue = java.lang.Double.valueOf(src)
        } catch (e: NumberFormatException) {
            numberValue = null
            val defaultValue = defaultValue
            if (defaultValue is Number) {
                numberValue = defaultValue.toDouble()
            }
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

    override fun toString(): String {
        return if (CTVariableUtils.FILE == kind) {
            val filePath = ctVariables.getVarCache().filePathFromDisk(stringValue)
            "Var($name,$filePath)"
        } else {
            "Var($name,$value)"
        }
    }

    internal fun warnIfNotStarted() {
        if (!ctVariables.hasVarsRequestCompleted() && !printedCallbackWarning) {
            log(
                "CleverTap hasn't finished retrieving values from the server. You should use a callback to make sure the value for " +
                        name + " is ready. Otherwise, your app may not use the most up-to-date value."
            )
            printedCallbackWarning = true
        }
    }

    open fun name(): String? {
        return name
    }

    open fun nameComponents(): Array<String>? {
        return nameComponents
    }

    open fun kind(): String? {
        return kind
    }

    @Suppress("UNCHECKED_CAST")
    open fun defaultValue(): T {
        return defaultValue as T
    }

    @Suppress("UNCHECKED_CAST")
    open fun value(): T {
        warnIfNotStarted()

        return if (CTVariableUtils.FILE == kind) {
            ctVariables.getVarCache().filePathFromDisk(stringValue) as T
        } else {
            value as T
        }
    }

    internal fun rawFileValue(): String? {
        return if (CTVariableUtils.FILE == kind) {
            stringValue
        } else {
            null
        }
    }

    open fun addValueChangedCallback(callback: VariableCallback<T>?) {
        if (callback == null) {
            log("Invalid callback parameter provided.")
            return
        }

        synchronized(valueChangedHandlers) {
            valueChangedHandlers.add(callback)
        }

        if (ctVariables.hasVarsRequestCompleted()) {
            callback.onValueChanged(this)
        }
    }

    open fun removeValueChangedHandler(handler: VariableCallback<T>?) {
        synchronized(valueChangedHandlers) {
            valueChangedHandlers.remove(handler)
        }
    }

    open fun triggerFileIsReady() {
        synchronized(fileReadyHandlers) {
            for (callback in fileReadyHandlers) {
                callback.setVariable(this)
                Utils.runOnUiThread(callback)
            }
        }
    }

    open fun numberValue(): Number? {
        warnIfNotStarted()
        return numberValue
    }

    open fun stringValue(): String? {
        warnIfNotStarted()
        return if (CTVariableUtils.FILE == kind) {
            ctVariables.getVarCache().filePathFromDisk(stringValue)
        } else {
            stringValue
        }
    }

    internal fun clearStartFlag() {
        hadStarted = false
    }

    open fun addFileReadyHandler(handler: VariableCallback<T>) {
        synchronized(fileReadyHandlers) {
            fileReadyHandlers.add(handler)
        }
    }

    /**
     * Removes file ready handler for a given variable.
     *
     * @param handler Handler to be removed.
     */
    open fun removeFileReadyHandler(handler: VariableCallback<T>) {
        synchronized(fileReadyHandlers) {
            fileReadyHandlers.remove(handler)
        }
    }

    companion object {

        private const val TAG = "variable"

        private var printedCallbackWarning = false

        private fun log(msg: String) {
            Logger.v(TAG, msg)
        }

        /**
         * SDK-internal: takes the internal [CTVariables]; customers use `CleverTapAPI#defineVariable`.
         */
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
        @JvmStatic
        @JvmName("define")
        internal fun <T> define(name: String?, defaultValue: T, kind: String?, ctVariables: CTVariables): Var<T>? {
            if (TextUtils.isEmpty(name)) {
                log("Empty name parameter provided.")
                return null
            }
            if (name!!.startsWith(".") || name.endsWith(".")) {
                log("Variable name starts or ends with a `.` which is not allowed: $name")
                return null
            }
            if (CTVariableUtils.FILE != kind && defaultValue == null) {
                Logger.d("Invalid Operation! Null values are not allowed as default values when defining the variable '$name'.")
                return null
            }

            val existing = ctVariables.getVarCache().getVariable<T>(name)
            if (existing != null) {
                return existing
            }

            val `var` = Var<T>(ctVariables)
            try {
                `var`.name = name
                `var`.nameComponents = CTVariableUtils.getNameComponents(name)
                `var`.defaultValue = defaultValue
                `var`.value = defaultValue
                `var`.kind = kind
                `var`.cacheComputedValues()
                ctVariables.getVarCache().registerVariable(`var`)
                `var`.update()
            } catch (t: Throwable) {
                Logger.v(TAG, "Error defining variable: $name", t)
            }
            return `var`
        }
    }
}
