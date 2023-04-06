package com.clevertap.android.sdk.variables;

import com.clevertap.android.sdk.variables.annotations.Variable;
import java.util.HashMap;
import java.util.Map;

public class VariableDefinitions {
    @Variable public static String welcomeMsg = "Hello User";
    @Variable public static boolean isOptedForOffers = true;
    @Variable public static int initialCoins = 45;
    @Variable public static float correctGuessPercentage = 50.0F;


    public static class TestVarsJI {
        @Variable public  String javaIStr = "code_string";
        @Variable public  double javaIDouble = 1.42;
        @Variable public  double javaIInt  = 1;
        @Variable public  boolean javaIBool = false;
    }

    public static class NullDefaultValue {
        @Variable public String string_with_null = null;
    }

    public static class Groups {
        @Variable(group = "group1") public static String var_string1 = "str1";

        @Variable
        public static Map<String, Object> group1 = new HashMap<String, Object>() {{
            put("var_int1", 1);
            put("var_string2", "str2");

            // nested map
            put("group2", new HashMap<String, Object>() {{
                put("var_int2", 2);
            }});
        }};

        @Variable public static int var_int3 = 3;
    }

}
