package com.clevertap.demo.variables_test.models;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.variables.Var;
import com.clevertap.android.sdk.variables.annotations.Variable;
import com.clevertap.android.sdk.variables.callbacks.VariableCallback;
import com.clevertap.demo.variables_test.UtilsKt;

public class Vars2 {
    @Variable public static String pStr2 = "default";
    @Variable public static double pDb2 = 9.99;
    @Variable public static int pIn2  = 9;
    @Variable public static boolean pBool2 = false;

    public static class Vars2Instance{
        @Variable public  String pStr2Inst = "default";
        @Variable public  double pDb2Inst = 9.99;
        @Variable public  int pIn2Inst  = 9;
        @Variable public  boolean pBool2Inst = false;

        private static Vars2Instance instance = null;

        public static Vars2Instance singleton() {
            if (instance != null) return instance;
            else {
                synchronized ((Vars2Instance.class)) {
                    if (instance == null) {
                        instance = new Vars2Instance();
                    }
                    return instance;
                }
            }
        }
    }


    public static String getParsedVars2(CleverTapAPI ct){
        String result = "";

       try {

           result+= String.format("pStr2:%s\t|\t%s\n", Vars2.pStr2,  ct.getVariable("pStr2"));
           result+= String.format("pDb2:%s\t|\t%s\n", Vars2.pDb2,  ct.getVariable("pDb2"));
           result+= String.format("pIn2:%d\t|\t%s\n", Vars2.pIn2, ct.getVariable("pIn2"));
           result+= String.format("pBool2:%s\t|\t%s\n", Vars2.pBool2, ct.getVariable("pBool2"));

           result+="-------<instance based>-------\n";
           Vars2Instance varsiI = Vars2Instance.singleton();
           result+= String.format("pStr2Inst:%s\t|\t%s\n", varsiI.pStr2Inst, ct.getVariable("pStr2Inst"));
           result+= String.format("pDb2Inst:%s\t|\t%s\n", varsiI.pDb2Inst, ct.getVariable("pDb2Inst"));
           result+= String.format("pIn2Inst:%s\t|\t%s\n", varsiI.pIn2Inst, ct.getVariable("pIn2Inst"));
           result+= String.format("pBool2Inst:%s\t|\t%s\n", varsiI.pBool2Inst, ct.getVariable("pBool2Inst"));
       }
       catch (Throwable t){
           t.printStackTrace();
       }

        return result;

    }

    public static void defineVarsWithListeners(CleverTapAPI ct){
        Var<String> va = ct.defineVariable("dStr2","default");
        Var<Double> vb = ct.defineVariable("dDb2",9.99);
        Var<Integer> vc = ct.defineVariable("dIn2",9);
        Var<Boolean> vd = ct.defineVariable("dBool2",false);
        va.addValueChangedHandler(new VariableCallback<String>() {
            @Override
            public void handle(Var<String> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ va.name()+ ". current value of variable="+va.value());
            }
        });

        vb.addValueChangedHandler(new VariableCallback<Double>() {
            @Override
            public void handle(Var<Double> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ vb.name()+ ". current value of variable="+vb.value());
            }
        });

        vc.addValueChangedHandler(new VariableCallback<Integer>() {
            @Override
            public void handle(Var<Integer> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ vc.name()+ ". current value of variable="+vc.value());
            }
        });

        vd.addValueChangedHandler(new VariableCallback<Boolean>() {
            @Override
            public void handle(Var<Boolean> variable) {
                UtilsKt.logg("VariableCallback@"+this.hashCode()+"called for "+ vd.name()+ ". current value of variable="+vd.value());
            }
        });


    }
    public static String getDefinedVars2(CleverTapAPI ct){
        String result = "";

        try {
            Object valueA = ct.getVariable("dStr2").value();
            result+= "dStr2:"+valueA+"\t|\t";

            Object valueB = ct.getVariable("dDb2").value();
            result+= "dDb2:"+valueB+"\t|\t";

            Object valueC =ct.getVariable("dIn2").value();
            result+= "dIn2:"+valueC+"\t|\t";

            Object valueD = ct.getVariable("dBool2").value();
            result+= "dBool2:"+valueD+"\t|\t";
        }
        catch (Throwable t){
            t.printStackTrace();
        }
        return result;
    }

}
