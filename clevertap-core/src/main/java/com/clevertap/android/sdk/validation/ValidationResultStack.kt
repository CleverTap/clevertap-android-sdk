package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class ValidationResultStack {

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
                if (pending.size > MAX_PENDING_RESULTS) {
                    // Trim down the list to 40, so that this trim needn't run for the next 10 events
                    // Hence, skip the first 10 elements
                    pendingValidationResults = ArrayList(pending.subList(TRIM_COUNT, pending.size))
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

                if (newSize <= MAX_PENDING_RESULTS) {
                    pending.addAll(vrList)
                    return
                }
                // Keep the newest 40 results: skip the oldest ones from the existing list first
                val skipCount = newSize - RESULTS_KEPT_AFTER_TRIM
                pendingValidationResults = if (skipCount >= currentSize) {
                    // Everything existing is dropped; take only the last 40 items from the new list
                    ArrayList(vrList.takeLast(RESULTS_KEPT_AFTER_TRIM))
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
        private const val MAX_PENDING_RESULTS = 50
        private const val RESULTS_KEPT_AFTER_TRIM = 40
        private const val TRIM_COUNT = MAX_PENDING_RESULTS - RESULTS_KEPT_AFTER_TRIM

        private val pendingValidationResultsLock = Any()
    }
}
