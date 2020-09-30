package com.clevertap.android.sdk.ab_testing;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ab_testing.gesture.ConnectionGesture;
import com.clevertap.android.sdk.ab_testing.models.CTABVariant;
import com.clevertap.android.sdk.ab_testing.uieditor.UIEditor;
import com.clevertap.android.sdk.java_websocket.client.WebSocketClient;
import com.clevertap.android.sdk.java_websocket.drafts.Draft_6455;
import com.clevertap.android.sdk.java_websocket.enums.Opcode;
import com.clevertap.android.sdk.java_websocket.exceptions.NotSendableException;
import com.clevertap.android.sdk.java_websocket.exceptions.WebsocketNotConnectedException;
import com.clevertap.android.sdk.java_websocket.handshake.ServerHandshake;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@RequiresApi(api = VERSION_CODES.KITKAT)
public class CTABTestController {

    @SuppressWarnings("unused")
    public static class LayoutErrorMessage {

        private final String errorName;

        private final String errorType;

        public LayoutErrorMessage(String type, String name) {
            errorType = type;
            errorName = name;
        }

        public String getName() {
            return errorName;
        }

        public String getType() {
            return errorType;
        }
    }

    private class ExecutionThreadHandler extends Handler {

        private class DashboardClient extends WebSocketClient {

            private URI dashboardURI;

            private DashboardClient(URI uri, int connectTimeout) {
                super(uri, new Draft_6455(), null, connectTimeout);
                this.dashboardURI = uri;
                setSocketFactory(SSLSocketFactory);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                getConfigLogger().verbose(getAccountId(),
                        "WebSocket closed. Code: " + code + ", reason: " + reason + "\nURI: " + dashboardURI);
                handleOnClose();
            }

            @Override
            public void onError(Exception ex) {
                if (ex != null && ex.getMessage() != null) {
                    getConfigLogger().verbose(getAccountId(), "Websocket Error: " + ex.getMessage());
                } else {
                    getConfigLogger().verbose(getAccountId(), "Unknown websocket error");
                }
            }

            @Override
            public void onMessage(String message) {
                try {
                    final JSONObject messageJson = new JSONObject(message);
                    if (messageJson.has("data")) {
                        if (messageJson.getJSONObject("data").keys().hasNext()) {
                            getConfigLogger().verbose(getAccountId(), "Received message from dashboard:\n" + message);
                        }
                    }
                    if (!connectionIsValid()) {
                        getConfigLogger().verbose(getAccountId(),
                                "Dashboard connection is stale, dropping message: " + message);
                        return;
                    }
                    handleDashboardMessage(messageJson);
                } catch (final JSONException e) {
                    getConfigLogger().verbose(getAccountId(), "Bad JSON message received:" + message, e);
                }
            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                getConfigLogger().verbose(getAccountId(), "Websocket connected");
                handleOnOpen();
            }
        }

        private class WebSocketOutputStream extends OutputStream {

            @Override
            public void close() {
                try {
                    wsClient.sendFragmentedFrame(Opcode.TEXT, EMPTY_BYTE_BUFFER, true);
                } catch (final WebsocketNotConnectedException e) {
                    getConfigLogger().debug(getAccountId(), "Web socket not connected", e);
                } catch (final NotSendableException e) {
                    getConfigLogger().debug(getAccountId(), "Unable to send data to web socket", e);
                }
            }

            @Override
            public void write(@NonNull byte[] b) {
                write(b, 0, b.length);
            }

            @Override
            public void write(@NonNull byte[] b, int off, int len) {
                final ByteBuffer message = ByteBuffer.wrap(b, off, len);
                try {
                    wsClient.sendFragmentedFrame(Opcode.TEXT, message, false);
                } catch (final WebsocketNotConnectedException e) {
                    getConfigLogger().debug(getAccountId(), "Web socket not connected", e);
                } catch (final NotSendableException e) {
                    getConfigLogger().debug(getAccountId(), "Unable to send data to web socket", e);
                }
            }

            @Override
            public void write(int b) {
                final byte[] oneByte = new byte[1];
                oneByte[0] = (byte) b;
                write(oneByte, 0, 1);
            }
        }

