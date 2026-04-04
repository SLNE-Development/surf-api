import java.nio.file.Files

plugins {
    id("dev.slne.surf.api.gradle.core")

    idea
    `kotlin-dsl`
}

val mcVersion: String by project

dependencies {
    implementation("dev.slne.surf:surf-api-gradle-plugin:$mcVersion+")
}

gradlePlugin {
    plugins {
        create("dev.slne.surf.microservice.gradle.plugin") {
            id = "dev.slne.surf.microservice"
            implementationClass = "dev.slne.surf.microservice.gradle.plugin.MicroservicePlugin"
        }
    }

    publishing {
        repositories {
            maven("https://repo.slne.dev/repository/maven-releases/") {
                name = "maven-releases"
                credentials {
                    username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                    password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
                }
            }
        }
    }
}

val constantsOutputDirectory: Provider<Directory> =
    layout.buildDirectory.dir("generated/dev/slne/surf/microservice/gradle/generated")

val generateConstants by tasks.registering {
    val outputFile = constantsOutputDirectory.map { it.file("Constants.kt") }

    inputs.property("mcVersion", mcVersion)
    inputs.property("version", rootProject.findProperty("version") as String)

    outputs.dir(constantsOutputDirectory)

    doLast {
        val content = """
            |package dev.slne.surf.microservice.gradle.generated
            |
            |internal object Constants {
            |    const val MINECRAFT_VERSION = "$mcVersion"
            |    const val SURF_MICROSERVICE_VERSION = "$mcVersion+"
            |    const val SURF_MICROSERVICE_FULL_VERSION = "${rootProject.findProperty("version") as String}"
            |}
        """.trimMargin()

        Files.createDirectories(constantsOutputDirectory.get().asFile.toPath())

        outputFile.get().asFile.writeText(content)
    }
}

sourceSets.main {
    kotlin.srcDirs(generateConstants.map { it.outputs })
}

idea {
    module {
        generatedSourceDirs.add(constantsOutputDirectory.get().asFile)
    }
}