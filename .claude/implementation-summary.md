# InApp Deep Link Attribution Implementation Summary

## Changes Made

### Modified File: `InAppController.kt`

**Location:** `/clevertap-core/src/main/java/com/clevertap/android/sdk/inapp/InAppController.kt`

### 1. Added Deep Link Extraction to Click Event (Lines 273-277)

Added logic in `inAppNotificationActionTriggered()` method to extract and include the deep link URL (`wzrk_dl`) in the event data bundle, right after the call-to-action (`wzrk_c2a`) is set.

```kotlin
// Extract and add deep link for attribution
val deepLink = extractDeepLink(inAppNotification, action)
if (!deepLink.isNullOrEmpty()) {
    data.putString(Constants.DEEP_LINK_KEY, deepLink)
}
```

**Impact:** This ensures that whenever an InApp notification is clicked, the deep link URL is automatically included in the "Notification Clicked" event sent to the backend.

### 2. Added Deep Link Extraction Helper Method (Lines 1096-1115)

Created `extractDeepLink()` private method that intelligently extracts the deep link URL with proper priority:

**Priority 1:** Button-level deep link (`CTInAppAction.actionUrl`)
- Used for CTA button clicks with OPEN_URL action
- Critical for multi-CTA templates where each button has its own URL

**Priority 2:** Template-level deep link (`CTInAppNotification.customInAppUrl`)
- Used for image-only and HTML templates without explicit button CTAs
- Only used if `shouldUseTemplateUrl()` returns true

**Returns:** The appropriate deep link URL, or null if no valid deep link exists

### 3. Added Template Type Detection Method (Lines 1132-1148)

Created `shouldUseTemplateUrl()` private method that determines which templates should use template-level URLs.

**Returns `true` for:**
- Image-only templates: `CTInAppTypeCoverImageOnly`, `CTInAppTypeInterstitialImageOnly`, `CTInAppTypeHalfInterstitialImageOnly`
- HTML templates: `CTInAppTypeCoverHTML`, `CTInAppTypeInterstitialHTML`, `CTInAppTypeHalfInterstitialHTML`, `CTInAppTypeHeaderHTML`, `CTInAppTypeFooterHTML`

**Returns `false` for:**
- Native templates with CTA buttons (button URLs take precedence)
- Ratings, Lead Generation (no navigation URLs)
- Custom Code templates (handled separately)
- All other template types

## How It Works

### Event Flow with Deep Link Attribution

```
User Clicks InApp
    ↓
CTInAppBaseFragment.handleButtonClickAtIndex(index)
    ↓
InAppController.inAppNotificationActionTriggered()
    ├─ Create data Bundle
    ├─ Put wzrk_id = campaignId
    ├─ Put wzrk_c2a = button.text
    ├─ ✨ NEW: Extract deep link via extractDeepLink()
    ├─ ✨ NEW: Put wzrk_dl = deep link URL (if exists)
    └─ Call analyticsManager.pushInAppNotificationStateEvent()
        ↓
AnalyticsManager.pushInAppNotificationStateEvent()
    ├─ Extract all wzrk_* fields via getWzrkFields()
    ├─ Merge customData (contains wzrk_c2a and wzrk_dl) ✨
    └─ Queue event with full attribution data
        ↓
Event Sent to Backend with Complete Attribution
{
  "evtName": "Notification Clicked",
  "evtData": {
    "wzrk_id": "campaign_123",
    "wzrk_c2a": "Click Here",
    "wzrk_dl": "https://example.com/deep-link" ✨
  }
}
```

### Template-Specific Behavior

| Template Type | wzrk_c2a | wzrk_dl Source | Implementation |
|---------------|----------|----------------|----------------|
| **Content with Image** | Button Text | `action.actionUrl` | Button action has OPEN_URL type |
| **Image only** | (empty) | `customInAppUrl` | No button, template URL used |
| **Custom HTML** | Button Text | `customInAppUrl` or `action.actionUrl` | Priority: button URL > template URL |
| **Header/Footer** | Button Text | `action.actionUrl` | Button action has OPEN_URL type |
| **Ratings** | Button Text | (none) | No OPEN_URL action |
| **Lead Generation** | Button Text | (none) | No OPEN_URL action |
| **Custom Code** | (empty) | (none) | Returns null |

