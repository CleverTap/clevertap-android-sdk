package com.clevertap.android.sdk.ab_testing.uieditor;

import androidx.annotation.NonNull;

class ViewProperty {
    public final String name;
    final Class<?> target;
    final ViewCaller accessor;
    private final String mutator;

    ViewProperty(String name, Class<?> targetClass, ViewCaller accessor, String mutatorName) {
        this.name = name;
        this.target = targetClass;
        this.accessor = accessor;
        this.mutator = mutatorName;
    }
    ViewCaller createMutator(Object[] methodArgs) throws NoSuchMethodException {
        return mutator == null ? null : new ViewCaller(this.target, mutator, methodArgs, Void.TYPE);
    }

    @NonNull
    @Override
    public String toString() {
        return "ViewProperty " + name + "," + target + ", " + accessor + "/" + mutator;
    }
}
