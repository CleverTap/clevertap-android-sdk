package com.clevertap.demo.variables_test;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.variables.Var;
import com.clevertap.android.sdk.variables.annotations.Variable;

public class Vars1 {
    @Variable public static String pStr1 = "default";
    @Variable public static double pDb1 = 9.99;
    @Variable public static int pIn1  = 9;
    @Variable public static boolean pBool1 = false;

    public static String getParsedVars1(CleverTapAPI ct){
        String result = "";

       try {
           Var<?> valueA = ct.getVariable("pStr1");
           result+= "pStr1:"+Vars1.pStr1+"\t|\t"+valueA+"\n";

           Var<?> valueB = ct.getVariable("pDb1");
           result+= "pDb1:"+Vars1.pDb1+"\t|\t"+valueB+"\n";

           Var<?> valueC = ct.getVariable("pIn1");
           result+= "pIn1:"+Vars1.pIn1+"\t|\t"+valueC+"\n";

           Var<?> valueD = ct.getVariable("pBool1");
           result+= "pBool1:"+Vars1.pBool1+"\t|\t"+valueD+"\n";
       }
       catch (Throwable t){
           t.printStackTrace();
       }

        return result;

    }

    public static void defineVars1(CleverTapAPI ct){
        ct.defineVariable("dStr1","default");
        ct.defineVariable("dDb1",9.99);
        ct.defineVariable("dIn1",9);
        ct.defineVariable("dBool1",false);
    }
    public static String getDefinedVars1(CleverTapAPI ct){
        String result = "";

        try {
            Object valueA = ct.getVariable("dStr1").value();
            result+= "dStr1:"+valueA+"\t|\t";

            Object valueB = ct.getVariable("dDb1").value();
            result+= "dDb1:"+valueB+"\t|\t";

            Object valueC =ct.getVariable("dIn1").value();
            result+= "dIn1:"+valueC+"\t|\t";

            Object valueD = ct.getVariable("dBool1").value();
            result+= "dBool1:"+valueD+"\t|\t";
        }
        catch (Throwable t){
            t.printStackTrace();
        }
        return result;
    }

}
