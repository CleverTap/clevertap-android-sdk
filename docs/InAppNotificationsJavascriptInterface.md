## Custom HTML In-App Notifications Javascript Interface

CleverTap Android SDK provides a Javascript interface that can be used from custom HTML InApps' templates. Before using any of the methods always check if the interface is available. It will be defined only when the InApp is running on an Android device. Refer to the CleverTap iOS SDK for using a similar interface for InApps running on iOS devices.

For methods with json parameters, the json must be passed as a string with `JSON.stringify`.

### Checking if the Interface is Available

```javascript
if (window.CleverTap) {    
    // Interface is available
    // You can now call methods on the CleverTap object
}
```

### Methods

### `pushEvent(eventName)`

Records a custom event with the given name.

### `pushEvent(eventName, eventActions)`

Records a custom event with the given name and associated properties.

**Parameters:**

*   `eventName` (String): The name of the event to record.
*   `eventActions` (String): A JSON string containing key-value pairs of event properties.

**Example Usage:**

```javascript
if (window.CleverTap) {
    const eventProperties = JSON.stringify({"productName": "T-Shirt", "color": "Blue", "size": "M"});
    window.CleverTap.pushEvent("Added to Cart", eventProperties);
}
```

#### `pushChargedEvent(chargeDetails, items)`

Records a "Charged" system event.

**Parameters:**

*   `chargeDetails` (String): A JSON string containing details about the charge (e.g., amount, payment mode).
    Example: `JSON.stringify({"amount": 300, "paymentMode": "Credit Card", "transactionId": "12345"})`
*   `items` (String): A JSON string array representing the items included in the charge. Each item is a JSON object.
    Example: `JSON.stringify([{"category": "Electronics", "name": "Smartphone", "price": 290}, {"category": "Accessories", "name": "Phone Case", "price": 10}])`

**Example Usage:**

```javascript
if (window.CleverTap) {
    const chargeDetails = JSON.stringify({
        "amount": 300,
        "paymentMode": "Credit Card",
        "transactionId": "12345"
    });
    const items = JSON.stringify([
        {"category": "Electronics", "name": "Smartphone", "price": 290},
        {"category": "Accessories", "name": "Phone Case", "price": 10}
    ]);
    window.CleverTap.pushChargedEvent(chargeDetails, items);
}
```

### `pushProfile(profile)`

Updates the current user profile with the provided properties.

**Parameters:**

*   `profile` (String): A JSON string containing the user profile properties to update.

**Example Usage:**

```javascript
if (window.CleverTap) {
    const userProfile = JSON.stringify({
        "Name": "John Doe",
        "Email": "john.doe@example.com",
        "Age": "30",
        "customData": "customValue"
    });
    window.CleverTap.pushProfile(userProfile);
}
```

### `onUserLogin(profile)`

Creates a separate and distinct user profile identified by one or more of `Identity`,`Email`, `FBID` or `GPID` and populated with the key-values included in the profile object parameter.

See `CleverTapAPI.onUserLogin.`

**Parameters:**

*   `profile` (String): A JSON string containing the user profile properties to set. This should include at least one unique identifier like `Identity`, `Email`, or a custom unique identifier.
    Example: `JSON.stringify({"Name": "John Doe", "Email": "john.doe@example.com", "Identity": "userXYZ", "customerSegment": "Premium"})`

**Example Usage:**

```javascript
if (window.CleverTap) {
    const loginProfile = JSON.stringify({
        "Name": "Logged In User",
        "Email": "loggedin@example.com",
        "Identity": "uniqueIdForUser"
    });
    window.CleverTap.onUserLogin(loginProfile);
}
```

#### `incrementValue(key, value)`

Increments the value of a numeric user profile property by the given amount. The value should be a positive number.

#### `decrementValue(key, value)`

Decrements the value of a numeric user profile property by the given amount. The value should be a positive number.

### `removeValueForKey(key)`

Removes a specific key and its associated value from the user profile.

### `setMultiValueForKey(key, values)`

Set a collection of unique values as a multi-value user profile property, any existing value will be overwritten. Max 100 values, on reaching 100 cap, oldest value(s) will be removed. Values must be strings and are limited to 512 characters. The values should be provided as a JSON string array.

See `CleverTapAPI.setMultiValuesForKey`

**Parameters:**

*   `key` (String): The key of the user profile property.
*   `values` (String): A JSON string representation of an array of string values to set for the key. E.g., `JSON.stringify(["valueA", "valueB"])`. Any existing values for this key will be replaced.

**Example Usage:**

```javascript
if (window.CleverTap) {
    const newHobbies = JSON.stringify(["coding", "gaming"]);
    window.CleverTap.setMultiValueForKey("hobbies", newHobbies);
}
```

