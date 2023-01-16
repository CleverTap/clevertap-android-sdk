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

package com.clevertap.android.sdk.feat_variable;

import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Parses CleverTap annotations when called via Parser.parseVariables(...)
 * or Parser.parseVariablesForClasses(...)
 *
 * @author Ansh Sachdeva
 */
public class Parser {


  private static final String TAG = "Parser>";


  /**
   * Parses CleverTap annotations for all given object instances(eg, if variables are defined in
   * Activity class, then Parser.parseVariables(this) can be called)
   */
  public static void parseVariables(Object... instances) {
    try {
      for (Object instance : instances) {
        parseVariablesHelper(instance, instance.getClass());
      }
    } catch (Throwable t) {
      Log.e(TAG,"Error parsing variables", t);
    }
  }

  /**
   * Parses CleverTap annotations for all given classes.(eg, if variables are defined in a
   * separate 'MyVariables' class, then Parser.parseVariablesForClasses(MyVariables.class) can be
   * called)
   */
  public static void parseVariablesForClasses(Class<?>... classes) {
    try {
      for (Class<?> clazz : classes) {
        parseVariablesHelper(null, clazz);
      }
    } catch (Throwable t) {
      Log.e(TAG,"Error parsing variables", t);
    }
  }


  // note : changes: removed all errors and try catch them here only
  //parseVariablesHelper(context,MainActivity::class.java)//<-- call that occurred when Parser.parseVariables(this) in MainActivity onCreate got called
  //parseVariablesHelper(null,MyVars::class.java);        //<-- call that occurred when Parser.parseVariablesForClasses(MyVars::class.java) got called
  private static void parseVariablesHelper(Object instance, Class<?> clazz) {

    try {
      //fields = array of all variables in clazz
      Field[] fields = clazz.getFields();

      /*
         for each field  in fields , if @Variable or @File annotation is present, it will try to call
         defineVariable(a,b,c,d,e) else continue with iteration. the parameters are:
           a) instance = null/context/activity/application obj
           b) variableName = either "abc.xyz" from @Variable(name="xyz",group="abc")/@File(name="xyz",group="abc")/ or field.getName()
           c) value of field. via field.getX() (x= Int/Float/Double/Boolean/etc.)
           d) a string : "integer" or "float" or "bool" or "group" or "string"
           e) the field itself
         it will also try to call defineFileVariable(...) if it has @File annotation
       */
      for (final Field field : fields) {
        String group = "", name = "";
        boolean isFile = false;
        if (field.isAnnotationPresent(Variable.class)) {
          Variable annotation = field.getAnnotation(Variable.class);
          if(annotation!=null){
            group = annotation.group();
            name = annotation.name();
          }
        } else if (field.isAnnotationPresent(File.class)) {
          File annotation = field.getAnnotation(File.class);
          if(annotation!=null){
            group = annotation.group();
            name = annotation.name();
          }
          isFile = true;
        } else {
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
          Log.e(TAG, "Variable " + variableName + " is an unsupported primitive type.");
        } else if (fieldType.isArray()) {
          Log.e(TAG, "Variable " + variableName + " should be a List instead of an Array.");
        } else if (fieldType.isAssignableFrom(List.class)) {
          defineVariable(instance, variableName, field.get(instance), "list", field);
        } else if (fieldType.isAssignableFrom(Map.class)) {
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
      Log.e(TAG, "Error parsing variables(IllegalArgumentException):", t);
      t.printStackTrace();
    } catch (IllegalAccessException t) {
      Log.e(TAG, "Error parsing variables(IllegalAccessException):", t);
      t.printStackTrace();
    } catch (Throwable t) {
      Log.e(TAG, "Error parsing variables:", t);
      t.printStackTrace();
    }

  }


  //defineVariable(activity,"some_global_var_name",12.4,"float",Field("some_global_var_name",12.4));
  //defineVariable(null,"some_global_var_name",12.4,"float",Field("some_global_var_name",12.4));
  private static <T> void defineVariable(final Object instance, String name, T value, String kind, final Field field) {
    // we first call var.define(..) with field name, value and kind
    final Var<T> var = Var.define(name, value, kind);

    final boolean hasInstance = instance != null;
    final WeakReference<Object> weakInstance = new WeakReference<>(instance);

    // if vars are defined via @Variable annotation, we always attach a value change listener(VariableCallback) , but if they are defined via Var.define(), its upto user to add such listers if needed.

    //then we set a listener on var instance which will give a callback with new var<x> instance
    // whenever its triggered. when this happens, we update field's field.value = var.value
    // (not variable.value, the one returned in callback
    // also note that to set field's value, field must not be a non final(i.e mutable) value, therefore we first call
    // field.setAccessible(true) to make it mutable
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
          Log.e(TAG,"Invalid value " + var.value() + " for field " + var.name(), e);
        } catch (IllegalAccessException e) {
          Log.e(TAG,"Error setting value for field " + var.name(), e);
        }
      }
    });
  }

}
