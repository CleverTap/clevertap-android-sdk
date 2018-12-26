package com.clevertap.android.sdk;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;

class CTInboxButtonClickListener implements View.OnClickListener {

    private int position,viewPagerPosition = -1;
    private CTInboxMessage inboxMessage;
    private Button button;
    private Fragment fragment;
    private Activity activity;

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

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, Activity activity, int viewPagerPosition) {
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.activity = activity;
        this.viewPagerPosition = viewPagerPosition;
    }

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, Fragment fragment, int viewPagerPosition) {
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.fragment = fragment;
        this.viewPagerPosition = viewPagerPosition;
    }


    @Override
    public void onClick(View v) {
        if(viewPagerPosition != -1){
            if(fragment != null) {
                ((CTInboxTabBaseFragment) fragment).handleViewPagerClick(position, viewPagerPosition);
            }else if(activity != null){
                ((CTInboxActivity)activity).handleViewPagerClick(position,viewPagerPosition);
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
