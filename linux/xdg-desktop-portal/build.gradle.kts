plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(projects.autoDarkModeBase)
    implementation(libs.darklaf.nativeUtils)

    implementation("com.github.hypfvieh:dbus-java:3.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.1")

    kapt(libs.autoservice.processor)
    compileOnly(libs.autoservice.annotations)
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
}
