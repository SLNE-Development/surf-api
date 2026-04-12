package dev.slne.surf.api.paper.server.display.web

import javafx.application.Platform
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

object JavaFxPlatform {
    private val initialized = AtomicBoolean(false)
    private val logger = Logger.getLogger("SurfDisplay-JavaFX")
    private var nativeTempDir: File? = null

    fun init() {
        if (!initialized.compareAndSet(false, true)) return

        extractNativeLibraries()

        System.setProperty("prism.order", "sw")
        System.setProperty("prism.text", "t2k")

        try {
            Platform.startup {
                logger.info("JavaFX Platform started")
            }
            Platform.setImplicitExit(false)
        } catch (_: IllegalStateException) {
            logger.info("JavaFX Platform was already running")
        }
    }

    private fun extractNativeLibraries() {
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        val extension = if (isWindows) ".dll" else ".so"
        val prefix = if (isWindows) "" else "lib"

        val nativeNames = listOf(
            "glass", "prism_common", "prism_sw", "jfxwebkit", "jfxmedia",
            "prism_d3d", "prism_es2", "javafx_font", "javafx_font_freetype",
            "javafx_iio", "decora_sse", "glassgtk3"
        )

        val tempDir = Files.createTempDirectory("surfdisp-javafx").toFile()
        nativeTempDir = tempDir

        val classLoader = JavaFxPlatform::class.java.classLoader
        var extractedCount = 0

        for (name in nativeNames) {
            val resourceName = "$prefix$name$extension"
            val stream = classLoader.getResourceAsStream(resourceName) ?: continue
            val file = File(tempDir, resourceName)
            try {
                stream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                extractedCount++
            } catch (e: Exception) {
                logger.warning("Failed to extract native library $resourceName: ${e.message}")
            }
        }

        logger.info("Extracted $extractedCount JavaFX native libraries to $tempDir")

        val currentPath = System.getProperty("java.library.path", "")
        val separator = File.pathSeparator
        System.setProperty("java.library.path", tempDir.absolutePath + separator + currentPath)

        try {
            val sysPathsField = ClassLoader::class.java.getDeclaredField("sys_paths")
            sysPathsField.isAccessible = true
            sysPathsField.set(null, null)
        } catch (e: Exception) {
            logger.fine("Could not reset ClassLoader sys_paths: ${e.message}")
        }
    }

    fun shutdown() {
        if (initialized.get()) {
            Platform.exit()
            initialized.set(false)
        }
        nativeTempDir?.let { dir ->
            try {
                dir.listFiles()?.forEach { it.delete() }
                dir.delete()
            } catch (_: Exception) {
            }
        }
    }

    fun <T> runOnFxThread(block: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        if (Platform.isFxApplicationThread()) {
            try {
                future.complete(block())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        } else {
            Platform.runLater {
                try {
                    future.complete(block())
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }
        return future
    }
}
