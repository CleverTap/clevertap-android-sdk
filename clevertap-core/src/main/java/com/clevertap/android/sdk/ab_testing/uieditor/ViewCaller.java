package com.clevertap.android.sdk.ab_testing.uieditor;

import android.view.View;
import com.clevertap.android.sdk.Logger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ViewCaller {

    private final Object[] methodArgs;

    private final String methodName;

    private final Class<?> methodResultType;

    private final Class<?> targetClass;

    private final Method targetMethod;

    ViewCaller(Class<?> targetClass, String methodName, Object[] methodArgs, Class<?> resultType)
            throws NoSuchMethodException {
        this.methodName = methodName;
        this.methodArgs = methodArgs;
        this.methodResultType = resultType;
        targetMethod = findMethod(targetClass);
        if (null == targetMethod) {
            throw new NoSuchMethodException("Method " + targetClass.getName() + "." + methodName + " doesn't exit");
        }
        this.targetClass = targetMethod.getDeclaringClass();
    }

    boolean argsAreApplicable(Object[] proposedArgs) {
        final Class<?>[] paramTypes = targetMethod.getParameterTypes();
        if (proposedArgs.length != paramTypes.length) {
            return false;
        }

        for (int i = 0; i < proposedArgs.length; i++) {
            final Class<?> paramType = assignableArgType(paramTypes[i]);
            if (proposedArgs[i] == null) {
                if (paramType == byte.class ||
                        paramType == short.class ||
                        paramType == int.class ||
                        paramType == long.class ||
                        paramType == float.class ||
                        paramType == double.class ||
                        paramType == boolean.class ||
                        paramType == char.class) {
                    return false;
                }
            } else {
                final Class<?> argumentType = assignableArgType(proposedArgs[i].getClass());
                if (argumentType.getCanonicalName() != null
                        && (argumentType.getCanonicalName().equals("android.content.res.ColorStateList") ||
                        argumentType.getCanonicalName().equals("android.graphics.drawable.ColorDrawable") ||
                        argumentType.getCanonicalName().equals("android.graphics.drawable.RippleDrawable"))) {
                    //no-op to skip
                } else if (!paramType.isAssignableFrom(argumentType)) {
                    return false;
                }
            }
        }
        return true;
    }

    Object[] getArgs() {
        return methodArgs;
    }

    String getMethodName() {
        return methodName;
    }

    Object invokeMethod(View target) {
        return invokeMethodWithArgs(target, methodArgs);
    }

    Object invokeMethodWithArgs(View target, Object[] arguments) {
        final Class<?> klass = target.getClass();
        if (targetClass.isAssignableFrom(klass)) {
            try {
                return targetMethod.invoke(target, arguments);
            } catch (final IllegalAccessException e) {
                Logger.v("Method " + targetMethod.getName() + " appears not to be public", e);
            } catch (final IllegalArgumentException e) {
                Logger.v("Method " + targetMethod.getName() + " called with arguments of the wrong type", e);
            } catch (final InvocationTargetException e) {
                Logger.v("Method " + targetMethod.getName() + " threw an exception", e);
            }
        }
        return null;
    }

    private Method findMethod(Class<?> klass) {
        final Class<?>[] argumentTypes = new Class[methodArgs.length];
        for (int i = 0; i < methodArgs.length; i++) {
            argumentTypes[i] = methodArgs[i].getClass();
        }

        for (final Method method : klass.getMethods()) {
            final String foundName = method.getName();
            final Class<?>[] params = method.getParameterTypes();

            if (!foundName.equals(methodName) || params.length != methodArgs.length) {
                continue;
            }

            final Class<?> assignType = assignableArgType(methodResultType);
            final Class<?> resultType = assignableArgType(method.getReturnType());
            if (!assignType.isAssignableFrom(resultType)) {
                continue;
            }

            boolean assignable = true;
            for (int i = 0; i < params.length && assignable; i++) {
                final Class<?> argumentType = assignableArgType(argumentTypes[i]);
                final Class<?> paramType = assignableArgType(params[i]);
                assignable = paramType.isAssignableFrom(argumentType);
            }

            if (!assignable) {
                continue;
            }

            return method;
        }
        return null;
    }

    private static Class<?> assignableArgType(Class<?> type) {
        if (type == Byte.class) {
            type = byte.class;
        } else if (type == Short.class) {
            type = short.class;
        } else if (type == Integer.class) {
            type = int.class;
        } else if (type == Long.class) {
            type = long.class;
        } else if (type == Float.class) {
            type = float.class;
        } else if (type == Double.class) {
            type = double.class;
        } else if (type == Boolean.class) {
            type = boolean.class;
        } else if (type == Character.class) {
            type = char.class;
        }
        return type;
    }
}
