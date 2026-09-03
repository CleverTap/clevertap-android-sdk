package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.ProgressTemplateData
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard: the progress template must validate with NO required content keys. Previously it
 * used ContentValidator (which force-unwraps title/message) and NPE'd on the empty key map.
 */
class ProgressTemplateValidatorTest {

    @Test
    fun `progress template validates true with an empty payload`() {
        val data = ProgressTemplateData(segments = emptyList(), points = emptyList())
        val validator = ValidatorFactory.getValidator(data)
        assertNotNull(validator)
        assertTrue(validator!!.validate())
    }
}
