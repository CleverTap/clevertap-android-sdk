package com.clevertap.android.sdk.variables

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Public + RestrictTo(LIBRARY_GROUP_PREFIX) (bucket E): used by the react-native wrapper.
 * Static JVM shape is unchanged (companion + @JvmStatic / const val).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class CTVariableUtils {

    companion object {

        private const val TAG = "CTVariableUtils"
        const val VARS = "vars"
        const val STRING = "string"
        const val BOOLEAN = "boolean"
        const val DICTIONARY = "group"
        const val NUMBER = "number"
        const val FILE = "file"

        private fun log(msg: String) {
            Logger.d(TAG, msg)
        }

        @JvmStatic
        fun updateValuesAndKinds(
            name: String?,
            nameComponents: Array<String>?,
            value: Any?,
            kind: String?,
            values: MutableMap<String, *>?,
            kinds: MutableMap<String, *>?
        ) {
            var value = value
            var valuesPtr: Any? = values
            if (nameComponents != null && nameComponents.isNotEmpty()) {
                for (i in 0 until nameComponents.size - 1) {
                    valuesPtr = traverse(valuesPtr, nameComponents[i], true) // can be either the actual value, or empty hashmap
                }
                if (valuesPtr is Map<*, *>) {
                    val map: MutableMap<String, Any?> = JsonUtil.uncheckedCast(valuesPtr)
                    val currentValue = map[nameComponents[nameComponents.size - 1]]

                    if (currentValue is Map<*, *> && value is Map<*, *>) {
                        value = mergeHelper(value, currentValue)
                    } else if (currentValue != null && currentValue == value) {
                        log(
                            String.format(
                                "Variable with name %s will override value: %s, with new value: %s.",
                                name,
                                currentValue,
                                value
                            )
                        )
                    }

                    map[nameComponents[nameComponents.size - 1]] = value
                }
            }
            if (kinds != null) {
                val kindsMap: MutableMap<String?, Any?> = JsonUtil.uncheckedCast(kinds)
                kindsMap[name] = kind
            }
        }

        /**
         * Converts a map like following:
         * ```
         * [
         *   "android.samsung.s22" : 54000,
         *   "android.samsung.s23" : "unreleased",
         *   "welcomeMsg": "hello"
         * ]
         * ```
         * to:
         * ```
         * [
         *   "android": {
         *     "samsung: {"s22": 54000, "s23" : "unreleased"}
         *   },
         *   "welcomeMsg" : "hello"
         * ]
         * ```
         * to support legacy parsing implementation
         */
        @JvmStatic
        fun convertFlatMapToNestedMaps(map: Map<String, *>): MutableMap<String, Any?> {
            val result: MutableMap<String, Any?> = HashMap()

            for ((key, entryValue) in map) {
                if (key.contains(".")) {
                    val components = getNameComponents(key)
                    val namePosition = components.size - 1
                    var currentMap = result
                    for (i in components.indices) {
                        val component = components[i]
                        if (i == namePosition) {
                            currentMap[component] = entryValue
                        } else {
                            val currentValue = currentMap[component]
                            if (currentValue !is Map<*, *>) { // null or not a map
                                // If it is not a map it will fix the invalid data.
                                val nestedMap: MutableMap<String, Any?> = HashMap()
                                currentMap[component] = nestedMap
                                currentMap = nestedMap
                            } else {
                                currentMap = JsonUtil.uncheckedCast(currentMap[component])
                            }
                        }
                    }
                } else {
                    result[key] = entryValue
                }
            }

            return result
        }

        /**
         * Returns the merge of vars and diff, where values in vars are replaced from values
         * in diff, i.e. calling mergeHelper with vars=[a:10, b:20] and diff=[a:15, c:25] produces a
         * merge=[a:15, b:20, c:25]. The methods works recursively when nested maps are presented.
         *
         * @param vars - Variable values as defined by user.
         * @param diff - Variable values from server.
         *
         * @return Merge of both parameters with priority of diff over vars when overriding values.
         */
        @JvmStatic
        fun mergeHelper(vars: Any?, diff: Any?): Any? {
            if (diff == null) {
                return vars
            }
            if (diff is Number ||
                diff is Boolean ||
                diff is String ||
                diff is Char ||
                vars is Number ||
                vars is Boolean ||
                vars is String ||
                vars is Char
            ) {
                return diff
            }

            val diffKeys: Iterable<*> = if (diff is Map<*, *>) diff.keys else diff as Iterable<*>
            val varsKeys: Iterable<*>? = if (vars is Map<*, *>) vars.keys else vars as Iterable<*>?
            val diffMap: Map<*, *>? = if (diff is Map<*, *>) diff else null
            val varsMap: Map<*, *>? = if (vars is Map<*, *>) vars else null

            // Merge dictionaries.
            if (vars is Map<*, *> || diff is Map<*, *>) {
                val merged = HashMap<Any?, Any?>()
                if (varsKeys != null) {
                    for (`var` in varsKeys) {
                        if (diffMap != null && varsMap != null) {
                            val diffVar = diffMap[`var`]
                            val value = varsMap[`var`]
                            if (diffVar == null && value != null) {
                                merged[`var`] = value
                            }
                        }
                    }
                }
                for (`var` in diffKeys) {
                    val diffsValue = diffMap?.get(`var`)
                    val varsValue = varsMap?.get(`var`)
                    val mergedValues = mergeHelper(varsValue, diffsValue)
                    merged[`var`] = mergedValues
                }
                return merged
            }
            return null
        }

        // check test for more info
        @JvmStatic
        fun traverse(collection: Any?, key: Any?, autoInsert: Boolean): Any? {
            if (collection == null) {
                return null
            }
            if (collection is Map<*, *>) {
                val castedCollection: MutableMap<Any?, Any?> = JsonUtil.uncheckedCast(collection)
                var result = castedCollection[key]
                if (autoInsert && result == null && key is String) {
                    result = HashMap<String, Any?>()
                    castedCollection[key] = result
                }
                return result
            }

            return null
        }

        /**
         * Resolves the server type of variable by checking the generic type T of the default value.
         */
        @JvmStatic
        fun <T> kindFromValue(defaultValue: T): String? {
            var kind: String? = null
            if (defaultValue is Int ||
                defaultValue is Long ||
                defaultValue is Short ||
                defaultValue is Char ||
                defaultValue is Byte ||
                defaultValue is BigInteger
            ) {
                kind = NUMBER
            } else if (defaultValue is Float ||
                defaultValue is Double ||
                defaultValue is BigDecimal
            ) {
                kind = NUMBER
            } else if (defaultValue is String) {
                kind = STRING
            } else if (defaultValue is Map<*, *>) {
                kind = DICTIONARY
            } else if (defaultValue is Boolean) {
                kind = BOOLEAN
            }
            return kind
        }

        /**
         * converts a string of this format : "a.b.c" to  an array of strings, i.e ["a","b","c"]
         */
        @JvmStatic
        fun getNameComponents(name: String?): Array<String> {
            return try {
                // java.lang.String#split(regex): drops trailing empty strings, unlike Kotlin's split.
                (name as java.lang.String).split("\\.")
            } catch (t: Throwable) {
                Logger.d(TAG, "Error splitting variable name: $name", t)
                arrayOf()
            }
        }

        /**
         * Converts a map like following:
         * ```
         * [
         *   "android": [
         *     "samsung: [
         *       "s22": 54000,
         *       "s23" : "unreleased"
         *     ]
         *   ],
         *   "welcomeMsg" : "hello"
         * ]
         * ```
         * to:
         * ```
         * [
         *   "android.samsung.s22" : 54000,
         *   "android.samsung.s23" : "unreleased",
         *   "welcomeMsg": "hello"
         * ]
         * ```
         * @author Ansh Sachdeva
         */
        @VisibleForTesting
        @JvmStatic
        internal fun convertNestedMapsToFlatMap(prefix: String, map: Map<String, *>, result: MutableMap<String, *>) {
            val resultMap: MutableMap<String, Any?> = JsonUtil.uncheckedCast(result)
            for ((key, value) in map) {
                if (value is Map<*, *>) {
                    convertNestedMapsToFlatMap("$prefix$key.", JsonUtil.uncheckedCast(value), resultMap)
                } else {
                    resultMap[prefix + key] = value
                }
            }
        }

        @JvmStatic
        fun getFlatVarsJson(
            values: Map<String, *>,
            kinds: Map<String, *>
        ): JSONObject {
            try {
                val resultJson = JSONObject()
                resultJson.put("type", Constants.variablePayloadType)

                val vars = JSONObject()
                for (valueKey in values.keys) {
                    val kind = kinds[valueKey] as String?
                    val value = values[valueKey]
                    if (value is Map<*, *>) {
                        val valueMap: MutableMap<String, Any?> = HashMap()
                        valueMap[valueKey] = value
                        val flattenedValueMap: MutableMap<String, Any?> = HashMap()
                        convertNestedMapsToFlatMap("", valueMap, flattenedValueMap)
                        for ((flattenedKey, flattenedValue) in flattenedValueMap) {
                            // check if it is file type, in this case we cannot deduce the type from value
                            val flattenedValueKind: String? = if (FILE == kinds[flattenedKey]) {
                                FILE
                            } else {
                                kindFromValue(flattenedValue)
                            }

                            val varData = JSONObject()
                            varData.put("type", flattenedValueKind)
                            varData.put("defaultValue", flattenedValue)
                            vars.put(flattenedKey, varData)
                        }
                    } else {
                        val varData = JSONObject()
                        varData.put("type", kind)
                        varData.put("defaultValue", value)
                        vars.put(valueKey, varData)
                    }
                }
                resultJson.put("vars", vars)
                return resultJson
            } catch (t: Throwable) {
                Logger.d(TAG, "Error creating flat vars JSON", t)
                return JSONObject()
            }
        }

        @JvmStatic
        fun deepCopyMap(originalMap: Map<*, *>): MutableMap<Any?, Any?> {
            val copiedMap: MutableMap<Any?, Any?> = HashMap()

            for ((key, value) in originalMap) {
                if (value is Map<*, *>) {
                    copiedMap[key] = deepCopyMap(JsonUtil.uncheckedCast(value))
                } else {
                    copiedMap[key] = value
                }
            }

            return copiedMap
        }
    }
}
