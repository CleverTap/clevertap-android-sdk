package com.clevertap.android.sdk.ab_testing.uieditor;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.JSONException;
import org.json.JSONObject;

class ViewEdit {

    static class PathElement {

        static final int ZERO_LENGTH_PREFIX = 0;

        static final int SHORTEST_PREFIX = 1;

        public final int index;

        final String contentDescription;

        final int prefix;

        final String tag;

        final String viewClassName;

        final int viewId;

        PathElement(int usePrefix, String className, int idx, int id, String cDesc, String vTag) {
            prefix = usePrefix;
            viewClassName = className;
            index = idx;
            viewId = id;
            contentDescription = cDesc;
            tag = vTag;
        }

        @NonNull
        @Override
        public String toString() {
            try {
                final JSONObject s = new JSONObject();
                if (prefix == SHORTEST_PREFIX) {
                    s.put("prefix", "shortest");
                }
                if (null != viewClassName) {
                    s.put("view_class", viewClassName);
                }
                if (index > -1) {
                    s.put("index", index);
                }
                if (viewId > -1) {
                    s.put("id", viewId);
                }
                if (null != contentDescription) {
                    s.put("contentDescription", contentDescription);
                }
                if (null != tag) {
                    s.put("tag", tag);
                }
                return s.toString();
            } catch (final JSONException e) {
                throw new RuntimeException("Can't serialize PathElement to String", e);
            }
        }
    }

    private class Pathfinder {

        private class IntStack {

            private static final int MAX_SIZE = 256;

            private final int[] stack;

            private int stackSize;

            IntStack() {
                stack = new int[MAX_SIZE];
                stackSize = 0;
            }

            public void free() {
                stackSize--;
                if (stackSize < 0) {
                    throw new ArrayIndexOutOfBoundsException(stackSize);
                }
            }

            public int read(int index) {
                return stack[index];
            }

            int allocate() {
                final int index = stackSize;
                stackSize++;
                stack[index] = 0;
                return index;
            }

            void increment(int index) {
                stack[index]++;
            }

            boolean isFull() {
                return stack.length == stackSize;
            }
        }

        private final IntStack indexStack;

        Pathfinder() {
            indexStack = new Pathfinder.IntStack();
        }

        void findTargetsInRoot(View givenRootView, List<PathElement> path, ViewEdit viewEdit) {
            if (path.isEmpty()) {
                return;
            }
            if (indexStack.isFull()) {
                Logger.v(
                        "There appears to be a concurrency issue in the pathfinding code. Path will not be matched.");
                return;
            }

            final PathElement rootPathElement = path.get(0);
            final List<PathElement> childPath = path.subList(1, path.size());

            final int indexKey = indexStack.allocate();
            final View rootView = findMatch(rootPathElement, givenRootView, indexKey);
            indexStack.free();

            if (rootView != null) {
                findTargetsInMatchedView(rootView, childPath, viewEdit);
            }
        }

        private View findMatch(PathElement pathElement, View target, int indexKey) {
            final int currentIndex = indexStack.read(indexKey);
            if (matches(pathElement, target)) {
                indexStack.increment(indexKey);
                if (pathElement.index == -1 || pathElement.index == currentIndex) {
                    return target;
                }
            }
            if (pathElement.prefix == PathElement.SHORTEST_PREFIX && target instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) target;
                final int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = group.getChildAt(i);
                    final View result = findMatch(pathElement, child, indexKey);
                    if (null != result) {
                        return result;
                    }
                }
            }

