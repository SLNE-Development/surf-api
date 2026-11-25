package dev.slne.surf.api.generator.registry

import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.*

/**
 * Runs the vanilla data generator of a Minecraft version and returns its output.
 *
 * Everything lives in `<workDirectory>/<version>/`: the server jar from Mojang's piston-meta,
 * verified against its SHA-1, the libraries the jar's bundler unpacks next to it, and the
 * generator output in `generated/`. Output of a completed run is reused without network access;
 * delete the version directory to force a fresh run.
 *
 * @param workDirectory the directory holding one subdirectory per Minecraft version
 */
class VanillaDataGenerator(private val workDirectory: Path) {
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns the vanilla data of [minecraftVersion], generating it first when needed.
     *
     * @param minecraftVersion a version id as listed by piston-meta, such as `26.3`
     * @return the data generator output
     * @throws IllegalStateException if piston-meta does not list the version, a download returns
     * a status other than 200 or fails its SHA-1 check, or the data generator exits with an error
     */
    fun generate(minecraftVersion: String): VanillaData {
        val versionDirectory = (workDirectory / minecraftVersion).createDirectories()
        val outputDirectory = versionDirectory / "generated"
        val completionMarker = versionDirectory / "generated.done"

        if (completionMarker.notExists()) {
            val serverJar = downloadServerJar(minecraftVersion, versionDirectory / "server.jar")
            runDataGenerator(serverJar, outputDirectory)
            completionMarker.createFile()
        }

        return VanillaData(outputDirectory)
    }

    private fun downloadServerJar(minecraftVersion: String, target: Path): Path {
        val download = fetchServerDownload(minecraftVersion)
        if (target.exists() && target.sha1() == download.sha1) return target

        println("Downloading the server jar of Minecraft $minecraftVersion")
        val partial = target.resolveSibling("${target.fileName}.part")

        try {
            val response = httpClient.send(request(download.url), BodyHandlers.ofFile(partial))
            check(response.statusCode() == 200) {
                "GET ${download.url} returned ${response.statusCode()}"
            }
            check(partial.sha1() == download.sha1) {
                "The server jar from ${download.url} does not match SHA-1 ${download.sha1}"
            }
        } catch (exception: Exception) {
            partial.deleteIfExists()
            throw exception
        }

        return partial.moveTo(target, REPLACE_EXISTING)
    }

    private fun fetchServerDownload(minecraftVersion: String): VersionDetails.Download {
        val manifest = fetchJson<VersionManifest>(VERSION_MANIFEST_URL)
        val version = manifest.versions.find { it.id == minecraftVersion }
            ?: error("$VERSION_MANIFEST_URL does not list Minecraft $minecraftVersion")
        val details = fetchJson<VersionDetails>(version.url)
        return details.downloads["server"]
            ?: error("Minecraft $minecraftVersion has no server download")
    }

    @OptIn(ExperimentalPathApi::class)
    private fun runDataGenerator(serverJar: Path, outputDirectory: Path) {
        outputDirectory.deleteRecursively()
        println("Running the vanilla data generator")

        // The bundler unpacks its libraries into the working directory.
        val exitCode = ProcessBuilder(
            javaExecutable(),
            "-DbundlerMainClass=net.minecraft.data.Main",
            "-jar", serverJar.absolutePathString(),
            "--server",
            "--reports",
            "--output", outputDirectory.absolutePathString(),
        )
            .directory(serverJar.parent.toFile())
            .inheritIO()
            .start()
            .waitFor()
        check(exitCode == 0) { "The vanilla data generator exited with code $exitCode" }
    }

    private inline fun <reified T> fetchJson(url: String): T {
        val response = httpClient.send(request(url), BodyHandlers.ofString())
        check(response.statusCode() == 200) { "GET $url returned ${response.statusCode()}" }

        return json.decodeFromString<T>(response.body())
    }

    private fun request(url: String) = HttpRequest.newBuilder(URI(url)).GET().build()

    private fun javaExecutable(): String {
        return ProcessHandle.current().info().command()
            .orElseThrow { IllegalStateException("Cannot locate the running Java executable") }
    }

    private fun Path.sha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        inputStream().use { it.copyTo(DigestOutputStream(OutputStream.nullOutputStream(), digest)) }

        return HexFormat.of().formatHex(digest.digest())
    }
}
