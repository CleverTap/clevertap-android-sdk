/*
 * Copyright 2022, CleverTap, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.clevertap.android.sdk.feat_variable.extras;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.clevertap.android.sdk.feat_variable.extras.Constants.Defaults;
import com.clevertap.android.sdk.feat_variable.mock.LPClassesMock;

import java.util.Map;


public class APIConfig {
  private static final String TAG = "ApiConfig>";
  private static final APIConfig INSTANCE = new APIConfig();

  // non persistable data
  private String appId;
  private String accessKey;
  private String deviceId;
  private String userId;

  // persistable data

  // The token is saved primarily for legacy SharedPreferences decryption. This could
  // likely be removed in the future.
  private String token;
  private String apiHost = "api.leanplum.com";
  private String apiPath = "api";
  private boolean apiSSL = true;
  private String socketHost = "dev.leanplum.com";
  private int socketPort = 443;

  @VisibleForTesting
  APIConfig() {
    load();
  }

  public static APIConfig getInstance() {
    return INSTANCE;
  }

  public void setAppId(String appId, String accessKey) {
    if (!TextUtils.isEmpty(appId)) {
      this.appId = appId.trim();
    }
    if (!TextUtils.isEmpty(accessKey)) {
      this.accessKey = accessKey.trim();
    }
  }

  public String appId() {
    return appId;
  }

  public String accessKey() {
    return accessKey;
  }

  public String deviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String userId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String token() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
    save();
  }

  private void load() {
    Context context = LPClassesMock.getContext();
    if (context == null) {
      Log.e(TAG,"Leanplum context is null. Please call Leanplum.setApplicationContext(context) "
          + "before anything else.");
      return;
    }
    SharedPreferences defaults = context.getSharedPreferences(
        Defaults.LEANPLUM, Context.MODE_PRIVATE);

    String token = defaults.getString(Defaults.TOKEN_KEY, null);
    if (token != null) {
      this.token = token;
    }
    String apiHost = defaults.getString(Defaults.API_HOST_KEY, null);
    if (apiHost != null) {
      this.apiHost = apiHost;
    }
    String apiPath = defaults.getString(Defaults.API_PATH_KEY, null);
    if (apiPath != null) {
      this.apiPath = apiPath;
    }
    this.apiSSL = defaults.getBoolean(Defaults.API_SSL_KEY, true);
    String socketHost = defaults.getString(Defaults.SOCKET_HOST_KEY, null);
    if (socketHost != null) {
      this.socketHost = socketHost;
    }
    int socketPort = defaults.getInt(Defaults.SOCKET_PORT_KEY, 0);
    if (socketPort != 0) {
      this.socketPort = socketPort;
    }
  }

  public void save() {
    Context context = LPClassesMock.getContext();
    if (context == null) {
      Log.e(TAG,"Leanplum context is null. Please call Leanplum.setApplicationContext(context) "
          + "before anything else.");
      return;
    }
    SharedPreferences defaults = context.getSharedPreferences(
        Defaults.LEANPLUM, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();

    editor.putString(Defaults.TOKEN_KEY, this.token);
    editor.putString(Defaults.API_HOST_KEY, this.apiHost);
    editor.putString(Defaults.API_PATH_KEY, this.apiPath);
    editor.putBoolean(Defaults.API_SSL_KEY, this.apiSSL);
    editor.putString(Defaults.SOCKET_HOST_KEY, this.socketHost);
    editor.putInt(Defaults.SOCKET_PORT_KEY, this.socketPort);

    SharedPreferencesUtil.commitChanges(editor);
  }

  public boolean attachApiKeys(Map<String, Object> dict) {
    if (appId == null || accessKey == null) {
      Log.e(TAG,"API keys are not set. Please use Leanplum.setAppIdForDevelopmentMode or "
          + "Leanplum.setAppIdForProductionMode.");
      return false;
    }
    dict.put(Constants.Params.APP_ID, appId);
    dict.put(Constants.Params.CLIENT_KEY, accessKey);
    dict.put(Constants.Params.CLIENT, Constants.CLIENT);
    return true;
  }

  public void setApiConfig(String apiHost, String apiPath, boolean apiSSL) {
    if (!TextUtils.isEmpty(apiHost)) {
      this.apiHost = apiHost;
    }
    if (!TextUtils.isEmpty(apiPath)) {
      this.apiPath = apiPath;
    }
    this.apiSSL = apiSSL;
    save();
  }

  public void setSocketConfig(String socketHost, int socketPort) {
    if (!TextUtils.isEmpty(socketHost)) {
      this.socketHost = socketHost;
      this.socketPort = socketPort;
      save();
    }
  }

  public String getApiHost() {
    return this.apiHost;
  }

  public String getApiPath() {
    return this.apiPath;
  }

  public String getSocketHost() {
    return this.socketHost;
  }

  public int getSocketPort() {
    return this.socketPort;
  }

  public boolean getApiSSL() {
    return this.apiSSL;
  }
}
