package com.clevertap.android.sdk.ab_testing;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

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

        @SuppressWarnings({"unused"})
        static CTVarType fromString(String type) {
            switch(type){
                case "bool" : {
                    return CTVarTypeBool;
                }
                case "double" : {
                    return CTVarTypeDouble;
                }
                case "integer" : {
                    return CTVarTypeInteger;
                }
                case "string" : {
                    return CTVarTypeString;
                }
                case "arrayofbool" : {
                    return CTVarTypeListOfBool;
                }
                case "arrayofdouble" : {
                    return CTVarTypeListOfDouble;
                }
                case "arrayofinteger" : {
                    return CTVarTypeListOfInteger;
                }
                case "arrayofstring" : {
                    return CTVarTypeListOfString;
                }
                case "dictionaryofbool" : {
                    return CTVarTypeMapOfBool;
                }
                case "dictionaryofdouble" : {
                    return CTVarTypeMapOfDouble;
                }
                case "dictionaryofinteger" : {
                    return CTVarTypeMapOfInteger;
                }
                case "dictionaryofstring" : {
                    return CTVarTypeMapOfString;
                }
                default:
                    return CTVarTypeUnknown;
            }
        }

        @NonNull
        @Override
        public String toString() {
            return varType;
        }

    }

    private String name;
    private CTVarType type;
    private Object _value;
    private String _stringValue;
    private Double _numberValue;
    private List<?> _listValue;
    private Map<?, ?> _mapValue;

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
            } catch (NumberFormatException e) {
                _numberValue = null;
            }
        } else if (_value instanceof Number) {
            _stringValue = "" + _value;
            _numberValue = ((Number) _value).doubleValue();
        } else if (_value instanceof List<?>) {
            _listValue = (List<?>) _value;

        } else if (_value instanceof Map<?, ?>) {
            _mapValue = (Map<?, ?>) _value;
        }
    }

    CTVar(String name, CTVarType type, Object value) {
        this.name = name;
        this.type = type;
        this._value = value;
        _computeValue();
    }

    @SuppressWarnings("unused")
    void update(CTVarType type, Object value) {
        this.type = type;
        this._value = value;
        _computeValue();
    }

    void clearValue() {
        this._value = null;
        _computeValue();
    }

    String getName() {
        return name;
    }

    CTVarType getType() {
        return type;
    }

    Boolean booleanValue() {
        if (_stringValue == null) {
            return null;
        }
        return Boolean.valueOf(_stringValue);
    }

    Integer integerValue() {
        if (_numberValue == null) {
            return null;
        }
        return _numberValue.intValue();
    }

    Double doubleValue() {
        return _numberValue;
    }

    String stringValue() {
        return _stringValue;
    }

    List<?> listValue() {
        return _listValue;
    }

    Map<?, ?> mapValue() {
        return _mapValue;
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
}
