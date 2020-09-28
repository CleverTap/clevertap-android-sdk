package com.clevertap.android.sdk.featureFlags;

public interface FeatureFlagListener {

    void featureFlagsDidUpdate();

    void fetchFeatureFlags();
}
