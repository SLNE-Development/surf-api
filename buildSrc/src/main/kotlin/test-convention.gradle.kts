import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    `java-library`
    kotlin("jvm")
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnlyApi.get())
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.lincheck)
    testImplementation(libs.kotlinxCoroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Lincheck attaches its bytecode-instrumentation agent dynamically at runtime
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
