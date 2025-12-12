package com.clevertap.android.sdk.validation;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import java.util.ArrayList;
import java.util.List;

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

    public void pushValidationResult(List<ValidationResult> vrList) {
        if (vrList == null || vrList.isEmpty()) {
            return;
        }

        synchronized (pendingValidationResultsLock) {
            try {
                int currentSize = pendingValidationResults.size();
                int newSize = currentSize + vrList.size();

                if (newSize > 50) {
                    ArrayList<ValidationResult> trimmed = new ArrayList<>();

                    // Calculate how many items to skip from the existing list
                    int skipCount = newSize - 40;

                    // If we need to skip more than what we have, just use the new list
                    if (skipCount >= currentSize) {
                        // Take only the last 40 items from the new list
                        int startIndex = vrList.size() - 40;
                        for (int i = Math.max(0, startIndex); i < vrList.size(); i++) {
                            trimmed.add(vrList.get(i));
                        }
                    } else {
                        // Keep some from existing, add all new ones
                        for (int i = skipCount; i < currentSize; i++) {
                            trimmed.add(pendingValidationResults.get(i));
                        }
                        trimmed.addAll(vrList);
                    }

                    pendingValidationResults = trimmed;
                } else {
                    pendingValidationResults.addAll(vrList);
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

    @RestrictTo(Scope.LIBRARY)
    public ArrayList<ValidationResult> getPendingValidationResults() {
        return pendingValidationResults;
    }
}
