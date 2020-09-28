package com.clevertap.android.sdk.ab_testing.uieditor;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.ImageCache;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.ab_testing.models.CTABVariant;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UIEditor {

    private static class UIChange {

        final List<String> imageUrls;

        final ViewEdit viewEdit;

        private UIChange(ViewEdit viewEdit, List<String> urls) {
            this.viewEdit = viewEdit;
            imageUrls = urls;
        }
    }

    private static class UIChangeBinding implements ViewTreeObserver.OnGlobalLayoutListener, Runnable {

        private boolean alive;

        private volatile boolean dying;

        private final Handler handler;

        private final ViewEdit viewEdit;

        private final WeakReference<View> viewRoot;

        UIChangeBinding(View viewRoot, ViewEdit edit, Handler uiThreadHandler) {
            viewEdit = edit;
            this.viewRoot = new WeakReference<>(viewRoot);
            handler = uiThreadHandler;
            alive = true;
            dying = false;

            final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.addOnGlobalLayoutListener(this);
            }
            run();
        }

        @Override
        public void onGlobalLayout() {
            run();
        }

        @Override
        public void run() {
            if (!alive) {
                return;
            }
            final View viewRoot = this.viewRoot.get();
            if (null == viewRoot || dying) {
                cleanUp();
                return;
            }
            viewEdit.run(viewRoot);
            handler.removeCallbacks(this);
            handler.postDelayed(this, 1000);
        }

        private void cleanUp() {
            if (alive) {
                final View viewRoot = this.viewRoot.get();
                if (viewRoot != null) {
                    final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.removeGlobalOnLayoutListener(this);
                    }
                }
                viewEdit.cleanup();
            }
            alive = false;
        }

        private void kill() {
            dying = true;
            handler.post(this);
        }
    }

    class ActivitySet {

        private Set<Activity> activitySet;

        ActivitySet() {
            activitySet = new HashSet<>();
        }

        void add(Activity activity) {
            checkThreadState();
            activitySet.add(activity);
        }

        Set<Activity> getAll() {
            checkThreadState();
            return Collections.unmodifiableSet(activitySet);
        }

        boolean isEmpty() {
            checkThreadState();
            return activitySet.isEmpty();
        }

        void remove(Activity activity) {
            checkThreadState();
            activitySet.remove(activity);
        }

        private void checkThreadState() throws RuntimeException {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                throw new RuntimeException("Can't access ActivitySet when not on the UI thread");
            }
        }
    }

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    private static final List<ViewEdit.PathElement> NEVER_MATCH_PATH = Collections.emptyList();

    private ActivitySet activitySet;

    private CleverTapInstanceConfig config;

    private Context context;

    private final Deque<UIChangeBinding> currentEdits;//Need a LIFO structure to reverse changes

    private final ArrayList<String> editorSessionImageUrls;

    private final Map<String, List<ViewEdit>> newEdits;

    private ResourceIds resourceIds;

    private SnapshotBuilder.ViewSnapshotConfig snapshotConfig;

    private final Handler uiThreadHandler;

    public UIEditor(Context context, CleverTapInstanceConfig config) {
        String resourcePackageName = config.getPackageName();
        if (resourcePackageName == null) {
            resourcePackageName = context.getPackageName();
        }
        this.resourceIds = new ResourceIds(resourcePackageName);
        this.config = config;
        uiThreadHandler = new Handler(Looper.getMainLooper());
        newEdits = new HashMap<>();
        currentEdits = new ArrayDeque<>();
        activitySet = new ActivitySet();
        editorSessionImageUrls = new ArrayList<>();
        this.context = context;
    }

    public void addActivity(Activity activity) {
        activitySet.add(activity);
        handleNewEditsOnUiThread();
    }

    public void applyVariants(Set<CTABVariant> variants, boolean isEditorSession) {
        final Map<String, List<ViewEdit>> edits = new HashMap<>();
        for (CTABVariant variant : variants) {
            for (CTABVariant.CTVariantAction action : variant.getActions()) {
                final UIChange change = generateUIChange(action.getChange());
                if (change != null) {
                    if (isEditorSession) {
                        editorSessionImageUrls
                                .addAll(change.imageUrls); // Add all images to a list to be cleared when dashboard disconnects.
                    }
                    variant.addImageUrls(change.imageUrls);

                    String name = action.getActivityName();
                    ViewEdit viewEdit = change.viewEdit;
                    final List<ViewEdit> mapElement;
                    if (edits.containsKey(name)) {
                        mapElement = edits.get(name);
                    } else {
                        mapElement = new ArrayList<>();
                        edits.put(name, mapElement);
                    }
                    if (mapElement != null) {
                        mapElement.add(viewEdit);
                    }
                }
            }
        }

        clearEdits();

        synchronized (newEdits) {
            newEdits.clear();
            newEdits.putAll(edits);
        }
        handleNewEditsOnUiThread();
    }

    public boolean loadSnapshotConfig(JSONObject data) {
        if (snapshotConfig == null) {
            List<ViewProperty> properties = loadViewProperties(data);
            if (properties != null) {
                snapshotConfig = new SnapshotBuilder.ViewSnapshotConfig(properties, resourceIds);
            }
        }
        return snapshotConfig != null;
    }

    public void removeActivity(Activity activity) {
        activitySet.remove(activity);
    }

    public void stopVariants() {
        clearEdits();
        for (final String assetUrl : editorSessionImageUrls) {
            ImageCache.removeBitmap(assetUrl, true);
        }
        editorSessionImageUrls.clear();
        snapshotConfig = null;
    }

    public void writeSnapshot(final OutputStream out) {
        if (snapshotConfig == null) {
            getConfigLogger().debug("UIEditor: Unable to write snapshot, snapshot config not set");
            return;
        }
        try {
            SnapshotBuilder.writeSnapshot(snapshotConfig, activitySet, out, config);
        } catch (Throwable t) {
            getConfigLogger().debug("UIEditor: error writing snapshot", t);
        }
    }

    // Only call on UI Thread
    private void applyEdits(View rootView, List<ViewEdit> viewEdits) {
        synchronized (currentEdits) {
            final int size = viewEdits.size();
            for (int i = 0; i < size; i++) {
                final ViewEdit viewEdit = viewEdits.get(i);
                final UIChangeBinding binding = new UIChangeBinding(rootView, viewEdit, uiThreadHandler);
                currentEdits.add(binding);
            }
        }
    }

    private Object castArgumentObject(Object jsonArgument, String type, List<String> imageUrls) {
        try {
            if (type == null) {
                return null;
            }
            switch (type) {
                case "java.lang.CharSequence":
                case "boolean":
                case "java.lang.Boolean":
                    return jsonArgument;
                case "int":
                case "java.lang.Integer":
                    return ((Number) jsonArgument).intValue();
                case "float":
                case "java.lang.Float":
                    return ((Number) jsonArgument).floatValue();
                case "android.graphics.drawable.Drawable":
                case "android.graphics.drawable.BitmapDrawable":
                    return readBitmapDrawable((JSONObject) jsonArgument, imageUrls);
                case "android.graphics.drawable.ColorDrawable":
                    int colorValue = ((Number) jsonArgument).intValue();
                    return new ColorDrawable(colorValue);
                default:
                    getConfigLogger().verbose(getAccountId(), "UIEditor: Unhandled argument object type: " + type);
                    return null;
            }
        } catch (final ClassCastException e) {
            getConfigLogger().verbose(getAccountId(),
                    "UIEditor: Error casting class while converting argument - " + e.getLocalizedMessage());
            return null;
        }
    }

    private Integer checkIds(int explicitId, String idName, ResourceIds idNameToId) {
        final int idFromName;
        if (idName != null) {
            if (idNameToId.knownIdName(idName)) {
                idFromName = idNameToId.idFromName(idName);
            } else {
                getConfigLogger().debug(getAccountId(),
                        "UIEditor: Path element contains an id name not known to the system. No views will be matched.\n"
                                +
                                "Make sure that you're not stripping your packages R class out with proguard.\n" +
                                "id name was \"" + idName + "\""
                );
                return null;
            }
        } else {
            idFromName = -1;
        }

        if (idFromName != -1 && explicitId != -1 && idFromName != explicitId) {
            getConfigLogger().debug(getAccountId(),
                    "UIEditor: Path contains both a named and an explicit id which don't match, can't match.");
            return null;
        }

        if (-1 != idFromName) {
            return idFromName;
        }

        return explicitId;
    }

    private void clearEdits() {
        synchronized (currentEdits) {
            while (!currentEdits.isEmpty()) {
                currentEdits.removeLast().kill();//removeLast() picks up the last change added to the Deque
            }
        }
    }

    private List<ViewEdit.PathElement> generatePath(JSONArray pathDesc, ResourceIds idNameToId) throws JSONException {
        final List<ViewEdit.PathElement> path = new ArrayList<>();
        for (int i = 0; i < pathDesc.length(); i++) {
            final JSONObject targetView = pathDesc.getJSONObject(i);
            final String prefixCode = Utils.optionalStringKey(targetView, "prefix");
            final String targetViewClass = Utils.optionalStringKey(targetView, "view_class");
            final int targetIndex = targetView.optInt("index", -1);
            final String targetDescription = Utils.optionalStringKey(targetView, "contentDescription");
            final int targetExplicitId = targetView.optInt("id", -1);
            final String targetIdName = Utils.optionalStringKey(targetView, "ct_id_name");
            final String targetTag = Utils.optionalStringKey(targetView, "tag");

            final int prefix;
            if (prefixCode == null) {
                prefix = ViewEdit.PathElement.ZERO_LENGTH_PREFIX;
            } else if (prefixCode.equals("shortest")) {
                prefix = ViewEdit.PathElement.SHORTEST_PREFIX;
            } else {
                getConfigLogger().verbose(getAccountId(),
                        "UIEditor: Unrecognized prefix type \"" + prefixCode + "\". No views will be matched");
                return NEVER_MATCH_PATH;
            }
            final int targetId;
            final Integer targetIdOrNull = checkIds(targetExplicitId, targetIdName, idNameToId);
            if (targetIdOrNull == null) {
                return NEVER_MATCH_PATH;
            } else {
                targetId = targetIdOrNull;
            }
            path.add(new ViewEdit.PathElement(prefix, targetViewClass, targetIndex, targetId, targetDescription,
                    targetTag));
        }

        return path;
    }

    private UIChange generateUIChange(JSONObject data) {
        final ViewEdit viewEdit;
        final List<String> imageUrls = new ArrayList<>();
        try {
            final JSONArray pathDesc = data.getJSONArray("path");
            final List<ViewEdit.PathElement> path = generatePath(pathDesc, resourceIds);
            if (path.size() == 0) {
                getConfigLogger().verbose("UIEditor: UI change path is empty: " + data.toString());
                return null;
            }
            if (data.getString("change_type").equals("property")) {
                final JSONObject propertyDesc = data.getJSONObject("property");
                final String targetClassName = propertyDesc.getString("classname");
                if (targetClassName == null) {
                    getConfigLogger().verbose("UIEditor: UI change target classname is missing: " + data.toString());
                    return null;
                }
                final Class<?> targetClass;
                try {
                    targetClass = Class.forName(targetClassName);
                } catch (final ClassNotFoundException e) {
                    getConfigLogger().verbose(getAccountId(),
                            "UIEditor: Class not found while generating UI change - " + e.getLocalizedMessage());
                    return null;
                }
                final ViewProperty prop = generateViewProperty(targetClass, data.getJSONObject("property"));
                final JSONArray argsAndTypes = data.getJSONArray("args");
                final Object[] methodArgs = new Object[argsAndTypes.length()];
                for (int i = 0; i < argsAndTypes.length(); i++) {
                    final JSONArray argPlusType = argsAndTypes.getJSONArray(i);
                    final Object jsonArg = argPlusType.get(0);
                    final String argType = argPlusType.getString(1);
                    methodArgs[i] = castArgumentObject(jsonArg, argType, imageUrls);
                }

                ViewCaller mutator = null;
                if (prop != null) {
                    mutator = prop.createMutator(methodArgs);
                }
                if (mutator == null) {
                    getConfigLogger().verbose("UIEditor: UI change unable to create mutator: " + data.toString());
                    return null;
                }
                viewEdit = new ViewEdit(path, mutator, prop.accessor, context);
            } else {
                getConfigLogger().verbose("UIEditor: UI change type is unknown: " + data.toString());
                return null;
            }
        } catch (final NoSuchMethodException e) {
            getConfigLogger().verbose(getAccountId(),
                    "UIEditor: No such method found while generating UI change - " + e.getLocalizedMessage());
            return null;
        } catch (final JSONException e) {
            getConfigLogger().verbose(getAccountId(),
                    "UIEditor: Unable to parse JSON while generating UI change - " + e.getLocalizedMessage());
            return null;
        }
        return new UIChange(viewEdit, imageUrls);
    }

    private ViewProperty generateViewProperty(Class<?> targetClass, JSONObject property) {
        try {
            final String propName = property.getString("name");

            ViewCaller accessor = null;
            if (property.has("get")) {
                final JSONObject accessorConfig = property.getJSONObject("get");
                final String accessorName = accessorConfig.getString("selector");
                final String accessorResultTypeName = accessorConfig.getJSONObject("result").getString("type");
                final Class<?> accessorResultType = Class.forName(accessorResultTypeName);
                accessor = new ViewCaller(targetClass, accessorName, EMPTY_PARAMS, accessorResultType);
            }

            final String mutatorName;
            if (property.has("set")) {
                final JSONObject mutatorConfig = property.getJSONObject("set");
                mutatorName = mutatorConfig.getString("selector");
            } else {
                mutatorName = null;
            }
            return new ViewProperty(propName, targetClass, accessor, mutatorName);
        } catch (final NoSuchMethodException e) {
            getConfigLogger().verbose("UIEditor: Error generating view property", e);
            return null;
        } catch (final JSONException e) {
            getConfigLogger().verbose("UIEditor: Error generating view property", e);
            return null;
        } catch (final ClassNotFoundException e) {
            getConfigLogger().verbose("UIEditor: Error generating view property", e);
            return null;
        }
    }

    private String getAccountId() {
        return config.getAccountId();
    }

    private Logger getConfigLogger() {
        return config.getLogger();
    }

    private Bitmap getOrFetchBitmap(String key) {
        initImageCache();
        return ImageCache.getOrFetchBitmap(key);
    }

    // Ony call on UI Thread
    private void handleNewEdits() {
        for (final Activity activity : activitySet.getAll()) {
            final String activityName = activity.getClass().getCanonicalName();
            final View rootView = activity.getWindow().getDecorView().getRootView();

            final List<ViewEdit> specific;
            final List<ViewEdit> wildcard;
            synchronized (newEdits) {
                specific = newEdits.get(activityName);
                wildcard = newEdits.get(null);
            }
            if (specific != null) {
                applyEdits(rootView, specific);
            }
            if (wildcard != null) {
                applyEdits(rootView, wildcard);
            }
        }
    }

    private void handleNewEditsOnUiThread() {
        if (Thread.currentThread() == uiThreadHandler.getLooper().getThread()) {
            handleNewEdits();
        } else {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleNewEdits();
                }
            });
        }
    }

    // only call off main thread as initWithPersistence touches the file system
    private void initImageCache() {
        ImageCache.initWithPersistence(context);
    }

    private List<ViewProperty> loadViewProperties(JSONObject data) {
        final List<ViewProperty> properties = new ArrayList<>();
        try {
            final JSONObject config = data.getJSONObject("config");
            final JSONArray classes = config.getJSONArray("classes");
            for (int i = 0; i < classes.length(); i++) {
                final JSONObject classDesc = classes.getJSONObject(i);
                final String targetName = classDesc.getString("name");
                final Class<?> targetClass = Class.forName(targetName);
                final JSONArray props = classDesc.getJSONArray("properties");
                for (int j = 0; j < props.length(); j++) {
                    final JSONObject prop = props.getJSONObject(j);
                    final ViewProperty desc = generateViewProperty(targetClass, prop);
                    properties.add(desc);
                }
            }
        } catch (JSONException e) {
            getConfigLogger().verbose("UIEditor: Error loading view properties json: " + data.toString());
            return null;
        } catch (final ClassNotFoundException e) {
            getConfigLogger().verbose("UIEditor: Error loading view properties", e);
            return null;
        }
        return properties;
    }

    private Drawable readBitmapDrawable(JSONObject description, List<String> imageUrls) {
        try {
            final String url = description.getString("url");
            final boolean useBounds;
            final int left;
            final int right;
            final int top;
            final int bottom;
            if (description.isNull("dimensions")) {
                left = right = top = bottom = 0;
                useBounds = false;
            } else {
                final JSONObject dimensions = description.getJSONObject("dimensions");
                left = dimensions.getInt("left");
                right = dimensions.getInt("right");
                top = dimensions.getInt("top");
                bottom = dimensions.getInt("bottom");
                useBounds = true;
            }

            final Bitmap image;
            image = getOrFetchBitmap(url);
            imageUrls.add(url);

            final Drawable ret = new BitmapDrawable(Resources.getSystem(), image);
            if (useBounds) {
                ret.setBounds(left, top, right, bottom);
            }

            return ret;
        } catch (JSONException e) {
            getConfigLogger().verbose(getAccountId(),
                    "UIEditor: Unable to parse JSON while reading Bitmap from payload - " + e.getLocalizedMessage());
            return null;
        }
    }

}
