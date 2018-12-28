package com.clevertap.android.sdk;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

class CTInboxButtonClickListener implements View.OnClickListener {

    private int position;
    private CTInboxMessage inboxMessage;
    private Button button;
    private Fragment fragment;
    private Activity activity;
    private ViewPager viewPager;

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, Fragment fragment){
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.fragment = fragment;
    }


    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, Activity activity){
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.activity = activity;
    }

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, Activity activity, ViewPager viewPager) {
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.activity = activity;
        this.viewPager = viewPager;
    }

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, Fragment fragment, ViewPager viewPager) {
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.fragment = fragment;
        this.viewPager = viewPager;
    }


    @Override
    public void onClick(View v) {
        if(viewPager != null){
            if(fragment != null) {
                ((CTInboxTabBaseFragment) fragment).handleViewPagerClick(position, viewPager.getCurrentItem());
            }else if(activity != null){
                ((CTInboxActivity)activity).handleViewPagerClick(position,viewPager.getCurrentItem());
            }
        }else{
            if(button != null) {
                if(fragment != null) {
                    ((CTInboxTabBaseFragment) fragment).handleClick(this.position, button.getText().toString());
                }else if(activity != null){
                    ((CTInboxActivity) activity).handleClick(this.position, button.getText().toString());
                }
            }else{
                if(fragment != null) {
                    ((CTInboxTabBaseFragment) fragment).handleClick(this.position, null);
                }else if(activity != null){
                    ((CTInboxActivity) activity).handleClick(this.position, null);
                }
            }
        }
    }
}
