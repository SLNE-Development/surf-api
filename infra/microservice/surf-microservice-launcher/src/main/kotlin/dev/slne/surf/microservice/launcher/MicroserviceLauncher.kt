package dev.slne.surf.microservice.launcher

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.surfapi.core.api.util.requiredService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {
    val launcher = requiredService<MicroserviceLauncher<*>>()

    launcher.run()
}

abstract class MicroserviceLauncher<M : Microservice> {
    abstract fun instantiate(): M

    val metadata: MicroserviceMetadata by lazy {
        val json = MicroserviceLauncher::class.java
            .getResourceAsStream("META-INF/microservice.json")
            ?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: throw IllegalStateException("Could not find microservice.json in classpath")

        Json.decodeFromString(json)
    }

    fun run() {
        if (Path.of("").toAbsolutePath().toString().contains("!")) {
            System.err.println("surf-microservice-launcher cannot be run from a JAR file. Please run it from the exploded directory.")
            exitProcess(1)
        }

        val classpathUrls = setupClasspath()
        val parentClassLoader = ClassLoader.getSystemClassLoader()

        val classLoader = URLClassLoader(classpathUrls.toTypedArray(), parentClassLoader)
        val launcherThread = thread(name = "LauncherThread") {
            val instance = instantiate()

            runBlocking {
                instance.bootstrap()
            }
        }

        launcherThread.contextClassLoader = classLoader
        launcherThread.start()
    }

    private fun setupClasspath(): List<URL> = listOf(
        getServiceJar(),
        *loadLibraries().toTypedArray()
    )

    private fun getServiceJar(): URL {
        val path = System.getProperty("launcher.serviceJarPath") ?: run {
            val resource = javaClass.getResource("/service.jar.disabled")
                ?: throw IllegalStateException("Could not find service.jar.disabled in classpath")

            val temp = File.createTempFile("service", ".jar")

            resource.openStream().use {
                Files.copy(it, temp.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            temp.absolutePath
        }

        return Path.of(path).toUri().toURL()
    }

    private fun loadLibraries(): List<URL> {
        val libraryLoader = MicroserviceLibraryLoader()
        val paths = libraryLoader.loadLibraries(metadata.dependencies)

        return paths.map { it.toUri().toURL() }
    }
}