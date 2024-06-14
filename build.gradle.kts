plugins {
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    kotlin("jvm") version "2.0.0" apply false
}

repositories {
    gradlePluginPortal()
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}