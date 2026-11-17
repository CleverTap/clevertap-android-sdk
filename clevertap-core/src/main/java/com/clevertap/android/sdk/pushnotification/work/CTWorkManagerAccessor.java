package com.clevertap.android.sdk.pushnotification.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.WorkManager;

/**
 * Gets the {@link WorkManager} instance through a Java call, on purpose.
 * <p>
 * Since WorkManager 2.10 the class is written in Kotlin. A Kotlin caller of
 * {@code WorkManager.getInstance(context)} compiles to {@code WorkManager$Companion}, a class
 * that only exists in WorkManager 2.10 and newer. A Java caller compiles to the static
 * {@code getInstance(Context)}, which every WorkManager version has. Calling it from here keeps
 * the SDK working when an app pins an older WorkManager.
 * <p>
 * Example: the app uses {@code strictly("2.9.1")}.
 * <ul>
 *   <li>Kotlin call: {@code NoClassDefFoundError: androidx/work/WorkManager$Companion}</li>
 *   <li>This call: returns the WorkManager instance</li>
 * </ul>
 * Do not convert this file to Kotlin, and do not call {@code WorkManager.getInstance} from Kotlin
 * code in the SDK; use this method instead (SDK-6153).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CTWorkManagerAccessor {

    private CTWorkManagerAccessor() {
    }

    @NonNull
    public static WorkManager getInstance(@NonNull Context context) {
        return WorkManager.getInstance(context);
    }
}