        static final int MESSAGE_UNKNOWN = -1;

        static final int MESSAGE_INITIALIZE_EXPERIMENTS = 0;

        static final int MESSAGE_CONNECT_TO_EDITOR = 1;

        static final int MESSAGE_SEND_SNAPSHOT = 2;

        static final int MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED = 3;

        static final int MESSAGE_SEND_DEVICE_INFO = 4;

        static final int MESSAGE_HANDLE_DISCONNECT = 5;

        static final int MESSAGE_EXPERIMENTS_RECEIVED = 6;

        static final int MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED = 7;

        static final int MESSAGE_HANDLE_EDITOR_VARS_RECEIVED = 8;

        static final int MESSAGE_SEND_LAYOUT_ERROR = 9;

        static final int MESSAGE_PERSIST_EXPERIMENTS = 10;

        static final int MESSAGE_SEND_VARS = 11;

        static final int MESSAGE_TEST_VARS = 12;

        static final int MESSAGE_MATCHED = 13;

        private static final String EXPERIMENTS_KEY = "experiments";

        private static final int CONNECT_TIMEOUT = 5000;

        private CleverTapInstanceConfig config;

        private Context context;

        private CTABVariant editorSessionVariant;

        private HashSet<CTABVariant> editorSessionVariantSet;

        private final Lock lock = new ReentrantLock();

        private Set<CTABVariant> variants;

        private DashboardClient wsClient;

        @SuppressWarnings("unused")
        ExecutionThreadHandler(Context context, CleverTapInstanceConfig config, Looper looper) {
            super(looper);
            this.config = config;
            this.context = context;
            this.variants = new HashSet<>();
            lock.lock();
        }

        @Override
        public void handleMessage(Message msg) {
            lock.lock();
            try {
                final int what = msg.what;
                Object data = msg.obj;
                switch (what) {
                    case MESSAGE_INITIALIZE_EXPERIMENTS:
                        loadStoredExperiments();
                        break;
                    case MESSAGE_CONNECT_TO_EDITOR:
                        createConnection();
                        break;
                    case MESSAGE_MATCHED:
                        handleMatched();
                        break;
                    case MESSAGE_HANDLE_DISCONNECT:
                        handleDashboardDisconnect();
                        break;
                    case MESSAGE_SEND_DEVICE_INFO:
                        sendDeviceInfo();
                        break;
                    case MESSAGE_SEND_SNAPSHOT:
                        sendSnapshot((JSONObject) data);
                        break;
                    case MESSAGE_SEND_LAYOUT_ERROR:
                        sendLayoutError((LayoutErrorMessage) msg.obj);
                        break;
                    case MESSAGE_EXPERIMENTS_RECEIVED:
                        applyExperiments((JSONArray) data, true);
                        break;
                    case MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED:
                        handleEditorChangesReceived((JSONObject) data);
                        break;
                    case MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED:
                        handleEditorChangesCleared((JSONObject) data);
                        break;
                    case MESSAGE_HANDLE_EDITOR_VARS_RECEIVED:
                    case MESSAGE_TEST_VARS:
                        handleEditorVarsReceived((JSONObject) data);
                        break;
                    case MESSAGE_PERSIST_EXPERIMENTS:
                        persistExperiments((JSONArray) data);
                        break;
                    case MESSAGE_SEND_VARS:
                        sendVars();
                        break;
                }
            } finally {
                lock.unlock();
            }
        }

        public void start() {
            lock.unlock();
        }

        void handleMatched() {
            varCache.reset();
            stopVariants();
        }

        boolean isConnected() {
            return wsClient != null && wsClient.isOpen();
        }

        private void applyExperiments(JSONArray experiments, boolean areNew) {
            loadVariants(experiments);
            applyVariants();
            if (areNew) {
                persistExperiments(experiments);
            }
            notifyExperimentsUpdated();
        }

        private void applyVariants() {
            for (CTABVariant variant : variants) {
                applyVars(variant.getVars());
            }
            uiEditor.applyVariants(variants, false);
        }

        private void applyVars(JSONArray vars) {
            try {
                for (int i = 0; i < vars.length(); i++) {
                    JSONObject var = vars.getJSONObject(i);
                    _registerVar(var.getString("name"), CTVar.CTVarType.fromString(var.getString("type")),
                            var.get("value"));
                }
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to apply Vars - " + t);
            }
        }

