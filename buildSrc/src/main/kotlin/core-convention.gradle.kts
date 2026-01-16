import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val libs = the<LibrariesForLibs>()

plugins {
    java
    `java-library`
    id("publish-convention")

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.google.devtools.ksp")
    id("com.gradleup.shadow")
}

val snapshot = (findProperty("snapshot") as String).toBooleanStrict()

group = findProperty("group") as String
version = findProperty("version") as String + if (snapshot) "-SNAPSHOT" else ""

repositories {
    mavenCentral()
    maven("https://repo.slne.dev/repository/maven-public") { name = "maven-public" }
}

dependencies {
    compileOnly(libs.auto.service.annotations)
    ksp(project(":surf-api-gradle-plugin:surf-api-processor"))

    compileOnlyApi("org.jetbrains:annotations:26.0.2-1")
}

ksp {
    arg("autoserviceKsp.verbose", "true")
    arg("autoserviceKsp.verify", "true")
}

extensions.configure<KotlinJvmProjectExtension> {
    val javaVersion: String by project

    jvmToolchain(javaVersion.toInt())
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

java {
    withSourcesJar()

    if (project.name != "surf-api-bukkit-plugin-test") {
        withJavadocJar()
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks {
    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    javadoc {
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.tags("apiNote:a:API Note:")
    }
}
