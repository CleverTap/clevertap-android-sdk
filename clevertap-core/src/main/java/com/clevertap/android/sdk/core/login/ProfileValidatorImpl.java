package com.clevertap.android.sdk.core.login;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Constants.IdentityType;
import java.util.HashSet;
import java.util.Iterator;

public class ProfileValidatorImpl implements IValidator {

    @Override
    public String toIdentityString(HashSet<Constants.IdentityType> identityTypes) {
        StringBuilder stringBuilder = new StringBuilder();
        if (identityTypes != null && !identityTypes.isEmpty()) {
            Iterator<Constants.IdentityType> iterator = identityTypes.iterator();
            while (iterator.hasNext()) {
                Constants.IdentityType type = iterator.next();
                if (type != IdentityType.TYPE_INVALID) {
                    stringBuilder.append(type.getKey())
                            .append(iterator.hasNext() ? Constants.SEPARATOR_COMMA : "");
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public HashSet<Constants.IdentityType> toIdentityType(String[] identityKeys) {
        HashSet<Constants.IdentityType> hashSet = new HashSet<>();
        if (identityKeys != null && identityKeys.length > 0) {
            for (String key : identityKeys) {
                Constants.IdentityType type = Constants.IdentityType.fromKey(key);
                if (type != Constants.IdentityType.TYPE_INVALID) {
                    hashSet.add(type);
                }
            }
        }
        return hashSet;
    }
}