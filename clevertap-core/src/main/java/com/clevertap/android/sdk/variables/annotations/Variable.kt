package com.clevertap.android.sdk.variables.annotations

/**
 * CleverTap variable annotation.
 *
 * Use this to make this variable changeable from the CleverTap dashboard. Variables must be of
 * type boolean, byte, short, int, long, float, double, char, String, or Map.
 *
 * Variables with this annotation update when the API call for CleverTap fetch completes
 * successfully or fails (in which case values are loaded from a cache stored on the device).
 *
 * Retention and target are unchanged from the Java original: read at runtime via reflection
 * (`Field#getAnnotation`), applicable to fields only.
 *
 * @property group (Optional). The group to put the variable in. Use "." to nest groups.
 * @property name (Optional). The name of the variable. If not set, then uses the actual name of the field.
 * @author Ansh Sachdeva
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Variable(
    val group: String = "",
    val name: String = ""
)
