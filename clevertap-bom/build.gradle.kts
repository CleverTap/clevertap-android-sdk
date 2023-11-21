ext {
    set("libraryName", "CleverTapAndroidSDK")
    set("artifact", "clevertap-android-sdk")
    set("libraryDescription", "The CleverTap Android SDK")

    set("libraryVersion", AndroidVersion.clevertapBoM)

    set("licenseName", "The Apache Software License, Version 2.0")
    set("licenseUrl", "http://www.apache.org/licenses/LICENSE-2.0.txt")
    set("allLicenses", "Apache-2.0")
    set("minSdkVersionVal", AndroidVersion.minSdkVersion)
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
    }
}