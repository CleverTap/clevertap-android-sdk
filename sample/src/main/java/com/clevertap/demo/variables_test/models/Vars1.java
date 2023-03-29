package com.clevertap.demo.variables_test.models;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.variables.Var;
import com.clevertap.android.sdk.variables.annotations.Variable;
import com.clevertap.android.sdk.variables.callbacks.VariableCallback;
import com.clevertap.demo.variables_test.UtilsKt;

public class Vars1 {
    @Variable public static String pStr1 = "default";
    @Variable public static double pDb1 = 9.99;
    @Variable public static int pIn1  = 9;
    @Variable public static boolean pBool1 = false;

    public static class Vars1Instance{
        @Variable public  String pStr1Inst = "default";
        @Variable public  double pDb1Inst = 9.99;
        @Variable public  int pIn1Inst  = 9;
        @Variable public  boolean pBool1Inst = false;

        private static Vars1Instance instance = null;

        public static Vars1Instance singleton() {
            if (instance != null) return instance;
            else {
                synchronized ((Vars1Instance.class)) {
                    if (instance == null) {
                        instance = new Vars1Instance();
                    }
                    return instance;
                }
            }
        }
    }


    public static String getParsedVars1(CleverTapAPI ct){
        String result = "";

       try {

           result+= String.format("pStr1:%s\t|\t%s\n", Vars1.pStr1,  ct.getVariableValue("pStr1"));
           result+= String.format("pDb1:%s\t|\t%s\n", Vars1.pDb1,  ct.getVariableValue("pDb1"));
           result+= String.format("pIn1:%d\t|\t%s\n", Vars1.pIn1, ct.getVariableValue("pIn1"));
           result+= String.format("pBool1:%s\t|\t%s\n", Vars1.pBool1, ct.getVariableValue("pBool1"));

           result+="-------<instance based>-------\n";
           Vars1Instance varsiI = Vars1Instance.singleton();
           result+= String.format("pStr1Inst:%s\t|\t%s\n", varsiI.pStr1Inst, ct.getVariableValue("pStr1Inst"));
           result+= String.format("pDb1Inst:%s\t|\t%s\n", varsiI.pDb1Inst, ct.getVariableValue("pDb1Inst"));
           result+= String.format("pIn1Inst:%s\t|\t%s\n", varsiI.pIn1Inst, ct.getVariableValue("pIn1Inst"));
           result+= String.format("pBool1Inst:%s\t|\t%s\n", varsiI.pBool1Inst, ct.getVariableValue("pBool1Inst"));
       }
       catch (Throwable t){
           t.printStackTrace();
       }

        return result;

    }

    public static void defineVarsWithListeners(CleverTapAPI ct){
        Var<String> va = ct.defineVariable("dStr1","default");
        Var<Double> vb = ct.defineVariable("dDb1",9.99);
        Var<Integer> vc = ct.defineVariable("dIn1",9);
        Var<Boolean> vd = ct.defineVariable("dBool1",false);
        va.addValueChangedCallback(new VariableCallback<String>() {
            @Override
            public void onValueChanged(Var<String> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ va.name()+ ". current value of variable="+va.value());
            }
        });

        vb.addValueChangedCallback(new VariableCallback<Double>() {
            @Override
            public void onValueChanged(Var<Double> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ vb.name()+ ". current value of variable="+vb.value());
            }
        });

        vc.addValueChangedCallback(new VariableCallback<Integer>() {
            @Override
            public void onValueChanged(Var<Integer> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ vc.name()+ ". current value of variable="+vc.value());
            }
        });

        vd.addValueChangedCallback(new VariableCallback<Boolean>() {
            @Override
            public void onValueChanged(Var<Boolean> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ vd.name()+ ". current value of variable="+vd.value());
            }
        });


    }
    public static String getDefinedVars1(CleverTapAPI ct){
        String result = "";

        try {
            Object valueA = ct.getVariableValue("dStr1");
            result+= "dStr1:"+valueA+"\t|\t";

            Object valueB = ct.getVariableValue("dDb1");
            result+= "dDb1:"+valueB+"\t|\t";

            Object valueC =ct.getVariableValue("dIn1");
            result+= "dIn1:"+valueC+"\t|\t";

            Object valueD = ct.getVariableValue("dBool1");
            result+= "dBool1:"+valueD+"\t|\t";
        }
        catch (Throwable t){
            t.printStackTrace();
        }
        return result;
    }

}
