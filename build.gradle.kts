plugins {
    java
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    id("io.papermc.paperweight.core") version "1.7.1"
    kotlin("jvm") version libs.versions.kotlinVersion apply false
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


    repositories {
        mavenCentral()
    }

    dependencies {
        api(kotlin("stdlib"))
        api(rootProject.libs.kotlinxCoroutines.core)
        api(rootProject.libs.kotlin.reflect)
    }
}

dependencies {
    paramMappings("net.fabricmc:yarn:1.21+build.1:mergedv2")
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
//    spigotDecompiler("io.papermc:patched-spigot-fernflower:0.1+build.13")
    paperclip("io.papermc:paperclip:3.0.3")
}





