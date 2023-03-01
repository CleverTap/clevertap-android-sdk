package com.clevertap.demo;

import com.clevertap.android.sdk.variables.annotations.Variable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMyVars {
    @Variable public static String welcomeMsg = "Hello User";
    @Variable public static boolean isOptedForOffers = true;
    @Variable public static int initialCoins = 45;
    @Variable public static float correctGuessPercentage = 50.0F;
//    @Variable public static List<String> aiNames = new ArrayList<>(Arrays.asList("don2", "jason2", "shiela2"));

    @Variable public static Map<String, Object> userConfigurableProps = new HashMap<>();

    static {
        userConfigurableProps.put("numberOfGuesses", 10);
        userConfigurableProps.put("difficultyLevel", 1.8F);
        userConfigurableProps.put("ai_Gender", "F");
    }


    @Variable(group = "android.samsung", name = "s22") public static double samsungS22 = 54999.99;
    @Variable(group = "android.samsung", name = "s23") public static String samsungS23 = "UnReleased";
    @Variable(group = "android.nokia", name = "6a") public static int nokia6a = 6400;
    @Variable(group = "android.nokia", name = "12") public static String nokia12 = "UnReleased";
    @Variable(group = "apple", name = "iphone15") public static String appleI15 = "UnReleased";


    public static JSONObject getUpdatedJSon(){
        welcomeMsg = "whyuser";
        initialCoins = 42;
        correctGuessPercentage = 21.4f;
        JSONObject j = FakeServer.Companion.getJson(3);
        return j;


    }

}
