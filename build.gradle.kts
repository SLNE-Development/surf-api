plugins {
    java
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    id("io.papermc.paperweight.core") version "1.7.1"
    kotlin("jvm") version libs.versions.kotlinVersion apply false
    kotlin("kapt") version libs.versions.kotlinVersion
    kotlin("plugin.lombok") version libs.versions.kotlinVersion
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    idea
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    paramMappingsRepo = paperMavenPublicUrl
    remapRepo = paperMavenPublicUrl
    decompileRepo = paperMavenPublicUrl
    minecraftVersion = "1.21"
}

repositories {
    gradlePluginPortal()
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

        implementation("com.nicholasnassar.dslbuilder:dsl-builder-api:0.0.2")
        "ksp"("com.nicholasnassar.dslbuilder:dsl-builder-ksp:0.0.2")
    }

    project.idea {
        module {
            sourceDirs = sourceDirs + project.file("build/generated/ksp/main/kotlin")
            testSourceDirs = testSourceDirs + project.file("build/generated/ksp/test/kotlin")
            generatedSourceDirs = generatedSourceDirs + project.file("build/generated/ksp/main/kotlin") + project.file("build/generated/ksp/test/kotlin")
        }
    }

    project.kapt {
        keepJavacAnnotationProcessors = true
    }
}

dependencies {
    paramMappings("net.fabricmc:yarn:1.21+build.1:mergedv2")
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
//    spigotDecompiler("io.papermc:patched-spigot-fernflower:0.1+build.13")
    paperclip("io.papermc:paperclip:3.0.3")
}





