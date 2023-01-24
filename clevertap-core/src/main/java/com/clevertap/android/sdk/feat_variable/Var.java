package com.clevertap.android.sdk.feat_variable;


import android.text.TextUtils;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.feat_variable.callbacks.VariableCallback;
import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CleverTap variable.
 *
 * @param <T> Type of the variable. Can be Boolean, Byte, Short, Integer, Long, Float, Double,
 * Character, String, List, or Map. You may nest lists and maps arbitrarily.
 * @author Ansh Sachdeva
 */
public class Var<T> {
    //constructor
    public Var() {}

    private String name;
    private String[] nameComponents;
    public String stringValue;
    private Double numberValue;
    private T defaultValue;
    private T value;
    private String kind;
    private boolean hadStarted = false;// todo @hristo @darshan this seems dangerous as it will stop update() from working after 1sr call. should we remove it?
    private final List<VariableCallback<T>> valueChangedHandlers = new ArrayList<>();
    private static boolean printedCallbackWarning;
    public static final String RESOURCES_VARIABLE = "__Android Resources";



    /*<basic getter-setters>*/
    @Override public String toString() {
        return "Var(" + name + ","+value+")" ;
    }
    public String name() {
        return name;
    }
    public String[] nameComponents() {
        return nameComponents;
    }
    public String kind() {
        return kind;
    }
    public T defaultValue() {
        return defaultValue;
    }
    public T value() {
        warnIfNotStarted();
        return value;
    }
    public void addValueChangedHandler(VariableCallback<T> handler) {
        if (handler == null) {
            Logger.v( "Invalid handler parameter provided.");
            return;
        }

        synchronized (valueChangedHandlers) {
            valueChangedHandlers.add(handler);
        }

        // make sure the handler is called evenm after the value has been updated
        if (CTVariables.variableResponseReceived()) {
            handler.handle(this);
        }
    }
    public void removeValueChangedHandler(VariableCallback<T> handler) {
        synchronized (valueChangedHandlers) {
            valueChangedHandlers.remove(handler);
        }
    }
    public Number numberValue() {
        warnIfNotStarted();
        return numberValue;
    }



    public static <T> Var<T> define(String name, T defaultValue) {
        String type = CTVariableUtils.kindFromValue(defaultValue);
        return define(name, defaultValue, type);
    }

    // define<Double>("some_global_var_name",12.4,"float",null)
    // define<String>("some_global_var_name2","hi","string",null)
    // this is called by parser( via define( name, value,kind)) when parsing code define @Variable variables for the first time
    public static <T> Var<T> define(String name, T defaultValue,String kind) {
        //checks if name is correct . if correct continues or  returns null
        if (TextUtils.isEmpty(name)) {
            Logger.v("Empty name parameter provided.");
            return null;
        }

        // checks if var exists  in  VarCache.getVariable(name) . if exists, then returns the var, otherwise continues .
        Var<T> existing = VarCache.getVariable(name);
        Logger.v("current variable value for name='"+name+"' is : '"+existing+"'");
        if (existing != null) {
            return existing;
        }

        // check if  name is not a special name. if either fails, then generates a log else continues // todo piyush : discuss our conditions for this check
        if (!name.startsWith(RESOURCES_VARIABLE)) {
            Logger.v("You should not create new variables after calling start (name=" + name + ")");
        }
        Var<T> var = new Var<>();
        try {
            var.name = name;
            var.nameComponents = CTVariableUtils.getNameComponents(name);// "group1.group2.name" -> ["group1","group2","name"]
            var.defaultValue = defaultValue;
            var.value = defaultValue;
            var.kind = kind;

            var.cacheComputedValues();
            VarCache.registerVariable(var);  // will put var in VarCache.vars , & update VarCache.valueFromClient , VarCache.defaultKinds

            var.update();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return var;
    }


    private void cacheComputedValues() {
        if (value instanceof String) {
            stringValue = (String) value;
            modifyNumberValue(stringValue);
            modifyValue(numberValue);

        } else if (value instanceof Number) {
            stringValue = "" + value;
            numberValue = ((Number) value).doubleValue();
            modifyValue((Number) value);

        } else if (value != null && !(value instanceof Iterable<?>) && !(value instanceof Map<?, ?>)) {
            stringValue = value.toString();
            numberValue = null;
        } else {
            stringValue = null;
            numberValue = null;
        }
    }

    // values come from the server as Number type. so we type cast in here to the actual  java type accordingly  using the defaultValue's type
    private void modifyValue(Number src) {
        if (src == null)
            return;

        if (defaultValue instanceof Byte) {
            value = (T) (Byte) src.byteValue();
        } else if (defaultValue instanceof Short) {
            value = (T) (Short) src.shortValue();
        } else if (defaultValue instanceof Integer) {
            value = (T) (Integer) src.intValue();
        } else if (defaultValue instanceof Long) {
            value = (T) (Long) src.longValue();
        } else if (defaultValue instanceof Float) {
            value = (T) (Float) src.floatValue();
        } else if (defaultValue instanceof Double) {
            value = (T) (Double) src.doubleValue();
        } else if (defaultValue instanceof Character) {
            value = (T) (Character) (char) src.intValue();
        }
    }

    //prob a  fault-tolernance check around a case where server values are number but in string format (like "20_000") . this will convert the string to actual number  in double format
    private void modifyNumberValue(String src) {
        try {
            numberValue = Double.valueOf(src);
        } catch (NumberFormatException e) {
            numberValue = null;
            if (defaultValue instanceof Number) {
                numberValue = ((Number) defaultValue).doubleValue();
            }
        }
    }

    // updates the values of value[g] from cached value to server value  . called by VarCahe.applyVariableDiffs()
    //todo : check what would happen when we start application, and then request variables from server again after sometime (after changing values in server)
    public synchronized void update() {
        T oldValue = value;
        value = VarCache.getMergedValueFromComponentArray(nameComponents);
        Logger.v("update: value='"+value+"', oldValue='"+oldValue+"', hadStarted="+hadStarted);
        if (value == null && oldValue == null) {
            return;
        }
        if (value != null && oldValue != null && value.equals(oldValue) && hadStarted) {
            return;
        }
        cacheComputedValues();
        Logger.v("CTVariables.variableResponseReceived()="+CTVariables.variableResponseReceived());
        if (CTVariables.variableResponseReceived()) {
            hadStarted = true;
            triggerValueChanged();
        }
    }

    private void warnIfNotStarted() {
        if ( !CTVariables.variableResponseReceived() && !printedCallbackWarning) {
            Logger.v( "CleverTap hasn't finished retrieving values from the server. You should use a callback to make sure the value for "+name+" is ready. Otherwise, your app may not use the most up-to-date value.");
            printedCallbackWarning = true;
        }
    }

    //triggers the user's callbacks of variable changing (VariableCallback)
    private void triggerValueChanged() {
        synchronized (valueChangedHandlers) {
            for (VariableCallback<T> callback : valueChangedHandlers) {
                callback.setVariable(this);
                Utils.runOnUiThread(callback);

            }
        }
    }

    // if variable is of type list, then it will return the size of list
    public int count() {
        try {
            warnIfNotStarted();
            Object result = VarCache.getMergedValueFromComponentArray(nameComponents);
            if (result instanceof List) {
                return ((List<?>) result).size();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return 0;
        }
        CTVariableUtils.maybeThrowException(new UnsupportedOperationException("This variable is not a list."));
        return 0;
    }

    public String stringValue() {
        warnIfNotStarted();
        return stringValue;
    }
}
