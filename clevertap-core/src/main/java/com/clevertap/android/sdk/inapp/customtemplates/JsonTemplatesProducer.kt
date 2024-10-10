package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.ACTION
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.BOOLEAN
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.FILE
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.NUMBER
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.STRING
import org.json.JSONException
import org.json.JSONObject

/**
 * A [TemplateProducer] that creates templates based on a json definition. Invalid definitions will
 * throw [CustomTemplateException] when [defineTemplates] is called.
 *
 * @param jsonTemplatesDefinition A string with a json definition of templates in the following format:
 *```
 *{
 *  "TemplateName": {
 *    "type": "template",
 *    "arguments": {
 *      "Argument1": {
 *        "type": "string|number|boolean|file|action|object",
 *        "value": "val" // different type depending on "type", e.g 12.5, true, "str" or {}
 *        },
 *      "Argument2": {
 *        "type": "object",
 *        "value": {
 *          "Nested1": {
 *            "type": "string|number|boolean|object", // file and action cannot be nested
 *            "value": {}
 *          },
 *          "Nested2": {
 *            "type": "string|number|boolean|object",
 *            "value": "val"
 *          }
 *        }
 *      }
 *    }
 *  },
 *  "functionName": {
 *    "type": "function",
 *    "isVisual": true|false,
 *    "arguments": {
 *      "a": {
 *      "type": "string|number|boolean|file|object", // action arguments are not supported for functions
 *      "value": "val"
 *      }
 *    }
 *  }
 *}
 *```
 *
 * @param templatesPresenter A presenter for all templates in the json definitions. Required if there
 * is at least one template with type "template".
 * @param functionsPresenter A presenter for all functions in the json definitions. Required if there
 * is at least one template with type "function".
 */
