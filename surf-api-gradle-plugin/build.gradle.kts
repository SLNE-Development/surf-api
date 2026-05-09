import java.nio.file.Files

// region properties
val relocationPrefix: String by project
val mcVersion: String by project
val groupId = findProperty("group") as String
val javaVersion: String by project
val snapshot = (findProperty("snapshot") as String).toBooleanStrict()
// endregion

plugins {
    `java-library`
    `kotlin-dsl`
    `java-toolchain-convention`

    id("com.gradle.plugin-publish") version "2.1.1"
    kotlin("plugin.serialization")
    idea
}

group = groupId
version = buildString {
    append("2.0.5")
    if (snapshot) append("-SNAPSHOT")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val pluginDependencies = listOf(
    libs.kotlin.gradle.plugin,
    libs.kotlin.all.open,
    libs.kotlin.no.arg,
    libs.kotlin.serialization,
    libs.shadow.gradle.plugin,
    libs.run.paper.gradle.plugin,
    libs.plugin.yml.paper.gradle.plugin,
    libs.ksp.gradle.plugin
)

dependencies {
    compileOnly(gradleApi())
    pluginDependencies.forEach { dep -> api(dep) }

    implementation("com.palantir.javapoet:javapoet:0.14.0")
    implementation(libs.bundles.kotlin.serialization)
}

gradlePlugin {
    plugins {
        create("settings") {
            id = "dev.slne.surf.api.gradle.settings"
            implementationClass = "dev.slne.surf.api.gradle.settings.SurfSettingsPlugin"
        }

        create("core") {
            id = "dev.slne.surf.api.gradle.core"
            implementationClass = "dev.slne.surf.api.gradle.platform.core.CoreSurfPlugin"
        }

        create("paper-plugin") {
            id = "dev.slne.surf.api.gradle.paper-plugin"
            implementationClass =
                "dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfPlugin"
        }

        create("paper-raw") {
            id = "dev.slne.surf.api.gradle.paper-raw"
            implementationClass = "dev.slne.surf.api.gradle.platform.paper.raw.RawPaperSurfPlugin"
        }

        create("standalone") {
            id = "dev.slne.surf.api.gradle.standalone"
            implementationClass =
                "dev.slne.surf.api.gradle.platform.standalone.StandaloneSurfPlugin"
        }

        create("velocity") {
            id = "dev.slne.surf.api.gradle.velocity"
            implementationClass = "dev.slne.surf.api.gradle.platform.velocity.VelocitySurfPlugin"
        }
    }

    publishing {
        repositories {
            maven("https://reposilite.slne.dev/releases/") {
                name = "slne-repository-releases"
                credentials {
                    username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                    password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
                }
            }
        }
    }
}

val constantsOutputDir =
    layout.buildDirectory.dir("generated/dev/slne/surf/api/gradle/generated")
val generateConstants by tasks.registering {
    val outputFile = constantsOutputDir.map { it.file("Constants.kt") }

    inputs.property("relocationPrefix", relocationPrefix)
    inputs.property("javaVersion", javaVersion)
    inputs.property("mcVersion", mcVersion)
    inputs.property("libs.paper.api", libs.paper.api.get().toString())
    inputs.property("libs.canvas.api", libs.canvas.api.get().toString())
    inputs.property("libs.velocity.api", libs.velocity.api.get().toString())
    inputs.property("libs.auto.service.annotations", libs.auto.service.annotations.get().toString())
    inputs.property("libs.versions.commandapi", libs.versions.commandapi.get())
    inputs.property("libs.versions.placeholder.api", libs.versions.placeholder.api.get())
    inputs.property("libs.versions.luckperms", libs.versions.luckperms.get())
    inputs.property(
        "libs.versions.packetevents",
        libs.versions.packetevents.plugin.get()
    )
    inputs.property(
        "version",
        rootProject.findProperty("version") as String + if (snapshot) "-SNAPSHOT" else ""
    )
    outputs.dir(constantsOutputDir)

    doLast {
        val generator = project(":surf-api-gradle-plugin:surf-api-processor")
        val content = """
            |package dev.slne.surf.api.gradle.generated
            |
            |internal object Constants {
            |    const val RELOCATION_PREFIX = "$relocationPrefix"
            |    const val SNAPSHOT_REPO_ID = "slne-repository-releases"
            |    const val SNAPSHOT_REPO = "https://reposilite.slne.dev/releases"
            |    const val PAPER_API = "${libs.paper.api.get()}"
            |    const val CANVAS_API = "${libs.canvas.api.get()}"
            |    const val VELOCITY_API = "${libs.velocity.api.get()}"
            |    const val AUTO_SERVICE_ANNOTATIONS = "${libs.auto.service.annotations.get()}"
            |    const val AUTO_SERVICE = "${generator.group}:${generator.name}:${generator.version}"
            |
            |    const val JAVA_VERSION = $javaVersion
            |    const val MINECRAFT_VERSION = "$mcVersion"
            |    const val SURF_API_VERSION = "+"
            |
            |    const val COMMAND_API_VERSION = "${libs.versions.commandapi.get()}"
            |    const val PLACEHOLDER_API_VERSION = "${libs.versions.placeholder.api.get()}"
            |    const val LUCKPERMS_VERSION = "${libs.versions.luckperms.get()}"
            |    const val PACKETEVENTS_VERSION = "${libs.versions.packetevents.plugin.get()}"
            |
            |    const val SURF_API_FULL_VERSION = "${rootProject.findProperty("version") as String + if (snapshot) "-SNAPSHOT" else ""}"
            |}
        """.trimMargin()

        Files.createDirectories(constantsOutputDir.get().asFile.toPath())
        outputFile.get().asFile.writeText(content)
    }
}

sourceSets.main {
    kotlin.srcDir(generateConstants.map { it.outputs })
}

idea {
    module {
        generatedSourceDirs.add(constantsOutputDir.get().asFile)
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