#### `addMultiValueForKey(key, value)`

Add a unique value to a multi-value user profile property. If the property does not exist it will be created. Max 100 values, on reaching 100 cap, oldest value(s) will be removed. Values must be strings and are limited to 512 characters.

See `CleverTapAPI.addMultiValueForKey`

#### `addMultiValuesForKey(key, values)`

Adds multiple string values to a multi-value user profile property. Max 100 values, on reaching 100 cap, oldest value(s) will be removed. Values must be strings and are limited to 512 characters. The values should be provided as a JSON string representing an array. If the key does not exist, it will be created.

See `CleverTapAPI.addMultiValuesForKey`

**Parameters:**

*   `key` (String): The key of the user profile property.
*   `values` (String): A JSON string representation of an array of string values to add. E.g., `JSON.stringify(["value1", "value2"])`.

**Example Usage:**

```javascript
if (window.CleverTap) {
    // Add multiple favorite colors
    const colors = JSON.stringify(["blue", "green", "red"]);
    window.CleverTap.addMultiValuesForKey("favoriteColors", colors);
}
```

### `removeMultiValueForKey(key, value)`

Removes a specific value from a multi-value user profile property.

See `CleverTapAPI.removeMultiValueForKey`

**Parameters:**

*   `key` (String): The key of the multi-value user profile property.
*   `value` (String): The value to remove from the key.

**Example Usage:**

```javascript
if (window.CleverTap) {
    window.CleverTap.removeMultiValueForKey("hobbies", "reading");
}
```

### `removeMultiValuesForKey(key, values)`

Remove a collection of unique values from a multi-value user profile property. The values should be provided as a JSON string array.

See `CleverTapAPI.removeMultiValuesForKey`

**Parameters:**

*   `key` (String): The key of the multi-value user profile property.
*   `values` (String): A JSON string representation of an array of string values to remove. E.g., `JSON.stringify(["value1", "value2"])`.

**Example Usage:**

```javascript
if (window.CleverTap) {
    const colorsToRemove = JSON.stringify(["green", "red"]);
    window.CleverTap.removeMultiValuesForKey("favoriteColors", colorsToRemove);
}
```

#### `dismissInAppNotification()`

Dismisses the currently displayed InApp notification.

#### `triggerInAppAction(actionJson, callToAction, buttonId)`

Triggers the specified action, raises a "Notification Clicked" for the current InApp and dismisses the current InApp.

**Parameters:**

*   `actionJson` (String): A JSON string defining the action to be performed. The structure of this JSON depends on the `type` of action. The following action are supported:

    * **Close**: Closes the in-app notification.
        ```javascript
        {
            "type": "close"
        }
        ```
    * **Open URL**: Opens a specified URL. The SDK handles opening this URL, which could be a deep link or a web URL.
        ```javascript
        {
            "type": "open-url",
            "android": "URL to be opened"
        }
        ```
    * **Key-Value pairs** : Passes the `string` values specified in the `"kv"` object to the InAppNotificationButtonListener registered in the application. See `CleverTapAPI.setInAppNotificationButtonListener`.
        ```javascript
        {
            "type": "kv",
            "kv": {
                "key1": "value1",
                "key2": "value2"
            }
        }
        ```
    * **Custom code**: Triggers a custom code template with the specified argument values in the `vars` object. Nested arguments can only be passed with the "dot" notation. See the Custom Code Templates documentation for more information on using templates.
        ```javascript
        {
            "type": "custom-code",
            "templateName": "The name of the custom template",
            "vars" : {
                "stringArg": "value",
                "boolArg": true,
                "numberArg": 1,
                "fileArg": "fileUrl",
                "actionArg": {
                    "actions": {
                        "templateName": "The name of the action",
                        "vars": {
                            "stringArg": "value"
                        }
                    }
                }

            }
        }
        ```

*   `callToAction` (String): The text or identifier of the button/element that was clicked to trigger this action (e.g., "Buy Now", "Learn More"). This value will be available in the "Notification Clicked" event data.

*   `buttonId` (String): An optional identifier for the button (can be null or empty). This can be used to identify the button that will be associated with the click. This value is included in the "Notification Clicked" event data.

**Example Usage:**

```javascript
if (window.CleverTap) {
    const action = JSON.stringify({
        "type": "open-url",
        "android": "https://clevertap.com"
    });
    window.CleverTap.triggerInAppAction(action, "CleverTap", "button1");
}
```

#### `promptPushPermission(shouldShowFallbackSettings)`

Dismisses the current InApp and prompts the user for push notification permission on devices running Android 13+. If the user has already denied permission, this method can optionally open the app's notification settings page as a fallback.

### `getSdkVersion()`

Returns the version code number of the CleverTap Android SDK.
