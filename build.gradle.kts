// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
}
// In your project-level build.gradle.kts
allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.20")
        }
    }
}
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}