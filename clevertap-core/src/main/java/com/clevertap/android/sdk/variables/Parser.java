/*
 * Copyright 2013, CleverTap, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.clevertap.android.sdk.variables;

import android.text.TextUtils;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.annotations.Variable;
import com.clevertap.android.sdk.variables.callbacks.VariableCallback;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Parses CleverTap annotations when called via Parser.parseVariables(...)
 * or Parser.parseVariablesForClasses(...).
 *
 * @author Ansh Sachdeva
 */
public class Parser {

  /**
   * Parses annotations for all given object instances(eg, if variables are defined in
   * Activity class, then Parser.parseVariables(activityObj) can be called).
   *
   *  @param instances Objects of a class
   */
  public static void parseVariables(Object... instances) {
    try {
      for (Object instance : instances) {
        parseVariablesHelper(instance, instance.getClass());
      }
    } catch (Throwable t) {
      Logger.v("Error parsing variables", t);
    }
  }

  /**
   * Parses annotations for all given classes.(eg, if variables are defined in a
   * separate 'MyVariables' class, then Parser.parseVariablesForClasses(MyVariables.class) can be
   * called)
   */
  public static void parseVariablesForClasses(Class<?>... classes) {
    try {
      for (Class<?> clazz : classes) {
        parseVariablesHelper(null, clazz);
      }
    } catch (Throwable t) {
      Logger.v("Error parsing variables", t);
    }
  }


  /**
   * Generic function to parse variables defined in a class or an object
   *
   * For each field defined in the class (i.e clazz instance) with @{@link Variable} annotation, this function
   * calls {@link #defineVariable(Object, String, Object, String, Field)} , where:
   *
   *
   * @param instance object of a class
   * @param clazz class instance of a class
   */
  private static void parseVariablesHelper(Object instance, Class<?> clazz) {

    try {
      Field[] fields = clazz.getFields();

      for (final Field field : fields) {
        String group = "", name = "";
        boolean isFile = false; //TODO @Ansh no use of boolean, remove it and related code
        if (field.isAnnotationPresent(Variable.class)) {
          Variable annotation = field.getAnnotation(Variable.class);
          if(annotation!=null){
            group = annotation.group();
            name = annotation.name();
          }
        }
      else {
          continue;
        }

        String variableName = name;
        if (TextUtils.isEmpty(variableName)) {
          variableName = field.getName();
        }
        if (!TextUtils.isEmpty(group)) {
          variableName = group + "." + variableName;
        }

        Class<?> fieldType = field.getType();
        String fieldTypeString = fieldType.toString();
        if (fieldTypeString.equals("int")) {
          defineVariable(instance, variableName, field.getInt(instance), "integer", field);
        } else if (fieldTypeString.equals("byte")) {
          defineVariable(instance, variableName, field.getByte(instance), "integer", field);
        } else if (fieldTypeString.equals("short")) {
          defineVariable(instance, variableName, field.getShort(instance), "integer", field);
        } else if (fieldTypeString.equals("long")) {
          defineVariable(instance, variableName, field.getLong(instance), "integer", field);
        } else if (fieldTypeString.equals("char")) {
          defineVariable(instance, variableName, field.getChar(instance), "integer", field);
        } else if (fieldTypeString.equals("float")) {
          defineVariable(instance, variableName, field.getFloat(instance), "float", field);
        } else if (fieldTypeString.equals("double")) {
          defineVariable(instance, variableName, field.getDouble(instance), "float", field);
        } else if (fieldTypeString.equals("boolean")) {
          defineVariable(instance, variableName, field.getBoolean(instance), "bool", field);
        } else if (fieldType.isPrimitive()) {
          Logger.v( "Variable " + variableName + " is an unsupported primitive type.");
        } else if (fieldType.isArray()) {
          Logger.v( "Variable " + variableName + " should be a List instead of an Array.");
        } else if (List.class.isAssignableFrom(fieldType)) {
          defineVariable(instance, variableName, field.get(instance), "list", field);
        } else if (Map.class.isAssignableFrom(fieldType)) {
          defineVariable(instance, variableName, field.get(instance), "group", field);
        } else {
          Object value = field.get(instance);
          String stringValue = value == null ? null : value.toString();
          if (!isFile) {
            defineVariable(instance, variableName, stringValue, "string", field);
          }
        }
      }
    } catch (IllegalArgumentException t) {
      Logger.v( "Error parsing variables(IllegalArgumentException):", t);
      t.printStackTrace();
    } catch (IllegalAccessException t) {
      Logger.v( "Error parsing variables(IllegalAccessException):", t);
      t.printStackTrace();
    } catch (Throwable t) {
      Logger.v( "Error parsing variables:", t);
      t.printStackTrace();
    }

  }


  /**
   * This function simply calls {@link Var#define(String, Object, String)} and attached a variable
   * update listener. When variable update listener is called, it sets the values in the field with
   * new data using reflection.
   *
   * @param instance obj of a class
   * @param name name of the field from class
   * @param value  value of the field
   * @param kind a string representing the type of field
   * @param field instance of field itself
   */
  private static <T> void defineVariable(final Object instance, String name, T value, String kind, final Field field) {
    // we first call var.define(..) with field name, value and kind
    final Var<T> var = Var.define(name, value, kind);
    if (var == null) {
      Logger.v("Something went wrong, var is null, returning");
      return;
    }

    final boolean hasInstance = instance != null;
    final WeakReference<Object> weakInstance = new WeakReference<>(instance);

    var.addValueChangedHandler(new VariableCallback<T>() {
      @Override
      public void handle(Var<T> variable) {
        Object instanceFromWeakRef = weakInstance.get();
        if ((hasInstance && instanceFromWeakRef == null) || field == null) {
          var.removeValueChangedHandler(this);
          return;
        }
        try {
          boolean accessible = field.isAccessible();
          if (!accessible) {
            field.setAccessible(true);
          }
          field.set(instanceFromWeakRef, var.value());
          if (!accessible) {
            field.setAccessible(false);
          }
        } catch (IllegalArgumentException e) {
          Logger.v("Invalid value " + var.value() + " for field " + var.name(), e);
        } catch (IllegalAccessException e) {
          Logger.v("Error setting value for field " + var.name(), e);
        }
      }
    });
  }

}