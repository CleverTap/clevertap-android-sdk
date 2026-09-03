package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.ProgressTemplateData
import com.clevertap.android.pushtemplates.styles.ProgressPayloadParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Validation for the progress template: title is mandatory, plus at least one progress indicator
 * (segments, points, indeterminate flag, or a pt_progress value). Everything else is optional.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressTemplateValidatorTest {

    private fun data(
        title: String? = "Order #1",
        progress: Int? = null,
        indeterminate: Boolean = false,
        segments: List<ProgressPayloadParser.SegmentData> = emptyList(),
        points: List<ProgressPayloadParser.PointData> = emptyList(),
    ) = ProgressTemplateData(
        title = title, progress = progress, progressMax = null,
        indeterminate = indeterminate, segments = segments, points = points
    )

    private fun validate(d: ProgressTemplateData): Boolean {
        val v = ValidatorFactory.getValidator(d)
        assertNotNull(v)
        return v!!.validate()
    }

    @Test
    fun `valid with title + segments`() {
        assertTrue(validate(data(segments = listOf(ProgressPayloadParser.SegmentData(1, null)))))
    }

    @Test
    fun `valid with title + a progress value`() {
        assertTrue(validate(data(progress = 40)))
    }

    @Test
    fun `valid with title + indeterminate`() {
        assertTrue(validate(data(indeterminate = true)))
    }

    @Test
    fun `invalid without a title`() {
        assertFalse(validate(data(title = null, progress = 40)))
        assertFalse(validate(data(title = "", progress = 40)))
    }

    @Test
    fun `invalid with a title but no progress indicator`() {
        assertFalse(validate(data()))
    }
}