        private void closeConnection() {
            if (connectionIsValid()) {
                try {
                    getConfigLogger().verbose(getAccountId(), "disconnecting from dashboard");
                    wsClient.closeBlocking();
                } catch (final Exception e) {
                    getConfigLogger().verbose(getAccountId(), "Unable to close dashboard connection", e);
                }
            }
        }

        private boolean connectionIsValid() {
            return wsClient != null && !wsClient.isClosed() && !wsClient.isClosing() && !wsClient.isFlushAndClose();
        }

        private void createConnection() {
            getConfigLogger().verbose(getAccountId(), "connecting to dashboard");
            if (isConnected() && connectionIsValid()) {
                getConfigLogger().verbose(getAccountId(), "There is already a valid dashboard connection.");
                return;
            }

            if (SSLSocketFactory == null) {
                getConfigLogger().verbose(getAccountId(),
                        "SSL is not available on this device, dashboard connection is not available.");
                return;
            }

            final String protocol = "wss";
            String region = config.getAccountRegion() != null ? config.getAccountRegion() : DEFAULT_REGION;
            region = config.isBeta() ? region + "-dashboard-beta" : region;
            final String domain = region + "." + DASHBOARD_URL;
            final String url = protocol + "://" + domain + "/" + getAccountId() + "/" + "websocket/screenab/sdk?tk="
                    + config.getAccountToken();
            getConfigLogger().verbose(getAccountId(), "Websocket URL - " + url);
            try {
                wsClient = new DashboardClient(new URI(url), CONNECT_TIMEOUT);
                wsClient.connectBlocking();
            } catch (final Exception e) {
                getConfigLogger().verbose(getAccountId(), "Unable to connect to dashboard", e);
            }
        }

        private String getAccountId() {
            return config.getAccountId();
        }

        private BufferedOutputStream getBufferedOutputStream() {
            return new BufferedOutputStream(new WebSocketOutputStream());
        }

        private Logger getConfigLogger() {
            return config.getLogger();
        }

        private JSONObject getDeviceInfo() {
            if (cachedDeviceInfo == null) {
                JSONObject data = new JSONObject();
                try {
                    Map<String, String> deviceInfo = CleverTapAPI.instanceWithConfig(context, config).getDeviceInfo();
                    for (final Map.Entry<String, String> entry : deviceInfo.entrySet()) {
                        data.put(entry.getKey(), entry.getValue());
                    }
                } catch (Throwable t) {
                    // no-op
                }
                cachedDeviceInfo = data;
            }
            return cachedDeviceInfo;
        }

        private CTABVariant getEditorSessionVariant() {
            if (editorSessionVariant == null) {
                try {
                    JSONObject variant = new JSONObject();
                    variant.put("id", "0");
                    variant.put("experiment_id", "0");
                    editorSessionVariant = CTABVariant.initWithJSON(variant);
                    editorSessionVariantSet = new HashSet<>();
                    editorSessionVariantSet.add(editorSessionVariant);
                } catch (Throwable t) {
                    getConfigLogger().verbose(getAccountId(), "Error creating editor session variant", t);
                }
            }
            return editorSessionVariant;
        }

        private SharedPreferences getSharedPreferences() {
            return context.getSharedPreferences(getSharedPrefsName(), Context.MODE_PRIVATE);
        }

        private String getSharedPrefsName() {
            return "clevertap.abtesting." + getAccountId() + "." + guid;
        }

        private void handleDashboardDisconnect() {
            stopVariants();
            closeConnection();
        }

        private void handleEditorChangesCleared(JSONObject request) {
            try {
                JSONArray changes = request.optJSONArray("actions");
                if (changes == null || changes.length() <= 0) {
                    getEditorSessionVariant().clearActions();
                } else {
                    getEditorSessionVariant().removeActionsByName(changes);
                }
                uiEditor.applyVariants(editorSessionVariantSet, true);
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to clear dashboard changes - " + t);
            }
        }

