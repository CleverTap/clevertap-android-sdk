package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig

internal class TemplatesManager(templates: Collection<CustomTemplate>) {

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

            return TemplatesManager(templates)
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
}
