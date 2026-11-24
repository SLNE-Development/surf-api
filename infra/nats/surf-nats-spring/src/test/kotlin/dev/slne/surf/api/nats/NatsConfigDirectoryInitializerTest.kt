package dev.slne.surf.api.nats

import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.SurfProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

/**
 * NATS reads its directory once per JVM, so the tests that install share one.
 */
class NatsConfigDirectoryInitializerTest {

    @AfterEach
    fun uninstallRoot() = SurfContexts.uninstall()

    @Test
    fun `contexts that are not the root context leave the NATS directory alone`() {
        val first = contextWithDirectory(directories.resolve("first"))
        val second = contextWithDirectory(directories.resolve("second"))

        NatsConfigDirectoryInitializer().initialize(first)
        NatsConfigDirectoryInitializer().initialize(second)
        first.refresh()
        second.refresh()
    }

    @Test
    fun `the root context installs the directory it publishes`() {
        val root = contextWithDirectory(sharedDirectory)
        SurfContexts.install(root)

        NatsConfigDirectoryInitializer().initialize(root)
        root.refresh()

        assertInstalled(sharedDirectory)
    }

    @Test
    fun `a context that becomes the root context after the initializer ran installs its directory`() {
        val root = contextWithDirectory(sharedDirectory)

        NatsConfigDirectoryInitializer().initialize(root)
        SurfContexts.install(root)
        root.refresh()

        assertInstalled(sharedDirectory)
    }

    private fun assertInstalled(directory: Path) {
        val failure = assertFailsWith<IllegalStateException> {
            NatsPlatform.install(
                directories.resolve(
                    "other",
                ),
            )
        }
        assertContains(failure.message.orEmpty(), directory.toString())
    }

    private fun contextWithDirectory(directory: Path) = AnnotationConfigApplicationContext().apply {
        environment.propertySources.addFirst(
            MapPropertySource(
                "test",
                mapOf(SurfProperties.CONFIG_DIRECTORY to directory.toString()),
            ),
        )
    }

    companion object {

        @TempDir
        @JvmStatic
        lateinit var directories: Path

        private val sharedDirectory get() = directories.resolve("root")
    }
}
