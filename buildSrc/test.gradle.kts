// File: buildSrc/src/main/kotlin/java-project-conventions.gradle.kts
plugins {
    `java-library`
    id("com.android.lint")
}

repositories {
    mavenCentral()
}

dependencies {

    // Logging
    implementation(Deps.futures)

}
