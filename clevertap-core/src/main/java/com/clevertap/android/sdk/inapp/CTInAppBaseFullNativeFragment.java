package com.clevertap.android.sdk.inapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.clevertap.android.sdk.Logger;

public abstract class CTInAppBaseFullNativeFragment extends CTInAppBaseFullFragment {

    int getDPI() {
        WindowManager wm = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }
        //Returns the dpi using Device Configuration API for API30 above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Configuration configuration = context.getResources().getConfiguration();
            return configuration.densityDpi;
        }else {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.densityDpi;
        }
    }

    void setupInAppButton(Button inAppButton, final CTInAppNotificationButton inAppNotificationButton,
            final int buttonIndex) {
        if (inAppNotificationButton != null) {
            inAppButton.setVisibility(View.VISIBLE);
            inAppButton.setTag(buttonIndex);
            inAppButton.setText(inAppNotificationButton.getText());
            inAppButton.setTextColor(Color.parseColor(inAppNotificationButton.getTextColor()));
            inAppButton.setOnClickListener(new CTInAppNativeButtonClickListener());

            ShapeDrawable borderDrawable = null;
            ShapeDrawable shapeDrawable = null;

            if (!inAppNotificationButton.getBorderRadius().isEmpty()) {
                float value = Float.parseFloat(inAppNotificationButton.getBorderRadius()) * (480.0f / getDPI()) * 2;
                shapeDrawable = new ShapeDrawable(new RoundRectShape(new float[]{
                        value, value, value, value,
                        value, value, value, value
                }, null,
                        new float[]{0, 0, 0, 0, 0, 0, 0, 0}));
                shapeDrawable.getPaint().setColor(Color.parseColor(inAppNotificationButton.getBackgroundColor()));
                shapeDrawable.getPaint().setStyle(Paint.Style.FILL);
                shapeDrawable.getPaint().setAntiAlias(true);
                borderDrawable = new ShapeDrawable(new RoundRectShape(new float[]{
                        value, value, value, value,
                        value, value, value, value}, null,
                        new float[]{value, value, value, value,
                                value, value, value, value}));
            }

            if (!inAppNotificationButton.getBorderColor().isEmpty()) {
                if (borderDrawable != null) {
                    borderDrawable.getPaint().setColor(Color.parseColor(inAppNotificationButton.getBorderColor()));
                    borderDrawable.setPadding(1, 1, 1, 1);
                    borderDrawable.getPaint().setStyle(Paint.Style.FILL);
                }
            }

            if (shapeDrawable != null) {
                Drawable[] drawables = new Drawable[]{
                        borderDrawable,
                        shapeDrawable
                };

                LayerDrawable layerDrawable = new LayerDrawable(drawables);

                inAppButton.setBackground(layerDrawable);
            }
        } else {
            inAppButton.setVisibility(View.GONE);
        }
    }


}
