plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.kotlin.serialization)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}