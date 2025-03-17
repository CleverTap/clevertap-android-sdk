package com.clevertap.android.sdk.inapp.customtemplates.system

import com.clevertap.android.sdk.Application
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class OpenUrlTemplateTest: BaseTestCase() {

    @Test
    fun `presenter should call context setPresented when there is a url param value`() {
        val mockApplication = mockk<Application>(relaxed = true)
        val mockLogger = mockk<Logger>(relaxed = true)
        val template =
            OpenUrlTemplate.createTemplate(InAppActionHandler(mockLogger), mockApplication)
        val mockFunctionContext = mockk<CustomTemplateContext.FunctionContext>(relaxed = true)
        every { mockFunctionContext.getString("Android") } returns ""

        (template.presenter as? FunctionPresenter)?.onPresent(mockFunctionContext)
        verify(exactly = 0) { mockFunctionContext.setPresented() }

        every { mockFunctionContext.getString("Android") } returns "https://clevertap.com"
        (template.presenter as? FunctionPresenter)?.onPresent(mockFunctionContext)
        verify(exactly = 1) { mockFunctionContext.setPresented() }
    }
}
