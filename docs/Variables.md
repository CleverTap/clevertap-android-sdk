# Supported Variable Types

Currently, CleverTap SDK supports the following variable types:

- boolean
- byte
- short
- int
- long
- float
- double
- String

# Define Variables

One can define variables using the following three methods:

- Using the `defineVariable(...)` method. 
- Using the `@Variable` annotation on a static field of a class
- Using the `@Variable` annotation on an instance field of a class

## Using `defineVariable(...)` Method

You can provide the name and default value of the variable when calling  `defineVariable(...)` method. 

Java:
```java
Var<Byte> var1 = cleverTap.defineVariable("var_byte", (byte) 1);

Var<Short> var2 = cleverTap.defineVariable("var_short", (short) 2);

Var<Integer> var3 = cleverTap.defineVariable("var_int", 3);

Var<Long> var4 = cleverTap.defineVariable("var_long", 4L);

Var<Float> var5 = cleverTap.defineVariable("var_float", 5F);

Var<Double> var6 = cleverTap.defineVariable("var_double", 6.);

Var<String> var7 = cleverTap.defineVariable("var_string", "str");

Var<Boolean> var8 = cleverTap.defineVariable("var_boolean", true);
```
Kotlin:
```kotlin
val var1: Var<Byte> = cleverTap.defineVariable("var_byte", 1)

val var2: Var<Short> = cleverTap.defineVariable("var_short", 2)

val var3: Var<Int> = cleverTap.defineVariable("var_int", 3)

val var4: Var<Long> = cleverTap.defineVariable("var_long", 4L)

val var5: Var<Float> = cleverTap.defineVariable("var_float", 5F)

val var6: Var<Double> = cleverTap.defineVariable("var_double", 6.0)

val var7: Var<String> = cleverTap.defineVariable("var_string", "str")

val var8: Var<Boolean> = cleverTap.defineVariable("var_boolean", true)
```



## Using `@Variable` Annotation on a Static Variable

You can use the `@Variable` annotation when defining a static variable; however, the class field must be publicly accessible. After defining the variables, you must instruct the CleverTap Android SDK to parse that specific class by calling the `parseVariablesForClasses()` method 

Java:
```java
public class MyClass {
  @Variable
  public static boolean var_boolean = true;

  @Variable
  public static byte var_byte = 1;

  @Variable
  public static short var_short = 2;

  @Variable
  public static int var_int = 3;

  @Variable
  public static long var_long = 4L;

  @Variable
  public static float var_float = 5F;

  @Variable
  public static double var_double = 6.;

  @Variable
  public static String var_string = "str";
}

// You must instruct CleverTap SDK that you have such definition by using:
cleverTap.parseVariablesForClasses(MyClass.class);
```
Kotlin:
```kotlin
// Using the @Variable annotation in Kotlin files is not supported yet.
// It will be added in a future release.
```



> ❗️ Proguard
> 
> It is mandatory to skip your classes from Proguard when using the `@Variable` annotation, because changing the names of the fields lead to runtime errors:
> 
> ```groovy
> -keep class com.package.MyClass {
>     *;
> }
> ```

## Using `@Variable` Annotation on an Instance Variable

You can use the annotation `@Variable` when defining an instance variable; however, the class field must be publicly accessible. After defining the variables, you must instruct the CleverTap Android SDK to parse that specific instance variable using the `parseVariables(instance)`.  This method for parsing the variables is different from the `parseVariablesForClasses()` method and takes the instance as an argument

Java:
```java
public class MyClass {
  @Variable
  public boolean var_boolean = true;

  @Variable
  public byte var_byte = 1;

  @Variable
  public short var_short = 2;

  @Variable
  public int var_int = 3;

  @Variable
  public long var_long = 4L;

  @Variable
  public float var_float = 5F;

  @Variable
  public double var_double = 6.;

  @Variable
  public String var_string = "str";
}

// You must instruct CleverTap SDK that you have such definition by using:
MyClass instance = new MyClass();
cleverTap.parseVariables(instance);
```
Kotlin:
```kotlin
// Using the @Variable annotation in Kotlin files is not supported yet.
// It will be added in a future release.
```



> ❗️ Proguard
> 
> It is mandatory to skip your classes from Proguard when using the `@Variable` annotation, because changing the names of the fields lead to runtime errors:
> 
> ```groovy
> -keep class com.package.MyClass {
>     *;
> }
> ```

# Set Up Callbacks

CleverTap Android SDK provides several callbacks for the developer to receive feedback from the SDK. You can use them as per your requirement, using all of them is not mandatory. They are as follows:

- Status of fetch variables request.
- Change in individual variable value.
- Initialize all variables or change all variable values

## Status of Fetch Variables Request

The `FetchVariablesCallback()` method provides feedback to ensure that the variables were successfully retrieved from the server.

Java:
```java
cleverTap.fetchVariables(new FetchVariablesCallback() {
    @Override
    public void onVariablesFetched(boolean isSuccess) {
        // isSuccess is true when server request is successful, false otherwise
    }
});
```
Kotlin:
```kotlin
cleverTap.fetchVariables { isSuccess ->
  // isSuccess is true when server request is successful, false otherwise
}
```



