package com.clevertap.demo;

import com.clevertap.android.sdk.variables.annotations.Variable;
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback;

public class ExampleVariables {

    @Variable
    public boolean var_boolean = true;

    @Variable
    public byte var_byte = 1;

    @Variable
    public short var_short = 2;

    @Variable
    public int var_int = 3;

    @Variable
    public long var_long = 4L;

    @Variable
    public float var_float = 5F;

    @Variable
    public double var_double = 6.;

    @Variable
    public String var_string = "str";

    private final VariablesChangedCallback oneTimeVariablesChangedCallback;
    private final VariablesChangedCallback variablesChangedCallback;

    public VariablesChangedCallback getOneTimeVariablesChangedCallback() {
        return oneTimeVariablesChangedCallback;
    }

    public VariablesChangedCallback getVariablesChangedCallback() {
        return variablesChangedCallback;
    }

    public ExampleVariables() {
        oneTimeVariablesChangedCallback = new VariablesChangedCallback() {
            @Override public void variablesChanged() {
                System.out.println("One Time Variables Changed");
            }
        };

        variablesChangedCallback = new VariablesChangedCallback() {
            @Override public void variablesChanged() {
                System.out.println("Variables Changed");
            }
        };
    }
}
