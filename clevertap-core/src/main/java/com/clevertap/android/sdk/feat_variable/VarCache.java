/*
 * Copyright 2022, Leanplum, Inc. All rights reserved.
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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.clevertap.android.sdk.feat_variable.extras.AESCrypt;
import com.clevertap.android.sdk.feat_variable.extras.APIConfig;
import com.clevertap.android.sdk.feat_variable.extras.CacheUpdateBlock;
import com.clevertap.android.sdk.feat_variable.extras.CollectionUtil;
import com.clevertap.android.sdk.feat_variable.extras.Constants;
import com.clevertap.android.sdk.feat_variable.extras.FileManager;
import com.clevertap.android.sdk.feat_variable.extras.FileManager.HashResults;
import com.clevertap.android.sdk.feat_variable.extras.JsonConverter;
import com.clevertap.android.sdk.feat_variable.extras.LocationManager;
import com.clevertap.android.sdk.feat_variable.extras.SharedPreferencesUtil;
import com.clevertap.android.sdk.feat_variable.mock.LPClassesMock;

/**
 * Variable cache.
 *
 * @author Andrew First.
 */
public class VarCache {
    private static final String TAG = "VarCache>";
    private static final Map<String, Var<?>> vars = new ConcurrentHashMap<>();
    private static final Map<String, Object> fileAttributes = new HashMap<>();
    private static final Map<String, StreamProvider> fileStreams = new HashMap<>();
    private static volatile String varsJson;
    private static volatile String varsSignature;

    /**
     * The default values set by the client. This is not thread-safe so traversals should be
     * synchronized.
     */
    public static final Map<String, Object> valuesFromClient = new HashMap<>();

    private static final Map<String, String> defaultKinds = new HashMap<>();
    private static final String LEANPLUM = "__leanplum__";
    private static Map<String, Object> diffs = new HashMap<>();
    private static Map<String, Object> regions = new HashMap<>();
    private static Map<String, Object> messageDiffs = new HashMap<>();
    private static Map<String, Object> devModeValuesFromServer;
    private static Map<String, Object> devModeFileAttributesFromServer;
    private static volatile List<Map<String, Object>> variants = new ArrayList<>();
    private static volatile List<Map<String, Object>> localCaps = new ArrayList<>();
    private static CacheUpdateBlock updateBlock;
    private static boolean hasReceivedDiffs = false;
    private static Map<String, Object> messages = new HashMap<>();
    private static Object merged;
    private static boolean silent;
    private static int contentVersion;
    private static Map<String, Object> userAttributes;
    private static Map<String, Object> variantDebugInfo = new HashMap<>();

    private static final String NAME_COMPONENT_REGEX = "(?:[^\\.\\[.(\\\\]+|\\\\.)+";
    private static final Pattern NAME_COMPONENT_PATTERN = Pattern.compile(NAME_COMPONENT_REGEX);

    public static String[] getNameComponents(String name) {
        Matcher matcher = NAME_COMPONENT_PATTERN.matcher(name);
        List<String> components = new ArrayList<>();
        while (matcher.find()) {
            components.add(name.substring(matcher.start(), matcher.end()));
        }
        return components.toArray(new String[0]);
    }

    private static Object traverse(Object collection, Object key, boolean autoInsert) {
        if (collection == null) {
            return null;
        }
        if (collection instanceof Map) {
            Map<Object, Object> castedCollection = CollectionUtil.uncheckedCast(collection);
            Object result = castedCollection.get(key);
            if (autoInsert && result == null && key instanceof String) {
                result = new HashMap<String, Object>();
                castedCollection.put(key, result);
            }
            return result;
        } else if (collection instanceof List) {
            List<Object> castedList = CollectionUtil.uncheckedCast(collection);
            Object result = castedList.get((Integer) key);
            if (autoInsert && result == null) {
                result = new HashMap<String, Object>();
                castedList.set((Integer) key, result);
            }
            return result;
        }
        return null;
    }

