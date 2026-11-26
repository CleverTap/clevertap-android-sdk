package com.clevertap.android.sdk.response

import android.content.Context
import org.json.JSONObject
import androidx.annotation.RestrictTo

/**
 * Abstract Decorator that will be used to decorate
 *
 * Extend this class to create different kind of response
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class CleverTapResponseDecorator : CleverTapResponse() {

    abstract override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context)
}
