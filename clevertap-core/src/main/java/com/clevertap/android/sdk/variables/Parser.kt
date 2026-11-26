package com.clevertap.android.sdk.variables

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
import androidx.annotation.RestrictTo

/**
 * Parses CleverTap annotations when called via Parser.parseVariables(...)
 * or Parser.parseVariablesForClasses(...).
 *
 * @author Ansh Sachdeva
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
                // a null instance aborts the remaining ones (caught below), as before
                parseVariablesHelper(instance, checkNotNull(instance).javaClass)
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
                // a null class aborts the remaining ones (caught below), as before
                parseVariablesHelper(null, checkNotNull(clazz))
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
            for (field in clazz.fields) {
                if (!field.isAnnotationPresent(Variable::class.java)) {
                    continue
                }
                val annotation: Variable? = field.getAnnotation(Variable::class.java)
                val group = annotation?.group.orEmpty()
                val name = annotation?.name.orEmpty()

                val baseName = name.ifEmpty { field.name }
                val variableName = if (group.isNotEmpty()) { "$group.$baseName" } else { baseName }

                val fieldType = field.type
                when (fieldType.toString()) {
                    "int" -> defineVariable(instance, variableName, field.getInt(instance), NUMBER, field)
                    "byte" -> defineVariable(instance, variableName, field.getByte(instance), NUMBER, field)
                    "short" -> defineVariable(instance, variableName, field.getShort(instance), NUMBER, field)
                    "long" -> defineVariable(instance, variableName, field.getLong(instance), NUMBER, field)
                    "char" -> defineVariable(instance, variableName, field.getChar(instance), NUMBER, field)
                    "float" -> defineVariable(instance, variableName, field.getFloat(instance), NUMBER, field)
                    "double" -> defineVariable(instance, variableName, field.getDouble(instance), NUMBER, field)
                    "boolean" -> defineVariable(instance, variableName, field.getBoolean(instance), BOOLEAN, field)
                    else -> when {
                        fieldType.isPrimitive -> log("Variable $variableName is an unsupported primitive type.")
                        fieldType.isArray -> log("Variable $variableName is an unsupported type of Array.")
                        Map::class.java.isAssignableFrom(fieldType) ->
                            defineVariable(instance, variableName, field.get(instance), DICTIONARY, field)

                        else -> defineVariable(instance, variableName, field.get(instance)?.toString(), STRING, field)
                    }
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
        val definedVar = Var.define(name, value, kind, ctVariables)
        if (definedVar == null) {
            log("Something went wrong, variable '$name' is null, returning")
            return
        }

        val hasInstance = instance != null
        val weakInstance = WeakReference(instance)

        definedVar.addValueChangedCallback(object : VariableCallback<T>() {
            override fun onValueChanged(variable: Var<T>) {
                val instanceFromWeakRef = weakInstance.get()
                if ((hasInstance && instanceFromWeakRef == null) || field == null) {
                    definedVar.removeValueChangedHandler(this)
                    return
                }
                try {
                    val accessible = field.isAccessible
                    if (!accessible) {
                        field.isAccessible = true
                    }
                    field.set(instanceFromWeakRef, definedVar.value())
                    if (!accessible) {
                        field.isAccessible = false
                    }
                } catch (e: IllegalArgumentException) {
                    log("Invalid value ${definedVar.value()} for field ${definedVar.name()}", e)
                } catch (e: IllegalAccessException) {
                    log("Error setting value for field ${definedVar.name()}", e)
                }
            }
        })
    }

    private companion object {
        private fun log(msg: String) = Logger.v("variables", msg)

        private fun log(msg: String, t: Throwable) = Logger.v("variables", msg, t)
    }
}
