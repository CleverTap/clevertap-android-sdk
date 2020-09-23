package com.clevertap.android.sdk.ab_testing;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class CTVarCache {

    private final Map<String, CTVar> vars = new ConcurrentHashMap<>();

    void registerVar(String name, CTVar.CTVarType type, Object value) {
        CTVar var = getVar(name);
        if (var == null) {
            vars.put(name, new CTVar(name, type, value));
        } else if (value != null) { // only overwrite if we have a new value, to explicitly clear the value use clearVar
            var.update(type, value);
        }
    }

    CTVar getVar(String name) {
        return vars.get(name);
    }

    @SuppressWarnings({"WeakerAccess"})
    void clearVar(String name) {
        CTVar var = getVar(name);
        if (var != null) {
            var.clearValue();
        }
    }

    @SuppressWarnings("unused")
    void reset() {
        for (String name : new HashMap<>(vars).keySet()) {
            clearVar(name);
        }
    }

    JSONArray serializeVars() {
        JSONArray serialized = new JSONArray();
        for (String name : new HashMap<>(vars).keySet()) {
            CTVar var = vars.get(name);
            if (var != null) {
                serialized.put(var.toJSON());
            }
        }
        return serialized;
    }
}
