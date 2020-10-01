package com.clevertap.android.sdk.ab_testing.uieditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;

final class SnapshotBuilder {

    final static class ViewSnapshotConfig {

        final List<ViewProperty> propertyDescriptionList;

        ResourceIds resourceIds;

        ViewSnapshotConfig(List<ViewProperty> propertyDescriptions, ResourceIds resourceIds) {
            this.resourceIds = resourceIds;
            this.propertyDescriptionList = propertyDescriptions;
        }
    }

    private static class ClassCache extends LruCache<Class<?>, String> {

        ClassCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected String create(Class<?> klass) {
            return klass.getCanonicalName();
        }
    }

    private static class RootView {

        private final static String UNSPECIFIED = "unspecified";

        private final static String LANDSCAPE = "landscape";

        private final static String PORTRAIT = "portrait";

        final String activityName;

        String orientation = UNSPECIFIED;

        final View rootView;

        float scale;

        Screenshot screenshot;

        RootView(String activityName, View rootView, int activityOrientation) {
            this.activityName = activityName;
            this.rootView = rootView;
            this.screenshot = null;
            this.scale = 1.0f;
            setOrientation(activityOrientation);
        }

        private void setOrientation(final int orientation) {
            this.orientation = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? LANDSCAPE : PORTRAIT;
        }
    }

    private static class Screenshot {

        private Bitmap cachedScreenshot;

        private final Paint paint;

        Screenshot() {
            cachedScreenshot = null;
            paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        }

        @SuppressWarnings("SameParameterValue")
        synchronized void regenerate(int width, int height, int destDensity, Bitmap source) {
            if (null == cachedScreenshot || cachedScreenshot.getWidth() != width
                    || cachedScreenshot.getHeight() != height) {
                try {
                    cachedScreenshot = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                } catch (final OutOfMemoryError e) {
                    cachedScreenshot = null;
                }

                if (cachedScreenshot != null) {
                    cachedScreenshot.setDensity(destDensity);
                }
            }

            if (cachedScreenshot != null) {
                final Canvas scaledCanvas = new Canvas(cachedScreenshot);
                scaledCanvas.drawBitmap(source, 0, 0, paint);
            }
        }

        @SuppressWarnings({"SameParameterValue", "unused"})
        synchronized void writeJSON(Bitmap.CompressFormat format, int quality, OutputStream out) throws IOException {
            if (cachedScreenshot == null || cachedScreenshot.getWidth() == 0 || cachedScreenshot.getHeight() == 0) {
                out.write("null".getBytes());
            } else {
                out.write('"');
                final Base64OutputStream imageOut = new Base64OutputStream(out, Base64.NO_WRAP);
                cachedScreenshot.compress(Bitmap.CompressFormat.PNG, 100, imageOut);
                imageOut.flush();
                out.write('"');
            }
        }
    }

    private static class RootViewsGenerator implements Callable<List<RootView>> {

        private UIEditor.ActivitySet activitySet;

        private final int clientDensity = DisplayMetrics.DENSITY_DEFAULT;

        private final DisplayMetrics displayMetrics;

        private final List<RootView> rootViews;

        private final Screenshot screenshot;

        RootViewsGenerator() {
            displayMetrics = new DisplayMetrics();
            rootViews = new ArrayList<>();
            screenshot = new Screenshot();
        }

        @Override
        public List<RootView> call() {
            rootViews.clear();

            final Set<Activity> activities = this.activitySet.getAll();

            for (final Activity activity : activities) {
                final String activityName = activity.getClass().getCanonicalName();
                final int orientation = activity.getResources().getConfiguration().orientation;
                final View view = activity.getWindow().getDecorView().getRootView();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                final RootView rootView = new RootView(activityName, view, orientation);
                rootViews.add(rootView);
            }

            final int viewCount = rootViews.size();
            for (int i = 0; i < viewCount; i++) {
                final RootView rootView = rootViews.get(i);
                takeScreenshot(rootView);
            }

            return rootViews;
        }

        void findInActivities(UIEditor.ActivitySet activitySet) {
            this.activitySet = activitySet;
        }

