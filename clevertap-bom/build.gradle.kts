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
    }
}

publishing {
    publications.create<MavenPublication>("Maven") {
        from(project.components["javaPlatform"])

        pom {
            name.set(project.name)
            description.set("The CleverTap Android SDK")
            url.set("https://github.com/CleverTap/clevertap-android-sdk/tree/master")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            scm {
                connection.set("scm:git:github.com/CleverTap/clevertap-android-sdk.git")
                developerConnection.set("scm:git:ssh:github.com/CleverTap/clevertap-android-sdk.git")
                url.set("https://github.com/CleverTap/clevertap-android-sdk/tree/master")
            }
            developers {
                developer {
                    name.set("CleverTap")
                }
            }
        }
    }
}