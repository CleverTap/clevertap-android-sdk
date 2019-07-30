package com.clevertap.android.sdk.ab_testing;

import android.support.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashMap;
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

        switch (type){
            case CTVarTypeBool:
                _stringValue = _value.toString();
                break;
            case CTVarTypeDouble:
                _numberValue = Double.valueOf(_value.toString());
                break;
            case CTVarTypeInteger:
                _numberValue = Double.valueOf(_value.toString());
                break;
            case CTVarTypeString:
                _stringValue = _value.toString();
                break;
            case CTVarTypeListOfBool:
            case CTVarTypeListOfDouble:
            case CTVarTypeListOfInteger:
            case CTVarTypeListOfString:
                _listValue = validateValueArray(_value);
                break;
            case CTVarTypeMapOfBool:
            case CTVarTypeMapOfDouble:
            case CTVarTypeMapOfInteger:
            case CTVarTypeMapOfString:
                _mapValue = validateValueMap(_value);
                break;
            case CTVarTypeUnknown:
                break;
        }
    }

    private Map<?,?> validateValueMap(Object _value){
        String stringValue = _value.toString();
        stringValue = stringValue.replace("\"","");
        stringValue = stringValue.replace("{","");
        stringValue = stringValue.replace("}","");

        Object[] objectArray = stringValue.split(",");

        Map<Object,Object> objectMap = new HashMap<>();

        for(Object o : objectArray){
            Object[] objectValuesArray = o.toString().split(":");
            switch (type) {
                case CTVarTypeListOfString:
                    if (!(objectValuesArray[1] instanceof String)) {
                        Logger.d("Failed to parse the array value, invalid value provided : " + o.toString());
                        return null;
                    }
                    break;
                case CTVarTypeListOfDouble:
                case CTVarTypeListOfInteger:
                    if (!(objectValuesArray[1] instanceof Number)) {
                        Logger.d("Failed to parse the array value, invalid value provided : " + o.toString());
                        return null;
                    }
                    break;
                case CTVarTypeListOfBool:
                    if(!(objectValuesArray[1] instanceof Boolean)){
                        Logger.d("Failed to parse the array value, invalid value provided : "+o.toString());
                        return null;
                    }
                    break;
            }
            objectMap.put(objectValuesArray[0],objectValuesArray[1]);
        }

        return objectMap;

    }

    private List<?> validateValueArray(Object _value){
        //TODO Example - If integerArray is passed from dashboard _value comes as [1,2,3].
        //TODO to remove the solid brackets I have to convert to String but then validation fails
        //TODO need to send array Vars in a better format. This happens for all data types except String
        String stringValue = _value.toString();
        stringValue = stringValue.replace("[","");
        stringValue = stringValue.replace("]","");
        Object[] objectArray = stringValue.split(",");

        for (Object o : objectArray) {
            switch (type) {
                case CTVarTypeListOfString:
                    if (!(o instanceof String)) {
                        Logger.d("Failed to parse the array value, invalid value provided : " + o.toString());
                        return null;
                    }
                    break;
                case CTVarTypeListOfDouble:
                case CTVarTypeListOfInteger:
                    if (!(o instanceof Number)) {
                        Logger.d("Failed to parse the array value, invalid value provided : " + o.toString());
                        return null;
                    }
                    break;
                case CTVarTypeListOfBool:
                    if(!(o instanceof Boolean)){
                        Logger.d("Failed to parse the array value, invalid value provided : "+o.toString());
                        return null;
                    }
                    break;
            }
        }
        return Arrays.asList(objectArray);

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
