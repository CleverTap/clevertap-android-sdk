package com.clevertap.android.sdk.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_CONTENT_PAGE_INDEX
import org.json.JSONObject

/**
 * Custom OnClickListener to handle both "onMessage" and "onLink" clicks
 */
internal class CTInboxButtonClickListener : View.OnClickListener {

    private var buttonObject: JSONObject? = null

    private val buttonText: String?

    private val fragment: CTInboxListViewFragment?

    private val inboxMessage: CTInboxMessage

    private val position: Int

    private var viewPager: ViewPager? = null

    private val isBodyClick: Boolean

    private val buttonIndex: Int

    constructor(
        position: Int, inboxMessage: CTInboxMessage, buttonText: String?, jsonObject: JSONObject?,
        fragment: CTInboxListViewFragment?, isInboxMessageBodyClick: Boolean, buttonIndex: Int
    ) {
        this.position = position
        this.inboxMessage = inboxMessage
        this.buttonText = buttonText
        this.fragment = fragment // be sure to pass this as a Weak Ref
        this.buttonObject = jsonObject
        this.isBodyClick = isInboxMessageBodyClick
        this.buttonIndex = buttonIndex
    }

    constructor(
        position: Int, inboxMessage: CTInboxMessage, buttonText: String?,
        fragment: CTInboxListViewFragment?, viewPager: ViewPager?, isInboxMessageBodyClick: Boolean, buttonIndex: Int
    ) {
        this.position = position
        this.inboxMessage = inboxMessage
        this.buttonText = buttonText
        this.fragment = fragment // be sure to pass this as a Weak Ref
        this.viewPager = viewPager
        this.isBodyClick = isInboxMessageBodyClick
        this.buttonIndex = buttonIndex
    }

    override fun onClick(v: View) {
        val viewPager = viewPager
        if (viewPager != null) { //Handles viewpager clicks
            if (fragment != null) {
                fragment.handleViewPagerClick(position, viewPager.currentItem)
            }
        } else { //Handles item and button clicks for non-carousel templates
            val buttonObject = buttonObject
            if (buttonText != null && buttonObject != null) {
                if (fragment != null) {
                    if (inboxMessage.inboxMessageContents[0].getLinktype(buttonObject)!!
                            .equals(Constants.COPY_TYPE, ignoreCase = true)
                    ) { //Copy to clipboard feature
                        if (fragment.activity != null) {
                            copyToClipboard(fragment.activity!!)
                        }
                    }

                    fragment.handleClick(
                        this.position,
                        APP_INBOX_ITEM_CONTENT_PAGE_INDEX,
                        buttonText,
                        buttonObject,
                        getKeyValues(inboxMessage),
                        buttonIndex
                    )
                }
            } else {
                if (fragment != null) {
                    fragment.handleClick(this.position, APP_INBOX_ITEM_CONTENT_PAGE_INDEX, null, null, null, buttonIndex)
                }
            }
        }
    }

    private fun copyToClipboard(context: Context) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clipData = ClipData.newPlainText(
            buttonText,
            inboxMessage.inboxMessageContents[0].getLinkCopyText(buttonObject)
        )
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(context, "Text Copied to Clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Returns Custom Key Value pairs if present in the payload
     *
     * @param inboxMessage - InboxMessage object
     * @return HashMap<String, String>
     */
    private fun getKeyValues(inboxMessage: CTInboxMessage?): HashMap<String, String>? {
        // inboxMessageContents and its elements are non-null in Kotlin; the Java null checks on
        // them are folded away.
        if (inboxMessage != null &&
            Constants.KEY_KV.equals(inboxMessage.inboxMessageContents[0].getLinktype(buttonObject), ignoreCase = true)
        ) {
            return inboxMessage.inboxMessageContents[0].getLinkKeyValue(buttonObject)
        }
        return null
    }
}
