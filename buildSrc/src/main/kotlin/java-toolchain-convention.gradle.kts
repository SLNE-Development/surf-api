import gradle.kotlin.dsl.accessors._bcd9a993373509de50154c5485fe667f.java
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
