import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val libs = the<LibrariesForLibs>()

plugins {
    java
    `java-library`
    `maven-publish`

    kotlin("jvm")
    kotlin("kapt")

    id("com.google.devtools.ksp")
    id("com.gradleup.shadow")
    id("org.hibernate.build.maven-repo-auth")
}

group = findProperty("group") as String
version = findProperty("version") as String

repositories {
    mavenCentral()
    maven("https://repo.slne.dev/repository/maven-proxy") { name = "maven-proxy" }
    maven("https://repo.slne.dev/repository/maven-public") { name = "maven-public" }
}

dependencies {
    compileOnly(libs.auto.service.annotations)
    "kapt"(libs.auto.service)

    compileOnlyApi("org.jetbrains:annotations:24.1.0")
}

extensions.configure<KotlinJvmProjectExtension> {
    val javaVersion: String by project

    jvmToolchain(javaVersion.toInt())
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

kapt {
    keepJavacAnnotationProcessors = true
}

publishing {
    repositories {
        maven("https://repo.slne.dev/repository/maven-snapshots/") { name = "maven-snapshots" }
    }

    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }
}
