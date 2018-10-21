package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;

public abstract class CTInAppBasePartialNativeFragment extends CTInAppBasePartialFragment implements View.OnTouchListener, View.OnLongClickListener {

    View inAppView;
    final GestureDetector gd = new GestureDetector(new GestureListener());

    void setupInAppButton(Button inAppButton, final CTInAppNotificationButton inAppNotificationButton, final CTInAppNotification inAppNotification, final int buttonIndex){
        if(inAppNotificationButton!=null) {
            inAppButton.setTag(buttonIndex);
            inAppButton.setText(inAppNotificationButton.getText());
            inAppButton.setTextColor(Color.parseColor(inAppNotificationButton.getTextColor()));
            inAppButton.setBackgroundColor(Color.parseColor(inAppNotificationButton.getBackgroundColor()));
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

    void hideSecondaryButton(Button mainButton, Button secondaryButton){
        secondaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,2);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
    }

    @Override
    public boolean onLongClick(View v) {
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gd.onTouchEvent(event) || (event.getAction() == MotionEvent.ACTION_MOVE);
    }



    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private final int SWIPE_MIN_DISTANCE = 120;
        private final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // Right to left
                return remove(e1, e2, false);
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // Left to right
                return remove(e1, e2, true);
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @SuppressWarnings("UnusedParameters")
        private boolean remove(MotionEvent e1, MotionEvent e2, boolean ltr) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation anim;
            if (ltr)
                anim = new TranslateAnimation(0, getScaledPixels(50), 0, 0);
            else
                anim = new TranslateAnimation(0, -getScaledPixels(50), 0, 0);
            animSet.addAnimation(anim);
            animSet.addAnimation(new AlphaAnimation(1, 0));
            animSet.setDuration(300);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            animSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    didDismiss(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            inAppView.startAnimation(animSet);
            return true;
        }
    }
}
