package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class ValidationResultStack {

    /**
     * The pending results, oldest first. Returned as the live list outside the lock (as before).
     */
    var pendingValidationResults = ArrayList<ValidationResult>()
        private set

    //Validation
    open fun pushValidationResult(vr: ValidationResult) {
        synchronized(pendingValidationResultsLock) {
            try {
                val pending = pendingValidationResults
                if (pending.size > 50) {
                    // Trim down the list to 40, so that this trim needn't run for the next 10 events
                    // Hence, skip the first 10 elements
                    pendingValidationResults = ArrayList(pending.subList(10, pending.size))
                }
                pendingValidationResults.add(vr)
            } catch (e: Exception) {
                // no-op
            }
        }
    }

    open fun pushValidationResult(vrList: List<ValidationResult>?) {
        if (vrList.isNullOrEmpty()) {
            return
        }

        synchronized(pendingValidationResultsLock) {
            try {
                val pending = pendingValidationResults
                val currentSize = pending.size
                val newSize = currentSize + vrList.size

                if (newSize <= 50) {
                    pending.addAll(vrList)
                    return
                }
                // Keep the newest 40 results: skip the oldest ones from the existing list first
                val skipCount = newSize - 40
                pendingValidationResults = if (skipCount >= currentSize) {
                    // Everything existing is dropped; take only the last 40 items from the new list
                    ArrayList(vrList.takeLast(40))
                } else {
                    // Keep some from existing, add all new ones
                    ArrayList(pending.subList(skipCount, currentSize)).apply { addAll(vrList) }
                }
            } catch (e: Exception) {
                // no-op
            }
        }
    }

    // really a shift
    open fun popValidationResult(): ValidationResult? = synchronized(pendingValidationResultsLock) {
        try {
            pendingValidationResults.removeFirstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        private val pendingValidationResultsLock = Any()
    }
}
