package com.clevertap.android.sdk;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONObject;

class CTInboxButtonClickListener implements View.OnClickListener {

    private int position;
    private CTInboxMessage inboxMessage;
    private Button button;
    private Fragment fragment;
    private Activity activity;
    private ViewPager viewPager;
    private JSONObject buttonObject;

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, JSONObject jsonObject, Fragment fragment){
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.fragment = fragment;
        this.buttonObject = jsonObject;
    }


    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button, JSONObject jsonObject, Activity activity){
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
        this.activity = activity;
        this.buttonObject = jsonObject;
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
            if(button != null && buttonObject != null) {
                if(fragment != null) {
                    if(inboxMessage.getInboxMessageContents().get(0).getLinktype(buttonObject).equalsIgnoreCase("copytext")) {
                        if(fragment.getActivity() !=null) {
                            copyToClipboard(fragment.getActivity(), buttonObject);
                        }
                        ((CTInboxTabBaseFragment) fragment).handleClick(this.position, button.getText().toString());
                    }else{
                        ((CTInboxTabBaseFragment) fragment).handleClick(this.position, button.getText().toString());
                    }
                }else if(activity != null){
                    if(activity.getApplicationContext() !=null) {
                        copyToClipboard(activity.getApplicationContext(), buttonObject);
                    }
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

    private void copyToClipboard(Context context, JSONObject jsonObject){
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(button.getText(),inboxMessage.getInboxMessageContents().get(0).getLinkCopyText(buttonObject));
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(context,"Text Copied to Clipboard",Toast.LENGTH_SHORT).show();
        }
    }
}
