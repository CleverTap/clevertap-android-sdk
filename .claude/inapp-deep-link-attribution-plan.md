# Implementation Plan: InApp Deep Link Click Attribution (wzrk_dl)

## Context

Currently, the CleverTap Android SDK captures different attribution data for Push vs InApp notifications:

- **Push notifications** capture both `wzrk_c2a` (call-to-action) AND `wzrk_dl` (deep link URL) on click events
- **InApp notifications** only capture `wzrk_c2a` (button text) but NOT `wzrk_dl`

This inconsistency prevents proper deep link attribution analysis for InApp campaigns. This implementation will add `wzrk_dl` to InApp click events to match Push notification behavior and enable complete click attribution tracking.

### PRD Requirements Summary

- Add `wzrk_dl` property to existing InApp click events (no new event name)
- Support button-level deep links (CTA buttons with specific URLs)
- Support template-level deep links (image-only and HTML templates)
- Handle multi-CTA templates (capture the specific clicked button's deep link)
- Support user-level personalized deep links
- Don't capture deep links for actions without navigation (CLOSE, Ratings, Lead Gen, etc.)

## Implementation Approach

### Core Changes

All changes are in a single file: **`InAppController.kt`**

The implementation adds deep link extraction logic to the existing click event flow at the point where `wzrk_c2a` is already being captured.

#### 1. Add Deep Link to Event Data Bundle

**Location:** `InAppController.kt` line 271 (after `data.putString(Constants.KEY_C2A, callToAction)`)

Add logic to extract and include the deep link URL:

```kotlin
// Existing code at line 271
data.putString(Constants.KEY_C2A, callToAction)

// NEW: Extract and add deep link
val deepLink = extractDeepLink(inAppNotification, action)
if (!deepLink.isNullOrEmpty()) {
    data.putString(Constants.DEEP_LINK_KEY, deepLink)
}
```

This ensures `wzrk_dl` is included in the Bundle passed to `pushInAppNotificationStateEvent()` at line 275, which will automatically include it in the event data through the existing `getWzrkFields()` mechanism.

#### 2. Add Deep Link Extraction Method

**Location:** `InAppController.kt` (after line 1077, near end of class)

Add a private helper method that extracts deep links with proper priority:

```kotlin
/**
 * Extracts the deep link URL from an InApp notification action.
 *
 * Priority order:
 * 1. Button-level deep link (CTInAppAction.actionUrl) - for multi-CTA scenarios
 * 2. Template-level deep link (CTInAppNotification.customInAppUrl) - for image-only and HTML templates
 *
 * @param inAppNotification The InApp notification being acted upon
 * @param action The action being triggered
 * @return The deep link URL, or null if no valid deep link exists
 */
private fun extractDeepLink(
    inAppNotification: CTInAppNotification,
    action: CTInAppAction
): String? {
    // Priority 1: Button-level deep link (for CTA buttons with OPEN_URL action)
    if (action.type == InAppActionType.OPEN_URL) {
        val actionUrl = action.actionUrl
        if (!actionUrl.isNullOrEmpty()) {
            return actionUrl
        }
    }

    // Priority 2: Template-level deep link (for image-only, HTML templates)
    val customInAppUrl = inAppNotification.customInAppUrl
    if (!customInAppUrl.isNullOrEmpty() && shouldUseTemplateUrl(inAppNotification.inAppType)) {
        return customInAppUrl
    }

    return null
}
```

**Design Decision:** Button-level URLs take precedence over template-level URLs because in multi-CTA scenarios, each button can have its own deep link and we must capture the specific clicked button's URL.

#### 3. Add Template Type Detection Method

**Location:** `InAppController.kt` (after the `extractDeepLink()` method)

Add a method to determine which templates should use template-level URLs:

```kotlin
/**
 * Determines if the template-level URL should be used for wzrk_dl.
 *
 * Template-level URLs are used for:
 * - Image-only templates (Cover, Interstitial, Half-Interstitial)
 * - HTML templates (Cover, Interstitial, Half-Interstitial, Header, Footer)
 *
 * Template-level URLs are NOT used for:
 * - Native templates with CTA buttons (button URLs take precedence)
 * - Ratings, Lead Generation (no navigation URLs)
 * - Custom Code templates (handled separately)
 */
private fun shouldUseTemplateUrl(inAppType: CTInAppType?): Boolean {
    return when (inAppType) {
        // Image-only templates - use template URL
        CTInAppTypeCoverImageOnly,
        CTInAppTypeInterstitialImageOnly,
        CTInAppTypeHalfInterstitialImageOnly,
        // HTML templates - use template URL
        CTInAppTypeCoverHTML,
        CTInAppTypeInterstitialHTML,
        CTInAppTypeHalfInterstitialHTML,
        CTInAppTypeHeaderHTML,
        CTInAppTypeFooterHTML -> true

        // All other templates - do NOT use template URL
        else -> false
    }
}
```

**Design Decision:** Only image-only and HTML templates use template-level URLs. Native templates with CTA buttons rely on button-level URLs for proper attribution.

### Template Behavior Matrix

| Template Type | wzrk_c2a | wzrk_dl Source |
|---------------|----------|----------------|
| Content with Image (Cover, Interstitial, Half, Alert) | Button Text | `action.actionUrl` |
| Image only | (empty) | `customInAppUrl` |
| Custom HTML | Button Text | `customInAppUrl` OR `action.actionUrl` |
| Header/Footer (native) | Button Text | `action.actionUrl` |
| Ratings | Button Text | (none) |
| Lead Generation | Button Text | (none) |
| Custom Code | (empty) | (none) |
| App Functions | (empty) | (none) |

### Why This Approach Works

1. **Leverages Existing Architecture:** The `getWzrkFields()` method in `CTJsonConverter.java` already extracts ALL fields prefixed with `wzrk_` from the Bundle, so adding `wzrk_dl` to the Bundle automatically includes it in the event.

2. **Minimal Code Changes:** Only one file needs modification, reducing risk and complexity.

3. **Backward Compatible:** The null check ensures we only add `wzrk_dl` when a valid deep link exists. Events without deep links remain unchanged.

4. **Consistent with Push:** Uses the same constant (`Constants.DEEP_LINK_KEY = "wzrk_dl"`) and follows the same pattern as push notification attribution.

5. **Handles All Template Types:** The `shouldUseTemplateUrl()` method correctly routes template-level URLs only for appropriate template types.

6. **Supports Multi-CTA:** Button-level URLs take priority, ensuring correct attribution in multi-button scenarios.

## Critical Files

### Files to Modify

1. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/inapp/InAppController.kt`**
   - Line 271: Add deep link extraction and Bundle insertion
   - Line ~1077: Add `extractDeepLink()` helper method
   - Line ~1077: Add `shouldUseTemplateUrl()` helper method

### Files to Reference (No Changes Needed)

2. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/inapp/CTInAppAction.kt`**
   - Already contains `actionUrl: String?` property for button-level deep links

3. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/inapp/CTInAppNotification.kt`**
   - Already contains `customInAppUrl: String?` for template-level deep links
   - Already contains `inAppType: CTInAppType` for template type detection

4. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/inapp/CTInAppType.kt`**
   - Enum defining all template types (used in `shouldUseTemplateUrl()` logic)

5. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/Constants.java`**
   - Line 120: `DEEP_LINK_KEY = "wzrk_dl"` (already defined)
   - Line 168: `KEY_C2A = "wzrk_c2a"` (already defined)

6. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/AnalyticsManager.java`**
   - Line 334: `pushInAppNotificationStateEvent()` already processes wzrk_* fields via `getWzrkFields()`

7. **`/home/user/clevertap-android-sdk/clevertap-core/src/main/java/com/clevertap/android/sdk/utils/CTJsonConverter.java`**
   - Line 190: `getWzrkFields()` already extracts all wzrk_* prefixed fields automatically

## Edge Cases Handled

1. **No Deep Link:** If both `actionUrl` and `customInAppUrl` are null/empty, `extractDeepLink()` returns null and `wzrk_dl` is not added to the Bundle → event remains unchanged ✓

2. **CLOSE Action:** `action.type != OPEN_URL` → `extractDeepLink()` returns null → no wzrk_dl ✓

3. **KEY_VALUES Action:** No actionUrl → `extractDeepLink()` returns null → no wzrk_dl ✓

4. **Multi-CTA Templates:** Button-level `actionUrl` takes precedence over template-level URL → correct per-button attribution ✓

5. **Empty String URLs:** The `isNullOrEmpty()` check filters out empty strings and whitespace → no empty wzrk_dl values ✓

6. **Custom Templates:** Custom template actions already use the same `inAppNotificationActionTriggered()` flow, so our logic automatically handles them ✓

## Verification Plan

### Manual Testing Steps

1. **Test Button-level Deep Links:**
   - Create InApp campaign with CTA button containing a deep link
   - Click the button
   - Verify "Notification Clicked" event contains `wzrk_dl` with button's URL
   - Verify `wzrk_c2a` contains button text

2. **Test Template-level Deep Links:**
   - Create image-only InApp campaign with template-level URL
   - Click the image
   - Verify event contains `wzrk_dl` with template URL
   - Verify `wzrk_c2a` is empty

3. **Test Multi-CTA Templates:**
   - Create InApp with 2+ buttons, each with different deep links
   - Click each button separately
   - Verify each event has the correct `wzrk_dl` for the clicked button

4. **Test No Deep Link:**
   - Create InApp with CLOSE action
   - Click close button
   - Verify event does NOT contain `wzrk_dl` property

5. **Test HTML Templates:**
   - Create HTML InApp with template-level URL
   - Add button with button-level URL
   - Click button → verify `wzrk_dl` = button URL (priority)
   - Click elsewhere → verify `wzrk_dl` = template URL

### Event Validation

Use Charles Proxy or similar to intercept network requests and verify:
- Event name: `"Notification Clicked"`
- Event data contains: `{"wzrk_c2a": "...", "wzrk_dl": "...", "wzrk_id": "..."}`
- wzrk_dl value matches the expected deep link URL
- Format matches Push notification click events

### Unit Testing

Create test file: `InAppControllerDeepLinkTest.kt`

**Test scenarios:**
1. Button-level deep link with OPEN_URL action → wzrk_dl populated
2. Template-level deep link for image-only template → wzrk_dl populated
3. Template-level deep link for HTML template → wzrk_dl populated
4. Native template with CTA → template URL ignored, button URL used
5. CLOSE action → no wzrk_dl
6. KEY_VALUES action → no wzrk_dl
7. Custom Code template → no wzrk_dl
8. Null actionUrl and customInAppUrl → no wzrk_dl
9. Empty string URLs → no wzrk_dl
10. Multi-CTA with different URLs → correct URL per button

## Summary

This is a focused, low-risk implementation that adds deep link attribution to InApp click events by:

1. Extracting deep links from existing data structures (`CTInAppAction.actionUrl` and `CTInAppNotification.customInAppUrl`)
2. Adding them to the event data Bundle using the existing `Constants.DEEP_LINK_KEY` constant
3. Leveraging the existing event processing pipeline that automatically includes all `wzrk_*` fields

**Total changes:** ~40 lines of code in a single file
**Risk level:** Low - additive change with null safety and backward compatibility
**Testing complexity:** Medium - requires validation across multiple template types
