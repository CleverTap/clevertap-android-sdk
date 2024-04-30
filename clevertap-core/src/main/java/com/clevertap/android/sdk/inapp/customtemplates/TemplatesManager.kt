package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.FunctionContext
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext

internal class TemplatesManager(templates: Collection<CustomTemplate>, private val logger: Logger) {

    companion object {

        private val templateProducers = mutableListOf<TemplateProducer>()

        @JvmStatic
        fun register(templateProducer: TemplateProducer) {
            templateProducers.add(templateProducer)
        }

        @JvmStatic
        fun createInstance(ctInstanceConfig: CleverTapInstanceConfig): TemplatesManager {
            val templates = mutableSetOf<CustomTemplate>()

            for (producer in templateProducers) {
                for (producedTemplate in producer.defineTemplates(ctInstanceConfig)) {
                    if (templates.contains(producedTemplate)) {
                        throw CustomTemplateException("CustomTemplate with a name \"${producedTemplate.name}\" is already registered")
                    }

                    templates.add(producedTemplate)
                }
            }

            return TemplatesManager(templates, ctInstanceConfig.logger)
        }

        fun clearRegisteredProducers() {
            templateProducers.clear()
        }
    }

    private val customTemplates: Map<String, CustomTemplate>

    init {
        customTemplates = templates.associateBy { template -> template.name }
    }

    fun isTemplateRegistered(templateName: String): Boolean = customTemplates.contains(templateName)

    fun getTemplate(templateName: String): CustomTemplate? = customTemplates[templateName]

    fun presentTemplate(notification: CTInAppNotification, inAppListener: InAppListener) {
        val context = createContextFromInApp(notification, inAppListener) ?: return
        val template = customTemplates[context.templateName]
        if (template == null) {
            logger.info("CustomTemplates", "Cannot find template with name ${context.templateName}")
            return
        }

        when (val presenter = template.presenter) {
            is TemplatePresenter -> {
                if (context is TemplateContext) {
                    presenter.onPresent(context)
                }
            }

            is FunctionPresenter -> {
                if (context is FunctionContext) {
                    presenter.onPresent(context)
                }
            }
        }
    }

    fun closeTemplate(notification: CTInAppNotification, inAppListener: InAppListener) {
        val context = createContextFromInApp(notification, inAppListener) ?: return
        val template = customTemplates[context.templateName]
        if (template == null) {
            logger.info("CustomTemplates", "Cannot find template with name ${context.templateName}")
            return
        }
        //only TemplateContext has onClose
        val presenter = template.presenter
        if (presenter is TemplatePresenter && context is TemplateContext) {
            presenter.onClose(context)
        }
    }

    private fun createContextFromInApp(
        notification: CTInAppNotification,
        inAppListener: InAppListener
    ): CustomTemplateContext? {
        val templateName = notification.customTemplateData?.templateName
        if (templateName == null) {
            logger.debug("CustomTemplates", "Cannot create TemplateContext from notification without template name")
            return null
        }

        val template = customTemplates[templateName]
        if (template == null) {
            logger.debug(
                "CustomTemplates",
                "Cannot create TemplateContext for non-registered template: $templateName"
            )
            return null
        }

        return CustomTemplateContext.createContext(template, notification, inAppListener, logger)
    }
}