        private void handleEditorChangesReceived(JSONObject request) {
            try {
                JSONArray changes = request.optJSONArray("actions");
                if (changes == null || changes.length() <= 0) {
                    getConfigLogger().debug(getAccountId(), "No changes received from dashboard");
                    return;
                } else {
                    getEditorSessionVariant().addActions(changes);
                }
                uiEditor.applyVariants(editorSessionVariantSet, true);
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to handle dashboard changes received - " + t);
            }
        }

        private void handleEditorVarsReceived(JSONObject request) {
            try {
                JSONArray vars = request.optJSONArray("vars");
                if (vars == null || vars.length() <= 0) {
                    getConfigLogger().debug(getAccountId(), "No Vars received from dashboard");
                    return;
                }
                applyVars(vars);
                notifyExperimentsUpdated();
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to handle dashboard Vars received - " + t);
            }
        }

        private void handleOnClose() {
            getConfigLogger().verbose(getAccountId(), "handle websocket on close");
            stopVariants();
            getEditorSessionVariant().clearActions();
            varCache.reset();
            applyVariants();
        }

        private void handleOnOpen() {
            sendHandshake();
        }

        private void loadStoredExperiments() {
            final SharedPreferences preferences = getSharedPreferences();
            final String storedExperiments = preferences.getString(EXPERIMENTS_KEY, null);
            if (storedExperiments != null) {
                try {
                    getConfigLogger().debug(getAccountId(),
                            "Loading Stored Experiments: " + storedExperiments + " for key: " + getSharedPrefsName());
                    final JSONArray _experiments = new JSONArray(storedExperiments);
                    applyExperiments(_experiments, false);
                } catch (JSONException e) {
                    final SharedPreferences.Editor editor = preferences.edit();
                    editor.remove(EXPERIMENTS_KEY);
                    editor.apply();
                }
            } else {
                getConfigLogger().debug(getAccountId(), "No Stored Experiments for key: " + getSharedPrefsName());
            }
        }

        private void loadVariants(JSONArray experiments) {
            if (experiments == null) {
                return;
            }
            // note:  experiments here will be all the currently running experiments for the user
            try {
                Set<CTABVariant> toRemove = new HashSet<>(this.variants);
                Set<CTABVariant> allVariants = new HashSet<>(this.variants);

                final int experimentsLength = experiments.length();
                for (int i = 0; i < experimentsLength; i++) {
                    final JSONObject nextVariant = experiments.getJSONObject(i);
                    final CTABVariant variant = CTABVariant.initWithJSON(nextVariant);
                    if (variant != null) {
                        boolean added = allVariants.add(variant);
                        if (added) {
                            toRemove.remove(variant);
                        }
                    }
                }
                if (!allVariants.containsAll(toRemove)) {
                    if (toRemove.size() > 0) {
                        for (CTABVariant v : toRemove) {
                            v.cleanup();
                            allVariants.remove(v);
                        }
                    }
                }

                //This will revert changes at SDK level when all experiments are stopped/revert without needing
                //another App Launched event
                if (experiments.length() == 0) {
                    allVariants.clear();
                }

                this.variants = allVariants;
            } catch (JSONException e) {
                getConfigLogger().verbose(getAccountId(), "Error loading variants, clearing all running variants", e);
                this.variants.clear();
            }
        }

        private void notifyExperimentsUpdated() {
            CTABTestListener listener = getListener();
            if (listener != null) {
                listener.ABExperimentsUpdated();
            }
        }

        private void persistExperiments(JSONArray experiments) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(EXPERIMENTS_KEY, experiments.toString());
            editor.apply();
        }

