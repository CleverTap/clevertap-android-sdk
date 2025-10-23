package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.InAppFCManager
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager

/**
 * In-app messaging feature
 * Manages in-app notifications, templates, evaluations, and impressions
 */
internal data class InAppFeature(
    val inAppController: InAppController,
    val evaluationManager: EvaluationManager,
    val impressionManager: ImpressionManager,
    val templatesManager: TemplatesManager
) {
    var inAppFCManager: InAppFCManager? = null
}
