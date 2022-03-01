package com.clevertap.android.directcall.utils;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.ENABLE_HTTP_LOG;
import static com.clevertap.android.directcall.Constants.HTTP_CONNECT_TIMEOUT;
import static com.clevertap.android.directcall.Constants.HTTP_MAX_RETRIES;
import static com.clevertap.android.directcall.Constants.HTTP_READ_TIMEOUT;
import static com.clevertap.android.directcall.Constants.HTTP_RETRY_DELAY;
import static com.clevertap.android.directcall.Constants.KEY_ACCESS_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_ACTIVE_SESSION;
import static com.clevertap.android.directcall.Constants.KEY_AUTHORIZATION_HEADER;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CC;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CUID;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_PHONE;
import static com.clevertap.android.directcall.Constants.KEY_JWT_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_INITIATOR_ACTIONS;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_RECEIVER_ACTIONS;
import static com.clevertap.android.directcall.Constants.KEY_RETRY_DELAY;
import static com.clevertap.android.directcall.Constants.KEY_SECONDARY_BASE_URL;
import static com.clevertap.android.directcall.Constants.KEY_SNA;
import static com.clevertap.android.directcall.Constants.KEY_STATUS;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.clevertap.android.directcall.ApiEndPoints;
import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.exception.InitException;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.interfaces.DirectCallInitResponse;
import com.clevertap.android.directcall.interfaces.TokenResponse;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.network.Http;
import com.clevertap.android.directcall.network.HttpMethod;
import com.clevertap.android.directcall.network.JSONObjectListener;
import com.clevertap.android.sdk.CleverTapAPI;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class JwtUtil {
    private static JwtUtil ourInstance =null;
    private static Context context;

    public static JwtUtil getInstance(Context ctx) {
        if(ourInstance == null){
            ourInstance = new JwtUtil();
            context = ctx;
        }
        return ourInstance;
    }

    private JwtUtil() {
    }

    /**
     * fetches the jwt token before starting the sigsock.
     *
     * @param cc:-            cc of the user
     * @param phone:-         phone number of the user
     * @param accountId:-     accountId of the user
     * @param cuid:-          cuid of the user.
     * @param tokenResponse:- failure and success callback.
     */
    public void getToken(final Context context, final String cc, final String phone, final String accountId, String cuid, final TokenResponse tokenResponse) {
        try {
            CleverTapAPI cleverTapAPI = DirectCallAPI.getInstance().getCleverTapApi();
            String baseUrl = StorageHelper.getString(context, KEY_SECONDARY_BASE_URL, null);
            String authToken = StorageHelper.getString(context, KEY_ACCESS_TOKEN, null);
            int retryDelay = StorageHelper.getInt(context, KEY_RETRY_DELAY, HTTP_RETRY_DELAY);

            if(cleverTapAPI != null && baseUrl != null && authToken != null){
                new Http.Request(cleverTapAPI, HttpMethod.GET)
                        .url(baseUrl + ApiEndPoints.GET_TOKEN)
                        .header("Authorization", authToken)
                        .queryParameter("cuid", cuid)
                        .queryParameter("accountId", accountId)
                        .withBackoffCriteria(HTTP_MAX_RETRIES, retryDelay, TimeUnit.SECONDS)
                        .backoffCriteriaFailedListener(DirectCallAPI.getInstance().getBackoffCriteriaFailedListener())
                        .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                        .enableLog(ENABLE_HTTP_LOG)
                        .execute(new JSONObjectListener(){
                            @Override
                            public void onResponse(@Nullable JSONObject response, int responseCode, Boolean isSuccessful) {
                                try {
                                    if (response != null && isSuccessful) {
                                        String token = response.has(KEY_JWT_TOKEN) ? response.getString(KEY_JWT_TOKEN): null;
                                        String sna = response.has(KEY_SNA) ? response.getString(KEY_SNA): null;
                                        if (token == null || token.equals("")) {
                                            tokenResponse.onFailure("token invalid");
                                        } else {
                                            SharedPreferences sharedPref = StorageHelper.getPreferences(context);
                                            SharedPreferences.Editor editor = sharedPref.edit();
                                            editor.putString(KEY_SNA, sna);
                                            editor.putString(KEY_JWT_TOKEN, token);
                                            editor.putString(KEY_ACTIVE_SESSION, "true");
                                            if(DataStore.getInstance().getMissedCallInitiatorActions()!=null){
                                                Utils.storeMissedCallActionsList(context, KEY_MISSED_CALL_INITIATOR_ACTIONS, DataStore.getInstance().getMissedCallInitiatorActions());
                                            }
                                            if(DataStore.getInstance().getMissedCallReceiverActions()!=null){
                                                Utils.storeMissedCallActionsList(context, KEY_MISSED_CALL_RECEIVER_ACTIONS, DataStore.getInstance().getMissedCallReceiverActions());
                                            }
                                            editor.apply();
                                            tokenResponse.onSuccess("200");
                                        }
                                    } else {
                                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failure while fetching the authorization details");
                                        tokenResponse.onFailure("failed to get the JWT token");
                                    }
                                } catch (Exception e) {
                                    tokenResponse.onFailure("Something went wrong. Please pass the correct parameters");
                                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failure while fetching the authorization details");
                                }
                            }

                            @Override
                            public void onFailure(@Nullable Exception e) {
                                if(e != null)
                                    tokenResponse.onFailure(e.toString());
                                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failure while fetching the authorization details");
                            }
                        });
            }
        } catch (Exception e) {
            tokenResponse.onFailure("failed to get the JWT token");
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failure while fetching the authorization details");
        }
    }

    public void verifyToken(final Context context, final Boolean isFcmSignalling, final DirectCallInitResponse directCallInitResponse) {
        try {
            final SharedPreferences sharedPref = StorageHelper.getPreferences(context);
            final String userCuID = sharedPref.getString(KEY_CONTACT_CUID, null);
            final String accountId = sharedPref.getString(KEY_ACCOUNT_ID, null);
            final String userCC = sharedPref.getString(KEY_CONTACT_CC, null);
            final String userPhone = sharedPref.getString(KEY_CONTACT_PHONE, null);
            final String jwtTokenToVerify = sharedPref.getString(KEY_JWT_TOKEN, null);
            final String authToken = StorageHelper.getString(context, KEY_ACCESS_TOKEN, null);
            final String baseUrl = StorageHelper.getString(context, KEY_SECONDARY_BASE_URL, null);

            String cleverTapAccountId = StorageHelper.getString(context, Constants.KEY_CLEVERTAP_ACCOUNT_ID, null);
            if(!Utils.getInstance().initCleverTapApiIfRequired(context, cleverTapAccountId)) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX,
                        "cleverTapApi instance is null, can't process the VoIP push further, dropping the call!");
                return;
            }
            CleverTapAPI cleverTapAPI = DirectCallAPI.getInstance().getCleverTapApi();
            int retryDelay = StorageHelper.getInt(context, KEY_RETRY_DELAY, HTTP_RETRY_DELAY);

            if(cleverTapAPI != null && baseUrl != null && jwtTokenToVerify!=null && !jwtTokenToVerify.isEmpty()){
                new Http.Request(cleverTapAPI, HttpMethod.GET)
                        .url(baseUrl + ApiEndPoints.GET_VERIFY_TOKEN)
                        .header(KEY_AUTHORIZATION_HEADER, authToken)
                        .queryParameter(KEY_CONTACT_CUID, userCuID)
                        .queryParameter(KEY_JWT_TOKEN, jwtTokenToVerify)
                        .queryParameter(KEY_ACCOUNT_ID, accountId)
                        .withBackoffCriteria(HTTP_MAX_RETRIES, retryDelay, TimeUnit.SECONDS)
                        .backoffCriteriaFailedListener(DirectCallAPI.getInstance().getBackoffCriteriaFailedListener())
                        .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                        .enableLog(ENABLE_HTTP_LOG)
                        .execute(new JSONObjectListener() {
                            @Override
                            public void onResponse(@Nullable JSONObject response, int responseCode, Boolean isSuccessful) {
                                try {
                                    if(isSuccessful){
                                        //if true, start jobService and sigSock else hit /jwt to get new token
                                        String status = null;
                                        if(response != null) {
                                            status = response.has(KEY_STATUS) ? response.getString(KEY_STATUS) : null;
                                        }
                                        if(status != null && status.equals("true")){
                                            if(isFcmSignalling){
                                                directCallInitResponse.onSuccess();
                                            }else {
                                                Utils.getInstance().startJobServiceForSigSock(context);
                                            }
                                        }else{
                                            //else means result = false , means token is expired so generate new one then store it
                                            getToken(context, userCC, userPhone, accountId, userCuID, new TokenResponse() {
                                                @Override
                                                public void onSuccess(String response) {
                                                    try {
                                                        if(response.equals("200")){
                                                            //200 means successfully stored the newly generated token so now starting service+sigsock
                                                            if(isFcmSignalling){
                                                                directCallInitResponse.onSuccess();
                                                            }else {
                                                                Utils.getInstance().startJobServiceForSigSock(context);
                                                            }
                                                        }
                                                    }catch (Exception e){
                                                        //no-op
                                                    }
                                                }

                                                @Override
                                                public void onFailure(String failure) {
                                                    Utils.getInstance().resetSession(context);
                                                }
                                            });
                                        }
                                    }else{
                                        //500 (internal server error) handling
                                        Utils.getInstance().resetSession(context);
                                        CustomHandler.getInstance().sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.SdkNotInitializedAppRestartRequiredException);
                                    }
                                } catch (Exception e) {
                                    //no-op
                                }
                            }

                            @Override
                            public void onFailure(@Nullable Exception e) {
                                try {
                                    Utils.getInstance().resetSession(context);
                                    CustomHandler.getInstance().sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.SdkNotInitializedAppRestartRequiredException);
                                }catch (Exception e1){
                                    //no-op
                                }
                            }
                        });
            }
        }catch (Exception e){
            //no-op
        }
    }
}
