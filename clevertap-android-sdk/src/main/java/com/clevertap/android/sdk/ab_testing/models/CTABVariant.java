package com.clevertap.android.sdk.ab_testing.models;

import com.clevertap.android.sdk.ImageCache;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CTABVariant {

    final public class CTVariantAction {
        private String name;
        private String activityName;
        private JSONObject change;

        CTVariantAction(String name, String activityName, JSONObject change) {
            this.name = name;
            this.activityName = activityName;
            this.change = change;
        }

        public String getName() {
            return name;
        }

        public String getActivityName() {
            return activityName;
        }

        public JSONObject getChange() {
            return change;
        }
    }

    private String id;
    private String variantId;
    private String experimentId;
    private Integer version;
    private ArrayList<CTVariantAction> actions = new ArrayList<>();
    private JSONArray vars = new JSONArray();
    private final Object actionsLock = new Object();
    private ArrayList<String> imageUrls;

    public CTABVariant(JSONObject variant) {
        try {
            variantId = variant.optString("id", "0");
            experimentId = variant.optString("experiment_id", "0");
            version = variant.optInt("variant_version", 0);
            this.id = variantId+":"+experimentId;
            imageUrls = new ArrayList<>();
            try {
                final JSONArray actions = variant.optJSONArray("actions");
                addActions(actions);
            } catch (Throwable t) {
                Logger.v("Error loading variant actions", t);
            }
            try {
                this.vars = variant.optJSONArray("vars");
            } catch (Throwable t) {
                Logger.v("Error loading variant vars", t);
            }
        } catch (Throwable t) {
            Logger.v("Error creating variant", t);
        }
    }

    public String getId() {
        return id;
    }

    @SuppressWarnings("unused")
    String getVariantId() {
        return variantId;
    }

    @SuppressWarnings("unused")
    String getExperimentId() {
        return experimentId;
    }

    public void addActions(JSONArray actions) {
        synchronized (actionsLock) {
            if (actions == null || actions.length() <= 0) { return; }
            final int actionsLength = actions.length();
            try {
                for (int j = 0; j < actionsLength; j++) {
                    final JSONObject change = actions.getJSONObject(j);
                    if (change == null) {
                        continue;
                    }
                    final String targetActivity = Utils.optionalStringKey(change, "target_activity");
                    final String name = change.getString("name");
                    final CTVariantAction action = new CTVariantAction(name, targetActivity, change);
                    this.actions.add(action);
                }
            } catch (Throwable t) {
                Logger.v("Error adding variant actions", t);
            }
        }
    }

    public void removeActionsByName(JSONArray names) {
        if (names == null || names.length() <=0) {
            return;
        }
        synchronized (actionsLock) {
            ArrayList<String> _names = new ArrayList<>();
            for (int i=0; i<names.length(); i++ ) {
                try {
                    _names.add(names.getString(i));
                } catch (Throwable t) {
                    // no-op
                }
            }
            ArrayList<CTVariantAction> newActions = new ArrayList<>();
            for (CTVariantAction action: actions) {
                if (!_names.contains(action.getName())) {
                    newActions.add(action);
                }
            }
            this.actions = newActions;
        }
    }

    public void clearActions() {
        synchronized (actionsLock) {
            actions.clear();
        }
    }

    public ArrayList<CTVariantAction> getActions() {
        synchronized (actionsLock) {
            return this.actions;
        }
    }

    public JSONArray getVars() {
        return vars;
    }

    public Integer getVersion() {
        return version;
    }

    public void addImageUrls(List<String>urls) {
        if (urls == null) return;
        this.imageUrls.addAll(urls);
    }

    public void cleanup() {
        for (String url: imageUrls) {
            ImageCache.removeBitmap(url, true);
        }
    }
}
