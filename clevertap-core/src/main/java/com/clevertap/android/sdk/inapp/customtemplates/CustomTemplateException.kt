package com.clevertap.android.sdk.inapp.customtemplates

/**
 * Class of exceptions that can be thrown when registering and defining [CustomTemplate]s. They are used for
 * validating for correctness of the templates and point to incorrect definitions. Those exceptions should generally
 * not be caught, since they will not be raised when the registered templates are correctly defined.
 */
class CustomTemplateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
