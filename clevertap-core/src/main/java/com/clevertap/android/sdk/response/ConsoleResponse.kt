package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONObject

internal open class ConsoleResponse(private val config: CleverTapInstanceConfig) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        val response = jsonBody
        // Handle "console" - print them as info to the console
        try {
            /*
             * Console info is no longer used
             * But the feature was to enable logs from LCr
             */
            if (response!!.has("console")) {
                val console = response.get("console") as JSONArray
                if (console.length() > 0) {
                    for (i in 0 until console.length()) {
                        logger.debug(config.accountId, console.get(i).toString())
                    }
                }
            }
        } catch (t: Throwable) {
            // Ignore
        }

        // Handle server set debug level
        try {
            if (response!!.has("dbg_lvl")) {
                val debugLevel = response.getInt("dbg_lvl")
                if (debugLevel >= 0) {
                    CleverTapAPI.setDebugLevel(debugLevel)
                    logger.verbose(
                        config.accountId,
                        "Set debug level to $debugLevel for this session (set by upstream)"
                    )
                }
            }
        } catch (t: Throwable) {
            // Ignore
        }
    }
}
