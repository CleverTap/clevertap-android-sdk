package com.clevertap.android.sdk.variables.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CleverTap variable annotation.
 * <p>
 * <p>Use this to make this variable changeable from the CleverTap dashboard. Variables must be of
 * type boolean, byte, short, int, long, float, double, char, String, or Map.
 * <p>
 * <p>Variables with this annotation update when the API call for CleverTap fetch completes
 * successfully or fails (in which case values are loaded from a cache stored on the device).
 *
 * @author Ansh Sachdeva
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {
  /**
   * (Optional). The group to put the variable in. Use "." to nest groups.
   */
  String group() default "";

  /**
   * (Optional). The name of the variable. If not set, then uses the actual name of the field.
   */
  String name() default "";
}
