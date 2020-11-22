package com.clevertap.android.sdk.login;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ValidationResult;
import com.clevertap.android.sdk.ValidationResultFactory;
import com.clevertap.android.sdk.ValidationResultStack;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class ProfileValidatorImpl implements IValidator {

    private final ValidationResultStack remoteLogger;

    public ProfileValidatorImpl(final ValidationResultStack remoteErrorLogger) {
        remoteLogger = remoteErrorLogger;
    }

    @Override
    public String toIdentityString(HashSet<String> identityTypes) {
        StringBuilder stringBuilder = new StringBuilder();
        if (identityTypes != null && !identityTypes.isEmpty()) {
            Iterator<String> iterator = identityTypes.iterator();
            while (iterator.hasNext()) {
                String type = iterator.next();
                if (Constants.ALL_PROFILE_IDENTIFIER_KEYS.contains(type)) {
                    stringBuilder.append(type)
                            .append(iterator.hasNext() ? Constants.SEPARATOR_COMMA : "");
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public HashSet<String> toIdentityType(String[] identityKeys) {
        HashSet<String> hashSet = new HashSet<>();
        if (identityKeys != null && identityKeys.length > 0) {
            for (String key : identityKeys) {
                if (Constants.ALL_PROFILE_IDENTIFIER_KEYS.contains(key)) {
                    hashSet.add(key);
                }
            }
        }
        return hashSet;
    }

    @Override
    public void sendErrorOnIdentityMismatch(String[] first, String[] second) {
        if (first != null && second != null) {
            if (first.length != second.length) {
                ValidationResult error = ValidationResultFactory.create(531);
                remoteLogger.pushValidationResult(error);
            } else {
                Arrays.sort(first);
                Arrays.sort(second);
                for (int i = 0; i < first.length; i++) {
                    if (first[i] != null && !first[i].equalsIgnoreCase(second[i])) {
                        ValidationResult error = ValidationResultFactory.create(531);
                        remoteLogger.pushValidationResult(error);
                        break;
                    }
                }
            }
        }
    }
}