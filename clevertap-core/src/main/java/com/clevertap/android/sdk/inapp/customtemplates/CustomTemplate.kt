package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateType.FUNCTION
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateType.TEMPLATE
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.ACTION
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.BOOLEAN
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.FILE
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.NUMBER
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.STRING

/**
 * A definition of a custom template. Can be a function or a code template.
 * Instances are uniquely identified by their name.
 */
class CustomTemplate private constructor(
    /**
     * The name of the template. Must be unique and non-blank.
     */
    val name: String,

    /**
     * The presenter associated with this template.
     */
    val presenter: CustomTemplatePresenter<*>,

    /**
     * Whether the template has UI or not. If set to `true` the template is registered as part of the in-apps queue
     * and must be explicitly dismissed before other in-apps can be shown. If set to `false` the template is executed
     * directly and does not require dismissal nor it impedes other in-apps.
     */
    val isVisual: Boolean,

    internal val args: List<TemplateArgument>,
    internal val type: CustomTemplateType
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomTemplate

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * Builder for [CustomTemplate] functions. Function arguments' names cannot contain "."
     * @param isVisual See [CustomTemplate.isVisual]
     */
    class FunctionBuilder(isVisual: Boolean) :
        Builder<FunctionPresenter, FunctionBuilder>(FUNCTION, allowHierarchicalNames = false, isVisual) {

        override val thisRef: FunctionBuilder = this
    }

    /**
     * Builder for [CustomTemplate] code templates. "." characters in template arguments' names denote hierarchical
     * structure. They are treated the same way as the keys provided by [mapArgument]. If a higher-level name (to the left
     * of a . symbol) matches a map argument's name it is treated the same as if the argument was part of the map itself.
     *
     * For example, the following code snippets define identical arguments:
     * ```
     * builder.mapArgument(
     *     name = "map",
     *     value = mapOf (
     *         "a" to 5,
     *         "b" to 6
     *     )
     * )
     * ```
     * and
     * ```
     * builder.intArgument("map.a", 5)
     * builder.intArgument("map.b", 6)
     * ```
     */
    class TemplateBuilder :
        Builder<TemplatePresenter, TemplateBuilder>(TEMPLATE, allowHierarchicalNames = true, isVisual = true) {

        override val thisRef: TemplateBuilder = this

        /**
         * Add a map structure to the arguments of the [CustomTemplate]. The [name] should be unique across all
         * arguments and also all keys in [value] should form unique names across all arguments.
         *
         * @param value The map must be non-empty. Values can be of type [Byte], [Short], [Int], [Long], [Float],
         * [Double], [Boolean], [String] or another [Map]<String, Any> which values can also be of the same types.
         */
        fun mapArgument(name: String, value: Map<String, Any>): TemplateBuilder {
            if (value.isEmpty()) {
                throw CustomTemplateException("Map argument must not be empty")
            }

            for (mapEntry in value) {
                val argValue = mapEntry.value
                val argName = "$name.${mapEntry.key}"

                @Suppress("UNCHECKED_CAST")
                when (argValue) {
                    is Byte -> byteArgument(argName, argValue)
                    is Short -> shortArgument(argName, argValue)
                    is Int -> intArgument(argName, argValue)
                    is Long -> longArgument(argName, argValue)
                    is Float -> floatArgument(argName, argValue)
                    is Double -> doubleArgument(argName, argValue)
                    is Boolean -> booleanArgument(argName, argValue)
                    is String -> stringArgument(argName, argValue)
                    is Map<*, *> -> mapArgument(argName, argValue as Map<String, Any>)
                    else -> throw CustomTemplateException("Unsupported value type ${argValue.javaClass} for argument $argName")
                }
            }
            return this
        }

        fun actionArgument(name: String): TemplateBuilder {
            addArgument(name, ACTION, null)
            return this
        }
    }

    sealed class Builder<P : CustomTemplatePresenter<*>, T : Builder<P, T>>(
        private val type: CustomTemplateType,
        private val allowHierarchicalNames: Boolean,
        private val isVisual: Boolean
    ) {

        protected abstract val thisRef: T

        private var templateName: String? = null
        private val argsNames = mutableSetOf<String>()
        private val parentArgsNames = mutableSetOf<String>()
        private val args = mutableListOf<TemplateArgument>()
        private var presenter: P? = null

        /**
         * The name for the template. It should be provided exactly once. It must be unique across template definitions.
         * Must be non-blank.
         */
        fun name(name: String): T {
            if (templateName != null) {
                throw CustomTemplateException("CustomTemplate name is already set as \"$templateName\"")
            }

            if (name.isBlank()) {
                throw CustomTemplateException("CustomTemplate must have a non-blank name")
            }

            templateName = name
            return thisRef
        }

        fun stringArgument(name: String, defaultValue: String): T {
            addArgument(name, STRING, defaultValue)
            return thisRef
        }

        fun booleanArgument(name: String, defaultValue: Boolean): T {
            addArgument(name, BOOLEAN, defaultValue)
            return thisRef
        }

        fun byteArgument(name: String, defaultValue: Byte): T {
            addArgument(name, NUMBER, defaultValue)
            return thisRef
        }

        fun shortArgument(name: String, defaultValue: Short): T {
            addArgument(name, NUMBER, defaultValue)
            return thisRef
        }

        fun intArgument(name: String, defaultValue: Int): T {
            addArgument(name, NUMBER, defaultValue)
            return thisRef
        }

        fun longArgument(name: String, defaultValue: Long): T {
            addArgument(name, NUMBER, defaultValue)
            return thisRef
        }

        fun floatArgument(name: String, defaultValue: Float): T {
            addArgument(name, NUMBER, defaultValue)
            return thisRef
        }

        fun doubleArgument(name: String, defaultValue: Double): T {
            addArgument(name, NUMBER, defaultValue)
            return thisRef
        }

        fun fileArgument(name: String): T {
            addArgument(name, FILE, null)
            return thisRef
        }

        /**
         * The presenter for this template.
         */
        fun presenter(presenter: P): T {
            this.presenter = presenter
            return thisRef
        }

        fun build(): CustomTemplate {
            val presenter = this.presenter ?: throw CustomTemplateException("CustomTemplate must have a presenter")
            val name = templateName ?: throw CustomTemplateException("CustomTemplate must have a name")

            return CustomTemplate(name, presenter, isVisual, getOrderedArgs(), type)
        }

        internal fun addArgument(name: String, type: TemplateArgumentType, defaultValue: Any?) {
            if (name.isBlank()) {
                throw CustomTemplateException("Argument name must not be blank")
            }

            if (!allowHierarchicalNames && name.contains(".")) {
                throw CustomTemplateException("Argument name must not contain \".\"")
            }

            if (name.startsWith(".") || name.endsWith(".") || name.contains("..")) {
                throw CustomTemplateException("Argument name must not begin or end with a \".\" nor have consecutive \".\"")
            }

            if (argsNames.contains(name)) {
                throw CustomTemplateException("Argument with name \"$name\" is already defined")
            }

            if (allowHierarchicalNames) {
                trackParentNames(name)
            }

            args.add(TemplateArgument(name, type, defaultValue))
            argsNames.add(name)
        }

        private fun trackParentNames(name: String) {
            //add parent args names and check if they are not already defined
            var currentStartIndex = 0
            var currentIndex = name.indexOf('.', currentStartIndex)
            while (currentIndex != -1) {
                val parentName = name.substring(0, currentIndex)

                if (argsNames.contains(parentName)) {
                    throw CustomTemplateException("Argument with name \"$name\" is already defined")
                }
                parentArgsNames.add(parentName)
                currentStartIndex = currentIndex + 1
                currentIndex = name.indexOf('.', currentStartIndex)
            }

            if (parentArgsNames.contains(name)) {
                throw CustomTemplateException("Argument with name \"$name\" is already defined")
            }
        }

        /**
         * Arguments are ordered by the way they are added to the list. Arguments with common hierarchical path are
         * ordered together after the first occurrence and then sorted alphabetically.
         */
        private fun getOrderedArgs(): List<TemplateArgument> {
            val orderedArgs = linkedMapOf<String, MutableList<TemplateArgument>>()

            for (arg in args) {
                val argName: String = arg.name.split(".", limit = 2).first()
                if (orderedArgs.contains(argName)) {
                    orderedArgs[argName]?.add(arg)
                } else {
                    orderedArgs[argName] = mutableListOf(arg)
                }
            }

            return orderedArgs.flatMap { it.value.toList().sortedBy { arg -> arg.name } }
        }
    }
}

internal enum class CustomTemplateType(val stringName: String) {
    TEMPLATE("template"),
    FUNCTION("function")
}
