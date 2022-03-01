package com.clevertap.android.directcall.network;

import static com.clevertap.android.directcall.network.HttpConstants.APPLICATION_JSON;
import static com.clevertap.android.directcall.network.HttpConstants.BUFFER_SIZE;
import static com.clevertap.android.directcall.network.HttpConstants.CONTENT_TYPE;
import static com.clevertap.android.directcall.network.HttpConstants.DEFAULT_CONNECT_TIMEOUT;
import static com.clevertap.android.directcall.network.HttpConstants.DEFAULT_READ_TIMEOUT;
import static com.clevertap.android.directcall.network.HttpConstants.TEXT_PLAIN;
import static com.clevertap.android.directcall.network.HttpConstants.UTF_8;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public final class Http {
    public static final String TAG = "HttpNetworking";

    private static long reqTimeStamp;
    public static final Http INSTANCE;

    private Http() {
    }

    static {
        INSTANCE = new Http();
    }

    public interface BackoffCriteriaFailedListener {
        void onBackoffCriteriaFailed();
    }

    public static final class Request {
        private final Map<String, String> queryParameter;
        private final Map<String, String> pathParameter;
        private final Map<String, String> header;
        @Nullable
        private String uri;

        private int connectTimeout; //default value is 0
        private int readTimeout; //default value is 0

        private int maxRetries = 0;
        private int retryDelayMillis = 0;
        private int pendingRetryAttempt = 0;
        private boolean backoffEnabled = false;
        private Integer[] responseCodesToExcludeForBackoff = new Integer[]{};
        private BackoffCriteriaFailedListener backoffCriteriaFailedListener;

        private StringListener stringListener;
        private JSONObjectListener jsonObjReqListener;
        private JSONArrayListener jsonArrayRequestListener;
        @Nullable
        private byte[] body;
        private boolean loggingEnabled = false;
        private final String method;
        @NonNull
        private final CleverTapAPI cleverTapAPI;

        public Request(@NonNull CleverTapAPI cleverTapAPI,
                       @NonNull @HttpMethod String method) {
            super();
            this.cleverTapAPI = cleverTapAPI;
            this.method = method;
            this.queryParameter = new HashMap<>();
            this.pathParameter = new HashMap<>();
            this.header = new HashMap<>();
            this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            this.readTimeout = DEFAULT_READ_TIMEOUT;
        }

        public final Request enableLog(boolean enableLogging) {
            HttpLogger.INSTANCE.setLogsRequired(enableLogging);
            this.loggingEnabled = enableLogging;
            return this;
        }

        public final Request withBackoffCriteria(int maxRetries, long retryDelay, @NonNull TimeUnit unit){
            this.maxRetries = maxRetries;
            this.pendingRetryAttempt = maxRetries;
            this.backoffEnabled = true;
            this.retryDelayMillis = HttpUtils.INSTANCE.checkTimeoutDuration("retryDelay", retryDelay, unit);
            return this;
        }

        public final Request excludeResponseCodesToBackoff(Integer[] responseCodesSet){
            this.responseCodesToExcludeForBackoff = responseCodesSet;
            return this;
        }

        public final Request backoffCriteriaFailedListener(BackoffCriteriaFailedListener listener){
            this.backoffCriteriaFailedListener = listener;
            return this;
        }

        public final Request connectTimeout(long timeout, TimeUnit unit) {
            this.connectTimeout = HttpUtils.INSTANCE.checkTimeoutDuration("connect timeout", timeout, unit);
            return this;
        }

        public final Request readTimeout(long timeout, TimeUnit unit) {
            this.readTimeout = HttpUtils.INSTANCE.checkTimeoutDuration("read timeout", timeout, unit);
            return this;
        }

        public final Request url(@Nullable String uri) {
            this.uri = uri;
            return this;
        }

        public final Request queryParameter(@Nullable Map<String, String> queryMap) {
            this.queryParameter.putAll(queryMap);
            return this;
        }

        public final Request queryParameter(String key, String value) {
            this.queryParameter.put(key, value);
            return this;
        }

        public final Request pathParameter(@Nullable Map<String, String> pathParameterMap) {
            this.pathParameter.putAll(pathParameterMap);
            return this;
        }

        public final Request pathParameter(String key, String value) {
            this.pathParameter.put(key, value);
            return this;
        }

        public final Request body(@NonNull JSONObject json) {
            this.body(json.toString());
            this.header(CONTENT_TYPE, APPLICATION_JSON);
            return this;
        }

        public final Request body(@NonNull List jsonObjectList) {
            this.body(jsonObjectList.toString());
            this.header(CONTENT_TYPE, APPLICATION_JSON);
            return this;
        }

        public final Request body(@Nullable String textBody) {
            if (textBody == null) {
                this.body = null;
                return this;
            } else {
                this.header(CONTENT_TYPE, TEXT_PLAIN);
                try {
                    this.body = textBody.getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) { /* Should never happen */
                }
                return this;
            }
        }

        public final Request header(@Nullable Map<String, String> header) {
            if(header!=null){
                this.header.putAll(header);
            }
            return this;
        }

        public final Request header(@NonNull String key, @NonNull String value) {
            this.header.put(key, value);
            return this;
        }

        public final Request body(@Nullable byte[] rawBody) {
            if (rawBody == null) {
                this.body = null;
                return this;
            } else {
                this.body = rawBody;
                return this;
            }
        }

        public final Request execute(@NonNull StringListener cb) {
            reqTimeStamp = System.currentTimeMillis();
            this.stringListener = cb;
            executeRequestTask();
            return this;
        }

        public final Request execute(@NonNull JSONObjectListener cb) {
            reqTimeStamp = System.currentTimeMillis();
            this.jsonObjReqListener = cb;
            executeRequestTask();
            return this;
        }

        public final Request execute(@NonNull JSONArrayListener cb) {
            reqTimeStamp = System.currentTimeMillis();
            this.jsonArrayRequestListener = cb;
            executeRequestTask();
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        private void executeRequestTask() {
            CleverTapInstanceConfig config = cleverTapAPI.getCoreState().getConfig();

            Task<Void> requestTask = CTExecutorFactory.executors(config).ioTask();
            requestTask.execute("networkCall", new RequestTask(this));
        }

        public final void sendResponse(@Nullable Response resp,
                                       @Nullable Integer responseCode,
                                       @Nullable Boolean isSuccessful,
                                       @Nullable Exception e) {
            if(e != null){
                HttpLogger.INSTANCE.d(
                        TAG,
                        "API CALL FAILURE : " + e.toString()
                );
            }else {
                HttpLogger.INSTANCE.d(
                        TAG,
                        "TIME TAKEN FOR API CALL(MILLIS) : " + (System.currentTimeMillis() - reqTimeStamp)
                );
            }
            if (jsonObjReqListener != null) {
                if (e != null) {
                    jsonObjReqListener.onFailure(e);
                } else {
                    try {
                        jsonObjReqListener.onResponse(resp != null ? resp.asJSONObject() : null, responseCode, isSuccessful);
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
            } else if (this.jsonArrayRequestListener != null) {
                if (e != null) {
                    jsonArrayRequestListener.onFailure(e);
                } else {
                    try {
                        jsonArrayRequestListener.onResponse(resp != null ? resp.asJSONArray() : null, responseCode, isSuccessful);
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
            } else if(this.stringListener != null){
                if (e != null) {
                    stringListener.onFailure(e);
                } else {
                    try {
                        stringListener.onResponse(resp != null ? resp.asString() : null, responseCode, isSuccessful);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            } else {
                if (e != null) {
                    e.printStackTrace();
                }
            }
        }

        public final String getQueryString() {
            if (this.queryParameter.isEmpty()) {
                return "";
            } else {
                StringBuilder result = new StringBuilder("?");
                Map<String, String> requestQuery = this.queryParameter;

                int entrySetSize = requestQuery.entrySet().size();
                for (Entry<String, String> entry : requestQuery.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    try {
                        result.append(URLEncoder.encode(key, UTF_8));
                        result.append("=");
                        result.append(URLEncoder.encode(value, UTF_8));
                        entrySetSize--;
                        if (entrySetSize != 0) {
                            result.append("&");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return result.toString();
            }
        }

        public final String getTempUrl() {
            String tempUrl = this.uri;
            for (Entry<String, String> entry : this.pathParameter.entrySet()) {
                tempUrl = tempUrl.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
            return tempUrl;
        }
    }

    public static final class RequestTask implements Callable {
        private final Request req;

        public RequestTask(@NonNull Request req) {
            super();
            this.req = req;
        }

        @Override
        public Void call() {
            try {
                HttpURLConnection conn = this.request();
                this.parseResponse(conn);
            } catch (IOException e) {
                if(req.backoffEnabled){
                    if (this.req.loggingEnabled) {
                        HttpLogger.INSTANCE.d(TAG, "Http : " + e.getMessage());
                    }

                    retry();
                }else {
                    this.req.sendResponse(null, null, null, e);
                    e.printStackTrace();
                }
            }
            return null;
        }

        private HttpURLConnection request() throws IOException {
            URL url = new URL(req.getTempUrl() + req.getQueryString());
            URLConnection openConnection = url.openConnection();
            if (openConnection == null) {
                throw new NullPointerException("null cannot be cast to non-null type java.net.HttpURLConnection");
            } else {
                HttpURLConnection conn = (HttpURLConnection) openConnection;
                String method = req.method;
                conn.setRequestMethod(method);
                conn.setConnectTimeout(req.connectTimeout);
                conn.setReadTimeout(req.readTimeout);
                conn.setDoInput(true);
                if (req.loggingEnabled) {
                    HttpLogger.INSTANCE.d(TAG, "Http : URL : " + url);
                    HttpLogger.INSTANCE.d(TAG, "Http : Method : " + method);
                    HttpLogger.INSTANCE.d(TAG, "Http : Headers : " + req.header.toString());
                    if (this.req.body != null) {
                        HttpLogger.INSTANCE.d(TAG, "Http : Request Body : " + HttpUtils.INSTANCE.asString(req.body));
                    }
                }

                Map<String, String> requestHeader = this.req.header;

                for (Entry<String, String> entry : requestHeader.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    conn.setRequestProperty(key, value);
                }

                if (this.req.body != null) {
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(this.req.body);
                }
                conn.connect();
                return conn;
            }
        }

        private void parseResponse(HttpURLConnection conn) throws IOException {
            try {
                int responseCode = conn.getResponseCode();
                if (this.req.loggingEnabled) {
                    HttpLogger.INSTANCE.d(TAG, "Http : Response Status Code : " + responseCode + " for URL: " + conn.getURL());
                }

                if (responseCode != HttpURLConnection.HTTP_OK
                        && !Arrays.asList(req.responseCodesToExcludeForBackoff).contains(responseCode)) {
                    throw new IOException("HTTP request failed");
                }
                //resetting the pendingRetryAttempt
                req.pendingRetryAttempt = 0;

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                String message;
                TreeMap<String, List<String>> respHeaders;
                boolean isSuccessfulStatus;
                label:
                {
                    message = conn.getResponseMessage();
                    respHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    Map<String, List<String>> headerFields = (new HashMap(conn.getHeaderFields()));
                    headerFields.remove(null);
                    respHeaders.putAll(headerFields);
                    if (responseCode >= 200) {
                        if (responseCode <= 399) {
                            isSuccessfulStatus = true; //200–299
                            break label;
                        }
                    }
                    isSuccessfulStatus = false;
                }

                InputStream inpStream = isSuccessfulStatus ? conn.getInputStream() : conn.getErrorStream();

                byte[] buf = new byte[BUFFER_SIZE];

                while (true) {
                    int read = inpStream.read(buf);
                    if (read == -1) {
                        Response resp = new Response(bos.toByteArray(), responseCode, message, respHeaders);
                        if (this.req.loggingEnabled && !isSuccessfulStatus) {
                            HttpLogger.INSTANCE.d(TAG, "Http : Response Body : " + resp.asString());
                        }

                        this.req.sendResponse(resp, responseCode, isSuccessfulStatus, null);
                        return;
                    }

                    bos.write(buf, 0, read);
                }
            } finally {
                conn.disconnect();
            }
        }

        private void retry() {
            //Retry logic
            if (req.maxRetries != 0 && req.pendingRetryAttempt != 0) {

                new Handler(Looper.getMainLooper())
                        .postDelayed(() -> {
                            if (req.loggingEnabled) {
                                HttpLogger.INSTANCE.d(TAG, "Http : Retrying the request...");
                            }
                            req.executeRequestTask();
                        }, req.retryDelayMillis);

                if(req.maxRetries != -1){
                    req.pendingRetryAttempt -= 1;
                }

            }else {

                if(req.backoffCriteriaFailedListener != null){
                    req.backoffCriteriaFailedListener.onBackoffCriteriaFailed();
                }

            }
        }
    }

    public static final class Response {
        private final byte[] data;
        private final int status;
        private final String message;
        private final Map<String, List<String>> respHeaders;

        public final JSONObject asJSONObject() throws JSONException {
            String str = this.asString();
            return str.length() == 0 ? new JSONObject() : new JSONObject(str);
        }

        public final JSONArray asJSONArray() throws JSONException {
            String str = this.asString();
            return str.length() == 0 ? new JSONArray() : new JSONArray(str);
        }

        public final String asString() {
            return HttpUtils.INSTANCE.asString(this.data);
        }

        public final int getStatus() {
            return this.status;
        }

        public final String getMessage() {
            return this.message;
        }

        public final Map<String, List<String>> getRespHeaders() {
            return this.respHeaders;
        }

        public Response(@NonNull byte[] data, int status, @NonNull String message, @NonNull Map<String, List<String>> respHeaders) {
            super();
            this.data = data;
            this.status = status;
            this.message = message;
            this.respHeaders = respHeaders;
        }
    }
}