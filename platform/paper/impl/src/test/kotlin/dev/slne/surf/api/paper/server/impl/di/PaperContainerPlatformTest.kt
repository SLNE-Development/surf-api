package dev.slne.surf.api.paper.server.impl.di

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import io.mockk.mockk
import org.bukkit.plugin.java.JavaPlugin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks that the Paper adapter answers the container without asking the container.
 *
 * This is the invariant [dev.slne.surf.api.di.ContainerPlatform] states, and it was broken: the
 * adapter read a plugin's classloader through surf-api's own reflection *service*, which became a
 * bean when the service loader went. `beanSources` and `classLoader` are called while the root
 * container is being built, before a single bean exists, so the server stopped starting — and
 * nothing noticed, because Paper's startup has no test.
 *
 * A real `JavaPlugin` cannot be made in a unit test and a mock cannot answer `getClassLoader`
 * usefully, so the assertion is not on the return value: it is that whatever goes wrong, it did
 * not go wrong *inside the container*. Before the fix the trace named `SurfContainer`; now it
 * cannot, because the adapter never reaches it.
 */
@OptIn(InternalSurfApi::class)
class PaperContainerPlatformTest {

    @Test
    fun `answering a platform question never reaches the container`() {
        // Deliberately nothing installed: if an answer resolved a bean, SurfContainer would be in
        // the trace, which is exactly what this forbids.
        val platform = PaperContainerPlatform()
        val plugin = mockk<JavaPlugin>(relaxed = true)

        val traces = listOf(
            "classLoader" to runCatching { platform.classLoader(plugin) },
            "beanSources" to runCatching { platform.beanSources(plugin) },
            "dataPath" to runCatching { platform.dataPath(plugin) },
            "logger" to runCatching { platform.logger(plugin) },
            "environments" to runCatching { platform.environments() },
        )

        traces.forEach { (member, outcome) ->
            val trace = outcome.exceptionOrNull()?.stackTraceToString().orEmpty()

            assertFalse(
                trace.contains("SurfContainer"),
                "$member went through the container, which cannot answer while it is being " +
                        "built:\n$trace",
            )
        }
    }

    @Test
    fun `surf-api's own descriptor keeps its classloader closed`() {
        // The root container is handed surf-api's own loader, and the only reason it can hold
        // nothing but surf-api's beans is that this loader cannot reach a downstream plugin's
        // classes. That is bought by one line in a *generated* descriptor, set from
        // platform/paper/impl/build.gradle.kts -- so it is worth asserting rather than assuming.
        val descriptor = checkNotNull(javaClass.getResource("/paper-plugin.yml")) {
            "surf-api's own paper-plugin.yml is not on the test classpath"
        }.readText()

        assertTrue(
            descriptor.lineSequence().any { it.trim() == "has-open-classloader: false" },
            "the root container's isolation depends on this line:\n$descriptor",
        )
    }

    @Test
    fun `an owner that is not a plugin is refused rather than given surf-api's own answers`() {
        val platform = PaperContainerPlatform()
        val failure = runCatching { platform.classLoader("not a plugin") }.exceptionOrNull()

        // Every answer on this platform comes from a plugin, so an owner that is not one would
        // otherwise quietly share whichever container's beans the fallback belonged to.
        assertTrue(failure is IllegalStateException, "expected a refusal, got $failure")
        assertTrue(
            failure.message.orEmpty().contains("JavaPlugin"),
            "the message has to say what was expected: ${failure.message}",
        )
    }
}
