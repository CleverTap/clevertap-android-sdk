package com.clevertap.android.sdk.login;

import java.util.HashSet;

public interface IValidator {

    String toIdentityString(HashSet<String> identityTypes);

    HashSet<String> toIdentityType(String[] identityKeys);

    void sendErrorOnIdentityMismatch(String[] first, String[] second);

}