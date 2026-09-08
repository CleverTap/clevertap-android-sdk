package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.ProgressTemplateData
import com.clevertap.android.pushtemplates.checkers.Checker

/**
 * Validator for the progress-centric (`pt_progress`) template. Both tiers (native
 * `NotificationCompat.ProgressStyle` on 16+, segmented RemoteViews fallback below) need only a small
 * set of mandatory fields; everything else (segment/point colors, tracker/start/end icons, chip,
 * promotion, styled-by-progress) is optional per the platform API.
 *
 * Mandatory:
 * - **title** — a progress notification with no title is not renderable content.
 * - **a progress indicator** — at least one of: segments, points, the indeterminate flag, or a
 *   `pt_progress` value. Without any of these there is nothing to draw.
 */
internal class ProgressTemplateValidator(private val data: ProgressTemplateData) : Validator(emptyMap()) {

    override fun loadKeys(): List<Checker<out Any>> = emptyList()

    override fun validate(): Boolean {
        if (data.title.isNullOrBlank()) {
            PTLog.verbose("pt_progress: title (nt / pt_title) is mandatory. Notification will be suppressed.")
            return false
        }
        val hasIndicator =
            data.segments.isNotEmpty() || data.points.isNotEmpty() || data.indeterminate || data.progress != null
        if (!hasIndicator) {
            PTLog.verbose(
                "pt_progress: needs a progress indicator — segments, points, pt_progress_indeterminate, " +
                    "or a pt_progress value. Notification will be suppressed."
            )
            return false
        }
        return true
    }
}