    @FunctionalInterface
    public interface StreamProvider {
        InputStream openStream();
    }

    private static boolean isStreamAvailable(StreamProvider stream) {
        if (stream == null)
            return false;

        try {
            InputStream is = stream.openStream();
            if (is != null) {
                is.close();
                return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static void registerFile(
            String stringValue, StreamProvider defaultStream, String hash, int size) {

        if (!isStreamAvailable(defaultStream)
                || !Constants.isDevelopmentModeEnabled
                || Constants.isNoop()) {
            return;
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Constants.Keys.HASH, hash);
        attributes.put(Constants.Keys.SIZE, size);

        Map<String, Object> variationAttributes = new HashMap<>();
        variationAttributes.put("", attributes);

        fileStreams.put(stringValue, defaultStream);
        fileAttributes.put(stringValue, variationAttributes);
        maybeUploadNewFiles();
    }

    public static void registerFile(
            String stringValue, String defaultValue, StreamProvider defaultStream) {

        if (!isStreamAvailable(defaultStream)
                || !Constants.isDevelopmentModeEnabled
                || Constants.isNoop()) {
            return;
        }

        Map<String, Object> variationAttributes = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();

        if (LPClassesMock.isSimulator()) {
            HashResults result = FileManager.fileMD5HashCreateWithPath(defaultStream.openStream());
            if (result != null) {
                attributes.put(Constants.Keys.HASH, result.hash);
                attributes.put(Constants.Keys.SIZE, result.size);
            }
        } else {
            int size = FileManager.getFileSize(
                    FileManager.fileValue(stringValue, defaultValue, null));
            attributes.put(Constants.Keys.SIZE, size);
        }

        variationAttributes.put("", attributes);
        fileStreams.put(stringValue, defaultStream);
        fileAttributes.put(stringValue, variationAttributes);
        maybeUploadNewFiles();
    }

    public static void updateValues(String name, String[] nameComponents, Object value, String kind,
                                    Map<String, Object> values, Map<String, String> kinds) {
        Object valuesPtr = values;
        if (nameComponents != null && nameComponents.length > 0) {
            for (int i = 0; i < nameComponents.length - 1; i++) {
                valuesPtr = traverse(valuesPtr, nameComponents[i], true);
            }
            if (valuesPtr instanceof Map) {
                Map<String, Object> map = CollectionUtil.uncheckedCast(valuesPtr);
                map.put(nameComponents[nameComponents.length - 1], value);
            }
        }
        if (kinds != null) {
            kinds.put(name, kind);
        }
    }

    public static void registerVariable(Var<?> var) {
        vars.put(var.name(), var);
        synchronized (valuesFromClient) {
            updateValues(
                    var.name(), var.nameComponents(), var.defaultValue(),
                    var.kind(), valuesFromClient, defaultKinds);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Var<T> getVariable(String name) {
        return (Var<T>) vars.get(name);
    }

    private static void computeMergedDictionary() {
        synchronized (valuesFromClient) {
            merged = mergeHelper(valuesFromClient, diffs);
        }
    }

    public static Object mergeHelper(Object vars, Object diff) {
        if (diff == null) {
            return vars;
        }
        if (diff instanceof Number
                || diff instanceof Boolean
                || diff instanceof String
                || diff instanceof Character
                || vars instanceof Number
                || vars instanceof Boolean
                || vars instanceof String
                || vars instanceof Character) {
            return diff;
        }

        Iterable<?> diffKeys = (diff instanceof Map) ? ((Map<?, ?>) diff).keySet() : (Iterable<?>) diff;
        Iterable<?> varsKeys = (vars instanceof Map) ? ((Map<?, ?>) vars).keySet() : (Iterable<?>) vars;
        Map<?, ?> diffMap = (diff instanceof Map) ? ((Map<?, ?>) diff) : null;
        Map<?, ?> varsMap = (vars instanceof Map) ? ((Map<?, ?>) vars) : null;

        // Infer that the diffs is an array if the vars value doesn't exist to tell us the type.
        boolean isArray = false;
        if (vars == null) {
            if (diff instanceof Map && ((Map<?, ?>) diff).size() > 0) {
                isArray = true;
                for (Object var : diffKeys) {
                    if (!(var instanceof String)) {
                        isArray = false;
                        break;
                    }
                    String str = ((String) var);
                    if (str.length() < 3 || str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']') {
                        isArray = false;
                        break;
                    }
                    String varSubscript = str.substring(1, str.length() - 1);
                    if (!("" + Integer.getInteger(varSubscript)).equals(varSubscript)) {
                        isArray = false;
                        break;
                    }
                }
            }
        }

        // Merge arrays.
        if (vars instanceof List || isArray) {
            ArrayList<Object> merged = new ArrayList<>();
            for (Object var : varsKeys) {
                merged.add(var);
            }

            // Merge values from server
            // Array values from server come as Dictionary
            // Example:
            // string[] items = new string[] { "Item 1", "Item 2"};
            // args.With<string[]>("Items", items); // Action Context arg value
            // "vars": {
            //      "Items": {
            //                  "[1]": "Item 222", // Modified value from server
            //                  "[0]": "Item 111"  // Modified value from server
            //              }
            //  }
            // Prevent error when loading variable diffs where the diff is an Array and not Dictionary
            if (diffMap != null) {
                for (Object varSubscript : diffKeys) {
                    if (varSubscript instanceof String) {
                        String strSubscript = (String) varSubscript;
                        if (strSubscript.length() > 2 && strSubscript.startsWith("[") && strSubscript.endsWith("]")) {
                            // Get the index from the string key: "[0]" -> 0
                            int subscript = Integer.parseInt(strSubscript.substring(1, strSubscript.length() - 1));
                            Object var = diffMap.get(strSubscript);
                            while (subscript >= merged.size() && merged.size() < Short.MAX_VALUE) {
                                merged.add(null);
                            }
                            merged.set(subscript, mergeHelper(merged.get(subscript), var));
                        }
                    }
                }
            }
            return merged;
        }

        // Merge dictionaries.
        if (vars instanceof Map || diff instanceof Map) {
            HashMap<Object, Object> merged = new HashMap<>();
            if (varsKeys != null) {
                for (Object var : varsKeys) {
                    if (diffMap != null && varsMap != null) {
                        Object diffVar = diffMap.get(var);
                        Object value = varsMap.get(var);
                        if (diffVar == null && value != null) {
                            merged.put(var, value);
                        }
                    }
                }
            }
            for (Object var : diffKeys) {
                Object diffsValue = diffMap != null ? diffMap.get(var) : null;
                Object varsValue = varsMap != null ? varsMap.get(var) : null;
                Object mergedValues = mergeHelper(varsValue, diffsValue);
                merged.put(var, mergedValues);
            }
            return merged;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = traverse(mergedPtr, component, false);
        }
        return (T) mergedPtr;
    }

    public static <T> T getMergedValueFromComponentArray(Object[] components) {
        return getMergedValueFromComponentArray(components,
                merged != null ? merged : valuesFromClient);
    }

    public static Map<String, Object> getDiffs() {
        return diffs;
    }

    public static Map<String, Object> getMessageDiffs() {
        return messageDiffs;
    }

    public static Map<String, Object> regions() {
        return regions;
    }

    public static boolean hasReceivedDiffs() {
        return hasReceivedDiffs;
    }

    public static Map<String, Object> getVariantDebugInfo() {
        return variantDebugInfo;
    }

    public static void setVariantDebugInfo(Map<String, Object> variantDebugInfo) {
        if (variantDebugInfo != null) {
            VarCache.variantDebugInfo = variantDebugInfo;
        } else {
            VarCache.variantDebugInfo = new HashMap<>();
        }
    }

    public static void loadDiffs() {
        if (Constants.isNoop()) {
            return;
        }
        Context context = LPClassesMock.getContext();
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        if (APIConfig.getInstance().token() == null) {
            applyVariableDiffs(
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new HashMap<>(),
                    "",
                    "");
            return;
        }
        try {
            // Crypt functions return input text if there was a problem.
            AESCrypt aesContext = new AESCrypt(APIConfig.getInstance().appId(), APIConfig.getInstance().token());
            String variables = aesContext.decodePreference(
                    defaults, Constants.Defaults.VARIABLES_KEY, "{}");
            String messages = aesContext.decodePreference(
                    defaults, Constants.Defaults.MESSAGES_KEY, "{}");
            String regions = aesContext.decodePreference(defaults, Constants.Defaults.REGIONS_KEY, "{}");
            String variants = aesContext.decodePreference(defaults, Constants.Keys.VARIANTS, "[]");
            String localCaps = aesContext.decodePreference(defaults, Constants.Keys.LOCAL_CAPS, "[]");
            String variantDebugInfo = aesContext.decodePreference(defaults, Constants.Keys.VARIANT_DEBUG_INFO, "{}");
            String varsJson = aesContext.decodePreference(defaults, Constants.Defaults.VARIABLES_JSON_KEY, "{}");
            String varsSignature = aesContext.decodePreference(defaults, Constants.Defaults.VARIABLES_SIGN_KEY, null);
            applyVariableDiffs(
                    JsonConverter.fromJson(variables),
                    JsonConverter.fromJson(messages),
                    JsonConverter.fromJson(regions),
                    JsonConverter.listFromJson(new JSONArray(variants)),
                    JsonConverter.listFromJson(new JSONArray(localCaps)),
                    JsonConverter.fromJson(variantDebugInfo),
                    varsJson,
                    varsSignature);
            String deviceId = aesContext.decodePreference(defaults, Constants.Params.DEVICE_ID, null);
            if (deviceId != null) {
                APIConfig.getInstance().setDeviceId(deviceId);
            }
            String userId = aesContext.decodePreference(defaults, Constants.Params.USER_ID, null);
            if (userId != null) {
                APIConfig.getInstance().setUserId(userId);
            }
            String loggingEnabled = aesContext.decodePreference(defaults, Constants.Keys.LOGGING_ENABLED,
                    "false");
            if (Boolean.parseBoolean(loggingEnabled)) {
                Constants.loggingEnabled = true;
            }
        } catch (Exception e) {
            Log.e(TAG,"Could not load variable diffs.\n" + Log.getStackTraceString(e));
        }
        userAttributes();
    }

    public static void saveDiffs() {
        if (Constants.isNoop()) {
            return;
        }
        if (APIConfig.getInstance().token() == null) {
            return;
        }
        Context context = LPClassesMock.getContext();
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = defaults.edit();

        // Crypt functions return input text if there was a problem.
        AESCrypt aesContext = new AESCrypt(APIConfig.getInstance().appId(), APIConfig.getInstance().token());

        String variablesCipher = aesContext.encrypt(JsonConverter.toJson(diffs));
        editor.putString(Constants.Defaults.VARIABLES_KEY, variablesCipher);

        String messagesCipher = aesContext.encrypt(JsonConverter.toJson(messages));
        editor.putString(Constants.Defaults.MESSAGES_KEY, messagesCipher);

        String regionsCipher = aesContext.encrypt(JsonConverter.toJson(regions));
        editor.putString(Constants.Defaults.REGIONS_KEY, regionsCipher);

        try {
            if (variants != null && !variants.isEmpty()) {
                String variantsJson = JsonConverter.listToJsonArray(variants).toString();
                editor.putString(Constants.Keys.VARIANTS, aesContext.encrypt(variantsJson));
            }
        } catch (JSONException e1) {
            Log.e(TAG,"Error converting " + variants + " to JSON.\n" + Log.getStackTraceString(e1));
        }

        try {
            if (localCaps != null) {
                String json = JsonConverter.listToJsonArray(localCaps).toString();
                editor.putString(Constants.Keys.LOCAL_CAPS, aesContext.encrypt(json));
            }
        } catch (JSONException e) {
            Log.e(TAG,"Error converting " + localCaps + " to JSON.\n" + Log.getStackTraceString(e));
        }

        if (variantDebugInfo != null) {
            editor.putString(
                    Constants.Keys.VARIANT_DEBUG_INFO,
                    aesContext.encrypt(JsonConverter.toJson(variantDebugInfo)));
        }

        editor.putString(Constants.Defaults.VARIABLES_JSON_KEY, aesContext.encrypt(varsJson));
        editor.putString(Constants.Defaults.VARIABLES_SIGN_KEY, aesContext.encrypt(varsSignature));

        editor.putString(Constants.Params.DEVICE_ID, aesContext.encrypt(APIConfig.getInstance().deviceId()));
        editor.putString(Constants.Params.USER_ID, aesContext.encrypt(APIConfig.getInstance().userId()));
        editor.putString(Constants.Keys.LOGGING_ENABLED,
                aesContext.encrypt(String.valueOf(Constants.loggingEnabled)));
        SharedPreferencesUtil.commitChanges(editor);
    }

    /**
     * Convert a resId to a resPath.
     */
    public static int getResIdFromPath(String resPath) {
        int resId = 0;
        try {
            String path = resPath.replace("res/", "");
            path = path.substring(0, path.lastIndexOf('.'));  // remove file extension
            String name = path.substring(path.lastIndexOf('/') + 1);
            String type = path.substring(0, path.lastIndexOf('/'));
            type = type.replace('/', '.');
            Context context = LPClassesMock.getContext();
            resId = context.getResources().getIdentifier(name, type, context.getPackageName());
        } catch (Exception e) {
            // Fall back to 0 on any exception
        }
        return resId;
    }

    /**
     * Update file variables stream info with override info, so that override files don't require
     * downloads if they're already available.
     */
    private static void fileVariableFinish() {
        for (String name : new HashMap<>(vars).keySet()) {
            Var<?> var = vars.get(name);
            if (var == null) {
                continue;
            }
            String overrideFile = var.stringValue;
            if (var.isResource && Constants.Kinds.FILE.equals(var.kind()) && overrideFile != null &&
                    !overrideFile.equals(var.defaultValue())) {
                Map<String, Object> variationAttributes = CollectionUtil.uncheckedCast(fileAttributes.get
                        (overrideFile));
                StreamProvider streamProvider = fileStreams.get(overrideFile);
                if (variationAttributes != null && streamProvider != null) {
                    var.setOverrideResId(getResIdFromPath(var.stringValue()));
                }
            }
        }
    }

    public static void applyVariableDiffs(
            Map<String, Object> diffs,
            Map<String, Object> messages,
            Map<String, Object> regions,
            List<Map<String, Object>> variants,
            List<Map<String, Object>> localCaps,
            Map<String, Object> variantDebugInfo,
            String varsJson,
            String varsSignature) {
        if (diffs != null) {
            VarCache.diffs = diffs;
            computeMergedDictionary();

            // Update variables with new values.
            // Have to copy the dictionary because a dictionary variable may add a new sub-variable,
            // modifying the variable dictionary.
            for (String name : new HashMap<>(vars).keySet()) {
                Var<?> var = vars.get(name);
                if (var != null) {
                    var.update();
                }
            }
            fileVariableFinish();
        }

        if (messages != null) {
            // Store messages.
            messageDiffs = messages;
            Map<String, Object> newMessages = new HashMap<>();
            for (Map.Entry<String, Object> entry : messages.entrySet()) {
                Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(entry.getValue());
                Map<String, Object> newConfig = new HashMap<>(messageConfig);
                Map<String, Object> actionArgs = CollectionUtil.uncheckedCast(messageConfig.get(Constants
                        .Keys.VARS));
                Map<String, Object> actionDefinitions = LPClassesMock.getActionDefinitionMaps();
                Map<String, Object> defaultArgs = LPClassesMock.multiIndex(actionDefinitions,
                        newConfig.get(Constants.Params.ACTION), "values");
                Map<String, Object> vars = CollectionUtil.uncheckedCast(mergeHelper(defaultArgs,
                        actionArgs));
                newMessages.put(entry.getKey(), newConfig);
                newConfig.put(Constants.Keys.VARS, vars);
            }

            VarCache.messages = newMessages;
            for (Map.Entry<String, Object> entry : VarCache.messages.entrySet()) {
                String name = entry.getKey();
                Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(VarCache.messages.get
                        (name));
                if (messageConfig != null && messageConfig.get("action") != null) {
                    Map<String, Object> actionArgs = CollectionUtil.uncheckedCast(messageConfig.get(Constants.Keys.VARS));
                    //new ActionContext(messageConfig.get("action").toString(), actionArgs, name).update(); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
                }
            }
        }

        if (regions != null) {
            VarCache.regions = regions;
        }

        if (messages != null || regions != null) {
            Set<String> foregroundRegionNames = new HashSet<>();
            Set<String> backgroundRegionNames = new HashSet<>();
            //ActionManager.getForegroundandBackgroundRegionNames(foregroundRegionNames, backgroundRegionNames); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
            //LocationManager locationManager = ActionManager.getLocationManager(); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
            LocationManager locationManager = null;
            if (locationManager != null) {
                locationManager.setRegionsData(regions, foregroundRegionNames, backgroundRegionNames);
            }
        }

        if (variants != null) {
            VarCache.variants = variants;
        }

        if (localCaps != null) {
            VarCache.localCaps = localCaps;
        }

        if (variantDebugInfo != null) {
            VarCache.setVariantDebugInfo(variantDebugInfo);
        }

        if (varsJson != null) {
            VarCache.varsJson = varsJson;
            VarCache.varsSignature = varsSignature;
        }

        contentVersion++;

        if (!silent) {
            saveDiffs();
            triggerHasReceivedDiffs();
        }
    }

    public static int contentVersion() {
        return contentVersion;
    }

    private static void triggerHasReceivedDiffs() {
        hasReceivedDiffs = true;
        if (updateBlock != null) {
            updateBlock.updateCache();
        }
    }

    static boolean sendVariablesIfChanged() {
        return sendContentIfChanged(true, false);
    }

    static boolean sendActionsIfChanged() {
        return sendContentIfChanged(false, true);
    }

    public static boolean sendContentIfChanged(boolean variables, boolean actions) {
        boolean changed = false;
        if (variables && devModeValuesFromServer != null
                && !valuesFromClient.equals(devModeValuesFromServer)) {
            changed = true;
        }
        Map<String, Object> actionDefinitions = LPClassesMock.getActionDefinitionMaps();

        //boolean areLocalAndServerDefinitionsEqual = ActionManagerDefinitionKt.areLocalAndServerDefinitionsEqual(ActionManager.getInstance()); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
        boolean areLocalAndServerDefinitionsEqual = false;

        if (actions && !areLocalAndServerDefinitionsEqual) {
            changed = true;
        }

        if (changed) {
            HashMap<String, Object> params = new HashMap<>();
            if (variables) {
                params.put(Constants.Params.VARS, JsonConverter.toJson(valuesFromClient));
                params.put(Constants.Params.KINDS, JsonConverter.toJson(defaultKinds));
            }
            if (actions) {
                params.put(Constants.Params.ACTION_DEFINITIONS, JsonConverter.toJson(actionDefinitions));
            }
            params.put(Constants.Params.FILE_ATTRIBUTES, JsonConverter.toJson(fileAttributes));
            //Request request = RequestBuilder.withSetVarsAction().andParams(params).andType(RequestType.IMMEDIATE).create(); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
            //RequestSender.getInstance().send(request); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
        }

        return changed;
    }

    static void maybeUploadNewFiles() {
        // First check to make sure we have all the data we need
        if (Constants.isNoop()
                || devModeFileAttributesFromServer == null
                || !LPClassesMock.hasStartedAndRegisteredAsDeveloper()
                || !Constants.enableFileUploadingInDevelopmentMode) {
            return;
        }

        List<String> filenames = new ArrayList<>();
        List<JSONObject> fileData = new ArrayList<>();
        List<InputStream> streams = new ArrayList<>();
        int totalSize = 0;
        for (Map.Entry<String, Object> entry : fileAttributes.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> variationAttributes = CollectionUtil.uncheckedCast(entry.getValue());
            Map<String, Object> serverVariationAttributes =
                    CollectionUtil.uncheckedCast(devModeFileAttributesFromServer.get(name));
            Map<String, Object> localAttributes = CollectionUtil.uncheckedCast(variationAttributes.get
                    (""));
            Map<String, Object> serverAttributes = CollectionUtil.uncheckedCast(
                    (serverVariationAttributes != null ? serverVariationAttributes.get("") : null));
            if (FileManager.isNewerLocally(localAttributes, serverAttributes)) {
                Log.d(TAG,"Will upload file " + name + ". Local attributes: " +
                        localAttributes + "; server attributes: " + serverAttributes);

                String hash = (String) localAttributes.get(Constants.Keys.HASH);
                if (hash == null) {
                    hash = "";
                }

                String variationPath = FileManager.fileRelativeToAppBundle(name);

                // Upload in batch if we can't put any more files in
                if ((totalSize > Constants.Files.MAX_UPLOAD_BATCH_SIZES && filenames.size() > 0) || filenames.size() >= Constants.Files.MAX_UPLOAD_BATCH_FILES) {

                    LPClassesMock.sendFilesNow(fileData, filenames, streams);

                    filenames = new ArrayList<>();
                    fileData = new ArrayList<>();
                    streams = new ArrayList<>();
                    totalSize = 0;
                }

                // Add the current file to the lists and update size
                Object size = localAttributes.get(Constants.Keys.SIZE);
                totalSize += (Integer) size;
                filenames.add(variationPath);
                JSONObject fileDatum = new JSONObject();
                try {
                    fileDatum.put(Constants.Keys.HASH, hash);
                    fileDatum.put(Constants.Keys.SIZE, localAttributes.get(Constants.Keys.SIZE) + "");
                    fileDatum.put(Constants.Keys.FILENAME, name);
                    fileData.add(fileDatum);
                } catch (JSONException e) {
                    // HASH, SIZE, or FILENAME are null, which they never should be (they're constants).
                    Log.e(TAG,"Unable to upload files.\n" + Log.getStackTraceString(e));
                    fileData.add(new JSONObject());
                }
                InputStream is = null;
                StreamProvider streamProvider = fileStreams.get(name);
                if (streamProvider != null) {
                    is = streamProvider.openStream();
                }
                streams.add(is);
            }
        }

        if (filenames.size() > 0) {
            LPClassesMock.sendFilesNow(fileData, filenames, streams);
        }
    }

    /**
     * Sets whether values should be saved and callbacks triggered when the variable values get
     * updated.
     */
    public static void setSilent(boolean silent) {
        VarCache.silent = silent;
    }

    public static boolean silent() {
        return silent;
    }

    public static void setDevModeValuesFromServer(Map<String, Object> values, Map<String, Object> fileAttributes, Map<String, Object> actionDefinitions) {
        devModeValuesFromServer = values;
        //ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(ActionManager.getInstance(), actionDefinitions); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
        devModeFileAttributesFromServer = fileAttributes;
    }

    public static void onUpdate(CacheUpdateBlock block) {
        updateBlock = block;
    }

    public static List<Map<String, Object>> variants() {
        return variants;
    }

    public static List<Map<String, Object>> localCaps() {
        return localCaps;
    }

    public static Map<String, Object> messages() {
        return messages;
    }

    public static <T> String kindFromValue(T defaultValue) {
        String kind = null;
        if (defaultValue instanceof Integer
                || defaultValue instanceof Long
                || defaultValue instanceof Short
                || defaultValue instanceof Character
                || defaultValue instanceof Byte
                || defaultValue instanceof BigInteger) {
            kind = Constants.Kinds.INT;
        } else if (defaultValue instanceof Float
                || defaultValue instanceof Double
                || defaultValue instanceof BigDecimal) {
            kind = Constants.Kinds.FLOAT;
        } else if (defaultValue instanceof String) {
            kind = Constants.Kinds.STRING;
        } else if (defaultValue instanceof List
                || defaultValue instanceof Array) {
            kind = Constants.Kinds.ARRAY;
        } else if (defaultValue instanceof Map) {
            kind = Constants.Kinds.DICTIONARY;
        } else if (defaultValue instanceof Boolean) {
            kind = Constants.Kinds.BOOLEAN;
        }
        return kind;
    }

    static Map<String, Object> userAttributes() {
        if (userAttributes == null) {
            Context context = LPClassesMock.getContext();
            SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
            AESCrypt aesContext = new AESCrypt(APIConfig.getInstance().appId(), APIConfig.getInstance().token());
            try {
                userAttributes = JsonConverter.fromJson(
                        aesContext.decodePreference(defaults, Constants.Defaults.ATTRIBUTES_KEY, "{}"));
            } catch (Exception e) {
                Log.e(TAG,"Could not load user attributes.\n" + Log.getStackTraceString(e));
                userAttributes = new HashMap<>();
            }
        }
        return userAttributes;
    }

    public static void saveUserAttributes() {
        if (Constants.isNoop() || APIConfig.getInstance().appId() == null || userAttributes == null) {
            return;
        }
        Context context = LPClassesMock.getContext();
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = defaults.edit();
        // Crypt functions return input text if there was a problem.
        String plaintext = JsonConverter.toJson(userAttributes);
        AESCrypt aesContext = new AESCrypt(APIConfig.getInstance().appId(), APIConfig.getInstance().token());
        editor.putString(Constants.Defaults.ATTRIBUTES_KEY, aesContext.encrypt(plaintext));
        SharedPreferencesUtil.commitChanges(editor);
    }

    /*
    @Nullable
    public static SecuredVars getSecuredVars() {
        if (TextUtils.isEmpty(varsJson) || TextUtils.isEmpty(varsSignature)) {
            return null;
        }
        return new SecuredVars(varsJson, varsSignature);
    }
    */

    public static void clearUserContent() {
        vars.clear();
        variants = new ArrayList<>();
        localCaps = new ArrayList<>();
        variantDebugInfo.clear();
        varsJson = null;
        varsSignature = null;

        diffs.clear();
        messageDiffs.clear();
        messages = null;
        userAttributes = null;
        merged = null;

        devModeValuesFromServer = null;
        devModeFileAttributesFromServer = null;
        //ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(ActionManager.getInstance(), null); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
    }

    /**
     * Resets the VarCache to stock state.
     */
    public static void reset() {
        vars.clear();
        variantDebugInfo.clear();
        fileAttributes.clear();
        fileStreams.clear();
        valuesFromClient.clear();
        defaultKinds.clear();
        //ActionManager.getInstance().getDefinitions().clear(); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
        diffs.clear();
        messageDiffs.clear();
        regions.clear();
        devModeValuesFromServer = null;
        devModeFileAttributesFromServer = null;
        //ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(ActionManager.getInstance(), null); // todo : check if its uncommented in LP. if yes, then do something about it. this code might be needed for functioning of feature!
        variants = new ArrayList<>();
        localCaps = new ArrayList<>();
        updateBlock = null;
        hasReceivedDiffs = false;
        messages = null;
        merged = null;
        silent = false;
        contentVersion = 0;
        userAttributes = null;
        varsJson = null;
        varsSignature = null;
    }
}
