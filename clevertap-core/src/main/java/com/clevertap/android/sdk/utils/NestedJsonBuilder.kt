package com.clevertap.android.sdk.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class NestedJsonBuilder {

    companion object {
        private val ARRAY_INDEX_PATTERN = Regex("""\[(\d+)]""")
    }

    /**
     * Builds a JSONObject from a dot notation path and value.
     *
     * Examples:
     * - "name" -> {"name": value}
     * - "user.age" -> {"user": {"age": value}}
     * - "items[0]" -> {"items": [value]}
     * - "users[0].name" -> {"users": [{"name": value}]}
     * - "profile.scores[2]" -> {"profile": {"scores": [null, null, value]}}
     * - "matrix[0][1]" -> {"matrix": [[null, value]]}
     * - "cube[1][2][3]" -> {"cube": [null, [null, null, [null, null, null, value]]]}
     *
     * @param path dot notation path (e.g., "user.profile.age" or "items[0].name" or "matrix[0][1]")
     * @param value value to set at the path
     * @return JSONObject with the nested structure
     */
    @Throws(JSONException::class)
    fun buildFromPath(path: String, value: Any?): JSONObject {
        val result = JSONObject()
        setValue(result, path, value)
        return result
    }

    /** Sets a value in a JSONObject using dot notation path */
    @Throws(JSONException::class)
    private fun setValue(root: JSONObject, path: String, value: Any?) {
        val segments = parsePath(path)
        setValueRecursive(root, segments, 0, value)
    }

    /**
     * Parses a dot notation path into segments.
     * Handles array indices like "items[0]" -> PathSegment("items", 0)
     * Handles consecutive indices like "matrix[0][1]" -> PathSegment("matrix", 0), PathSegment("", 1)
     */
    private fun parsePath(path: String): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()
        val parts = path.split('.')

        for (part in parts) {
            // Extract base key and all array indices
            val remaining = part
            var baseKey: String?

            // Find all array indices in this part
            val indices = mutableListOf<Int>()
            val matches = ARRAY_INDEX_PATTERN.findAll(remaining)
            
            for (match in matches) {
                val indexStr = match.groupValues[1]
                val index = indexStr.toInt()
                indices.add(index)
            }
            
            // Extract the base key (part before first [)
            if (indices.isNotEmpty()) {
                val firstBracket = remaining.indexOf('[')
                baseKey = remaining.substring(0, firstBracket)
                
                // Add base key with first index
                segments.add(PathSegment(baseKey, indices[0]))
                
                // Add remaining indices as empty-key segments
                for (i in 1 until indices.size) {
                    segments.add(PathSegment("", indices[i]))
                }
            } else {
                // No array indices, just a regular key
                segments.add(PathSegment(part, null))
            }
        }

        return segments
    }

    /** Recursively sets the value following the path segments */
    @Throws(JSONException::class)
    private fun setValueRecursive(
        current: Any,
        segments: List<PathSegment>,
        index: Int,
        value: Any?
    ) {
        if (index >= segments.size) return

        val segment = segments[index]
        val isLastSegment = index == segments.size - 1

        when (current) {
            is JSONObject -> {
                if (segment.arrayIndex != null) {
                    // This segment represents an array
                    val array = current.optJSONArray(segment.key) ?: JSONArray()
                    current.put(segment.key, array)
                    
                    // Ensure array has enough space for the index we're accessing
                    ensureArraySize(array, segment.arrayIndex + 1)

                    if (isLastSegment) {
                        // Set the value at the array index
                        array.put(segment.arrayIndex, convertValue(value))
                    } else {
                        // Continue navigation into the array element
                        val nextSegment = segments[index + 1]

                        // Check if next segment is a consecutive array index (empty key)
                        // vs a new object property that happens to have an array
                        if (nextSegment.arrayIndex != null && nextSegment.key.isEmpty()) {
                            // Next is also an array index (matrix[0][1] case)
                            var nested = array.optJSONArray(segment.arrayIndex)
                            if (nested == null) {
                                nested = JSONArray()
                                array.put(segment.arrayIndex, nested)
                            }
                            setValueRecursive(nested, segments, index + 1, value)
                        } else {
                            // Next is an object (includes cases like users[0].addresses[1])
                            var nested = array.optJSONObject(segment.arrayIndex)
                            if (nested == null) {
                                nested = JSONObject()
                                array.put(segment.arrayIndex, nested)
                            }
                            setValueRecursive(nested, segments, index + 1, value)
                        }
                    }
                } else {
                    // This segment represents an object key
                    if (isLastSegment) {
                        current.put(segment.key, convertValue(value))
                    } else {
                        val nextSegment = segments[index + 1]

                        if (nextSegment.arrayIndex != null) {
                            // Next segment has array index, so we need to create an object
                            // that will contain the array
                            var nested = current.optJSONObject(segment.key)
                            if (nested == null) {
                                nested = JSONObject()
                                current.put(segment.key, nested)
                            }
                            setValueRecursive(nested, segments, index + 1, value)
                        } else {
                            // Next segment is an object
                            var nested = current.optJSONObject(segment.key)
                            if (nested == null) {
                                nested = JSONObject()
                                current.put(segment.key, nested)
                            }
                            setValueRecursive(nested, segments, index + 1, value)
                        }
                    }
                }
            }

            is JSONArray -> {
                // When navigating through array, we must have an index
                if (segment.arrayIndex == null) {
                    throw JSONException("Array requires index notation, got key: ${segment.key}")
                }
                
                ensureArraySize(current, segment.arrayIndex + 1)

                if (isLastSegment) {
                    current.put(segment.arrayIndex, convertValue(value))
                } else {
                    val nextSegment = segments[index + 1]

                    // Check if next segment is a consecutive array index (empty key)
                    if (nextSegment.arrayIndex != null && nextSegment.key.isEmpty()) {
                        // Next is consecutive array index (matrix[0][1] case)
                        var nested = current.optJSONArray(segment.arrayIndex)
                        if (nested == null) {
                            nested = JSONArray()
                            current.put(segment.arrayIndex, nested)
                        }
                        setValueRecursive(nested, segments, index + 1, value)
                    } else {
                        // Next is an object (includes cases like users[0].addresses[1])
                        var nested = current.optJSONObject(segment.arrayIndex)
                        if (nested == null) {
                            nested = JSONObject()
                            current.put(segment.arrayIndex, nested)
                        }
                        setValueRecursive(nested, segments, index + 1, value)
                    }
                }
            }
        }
    }

    /** Ensures a JSONArray has at least the specified size by filling with nulls */
    private fun ensureArraySize(array: JSONArray, size: Int) {
        while (array.length() < size) {
            array.put(JSONObject.NULL)
        }
    }

    /** Converts Kotlin/Java values to JSON-compatible values */
    private fun convertValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is JSONObject, is JSONArray -> value
            is Map<*, *> -> {
                val json = JSONObject()
                value.forEach { (k, v) -> json.put(k.toString(), convertValue(v)) }
                json
            }

            is List<*> -> {
                val json = JSONArray()
                value.forEach { json.put(convertValue(it)) }
                json
            }

            else -> value
        }
    }

    /** Represents a segment in a dot notation path */
    private data class PathSegment(
        val key: String,
        val arrayIndex: Int?
    )
}
