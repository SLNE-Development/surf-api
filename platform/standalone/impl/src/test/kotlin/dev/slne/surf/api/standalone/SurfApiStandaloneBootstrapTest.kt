package dev.slne.surf.api.standalone

import dev.slne.surf.api.core.config.SurfConfigApi
import dev.slne.surf.api.core.reflection.SurfReflection
import dev.slne.surf.api.di.SurfContainer
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Checks that a standalone process can actually resolve a bean.
 *
 * This is the case nothing covered, and the gap was not theoretical: every surf-api service is a
 * bean now, so a platform that does not start the root container answers every single lookup with
 * a failure. Paper and Minestom are exercised by their own server tests; a standalone process had
 * no test at all, which is precisely why it was the platform left behind.
 *
 * The test therefore goes through the published entry point rather than building a container by
 * hand. Building one would prove that the container works, which other tests already do, and would
 * say nothing about whether [SurfApiStandaloneBootstrap] starts it.
 */
@OptIn(InternalSurfApi::class)
class SurfApiStandaloneBootstrapTest {

    @AfterTest
    fun shutDown() = runTest {
        // Process-wide state: a left-over container would decide the next test's answer, and
        // shutdown is idempotent, so calling it after a failed start is safe.
        SurfApiStandaloneBootstrap.shutdown()
    }

    @Test
    fun `bootstrap starts the root container and its beans resolve`() = runTest {
        assertNull(SurfContainer.rootOrNull, "nothing has started yet")

        SurfApiStandaloneBootstrap.bootstrap()

        val root = assertNotNull(SurfContainer.rootOrNull, "bootstrap has to start the root")

        // Two services from different modules, read the way an api module reads them: through the
        // accessor, not through the container. That is the path that was broken.
        assertNotNull(SurfReflection.INSTANCE, "a core-impl bean resolves")
        assertNotNull(SurfConfigApi.instance, "a config bean resolves")
        assertSame(
            root.bean(SurfReflection::class),
            SurfReflection.INSTANCE,
            "the accessor hands out the root's singleton rather than a second instance",
        )
    }

    @Test
    fun `shutdown stops the container so the process can start again`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.shutdown()

        assertNull(SurfContainer.rootOrNull, "shutdown has to uninstall the host")
    }
}
