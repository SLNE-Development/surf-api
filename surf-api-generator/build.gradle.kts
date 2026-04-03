@file:OptIn(ExperimentalPathApi::class)

import io.papermc.paperweight.util.Hash
import io.papermc.paperweight.util.HashingAlgorithm
import io.papermc.paperweight.util.fromJson
import io.papermc.paperweight.util.gson
import org.gradle.kotlin.dsl.support.serviceOf
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

plugins {
    `core-convention`
}

dependencies {
    implementation(libs.bundles.kotlin.serialization)

    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("org.jetbrains:annotations:26.1.0")
    implementation(libs.paper.api)

    implementation(projects.surfApiCore.surfApiCoreApi)
}

val mcManifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

val downloadRegistriesTask by tasks.register("downloadRegistries") {
    group = "generation"
    val mcVersion = project.findProperty("mcVersion") as String
    val tmp = temporaryDir.toPath()

    doLast {
        println("Downloading Minecraft data for version $mcVersion...")
        val mcManifestPath = tmp.resolve("mc.json")
        mcManifestUrl.downloadTo(mcManifestPath)
        val mcManifest = gson.fromJson<MinecraftManifest>(mcManifestPath)

        println("Downloading version manifest for $mcVersion...")
        val versionManifestPath = tmp.resolve("version.json")
        val ver = mcManifest.versions.first { it.id == mcVersion }
        ver.url.downloadTo(versionManifestPath)
        val versionManifest = gson.fromJson<MinecraftVersionManifest>(versionManifestPath)

        println("Downloading server jar for $mcVersion...")
        val serverTmp = tmp.resolve("server.jar")
        versionManifest.clientDownload().url.downloadTo(serverTmp)

        println("Downloading libraries for $mcVersion...")
        val librariesDir = tmp.resolve("libraries")
        librariesDir.createDirectories()

        versionManifest.libraries.forEach { lib ->
            val artifact = lib.downloads.artifact
            val libPath = librariesDir.resolve(artifact.path)
            libPath.parent.createDirectories()

            println("—  Downloading ${artifact.url}...")
            artifact.url.downloadTo(libPath)
        }


        val classPath = listOf(serverTmp) + librariesDir.walk()
            .filter { it.absolutePathString().endsWith(".jar") }
        val outputDir = file("registries")
        outputDir.mkdirs()

        println("Launching Minecraft data generator...")

        val execOps = project.serviceOf<ExecOperations>()
        execOps.javaexec {
            mainClass.set("net.minecraft.data.Main")
            classpath = files(classPath.map { it.toFile() })
            args("--reports", "--output", outputDir.absolutePath, "--all")

            standardOutput = System.out
            errorOutput = System.err
        }

        println("Cleaning up...")
        val resourcesDir = file("src/main/resources/registries")
        resourcesDir.deleteRecursively()
        outputDir.resolve("reports").toPath()
            .moveTo(resourcesDir.toPath(), StandardCopyOption.REPLACE_EXISTING)
        outputDir.resolve("data").resolve("minecraft").resolve("advancement").toPath()
            .moveTo(
                resourcesDir.resolve("advancement").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        outputDir.deleteRecursively()

        println("Done!")
    }
}

tasks.register<JavaExec>("generate") {
    group = "generation"
    dependsOn(tasks.check)
    dependsOn(downloadRegistriesTask)

    mainClass.set("dev.slne.surf.api.gen.MainKt")
    classpath(sourceSets.main.map { it.runtimeClasspath })
    args(projectDir.toPath().resolve("generated").toString())
}


private fun String.downloadTo(output: Path) {
    uri(this).toURL().openStream()
        .use { Files.copy(it, output, StandardCopyOption.REPLACE_EXISTING) }
}

data class MinecraftManifest(
    val latest: Map<String, *>,
    val versions: List<ManifestVersion>,
)

data class ManifestVersion(
    val id: String,
    val type: String,
    val time: String,
    val releaseTime: String,
    val url: String,
    val sha1: String,
) {
    fun hash(): Hash = Hash(sha1, HashingAlgorithm.SHA1)
}

data class MinecraftVersionManifest(
    val downloads: Map<String, Download>,
    val libraries: List<Library>,
) {
    data class Download(
        val sha1: String,
        val url: String,
    ) {
        fun hash(): Hash = Hash(sha1, HashingAlgorithm.SHA1)
    }

    data class Library(
        val downloads: LibraryDownloads,
        val name: String,
    ) {
        data class LibraryDownloads(
            val artifact: Artifact,
        ) {
            data class Artifact(
                val path: String,
                val sha1: String,
                val size: Long,
                val url: String,
            )
        }
    }

    fun download(name: String): Download {
        return downloads[name] ?: error("No such download '$name' in version manifest")
    }

    fun clientDownload(): Download = download("client")
}