            return null;
        }

        private void findTargetsInMatchedView(View alreadyMatched, List<PathElement> remainingPath,
                ViewEdit viewEdit) {
            if (remainingPath.isEmpty()) {
                // found, apply the edit
                viewEdit.apply(alreadyMatched);
                return;
            }

            if (!(alreadyMatched instanceof ViewGroup)) {
                // Not possible as there are no children
                return;
            }

            if (indexStack.isFull()) {
                Logger.v("Path too deep, will not match");
                return;
            }

            final ViewGroup parent = (ViewGroup) alreadyMatched;
            final PathElement pathElement = remainingPath.get(0);
            final List<PathElement> next = remainingPath.subList(1, remainingPath.size());

            final int childCount = parent.getChildCount();
            final int indexKey = indexStack.allocate();
            for (int i = 0; i < childCount; i++) {
                final View givenChild = parent.getChildAt(i);
                final View child = findMatch(pathElement, givenChild, indexKey);
                if (child != null) {
                    findTargetsInMatchedView(child, next, viewEdit);
                }
                if (pathElement.index >= 0 && indexStack.read(indexKey) > pathElement.index) {
                    break;
                }
            }
            indexStack.free();
        }

        private boolean hasClassName(Object o, String className) {
            Class<?> klass = o.getClass();
            while (true) {
                //noinspection ConstantConditions
                String klassCanonicalName = klass.getCanonicalName();
                return klassCanonicalName != null && klassCanonicalName.equals(className);
            }
        }

        private boolean matches(PathElement pathElement, View target) {
            if (pathElement.viewClassName != null && !hasClassName(target, pathElement.viewClassName)) {
                return false;
            }
            if (pathElement.viewId != -1 && (target.getId() != pathElement.viewId)) {
                return false;
            }
            if (pathElement.contentDescription != null && !pathElement.contentDescription
                    .contentEquals(target.getContentDescription())) {
                return false;
            }

            final String matchTag = pathElement.tag;
            if (pathElement.tag != null) {
                final Object targetTag = target.getTag();
                return targetTag != null && matchTag.equals(target.getTag().toString());
            }
            return true;
        }
    }

    private final ViewCaller accessor;

    private Context context;

    private final ViewCaller mutator;

    private final Object[] originalValueHolder;

    private final WeakHashMap<View, Object> originalValues;

    private final List<PathElement> path;

    private final Pathfinder pathFinder;

    ViewEdit(List<PathElement> path, ViewCaller mutator, ViewCaller accessor, Context context) {
        this.path = path;
        pathFinder = new Pathfinder();
        this.mutator = mutator;
        this.accessor = accessor;
        originalValueHolder = new Object[1];
        originalValues = new WeakHashMap<>();
        this.context = context;
    }

    protected String name() {
        return "Property Mutator";
    }

    void cleanup() {
        for (Map.Entry<View, Object> original : originalValues.entrySet()) {
            final View changedView = original.getKey();
            final Object originalValue = original.getValue();
            if (originalValue != null) {
                if (originalValue instanceof ColorStateList) {
                    originalValueHolder[0] = ((ColorStateList) originalValue).getDefaultColor();
                } else {
                    originalValueHolder[0] = originalValue;
                }
                mutator.invokeMethodWithArgs(changedView, originalValueHolder);
            }
        }
    }

    void run(View rootView) {
        pathFinder.findTargetsInRoot(rootView, getPath(), this);
    }

    private void apply(View targetView) {
        if (accessor != null) {
            final Object[] args = mutator.getArgs();
            if (args.length == 1) {
                final Object targetValue = args[0];
                Object currentValue = accessor.invokeMethod(targetView);

                if (accessor.getMethodName().equals("getTextSize")) {
                    currentValue = (float) currentValue / context.getResources().getDisplayMetrics().scaledDensity;
                }

                if (targetValue == currentValue) {
                    return;
                }

                if (targetValue != null) {
                    if (targetValue instanceof Bitmap && currentValue instanceof Bitmap) {
                        final Bitmap targetBitmap = (Bitmap) targetValue;
                        final Bitmap currentBitmap = (Bitmap) currentValue;
                        if (targetBitmap.sameAs(currentBitmap)) {
                            return;
                        }
                    } else if (targetValue instanceof BitmapDrawable && currentValue instanceof BitmapDrawable) {
                        final Bitmap targetBitmap = ((BitmapDrawable) targetValue).getBitmap();
                        final Bitmap currentBitmap = ((BitmapDrawable) currentValue).getBitmap();
                        if (targetBitmap != null && targetBitmap.sameAs(currentBitmap)) {
                            return;
                        }
                    } else if (targetValue.equals(currentValue)) {
                        return;
                    }
                }
                if (!originalValues.containsKey(targetView)) {
                    originalValueHolder[0] = currentValue;
                    if (mutator.argsAreApplicable(originalValueHolder)) {
                        originalValues.put(targetView, currentValue);
                    } else {
                        originalValues.put(targetView, null);
                    }
                }
            }
        }
        mutator.invokeMethod(targetView);
    }

    private List<PathElement> getPath() {
        return this.path;
    }
}
