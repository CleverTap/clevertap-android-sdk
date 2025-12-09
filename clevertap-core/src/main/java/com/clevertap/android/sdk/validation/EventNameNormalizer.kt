package com.clevertap.android.sdk.validation

/**
 * Normalizes event names according to ValidationConfig.
 * Reports what was found (including violations) THEN cleans the name.
 */
class EventNameNormalizer(private val config: ValidationConfig) {
    
    /**
     * Normalizes an event name and returns the cleaned name with metrics.
     */
    fun normalize(eventName: String?): EventNameNormalizationResult {
        // Check for null event name
        if (eventName == null) {
            return EventNameNormalizationResult(
                originalName = null,
                cleanedName = null,
                metrics = EventNameMetrics(
                    originalLength = 0,
                    cleanedLength = 0,
                    maxLength = config.maxEventNameLength,
                    modifications = emptyList()
                )
            )
        }
        
        val original = eventName
        var cleaned = eventName.trim()
        val modifications = mutableListOf<EventNameModificationReason>()
        
        // Remove disallowed characters if configured
        config.eventNameCharsNotAllowed?.let { notAllowed ->
            val filtered = cleaned.filterNot { char -> char in notAllowed }

            if (filtered != cleaned) {
                cleaned = filtered
                modifications.add(EventNameModificationReason.INVALID_CHARACTERS_REMOVED)
            }
        }
        
        // Truncate if exceeds max length (if configured)
        config.maxEventNameLength?.let { maxLength ->
            if (cleaned.length > maxLength) {
                modifications.add(EventNameModificationReason.TRUNCATED_TO_MAX_LENGTH)
                cleaned = cleaned.substring(0, maxLength)
            }
        }
        
        val result = cleaned.trim()
        
        return EventNameNormalizationResult(
            originalName = original,
            cleanedName = result,
            metrics = EventNameMetrics(
                originalLength = original.length,
                cleanedLength = result.length,
                maxLength = config.maxEventNameLength,
                modifications = modifications
            )
        )
    }
}

/**
 * Result of event name normalization containing the cleaned name and metrics.
 */
data class EventNameNormalizationResult(
    val originalName: String?,
    val cleanedName: String?,
    val metrics: EventNameMetrics
)

/**
 * Metrics about event name normalization.
 */
data class EventNameMetrics(
    val originalLength: Int,
    val cleanedLength: Int,
    val maxLength: Int?,
    val modifications: List<EventNameModificationReason>
)

/**
 * Reasons why an event name was modified.
 */
enum class EventNameModificationReason {
    INVALID_CHARACTERS_REMOVED,
    TRUNCATED_TO_MAX_LENGTH
}
