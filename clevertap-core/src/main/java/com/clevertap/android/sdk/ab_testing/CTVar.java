package com.clevertap.android.sdk.ab_testing;

import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

final class CTVar {

    enum CTVarType {
        CTVarTypeBool("bool"),
        CTVarTypeDouble("double"),
        CTVarTypeInteger("integer"),
        CTVarTypeString("string"),
        CTVarTypeListOfBool("arrayofbool"),
        CTVarTypeListOfDouble("arrayofdouble"),
        CTVarTypeListOfInteger("arrayofinteger"),
        CTVarTypeListOfString("arrayofstring"),
        CTVarTypeMapOfBool("dictionaryofbool"),
        CTVarTypeMapOfDouble("dictionaryofdouble"),
        CTVarTypeMapOfInteger("dictionaryofinteger"),
        CTVarTypeMapOfString("dictionaryofstring"),
        CTVarTypeUnknown("unknown");

        private final String varType;

        CTVarType(String type) {
            this.varType = type;
        }

        @NonNull
        @Override
        public String toString() {
            return varType;
        }

        @SuppressWarnings({"unused"})
        static CTVarType fromString(String type) {
            switch (type) {
                case "bool": {
                    return CTVarTypeBool;
                }
                case "double": {
                    return CTVarTypeDouble;
                }
                case "integer": {
                    return CTVarTypeInteger;
                }
                case "string": {
                    return CTVarTypeString;
                }
                case "arrayofbool": {
                    return CTVarTypeListOfBool;
                }
                case "arrayofdouble": {
                    return CTVarTypeListOfDouble;
                }
                case "arrayofinteger": {
                    return CTVarTypeListOfInteger;
                }
                case "arrayofstring": {
                    return CTVarTypeListOfString;
                }
                case "dictionaryofbool": {
                    return CTVarTypeMapOfBool;
                }
                case "dictionaryofdouble": {
                    return CTVarTypeMapOfDouble;
                }
                case "dictionaryofinteger": {
                    return CTVarTypeMapOfInteger;
                }
                case "dictionaryofstring": {
                    return CTVarTypeMapOfString;
                }
                default:
                    return CTVarTypeUnknown;
            }
        }

    }

    private List<?> _listValue;

    private Map<?, ?> _mapValue;

    private Double _numberValue;

    private String _stringValue;

    private Object _value;

    private String name;

    private CTVarType type;

    CTVar(String name, CTVarType type, Object value) {
        this.name = name;
        this.type = type;
        this._value = value;
        _computeValue();
    }

    Boolean booleanValue() {
        if (_stringValue == null) {
            return null;
        }
        try {
            return Boolean.valueOf(_stringValue);
        } catch (Throwable t) {
            return null;
        }
    }

    void clearValue() {
        this._value = null;
        _computeValue();
    }

    Double doubleValue() {
        return _numberValue;
    }

    String getName() {
        return name;
    }

    CTVarType getType() {
        return type;
    }

    Integer integerValue() {
        if (_numberValue == null) {
            return null;
        }
        try {
            return _numberValue.intValue();
        } catch (Throwable t) {
            return null;
        }
    }

    List<?> listValue() {
        return _listValue;
    }

    Map<?, ?> mapValue() {
        return _mapValue;
    }

    String stringValue() {
        return _stringValue;
    }

    JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("type", type.toString());
        } catch (Throwable t) {
            // no-op
        }
        return json;
    }

    @SuppressWarnings("unused")
    void update(CTVarType type, Object value) {
        this.type = type;
        this._value = value;
        _computeValue();
    }

    private void _computeValue() {
        _stringValue = null;
        _numberValue = null;
        _listValue = null;
        _mapValue = null;
        if (_value == null) {
            return;
        }

        if (_value instanceof String) {
            _stringValue = (String) _value;
            try {
                _numberValue = Double.valueOf(_stringValue);
            } catch (Throwable t) {
                // no-op
            }
        } else if (_value instanceof Number) {
            _stringValue = "" + _value;
            _numberValue = ((Number) _value).doubleValue();
        } else {
            try {
                _stringValue = _value.toString();
            } catch (Throwable t) {
                Logger.d("Error parsing var", t);
                return;
            }
        }

        switch (type) {
            case CTVarTypeListOfBool:
            case CTVarTypeListOfDouble:
            case CTVarTypeListOfInteger:
            case CTVarTypeListOfString:
                _listValue = listFromString(_stringValue, type);
                break;
            case CTVarTypeMapOfBool:
            case CTVarTypeMapOfDouble:
            case CTVarTypeMapOfInteger:
            case CTVarTypeMapOfString:
                _mapValue = mapFromString(_stringValue, type);
                break;
        }
    }

    private List<?> listFromString(String stringValue, CTVarType type) {
        try {
            String[] stringArray = stringValue.replace("[", "")
                    .replace("]", "").replace("\"", "")
                    .split(","); // ["value", "value"...]

            if (type == CTVarType.CTVarTypeListOfString) {
                return Arrays.asList(stringArray);
            }

            ArrayList<Object> parsed = new ArrayList<>();

            for (String s : stringArray) {
                switch (type) {
                    case CTVarTypeListOfBool:
                        parsed.add(Boolean.valueOf(s));
                        break;
                    case CTVarTypeListOfDouble:
                        parsed.add(Double.valueOf(s));
                        break;
                    case CTVarTypeListOfInteger:
                        parsed.add(Integer.valueOf(s));
                        break;
                }
            }
            return parsed;
        } catch (Throwable t) {
            Logger.d("Unable to parse list of type: " + type.toString() + " from : " + stringValue);
            return null;
        }
    }

    private Map<String, ?> mapFromString(String stringValue, CTVarType type) {
        try {
            String[] stringArray = stringValue.replace("\"", "")
                    .replace("{", "")
                    .replace("}", "")
                    .split(",");  // ["key:value", "key:value"...]

            Map<String, Object> objectMap = new HashMap<>();

            for (String s : stringArray) {
                String[] stringValuesArray = s.split(":");
                String key = stringValuesArray[0];
                String _stringValue = stringValuesArray[1];
                Object value = null;
                switch (type) {
                    case CTVarTypeMapOfBool:
                        value = Boolean.valueOf(_stringValue);
                        break;
                    case CTVarTypeMapOfDouble:
                        value = Double.valueOf(_stringValue);
                        break;
                    case CTVarTypeMapOfInteger:
                        value = Integer.valueOf(_stringValue);
                        break;
                    case CTVarTypeMapOfString:
                        value = _stringValue;
                        break;
                }
                if (value != null) {
                    objectMap.put(key, value);
                }
            }
            return objectMap;
        } catch (Throwable t) {
            Logger.d("Unable to parse map of type: " + type.toString() + " from : " + stringValue);
            return null;
        }
    }
}
