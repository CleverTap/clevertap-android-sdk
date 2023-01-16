package com.clevertap.android.sdk.feat_variable;


import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.clevertap.android.sdk.feat_variable.extras.Constants;
import com.clevertap.android.sdk.feat_variable.extras.FileManager;
import com.clevertap.android.sdk.feat_variable.extras.FileManager.DownloadFileResult;
import com.clevertap.android.sdk.feat_variable.extras.OperationQueue;
import com.clevertap.android.sdk.feat_variable.mock.LPClassesMock;

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
    private final List<VariableCallback<T>> fileReadyHandlers = new ArrayList<>();
    private final List<VariableCallback<T>> valueChangedHandlers = new ArrayList<>();
    private boolean fileIsPending;
    private boolean hadStarted;
    private boolean isAsset;
    public boolean isResource;
    private int size;
    private String hash;
    private byte[] data;
    private boolean valueIsInAssets = false;
    private boolean isInternal;
    private int overrideResId;
    private static boolean printedCallbackWarning;
    private static final String TAG = "Var>";


    /*<basic getter-setters>*/
    @Override public String toString() {
        return "Var(" + name + ")=" + value;
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
    public int overrideResId() {
        return overrideResId;
    }
    public void setOverrideResId(int resId) {
        overrideResId = resId;
    }
    public void addValueChangedHandler(VariableCallback<T> handler) {
        if (handler == null) {
            Log.e(TAG,"Invalid handler parameter provided.");
            return;
        }

        synchronized (valueChangedHandlers) {
            valueChangedHandlers.add(handler);
        }
        if (LPClassesMock.hasStarted()) {
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

    /*</basic getter-setters>*/




    // define<Double>("some_global_var_name",12.4,"float")
    // define<String>("some_global_var_name2","hi","string")
    // this is called by parser when parsing code define @Variable variables for the first time
    public static <T> Var<T> define(String name, T defaultValue, String kind) {
        return define(name, defaultValue, kind, null);
    }

    public static <T> Var<T> define(String name, T defaultValue) {
        String type = VarCache.kindFromValue(defaultValue); // 1.2 -> "float" , 1 -> "integer" etc
        return define(name, defaultValue, type, null);
    }

    // define<Double>("some_global_var_name",12.4,"float",null)
    // define<String>("some_global_var_name2","hi","string",null)
    // this is called by parser( via define( name, value,kind)) when parsing code define @Variable variables for the first time
    private static <T> Var<T> define(String name, T defaultValue, String kind, VarInitializer<T> initializer) {
        //checks if name is correct . if correct continues or  returns null
        if (TextUtils.isEmpty(name)) {
            Log.e(TAG,"Empty name parameter provided.");
            return null;
        }

        // checks if var exists  in  VarCache.getVariable(name) . if exists, then returns the var, otherwise continues .
        Var<T> existing = VarCache.getVariable(name);
        if (existing != null) {
            return existing;
        }

        // check if LP has called start and whether name is not a special name. if either fails, then generates a log else continues // todo : discuss our conditions for this check
        if (LPClassesMock.hasCalledStart() && !name.startsWith(Constants.Values.RESOURCES_VARIABLE)) {
            Log.i(TAG,"You should not create new variables after calling start (name=" + name + ")");
        }
        Var<T> var = new Var<>();
        try {
            var.name = name;
            var.nameComponents = VarCache.getNameComponents(name);// "group1.group2.name" -> ["group1","group2","name"]
            var.defaultValue = defaultValue;
            var.value = defaultValue;
            var.kind = kind;
            if (name.startsWith(Constants.Values.RESOURCES_VARIABLE)) {
                var.isInternal = true;
            }
            if (initializer != null) {
                initializer.init(var);
            }

            var.cacheComputedValues(); //todo check var.cacheComputedValues()
            VarCache.registerVariable(var);  // will put var in VarCache.vars , & update VarCache.valueFromClient , VarCache.defaultKinds
            //can be removed //todo
            if (Constants.Kinds.FILE.equals(var.kind)) {
                if (var.isResource) {
                    VarCache.registerFile(var.stringValue, var::defaultStream, var.hash, var.size);
                } else {
                    String defaultVal = var.defaultValue() == null ? null : var.defaultValue().toString();
                    VarCache.registerFile(var.stringValue, defaultVal, var::defaultStream);
                }
            }


            var.update(); //todo  check var.update()
        } catch (Throwable t) {
            LPClassesMock.exception(t);
        }
        return var;
    }


    //todo what does this function do, and why it is used? //  unsure
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
    public synchronized void update() {
        T oldValue = value;
        value = VarCache.getMergedValueFromComponentArray(nameComponents);
        if (value == null && oldValue == null) {
            return;
        }
        if (value != null && oldValue != null && value.equals(oldValue) && hadStarted) {
            return;
        }
        cacheComputedValues();

        //can be removed//todo
        if (VarCache.silent() && name.startsWith(Constants.Values.RESOURCES_VARIABLE) && Constants.Kinds.FILE.equals(kind) && !fileIsPending) {
            triggerFileIsReady();
        }

        if (VarCache.silent()) {
            return;
        }

        if (LPClassesMock.hasStarted()) {
            triggerValueChanged();
        }

        //can be removed// Check if file exists, otherwise we need to download it. //todo
        if (Constants.Kinds.FILE.equals(kind)) {
            if (!Constants.isNoop()) {
                DownloadFileResult result = FileManager.maybeDownloadFile(
                        isResource, stringValue, (String) defaultValue, null,
                        new Runnable() {
                            @Override
                            public void run() {
                                triggerFileIsReady();
                            }
                        });
                valueIsInAssets = false;
                if (result == DownloadFileResult.DOWNLOADING) {
                    fileIsPending = true;
                } else if (result == DownloadFileResult.EXISTS_IN_ASSETS) {
                    valueIsInAssets = true;
                }
            }
            if (LPClassesMock.hasStarted() && !fileIsPending) {
                triggerFileIsReady();
            }
        }

        if (LPClassesMock.hasStarted()) {
            hadStarted = true;
        }
    }

    private void warnIfNotStarted() {
        if (!isInternal && !LPClassesMock.hasStarted() && !printedCallbackWarning) {
            Log.i(TAG,"CleverTap hasn't finished retrieving values from the server. " + "You should use a callback to make sure the value for '%s' is ready. " + "Otherwise, your app may not use the most up-to-date value."+name);
            printedCallbackWarning = true;
        }
    }

    //triggers the user's callbacks of variable changing (VariableCallback)
    private void triggerValueChanged() {
        synchronized (valueChangedHandlers) {
            for (VariableCallback<T> callback : valueChangedHandlers) {

                callback.setVariable(this);

                OperationQueue.sharedInstance().addUiOperation(callback);//runs this task on ui thread
            }
        }
    }


    // not needed for now
    public Object objectForKeyPath(Object... keys) {
        try {
            warnIfNotStarted();
            List<Object> components = new ArrayList<>();
            Collections.addAll(components, nameComponents);
            if (keys != null && keys.length > 0) {
                Collections.addAll(components, keys);
            }
            return VarCache.getMergedValueFromComponentArray(
                    components.toArray(new Object[components.size()]));
        } catch (Throwable t) {
            LPClassesMock.exception(t);
            return null;
        }
    }

    public int count() {
        try {
            warnIfNotStarted();
            Object result = VarCache.getMergedValueFromComponentArray(nameComponents);
            if (result instanceof List) {
                return ((List<?>) result).size();
            }
        } catch (Throwable t) {
            LPClassesMock.exception(t);
            return 0;
        }
        LPClassesMock.maybeThrowException(new UnsupportedOperationException("This variable is not a list."));
        return 0;
    }

    public String stringValue() {
        warnIfNotStarted();
        return stringValue;
    }





    /*<FILE RELATED STUFF : TO BE REMOVED>*/


    //canbe removed //todo
    public InputStream stream() {
        try {
            if (!Constants.Kinds.FILE.equals(kind)) {
                return null;
            }
            warnIfNotStarted();
            InputStream stream = FileManager.stream(isResource, isAsset, valueIsInAssets, fileValue(), (String) defaultValue, data);
            if (stream == null) {return defaultStream();}
            return stream;
        } catch (Throwable t) {
            LPClassesMock.exception(t);
            return null;
        }
    }


    //can be removed //todo
    public static Var<String> defineAsset(String name, String defaultFilename) {
        return define(name, defaultFilename, Constants.Kinds.FILE, new VarInitializer<String>() {
            @Override
            public void init(Var<String> var) {
                var.isAsset = true;
            }
        });
    }

    //can be removed //todo
    public static Var<String> defineResource(String name, int resId) {
        String resourceName = LPClassesMock.generateResourceNameFromId(resId);
        return define(name, resourceName, Constants.Kinds.FILE, new VarInitializer<String>() {
            @Override
            public void init(Var<String> var) {
                var.isResource = true;
            }
        });
    }


    //can be removed //todo
    public static Var<String> defineResource(String name, String defaultFilename, final int size, final String hash, final byte[] data) {
        return define(name, defaultFilename, Constants.Kinds.FILE, new VarInitializer<String>() {
            @Override
            public void init(Var<String> var) {
                var.isResource = true;
                var.size = size;
                var.hash = hash;
                var.data = data;
            }
        });
    }

    //can be removed //todo
    public static Var<Integer> defineColor(String name, int defaultValue) {
        return define(name, defaultValue, Constants.Kinds.COLOR, null);
    }

    //can be removed //todo
    public static Var<String> defineFile(String name, String defaultFilename) {
        return define(name, defaultFilename, Constants.Kinds.FILE, null);
    }

    //can be removed //todo
    public void addFileReadyHandler(VariableCallback<T> handler) {
        if (handler == null) {
            Log.e(TAG,"Invalid handler parameter provided.");
            return;
        }
        synchronized (fileReadyHandlers) {
            fileReadyHandlers.add(handler);
        }
        if (LPClassesMock.hasStarted() && !fileIsPending) {
            handler.handle(this);
        }
    }

    //can be removed //todo
    public void removeFileReadyHandler(VariableCallback<T> handler) {
        if (handler == null) {
            Log.e(TAG,"Invalid handler parameter provided.");
            return;
        }
        synchronized (fileReadyHandlers) {
            fileReadyHandlers.remove(handler);
        }
    }

    //can be removed //todo
    public String fileValue() {
        try {
            warnIfNotStarted();
            if (Constants.Kinds.FILE.equals(kind)) {
                return FileManager.fileValue(stringValue, (String) defaultValue, valueIsInAssets);
            }
        } catch (Throwable t) {
            LPClassesMock.exception(t);
        }
        return null;
    }

    //can be removed //todo
    private void triggerFileIsReady() {
        synchronized (fileReadyHandlers) {
            fileIsPending = false;
            for (VariableCallback<T> callback : fileReadyHandlers) {
                callback.setVariable(this);
                OperationQueue.sharedInstance().addUiOperation(callback);
            }
        }
    }

    //can be removed //todo
    private InputStream defaultStream() {
        try {
            if (!Constants.Kinds.FILE.equals(kind)) {
                return null;
            }
            return FileManager.stream(isResource, isAsset, valueIsInAssets, (String) defaultValue, (String) defaultValue, data);
        } catch (Throwable t) {
            LPClassesMock.exception(t);
            return null;
        }
    }



    private interface VarInitializer<T> {
        void init(Var<T> var);
    }

}
