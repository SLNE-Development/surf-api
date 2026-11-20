package dev.slne.surf.api.standalone

import dev.slne.surf.api.config.SurfConfigApi
import dev.slne.surf.api.reflection.SurfReflection
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfContexts
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Checks that a standalone process starts surf-api's root context and resolves its beans.
 *
 * The test goes through the published entry point rather than building a context by hand: building
 * one would prove that Spring works, and say nothing about whether [SurfApiStandaloneBootstrap]
 * starts it.
 */
@OptIn(InternalSurfApi::class)
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
        assertNotNull(SurfReflection.instance, "a core-impl bean resolves")
        assertNotNull(SurfConfigApi.instance, "a config bean resolves")
        assertSame(
            root.getBean(SurfReflection::class.java),
            SurfReflection.instance,
            "the accessor hands out the root's singleton rather than a second instance",
        )
    }

    @Test
    fun `shutdown closes the root context so the process can start again`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.shutdown()

        assertNull(SurfContexts.rootOrNull, "shutdown has to uninstall the root context")
    }
}
