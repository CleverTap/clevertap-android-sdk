package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.ContextDismissListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.FunctionContext
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal class TemplatesManager(templates: Collection<CustomTemplate>, private val logger: Logger) :
    ContextDismissListener {

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

    private val customTemplates: Map<String, CustomTemplate> = templates.associateBy { template -> template.name }
    private val activeContexts: MutableMap<String, CustomTemplateContext> = mutableMapOf()

    fun isTemplateRegistered(templateName: String): Boolean = customTemplates.contains(templateName)

    fun getAllRegisteredTemplates() = customTemplates.values

    fun getTemplate(templateName: String): CustomTemplate? = customTemplates[templateName]

    fun getActiveContextForTemplate(templateName: String): CustomTemplateContext? = activeContexts[templateName]

    fun presentTemplate(
        notification: CTInAppNotification,
        inAppListener: InAppListener,
        resourceProvider: FileResourceProvider
    ) {
        val context = createContextFromInApp(notification, inAppListener,resourceProvider) ?: return
        val template = customTemplates[context.templateName]
        if (template == null) {
            logger.info("CustomTemplates", "Cannot find template with name ${context.templateName}")
            return
        }

        when (val presenter = template.presenter) {
            is TemplatePresenter -> {
                if (context is TemplateContext) {
                    activeContexts[template.name] = context
                    presenter.onPresent(context)
                }
            }

            is FunctionPresenter -> {
                if (context is FunctionContext) {
                    activeContexts[template.name] = context
                    presenter.onPresent(context)
                }
            }
        }
    }

    fun closeTemplate(notification: CTInAppNotification) {
        val templateName = notification.customTemplateData?.templateName
        if (templateName == null) {
            logger.debug("CustomTemplates", "Cannot close custom template from notification without template name")
            return
        }

        val context = activeContexts[templateName]
        if (context == null) {
            logger.debug("CustomTemplates", "Cannot close custom template without active context")
            return
        }

        val template = customTemplates[templateName]
        if (template == null) {
            logger.info("CustomTemplates", "Cannot find template with name $templateName")
            return
        }

        //only TemplateContext has onClose
        val presenter = template.presenter
        if (presenter is TemplatePresenter && context is TemplateContext) {
            presenter.onClose(context)
        }
    }

    override fun onDismissContext(context: CustomTemplateContext) {
        activeContexts.remove(context.templateName)
    }

    private fun createContextFromInApp(
        notification: CTInAppNotification,
        inAppListener: InAppListener,
        resourceProvider: FileResourceProvider
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

        return CustomTemplateContext.createContext(
            template,
            notification,
            inAppListener,
            resourceProvider,
            dismissListener = this,
            logger
        )
    }
}
