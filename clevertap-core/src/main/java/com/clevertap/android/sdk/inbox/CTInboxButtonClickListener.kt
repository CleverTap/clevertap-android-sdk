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
 * Custom OnClickListener to handle both "onMessage" and "onLink" clicks.
 *
 * With a [viewPager] (carousels) a click opens the page on screen; otherwise it is a button click
 * when [buttonText] and [buttonObject] are given, and a message body click when they are not.
 */
internal class CTInboxButtonClickListener(
    private val position: Int,
    private val inboxMessage: CTInboxMessage,
    private val fragment: CTInboxListViewFragment?, // be sure to pass this as a Weak Ref
    private val buttonIndex: Int,
    private val buttonText: String? = null,
    private val buttonObject: JSONObject? = null,
    private val viewPager: ViewPager? = null
) : View.OnClickListener {

    override fun onClick(v: View) {
        val fragment = fragment ?: return
        if (viewPager != null) { //Handles viewpager clicks
            fragment.handleViewPagerClick(position, viewPager.currentItem)
            return
        }
        //Handles item and button clicks for non-carousel templates
        if (buttonText == null || buttonObject == null) {
            fragment.handleClick(position, APP_INBOX_ITEM_CONTENT_PAGE_INDEX, null, null, null, buttonIndex)
            return
        }
        // A link without a readable type threw from Java as well.
        val linkType = checkNotNull(inboxMessage.inboxMessageContents[0].getLinktype(buttonObject))
        if (linkType.equals(Constants.COPY_TYPE, ignoreCase = true)) { //Copy to clipboard feature
            fragment.activity?.let { copyToClipboard(it, buttonObject) }
        }

        fragment.handleClick(
            position,
            APP_INBOX_ITEM_CONTENT_PAGE_INDEX,
            buttonText,
            buttonObject,
            keyValues(buttonObject),
            buttonIndex
        )
    }

    private fun copyToClipboard(context: Context, buttonObject: JSONObject) {
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

    /** Custom key value pairs of the button, if it is a key-value link. */
    private fun keyValues(buttonObject: JSONObject): HashMap<String, String>? {
        val content = inboxMessage.inboxMessageContents[0]
        return if (Constants.KEY_KV.equals(content.getLinktype(buttonObject), ignoreCase = true)) {
            content.getLinkKeyValue(buttonObject)
        } else {
            null
        }
    }
}
