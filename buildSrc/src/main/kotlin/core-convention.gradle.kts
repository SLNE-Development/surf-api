import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val libs = the<LibrariesForLibs>()

plugins {
    java
    `java-library`
    `maven-publish`

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.google.devtools.ksp")
    id("com.gradleup.shadow")
}

group = findProperty("group") as String
version = findProperty("version") as String

repositories {
    mavenCentral()
    maven("https://repo.slne.dev/repository/maven-public") { name = "maven-public" }
}

dependencies {
    compileOnly(libs.auto.service.annotations)
    ksp(libs.auto.service)

    compileOnlyApi("org.jetbrains:annotations:24.1.0")
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

publishing {
    repositories {
        maven("https://repo.slne.dev/repository/maven-snapshots/") {
            name = "maven-snapshots"
            credentials {
                username = System.getenv("SLNE_SNAPSHOTS_REPO_USERNAME")
                password = System.getenv("SLNE_SNAPSHOTS_REPO_PASSWORD")
            }
        }

        maven("https://maven.pkg.github.com/SLNE-DEVELOPMENT/surf-api") {
            name = "GitHubPackages"
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
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

    javadoc {
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.tags("apiNote:a:API Note:")
    }
}
