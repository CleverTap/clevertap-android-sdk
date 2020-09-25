package com.clevertap.android.sdk;

import java.util.ArrayList;

class ValidationResultStack {

    private static final Boolean pendingValidationResultsLock = true;

    private ArrayList<ValidationResult> pendingValidationResults = new ArrayList<>();

    ValidationResult popValidationResult() {
        // really a shift
        ValidationResult vr = null;

        synchronized (pendingValidationResultsLock) {
            try {
                if (!pendingValidationResults.isEmpty()) {
                    vr = pendingValidationResults.remove(0);
                }
            } catch (Exception e) {
                // no-op
            }
        }
        return vr;
    }

    //Validation
    void pushValidationResult(ValidationResult vr) {
        synchronized (pendingValidationResultsLock) {
            try {
                int len = pendingValidationResults.size();
                if (len > 50) {
                    ArrayList<ValidationResult> trimmed = new ArrayList<>();
                    // Trim down the list to 40, so that this loop needn't run for the next 10 events
                    // Hence, skip the first 10 elements
                    for (int i = 10; i < len; i++) {
                        trimmed.add(pendingValidationResults.get(i));
                    }
                    trimmed.add(vr);
                    pendingValidationResults = trimmed;
                } else {
                    pendingValidationResults.add(vr);
                }
            } catch (Exception e) {
                // no-op
            }
        }
    }
}