> 📘 Retrieving Variables
> 
> The retrieval of variables is limited to a single callback, meaning that any subsequent calls to the method overwrites the callback internally. As a result, only the most recent callback is triggered when the variables are retrieved.

## Change in Individual Variable Value

The individual callback is registered per variable. It is called on the app start or whenever the variable value changes.

Java:
```java
Var variable = cleverTap.defineVariable("var_int", 1);
variable.addValueChangedCallback(new VariableCallback() {
    @Override
    public void onValueChanged(Var varInstance) {
        // invoked on app start and whenever value is changed
    }
});
```
Kotlin:
```kotlin
val variable: Var<Int> = cleverTap.defineVariable("var_int", 1)
variable.addValueChangedCallback(object : VariableCallback<Int>() {
  override fun onValueChanged(varInstance: Var<Int>) {
    // invoked on app start and whenever value is changed
  }
})
```



## Initialize All Variables or Change All Variable Values

The global callback registered on the CleverTap instance is called when variables are initialized with a value or changed with a new server value.

Java:
```java
// invoked on app start and whenever vars are fetched from server
cleverTap.addVariablesChangedCallback(new VariablesChangedCallback() {
    @Override
    public void variablesChanged() {
        // implement
    }
});
// invoked only once on app start, or when added if server values are already received
cleverTap.addOneTimeVariablesChangedCallback(new VariablesChangedCallback() {
    @Override
    public void variablesChanged() {
        // implement
    }
});
```
Kotlin:
```kotlin
// invoked on app start and whenever vars are fetched from server
cleverTap.addVariablesChangedCallback(object : VariablesChangedCallback() {
  override fun variablesChanged() {
    // implement
  }
})

// invoked only once on app start, or when added if server values are already received
cleverTap.addOneTimeVariablesChangedCallback(object : VariablesChangedCallback() {
  override fun variablesChanged() {
    // implement
  }
})
```



# Sync Defined Variables

After defining your variables in the code, you must send/sync variables to the server. To do so, the app must be in DEBUG mode and mark a particular CleverTap user profile as a test profile from the CleverTap dashboard. [Learn how to mark a profile as **Test Profile**](https://developer.clevertap.com/docs/concepts-user-profiles#mark-a-user-profile-as-a-test-profile)

After marking the profile as a test profile,  you must sync the app variables in DEBUG mode:

Java:
```java
cleverTap.syncVariables();
```
Kotlin:
```kotlin
cleverTap.syncVariables()
```



> 📘 Key Points to Remember
> 
> - In a scenario where there is already a draft created by another user profile in the dashboard, the sync call will fail to avoid overriding important changes made by someone else. In this case, Publish or Dismiss the existing draft before you proceed with syncing variables again. However, you can override a draft you created via the sync method previously to optimize the integration experience.
> - You can receive the following Logcat logs from the CleverTap SDK:
>   - Variables synced successfully.
>   - Unauthorized access from a non-test profile. To address this, mark the profile as a test profile from the CleverTap dashboard.

# Fetch Variables During a Session

During a session, you can fetch the updated values for your CleverTap variables from the server. If variables have changed, the appropriate callbacks are fired. The callback returns a boolean flag that indicates if the update was successful. The callback is fired regardless of whether the variables have changed or not.

Java:
```java
cleverTap.fetchVariables(new FetchVariablesCallback() {
    @Override
    public void onVariablesFetched(boolean isSuccess) {
        // isSuccess is true when server request is successful, false otherwise
    }
});
```
Kotlin:
```kotlin
cleverTap.fetchVariables { isSuccess ->
  // isSuccess is true when server request is successful, false otherwise
}
```



# Use Fetched Variables Values

This process involves the following two major steps

1. Fetch variable values.
2. Access variable values.

## Fetch Variable Values

Variables are updated automatically when server values are received. If you want to receive feedback when a specific variable is updated, use the individual callback:

Java:
```java
variable.addValueChangedCallback(new VariableCallback() {
    @Override
    public void onValueChanged(Var varInstance) {
        // invoked on app start and whenever value is changed
    }
});
```
Kotlin:
```kotlin
variable.addValueChangedCallback(object : VariableCallback<Int>() {
  override fun onValueChanged(varInstance: Var<Int>) {
    // invoked on app start and whenever value is changed
  }
})
```



## Access Variable Values

You can access these fetched values in the following three ways

### From Annotated @Variable field

Class fields annotated by `@Variable` are automatically updated and when you use the field it will have the latest server value.

### From `Var` instance

You can use several methods on the `Var` instance as shown in the following code:

Java:
```java
variable.defaultValue(); // returns default value
variable.value(); // returns current value
variable.numberValue(); // returns value as Number if applicable
variable.stringValue(); // returns value as String
```
Kotlin:
```kotlin
variable.defaultValue() // returns default value
variable.value() // returns current value
variable.numberValue() // returns value as Number if applicable
variable.stringValue() // returns value as String
```



### Using `CleverTapAPI` Class

You can use the `CleverTapAPI` class to get the current value of a variable. If the variable is nonexistent, the method returns `null`:

Java:
```java
cleverTap.getVariableValue(“variable name”);
```
Kotlin:
```kotlin
cleverTap.getVariableValue(“variable name”)
```
