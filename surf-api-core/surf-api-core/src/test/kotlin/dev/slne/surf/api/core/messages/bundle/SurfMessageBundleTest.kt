package dev.slne.surf.api.core.messages.bundle

import java.net.URLClassLoader
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class SurfMessageBundleTest {
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `load copies missing root and localized bundles into their package directory`() {
        val classpath = createTempDirectory("surf-bundle-classpath")
        val dataFolder = createTempDirectory("surf-bundle-data")

        try {
            val bundledFile = classpath.resolve("messages/example.properties")
            bundledFile.parent.createDirectories()
            bundledFile.writeText("example.key=Example value\n")
            classpath.resolve("messages/example_de.properties")
                .writeText("example.key=Beispielwert\n")

            URLClassLoader(arrayOf(classpath.toUri().toURL()), null).use { classLoader ->
                SurfMessageBundle(
                    bundleClazz = javaClass,
                    pathToBundle = "messages.example",
                    dataFolder = dataFolder,
                    classLoader = classLoader,
                ).load()
            }

            assertTrue(dataFolder.resolve("messages/example.properties").exists())
            assertTrue(dataFolder.resolve("messages/example_de.properties").exists())
        } finally {
            classpath.deleteRecursively()
            dataFolder.deleteRecursively()
        }
    }
}