## Key Design Decisions

### 1. Priority Order
Button-level URLs take precedence over template-level URLs because in multi-CTA scenarios, each button can have its own deep link and we must capture the specific clicked button's URL.

### 2. Null Safety
Only add `wzrk_dl` when a valid deep link exists. This maintains backward compatibility and doesn't pollute events with empty values.

### 3. Leveraging Existing Architecture
The `getWzrkFields()` method in `CTJsonConverter.java` already extracts ALL fields prefixed with `wzrk_` from the Bundle, so adding `wzrk_dl` to the Bundle automatically includes it in the event.

### 4. No URL Validation
URLs are passed through as-is without validation. URL validation happens at the navigation layer (in `InAppActionHandler.openUrl()`), not at the analytics layer.

### 5. Template Type Specificity
Only image-only and HTML templates use template-level URLs. Native templates with CTA buttons rely on button-level URLs for proper attribution.

## Edge Cases Handled

✅ **No Deep Link:** If both `actionUrl` and `customInAppUrl` are null/empty → no `wzrk_dl` added

✅ **CLOSE Action:** `action.type != OPEN_URL` → `extractDeepLink()` returns null → no `wzrk_dl`

✅ **KEY_VALUES Action:** No actionUrl → `extractDeepLink()` returns null → no `wzrk_dl`

✅ **Multi-CTA Templates:** Button-level `actionUrl` takes precedence → correct per-button attribution

✅ **Empty String URLs:** `isNullOrEmpty()` check filters out empty strings → no empty `wzrk_dl` values

✅ **Custom Templates:** Use same `inAppNotificationActionTriggered()` flow → handled automatically

## Lines of Code Changed

- **Total lines added:** ~70 lines (including documentation)
- **Files modified:** 1 (`InAppController.kt`)
- **Files created:** 2 (`.claude/inapp-deep-link-attribution-plan.md`, `.claude/implementation-summary.md`)

## Backward Compatibility

✅ **Fully backward compatible:** The null check ensures we only add `wzrk_dl` when a valid deep link exists

✅ **Existing events unchanged:** Events without deep links remain exactly the same

✅ **No breaking changes:** No modifications to public APIs or existing method signatures

✅ **Additive only:** New functionality added without changing existing behavior

## Testing Recommendations

### Manual Testing

1. **Button-level deep links:** Create InApp with CTA button + deep link → verify `wzrk_dl` in event
2. **Template-level deep links:** Create image-only InApp + URL → verify `wzrk_dl` in event
3. **Multi-CTA templates:** Create InApp with 2+ buttons → verify correct `wzrk_dl` per button
4. **No deep link:** Create InApp with CLOSE action → verify NO `wzrk_dl` in event
5. **HTML templates:** Test both button URL and template URL scenarios

### Event Validation

Use Charles Proxy or similar to intercept network requests and verify:
- Event name: `"Notification Clicked"`
- Event data contains: `{"wzrk_c2a": "...", "wzrk_dl": "...", "wzrk_id": "..."}`
- `wzrk_dl` value matches the expected deep link URL
- Format consistent with Push notification click events

### Unit Testing

Create test file: `InAppControllerDeepLinkTest.kt`

Test scenarios:
1. Button-level deep link extraction
2. Template-level deep link extraction
3. Priority: button URL over template URL
4. No deep link scenarios (CLOSE, KEY_VALUES, etc.)
5. Edge cases (null, empty, whitespace)
6. All template types

## Implementation Date

February 12, 2026

## PRD Reference

Atlassian: https://wizrocket.atlassian.net/wiki/x/CgDYeQE

## Status

✅ **Code Implementation:** Complete
⏳ **Unit Tests:** Pending
⏳ **Integration Tests:** Pending
⏳ **Manual Verification:** Pending
