package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class JsonUtil {

    public static JSONArray toJsonArray(@NonNull List<?> list) {
        JSONArray array = new JSONArray();
        for (Object item : list) {
            if (item != null) {
                array.put(item);
            }
        }
        return array;
    }

    public static ArrayList<?> toList(@NonNull JSONArray array) {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }


    public static <T> Object[] toArray(@NonNull JSONArray jsonArray) {
        Object[] array = new Object[jsonArray.length()];
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                array[i] = jsonArray.get(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array;
    }
}