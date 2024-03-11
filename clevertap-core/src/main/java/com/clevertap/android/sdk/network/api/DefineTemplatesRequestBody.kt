package com.clevertap.android.sdk.network.api

import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.utils.putObject
import org.json.JSONArray
import org.json.JSONObject

internal class DefineTemplatesRequestBody(header: JSONObject, templates: Collection<CustomTemplate>) {

    val jsonArray = JSONArray().apply {
        put(header)
        put(templates.toJSON())
    }

    override fun toString(): String = jsonArray.toString()
}

private fun Collection<CustomTemplate>.toJSON(): JSONObject {
    val templates = this

    return JSONObject().apply {
        put("type", "templatePayload")
        putObject("definitions") {
            for (template in templates) {
                putObject(template.name) {
                    put("type", template.type.stringName)
                    if (template.args.isNotEmpty()) {
                        putObject("vars") {
                            template.getOrderedArgs().forEachIndexed { index, arg ->
                                putObject(arg.name) {
                                    arg.defaultValue?.let {
                                        put("defaultValue", it)
                                    }
                                    put("type", arg.type.stringName)
                                    put("order", index)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
