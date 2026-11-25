package dev.slne.surf.api.standalone

import dev.slne.standalone.fixture.ApplicationEvents
import dev.slne.standalone.fixture.ApplicationService
import dev.slne.standalone.fixture.FixtureEvent
import dev.slne.standalone.fixture.FixtureEventPublisher
import dev.slne.standalone.samename.SAME_NAME_ID
import dev.slne.standalone.samename.SameNameConfig
import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.event.SurfEventBus
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.PluginDescriptor
import kotlinx.coroutines.test.runTest
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContextException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.*

/**
 * Checks that an application passed to [SurfApiStandaloneBootstrap.bootstrap] lives in surf-api's
 * root context and gets what a plugin context gets.
 */
class StandaloneApplicationTest {

    private val dataDirectory = createTempDirectory("surf-standalone-application")

    @BeforeTest
    fun clearEvents() {
        ApplicationEvents.recorded.clear()
        ApplicationEvents.handled.clear()
    }

    @AfterTest
    fun shutDown() = runTest {
        SurfApiStandaloneBootstrap.shutdown()
    }

    @Test
    fun `the application gets contributions, config handles and phase callbacks`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap(application(listOf(FIXTURE_PACKAGE)))
        SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)

        val service = SurfContexts.root.getBean<ApplicationService>()
        assertEquals("hello", service.config.value.greeting)
        assertTrue(
            dataDirectory.resolve("application.yml").exists(),
            "the handle writes its defaults",
        )

        SurfApiStandaloneBootstrap.shutdown()

        assertEquals(
            listOf(
                "contribute:application",
                "onEnable",
                "afterEnable",
                "beforeDisable",
                "onDisable",
            ),
            ApplicationEvents.recorded,
        )
    }

    @Test
    fun `the application's event handler beans receive events from the enable hooks until shutdown`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap(application(listOf(FIXTURE_PACKAGE)))
        SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)

        FixtureEvent("enabled").call()
        SurfApiStandaloneBootstrap.shutdown()
        FixtureEvent("shut down").call()

        assertEquals(
            listOf(FixtureEventPublisher.ON_ENABLE_SOURCE, "enabled"),
            ApplicationEvents.handled,
        )
    }

    @Test
    fun `after a second bootstrap the event handler beans listen on the new root's event bus`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap(application(listOf(FIXTURE_PACKAGE)))
        SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)
        SurfApiStandaloneBootstrap.shutdown()
        ApplicationEvents.handled.clear()

        SurfApiStandaloneBootstrap.bootstrap(application(listOf(FIXTURE_PACKAGE)))
        SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)

        SurfContexts.root.getBean<SurfEventBus>().callSync(FixtureEvent("injected bus"))
        FixtureEvent("call").call()

        assertEquals(
            listOf(FixtureEventPublisher.ON_ENABLE_SOURCE, "injected bus", "call"),
            ApplicationEvents.handled,
        )
    }

    @Test
    fun `the event handler beans of an adopted context are registered once while it runs`() {
        val context = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(context, application(listOf(FIXTURE_PACKAGE)))
        context.scan(FIXTURE_PACKAGE)
        context.refresh()

        FixtureEvent("refreshed").call()
        context.close()
        FixtureEvent("closed").call()

        assertEquals(
            listOf(FixtureEventPublisher.ON_ENABLE_SOURCE, "refreshed"),
            ApplicationEvents.handled,
        )
    }

    @Test
    fun `an application serializer may share its simple name with a root default`() = runTest {
        SurfApiStandaloneBootstrap.bootstrap(application(listOf(SAME_NAME_PACKAGE)))
        SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)

        val handle = SurfContexts.root.getBean<ConfigHandle<SameNameConfig>>()

        assertEquals(SAME_NAME_ID, handle.value.id)
        assertContains(
            dataDirectory.resolve("same-name.yml").readText(),
            SAME_NAME_ID.toString().uppercase(),
        )
    }

    @Test
    fun `an application inside surf-api's package is rejected`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            SurfApiStandaloneBootstrap.bootstrap(
                application(listOf("dev.slne.surf.api.standalone")),
            )
        }

        assertNull(SurfContexts.rootOrNull, "a failed start leaves no root context installed")
    }

    @Test
    fun `an adopted context runs the hooks of the components its creator scanned`() {
        val context = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(context, application(listOf(FIXTURE_PACKAGE)))
        context.scan(FIXTURE_PACKAGE)
        context.refresh()

        assertEquals("hello", context.getBean<ApplicationService>().config.value.greeting)
        assertSame(context, SurfContexts.rootOrNull)

        context.close()

        assertEquals(
            listOf(
                "contribute:application",
                "onEnable",
                "afterEnable",
                "beforeDisable",
                "onDisable",
            ),
            ApplicationEvents.recorded,
        )
        assertNull(SurfContexts.rootOrNull, "stopping the context shuts surf-api down")
    }

    @Test
    fun `a failing start of an adopted context shuts surf-api down`() {
        val context = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(context, application(listOf(FAILING_PACKAGE)))
        context.scan(FAILING_PACKAGE)

        assertFails { context.refresh() }

        assertNull(SurfContexts.rootOrNull)
    }

    @Test
    fun `a refresh that fails before the lifecycle starts releases surf-api and allows a second adoption`() {
        val failing = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(failing, application(listOf(BROKEN_PACKAGE)))
        failing.scan(BROKEN_PACKAGE)

        assertFails { failing.refresh() }

        assertNull(SurfContexts.rootOrNull, "the failed refresh uninstalls the root context")

        val context = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(context, application(listOf(FIXTURE_PACKAGE)))
        context.scan(FIXTURE_PACKAGE)
        context.refresh()

        assertSame(context, SurfContexts.rootOrNull)

        context.close()

        assertNull(SurfContexts.rootOrNull)
    }

    @Test
    fun `shutdown releases an adopted context that never refreshed`() = runTest {
        val context = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(context, application(listOf(FIXTURE_PACKAGE)))

        SurfApiStandaloneBootstrap.shutdown()

        assertNull(SurfContexts.rootOrNull)
        SurfApiStandaloneBootstrap.adopt(
            AnnotationConfigApplicationContext(),
            application(listOf(FIXTURE_PACKAGE)),
        )
    }

    @Test
    fun `an adopted context cannot be started again after it stopped`() {
        val context = AnnotationConfigApplicationContext()
        SurfApiStandaloneBootstrap.adopt(context, application(listOf(FIXTURE_PACKAGE)))
        context.scan(FIXTURE_PACKAGE)
        context.refresh()

        context.stop()
        val failure = assertFailsWith<ApplicationContextException> { context.start() }

        assertTrue(
            generateSequence<Throwable>(failure) { it.cause }
                .any { "cannot be restarted" in it.message.orEmpty() },
        )
        context.close()
    }

    private fun application(basePackages: List<String>) = PluginDescriptor(
        name = "application",
        classLoader = javaClass.classLoader,
        source = null,
        basePackages = basePackages,
        dataDirectory = dataDirectory,
    )

    private companion object {
        const val FIXTURE_PACKAGE = "dev.slne.standalone.fixture"
        const val FAILING_PACKAGE = "dev.slne.standalone.failing"
        const val BROKEN_PACKAGE = "dev.slne.standalone.broken"
        const val SAME_NAME_PACKAGE = "dev.slne.standalone.samename"
    }
}
