plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}
allprojects {
    repositories {
        maven(url = "https://jitpack.io")
    }
}
android {
    namespace = "com.example.ct_macrobenchmark"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 33
        missingDimensionStrategy("remote","deps")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        testInstrumentationRunnerArguments["androidx.benchmark.compilation.enabled"] = "false"
        testInstrumentationRunnerArguments["androidx.benchmark.startupProfiles.enable"] = "false"

    }

    flavorDimensions += "deps"
    productFlavors {
        create("remote") {
           dimension = "deps"
            //applicationIdSuffix = ".remote"
            //versionNameSuffix = "-remote"
        }
        create("local") {
            dimension = "deps"
        }
        create("staging") {
            dimension = "deps"
            //applicationIdSuffix = ".staging"
            //versionNameSuffix = "-staging"

            repositories {
                maven ( url = "https://oss.sonatype.org/content/groups/staging/" )
            }
        }
    }

    buildTypes {
        // This benchmark buildType is used for benchmarking, and should function like your
        // release build (for example, with minification on). It"s signed with a debug key
        // for easy local/CI testing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":sample"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.2.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.3")
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enabled = it.buildType == "benchmark"
    }
}