open class JsonTemplatesProducer(
    private val jsonTemplatesDefinition: String,
    private val templatesPresenter: TemplatePresenter?,
    private val functionsPresenter: FunctionPresenter?
) : TemplateProducer {

    /**
     * Creates [CustomTemplate]s based on the [jsonTemplatesDefinition] of this [JsonTemplatesProducer]
     *
     * @throws CustomTemplateException When an invalid JSON format or values occur while parsing
     * [jsonTemplatesDefinition]. See "Caused by" in the stacktrace for details why the exception
     * happened.
     */
    override fun defineTemplates(ctConfig: CleverTapInstanceConfig): Set<CustomTemplate> {

        return try {
            val jsonDefinitions = JSONObject(jsonTemplatesDefinition)
            val templatesSet = mutableSetOf<CustomTemplate>()
            for (templateName in jsonDefinitions.keys()) {
                templatesSet.add(
                    createTemplateFromJson(
                        templateName,
                        jsonDefinitions.getJSONObject(templateName)
                    )
                )
            }
            templatesSet
        } catch (je: JSONException) {
            throw CustomTemplateException("Invalid JSON format for templates' definitions", je)
        }
    }

    /**
     * Creates a [CustomTemplate] based on the provided [JSONObject]
     *
     * @throws JSONException If [json] does not have a required field or a field is of incorrect type
     * @throws CustomTemplateException if [json] contains fields with invalid values
     */
    private fun createTemplateFromJson(templateName: String, json: JSONObject): CustomTemplate {
        val stringType = json.getString("type")
        val type = CustomTemplateType.fromString(stringType)
            ?: throw CustomTemplateException("Invalid template type: \"$stringType\"")

        return when (type) {
            CustomTemplateType.TEMPLATE -> {
                if (templatesPresenter == null) {
                    throw CustomTemplateException("JSON definition contains a template definition and a templates presenter is required")
                }

                val builder = CustomTemplate.TemplateBuilder()
                builder.name(templateName)
                builder.presenter(templatesPresenter)
                addJsonArgumentsToBuilder(builder, json.getJSONObject("arguments"))
                builder.build()
            }

            CustomTemplateType.FUNCTION -> {
                if (functionsPresenter == null) {
                    throw CustomTemplateException("JSON definition contains a function definition and a function presenter is required")
                }

                val isVisual = json.getBoolean("isVisual")

                val builder = CustomTemplate.FunctionBuilder(isVisual)
                builder.name(templateName)
                builder.presenter(functionsPresenter)
                addJsonArgumentsToBuilder(builder, json.getJSONObject("arguments"))
                builder.build()
            }
        }
    }

    /**
     * Add all arguments defined in [json] as template arguments in [builder]
     *
     * @throws JSONException If [json] does not have a required field or a field is of incorrect type
     * @throws CustomTemplateException if [json] contains fields with invalid values
     */
    private fun addJsonArgumentsToBuilder(
        builder: CustomTemplate.TemplateBuilder,
        json: JSONObject
    ) {
        for (key in json.keys()) {
            val argumentJson = json.getJSONObject(key)
            val typeString = argumentJson.getString("type")
            if (typeString == "object") {
                builder.mapArgument(key, jsonArgToMap(argumentJson.getJSONObject("value")))
                continue
            }

            val argumentType = TemplateArgumentType.fromString(typeString)
                ?: throw CustomTemplateException("Unsupported argument type: \"$typeString\"")

            when (argumentType) {
                BOOLEAN -> {
                    val value = argumentJson.getBoolean("value")
                    builder.booleanArgument(key, value)
                }

                NUMBER -> {
                    val value = argumentJson.getDouble("value")
                    builder.doubleArgument(key, value)
                }

                STRING -> {
                    val value = argumentJson.getString("value")
                    builder.stringArgument(key, value)
                }

                FILE -> {
                    if (argumentJson.has("value")) {
                        throw CustomTemplateException("File arguments should not specify a value. Remove value from argument: \"$key\"")
                    }

                    builder.fileArgument(key)
                }

                ACTION -> {
                    if (argumentJson.has("value")) {
                        throw CustomTemplateException("Action arguments should not specify a value. Remove value from argument: \"$key\"")
                    }

                    builder.actionArgument(key)
                }
            }
        }
    }

    /**
     * Add all arguments defined in [json] as function arguments in [builder]
     *
     * @throws JSONException If [json] does not have a required field or a field is of incorrect type
     * @throws CustomTemplateException if [json] contains fields with invalid values
     */
    private fun addJsonArgumentsToBuilder(
        builder: CustomTemplate.FunctionBuilder,
        json: JSONObject
    ) {
        for (key in json.keys()) {
            val argumentJson = json.getJSONObject(key)
            val typeString = argumentJson.getString("type")
            if (typeString == "object") {
                builder.mapArgument(key, jsonArgToMap(argumentJson.getJSONObject("value")))
                continue
            }

            val type = argumentTypeFromStringOrThrow(typeString)
            when (type) {
                BOOLEAN -> {
                    val value = argumentJson.getBoolean("value")
                    builder.booleanArgument(key, value)
                }

                NUMBER -> {
                    val value = argumentJson.getDouble("value")
                    builder.doubleArgument(key, value)
                }

                STRING -> {
                    val value = argumentJson.getString("value")
                    builder.stringArgument(key, value)
                }

                FILE -> {
                    if (argumentJson.has("value")) {
                        throw CustomTemplateException("File arguments should not specify a value. Remove value from argument: \"$key\"")
                    }

                    builder.fileArgument(key)
                }

                ACTION -> {
                    throw CustomTemplateException("Function templates cannot have action arguments. Remove argument: \"$key\"")
                }
            }
        }
    }

    /**
     * Create a map of all arguments defined in [json]
     *
     * @throws JSONException If [json] does not have a required field or a field is of incorrect type
     * @throws CustomTemplateException if [json] contains fields with invalid values
     */
    private fun jsonArgToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in json.keys()) {
            val argumentJson = json.getJSONObject(key)
            val typeString = argumentJson.getString("type")
            if (typeString == "object") {
                map[key] = jsonArgToMap(argumentJson.getJSONObject("value"))
                continue
            }

            val type = argumentTypeFromStringOrThrow(typeString)
            when (type) {
                BOOLEAN -> {
                    map[key] = argumentJson.getBoolean("value")
                }

                NUMBER -> {
                    map[key] = argumentJson.getDouble("value")
                }

                STRING -> {
                    map[key] = argumentJson.getString("value")
                }

                FILE, ACTION -> {
                    throw CustomTemplateException(
                        "Nesting of file and action arguments within objects is not supported." +
                                " To define nested file and actions use '.' notation in the argument name."
                    )
                }
            }
        }

        return map
    }

    private fun argumentTypeFromStringOrThrow(argumentTypeString: String): TemplateArgumentType {
        return TemplateArgumentType.fromString(argumentTypeString)
            ?: throw CustomTemplateException("Unsupported argument type: \"$argumentTypeString\"")
    }
}
