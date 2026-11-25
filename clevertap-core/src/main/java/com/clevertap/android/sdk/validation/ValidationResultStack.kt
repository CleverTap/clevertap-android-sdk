package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo
import kotlin.math.max

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class ValidationResultStack {

    var pendingValidationResults = ArrayList<ValidationResult>()
        private set

    //Validation
    open fun pushValidationResult(vr: ValidationResult?) {
        synchronized(pendingValidationResultsLock) {
            try {
                val len = pendingValidationResults.size
                if (len > 50) {
                    val trimmed = ArrayList<ValidationResult>()
                    // Trim down the list to 40, so that this loop needn't run for the next 10 events
                    // Hence, skip the first 10 elements
                    for (i in 10 until len) {
                        trimmed.add(pendingValidationResults[i])
                    }
                    @Suppress("UNCHECKED_CAST")
                    (trimmed as ArrayList<ValidationResult?>).add(vr)
                    pendingValidationResults = trimmed
                } else {
                    @Suppress("UNCHECKED_CAST")
                    (pendingValidationResults as ArrayList<ValidationResult?>).add(vr)
                }
            } catch (e: Exception) {
                // no-op
            }
        }
    }

    open fun pushValidationResult(vrList: List<ValidationResult>?) {
        if (vrList == null || vrList.isEmpty()) {
            return
        }

        synchronized(pendingValidationResultsLock) {
            try {
                val currentSize = pendingValidationResults.size
                val newSize = currentSize + vrList.size

                if (newSize > 50) {
                    val trimmed = ArrayList<ValidationResult>()

                    // Calculate how many items to skip from the existing list
                    val skipCount = newSize - 40

                    // If we need to skip more than what we have, just use the new list
                    if (skipCount >= currentSize) {
                        // Take only the last 40 items from the new list
                        val startIndex = vrList.size - 40
                        for (i in max(0, startIndex) until vrList.size) {
                            trimmed.add(vrList[i])
                        }
                    } else {
                        // Keep some from existing, add all new ones
                        for (i in skipCount until currentSize) {
                            trimmed.add(pendingValidationResults[i])
                        }
                        trimmed.addAll(vrList)
                    }

                    pendingValidationResults = trimmed
                } else {
                    pendingValidationResults.addAll(vrList)
                }
            } catch (e: Exception) {
                // no-op
            }
        }
    }

    open fun popValidationResult(): ValidationResult? {
        // really a shift
        var vr: ValidationResult? = null

        synchronized(pendingValidationResultsLock) {
            try {
                if (pendingValidationResults.isNotEmpty()) {
                    vr = pendingValidationResults.removeAt(0)
                }
            } catch (e: Exception) {
                // no-op
            }
        }
        return vr
    }

    private companion object {
        private val pendingValidationResultsLock = Any()
    }
}
