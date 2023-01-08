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


    /**
     * Defines a variable with kind. Can be Boolean, Byte, Short, Integer, Long, Float, Double,
     * Character, String, List, or Map. You may nest lists and maps arbitrarily.
     *
     * @param name Name of the variable.
     * @param defaultValue Default value.
     * @param kind Kind of the variable.
     * @param <T> Boolean, Byte, Short, Integer, Long, Float, Double, Character, String, List, or
     * Map.
     * @return Initialized variable.
     */
    public static <T> Var<T> define(String name, T defaultValue, String kind) {
        return define(name, defaultValue, kind, null);
    }

    /**
     * Defines a new variable with a default value.
     *
     * @param name Name of the variable.
     * @param defaultValue Default value of the variable. Can't be null.
     */
    public static <T> Var<T> define(String name, T defaultValue) {
        return define(name, defaultValue, VarCache.kindFromValue(defaultValue), null);
    }

    private static <T> Var<T> define(String name, T defaultValue, String kind, VarInitializer<T> initializer) {
        if (TextUtils.isEmpty(name)) {
            Log.e(TAG,"Empty name parameter provided.");
            return null;
        }
        Var<T> existing = VarCache.getVariable(name);
        if (existing != null) {
            return existing;
        }
        if (LPClassesMock.hasCalledStart() && !name.startsWith(Constants.Values.RESOURCES_VARIABLE)) {
            Log.i(TAG,"You should not create new variables after calling start (name=" + name + ")");
        }
        Var<T> var = new Var<>();
        try {
            var.name = name;
            var.nameComponents = VarCache.getNameComponents(name);
            var.defaultValue = defaultValue;
            var.value = defaultValue;
            var.kind = kind;
            if (name.startsWith(Constants.Values.RESOURCES_VARIABLE)) {
                var.isInternal = true;
            }
            if (initializer != null) {
                initializer.init(var);
            }
            var.cacheComputedValues();
            VarCache.registerVariable(var);
            if (Constants.Kinds.FILE.equals(var.kind)) {
                if (var.isResource) {
                    VarCache.registerFile(var.stringValue, var::defaultStream, var.hash, var.size);
                } else {
                    String defaultVal = var.defaultValue() == null ? null : var.defaultValue().toString();
                    VarCache.registerFile(var.stringValue, defaultVal, var::defaultStream);
                }
            }
            var.update();
        } catch (Throwable t) {
            LPClassesMock.exception(t);
        }
        return var;
    }

    protected Var() {
    }

    /**
     * Gets name of the variable.
     *
     * @return Varaible name.
     */
    public String name() {
        return name;
    }

    /**
     * Gets name components of a variable.
     *
     * @return Name components.
     */
    public String[] nameComponents() {
        return nameComponents;
    }

    /**
     * Gets the kind of a variable.
     *
     * @return Kind of a variable.
     */
    public String kind() {
        return kind;
    }

    /**
     * Gets variable default value.
     *
     * @return Default value.
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Get variable value.
     *
     * @return Value.
     */
    public T value() {
        warnIfNotStarted();
        return value;
    }

    /**
     * Gets overridden resource id for variable.
     *
     * @return Id of the overridden resource.
     */
    public int overrideResId() {
        return overrideResId;
    }

    /**
     * Sets overridden resource id for a variable.
     *
     * @param resId Resource id.
     */
    public void setOverrideResId(int resId) {
        overrideResId = resId;
    }

    /**
     * Applies correct number type to the value member based on defaultValue type.
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Parses src to initialize numberValue. If not parsable it uses the defaultValue.
     */
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

    private void cacheComputedValues() {
        if (value instanceof String) {
            stringValue = (String) value;
            modifyNumberValue(stringValue);
            modifyValue(numberValue);

        } else if (value instanceof Number) {
            stringValue = "" + value;
            numberValue = ((Number) value).doubleValue();
            modifyValue((Number) value);

        } else if (value != null &&
                !(value instanceof Iterable<?>) && !(value instanceof Map<?, ?>)) {
            stringValue = value.toString();
            numberValue = null;
        } else {
            stringValue = null;
            numberValue = null;
        }
    }

    private void warnIfNotStarted() {
        if (!isInternal && !LPClassesMock.hasStarted() && !printedCallbackWarning) {
            Log.i(TAG,"CleverTap hasn't finished retrieving values from the server. " +
                    "You should use a callback to make sure the value for '%s' is ready. " +
                    "Otherwise, your app may not use the most up-to-date value."+name);
            printedCallbackWarning = true;
        }
    }


    /**
     * Updates variable with values from server.
     */
    public synchronized void update() {
        // TODO: Clean up memory for resource variables.
        //data = null; // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
        T oldValue = value;
        value = VarCache.getMergedValueFromComponentArray(nameComponents);
        if (value == null && oldValue == null) {
            return;
        }
        if (value != null && oldValue != null && value.equals(oldValue) && hadStarted) {
            return;
        }
        cacheComputedValues();

        if (VarCache.silent() && name.startsWith(Constants.Values.RESOURCES_VARIABLE)
                && Constants.Kinds.FILE.equals(kind) && !fileIsPending) {
            triggerFileIsReady();
        }

        if (VarCache.silent()) {
            return;
        }

        if (LPClassesMock.hasStarted()) {
            triggerValueChanged();
        }

        // Check if file exists, otherwise we need to download it.
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

    private void triggerValueChanged() {
        synchronized (valueChangedHandlers) {
            for (VariableCallback<T> callback : valueChangedHandlers) {
                callback.setVariable(this);
                OperationQueue.sharedInstance().addUiOperation(callback);
            }
        }
    }

    /**
     * Adds value changed handler for a given variable.
     *
     * @param handler Handler to add.
     */
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

    /**
     * Removes value changed handler for a given variable.
     *
     * @param handler Handler to be removed.
     */
    public void removeValueChangedHandler(VariableCallback<T> handler) {
        synchronized (valueChangedHandlers) {
            valueChangedHandlers.remove(handler);
        }
    }

    private void triggerFileIsReady() {
        synchronized (fileReadyHandlers) {
            fileIsPending = false;
            for (VariableCallback<T> callback : fileReadyHandlers) {
                callback.setVariable(this);
                OperationQueue.sharedInstance().addUiOperation(callback);
            }
        }
    }

    /**
     * Adds file ready handler for a given variable.
     *
     * @param handler Handler to add.
     */
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

    /**
     * Removes file ready handler for a given variable.
     *
     * @param handler Handler to be removed.
     */
    public void removeFileReadyHandler(VariableCallback<T> handler) {
        if (handler == null) {
            Log.e(TAG,"Invalid handler parameter provided.");
            return;
        }
        synchronized (fileReadyHandlers) {
            fileReadyHandlers.remove(handler);
        }
    }

    /**
     * Returns file value for variable initialized as file/asset/resource.
     *
     * @return String representing file value.
     */
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

    /**
     * Returns object for specified key path.
     *
     * @param keys Keys to look for.
     * @return Object if found, null otherwise.
     */
    @SuppressWarnings("WeakerAccess") // Used by Air SDK.
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

    /**
     * Returns a number of elements contained in a List variable.
     *
     * @return Elements count or 0 if Variable is not a List.
     */
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

    /**
     * Gets a value from a variable initialized as Number.
     *
     * @return A Number value.
     */
    public Number numberValue() {
        warnIfNotStarted();
        return numberValue;
    }

    /**
     * Gets a value from a variable initialized as String.
     *
     * @return A String value.
     */
    public String stringValue() {
        warnIfNotStarted();
        return stringValue;
    }

    /**
     * Creates and returns InputStream for overridden file/asset/resource variable.
     * Caller is responsible for closing it properly to avoid leaking resources.
     *
     * @return InputStream for a file.
     */
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

    /**
     * Creates and returns InputStream for default file/asset/resource variable.
     * Caller is responsible for closing it properly to avoid leaking resources.
     *
     * @return InputStream for a file.
     */
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

    @Override
    public String toString() {
        return "Var(" + name + ")=" + value;
    }



    //can be removed //todo
    /**
     * Defines a variable for a file located in assets directory.
     *
     * @param name Name of the variable.
     * @param defaultFilename Default filename.
     * @return Initialized variable.
     */
    public static Var<String> defineAsset(String name, String defaultFilename) {
        return define(name, defaultFilename, Constants.Kinds.FILE, new VarInitializer<String>() {
            @Override
            public void init(Var<String> var) {
                var.isAsset = true;
            }
        });
    }

    //can be removed //todo
    /**
     * Define a resource variable with default value referencing id of the file located in
     * res/ directory.
     *
     * @param name Name of the variable.
     * @param resId Resource id of any file located in res/ directory.
     * @return Initalized variable.
     */
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
    /**
     * Defines a resource.
     *
     * @param name Name of the variable.
     * @param defaultFilename Default filename.
     * @param size Size of the data.
     * @param hash Hash of the data.
     * @param data Data.
     * @return Initalized variable.
     */
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
    /**
     * Defines a color.
     *
     * @param name Name of the variable
     * @param defaultValue Default value.
     * @return Initialized variable.
     */
    @SuppressWarnings("WeakerAccess")
    public static Var<Integer> defineColor(String name, int defaultValue) {
        return define(name, defaultValue, Constants.Kinds.COLOR, null);
    }

    //can be removed //todo
    /**
     * Defines a variable for a file.
     *
     * @param name Name of the variable.
     * @param defaultFilename Default filename.
     * @return Initialized variable.
     */
    public static Var<String> defineFile(String name, String defaultFilename) {
        return define(name, defaultFilename, Constants.Kinds.FILE, null);
    }


    private interface VarInitializer<T> {
        void init(Var<T> var);
    }

}
