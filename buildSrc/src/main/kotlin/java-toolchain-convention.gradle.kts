import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val javaVersion = project.findProperty("javaVersion") as String

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
