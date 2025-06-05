package com.clevertap.android.pushtemplates

enum class PTScaleType {
    FIT_CENTER,
    CENTER_CROP;

    companion object {
        fun fromString(value: String?): PTScaleType {
            return values().firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: CENTER_CROP
        }
    }
}