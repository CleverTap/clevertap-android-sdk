package com.clevertap.android.sdk.ab_testing.uieditor;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.clevertap.android.sdk.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

class ViewEdit {

    static class PathElement {
        final int prefix;
        final String viewClassName;
        public final int index;
        final int viewId;
        final String contentDescription;
        final String tag;

        static final int ZERO_LENGTH_PREFIX = 0;
        static final int SHORTEST_PREFIX = 1;

        PathElement(int usePrefix, String vClass, int ix, int vId, String cDesc, String vTag) {
            prefix = usePrefix;
            viewClassName = vClass;
            index = ix;
            viewId = vId;
            contentDescription = cDesc;
            tag = vTag;
        }

        @NonNull
        @Override
        public String toString() {
            try {
                final JSONObject ret = new JSONObject();
                if (prefix == SHORTEST_PREFIX) {
                    ret.put("prefix", "shortest");
                }
                if (null != viewClassName) {
                    ret.put("view_class", viewClassName);
                }
                if (index > -1) {
                    ret.put("index", index);
                }
                if (viewId > -1) {
                    ret.put("id", viewId);
                }
                if (null != contentDescription) {
                    ret.put("contentDescription", contentDescription);
                }
                if (null != tag) {
                    ret.put("tag", tag);
                }
                return ret.toString();
            } catch (final JSONException e) {
                throw new RuntimeException("Can't serialize PathElement to String", e);
            }
        }
    }

    private final List<PathElement> path;
    private final Pathfinder pathFinder;
    private final ViewCaller mutator;
    private final ViewCaller accessor;
    private final WeakHashMap<View, Object> originalValues;
    private final Object[] originalValueHolder;

    ViewEdit(List<PathElement> path, ViewCaller mutator, ViewCaller accessor) {
        this.path = path;
        pathFinder = new Pathfinder();
        this.mutator = mutator;
        this.accessor = accessor;
        originalValueHolder = new Object[1];
        originalValues = new WeakHashMap<>();
    }

    protected List<PathElement> getPath() {
        return this.path;
    }

    void run(View rootView) {
        pathFinder.findTargetsInRoot(rootView, path, this);
    }

