package com.clevertap.android.sdk;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;
import android.widget.Button;

public abstract class CTInAppBaseFullNativeFragment extends CTInAppBaseFullFragment {

    void setupInAppButton(Button inAppButton, final CTInAppNotificationButton inAppNotificationButton, final CTInAppNotification inAppNotification, final int buttonIndex){
        if(inAppNotificationButton!=null) {
            inAppButton.setVisibility(View.VISIBLE);
            inAppButton.setTag(buttonIndex);
            inAppButton.setText(inAppNotificationButton.getText());
            inAppButton.setTextColor(Color.parseColor(inAppNotificationButton.getTextColor()));
            //inAppButton.setBackgroundColor(Color.parseColor(inAppNotificationButton.getBackgroundColor()));
            inAppButton.setOnClickListener(new CTInAppNativeButtonClickListener());
            ShapeDrawable borderDrawable = null;
            ShapeDrawable shapeDrawable = null;

            if(!inAppNotificationButton.getBorderRadius().isEmpty()) {
               shapeDrawable = new ShapeDrawable(new RoundRectShape(new float[]{Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                        Float.parseFloat(inAppNotificationButton.getBorderRadius())*2},null,
                        new float[]{0,0,0,0,0,0,0,0}));
                shapeDrawable.getPaint().setColor(Color.parseColor(inAppNotificationButton.getBackgroundColor()));
                shapeDrawable.getPaint().setStyle(Paint.Style.FILL);
                shapeDrawable.getPaint().setAntiAlias(true);
                borderDrawable = new ShapeDrawable(new RoundRectShape(new float[]{Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                      Float.parseFloat(inAppNotificationButton.getBorderRadius())*2},null,
                        new float[]{Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2,
                                Float.parseFloat(inAppNotificationButton.getBorderRadius())*2}));
            }

            if(!inAppNotificationButton.getBorderColor().isEmpty()) {
                if(borderDrawable!=null) {
                    borderDrawable.getPaint().setColor(Color.parseColor(inAppNotificationButton.getBorderColor()));
                    borderDrawable.setPadding(1,1,1,1);
                    borderDrawable.getPaint().setStyle(Paint.Style.FILL);
                }
            }

            if(shapeDrawable!=null) {
                Drawable[] drawables = new Drawable[]{
                        borderDrawable,
                        shapeDrawable
                };

                LayerDrawable layerDrawable = new LayerDrawable(drawables);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    inAppButton.setBackground(layerDrawable);
                } else {
                    inAppButton.setBackgroundDrawable(layerDrawable);
                }
            }
        }else{
            inAppButton.setVisibility(View.GONE);
        }
    }
}
