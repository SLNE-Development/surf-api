package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.context.fixture.Journal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.metrics.ApplicationStartup
import org.springframework.core.metrics.StartupStep
import org.springframework.core.metrics.jfr.FlightRecorderApplicationStartup
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class ContextStartupTest {

    private val startup = RecordingStartup()
    private val rootScope = CoroutineScope(SupervisorJob())
    private val root = AnnotationConfigApplicationContext().apply {
        registerCoroutineScope(this, rootScope, primary = false)
        refresh()
    }
    private val manager = SurfContextManager(root) { false }
    private val pluginLoader = URLClassLoader(
        "startup-test-loader",
        emptyArray(),
        javaClass.classLoader,
    )

    /**
     * A loader that sees no classes, so a root context built on it scans no components.
     */
    private val emptyLoader = URLClassLoader("empty-root-loader", emptyArray(), null)

    @BeforeEach
    fun installStartup() {
        Journal.clear()
        ContextStartup.override = startup
    }

    @AfterEach
    fun closeContexts() {
        ContextStartup.override = null
        manager.closeAll()
        root.close()
        rootScope.cancel()
        pluginLoader.close()
        emptyLoader.close()
    }

    @Test
    fun `a context build is one step around scan and refresh, tagged with the display name`() {
        val plugin = start("provider")

        val build = startup.single(StartupPhase.BUILD.stepName)
        val refreshes = startup.named("spring.context.refresh")

        assertSame(startup, plugin.context.applicationStartup)
        assertEquals(mapOf(DISPLAY_NAME_TAG to "surf plugin context of 'provider'"), build.tags)
        assertEquals(listOf(null, build.getId()), refreshes.map { it.getParentId() })
        assertTrue(build.ended)
    }

    @Test
    fun `every plugin phase is a step tagged with the plugin name`() = runTest {
        val plugin = start("provider")

        plugin.load()
        plugin.enable()
        plugin.disable()

        val phases = listOf(
            StartupPhase.BOOTSTRAP,
            StartupPhase.LOAD,
            StartupPhase.ENABLE,
            StartupPhase.DISABLE,
        )

        for (phase in phases) {
            val step = startup.single(phase.stepName)
            assertEquals(mapOf(PLUGIN_TAG to "provider"), step.tags, phase.name)
            assertTrue(step.ended, phase.name)
        }
    }

    @Test
    fun `a bootstrapped plugin records one bootstrap step and builds two contexts`() = runTest {
        val bootstrap = manager.bootstrap(descriptor("deferred"))
        bootstrap.bootstrap()
        val plugin = start("deferred")

        plugin.load()
        plugin.enable()

        val bootstrapStep = startup.single(StartupPhase.BOOTSTRAP.stepName)
        val builtContexts = startup.named(StartupPhase.BUILD.stepName).map {
            it.tags[DISPLAY_NAME_TAG]
        }
        val phases = plugin.startupTimings
            .flatMap { it.snapshot().keys }
            .toSet()

        assertEquals(mapOf(PLUGIN_TAG to "deferred"), bootstrapStep.tags)
        assertEquals(
            listOf("surf bootstrap context of 'deferred'", "surf plugin context of 'deferred'"),
            builtContexts,
        )
        assertEquals(
            setOf(
                StartupPhase.BUILD,
                StartupPhase.BOOTSTRAP,
                StartupPhase.LOAD,
                StartupPhase.ENABLE,
            ),
            phases,
        )
    }

    @Test
    fun `a root context records its build and every phase tagged with surf-api`() = runTest {
        val rootContext = SurfRootContext.create(emptyLoader, CoroutineScope(SupervisorJob()))

        try {
            rootContext.load()
            rootContext.enable()
            rootContext.disable()
        } finally {
            rootContext.close()
        }

        val build = startup.single(StartupPhase.BUILD.stepName)
        val phases = listOf(
            StartupPhase.BOOTSTRAP,
            StartupPhase.LOAD,
            StartupPhase.ENABLE,
            StartupPhase.DISABLE,
        )

        assertEquals(mapOf(DISPLAY_NAME_TAG to "surf root context"), build.tags)

        for (phase in phases) {
            val step = startup.single(phase.stepName)
            assertEquals(mapOf(PLUGIN_TAG to "surf-api"), step.tags, phase.name)
            assertTrue(step.ended, phase.name)
        }
    }

    @Test
    fun `an adopted context keeps the startup its creator installed`() {
        val ownStartup = RecordingStartup()
        val adopted = AnnotationConfigApplicationContext()
        adopted.applicationStartup = ownStartup

        adopt(adopted)

        assertSame(ownStartup, adopted.applicationStartup)
    }

    @Test
    fun `an adopted context without a startup of its own gets surf-api's`() {
        val adopted = AnnotationConfigApplicationContext()

        adopt(adopted)

        assertSame(startup, adopted.applicationStartup)
    }

    @Test
    fun `a failing phase still ends its step and records its duration`() {
        val timings = PhaseTimings()

        assertFailsWith<IllegalStateException> {
            startup.measure(StartupPhase.LOAD, PLUGIN_TAG, "failing", timings) {
                error("load fails on purpose")
            }
        }

        assertTrue(startup.single(StartupPhase.LOAD.stepName).ended)
        assertTrue(StartupPhase.LOAD in timings.snapshot())
    }

    @Test
    fun `the jfr property selects the flight recorder startup`() {
        ContextStartup.override = null
        val previous = System.getProperty(ContextStartup.JFR_PROPERTY)

        try {
            System.clearProperty(ContextStartup.JFR_PROPERTY)
            assertSame(ApplicationStartup.DEFAULT, ContextStartup.current())

            System.setProperty(ContextStartup.JFR_PROPERTY, "true")
            assertIs<FlightRecorderApplicationStartup>(ContextStartup.current())
            assertSame(ContextStartup.current(), start("provider").context.applicationStartup)
        } finally {
            if (previous == null) {
                System.clearProperty(ContextStartup.JFR_PROPERTY)
            } else {
                System.setProperty(ContextStartup.JFR_PROPERTY, previous)
            }
        }
    }

    @Test
    fun `the startup line sums the phases of all contexts in phase order`() {
        val bootstrapTimings = PhaseTimings().apply {
            add(StartupPhase.BOOTSTRAP, 2.milliseconds)
            add(StartupPhase.BUILD, 5.milliseconds)
        }
        val ownTimings = PhaseTimings().apply {
            add(StartupPhase.ENABLE, 4.milliseconds)
            add(StartupPhase.BUILD, 7.milliseconds)
            add(StartupPhase.LOAD, 3.milliseconds)
        }

        assertEquals(
            "Plugin 'shop' started: build 12 ms, bootstrap 2 ms, load 3 ms, enable 4 ms",
            describeStartup("Plugin 'shop'", bootstrapTimings, ownTimings),
        )
    }

    private fun adopt(context: AnnotationConfigApplicationContext) {
        val scope = CoroutineScope(SupervisorJob())
        val application = PluginDescriptor(
            name = "adopted",
            classLoader = emptyLoader,
            source = testClasses,
            basePackages = listOf("dev.slne.adopted"),
        )

        try {
            SurfRootContext.prepare(context, emptyLoader, scope, application)
        } finally {
            scope.cancel()
        }
    }

    private fun start(fixture: String) = manager.start(
        descriptor(fixture),
        surfCoroutineScope(fixture, pluginLoader, Dispatchers.Unconfined),
    )

    private fun descriptor(fixture: String) = PluginDescriptor(
        name = fixture,
        classLoader = pluginLoader,
        source = testClasses,
        basePackages = listOf("$FIXTURE_PACKAGE.$fixture"),
    )

    /**
     * Records every step with its parent, the innermost step still open when it started.
     */
    private class RecordingStartup : ApplicationStartup {
        private val steps = mutableListOf<RecordedStep>()
        private val open = ArrayDeque<RecordedStep>()

        @Synchronized
        override fun start(name: String): StartupStep {
            val step = RecordedStep(steps.size.toLong() + 1, name, open.lastOrNull()?.id, this)
            steps += step
            open.addLast(step)

            return step
        }

        @Synchronized
        fun ended(step: RecordedStep) {
            open.remove(step)
        }

        @Synchronized
        fun named(name: String): List<RecordedStep> = steps.filter { it.name == name }

        fun single(name: String): RecordedStep = named(name).single()
    }

    private class RecordedStep(
        private val stepId: Long,
        private val stepName: String,
        private val parent: Long?,
        private val owner: RecordingStartup,
    ) : StartupStep {
        val tags = LinkedHashMap<String, String>()
        var ended = false
            private set

        override fun getName() = stepName
        override fun getId() = stepId
        override fun getParentId() = parent

        override fun getTags() = StartupStep.Tags {
            tags.mapTo(mutableListOf<StartupStep.Tag>()) { (key, value) -> RecordedTag(key, value) }
                .iterator()
        }

        override fun tag(key: String, value: String): StartupStep = apply { tags[key] = value }
        override fun tag(key: String, value: Supplier<String>): StartupStep = tag(key, value.get())

        override fun end() {
            ended = true
            owner.ended(this)
        }
    }

    private class RecordedTag(
        private val key: String,
        private val value: String,
    ) : StartupStep.Tag {
        override fun getKey() = key
        override fun getValue() = value
    }

    private companion object {
        const val FIXTURE_PACKAGE = "dev.slne.surf.api.spring.context.fixture"

        val testClasses: Path = Path.of(
            Journal::class.java.protectionDomain.codeSource.location.toURI(),
        )
    }
}
