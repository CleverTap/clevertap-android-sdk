package com.clevertap.android.sdk;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public abstract class CTInAppBaseFullFragment extends CTInAppBaseFragment {

    @Override
    void cleanup() {/* no-op */}

    @Override
    void generateListener() {
        if (parent instanceof InAppNotificationActivity) {
            setListener((CTInAppBaseFragment.InAppListener) parent);
        }
    }

    boolean isTablet() {
        if (Utils.isActivityDead(getActivity())) {
            return false;
        }
        WindowManager wm = (WindowManager) parent.getBaseContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            Logger.v("Screen size is null ");
            return false;
        }
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        float yInches = dm.heightPixels / dm.ydpi;
        float xInches = dm.widthPixels / dm.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        if (diagonalInches >= 7) {
            Logger.v("Screen size is : " + diagonalInches);
            return true;
        } else {
            Logger.v("Screen size is : " + diagonalInches);
            return false;
        }
    }
}
