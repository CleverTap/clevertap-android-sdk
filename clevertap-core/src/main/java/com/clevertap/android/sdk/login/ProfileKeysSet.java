package com.clevertap.android.sdk.login;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Utils;
import java.util.HashSet;
import java.util.Iterator;

class ProfileKeysSet {

    private final HashSet<String> profileKeys = new HashSet<>();

    ProfileKeysSet(String[] keys) {
        init(keys);
    }

    ProfileKeysSet(HashSet<String> hashSet) {
        profileKeys.addAll(hashSet);
    }

    private void init(final String[] keys) {
        if (keys != null && keys.length > 0) {
            for (String key : keys) {
                if (Utils.containsIgnoreCase(Constants.ALL_PROFILE_IDENTIFIER_KEYS, key)) {
                    profileKeys.add(Utils.convertToTitleCase(key));
                }
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProfileKeysSet that = (ProfileKeysSet) o;
        return profileKeys.equals(that.profileKeys);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = profileKeys.iterator();
        while (iterator.hasNext()) {
            String type = iterator.next();
            if (Constants.ALL_PROFILE_IDENTIFIER_KEYS.contains(type)) {
                stringBuilder.append(type)
                        .append(iterator.hasNext() ? Constants.SEPARATOR_COMMA : "");
            }
        }
        return stringBuilder.toString();
    }

    boolean isValid() {
        return !profileKeys.isEmpty();
    }

    static ProfileKeysSet from(String keys) {
        return new ProfileKeysSet(keys.split(Constants.SEPARATOR_COMMA));
    }

    static ProfileKeysSet from(String[] keys) {
        return new ProfileKeysSet(keys);
    }

    static ProfileKeysSet getDefault() {
        return new ProfileKeysSet(Constants.DEFAULT_PROFILE_IDENTIFIER_KEYS);
    }

    boolean containsKey(String Key) {
        return profileKeys.contains(Key);
    }
}