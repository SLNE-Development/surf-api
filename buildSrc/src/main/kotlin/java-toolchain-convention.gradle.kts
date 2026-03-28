import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val javaVersion: String by project

plugins {
    java
}

extensions.findByType<KotlinJvmProjectExtension>()?.apply {
    jvmToolchain(javaVersion.toInt())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}
