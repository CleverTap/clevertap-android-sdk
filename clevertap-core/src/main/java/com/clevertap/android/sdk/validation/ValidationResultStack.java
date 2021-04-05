package com.clevertap.android.sdk.validation;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import java.util.ArrayList;

@RestrictTo(Scope.LIBRARY)
public class ValidationResultStack {

    private static final Object pendingValidationResultsLock = new Object();

    private ArrayList<ValidationResult> pendingValidationResults = new ArrayList<>();

    //Validation
    public void pushValidationResult(ValidationResult vr) {
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

    public ValidationResult popValidationResult() {
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
}
