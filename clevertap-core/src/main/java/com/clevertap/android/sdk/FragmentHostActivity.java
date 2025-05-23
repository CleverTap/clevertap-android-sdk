package com.clevertap.android.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import java.util.concurrent.atomic.AtomicBoolean;

public class FragmentHostActivity extends AppCompatActivity {

    private static final AtomicBoolean hosting = new AtomicBoolean(false);

    public static boolean isHosting() {
        return hosting.get();
    }

    public static void launch(Activity current) {
        Intent intent = new Intent(current, FragmentHostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        current.startActivity(intent);

        // try to supress any transitions
        current.overridePendingTransition(0, 0);
    }

    @Override protected void onCreate(Bundle b) {
        hosting.set(true);
        super.onCreate(b);

        // try to supress any transitions
        overridePendingTransition(0, 0); 

        // try to supress any transitions
        getSupportFragmentManager()
            .registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
                    f.setEnterTransition(null);
                    f.setExitTransition(null);
                    f.setReenterTransition(null);
                    f.setReturnTransition(null);
                }
            }, true);
    }

    @Override protected void onResume() {
        super.onResume();
    }
    
    @Override protected void onDestroy() {
        super.onDestroy();
        hosting.set(false);

        // try to supress any transitions
        overridePendingTransition(0, 0);
    }
}
