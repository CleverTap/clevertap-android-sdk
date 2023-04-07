package com.clevertap.android.sdk.variables;

import android.text.Editable;
import com.clevertap.android.sdk.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {

  @SuppressWarnings({"unchecked"})
  public static <T> T uncheckedCast(Object obj) {
    return (T) obj;
  }

  public static Map<String, Object> fromJson(String json) {
    if (json == null) {
      return null;
    }
    try {
      return mapFromJson(new JSONObject(json));
    } catch (JSONException e) {
      Logger.v("Error converting " + json + " from JSON", e);
      return null;
    }
  }

  private static JSONArray listToJsonArray(Iterable<?> list) throws JSONException {
    if (list == null) {
      return null;
    }
    JSONArray obj = new JSONArray();
    for (Object value : list) {
      if (value instanceof Map) {
        Map<String, ?> mappedValue = uncheckedCast(value);
        value = mapToJsonObject(mappedValue);
      } else if (value instanceof Iterable) {
        value = listToJsonArray((Iterable<?>) value);
      } else if (value == null) {
        value = JSONObject.NULL;
      }
      obj.put(value);
    }
    return obj;
  }

  public static <T> Map<String, T> mapFromJson(JSONObject object) {
    if (object == null) {
      return null;
    }
    Map<String, T> result = new HashMap<>();
    Iterator<?> keysIterator = object.keys();
    while (keysIterator.hasNext()) {
      String key = (String) keysIterator.next();
      Object value = object.opt(key);
      if (value == null || value == JSONObject.NULL) {
        value = null;
      } else if (value instanceof JSONObject) {
        value = mapFromJson((JSONObject) value);
      } else if (value instanceof JSONArray) {
        value = listFromJson((JSONArray) value);
      } else if (JSONObject.NULL.equals(value)) {
        value = null;
      }
      T castedValue = uncheckedCast(value);
      result.put(key, castedValue);
    }
    return result;
  }

  private static <T> List<T> listFromJson(JSONArray json) {
    if (json == null) {
      return null;
    }
    List<Object> result = new ArrayList<>(json.length());
    for (int i = 0; i < json.length(); i++) {
      Object value = json.opt(i);
      if (value == null || value == JSONObject.NULL) {
        value = null;
      } else if (value instanceof JSONObject) {
        value = mapFromJson((JSONObject) value);
      } else if (value instanceof JSONArray) {
        value = listFromJson((JSONArray) value);
      } else if (JSONObject.NULL.equals(value)) {
        value = null;
      }
      result.add(value);
    }
    return uncheckedCast(result);
  }

  private static JSONObject mapToJsonObject(Map<String, ?> map) throws JSONException {
    if (map == null) {
      return null;
    }
    JSONObject obj = new JSONObject();
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Map) {
        Map<String, ?> mappedValue = uncheckedCast(value);
        value = mapToJsonObject(mappedValue);
      } else if (value instanceof Iterable) {
        value = listToJsonArray((Iterable<?>) value);
      } else if (value instanceof Editable) {
        value = value.toString();
      } else if (value == null) {
        value = JSONObject.NULL;
      }
      obj.put(key, value);
    }
    return obj;
  }

  public static String toJson(Map<String, ?> map) {
    if (map == null) {
      return null;
    }
    try {
      return mapToJsonObject(map).toString();
    } catch (JSONException e) {
      Logger.v("Error converting " + map + " to JSON", e);
      return null;
    }
  }
}