        private void sendDeviceInfo() {
            try {
                JSONObject payload = new JSONObject();
                payload.put(TYPE_KEY, MESSAGE_TYPE_DEVICE_INFO_RESPONSE);
                payload.put(DATA_KEY, getDeviceInfo());
                sendMessage(payload.toString());
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to create deviceInfo message", t);
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void sendError(String errorMessage) {
            try {
                JSONObject data = new JSONObject();
                data.put("error", errorMessage);
                JSONObject payload = new JSONObject();
                payload.put(TYPE_KEY, MESSAGE_TYPE_GENERIC_ERROR);
                payload.put(DATA_KEY, data);
                sendMessage(payload.toString());
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to create error message", t);
            }
        }

        private void sendHandshake() {
            try {
                JSONObject deviceInfo = getDeviceInfo();
                JSONObject data = new JSONObject();
                data.put("id", guid);
                data.put("os", deviceInfo.getString("osName"));
                data.put("name", deviceInfo.getString("manufacturer") + " " + deviceInfo.getString("model"));
                if (deviceInfo.has("library")) {
                    data.put("library", deviceInfo.getString("library"));
                }
                JSONObject payload = new JSONObject();
                payload.put(TYPE_KEY, MESSAGE_TYPE_HANDSHAKE);
                payload.put(DATA_KEY, data);
                sendMessage(payload.toString());
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to create handshake message", t);
            }
        }

        private void sendLayoutError(LayoutErrorMessage errorMessage) {
            try {
                JSONObject data = new JSONObject();
                data.put("type", errorMessage.getType());
                data.put("name", errorMessage.getName());
                JSONObject payload = new JSONObject();
                payload.put(TYPE_KEY, MESSAGE_TYPE_LAYOUT_ERROR);
                payload.put(DATA_KEY, data);
                sendMessage(payload.toString());
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to create error message", t);
            }
        }

        private void sendMessage(String message) {
            if (!connectionIsValid()) {
                getConfigLogger().debug(getAccountId(),
                        "Unable to send websocket message: " + message + " connection is invalid");
                return;
            }
            final OutputStreamWriter writer = new OutputStreamWriter(getBufferedOutputStream());
            getConfigLogger().verbose("Sending message to dashboard - " + message);
            try {
                writer.write(message);
            } catch (final IOException e) {
                getConfigLogger().verbose(getAccountId(), "Can't message to editor", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    getConfigLogger().verbose(getAccountId(), "Could not close output writer to editor", e);
                }
            }
        }

        private void sendSnapshot(JSONObject data) {
            final long startSnapshot = System.currentTimeMillis();
            boolean isValidSnapshot = uiEditor.loadSnapshotConfig(data);
            if (!isValidSnapshot) {
                String err = "Missing or invalid snapshot configuration.";
                sendError(err);
                getConfigLogger().debug(getAccountId(), err);
                return;
            }

            final OutputStream out = getBufferedOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);

            try {
                writer.write("{");
                writer.write("\"" + TYPE_KEY + "\": \"" + MESSAGE_TYPE_SNAPSHOT_RESPONSE + "\",");
                writer.write("\"" + DATA_KEY + "\": {");
                {
                    writer.write("\"activities\":");
                    writer.flush();
                    uiEditor.writeSnapshot(out);
                }

                final long snapshotTime = System.currentTimeMillis() - startSnapshot;
                writer.write(",\"snapshot_time_millis\": ");
                writer.write(Long.toString(snapshotTime));

                writer.write("}"); // } payload
                writer.write("}"); // } whole message
            } catch (final IOException e) {
                getConfigLogger().verbose(getAccountId(), "Failure sending snapshot", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    getConfigLogger().verbose(getAccountId(), "Failure closing json writer", e);
                }
            }
        }

        private void sendVars() {
            try {
                JSONObject data = new JSONObject();
                data.put("vars", varCache.serializeVars());
                JSONObject payload = new JSONObject();
                payload.put(TYPE_KEY, MESSAGE_TYPE_VARS_RESPONSE);
                payload.put(DATA_KEY, data);
                sendMessage(payload.toString());
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "Unable to create vars message", t);
            }
        }

