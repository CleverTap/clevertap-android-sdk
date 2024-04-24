@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("com.android.application")
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.clevertap.android.benchmark.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.clevertap.android.benchmark.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.profileinstaller)

   /* implementation(project(":clevertap-core")) //CleverTap Android SDK, make sure the AAR file is in the libs folder
    implementation(project(":clevertap-geofence")) // Geofence
    implementation(project(":clevertap-xps")) // For Xiaomi Push use
    implementation(project(":clevertap-pushtemplates"))
    implementation(project(":clevertap-hms")) // For Huawei Push use*/

    implementation("com.clevertap.android:clevertap-android-sdk:6.0.0")
    implementation("com.clevertap.android:clevertap-geofence-sdk:1.3.0")
    implementation("com.clevertap.android:clevertap-xiaomi-sdk:1.5.4")
    implementation("com.clevertap.android:push-templates:1.2.3")
    implementation("com.clevertap.android:clevertap-hms-sdk:1.3.4")

    implementation("com.google.android.gms:play-services-location:21.0.0") // Needed for geofence
    implementation("androidx.work:work-runtime:2.7.1") // Needed for geofence
    implementation("androidx.concurrent:concurrent-futures:1.1.0") // Needed for geofence

    implementation("com.google.firebase:firebase-messaging:23.0.6") //Needed for FCM
    implementation("com.google.android.gms:play-services-ads:20.4.0") //Needed to use Google Ad Ids
    //ExoPlayer Libraries for Audio/Video InApp Notifications
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.github.bumptech.glide:glide:4.12.0")

    //Mandatory if you are using Notification Inbox
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.fragment:fragment:1.3.6")
    implementation("androidx.appcompat:appcompat:1.6.0-rc01")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.viewpager:viewpager:1.0.0")

    implementation("com.android.installreferrer:installreferrer:2.2")

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}