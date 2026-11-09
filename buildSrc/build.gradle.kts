plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

// `kotlin-dsl` gives buildSrc a test source set but no test framework, and the checks in here
// decide whether a build passes, so they get the same treatment as the code they guard.
tasks.test { useJUnitPlatform() }

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.kotlin.serialization)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.asm)
    implementation(libs.gremlin.gradle.plugin)
}