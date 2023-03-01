package com.clevertap.demo;

import com.clevertap.android.sdk.variables.annotations.Variable;

import java.util.HashMap;
import java.util.Map;


public class TestMyVarsJava {
    @Variable public static String welcomeMsg = "Hello User";
    @Variable public static boolean isOptedForOffers = true;
    @Variable public static int initialCoins = 45;
    @Variable public static float correctGuessPercentage = 50.0F;
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




}


