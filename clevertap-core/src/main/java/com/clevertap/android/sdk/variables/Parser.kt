package com.clevertap.android.sdk.variables

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.variables.CTVariableUtils.Companion.BOOLEAN
import com.clevertap.android.sdk.variables.CTVariableUtils.Companion.DICTIONARY
import com.clevertap.android.sdk.variables.CTVariableUtils.Companion.NUMBER
import com.clevertap.android.sdk.variables.CTVariableUtils.Companion.STRING
import com.clevertap.android.sdk.variables.annotations.Variable
import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import java.lang.ref.WeakReference
import java.lang.reflect.Field

/**
 * Parses CleverTap annotations when called via Parser.parseVariables(...)
 * or Parser.parseVariablesForClasses(...).
 *
 * @author Ansh Sachdeva
 */
internal open class Parser(private val ctVariables: CTVariables) {

    /**
     * Parses annotations for all given object instances(eg, if variables are defined in
     * Activity class, then Parser.parseVariables(activityObj) can be called).
     *
     * @param instances Objects of a class
     */
    open fun parseVariables(vararg instances: Any?) {
        try {
            for (instance in instances) {
                parseVariablesHelper(instance, instance!!.javaClass)
            }
        } catch (t: Throwable) {
            log("Error parsing variables", t)
        }
    }

    /**
     * Parses annotations for all given classes.(eg, if variables are defined in a
     * separate 'MyVariables' class, then Parser.parseVariablesForClasses(MyVariables.class) can be
     * called)
     */
    open fun parseVariablesForClasses(vararg classes: Class<*>?) {
        try {
            for (clazz in classes) {
                parseVariablesHelper(null, clazz!!)
            }
        } catch (t: Throwable) {
            log("Error parsing variables", t)
        }
    }

    /**
     * Generic function to parse variables defined in a class or an object
     *
     * For each field defined in the class (i.e clazz instance) with @[Variable] annotation, this function
     * calls [defineVariable], where:
     *
     * @param instance object of a class
     * @param clazz    class instance of a class
     */
    @VisibleForTesting
    internal fun parseVariablesHelper(instance: Any?, clazz: Class<*>) {
        try {
            val fields = clazz.fields

            for (field in fields) {
                var group = ""
                var name = ""
                if (field.isAnnotationPresent(Variable::class.java)) {
                    val annotation: Variable? = field.getAnnotation(Variable::class.java)
                    if (annotation != null) {
                        group = annotation.group
                        name = annotation.name
                    }
                } else {
                    continue
                }

                var variableName = name
                if (TextUtils.isEmpty(variableName)) {
                    variableName = field.name
                }
                if (!TextUtils.isEmpty(group)) {
                    variableName = "$group.$variableName"
                }

                val fieldType = field.type
                val fieldTypeString = fieldType.toString()
                if (fieldTypeString == "int") {
                    defineVariable(instance, variableName, field.getInt(instance), NUMBER, field)
                } else if (fieldTypeString == "byte") {
                    defineVariable(instance, variableName, field.getByte(instance), NUMBER, field)
                } else if (fieldTypeString == "short") {
                    defineVariable(instance, variableName, field.getShort(instance), NUMBER, field)
                } else if (fieldTypeString == "long") {
                    defineVariable(instance, variableName, field.getLong(instance), NUMBER, field)
                } else if (fieldTypeString == "char") {
                    defineVariable(instance, variableName, field.getChar(instance), NUMBER, field)
                } else if (fieldTypeString == "float") {
                    defineVariable(instance, variableName, field.getFloat(instance), NUMBER, field)
                } else if (fieldTypeString == "double") {
                    defineVariable(instance, variableName, field.getDouble(instance), NUMBER, field)
                } else if (fieldTypeString == "boolean") {
                    defineVariable(instance, variableName, field.getBoolean(instance), BOOLEAN, field)
                } else if (fieldType.isPrimitive) {
                    log("Variable $variableName is an unsupported primitive type.")
                } else if (fieldType.isArray) {
                    log("Variable $variableName is an unsupported type of Array.")
                } else if (Map::class.java.isAssignableFrom(fieldType)) {
                    defineVariable(instance, variableName, field.get(instance), DICTIONARY, field)
                } else {
                    val value = field.get(instance)
                    val stringValue = value?.toString()
                    defineVariable(instance, variableName, stringValue, STRING, field)
                }
            }
        } catch (t: Throwable) {
            log("Error parsing variables:", t)
        }
    }

    /**
     * This function simply calls [Var.define] and attached a
     * variable
     * update listener. When variable update listener is called, it sets the values in the field with
     * new data using reflection.
     *
     * @param instance obj of a class
     * @param name     name of the field from class
     * @param value    value of the field
     * @param kind     a string representing the type of field
     * @param field    instance of field itself
     */
    @VisibleForTesting
    internal fun <T> defineVariable(instance: Any?, name: String?, value: T, kind: String?, field: Field?) {
        // we first call var.define(..) with field name, value and kind
        val `var` = Var.define(name, value, kind, ctVariables)
        if (`var` == null) {
            log("Something went wrong, variable '$name' is null, returning")
            return
        }

        val hasInstance = instance != null
        val weakInstance = WeakReference(instance)

        `var`.addValueChangedCallback(object : VariableCallback<T>() {
            override fun onValueChanged(variable: Var<T>) {
                val instanceFromWeakRef = weakInstance.get()
                if ((hasInstance && instanceFromWeakRef == null) || field == null) {
                    `var`.removeValueChangedHandler(this)
                    return
                }
                try {
                    val accessible = field.isAccessible
                    if (!accessible) {
                        field.isAccessible = true
                    }
                    field.set(instanceFromWeakRef, `var`.value())
                    if (!accessible) {
                        field.isAccessible = false
                    }
                } catch (e: IllegalArgumentException) {
                    log("Invalid value " + `var`.value() + " for field " + `var`.name(), e)
                } catch (e: IllegalAccessException) {
                    log("Error setting value for field " + `var`.name(), e)
                }
            }
        })
    }

    private companion object {
        private fun log(msg: String) {
            Logger.v("variables", msg)
        }

        private fun log(msg: String, t: Throwable) {
            Logger.v("variables", msg, t)
        }
    }
}
