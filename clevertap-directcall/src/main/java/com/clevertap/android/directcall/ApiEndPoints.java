package com.clevertap.android.directcall;

public class ApiEndPoints {
    public static final String BASE_URL = "https://ctapi.patchus.in/api/v2/";
    public static final String GET_BASE_URL = "accounts/{id}/baseUrl";
    public static final String POST_CREATE_CONTACT = "accounts/{id}/contacts/signin";
    public static final String GET_TOKEN = "contacts/jwt";
    public static final String GET_VERIFY_TOKEN = "contacts/jwt/verify";
    public static final String PATCH_UPDATE_CALL_STATUS = "voice-calls/{id}";
    public static final String PATCH_UPDATE_DEVICE_ID = "contacts/{id}";
}
