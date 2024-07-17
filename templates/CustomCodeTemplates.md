# Custom Code Templates

Android SDK 7.0.0 and above offers support for a custom presentation of in-app messages. This allows for utilizing the in-app notifications functionality with custom configuration and presentation logic. There are two types of templates that can be defined through the SDK: Templates and Functions. Templates can contain action arguments while Functions cannot and Functions can be used as actions while Templates cannot. Functions can be either 'visual' or not. 'Visual' functions can contain UI logic and will be part of the [In-App queue](#in-App-queue), while non-visual functions will be triggered directly when invoked and should not contain UI logic.

## Creating templates and functions
All templates consist of a name, arguments and a presenter. They are all specified when creating a template through the builder. Name and presenter are required and names must be unique across the application. The template builders validate the correctness of the template definitions and will throw `CustomTemplateException` when an invalid template is being created. Those exceptions should generally not be caught and template definitions must be valid in order to be triggered correctly.

### Arguments
Arguments are key-value pairs that represent the configuration of the custom code templates. The supported argument types are:
- Primitives - `Boolean`, `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `String`. They must have a default value which would be used if no other value is configured for the notification.
- Map - A `Map` of supported primitives with keys being the argument names.
- File - a file argument that will be downloaded when the template is triggered
- Action - an action argument that could be a function template or a built-in action like ‘close’ or ‘open url’

#### Hierarchical arguments
You can group arguments together by either using a map argument or indicating the group in the argument's name by using a '.' symbol. Both definitions are treated the same. File and Action type arguments can only be added to a group by specifying it in the name of the argument.

The following code snippets define identical arguments:
```kotlin
mapArgument(
   name = "map",
   value = mapOf (
       "a" to 5,
       "b" to 6
   )
)
```
and
```kotlin
builder.intArgument("map.a", 5)
builder.intArgument("map.b", 6)
```

### Example
#### Java
```java
new CustomTemplate.TemplateBuilder()
               .name("template")
               .presenter(presenter)
               .stringArgument("string", "Default Text")
               .fileArgument("file")
               .intArgument("int", 0)
               .build();
```
#### Kotlin
```kotlin
template {
   name("template")
   presenter(presenter)
   stringArgument("string", "Default Text")
   fileArgument("file")
   intArgument("int", 0)
}
```

## Registering custom templates

Templates must be registered before the CleverTapAPI instance that would use them is created. A common place for this initialization is in `Application.onCreate()`. If your application uses multiple `CleverTapAPI` instances, use the `CleverTapInstanceConfig` to differentiate which templates should be registered to which `CleverTapAPI` instance(s).

Custom templates are registered through `CleverTapAPI.registerCustomInAppTemplates` which accepts a `TemplatesProducer` that contains the definitions of the templates.

### Java
```java
CleverTapAPI.registerCustomInAppTemplates(ctConfig ->
   CustomTemplatesExtKt.templatesSet( // or create a Set<CustomTemplate> and add your templates
       new CustomTemplate.TemplateBuilder()
           .name("template")
           .presenter(templatePresenter)
           .stringArgument("string", "Text")
           .build(),
       new CustomTemplate.FunctionBuilder(true)
           .name("function")
           .presenter(functionPresenter)
           .intArgument("int", 0)
           .build()
       ));
```
### Kotlin
```kotlin
CleverTapAPI.registerCustomInAppTemplates {
   setOf(
       template {
           name("template")
           presenter(templatePresenter)
           stringArgument("string", "Text")
       },
       function(isVisual = true) {
           name("function")
           presenter(functionPresenter)
           intArgument("int", 0)
       }
   )
}
```

## Synching in-app templates to the dashboard

In order for the templates to be usable in campaigns they must be synched with the dashboard. When all templates and functions are defined and registered in the SDK, they can be synched by `CleverTapAPI.syncRegisteredInAppTemplates`. The synching can only be done in debug builds and with a SDK user that is marked as 'test user'. We recommend only running this function while developing the templates and delete the invocation in release builds.

## Presenting templates

When a custom template is triggered, its presenter will be invoked. Presenters can either extend `TemplatePresenter` or `FunctionPresenter` depending on the type of the template. Both of them must implement the `onPresent()` method in which to use the template invocation to present their custom UI logic. `TemplatePresenter` should also implement `onClose` which will be invoked when a template should be closed (which could occur when an action of type 'close' is triggered). Use this method to remove the UI associated with the template and call `TemplateContext.setDismissed`.

All presenter methods provide a `CustomTemplateContext`. It can be used to:
- Obtain argument values by using the appropriate `get*(String name)` methods.
- Trigger actions by their name through `CustomTemplateContext.triggerActionArgument`.
- Set the state of the template invocation. `CustomTemplateContext.setPresented` and `CustomTemplateContext.setDismissed` notify the SDK of the state of the current template invocation. The presented state is when an in-app is displayed to the user and the dismissed state is when the in-app is no longer being displayed.

#### Template presenter
```kotlin
MyTemplatePresenter: TemplatePresenter {


   override fun onClose(context: TemplateContext) {
       // be sure to keep the context as long as the template UI is being displayed
       // so that context.setDismissed() can be called when the UI is closed.
       showUI(context)
       context.setPresented()
   }


   override fun onPresent(context: TemplateContext) {
       // close the corresponding UI
       context.setDismissed()
   }
}
```

Only one visual template or other InApp message can be displayed at a time by the SDK and no new messages can be shown until the current one is dismissed.

### In-App queue
When an in-app needs to be shown it is added to a queue (depending on its priority) and is displayed when all messages before it have been dismissed. The queue is persisted to the storage and kept across app launches to ensure all messages are displayed when possible. The custom code in-apps behave in the same way. They will be triggered once their corresponding notification is the next in the queue to be shown. However since the control of the dismissal is left to the application's code, the next in-app message will not be shown until the current code template has called `CustomTemplateContext.setDismissed`

### File downloading and caching
File arguments are automatically downloaded and are ready for use when an in-app template is presented. The files are downloaded when a file argument has changed and this file is not already cached. For client-side in-apps this happens both at App Launch and retried if needed when an in-app should be presented. For server-side in-apps the file downloading happens only before presenting the in-app. If any of the file arguments of an in-app fails to be downloaded, the whole in-app is skipped and the custom template will not be triggered.
