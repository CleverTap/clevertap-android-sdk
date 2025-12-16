package com.clevertap.android.sdk.validation.chargedevent

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.ChargedEventItemsNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.Normalizer

/**
 * Normalizes charged event items according to ValidationConfig.
 * Only performs normalization - does not validate.
 *
 * Normalization includes:
 * - Counting the number of items
 * - Converting null input to empty list
 */
class ChargedEventItemsNormalizer(
    private val config: ValidationConfig
) : Normalizer<List<*>?, ChargedEventItemsNormalizationResult> {

    override fun normalize(input: List<*>?): ChargedEventItemsNormalizationResult {
        val items = input ?: emptyList<Any>()
        
        return ChargedEventItemsNormalizationResult(
            itemsCount = items.size
        )
    }
}
