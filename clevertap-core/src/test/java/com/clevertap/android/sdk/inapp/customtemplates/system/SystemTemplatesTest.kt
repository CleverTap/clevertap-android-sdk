package com.clevertap.android.sdk.inapp.customtemplates.system

import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertTrue

class SystemTemplatesTest : BaseTestCase() {

    @Test
    fun `system functions should always call context setDismissed()`() {
        val mockInAppActionHandler = mockk<InAppActionHandler>(relaxed = true)
        val systemTemplates =
            SystemTemplates.getSystemTemplates(mockInAppActionHandler, application)
        assertTrue(systemTemplates.isNotEmpty())
        for (template in systemTemplates) {
            when (template.presenter) {
                is FunctionPresenter -> {
                    val mockFunctionContext =
                        mockk<CustomTemplateContext.FunctionContext>(relaxed = true)
                    template.presenter.onPresent(mockFunctionContext)
                    verify { mockFunctionContext.setDismissed() }
                }

                is TemplatePresenter -> {
                    val mockTemplateContext =
                        mockk<CustomTemplateContext.TemplateContext>(relaxed = true)
                    template.presenter.onPresent(mockTemplateContext)
                    verify { mockTemplateContext.setDismissed() }
                }
            }
        }
    }
}
