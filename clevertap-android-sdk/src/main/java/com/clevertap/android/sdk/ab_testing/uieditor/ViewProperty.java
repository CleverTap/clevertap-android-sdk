package com.clevertap.android.sdk.ab_testing.uieditor;

import android.support.annotation.NonNull;

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

    ViewCaller makeMutator(Object[] methodArgs)
            throws NoSuchMethodException {
        if (null == mutator) {
            return null;
        }

        return new ViewCaller(this.target, mutator, methodArgs, Void.TYPE);
    }

    @NonNull
    @Override
    public String toString() {
        return "[ViewProperty " + name + "," + target + ", " + accessor + "/" + mutator + "]";
    }
}