        private void stopVariants() {
            uiEditor.stopVariants();
        }
    }

    private class EmulatorConnectRunnable implements Runnable {

        private volatile boolean stopped;

        EmulatorConnectRunnable() {
            stopped = true;
        }

        @Override
        public void run() {
            if (!stopped) {
                final Message message = executionThreadHandler
                        .obtainMessage(ExecutionThreadHandler.MESSAGE_CONNECT_TO_EDITOR);
                executionThreadHandler.sendMessage(message);
            }
            executionThreadHandler.postDelayed(this, EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS);
        }

        void start() {
            stopped = false;
            executionThreadHandler.post(this);
        }

        void stop() {
            stopped = true;
            executionThreadHandler.removeCallbacks(this);
        }
    }

    private class LifecycleCallbacks
            implements Application.ActivityLifecycleCallbacks, ConnectionGesture.OnGestureListener {

        private EmulatorConnectRunnable emulatorConnectRunnable;

        private ConnectionGesture gesture;

        private LifecycleCallbacks() {
            gesture = new ConnectionGesture(this);
            emulatorConnectRunnable = new EmulatorConnectRunnable();
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
            uiEditor.removeActivity(activity);
            deregisterConnectionTrigger(activity);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            registerConnectionTrigger(activity);
            uiEditor.addActivity(activity);
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onGesture() {
            final Message message = executionThreadHandler
                    .obtainMessage(ExecutionThreadHandler.MESSAGE_CONNECT_TO_EDITOR);
            executionThreadHandler.sendMessage(message);
        }

        private void deregisterConnectionTrigger(final Activity activity) {
            if (!enableEditor) {
                config.getLogger().debug(config.getAccountId(), "UIEditor is disabled");
                return;
            }
            if (inEmulator()) {
                emulatorConnectRunnable.stop();
            } else {
                final SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
                if (sensorManager != null) {
                    sensorManager.unregisterListener(gesture);
                }
            }
        }

        private boolean inEmulator() {
            if (!Build.HARDWARE.toLowerCase().equals("goldfish") && !Build.HARDWARE.toLowerCase().equals("ranchu")) {
                return false;
            }

            if (!Build.BRAND.toLowerCase().startsWith("generic") && !Build.BRAND.toLowerCase().equals("android")
                    && !Build.BRAND.toLowerCase().equals("google")) {
                return false;
            }

            if (!Build.DEVICE.toLowerCase().startsWith("generic")) {
                return false;
            }

            if (!Build.PRODUCT.toLowerCase().contains("sdk")) {
                return false;
            }

            return Build.MODEL.toLowerCase(Locale.US).contains("sdk");
        }

        private void registerConnectionTrigger(final Activity activity) {
            if (!enableEditor) {
                config.getLogger().debug(config.getAccountId(), "UIEditor is disabled");
                return;
            }
            if (inEmulator()) {
                emulatorConnectRunnable.start();
            } else {
                try {
                    final SensorManager sensorManager = (SensorManager) activity
                            .getSystemService(Context.SENSOR_SERVICE);
                    // noinspection ConstantConditions
                    final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    sensorManager.registerListener(gesture, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                } catch (Throwable t) {
                    // no-op
                    config.getLogger().debug(config.getAccountId(), "Unable to register UIEditor connection gesture");
                }
            }
        }
    }

    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);

    private static final int EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS = 1000 * 30;

    private static final String DASHBOARD_URL = "dashboard.clevertap.com";

    private static final String DEFAULT_REGION = "eu1";

    private static final String MESSAGE_TYPE_HANDSHAKE = "handshake";

    private static final String MESSAGE_TYPE_CLEAR_REQUEST = "clear_request";

    private static final String MESSAGE_TYPE_CHANGE_REQUEST = "change_request";

    private static final String MESSAGE_TYPE_DEVICE_INFO_REQUEST = "device_info_request";

    private static final String MESSAGE_TYPE_DEVICE_INFO_RESPONSE = "device_info_response";

    private static final String MESSAGE_TYPE_SNAPSHOT_REQUEST = "snapshot_request";

    private static final String MESSAGE_TYPE_SNAPSHOT_RESPONSE = "snapshot_response";

    private static final String MESSAGE_TYPE_VARS_REQUEST = "vars_request";

    private static final String MESSAGE_TYPE_VARS_RESPONSE = "vars_response";

    private static final String MESSAGE_TYPE_LAYOUT_ERROR = "layout_error";

    private static final String MESSAGE_TYPE_GENERIC_ERROR = "error";

    private static final String MESSAGE_TYPE_VARS_TEST = "test_vars";

    private static final String MESSAGE_TYPE_MATCHED = "matched";

    private static final String MESSAGE_TYPE_DISCONNECT = "disconnect";

    private static final String DATA_KEY = "data";

    private static final String TYPE_KEY = "type";

    private static javax.net.ssl.SSLSocketFactory SSLSocketFactory;

    private JSONObject cachedDeviceInfo;

    private CleverTapInstanceConfig config;

    private boolean enableEditor;

    private ExecutionThreadHandler executionThreadHandler;

    private String guid;

    private WeakReference<CTABTestListener> listenerWeakReference;

    private UIEditor uiEditor;

    private CTVarCache varCache;

    public CTABTestController(Context context, CleverTapInstanceConfig config, String guid,
            CTABTestListener listener) {
        try {
            this.varCache = new CTVarCache();
            this.enableEditor = config.isUIEditorEnabled();
            this.config = config;
            this.guid = guid;
            this.setListener(listener);
            this.uiEditor = new UIEditor(context, config);

            final HandlerThread thread = new HandlerThread(CTABTestController.class.getCanonicalName());
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            executionThreadHandler = new ExecutionThreadHandler(context, config, thread.getLooper());
            executionThreadHandler.start();

            if (enableEditor) {
                final Application app = (Application) context.getApplicationContext();
                app.registerActivityLifecycleCallbacks(new LifecycleCallbacks());
            } else {
                config.getLogger().debug(config.getAccountId(), "UIEditor connection is disabled");
            }
            applyStoredExperiments();
        } catch (Throwable t) {
            config.setEnableABTesting(false);
            config.setEnableUIEditor(false);
            config.getLogger().debug(config.getAccountId(), t);
        }
    }

    @SuppressWarnings({"unused"})
    public Boolean getBooleanVariable(String name, Boolean defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.booleanValue() != null) {
                return var.booleanValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public Double getDoubleVariable(String name, Double defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.doubleValue() != null) {
                return var.doubleValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public Integer getIntegerVariable(String name, Integer defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.integerValue() != null) {
                return var.integerValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public List<Boolean> getListOfBooleanVariable(String name, List<Boolean> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.listValue() != null) {
                // noinspection unchecked
                return (List<Boolean>) var.listValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public List<Double> getListOfDoubleVariable(String name, List<Double> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.listValue() != null) {
                // noinspection unchecked
                return (List<Double>) var.listValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public List<Integer> getListOfIntegerVariable(String name, List<Integer> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.listValue() != null) {
                // noinspection unchecked
                return (List<Integer>) var.listValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public List<String> getListOfStringVariable(String name, List<String> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.listValue() != null) {
                // noinspection unchecked
                return (List<String>) var.listValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public Map<String, Boolean> getMapOfBooleanVariable(String name, Map<String, Boolean> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.mapValue() != null) {
                // noinspection unchecked
                return (Map<String, Boolean>) var.mapValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public Map<String, Double> getMapOfDoubleVariable(String name, Map<String, Double> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.mapValue() != null) {
                // noinspection unchecked
                return (Map<String, Double>) var.mapValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public Map<String, Integer> getMapOfIntegerVariable(String name, Map<String, Integer> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.mapValue() != null) {
                // noinspection unchecked
                return (Map<String, Integer>) var.mapValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public Map<String, String> getMapOfStringVariable(String name, Map<String, String> defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.mapValue() != null) {
                // noinspection unchecked
                return (Map<String, String>) var.mapValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public String getStringVariable(String name, String defaultValue) {
        CTVar var = this.varCache.getVar(name);
        try {
            if (var != null && var.stringValue() != null) {
                return var.stringValue();
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Error getting variable with name: " + name, t);
            return defaultValue;
        }
        return defaultValue;
    }

    @SuppressWarnings({"unused"})
    public void registerBooleanVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeBool, null);
    }

    @SuppressWarnings({"unused"})
    public void registerDoubleVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeDouble, null);
    }

    @SuppressWarnings({"unused"})
    public void registerIntegerVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeInteger, null);
    }

    @SuppressWarnings({"unused"})
    public void registerListOfBooleanVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeListOfBool, null);
    }

    @SuppressWarnings({"unused"})
    public void registerListOfDoubleVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeListOfDouble, null);
    }

    @SuppressWarnings({"unused"})
    public void registerListOfIntegerVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeListOfInteger, null);
    }

    @SuppressWarnings({"unused"})
    public void registerListOfStringVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeListOfString, null);
    }

    @SuppressWarnings({"unused"})
    public void registerMapOfBooleanVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeMapOfBool, null);
    }

    @SuppressWarnings({"unused"})
    public void registerMapOfDoubleVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeMapOfDouble, null);
    }

    @SuppressWarnings({"unused"})
    public void registerMapOfIntegerVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeMapOfInteger, null);
    }

    @SuppressWarnings({"unused"})
    public void registerMapOfStringVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeMapOfString, null);
    }

    @SuppressWarnings({"unused"})
    public void registerStringVariable(String name) {
        _registerVar(name, CTVar.CTVarType.CTVarTypeString, null);
    }

    public void resetWithGuid(String guid) {
        this.guid = guid;
        this.varCache.reset();
        uiEditor.stopVariants();
        applyStoredExperiments();
    }

    public void updateExperiments(JSONArray experiments) {
        if (experiments != null) {
            final Message message = executionThreadHandler
                    .obtainMessage(ExecutionThreadHandler.MESSAGE_EXPERIMENTS_RECEIVED);
            message.obj = experiments;
            executionThreadHandler.sendMessage(message);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void _registerVar(String name, CTVar.CTVarType type, Object value) {
        this.varCache.registerVar(name, type, value);
        config.getLogger().verbose(config.getAccountId(),
                "Registered Var with name: " + name + " type: " + type.toString() + " and value: " + ((value != null)
                        ? value.toString() : "null"));
    }

    private void applyStoredExperiments() {
        executionThreadHandler.sendMessage(
                executionThreadHandler.obtainMessage(ExecutionThreadHandler.MESSAGE_INITIALIZE_EXPERIMENTS));
    }

    private CTABTestListener getListener() {
        CTABTestListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(), "CTABTestListener is null in CTABTestController");
        }
        return listener;
    }

    private void setListener(CTABTestListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    private void handleDashboardMessage(JSONObject msg) {

        String type = msg.optString(TYPE_KEY, "unknown");
        int messageCode = ExecutionThreadHandler.MESSAGE_UNKNOWN;
        switch (type) {
            case MESSAGE_TYPE_CHANGE_REQUEST:
                messageCode = ExecutionThreadHandler.MESSAGE_HANDLE_EDITOR_CHANGES_RECEIVED;
                break;
            case MESSAGE_TYPE_CLEAR_REQUEST:
                messageCode = ExecutionThreadHandler.MESSAGE_HANDLE_EDITOR_CHANGES_CLEARED;
                break;
            case MESSAGE_TYPE_DEVICE_INFO_REQUEST:
                messageCode = ExecutionThreadHandler.MESSAGE_SEND_DEVICE_INFO;
                break;
            case MESSAGE_TYPE_SNAPSHOT_REQUEST:
                messageCode = ExecutionThreadHandler.MESSAGE_SEND_SNAPSHOT;
                break;
            case MESSAGE_TYPE_VARS_REQUEST:
                messageCode = ExecutionThreadHandler.MESSAGE_SEND_VARS;
                break;
            case MESSAGE_TYPE_VARS_TEST:
                messageCode = ExecutionThreadHandler.MESSAGE_TEST_VARS;
                break;
            case MESSAGE_TYPE_MATCHED:
                messageCode = ExecutionThreadHandler.MESSAGE_MATCHED;
                break;
            case MESSAGE_TYPE_DISCONNECT:
                messageCode = ExecutionThreadHandler.MESSAGE_HANDLE_DISCONNECT;
            default:
                break;
        }
        final Message m = executionThreadHandler.obtainMessage(messageCode);

        JSONObject messageObject;
        try {
            messageObject = msg.getJSONObject(DATA_KEY);
        } catch (Throwable t) {
            // no-op
            messageObject = new JSONObject();
        }
        m.obj = messageObject;

        executionThreadHandler.sendMessage(m);
    }

    static {
        SSLSocketFactory found;
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            found = sslContext.getSocketFactory();
        } catch (final GeneralSecurityException e) {
            Logger.d("No SSL support. ABTest editor not available", e.getLocalizedMessage());
            found = null;
        }
        SSLSocketFactory = found;
    }
}
