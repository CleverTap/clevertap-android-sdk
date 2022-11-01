package com.clevertap.android.sdk;

/**
 * Internal interface for communication between fragment and its respective activity when action buttons
 * are clicked via InApp/Inbox payload.
 */
public interface DidClickForHardPermissionListener {
    void didClickForHardPermissionWithFallbackSettings(boolean fallbackToSettings);
}