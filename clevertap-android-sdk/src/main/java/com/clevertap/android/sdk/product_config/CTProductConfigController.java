package com.clevertap.android.sdk.product_config;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONException;
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

    private void initAsync() {
        if (TextUtils.isEmpty(guid))
            return;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String content = Utils.readFromFile(context, getActivatedFullPath());
                //apply default config first
                if (defaultConfig != null && !defaultConfig.isEmpty()) {
                    activatedConfig.putAll(defaultConfig);
                }
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
                        isInitialized = true;
                        Utils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onActivateCompleted();
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void setDefaults(int resourceID) {
        defaultConfig = DefaultXmlParser.getDefaultsFromXml(context, resourceID);
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
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
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
            protected void onPostExecute(Boolean isSuccess) {
                super.onPostExecute(isSuccess);
                if (isSuccess) {
                    listener.onActivateCompleted();
                }
            }
        };
        asyncTask.execute();

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
        return true;
    }

    public void afterFetchProductConfig(JSONObject kvResponse) {
        if (kvResponse != null) {
            try {
                Utils.writeJsonToFile(context, getFetchedDirName(), getFetchedFileName(), kvResponse);
                Utils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFetchCompleted();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void resetWithGuid(String cleverTapID) {
        this.guid = cleverTapID;
    }

    private String getActivatedFileName() {
        return CTProductConfigConstants.FILE_NAME_ACTIVATED + ".json";
    }

    private String getActivatedDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getFetchedDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }


    private String getFetchedFileName() {
        return CTProductConfigConstants.FILE_NAME_FETCHED + ".json";
    }

    private String getFetchedFullPath() {
        return getFetchedDirName() + "/" + getFetchedFileName();
    }

    private String getActivatedFullPath() {
        return getActivatedDirName() + "/" + getActivatedFileName();
    }
    public interface Listener{
        void fetchProductConfig();

        void onFetchCompleted();

        void onActivateCompleted();
    }
}