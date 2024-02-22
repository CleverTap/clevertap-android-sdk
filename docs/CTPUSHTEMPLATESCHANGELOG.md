## CleverTap Push Templates SDK CHANGE LOG

### Version 1.2.3 (February 21, 2024)

#### New features

* Supports Android 14, made it compliant with Android 14 requirements. Details [here](https://developer.android.com/about/versions/14/summary)
* Upgrades AGP to 8.2.2 for building the SDK and adds related consumer proguard rules

#### Bug Fixes
* Fixes [Input Box](https://developer.clevertap.com/docs/push-templates-android#input-box-template) push template. 

### Version 1.2.2 (January 15, 2024)

* Minor changes and improvements

### Version 1.2.1 (December 22, 2023)

#### Bug Fixes

* Updates `PendingIntent.FLAG_MUTABLE` to `PendingIntent.FLAG_IMMUTABLE` to prevent vulnerabilities in pre-launch
  report on PlayStore

### Version 1.2.0 (October 27, 2023)

#### New features

* Adds support for developer defined default notification channel for PushTemplates. Please refer to
  the [EXAMPLES.md](EXAMPLES.md#push-notifications) file to read more on how to setup default
  channel in your app.
#### Bug Fixes
* Fixes a bug in the Rating PushTemplate where clicking of a star resulted in no action.
* Fixes [#488](https://github.com/CleverTap/clevertap-android-sdk/issues/488) - a bug related to the image sequence in Manual Carousel PushTemplate.

### Version 1.1.0 (June 28, 2023)

* Supports CleverTap Android SDK v5.1.0.
* RenderMax Push SDK functionality is now supported directly within the CleverTap Core SDK starting
  from core v5.1.0.

### Version 1.0.9 (May 5, 2023)

#### Bug Fixes

* Fixes a bug where Rating Submitted event was not being raised by the Rating Template on Android 12
  and above.

### Version 1.0.8 (March 8, 2023)

* Supports CleverTap Android SDK v4.7.5. CleverTap Push Templates SDK `v1.0.8`
  requires [CleverTap Android SDK v4.7.5](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md)
  to work properly.

### Version 1.0.7 (December 5, 2022)

* UI bug fixes for Non-linear Product Catalogue template.
* Supports CleverTap Android SDK v4.7.2.

### Version 1.0.6 (November 1, 2022)
* Targets Android 13
* Supports CleverTap Android SDK v4.7.0
* Minimum Android SDK version bumped to API 19 (Android 4.4).

### Version 1.0.5.1 (March 15, 2023)
* Supports CleverTap Android SDK v4.6.7. CleverTap Push Templates SDK `v1.0.5.1` requires [CleverTap Android SDK v4.6.7](https://github.com/CleverTap/clevertap-android-sdk/blob/master_android12/docs/CTCORECHANGELOG.md) to work properly.
* **Note:** This release is being done for Android 12 targeted users, satisfying below points.
  * Targeting Android 12 and
  * Using RenderMax and/or using Push Templates

### Version 1.0.5 (September 13, 2022)
* fixes a bug on android 12 where push template notification header was not displaying subtitle text.

### Version 1.0.4 (August 4, 2022)
* Supports CleverTap Android SDK v4.6.0

### Version 1.0.3 (July 22, 2022)
* Fixes a bug for notification CTA deeplink for Android 12 and above devices - On clicking notification CTA, deeplink launches third party app instead of X app even though X app is capable of handling deeplink. For example, if X app is capable of handling https://google.com(sample link) but deeplink launches browser instead of X app.
* Fixes push impression not raised bug for Timer template

### Version 1.0.2 (April 26, 2022)
* UI bug fixes for Push Notification metadata
* Timer template fixes for `title`, `title_alt` along with `pt_big_img`, `pt_big_img_alt`
* Small content view text cropping fixes for Android versions less than 23
* Darker arrows for navigation in ManualCarousel & Filmstrip Carousel template
* Fixes five icon template dismissible on multiple onClickListeners
* Fixes UI issue when only 3 icons were configured in Five icon template
* Fix validation for `deepLink(optional)` key in ZeroBezel template

### Version 1.0.1 (March 2, 2022)
* Improved image handling for Basic, AutoCarousel, ManualCarousel templates.
* Allows either or both  `pt_timer_threshold` and `pt_timer_end` for Timer template.

### Version 1.0.0 (December 20, 2021)
* Stable release! 🎉
* Supports Xiaomi, Huawei notification messages out of the box
* Supports Pull Notifications out of the box
* Supports Android 12
* Supports CleverTap Android SDK v4.4.0

### Version 0.0.8 (April 27, 2021)
* Making deep-links truly optional. Fixes issues #43 & #46
* Fixes issue #45 where `break` statement was missing
* Adds support for Android 11 and [CleverTap Android SDK v4.1.0](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md)
* Fixes a rare NPE when silent notification channel was being created
* Removes support for JCenter

### Version 0.0.7 (February 4, 2021)
* Fixes deeplinks in Manual Carousel Template

### Version 0.0.6 (January 20, 2021)
* Supports CleverTap Android SDK v4.0.2
* Removes support for Video Push Notifications.

Video Push Notifications required an `implementation` dependency of Exoplayer by the app.
Based on feedback, not all developers were comfortable with adding the Exoplayer dependency.
This version removes Video Push Notifications and we will re-introduce them as a separate module soon.
Video Push notifications can still be used with `v0.0.5` of the Push Templates SDK.

### Version 0.0.5 (October 29, 2020)
* Added support for [CleverTap Android SDK v4.0.0](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTV4CHANGES.md)
* Added `filmstrip` type to Manual Carousel Template. See [README](https://github.com/CleverTap/PushTemplates/blob/master/README.md) for details.
* ExoPlayer is now an `implementation` dependency for PushTemplates SDK

### Version 0.0.4 (August 19, 2020)
* Removed `Rated` event.
* The library will now never raise extra events apart from CleverTap System Events and events given to the library by KV pairs.
* CTAs on Video, Rating and 5 CTA Template can now be tracked in the event Notification Clicked with in the property `wzrk_c2a`.
* Added support for collapse key.
* Performance enhancements

### Version 0.0.3 (August 3, 2020)
* Added 5 more templates - Video, Manual Carousel, Timer, Zero Bezel & Input Templates
* Added support for multiple instances of CleverTap
* Performance enhancements

### Version 0.0.2 (May 21, 2020)
* Added Duplication check
* Added support to enable/disable logs
* Performance enhancements

### Version 0.0.1 (May 12, 2020)
* First release :tada:
* Supports 5 templates - Basic, Auto Carousel, Rating, Product Catalog and Five Icons
* Compatible with CleverTap Android SDK v3.8.0