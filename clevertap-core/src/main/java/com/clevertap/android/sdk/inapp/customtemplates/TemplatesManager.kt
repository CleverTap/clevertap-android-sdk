package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.ContextDismissListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.FunctionContext
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TemplatesManager(templates: Collection<CustomTemplate>, private val logger: Logger) :
    ContextDismissListener {

    companion object {

        private val templateProducers = mutableListOf<TemplateProducer>()

        @JvmStatic
        fun register(templateProducer: TemplateProducer) {
            templateProducers.add(templateProducer)
        }

        @JvmStatic
        fun createInstance(
            ctInstanceConfig: CleverTapInstanceConfig,
            systemTemplates: Set<CustomTemplate>
        ): TemplatesManager {
            val templates = mutableSetOf<CustomTemplate>()

            for (producer in templateProducers) {
                addProducedTemplates(
                    producer = producer,
                    ctInstanceConfig = ctInstanceConfig,
                    systemTemplates = systemTemplates,
                    templates = templates
                )
            }

            templates.addAll(systemTemplates)

            return TemplatesManager(templates, ctInstanceConfig.logger)
        }

        /**
         * Adds every template defined by [producer] to [templates].
         *
         * @throws CustomTemplateException for a template that cannot be registered, see [registrationError]
         */
        private fun addProducedTemplates(
            producer: TemplateProducer,
            ctInstanceConfig: CleverTapInstanceConfig,
            systemTemplates: Set<CustomTemplate>,
            templates: MutableSet<CustomTemplate>
        ) {
            for (producedTemplate in producer.defineTemplates(ctInstanceConfig)) {
                val registrationError = registrationError(
                    producedTemplate = producedTemplate,
                    systemTemplates = systemTemplates,
                    registeredTemplates = templates
                )
                if (registrationError != null) {
                    throw CustomTemplateException(registrationError)
                }
                templates.add(producedTemplate)
            }
        }

        /**
         * Why [producedTemplate] cannot be registered (it is system-defined, clashes with a system template
         * or is already in [registeredTemplates]), or null if it can.
         */
        private fun registrationError(
            producedTemplate: CustomTemplate,
            systemTemplates: Set<CustomTemplate>,
            registeredTemplates: Set<CustomTemplate>
        ): String? {
            return when {
                producedTemplate.isSystemDefined ->
                    "Cannot define system template with a name \"${producedTemplate.name}\"."

                systemTemplates.contains(producedTemplate) ->
                    "CustomTemplate with a name \"${producedTemplate.name}\" is a system template."

                registeredTemplates.contains(producedTemplate) ->
                    "CustomTemplate with a name \"${producedTemplate.name}\" is already registered."

                else -> null
            }
        }

        fun clearRegisteredProducers() {
            templateProducers.clear()
        }
    }

    private val customTemplates: Map<String, CustomTemplate> = templates.associateBy { template -> template.name }
    private val activeContexts: MutableMap<String, CustomTemplateContext> = mutableMapOf()

    fun isTemplateRegistered(templateName: String): Boolean = customTemplates.contains(templateName)

    fun getAllRegisteredTemplates() = customTemplates.values.filter { !it.isSystemDefined }

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
