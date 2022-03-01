package com.clevertap.android.directcall.init;

import static com.clevertap.android.directcall.Constants.ACTION_CANCEL_CALL;
import static com.clevertap.android.directcall.Constants.ACTION_INCOMING_CALL;
import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.ENABLE_HTTP_LOG;
import static com.clevertap.android.directcall.Constants.HTTP_CONNECT_TIMEOUT;
import static com.clevertap.android.directcall.Constants.HTTP_INFINITE_RETRIES;
import static com.clevertap.android.directcall.Constants.HTTP_MAX_RETRIES;
import static com.clevertap.android.directcall.Constants.HTTP_READ_TIMEOUT;
import static com.clevertap.android.directcall.Constants.HTTP_RETRY_DELAY;
import static com.clevertap.android.directcall.Constants.KEY_ACCESS_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_ACTIVE_SESSION;
import static com.clevertap.android.directcall.Constants.KEY_API_KEY;
import static com.clevertap.android.directcall.Constants.KEY_AUTHORIZATION_HEADER;
import static com.clevertap.android.directcall.Constants.KEY_BG_COLOR;
import static com.clevertap.android.directcall.Constants.KEY_BRAND_LOGO;
import static com.clevertap.android.directcall.Constants.KEY_CLEVERTAP_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_CLEVERTAP_API_KEY;
import static com.clevertap.android.directcall.Constants.KEY_CLEVERTAP_ID;
import static com.clevertap.android.directcall.Constants.KEY_CLI_LIST;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CC;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CUID;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_ID;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_NAME;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_PHONE;
import static com.clevertap.android.directcall.Constants.KEY_DEVICE_ID;
import static com.clevertap.android.directcall.Constants.KEY_ECTA;
import static com.clevertap.android.directcall.Constants.KEY_FCM_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_JWT_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_INITIATOR_ACTIONS;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_INITIATOR_HOST;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_RECEIVER_ACTIONS;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_RECEIVER_HOST;
import static com.clevertap.android.directcall.Constants.KEY_RETRY_DELAY;
import static com.clevertap.android.directcall.Constants.KEY_RINGTONE;
import static com.clevertap.android.directcall.Constants.KEY_SDK_VERSION;
import static com.clevertap.android.directcall.Constants.KEY_SECONDARY_BASE_URL;
import static com.clevertap.android.directcall.Constants.KEY_SNA;
import static com.clevertap.android.directcall.Constants.KEY_TEMPLATE_PIN_VIEW_CONFIG;
import static com.clevertap.android.directcall.Constants.KEY_TEMPLATE_SCRATCHCARD_CONFIG;
import static com.clevertap.android.directcall.Constants.KEY_TEXT_COLOR;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.clevertap.android.directcall.ApiEndPoints;
import com.clevertap.android.directcall.BuildConfig;
import com.clevertap.android.directcall.CTCallingLogger;
import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.events.CTDirectCallTaskManager;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.events.PushDirectCallEventTask;
import com.clevertap.android.directcall.exception.CallException;
import com.clevertap.android.directcall.exception.DirectCallException;
import com.clevertap.android.directcall.exception.InitException;
import com.clevertap.android.directcall.fcm.DirectCallNotificationHandler;
import com.clevertap.android.directcall.fcm.FcmCacheManger;
import com.clevertap.android.directcall.fcm.FcmSigSockService;
import com.clevertap.android.directcall.fcm.FcmUtil;
import com.clevertap.android.directcall.interfaces.DirectCallInitResponse;
import com.clevertap.android.directcall.interfaces.OutgoingCallResponse;
import com.clevertap.android.directcall.interfaces.TokenResponse;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.models.Cli;
import com.clevertap.android.directcall.models.MissedCallNotificationOpenResult;
import com.clevertap.android.directcall.network.Http;
import com.clevertap.android.directcall.network.HttpMethod;
import com.clevertap.android.directcall.network.JSONObjectListener;
import com.clevertap.android.directcall.network.StringListener;
import com.clevertap.android.directcall.services.JobSchedulerSocketService;
import com.clevertap.android.directcall.utils.CustomHandler;
import com.clevertap.android.directcall.utils.JwtUtil;
import com.clevertap.android.directcall.utils.PinViewObservable;
import com.clevertap.android.directcall.utils.SocketIOManager;
import com.clevertap.android.directcall.utils.Utils;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.pushnotification.PushConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DirectCallAPI {
    static DirectCallAPI instance = null;
    static JobInfo myJob;
    static JobScheduler jobScheduler;
    static JobSchedulerSocketService jobSchedulerSocketService;
    public static Boolean sdkReady = true;
    private final CustomHandler customHandler = CustomHandler.getInstance();
    DataStore dataStore = DataStore.getInstance();
    private Context context;
    private JSONObject initJson;
    private DirectCallInitResponse directCallInitResponse;

    @Nullable
    private CleverTapAPI cleverTapAPI;

    private static final CTCallingLogger logger;

    private boolean isEnabled = false;

    private final Http.BackoffCriteriaFailedListener backoffCriteriaFailedListener = () -> {

        getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Session timeout, reinitializing the SDK");

        reinitializeSdk();
    };

    static {
        logger = new CTCallingLogger(CTCallingLogger.DEBUG);
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public static CTCallingLogger getLogger() {
        return logger;
    }

    public static Boolean isGoodToGo() {
        return sdkReady;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Http.BackoffCriteriaFailedListener getBackoffCriteriaFailedListener() {
        return backoffCriteriaFailedListener;
    }

    /**
     * Creates a new instance of this class if not created and returns the instance.
     */
    public static DirectCallAPI getInstance() {
        if (instance == null) {
            instance = new DirectCallAPI();
        }
        return instance;
    }

    /**
     * function to initialize the sdk
     *
     * @param context:           context of the application from which the function is called
     * @param initJson           JsonObject with all the fields required for initialization
     * @param directCallInitResponse: success or failure response
     */
    private void init(final Context context,
                      final JSONObject initJson,
                      final DirectCallInitOptions initOptions,
                      final DirectCallInitResponse directCallInitResponse) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean isInternetConnected = Utils.getInstance().hasInternetAccess(context);
                        if (!isInternetConnected) {
                            logger.verbose(CALLING_LOG_TAG_SUFFIX, "Network connectivity unavailable, initialization failed! ");
                            customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, DirectCallException.NoInternetException);
                            return;
                        }

                        prepareSdkForInitialization(context, initOptions);

                        dataStore.setContext(context);
                        dataStore.setInitJsonOptions(initJson);
                        dataStore.setDirectCallInitResponse(directCallInitResponse);
                        dataStore.setReadPhoneStatePermission(initOptions.isReadPhoneStateEnabled());
                        dataStore.setMissedCallInitiatorActions(initOptions.getMissedCallInitiatorActions());
                        dataStore.setMissedCallReceiverActions(initOptions.getMissedCallReceiverActions());
                        if(initOptions.getMissedCallInitiatorHost() != null)
                        dataStore.setMissedCallInitiatorHost(initOptions.getMissedCallInitiatorHost(), missedCallNotificationOpenedHandler -> {

                        });
                        if(initOptions.getMissedCallReceiverHost() != null)
                            dataStore.setMissedCallReceiverHost(initOptions.getMissedCallReceiverHost(), missedCallNotificationOpenedHandler -> {

                        });

                        jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

                        String session = StorageHelper.getString(context, KEY_ACTIVE_SESSION, null);
                        if (session != null && session.equals("true")) {
                            //verify the token
                            JwtUtil.getInstance(context).verifyToken(context, false, directCallInitResponse);
                            String newToken = initJson.has(KEY_FCM_TOKEN) ? initJson.getString(KEY_FCM_TOKEN) : null;
                            String storedToken = StorageHelper.getString(context, KEY_FCM_TOKEN, null);
                            if (newToken != null &&
                                    !newToken.isEmpty() &&
                                    !newToken.equals(storedToken)) {
                                //Passed token & the stored token do not match, hence updating registering the
                                // so updating new deviceToken in contact model
                                registerNewFcmToken(context, newToken);
                            }
                        } else {
                            initializeSdk();
                        }
                    } catch (Exception e) {
                        //no-op
                    }
                }
            }).start();
        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while initializing the SDK: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void prepareSdkForInitialization(Context context, DirectCallInitOptions initOptions) {
        resetSessionForDifferentSdkVersion(context);

        //to prevent retention of FCM singleton details while app is in killed state)
        FcmCacheManger.getInstance().resetFcmCache();

        storePreferences(context, initOptions);

        SocketIOManager.resetSocketConfiguration();
    }

    private void resetSessionForDifferentSdkVersion(Context context) {
        String storedSdkVersion = StorageHelper.getString(context, KEY_SDK_VERSION, null);
        String currentSdkVersion = BuildConfig.VERSION_NAME;
        if (storedSdkVersion != null &&
                Utils.getInstance().compareVersionNames(storedSdkVersion, currentSdkVersion) != 0) {
            //versions are different, delete stored session here
            Utils.getInstance().resetSession(context);
        }
    }

    private void initializeSdk() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dataStore = DataStore.getInstance();
                    if (dataStore == null) {
                        return;
                    }
                    if (dataStore.getContext() == null || dataStore.getInitJsonOptions() == null || dataStore.getDirectCallInitResponse() == null) {
                        logger.debug(CALLING_LOG_TAG_SUFFIX, "Context or other initialisation parameters can not be null");
                        return;
                    }
                    context = dataStore.getContext();
                    initJson = dataStore.getInitJsonOptions();
                    directCallInitResponse = dataStore.getDirectCallInitResponse();

                    boolean isInternetConnected = Utils.getInstance().hasInternetAccess(context);
                    if (isInternetConnected) {
                        String numberRegex = "\\d+";
                        String cuid;
                        String malformedCuidRegex = "^(\\d{0,}(?:[-,.\"'*+:@!#$%^&*()_=~<>|\\}{])\\d{0,})*$";
                        String parsedCuidRegex = "^[a-zA-Z0-9 \\@._-]*$";
                        if (initJson.has("name") && initJson.getString("name").length() > 25) {
                            sdkReady = true;
                            logger.debug(CALLING_LOG_TAG_SUFFIX, "Length of the name of the contact can't exceed by 25");
                            return;
                        }
                        if ((initJson.has("accountID") && initJson.getString("accountID").equals("") || initJson.getString("accountID") == "") ||
                                (!initJson.has("apikey") || initJson.getString("apikey").equals(""))) {
                            sdkReady = true;
                            logger.debug(CALLING_LOG_TAG_SUFFIX, "Both AccountId and ApiKey are required for SDK initialization");
                            return;
                        }
                        if (initJson.has("cuid") && initJson.getString("cuid").length() > 0) {
                            if (initJson.getString("cuid").trim().length() < 5 || initJson.getString("cuid").trim().length() > 50) {
                                sdkReady = true;
                                logger.debug(CALLING_LOG_TAG_SUFFIX, "CUID length should be in range of 5 to 50 characters");
                                return;
                            } else if (!(Pattern.compile((parsedCuidRegex)).matcher(initJson.getString("cuid")).matches())
                                    ||
                                    (Pattern.compile((malformedCuidRegex)).matcher(initJson.getString("cuid")).matches())) {
                                sdkReady = true;
                                logger.debug(CALLING_LOG_TAG_SUFFIX, "CUID having special character between numbers is not allowed");
                                return;
                            } else if ((initJson.has("phone") && initJson.getString("phone").length() > 0) ||
                                    (initJson.has("cc") && initJson.getString("cc").length() > 0)) {
                                if (initJson.getString("phone").length() < 6 || initJson.getString("phone").length() > 15 || !(initJson.getString("phone").matches(numberRegex))) {
                                    sdkReady = true;
                                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Invalid phone number or length is not in the range from 6 to 15");
                                    return;
                                }
                                if (initJson.getString("cc").length() < 1 || initJson.getString("cc").length() > 4 || !(initJson.getString("cc").matches(numberRegex))) {
                                    sdkReady = true;
                                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Invalid cc(country-code) or length is not in the range from 1 to 4");
                                    return;
                                }
                            }
                        } else if (initJson.has("phone") && initJson.getString("phone").length() > 0
                                && initJson.has("cc") && initJson.getString("cc").length() > 0) {
                            if (initJson.getString("phone").length() < 6 || initJson.getString("phone").length() > 15 || !(initJson.getString("phone").matches(numberRegex))) {
                                sdkReady = true;
                                logger.debug(CALLING_LOG_TAG_SUFFIX, "Invalid phone number or length is not in the range from 6 to 15");
                                return;
                            }
                            if (initJson.getString("cc").length() < 1 || initJson.getString("cc").length() > 4 || !(initJson.getString("cc").matches(numberRegex))) {
                                sdkReady = true;
                                logger.debug(CALLING_LOG_TAG_SUFFIX, "Invalid cc(country-code) or length is not in the range from 1 to 4");
                                return;
                            }
                            if (initJson.has("cuid") && initJson.getString("cuid").trim().length() == 0) {
                                initJson.remove("cuid");
                            }
                        } else {
                            sdkReady = true;
                            logger.debug(CALLING_LOG_TAG_SUFFIX, "Either CUID or CC and Phone is required for initialization");
                            return;
                        }

                        if ((context == null)) {
                            sdkReady = true;
                            logger.debug(CALLING_LOG_TAG_SUFFIX, "activity context is required for initialization");
                            return;
                        }

                        String accountId = initJson.getString("accountID");
                        String apikey = initJson.getString("apikey");

                        DataStore.getInstance().setAccountId(accountId);
                        DataStore.getInstance().setApikey(apikey);
                        getBaseUrl(context, accountId, apikey);
                    } else {
                        sdkReady = true;
                        customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, DirectCallException.NoInternetException);
                    }
                } catch (Exception e) {
                    sdkReady = true;
                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Invalid/Incorrect parameters are passed, please make sure parameters are valid");
                }
            }
        });
        thread.start();
    }

    /**
     * function to register a contact of the user with rest API
     *
     * @param context:           context of the application from which the function is called
     * @param cc:                country code of the user
     * @param phone:             phone number of the user
     * @param name:              name of the user
     * @param directCallInitResponse: for success or failure responses
     */
    private void registerContact(final Context context, final String fcmToken, final String appId, final String name, final String cc, final String phone, final String accountId, final String apiKey, String ctAccountId, String guid, final String ringtone, final String cuid, String cleverTapId, final DirectCallInitResponse directCallInitResponse) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isInternetConnected = Utils.getInstance().hasInternetAccess(context);
                    final String plateform = context.getString(R.string.platform_android);
                    if (isInternetConnected) {

                        JSONObject createContact = new JSONObject();
                        try {
                            if (appId != null && !appId.isEmpty()) {
                                createContact.put(Constants.KEY_APP_ID, appId);
                            }
                            if (accountId != null) {
                                createContact.put(Constants.KEY_ACCOUNT_ID, accountId);
                            }
                            if (apiKey != null) {
                                createContact.put(Constants.KEY_API_KEY, apiKey);
                            }
                            if (ctAccountId != null) {
                                createContact.put(KEY_CLEVERTAP_ACCOUNT_ID, ctAccountId);
                            }
                            if (guid != null) {
                                createContact.put(KEY_CLEVERTAP_ID, guid);
                            }
                            if (cuid != null) {
                                createContact.put(Constants.KEY_CONTACT_CUID, cuid);
                            }
                            if (name != null) {
                                createContact.put(Constants.KEY_CONTACT_NAME, name);
                            }
                            if (cc != null) {
                                createContact.put(Constants.KEY_CONTACT_CC, cc);
                            }
                            if (phone != null) {
                                createContact.put(Constants.KEY_CONTACT_PHONE, phone);
                            }
                            if (plateform != null) {
                                createContact.put(Constants.KEY_PLATFORM, plateform);
                            }
                            if (fcmToken != null) {
                                createContact.put(KEY_DEVICE_ID, fcmToken);
                            }
                            createContact.put(KEY_SDK_VERSION, BuildConfig.VERSION_NAME);
                        } catch (Exception e) {
                            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while creating the contact object" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }

                        CleverTapAPI cleverTapAPI = getCleverTapApi();
                        String baseUrl = StorageHelper.getString(context, KEY_SECONDARY_BASE_URL, null);
                        int retryDelay = StorageHelper.getInt(context, KEY_RETRY_DELAY, HTTP_RETRY_DELAY);

                        if (cleverTapAPI != null && baseUrl != null)
                            new Http.Request(cleverTapAPI, HttpMethod.POST)
                                    .url(baseUrl + ApiEndPoints.POST_CREATE_CONTACT)
                                    .pathParameter("id", accountId)
                                    .body(createContact)
                                    .withBackoffCriteria(HTTP_MAX_RETRIES, retryDelay, TimeUnit.SECONDS)
                                    .excludeResponseCodesToBackoff(new Integer[]{401})
                                    .backoffCriteriaFailedListener(backoffCriteriaFailedListener)
                                    .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                                    .enableLog(ENABLE_HTTP_LOG)
                                    .execute(new JSONObjectListener() {
                                        @Override
                                        public void onResponse(@Nullable JSONObject response, int responseCode, Boolean isSuccessful) {
                                            try {
                                                if (response != null && isSuccessful) {
                                                    String textColor = "", bgColor = "", brandLogoUrl = "";

                                                    String accessToken = response.has(Constants.KEY_ACCESS_TOKEN) ? response.getString(Constants.KEY_ACCESS_TOKEN) : null;
                                                    String contactId = response.has(Constants.KEY_CONTACT_ID) ? response.getString(Constants.KEY_CONTACT_ID) : null;
                                                    String contactName = response.has(Constants.KEY_CONTACT_NAME) ? response.getString(Constants.KEY_CONTACT_NAME) : null;
                                                    String contactCC = response.has(Constants.KEY_CONTACT_CC) ? response.getString(Constants.KEY_CONTACT_CC) : "";
                                                    String contactPhone = response.has(Constants.KEY_CONTACT_PHONE) ? response.getString(Constants.KEY_CONTACT_PHONE) : "";
                                                    String ecta = response.has(KEY_ECTA) ? response.getString(KEY_ECTA) : "";

                                                    JSONObject brandingObject = response.has(Constants.KEY_BRANDING) ? response.getJSONObject(Constants.KEY_BRANDING) : null;

                                                    if (brandingObject != null) {
                                                        textColor = brandingObject.has(KEY_TEXT_COLOR) ? brandingObject.getString(KEY_TEXT_COLOR) : "";
                                                        bgColor = brandingObject.has(KEY_BG_COLOR) ? brandingObject.getString(KEY_BG_COLOR) : "";
                                                        brandLogoUrl = brandingObject.has(KEY_BRAND_LOGO) ? brandingObject.getString(KEY_BRAND_LOGO) : "";
                                                    }

                                                    JSONArray cliArray = response.has(KEY_CLI_LIST) ? response.getJSONArray(KEY_CLI_LIST) : null;

                                                    final SharedPreferences prefs = StorageHelper.getPreferences(context);
                                                    SharedPreferences.Editor editor = prefs.edit();
                                                    editor.putString(KEY_ACCOUNT_ID, accountId);
                                                    editor.putString(KEY_API_KEY, apiKey);
                                                    editor.putString(KEY_ACCESS_TOKEN, accessToken);
                                                    editor.putString(KEY_CONTACT_ID, contactId);
                                                    editor.putString(KEY_FCM_TOKEN, fcmToken);
                                                    editor.putString(KEY_CONTACT_CUID, cuid);
                                                    editor.putString(KEY_CONTACT_NAME, contactName);
                                                    editor.putString(KEY_CONTACT_CC, contactCC);
                                                    editor.putString(KEY_CONTACT_PHONE, contactPhone);
                                                    editor.putString(KEY_SDK_VERSION, BuildConfig.VERSION_NAME);
                                                    editor.putString(KEY_TEXT_COLOR, textColor);
                                                    editor.putString(KEY_BG_COLOR, bgColor);
                                                    editor.putString(KEY_BRAND_LOGO, brandLogoUrl);
                                                    editor.putString(KEY_ECTA, ecta);
                                                    editor.putString(KEY_CLEVERTAP_ID, cleverTapId);
                                                    if(cliArray != null)
                                                    editor.putString(KEY_CLI_LIST, cliArray.toString());
                                                    if (ringtone != null && !ringtone.isEmpty()) {
                                                        editor.putString(KEY_RINGTONE, ringtone);
                                                    }
                                                    StorageHelper.persist(editor);

                                                    Utils.getInstance().preloadBrandLogo(context, brandLogoUrl);

                                                    JwtUtil.getInstance(context).getToken(context, cc, phone, accountId, cuid, new TokenResponse() {
                                                        @Override
                                                        public void onSuccess(String response) {
                                                            if (response.equals("200")) {
                                                                jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
                                                                myJob = new JobInfo.Builder(10, new ComponentName(context, jobSchedulerSocketService.getClass()))
                                                                        .setBackoffCriteria(4000, JobInfo.BACKOFF_POLICY_LINEAR)
                                                                        .setPersisted(true)
                                                                        .setMinimumLatency(1)
                                                                        .setOverrideDeadline(1)
                                                                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                                                        .build();

                                                                jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                                                if (jobScheduler != null) {
                                                                    jobScheduler.schedule(myJob);
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(String failure) {
                                                            customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, DirectCallException.SdkNotInitializedAppRestartRequiredException);
                                                        }
                                                    });
                                                } else {
                                                    if (responseCode == 401) {
                                                        sdkReady = false;
                                                        customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.SdkNotInitializedDueToCuidConnectedElsewhereException);
                                                    } else {
                                                        customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.DirectCallSdkNotInitializedException);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.DirectCallSdkNotInitializedException);
                                                logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while initializing the SDK: " + e.getLocalizedMessage());
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(@Nullable Exception e) {
                                            if (directCallInitResponse != null)
                                                customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.ContactNotRegisteredException);
                                            if(e != null){
                                                logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while SDK initialization: " + e.getLocalizedMessage());
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                    } else {
                        customHandler.sendInitAnnotations(directCallInitResponse, CustomHandler.Init.ON_FAILURE, DirectCallException.NoInternetException);
                    }
                } catch (Exception e) {
                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while SDK initialization: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void storePreferences(Context context, DirectCallInitOptions initOptions) {
        try {
            if (initOptions.getPinViewConfig() != null) {
                StorageHelper.putString(
                        context,
                        KEY_TEMPLATE_PIN_VIEW_CONFIG,
                        initOptions.getPinViewConfig().toJson().toString()
                );
            }
            if (initOptions.getScratchCardConfig() != null) {
                StorageHelper.putString(
                        context,
                        KEY_TEMPLATE_SCRATCHCARD_CONFIG,
                        initOptions.getScratchCardConfig().toJson().toString()
                );
            }
            if(initOptions.getMissedCallInitiatorHost() != null){
                StorageHelper.putString(
                        context,
                        KEY_MISSED_CALL_INITIATOR_HOST,
                        initOptions.getMissedCallInitiatorHost()
                );
            }
            if(initOptions.getMissedCallReceiverHost() != null){
                StorageHelper.putString(
                        context,
                        KEY_MISSED_CALL_RECEIVER_HOST,
                        initOptions.getMissedCallReceiverHost()
                );
            }
        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while persisting the initialization details: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void getBaseUrl(final Context context, final String accountId, final String apikey) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put(Constants.KEY_API_KEY, apikey);

            CleverTapAPI cleverTapAPI = getCleverTapApi();
            if (cleverTapAPI != null)
                new Http.Request(cleverTapAPI, HttpMethod.GET)
                        .url(ApiEndPoints.BASE_URL + ApiEndPoints.GET_BASE_URL)
                        .pathParameter("id", accountId)
                        .body(requestBody)
                        .withBackoffCriteria(HTTP_INFINITE_RETRIES, HTTP_RETRY_DELAY, TimeUnit.SECONDS)
                        .excludeResponseCodesToBackoff(new Integer[]{400})
                        .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                        .enableLog(ENABLE_HTTP_LOG)
                        .execute(new JSONObjectListener() {
                            @Override
                            public void onResponse(@Nullable JSONObject response, int responseCode, Boolean isSuccessful) {
                                try {
                                    if (response != null && isSuccessful) {
                                        String baseUrl = response.has(Constants.KEY_SECONDARY_BASE_URL) ? response.getString(Constants.KEY_SECONDARY_BASE_URL) : null;
                                        int retryDelay = response.has(Constants.KEY_RETRY_DELAY) ? response.getInt(Constants.KEY_RETRY_DELAY) : HTTP_RETRY_DELAY;

                                        StorageHelper.putString(context, KEY_SECONDARY_BASE_URL, baseUrl);
                                        StorageHelper.putIntImmediate(context, KEY_RETRY_DELAY, retryDelay);

                                        signIn();
                                    } else {
                                        //when 401, 400,404 error code received
                                        if (responseCode == 400) {
                                            logger.debug(CALLING_LOG_TAG_SUFFIX, "Can't initialize the SDK because passed AccountID or Apikey are not valid");
                                        } else {
                                            customHandler.sendInitAnnotations(DirectCallAPI.this.directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.DirectCallSdkNotInitializedException);
                                        }
                                    }
                                } catch (Exception e) {
                                    try {
                                        customHandler.sendInitAnnotations(DirectCallAPI.this.directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.DirectCallSdkNotInitializedException);
                                    } catch (Exception e1) {
                                        //no-op
                                    }
                                }
                            }

                            @Override
                            public void onFailure(@Nullable Exception e) {
                                try {
                                    customHandler.sendInitAnnotations(DirectCallAPI.this.directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.DirectCallSdkNotInitializedException);
                                } catch (Exception e1) {
                                    //no-op
                                }
                            }
                        });
        } catch (Exception e) {
            //no-op
        }
    }

    private void signIn() {
        try {
            registerContact(context, initJson.has("fcmToken") ? initJson.getString("fcmToken") : null
                    , initJson.has("appId") ? initJson.getString("appId") : "",
                    initJson.has("name") ? initJson.getString("name") : "",
                    initJson.has("cc") ? initJson.getString("cc") : "",
                    initJson.has("phone") ? initJson.getString("phone") : "",
                    initJson.getString("accountID"),
                    initJson.getString("apikey"),
                    initJson.getString(KEY_CLEVERTAP_ACCOUNT_ID),
                    initJson.getString(KEY_CLEVERTAP_ID),
                    initJson.has("ringtone") ? initJson.getString("ringtone") : null,
                    initJson.has("cuid") ? initJson.getString("cuid") : null,
                    initJson.has("cleverTapId") ? initJson.getString("cleverTapId") : null, new DirectCallInitResponse() {
                        @Override
                        public void onSuccess() {
                            sdkReady = true;
                        }

                        @Override
                        public void onFailure(InitException initException) {
                            if (initException == InitException.SdkNotInitializedDueToCuidConnectedElsewhereException) {
                                sdkReady = true;
                                customHandler.sendInitAnnotations(DirectCallAPI.this.directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.SdkNotInitializedDueToCuidConnectedElsewhereException);
                            } else {
                                sdkReady = true;
                                customHandler.sendInitAnnotations(DirectCallAPI.this.directCallInitResponse, CustomHandler.Init.ON_FAILURE, InitException.DirectCallSdkNotInitializedException);
                            }
                        }
                    });
        } catch (Exception e) {

        }
    }

    /**
     * clears all the values stored inside sharedPreference.
     *
     * @param logoutContext:- context of the activity from which this function is called.
     */
    public void logout(Context logoutContext) {
        try {
            SocketIOManager.resetSocketConfiguration();

            SharedPreferences sharedPreferences = StorageHelper.getPreferences(logoutContext);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_ACTIVE_SESSION);
            editor.remove(KEY_MISSED_CALL_RECEIVER_ACTIONS);
            editor.remove(KEY_MISSED_CALL_INITIATOR_ACTIONS);
            editor.remove(KEY_ACCESS_TOKEN);
            editor.remove(KEY_SECONDARY_BASE_URL);
            editor.remove(KEY_CONTACT_ID);
            editor.remove(KEY_SNA);
            editor.remove(KEY_JWT_TOKEN);
            editor.remove(KEY_CONTACT_NAME);
            editor.remove(KEY_CONTACT_CC);
            editor.remove(KEY_CONTACT_PHONE);
            editor.remove(KEY_ACCOUNT_ID);
            editor.remove(KEY_API_KEY);
            editor.remove(KEY_TEXT_COLOR);
            editor.remove(KEY_BG_COLOR);
            editor.remove(KEY_BRAND_LOGO);
            editor.remove(KEY_RINGTONE);
            editor.remove(KEY_CONTACT_CUID);
            editor.remove(KEY_CLEVERTAP_ID);
            editor.remove(KEY_JWT_TOKEN);
            editor.remove(KEY_TEMPLATE_PIN_VIEW_CONFIG);
            editor.remove(KEY_TEMPLATE_SCRATCHCARD_CONFIG);

            editor.remove(KEY_MISSED_CALL_INITIATOR_HOST);
            editor.remove(KEY_MISSED_CALL_RECEIVER_HOST);

            StorageHelper.persistImmediately(editor);

            jobScheduler = (JobScheduler) logoutContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }

        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while logging out from the SDK: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * called when the user is trying to make a call using phone and cc.
     *
     * @param activityContext:-      context of the activity from which this method is called.
     * @param callee:-               jsonObject containing phone and cc.
     * @param context:-              context of the call.
     * @param callOptions:-          jsonObject containig call options passed while making a call.
     * @param outgoingCallResponse:- statuses of the call to be returned while making a call i.e. success or failure.
     * @throws JSONException
     */
    public void call(final Context activityContext, final JSONObject callee, final String context, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) throws JSONException {
        sdkReady = false;
        final String[] PERMISSIONS = {
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.RECORD_AUDIO
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activityContext == null) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidActivityContextException, null);
                        return;
                    }
                    if (callOptions == null) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallOptionsRequiredException, null);
                        return;
                    }
                    if (Utils.getInstance().hasInternetAccess(activityContext)) {
                        if (Utils.getInstance().hasPermissions(activityContext, PERMISSIONS)) {
                            if (!Utils.getInstance().isNetworkBandwidthGood()) {
                                sdkReady = true;
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.BadNetworkException, null);
                            } else {
                                voipOrPstnCall(activityContext, callee, context, callOptions, outgoingCallResponse);
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MicrophonePermissionNotGrantedException, null);
                        }
                    } else {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.NoInternetException, null);
                    }
                } catch (Exception e) {
                    sdkReady = true;
                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while initiating the call: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void voipOrPstnCall(Context activityContext, JSONObject callee, final String context, JSONObject callOptions, OutgoingCallResponse outgoingCallResponse) {
        try {
            if (SocketIOManager.getIsUnAuthorized()) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CuidConnectedElsewhereException, null);
                return;
            }
            DataStore dataStore = DataStore.getInstance();
            JSONArray tags = null;
            if (callOptions.has("tags") && callOptions.get("tags") instanceof JSONArray) {
                tags = callOptions.getJSONArray("tags");
            }
            if (!callOptions.has("pstn")) {
                callOptions.put("pstn", false);
            }
            if (!callOptions.has("recording")) {
                callOptions.put("recording", false);
            }
            if (!callOptions.has("webhook")) {
                callOptions.put("webhook", "");
            }
            if (!callOptions.has("var1")) {
                callOptions.put("var1", "");
            }
            if (!callOptions.has("var2")) {
                callOptions.put("var2", "");
            }
            if (!callOptions.has("var3")) {
                callOptions.put("var3", "");
            }
            if (!callOptions.has("var4")) {
                callOptions.put("var4", "");
            }
            if (!callOptions.has("var5")) {
                callOptions.put("var5", "");
            }
            String webhook = callOptions.getString("webhook");
            if (dataStore.getSocket() == null) {
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.ClientDisconnectedDueToNetworkProblemException, null);
                sdkReady = true;
                return;
            }
            if (context == null || context.equals("")) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallContextRequiredException, null);
                return;
            }
            if (context.length() > 64) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallContextExceededBy64Exception, null);
                return;
            }
            String numberRegex = "\\d+";
            if (!callee.has("cc") || !callee.has("phone")) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.BothCcPhoneRequiredException, null);
                return;
            }
            if (callee.has("cc") &&
                    (callee.getString("cc").length() < 1 || callee.getString("cc").length() > 4 || !(callee.getString("cc").matches(numberRegex)))) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidCcLengthException, null);
                return;
            }
            if (callee.has("phone") &&
                    ((callee.getString("phone").length() < 6) || callee.getString("phone").length() > 20 || !(callee.getString("phone").matches(numberRegex)))) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidPhoneNumberLengthException, null);
                return;
            }
            if (tags != null) {
                if (tags.length() > 0) {
                    if (tags.length() > 10) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.TagCountExceededBy10Exception, null);
                        return;
                    }
                    for (int i = 0; i < tags.length(); i++) {
                        if (tags.getString(i).length() > 32) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.TagLengthExceededBy32Exception, null);
                            return;
                        }
                    }
                }
            }
            if (webhook.length() > 0) {
                try {
                    new URL(webhook).toURI();
                } catch (Exception e) {
                    sdkReady = true;
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidWebhookException, null);
                    return;
                }
            }
            if (callOptions.getString("var1").length() > 128
                    || callOptions.getString("var2").length() > 128
                    || callOptions.getString("var3").length() > 128
                    || callOptions.getString("var4").length() > 128
                    || callOptions.getString("var5").length() > 128) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.VarLengthExceededBy128Exception, null);
                return;
            }
            SharedPreferences sharedPref = StorageHelper.getPreferences(activityContext);
            if (sharedPref.getString(KEY_ECTA, null).equals("true")) {
                if (callOptions.has("callToken") && callOptions.getString("callToken") != null && callOptions.getString("callToken") != "") {
                } else {
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallTokenExpectedException, null);
                    sdkReady = true;
                    return;
                }
            }
            String cc = sharedPref.getString(KEY_CONTACT_CC, "");
            String phone = sharedPref.getString(KEY_CONTACT_PHONE, "");
            String userCid = sharedPref.getString(KEY_CONTACT_CUID, "");
            if ((callOptions.getBoolean("pstn") ||
                    (callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")))
                    && userCid.length() > 0) {
                if (cc.length() == 0 && phone.length() == 0) {
                    if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.EmptyVerifiedCliListException, null);
                        return;
                    } else if (callOptions.has("cli")) {
                        JSONObject cliJson = callOptions.getJSONObject("cli");
                        if (!cliJson.has("cc") || !cliJson.has("phone")) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MissingCcPhoneInCliException, null);
                            return;
                        }
                        String cliCC = cliJson.getString("cc");
                        String cliPhone = cliJson.getString("phone");
                        if (cliCC.length() > 0 && cliPhone.length() > 0) {
                            if (isPassedCliAuthorized(activityContext, cliJson)) {
                                DataStore.getInstance().setCli(cliJson);
                            } else {
                                sdkReady = true;
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.UnauthorizedCliException, null);
                                return;
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidLengthOfCcOrPhoneInCliException, null);
                            return;
                        }
                    } else {
                        DataStore.getInstance().setCli(getDefaultCli(activityContext, 0));
                    }
                } else {
                    if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.EmptyVerifiedCliListException, null);
                        return;
                    } else if (callOptions.has("cli")) {
                        JSONObject cliJson = callOptions.getJSONObject("cli");
                        if (!cliJson.has("cc") || !cliJson.has("phone")) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MissingCcPhoneInCliException, null);
                            return;
                        }
                        String cliCC = cliJson.getString("cc");
                        String cliPhone = cliJson.getString("phone");
                        if (cliCC.length() > 0 && cliPhone.length() > 0) {
                            if (isPassedCliAuthorized(activityContext, cliJson)) {
                                DataStore.getInstance().setCli(cliJson);
                            } else {
                                sdkReady = true;
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.UnauthorizedCliException, null);
                                return;
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidLengthOfCcOrPhoneInCliException, null);
                            return;
                        }
                    } else {
                        DataStore.getInstance().setCli(getDefaultCli(activityContext, 0));
                    }
                }
            }

            dataStore.setOutgoingCallResponse(outgoingCallResponse);
            jobSchedulerSocketService = JobSchedulerSocketService.getInstance(activityContext);
            if (SocketIOManager.isSocketConnected()) {
                jobSchedulerSocketService.makeCall(callee.getString("cc"), callee.getString("phone"), "", context, callOptions, outgoingCallResponse);
            } else {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.ClientDisconnectedDueToNetworkProblemException, null);
            }
        } catch (Exception e) {
            sdkReady = true;
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while initiating the call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * called when the user is trying to make a call using cuid.
     *
     * @param activityContext:-      context of the activity from which user is trying to make a call.
     * @param calleeCuid:-           cuid of the user.
     * @param context:-              context of the call.
     * @param callOptions:-          jsonObject containing call options passed while making a call.
     * @param outgoingCallResponse:- statuses of the call to be returned while making a call i.e. success or failure.
     * @throws JSONException
     */

    public void call(final Context activityContext, final String calleeCuid, final String context, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) {
        sdkReady = false;
        final String[] PERMISSIONS = {
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.RECORD_AUDIO
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activityContext == null) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidActivityContextException, null);
                        return;
                    }
                    if (callOptions == null) {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallOptionsRequiredException, null);
                        return;
                    }
                    if (Utils.getInstance().hasInternetAccess(activityContext)) {
                        if (Utils.getInstance().hasPermissions(activityContext, PERMISSIONS)) {
                            if (!Utils.getInstance().isNetworkBandwidthGood()) {
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_BAD_NETWORK);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.BadNetworkException, null);
                            } else {
                                if (SocketIOManager.getIsUnAuthorized()) {
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CuidConnectedElsewhereException, null);
                                    sdkReady = true;
                                    return;
                                }
                                DataStore dataStore = DataStore.getInstance();
                                JSONArray tags = null;
                                if (callOptions.has("tags") && callOptions.get("tags") instanceof JSONArray) {
                                    tags = callOptions.getJSONArray("tags");
                                }
                                if (!callOptions.has("pstn")) {
                                    callOptions.put("pstn", false);
                                }
                                if (!callOptions.has("recording")) {
                                    callOptions.put("recording", false);
                                }
                                if (!callOptions.has("webhook")) {
                                    callOptions.put("webhook", "");
                                }
                                if (!callOptions.has("var1")) {
                                    callOptions.put("var1", "");
                                }
                                if (!callOptions.has("var2")) {
                                    callOptions.put("var2", "");
                                }
                                if (!callOptions.has("var3")) {
                                    callOptions.put("var3", "");
                                }
                                if (!callOptions.has("var4")) {
                                    callOptions.put("var4", "");
                                }
                                if (!callOptions.has("var5")) {
                                    callOptions.put("var5", "");
                                }
                                String webhook = callOptions.getString("webhook");
                                if (dataStore.getSocket() == null) {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.ClientDisconnectedDueToNetworkProblemException, null);
                                    return;
                                }
                                if (context == null || context.equals("")) {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallContextRequiredException, null);
                                    return;
                                }
                                if (context.length() > 64) {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallContextExceededBy64Exception, null);
                                    return;
                                }
                                if (calleeCuid == null || calleeCuid.length() == 0 || calleeCuid.trim().length() == 0) {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidCalleeCuidException, null);
                                    return;
                                }
                                if (tags != null) {
                                    if (tags.length() > 0) {
                                        if (tags.length() > 10) {
                                            sdkReady = true;
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.TagCountExceededBy10Exception, null);
                                            return;
                                        }
                                        for (int i = 0; i < tags.length(); i++) {
                                            if (tags.getString(i).length() > 32) {
                                                sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.TagLengthExceededBy32Exception, null);
                                                return;
                                            }
                                        }
                                    }
                                }
                                if (webhook.length() > 0) {
                                    try {
                                        new URL(webhook).toURI();
                                    } catch (Exception e) {
                                        sdkReady = true;
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidWebhookException, null);
                                        return;
                                    }
                                }
                                if (callOptions.getString("var1").length() > 128
                                        || callOptions.getString("var2").length() > 128
                                        || callOptions.getString("var3").length() > 128
                                        || callOptions.getString("var4").length() > 128
                                        || callOptions.getString("var5").length() > 128) {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.VarLengthExceededBy128Exception, null);
                                    return;
                                }
                                SharedPreferences sharedPref = StorageHelper.getPreferences(activityContext);
                                if (sharedPref.getString(KEY_ECTA, null) != null &&
                                        sharedPref.getString(KEY_ECTA, null).equals("true")) {
                                    if (callOptions.has("callToken") && !callOptions.getString("callToken").equals("")) {
                                    } else {
                                        sdkReady = true;
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CallTokenExpectedException, null);
                                        return;
                                    }
                                }
                                String cc = sharedPref.getString(KEY_CONTACT_CC, "");
                                String phone = sharedPref.getString(KEY_CONTACT_PHONE, "");
                                String userCid = sharedPref.getString(KEY_CONTACT_CUID, "");
                                if (userCid.equals(calleeCuid)) {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CanNotCallSelfException, null);
                                    return;
                                }
                                if ((callOptions.getBoolean("pstn") || (callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")) && userCid.length() > 0)) {
                                    if (cc.length() == 0 && phone.length() == 0) {

                                        if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                                            sdkReady = true;
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.EmptyVerifiedCliListException, null);
                                            return;
                                        } else if (callOptions.has("cli")) {
                                            JSONObject cliJson = callOptions.getJSONObject("cli");
                                            if (!cliJson.has("cc") || !cliJson.has("phone")) {
                                                sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MissingCcPhoneInCliException, null);
                                                return;
                                            }
                                            String cliCC = cliJson.getString("cc");
                                            String cliPhone = cliJson.getString("phone");
                                            if (cliCC.length() > 0 && cliPhone.length() > 0) {
                                                if (isPassedCliAuthorized(activityContext, cliJson)) {
                                                    DataStore.getInstance().setCli(cliJson);
                                                } else {
                                                    sdkReady = true;
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.UnauthorizedCliException, null);
                                                    return;
                                                }
                                            } else {
                                                sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidLengthOfCcOrPhoneInCliException, null);
                                                return;
                                            }
                                        } else {
                                            DataStore.getInstance().setCli(getDefaultCli(activityContext, 0));
                                        }
                                    } else {
                                        if (!(Objects.requireNonNull(getVerifiedNumbers(activityContext)).size() > 0)) {
                                            sdkReady = true;
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.EmptyVerifiedCliListException, null);
                                            return;
                                        } else if (callOptions.has("cli")) {
                                            JSONObject cliJson = callOptions.getJSONObject("cli");
                                            if (!cliJson.has("cc") || !cliJson.has("phone")) {
                                                sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MissingCcPhoneInCliException, null);
                                                return;
                                            }
                                            String cliCC = cliJson.getString("cc");
                                            String cliPhone = cliJson.getString("phone");
                                            if (cliCC.length() > 0 && cliPhone.length() > 0) {
                                                if (isPassedCliAuthorized(activityContext, cliJson)) {
                                                    DataStore.getInstance().setCli(cliJson);
                                                } else {
                                                    sdkReady = true;
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.UnauthorizedCliException, null);
                                                    return;
                                                }
                                            } else {
                                                sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidLengthOfCcOrPhoneInCliException, null);
                                                return;
                                            }
                                        } else {
                                            DataStore.getInstance().setCli(getDefaultCli(activityContext, 0));
                                        }
                                    }
                                }

                                dataStore.setOutgoingCallResponse(outgoingCallResponse);
                                jobSchedulerSocketService = JobSchedulerSocketService.getInstance(activityContext);
                                if (SocketIOManager.isSocketConnected()) {
                                    jobSchedulerSocketService.makeCall("", "", calleeCuid, context, callOptions, outgoingCallResponse);
                                } else {
                                    sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.ClientDisconnectedDueToNetworkProblemException, null);
                                }
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MicrophonePermissionNotGrantedException, null);
                        }
                    } else {
                        sdkReady = true;
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.NoInternetException, null);
                    }
                } catch (Exception e) {
                    sdkReady = true;
                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while initiating the call: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private List<Cli> getVerifiedNumbers(Context activityContext) {
        try {
            SharedPreferences sharedPref = StorageHelper.getPreferences(activityContext);
            String json = sharedPref.getString(KEY_CLI_LIST, null);
            JSONArray verifiedNumbers = new JSONArray(json);
            List<Cli> cliList = new ArrayList<>();
            for (int index = 0; index < verifiedNumbers.length(); index++) {
                JSONObject cli = verifiedNumbers.getJSONObject(index);
                cliList.add(new Cli(cli.getString(KEY_CONTACT_CC), cli.getString(KEY_CONTACT_PHONE)));
            }
            return cliList;
        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while getting the verified CLIs: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getDefaultCli(Context context, int defaultIndex) {
        try {
            List<Cli> cliList = getVerifiedNumbers(context);
            if(cliList != null && cliList.size() > 0){
                Collections.shuffle(cliList);
                return cliList.get(defaultIndex).toJson();
            }
        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Exception while getting the default CLI: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Boolean isPassedCliAuthorized(Context context, JSONObject jsonObject) {
        boolean isCliAuthorized = false;
        try {
            for (int index = 0; index < getVerifiedNumbers(context).size(); index++) {
                JSONObject jsonObject1 = getDefaultCli(context, index);
                if (jsonObject.has("cc") && jsonObject1 != null && jsonObject1.has("cc")) {
                    if (jsonObject.getString("cc").equals(jsonObject1.getString("cc"))) {
                        if (jsonObject.has("phone") && jsonObject1.has("phone")) {
                            if (jsonObject.getString("phone").equals(jsonObject1.getString("phone"))) {
                                isCliAuthorized = true;
                            }
                        }
                    }
                }
            }
            return isCliAuthorized;
        } catch (Exception e) {
            return isCliAuthorized;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void handleFcmMessage(@NonNull final Context context,
                                 @NonNull final Bundle incomingCallData) {
        try {

            logger.debug(CALLING_LOG_TAG_SUFFIX, "VoIP Push Handling");

            //resetting fcm cache
            FcmCacheManger.getInstance().resetFcmCache();

            final boolean isValid = incomingCallData.containsKey("source") && incomingCallData.get("source").equals("directcall");
            if (isValid) {

                if (incomingCallData.containsKey("action")) {
                    String action = incomingCallData.getString("action");
                    FcmUtil.getInstance(context).resetExpiredCallId(null);

                    if (incomingCallData.containsKey("payload")) {
                        JSONObject incomingdata = new JSONObject(incomingCallData.getString("payload"));
                        switch (action) {
                            case ACTION_INCOMING_CALL:
                                startFcmService(context, incomingdata.toString(), ACTION_INCOMING_CALL);
                                break;
                            case ACTION_CANCEL_CALL:
                                if (incomingdata.has("callId") && incomingdata.getString("callId") != null) {
                                    startFcmService(context, incomingdata.toString(), ACTION_CANCEL_CALL);
                                }
                                break;
                            /*case "notification":
                                startFcmService(context, incomingdata.toString(), ACTION_FCM_NOTIFICATION_RECEIVED);
                                break;*/
                        }
                    }
                }

            }
        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "Failed to parse the incoming call payload: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void startFcmService(final Context context, final String data, final String actionType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE); //<-----  this is mandatory statement
                if (jobScheduler != null) {
                    jobScheduler.cancelAll();
                }

                Intent intent = new Intent(context, FcmSigSockService.class);
                intent.setAction(actionType);
                intent.putExtra(Constants.FCM_NOTIFICATION_PAYLOAD, data);
                context.startService(intent);
            }
        }).start();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private void registerNewFcmToken(final Context context, final String fcmToken) {
        try {
            String session = StorageHelper.getString(context, KEY_ACTIVE_SESSION, null);
            final String contactId = StorageHelper.getString(context, KEY_CONTACT_ID, null);

            if (session != null && contactId != null) {
                CleverTapAPI cleverTapAPI = getCleverTapApi();
                String authToken = StorageHelper.getString(context, KEY_ACCESS_TOKEN, null);
                String baseUrl = StorageHelper.getString(context, KEY_SECONDARY_BASE_URL, null);
                int retryDelay = StorageHelper.getInt(context, KEY_RETRY_DELAY, HTTP_RETRY_DELAY);

                if (cleverTapAPI != null && authToken != null && baseUrl != null) {
                    JSONObject contactDeviceId = new JSONObject();
                    contactDeviceId.put(KEY_DEVICE_ID, fcmToken);
                    new Http.Request(cleverTapAPI, HttpMethod.GET)
                            .url(baseUrl + ApiEndPoints.PATCH_UPDATE_DEVICE_ID)
                            .header(KEY_AUTHORIZATION_HEADER, authToken)
                            .pathParameter("id", contactId)
                            .body(contactDeviceId)
                            .withBackoffCriteria(HTTP_MAX_RETRIES, retryDelay, TimeUnit.SECONDS)
                            .backoffCriteriaFailedListener(DirectCallAPI.getInstance().getBackoffCriteriaFailedListener())
                            .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                            .enableLog(ENABLE_HTTP_LOG)
                            .execute(new StringListener() {
                                @Override
                                public void onResponse(@Nullable String response, int responseCode, Boolean isSuccessful) {
                                    if (isSuccessful) {
                                        //token is updated for the contact so updating the same in local sharedPrefs
                                        StorageHelper.putString(context, KEY_FCM_TOKEN, fcmToken);
                                    } else {
                                        logger.debug(CALLING_LOG_TAG_SUFFIX, "Failed to update the new token in the contact details, will try again later");
                                    }
                                }

                                @Override
                                public void onFailure(@Nullable Exception e) {
                                    logger.debug(CALLING_LOG_TAG_SUFFIX, "Failed to update the new token in the contact details, will try again later");
                                }
                            });
                }
            }
        } catch (Exception e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "FCM registration with new token is failed: " + e.getLocalizedMessage());
        }
    }

    public void init(Context context,
                     DirectCallInitOptions initOptions,
                     CleverTapAPI cleverTapAPI,
                     DirectCallInitResponse directCallInitResponse) {

        if (cleverTapAPI == null) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "cleverTapAPI instance can't be null to initialize the SDK");
            return;
        }

        String CTAccountId = cleverTapAPI.getAccountId();
        String CTAccountToken = cleverTapAPI.getCoreState().getConfig().getAccountToken();

        if (CTAccountId == null || CTAccountToken == null) {
            logger.debug(
                    "Account ID or Account token is missing from AndroidManifest.xml, unable to initialise Direct Call SDK");
            return;
        }

        storeCleverTapAccountDetails(context, CTAccountId, CTAccountToken);

        String fcmToken = cleverTapAPI.getDevicePushToken(PushConstants.PushType.FCM);
        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.debug(CALLING_LOG_TAG_SUFFIX, "fcmToken is null, can't initialize the SDK");
            return;
        }

        logger.debug(CALLING_LOG_TAG_SUFFIX, "fcmToken is available: " + fcmToken);

        setCleverTapApi(cleverTapAPI);

        CleverTapAPI.setDirectCallNotificationHandler(new DirectCallNotificationHandler());

        JSONObject initJson = initOptions.getInitJson();
        try {
//                initJson.put("accountId", accountId);
//                initJson.put("apikey", accountToken);
            initJson.put("fcmToken", fcmToken);

            cleverTapAPI.getCleverTapID(cleverTapID -> {
                try {
                    initJson.put(KEY_CLEVERTAP_ID, cleverTapID);
                    initJson.put(KEY_CLEVERTAP_ACCOUNT_ID, CTAccountId);
                    initJson.put(KEY_CLEVERTAP_API_KEY, CTAccountToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

            init(context, initJson, initOptions, directCallInitResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void storeCleverTapAccountDetails(Context context, String CTAccountId, String CTAccountToken) {
        StorageHelper.putString(context, KEY_CLEVERTAP_ACCOUNT_ID, CTAccountId);
        StorageHelper.putString(context, KEY_CLEVERTAP_API_KEY, CTAccountToken);
    }

    private void reinitializeSdk() {
        DirectCallAPI directCallAPIInstance = DirectCallAPI.getInstance();
        if(directCallAPIInstance != null){
            Context context = directCallAPIInstance.context;

            String accountId = DataStore.getInstance().getAccountId();
            String apiKey = DataStore.getInstance().getApikey();

            if(accountId == null || apiKey == null){
                getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Account credentials are not available to reinitialize the SDK");
            }
            //1. reset/clear the session
            setEnabled(false);
            directCallAPIInstance.logout(context);
            directCallAPIInstance.getBaseUrl(directCallAPIInstance.context, accountId, apiKey);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public CleverTapAPI getCleverTapApi() {
        return cleverTapAPI;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setCleverTapApi(CleverTapAPI cleverTapAPI) {
        this.cleverTapAPI = cleverTapAPI;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String getAccountId(){
        return DataStore.getInstance().getAccountId();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String getAccountToken(){
        return DataStore.getInstance().getApikey();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void pushDCSystemEvent(CTSystemEvent systemEvent,
                                  DCSystemEventInfo dcSystemEventInfo) {

        Context context = DataStore.getInstance().getAppContext();
        if(context == null) {
            return;
        }

        String ctAccountID = StorageHelper.getString(context, KEY_CLEVERTAP_ACCOUNT_ID, null);
        String dcAccountID = StorageHelper.getString(context, KEY_ACCOUNT_ID, null);

        if(systemEvent == null || ctAccountID == null || dcAccountID == null){
            logger.debug(CALLING_LOG_TAG_SUFFIX,
                    "DC system event details are null! dropping further processing");
            return;
        }

        JSONObject eventProperties = new JSONObject();
        try {
            eventProperties.put("ct_account_id", ctAccountID);
            eventProperties.put("dc_account_id", dcAccountID);
            eventProperties.put("guid", StorageHelper.getString(context, KEY_CLEVERTAP_ID, null));
            eventProperties.put("cuid", StorageHelper.getString(context, KEY_CONTACT_CUID, null));
            eventProperties.put("call_id", dcSystemEventInfo.getCallId());
            eventProperties.put("context", dcSystemEventInfo.getCallContext());
            eventProperties.put("epoch", Utils.getInstance().getNow());

            if(systemEvent.equals(CTSystemEvent.DC_END)){
                String callStatus = dcSystemEventInfo.getCallStatus().getName();
                eventProperties.put("call_status", callStatus);

                if(callStatus.equals(CallStatus.CALL_DECLINED.getName())) {
                    CallStatus.DeclineReason declineReason = dcSystemEventInfo.getDeclineReason();
                    if (declineReason != null) {
                        eventProperties.put("reason", declineReason.getName());
                    }
                }
            }
        } catch (JSONException e) {
            logger.debug(CALLING_LOG_TAG_SUFFIX,
                    "Problem process push system event! dropping further processing");
            e.printStackTrace();
        }

        PushDirectCallEventTask pushDirectCallEventTask = new PushDirectCallEventTask(context, systemEvent, eventProperties);

        logger.debug(CALLING_LOG_TAG_SUFFIX, "Raising the system event for VoIP call: \n" + eventProperties);

        CTDirectCallTaskManager.getInstance().postAsyncSafely("ProcessDirectCallEvents",
                pushDirectCallEventTask);

    }

    public interface MissedCallNotificationOpenedHandler {
        /**
         * Fires when a user taps on a notification.
         *
         * @param result a {@link MissedCallNotificationOpenResult} with the user's response and properties of this notification
         */
        void onMissedCallNotificationOpened(Context context, MissedCallNotificationOpenResult result);
    }

    private void addPinViewTextObserver(PinViewObservable.PinViewTextObserver pinViewTextObserver) {
        PinViewObservable.getInstance().setPinviewTextObserver(pinViewTextObserver);
    }
}
