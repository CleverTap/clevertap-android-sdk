package com.clevertap.android.directcall.network;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.PATCH})
@Retention(RetentionPolicy.SOURCE)
public @interface HttpMethod {
    String GET = "GET";
    String POST = "POST";
    String DELETE = "DELETE";
    String PUT = "PUT";
    String PATCH = "PATCH";
}