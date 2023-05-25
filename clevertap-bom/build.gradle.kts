ext {
    set("libraryName", "CleverTapAndroidSDK")
    set("artifact", "clevertap-android-sdk")
    set("libraryDescription", "The CleverTap Android SDK")

    set("libraryVersion", Android.clevertapBoM)

    set("licenseName", "The Apache Software License, Version 2.0")
    set("licenseUrl", "http://www.apache.org/licenses/LICENSE-2.0.txt")
    set("allLicenses", "Apache-2.0")
    set("minSdkVersionVal", Android.minSdkVersionVal)
}
plugins {
    id("java-platform")
    id("maven-publish")
}

apply(from = "../gradle-scripts/commons.gradle")
dependencies {
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
          if (subproject.name != "clevertap-bom" && subproject.name != "test_shared"
              && subproject.name != "sample") {
            api(subproject)
          }
        }

        api("com.clevertap.android:clevertap-android-sdk:5.0.0")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(project.components["javaPlatform"])

        pom {
            name.set(project.name)
            description.set("Square’s meticulous HTTP client for Java and Kotlin.")
            url.set("https://square.github.io/okhttp/")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/square/okhttp.git")
                developerConnection.set("scm:git:ssh://git@github.com/square/okhttp.git")
                url.set("https://github.com/square/okhttp")
            }
            developers {
                developer {
                    name.set("Square, Inc.")
                }
            }
        }
    }
}