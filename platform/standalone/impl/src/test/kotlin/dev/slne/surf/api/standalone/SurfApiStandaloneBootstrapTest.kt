package dev.slne.surf.api.standalone

import dev.slne.surf.api.config.ConfigHandles
import dev.slne.surf.api.config.serializer.ConfigSerializerCollections
import dev.slne.surf.api.config.type.ConfigDuration
import dev.slne.surf.api.event.SurfEventBus
import dev.slne.surf.api.spring.SurfContexts
import kotlinx.coroutines.test.runTest
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Checks that a standalone process starts surf-api's root context and resolves its beans.
 *
 * The test goes through the published entry point rather than building a context by hand: building
 * one would prove that Spring works, and say nothing about whether [SurfApiStandaloneBootstrap]
 * starts it.
 */
class SurfApiStandaloneBootstrapTest {

    @AfterTest
    fun shutDown() = runTest {
        // Process-wide state: a left-over root context would decide the next test's answer, and
        // shutdown is idempotent, so calling it after a failed start is safe.
        SurfApiStandaloneBootstrap.shutdown()
    }

    @Test
    fun `bootstrap starts the root context and its beans resolve`() = runTest {
        assertNull(SurfContexts.rootOrNull, "nothing has started yet")

        SurfApiStandaloneBootstrap.bootstrap()

        val root = assertNotNull(SurfContexts.rootOrNull, "bootstrap has to start the root context")

        // Two services from different modules, read the way an api module reads them: through the
        // accessor, not through the context.
        assertNotNull(SurfEventBus.instance, "a core-impl bean resolves")
        assertNotNull(ConfigSerializerCollections.instance, "a config bean resolves")
        assertSame(
            root.getBean(SurfEventBus::class.java),
            SurfEventBus.instance,
            "the accessor hands out the root's singleton rather than a second instance",
        )
    }

    @Test
    fun `shutdown closes the root context so the process can start again`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.shutdown()

        assertNull(SurfContexts.rootOrNull, "shutdown has to uninstall the root context")
    }

    @Test
    fun `a config file opened in the runtime uses the root default serializers`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap()
        val file = createTempDirectory("surf-config").resolve("ids.yml")

        val ids = ConfigHandles.open<Ids>(file).use { handle -> handle.reload() }

        assertEquals(Ids(), ids)
        assertContains(file.readText(), "id: ${Ids().id}")
        assertContains(file.readText(), "timeout: 30s")
    }

    @ConfigSerializable
    data class Ids(
        val id: UUID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"),
        val timeout: ConfigDuration = ConfigDuration(30.seconds),
    )
}
