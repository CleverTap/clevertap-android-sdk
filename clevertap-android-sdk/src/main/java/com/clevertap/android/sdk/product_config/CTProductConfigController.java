package com.clevertap.android.sdk.product_config;

import android.content.Context;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.TaskManager;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public final class CTProductConfigController {
    private String guid;

    public boolean isInitialized() {
        return isInitialized;
    }

    private boolean isInitialized = false;
    private final CleverTapInstanceConfig config;
    private final Context context;
    private HashMap<String, String> defaultConfig;
    private final HashMap<String, String> activatedConfig = new HashMap<>();
    private final Listener listener;
    private long minFetchIntervalInSeconds;

    public CTProductConfigController(Context context, String guid, CleverTapInstanceConfig config, Listener listener) {
        this.context = context;
        this.guid = guid;
        this.config = config;
        this.listener = listener;
        initAsync();
    }

    private synchronized void initAsync() {
        if (TextUtils.isEmpty(guid))
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void params) {
                activatedConfig.clear();
                String content = Utils.readFromFile(context, getActivatedFullPath());
                //apply default config first
                if (defaultConfig != null && !defaultConfig.isEmpty()) {
                    activatedConfig.putAll(defaultConfig);
                }
                if (!TextUtils.isEmpty(content)) {
                    try {
                        JSONObject jsonObject = new JSONObject(content);
                        Iterator<String> iterator = jsonObject.keys();
                        while (iterator.hasNext()) {

                            String key = iterator.next();
                            if (!TextUtils.isEmpty(key)) {
                                String value = String.valueOf(jsonObject.get(key));
                                activatedConfig.put(key, value);
                            }
                        }
                        isInitialized = true;
                        Utils.writeJsonToFile(context, getActivatedDirName(), getActivatedFileName(), new JSONObject(activatedConfig));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isInitSuccess) {
                if (isInitSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Init Success");
                    listener.onInitSuccess();
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Init Failed");
                    listener.onInitFailed();
                }
            }
        });
    }

    public void setDefaults(int resourceID) {
        defaultConfig = DefaultXmlParser.getDefaultsFromXml(context, resourceID);
        initAsync();
    }

    public void fetch() {
        fetch(minFetchIntervalInSeconds);
    }

    public void fetch(long minimumFetchIntervalInSeconds) {
        if (canRequest()) {
            listener.fetchProductConfig();
        }
    }

    public void activate() {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void params) {
                activatedConfig.clear();
                //apply default config first
                if (defaultConfig != null && !defaultConfig.isEmpty()) {
                    activatedConfig.putAll(defaultConfig);
                }
                //read fetched info
                String content = Utils.readFromFile(context, getFetchedFullPath());
                if (!TextUtils.isEmpty(content)) {
                    try {
                        JSONObject jsonObject = new JSONObject(content);
                        JSONArray kvArray = jsonObject.getJSONArray(Constants.KEY_KV);

                        if (kvArray != null && kvArray.length() > 0) {
                            for (int i = 0; i < kvArray.length(); i++) {
                                JSONObject object = (JSONObject) kvArray.get(i);
                                if (object != null) {
                                    Iterator<String> iterator = object.keys();
                                    while (iterator.hasNext()) {
                                        String key = iterator.next();
                                        if (!TextUtils.isEmpty(key)) {
                                            String value = String.valueOf(object.get(key));
                                            activatedConfig.put(key, value);
                                        }
                                    }
                                }
                            }
                        }

                        Utils.writeJsonToFile(context, getActivatedDirName(), getActivatedFileName(), new JSONObject(activatedConfig));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isSuccess) {
                if (isSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Activate Success");
                    listener.onActivateSuccess();
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Activate Failed");
                    listener.onActivateFailed();
                }
            }
        });
    }

    public void setMinimumFetchIntervalInSeconds(long fetchIntervalInSeconds) {
        this.minFetchIntervalInSeconds = fetchIntervalInSeconds;
    }

    public String getString(String Key) {
        return defaultConfig.get(Key);
    }

    public boolean getBoolean(String Key) {
        return Boolean.parseBoolean(defaultConfig.get(Key));
    }

    private boolean canRequest() {
        //TODO throttling logic
        return !TextUtils.isEmpty(guid);
    }

    public void afterFetchProductConfig(JSONObject kvResponse) {
        if (kvResponse != null) {
            try {
                Utils.writeJsonToFile(context, getFetchedDirName(), getFetchedFileName(), kvResponse);
                Utils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        config.getLogger().verbose(config.getAccountId(), "fetch Success");
                        listener.onFetchSuccess();
                    }
                });

            } catch (Exception e) {
                config.getLogger().verbose(config.getAccountId(), "fetch Failed");
                listener.onFetchFailed();
                e.printStackTrace();
            }
        }
    }

    public void setGuidAndInit(String cleverTapID) {
        if (TextUtils.isEmpty(guid))
            return;
        this.guid = cleverTapID;
        initAsync();
    }

    private String getActivatedFileName() {
        return CTProductConfigConstants.FILE_NAME_ACTIVATED;
    }

    private String getActivatedDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getFetchedDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }


    private String getFetchedFileName() {
        return CTProductConfigConstants.FILE_NAME_FETCHED;
    }

    private String getFetchedFullPath() {
        return getFetchedDirName() + "/" + getFetchedFileName();
    }

    private String getActivatedFullPath() {
        return getActivatedDirName() + "/" + getActivatedFileName();
    }

    public interface Listener {
        void onInitSuccess();

        void onInitFailed();

        void fetchProductConfig();

        void onFetchSuccess();

        void onFetchFailed();

        void onActivateSuccess();

        void onActivateFailed();
    }
}