plugins {
    java
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-LOCAL-SNAPSHOT" apply false
    kotlin("jvm") version libs.versions.kotlinVersion apply false
    kotlin("kapt") version libs.versions.kotlinVersion
    kotlin("plugin.lombok") version libs.versions.kotlinVersion
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.java-library")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "com.google.devtools.ksp")
    apply(plugin = "org.gradle.idea")
    apply(plugin = "org.jetbrains.kotlin.plugin.lombok")


    repositories {
        mavenCentral()
    }

    dependencies {
        api(kotlin("stdlib", rootProject.libs.versions.kotlinVersion.get()))
        api(rootProject.libs.kotlinxCoroutines.core)
        api(rootProject.libs.kotlinxCoroutines.reactive)
        api(rootProject.libs.kotlinxCoroutines.reactor)
        api(rootProject.libs.kotlin.reflect)
        implementation("com.google.auto.service:auto-service:1.1.1")
        "kapt"("com.google.auto.service:auto-service:1.1.1")
    }

    project.kapt {
        keepJavacAnnotationProcessors = true
    }
}