    public void cleanup() {
        for (Map.Entry<View, Object> original:originalValues.entrySet()) {
            final View changedView = original.getKey();
            final Object originalValue = original.getValue();
            if (null != originalValue) {
                originalValueHolder[0] = originalValue;
                mutator.invokeMethodWithArgs(changedView, originalValueHolder);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void apply(View found) {
        if (null != accessor) {
            final Object[] setArgs = mutator.getArgs();
            if (1 == setArgs.length) {
                final Object desiredValue = setArgs[0];
                final Object currentValue = accessor.invokeMethod(found);

                if (desiredValue == currentValue) {
                    return;
                }

                if (null != desiredValue) {
                    if (desiredValue instanceof Bitmap && currentValue instanceof Bitmap) {
                        final Bitmap desiredBitmap = (Bitmap) desiredValue;
                        final Bitmap currentBitmap = (Bitmap) currentValue;
                        if (desiredBitmap.sameAs(currentBitmap)) {
                            return;
                        }
                    } else if (desiredValue instanceof BitmapDrawable && currentValue instanceof BitmapDrawable) {
                        final Bitmap desiredBitmap = ((BitmapDrawable) desiredValue).getBitmap();
                        final Bitmap currentBitmap = ((BitmapDrawable) currentValue).getBitmap();
                        if (desiredBitmap != null && desiredBitmap.sameAs(currentBitmap)) {
                            return;
                        }
                    } else if (desiredValue.equals(currentValue)) {
                        return;
                    }
                }

                if (currentValue instanceof Bitmap ||
                        currentValue instanceof BitmapDrawable ||
                        originalValues.containsKey(found)) {
                    // Cache exactly one non-image original value
                } else {
                    originalValueHolder[0] = currentValue;
                    if (mutator.argsAreApplicable(originalValueHolder)) {
                        originalValues.put(found, currentValue);
                    } else {
                        originalValues.put(found, null);
                    }
                }
            }
        }
        mutator.invokeMethod(found);
    }
    protected String name() {
        return "Property Mutator";
    }

    private class Pathfinder {
        private final IntStack indexStack;

        Pathfinder() {
            indexStack = new Pathfinder.IntStack();
        }

        void findTargetsInRoot(View givenRootView, List<PathElement> path, ViewEdit viewEdit) {
            if (path.isEmpty()) {
                return;
            }

            if (indexStack.isFull()) {
                Logger.v("There appears to be a concurrency issue in the pathfinding code. Path will not be matched.");
                return;
            }

            final PathElement rootPathElement = path.get(0);
            final List<PathElement> childPath = path.subList(1, path.size());

            final int indexKey = indexStack.allocate();
            final View rootView = findPrefixedMatch(rootPathElement, givenRootView, indexKey);
            indexStack.free();

            if (null != rootView) {
                findTargetsInMatchedView(rootView, childPath, viewEdit);
            }
        }

        private void findTargetsInMatchedView(View alreadyMatched, List<PathElement> remainingPath, ViewEdit viewEdit) {
            // When this is run, alreadyMatched has already been matched to a path prefix.
            // path is a possibly empty "remaining path" suffix left over after the match

            if (remainingPath.isEmpty()) {
                // Nothing left to match- we're found!
                viewEdit.apply(alreadyMatched);
                return;
            }

            if (!(alreadyMatched instanceof ViewGroup)) {
                // Matching a non-empty path suffix is impossible, because we have no children
                return;
            }

            if (indexStack.isFull()) {
                Logger.v("Path is too deep, will not match");
                // Can't match anyhow, stack is too deep
                return;
            }

            final ViewGroup parent = (ViewGroup) alreadyMatched;
            final PathElement matchElement = remainingPath.get(0);
            final List<PathElement> nextPath = remainingPath.subList(1, remainingPath.size());

            final int childCount = parent.getChildCount();
            final int indexKey = indexStack.allocate();
            for (int i = 0; i < childCount; i++) {
                final View givenChild = parent.getChildAt(i);
                final View child = findPrefixedMatch(matchElement, givenChild, indexKey);
                if (null != child) {
                    findTargetsInMatchedView(child, nextPath, viewEdit);
                }
                if (matchElement.index >= 0 && indexStack.read(indexKey) > matchElement.index) {
                    break;
                }
            }
            indexStack.free();
        }

        // Finds the first matching view of the path element in the given subject's view hierarchy.
        // If the path is indexed, it needs a start index, and will consume some indexes
        private View findPrefixedMatch(PathElement findElement, View subject, int indexKey) {
            final int currentIndex = indexStack.read(indexKey);
            if (matches(findElement, subject)) {
                indexStack.increment(indexKey);
                if (findElement.index == -1 || findElement.index == currentIndex) {
                    return subject;
                }
            }

            if (findElement.prefix == PathElement.SHORTEST_PREFIX && subject instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) subject;
                final int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = group.getChildAt(i);
                    final View result = findPrefixedMatch(findElement, child, indexKey);
                    if (null != result) {
                        return result;
                    }
                }
            }

            return null;
        }

        private boolean matches(PathElement matchElement, View subject) {
            if (null != matchElement.viewClassName &&
                    !hasClassName(subject, matchElement.viewClassName)) {
                return false;
            }

            if (-1 != matchElement.viewId && subject.getId() != matchElement.viewId) {
                return false;
            }

            if (null != matchElement.contentDescription &&
                    !matchElement.contentDescription.contentEquals(subject.getContentDescription())) {
                return false;
            }

            final String matchTag = matchElement.tag;
            if (null != matchElement.tag) {
                final Object subjectTag = subject.getTag();
                return null != subjectTag && matchTag.equals(subject.getTag().toString());
            }
            return true;
        }

        private boolean hasClassName(Object o, String className) {
            Class<?> klass = o.getClass();
            while (true) {
                //noinspection ConstantConditions
                String klassCanonicalName = klass.getCanonicalName();
                if (klassCanonicalName != null && klassCanonicalName.equals(className)) {
                    return true;
                }

                if (klass == Object.class) {
                    return false;
                }

                klass = klass.getSuperclass();
            }
        }

        private class IntStack {
            private final int[] stack;
            private int stackSize;

            private static final int MAX_SIZE = 256;

            IntStack() {
                stack = new int[MAX_SIZE];
                stackSize = 0;
            }

            boolean isFull() {
                return stack.length == stackSize;
            }

            int allocate() {
                final int index = stackSize;
                stackSize++;
                stack[index] = 0;
                return index;
            }

            public int read(int index) {
                return stack[index];
            }

            void increment(int index) {
                stack[index]++;
            }

            public void free() {
                stackSize--;
                if (stackSize < 0) {
                    throw new ArrayIndexOutOfBoundsException(stackSize);
                }
            }
        }
    }
}
