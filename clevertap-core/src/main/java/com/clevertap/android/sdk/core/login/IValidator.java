package com.clevertap.android.sdk.core.login;

import com.clevertap.android.sdk.Constants;
import java.util.HashSet;

public interface IValidator {

    String toIdentityString(HashSet<Constants.IdentityType> identityTypes);

    HashSet<Constants.IdentityType> toIdentityType(String[] identityKeys);

}