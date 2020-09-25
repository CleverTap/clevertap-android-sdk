package com.clevertap.android.sdk.ab_testing.uieditor;

import androidx.annotation.NonNull;

class ViewProperty {

    public final String name;

    final ViewCaller accessor;

    final Class<?> target;

    private final String mutator;

    ViewProperty(String name, Class<?> targetClass, ViewCaller accessor, String mutatorName) {
        this.name = name;
        this.target = targetClass;
        this.accessor = accessor;
        this.mutator = mutatorName;
    }

    @NonNull
    @Override
    public String toString() {
        return "ViewProperty " + name + "," + target + ", " + accessor + "/" + mutator;
    }

    ViewCaller createMutator(Object[] methodArgs) throws NoSuchMethodException {
        return mutator == null ? null : new ViewCaller(this.target, mutator, methodArgs, Void.TYPE);
    }
}