        private void takeScreenshot(final RootView root) {
            final View rootView = root.rootView;
            Bitmap bitmap = null;

            try {
                @SuppressWarnings("JavaReflectionMemberAccess") @SuppressLint("PrivateApi") final Method
                        createSnapshot = View.class
                        .getDeclaredMethod("createSnapshot", Bitmap.Config.class, Integer.TYPE, Boolean.TYPE);
                createSnapshot.setAccessible(true);
                bitmap = (Bitmap) createSnapshot.invoke(rootView, Bitmap.Config.RGB_565, Color.WHITE, false);
            } catch (final NoSuchMethodException e) {
                Logger.v("Can't call createSnapshot, will use drawCache");
            } catch (final IllegalArgumentException e) {
                Logger.v("Can't call createSnapshot with arguments");
            } catch (final InvocationTargetException e) {
                Logger.v("Exception when calling createSnapshot", e.getLocalizedMessage());
            } catch (final IllegalAccessException e) {
                Logger.v("Can't access createSnapshot, using drawCache");
            } catch (final ClassCastException e) {
                Logger.v("createSnapshot didn't return a bitmap?", e.getLocalizedMessage());
            }

            Boolean originalCacheState = null;
            try {
                if (bitmap == null) {
                    originalCacheState = rootView.isDrawingCacheEnabled();
                    rootView.setDrawingCacheEnabled(true);
                    rootView.buildDrawingCache(true);
                    bitmap = rootView.getDrawingCache();
                }
            } catch (final RuntimeException e) {
                Logger.v("Error taking a bitmap snapshot of view " + rootView + ", skipping", e);
            }

            float scale = 1.0f;
            if (bitmap != null) {
                final int density = bitmap.getDensity();

                if (density != Bitmap.DENSITY_NONE) {
                    scale = ((float) clientDensity) / density;
                }

                final int rawWidth = bitmap.getWidth();
                final int rawHeight = bitmap.getHeight();
                final int destWidth = (int) ((bitmap.getWidth() * scale) + 0.5);
                final int destHeight = (int) ((bitmap.getHeight() * scale) + 0.5);

                if (rawWidth > 0 && rawHeight > 0 && destWidth > 0 && destHeight > 0) {
                    screenshot.regenerate(destWidth, destHeight, clientDensity, bitmap);
                }
            }

            if (originalCacheState != null && !originalCacheState) {
                rootView.setDrawingCacheEnabled(false);
            }
            root.scale = scale;
            root.screenshot = screenshot;
        }
    }

    private static final int MAX_CLASS_CACHE_SIZE = 255;

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final RootViewsGenerator rootViewsGenerator = new RootViewsGenerator();

    static private final ClassCache classCache = new ClassCache(MAX_CLASS_CACHE_SIZE);

