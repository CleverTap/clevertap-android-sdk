package com.clevertap.android.sdk.variables;

import com.clevertap.android.sdk.variables.annotations.Variable;

public class VariableDefinitions {
    @Variable public static String welcomeMsg = "Hello User";
    @Variable public static boolean isOptedForOffers = true;
    @Variable public static int initialCoins = 45;
    @Variable public static float correctGuessPercentage = 50.0F;


    public static class TestVarsJI {
        @Variable public  String javaIStr = "code_string";
        @Variable public  double javaIDouble = 1.42;
        @Variable  public  double javaIInt  = 1;
        @Variable public  boolean javaIBool = false;
    }

    public static class NullDefaultValue {
        @Variable public String string_with_null = null;
    }

}
