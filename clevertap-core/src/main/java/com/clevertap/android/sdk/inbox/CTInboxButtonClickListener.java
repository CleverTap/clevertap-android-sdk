package com.clevertap.android.sdk.inbox;

import static com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_CONTENT_PAGE_INDEX;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Toast;
import androidx.viewpager.widget.ViewPager;
import com.clevertap.android.sdk.Constants;
import java.util.HashMap;
import org.json.JSONObject;

/**
 * Custom OnClickListener to handle both "onMessage" and "onLink" clicks
 */
class CTInboxButtonClickListener implements View.OnClickListener {

    private JSONObject buttonObject;

    private final String buttonText;

    private final CTInboxListViewFragment fragment;

    private final CTInboxMessage inboxMessage;

    private final int position;

    private ViewPager viewPager;

    private final boolean isBodyClick;

    private final int buttonIndex;

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, String buttonText, JSONObject jsonObject,
            CTInboxListViewFragment fragment, boolean isInboxMessageBodyClick, int buttonIndex) {
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.buttonText = buttonText;
        this.fragment = fragment; // be sure to pass this as a Weak Ref
        this.buttonObject = jsonObject;
        this.isBodyClick = isInboxMessageBodyClick;
        this.buttonIndex = buttonIndex;
    }

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, String buttonText,
            CTInboxListViewFragment fragment, ViewPager viewPager, boolean isInboxMessageBodyClick, int buttonIndex) {
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.buttonText = buttonText;
        this.fragment = fragment; // be sure to pass this as a Weak Ref
        this.viewPager = viewPager;
        this.isBodyClick = isInboxMessageBodyClick;
        this.buttonIndex = buttonIndex;
    }


    @Override
    public void onClick(View v) {
        if (viewPager != null) {//Handles viewpager clicks
            if (fragment != null) {
                fragment.handleViewPagerClick(position, viewPager.getCurrentItem());
            }
        } else {//Handles item and button clicks for non-carousel templates
            if (buttonText != null && buttonObject != null) {
                if (fragment != null) {
                    if (inboxMessage.getInboxMessageContents().get(0).getLinktype(buttonObject)
                            .equalsIgnoreCase(Constants.COPY_TYPE)) {//Copy to clipboard feature
                        if (fragment.getActivity() != null) {
                            copyToClipboard(fragment.getActivity());
                        }
                    }

                    fragment.handleClick(this.position, APP_INBOX_ITEM_CONTENT_PAGE_INDEX, buttonText, buttonObject, getKeyValues(inboxMessage), buttonIndex);
                }
            } else {
                if (fragment != null) {
                    fragment.handleClick(this.position, APP_INBOX_ITEM_CONTENT_PAGE_INDEX,null, null, null, buttonIndex);
                }
            }
        }
    }

    private void copyToClipboard(Context context) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(buttonText,
                inboxMessage.getInboxMessageContents().get(0).getLinkCopyText(buttonObject));
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(context, "Text Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns Custom Key Value pairs if present in the payload
     *
     * @param inboxMessage - InboxMessage object
     * @return HashMap<String, String>
     */
    private HashMap<String, String> getKeyValues(CTInboxMessage inboxMessage) {
        if (inboxMessage != null
                && inboxMessage.getInboxMessageContents() != null
                && inboxMessage.getInboxMessageContents().get(0) != null
                && Constants.KEY_KV
                .equalsIgnoreCase(inboxMessage.getInboxMessageContents().get(0).getLinktype(buttonObject))) {

            return inboxMessage.getInboxMessageContents().get(0).getLinkKeyValue(buttonObject);
        }
        return null;
    }
}