    static void writeSnapshot(ViewSnapshotConfig snapshotConfig, UIEditor.ActivitySet liveActivities,
            OutputStream out, CleverTapInstanceConfig config) throws IOException {
        rootViewsGenerator.findInActivities(liveActivities);
        final FutureTask<List<RootView>> rootViewsFuture = new FutureTask<>(rootViewsGenerator);
        mainThreadHandler.post(rootViewsFuture);

        final OutputStreamWriter writer = new OutputStreamWriter(out);
        List<RootView> rootViewList = Collections.emptyList();
        writer.write("[");

        try {
            rootViewList = rootViewsFuture.get(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            getConfigLogger(config).debug(getAccountId(config), "Screenshot interrupted.", e);
        } catch (final TimeoutException e) {
            getConfigLogger(config).debug(getAccountId(config), "Screenshot timed out.", e);
        } catch (final ExecutionException e) {
            getConfigLogger(config).verbose(getAccountId(config), "Screenshot error", e);
        }

        final int viewCount = rootViewList.size();
        for (int i = 0; i < viewCount; i++) {
            if (i > 0) {
                writer.write(",");
            }
            final RootView rootView = rootViewList.get(i);
            writer.write("{");
            writer.write("\"activity\":");
            writer.write(JSONObject.quote(rootView.activityName));
            writer.write(",");
            writer.write("\"scale\":");
            writer.write(String.format("%s", rootView.scale));
            writer.write(",");
            writer.write("\"orientation\":");
            writer.write(JSONObject.quote(rootView.orientation));
            writer.write(",");
            writer.write("\"serialized_objects\":");
            {
                final JsonWriter j = new JsonWriter(writer);
                j.beginObject();
                j.name("rootObject").value(rootView.rootView.hashCode());
                j.name("objects");
                viewHierarchySnapshot(j, rootView.rootView, snapshotConfig);
                j.endObject();
                j.flush();
            }
            writer.write(",");
            writer.write("\"screenshot\":");
            writer.flush();
            rootView.screenshot.writeJSON(Bitmap.CompressFormat.PNG, 100, out);
            writer.write("}");
        }

        writer.write("]");
        writer.flush();
    }

    private static String getAccountId(CleverTapInstanceConfig config) {
        return config.getAccountId();
    }

    private static Logger getConfigLogger(CleverTapInstanceConfig config) {
        return config.getLogger();
    }

    private static void viewHierarchySnapshot(JsonWriter j, View rootView, ViewSnapshotConfig snapshotConfig)
            throws IOException {
        j.beginArray();
        viewSnapshot(j, rootView, snapshotConfig);
        j.endArray();
    }

    private static void viewSnapshot(JsonWriter j, View view, ViewSnapshotConfig snapshotConfig) throws IOException {
        final int viewId = view.getId();
        final String viewName;
        if (viewId == -1) {
            viewName = null;
        } else {
            viewName = snapshotConfig.resourceIds.nameForId(viewId);
        }

        j.beginObject();
        j.name("hashCode").value(view.hashCode());
        j.name("id").value(viewId);
        j.name("ct_id_name").value(viewName);

        final CharSequence contentDescription = view.getContentDescription();
        if (contentDescription == null) {
            j.name("contentDescription").nullValue();
        } else {
            j.name("contentDescription").value(contentDescription.toString());
        }

        final Object tag = view.getTag();
        if (tag == null) {
            j.name("tag").nullValue();
        } else if (tag instanceof CharSequence) {
            j.name("tag").value(tag.toString());
        }

        j.name("top").value(view.getTop());
        j.name("left").value(view.getLeft());
        j.name("width").value(view.getWidth());
        j.name("height").value(view.getHeight());
        j.name("scrollX").value(view.getScrollX());
        j.name("scrollY").value(view.getScrollY());
        j.name("visibility").value(view.getVisibility());

        float transX;
        float transY;
        transX = view.getTranslationX();
        transY = view.getTranslationY();
        j.name("translationX").value(transX);
        j.name("translationY").value(transY);

        j.name("classes");
        j.beginArray();
        Class<?> klass = view.getClass();
        do {
            j.value(classCache.get(klass));
            klass = klass.getSuperclass();
        } while (klass != Object.class && klass != null);
        j.endArray();

        writeViewProperties(j, view, snapshotConfig);

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) layoutParams;
            int[] rules = relativeLayoutParams.getRules();
            j.name("layoutRules");
            j.beginArray();
            for (int rule : rules) {
                j.value(rule);
            }
            j.endArray();
        }

        j.name("subviews");
        j.beginArray();
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                if (child != null) {
                    j.value(child.hashCode());
                }
            }
        }
        j.endArray();
        j.endObject();

        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                if (child != null) {
                    viewSnapshot(j, child, snapshotConfig);
                }
            }
        }
    }

    private static void writeViewProperties(JsonWriter j, View v, ViewSnapshotConfig snapshotConfig)
            throws IOException {
        final Class<?> viewClass = v.getClass();
        for (final ViewProperty desc : snapshotConfig.propertyDescriptionList) {
            if (desc.target.isAssignableFrom(viewClass) && null != desc.accessor) {
                final Object value = desc.accessor.invokeMethod(v);
                //noinspection StatementWithEmptyBody
                if (null == value) {
                    // no-op
                } else if (value instanceof Boolean) {
                    j.name(desc.name).value((Boolean) value);
                } else if (value instanceof Number) {
                    j.name(desc.name).value((Number) value);
                } else if (value instanceof ColorStateList) {
                    j.name(desc.name).value((Integer) ((ColorStateList) value).getDefaultColor());
                } else if (value instanceof Drawable) {
                    final Drawable drawable = (Drawable) value;
                    final Rect bounds = drawable.getBounds();
                    j.name(desc.name);
                    j.beginObject();
                    j.name("classes");
                    j.beginArray();
                    Class klass = drawable.getClass();
                    while (klass != Object.class) {
                        if (klass != null) {
                            j.value(klass.getCanonicalName());
                            klass = klass.getSuperclass();
                        }
                    }
                    j.endArray();
                    j.name("dimensions");
                    j.beginObject();
                    j.name("left").value(bounds.left);
                    j.name("right").value(bounds.right);
                    j.name("top").value(bounds.top);
                    j.name("bottom").value(bounds.bottom);
                    j.endObject();
                    if (drawable instanceof ColorDrawable) {
                        final ColorDrawable colorDrawable = (ColorDrawable) drawable;
                        j.name("color").value(colorDrawable.getColor());
                    }
                    j.endObject();
                } else {
                    j.name(desc.name).value(value.toString());
                }
            }
        }
    }

}
