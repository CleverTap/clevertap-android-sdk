---
name: verify-on-device
description: Check a UI or push change on an emulator or phone with the sample app and adb. Use when a change affects in-apps, App Inbox, push layouts, screen sizes, permissions, or when asked to "test on device" or "verify on emulator".
---

# Verify on a device

Unit tests do not show layouts. Measure on a device instead of guessing from XML.

## Install and start

```
./gradlew :sample:installLocalDebug
adb shell am start -n com.clevertap.demo/.HomeScreenActivity
```

The sample needs account values in `sample/src/main/AndroidManifest.xml`. Never commit them.

## Useful checks

| Need | Command |
|---|---|
| See what is on screen (positions, sizes, text) | `adb shell uiautomator dump` then pull the file |
| Simulate a bigger or smaller screen | `adb shell wm density <number>`, and `adb shell wm density reset` after |
| Show the notification permission prompt | `adb shell pm revoke com.clevertap.demo android.permission.POST_NOTIFICATIONS` |
| Look for crashes | `adb logcat -d` and search for `FATAL EXCEPTION` |
| Screenshot | `adb exec-out screencap -p > shot.png` |

## Steps

1. Run the app on the base branch and note what you see.
2. Run it on your branch with the same steps. Compare sizes and positions from `uiautomator`, not by eye.
3. Change the screen size or rotate while the screen is open. Many bugs only show then.
4. Check `logcat` for `FATAL EXCEPTION` after each step.
5. For resource problems, build a release variant with `minifyEnabled true` and `shrinkResources true`.
   An unreferenced resource that is looked up by name will be missing there.

## Report

Say the device or emulator, the Android version, the screen size, the steps, and what you saw. Say what you
did not test (other versions, tablets, dark mode).
