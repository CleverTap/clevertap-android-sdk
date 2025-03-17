package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils

internal class InAppActionHandler(private val logger: Logger) {

    fun openUrl(url: String, launchContext: Context): Boolean {
        try {
            val uri = Uri.parse(url.replace("\n", "").replace("\r", ""))
            val queryParamSet = uri.getQueryParameterNames()
            val queryBundle = Bundle()
            if (queryParamSet != null && !queryParamSet.isEmpty()) {
                for (queryName in queryParamSet) {
                    queryBundle.putString(queryName, uri.getQueryParameter(queryName))
                }
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (!queryBundle.isEmpty()) {
                intent.putExtras(queryBundle)
            }

            if (launchContext == launchContext.applicationContext) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Utils.setPackageNameFromResolveInfoList(launchContext, intent)
            launchContext.startActivity(intent)
            return true
        } catch (_: Exception) {
            if (url.startsWith(Constants.WZRK_URL_SCHEMA)) {
                // Ignore logging CT scheme actions
                return true
            }
            logger.debug("No activity found to open url: $url")
            return false
        }
    }
}
