package com.clevertap.android.sdk;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;

public  class TestStaticTesting {

    public static class SecretHelper{
        private static final ArrayList<String> secretList = new ArrayList<>();

        public static void updateSecret(String secret){
            if (!secretList.contains(secret)) {
                secretList.clear();
                secretList.add(secret);
            }
        }
    }

    public static class SecretHelper2{
        private static final ArrayList<String> secretList = new ArrayList<>();

        public static void updateSecret(String secret){
            updateSecret(secret,secretList);
        }

        private static void updateSecret(String secret, ArrayList<String> sl ){
            if (!sl.contains(secret)) {
                sl.clear();
                sl.add(secret);
            }
        }

    }


}
