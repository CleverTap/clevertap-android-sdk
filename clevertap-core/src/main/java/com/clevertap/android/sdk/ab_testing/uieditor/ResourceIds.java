package com.clevertap.android.sdk.ab_testing.uieditor;

import android.util.SparseArray;
import com.clevertap.android.sdk.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

final class ResourceIds {

    private final Map<String, Integer> idNameToId;

    private final SparseArray<String> idToIdName;

    private final String resourcePackageName;

    ResourceIds(String resourcePackageName) {
        idNameToId = new HashMap<>();
        idToIdName = new SparseArray<>();
        this.resourcePackageName = resourcePackageName;
        read();
    }

    int idFromName(String name) {
        // noinspection ConstantConditions
        return idNameToId.get(name);
    }

    boolean knownIdName(String name) {
        return idNameToId.containsKey(name);
    }

    String nameForId(int id) {
        return idToIdName.get(id);
    }

    private String getLocalClassName() {
        return resourcePackageName + ".R$id";
    }

    private Class<?> getSystemClass() {
        return android.R.id.class;
    }

    private void read() {
        idNameToId.clear();
        idToIdName.clear();

        final Class<?> sysIdClass = getSystemClass();
        readClassIds(sysIdClass, "android", idNameToId);

        final String localClassName = getLocalClassName();
        try {
            final Class<?> rIdClass = Class.forName(localClassName);
            readClassIds(rIdClass, null, idNameToId);
        } catch (ClassNotFoundException e) {
            Logger.d("Can't load names for Android view ids from '" + localClassName
                    + "', ids by name will not be available in the events editor.");
            Logger.d("You may be missing a Resources class for your package due to your proguard configuration, " +
                    "or you may be using an applicationId in your build that isn't the same as the package declared in your AndroidManifest.xml file.\n"
                    +
                    "If you're using proguard, you can fix this issue by adding the following to your proguard configuration:\n\n"
                    +
                    "-keep class **.R$* {\n" +
                    "    <fields>;\n" +
                    "}\n\n" +
                    "If you're not using proguard, or if your proguard configuration already contains the directive above, "
                    +
                    "you can add the following to your AndroidManifest.xml file to explicitly point CleverTap SDK to "
                    +
                    "the appropriate library for your resources class:\n\n" +
                    "<meta-data android:name=\"CLEVERTAP_APP_PACKAGE\" android:value=\"YOUR_PACKAGE_NAME\" />\n\n" +
                    "where YOUR_PACKAGE_NAME is the same string you use for the \"package\" attribute in your <manifest> tag."
            );
        }

        for (Map.Entry<String, Integer> idMapping : idNameToId.entrySet()) {
            idToIdName.put(idMapping.getValue(), idMapping.getKey());
        }
    }

    private static void readClassIds(Class<?> platformIdClass, String namespace, Map<String, Integer> namesToIds) {
        try {
            final Field[] fields = platformIdClass.getFields();
            for (final Field field : fields) {
                final int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    final Class fieldType = field.getType();
                    if (fieldType == int.class) {
                        final String name = field.getName();
                        final int value = field.getInt(null);
                        final String namespacedName;
                        if (null == namespace) {
                            namespacedName = name;
                        } else {
                            namespacedName = namespace + ":" + name;
                        }

                        namesToIds.put(namespacedName, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            Logger.v("Can't read built-in id names from " + platformIdClass.getName(), e);
        }
    }
}