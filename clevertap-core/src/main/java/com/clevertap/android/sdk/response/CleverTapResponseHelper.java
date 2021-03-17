package com.clevertap.android.sdk.response;

/**
 * The Concrete Response class whose object we’re going to dynamically add new behavior to.
 * for ex:<pre>{@code}CleverTapResponse cleverTapResponse = new CleverTapResponseHelper();
 *                 cleverTapResponse = new GeofenceResponse(cleverTapResponse);
 *                 cleverTapResponse = new InAppResponse(cleverTapResponse);
 *         </pre>
 */
public class CleverTapResponseHelper extends CleverTapResponse {}