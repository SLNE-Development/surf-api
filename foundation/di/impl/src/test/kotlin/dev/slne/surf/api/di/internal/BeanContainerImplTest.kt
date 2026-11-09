package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.BeanPostProcessor
import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.definition.BeanDefinition
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the container through a stub [BeanInstantiator], so ordering, resolution, scoping and
 * the lifecycle are tested without any class having to exist on a classpath.
 */
class BeanContainerImplTest {

    // --- test doubles -----------------------------------------------------------------------

    private class Probe(val label: String, val arguments: List<Any?> = emptyList())

    private class Phases(val label: String, private val log: MutableList<String>) : SurfComponent {
        override suspend fun load() { log += "load:$label" }
        override suspend fun enable() { log += "enable:$label" }
        override suspend fun disable() { log += "disable:$label" }
    }

    private class Observer(private val log: MutableList<String>) : BeanPostProcessor {
        override suspend fun afterInitialization(bean: Any, name: String): Any {
            log += "after:$name"
            return bean
        }

        override suspend fun beforeDestruction(bean: Any, name: String) {
            log += "before:$name"
        }
    }

    private class Replacer(private val replacement: Any) : BeanPostProcessor {
        override suspend fun afterInitialization(bean: Any, name: String) =
            if (bean is Probe && bean.label == "replaced") replacement else bean
    }

    /** The class name plus [Probe], so a test can look the bean up by type. */
    private fun probeTypes(className: String) = listOf(className, Probe::class.java.name)

    /** Builds an instantiator from a factory per provider id. */
    private fun instantiator(vararg factories: Pair<String, (List<Any?>) -> Any>): BeanInstantiator {
        val byId = factories.toMap()
        return BeanInstantiator { provider, arguments, _ ->
            val factory = byId[provider.id] ?: error("no test factory for ${provider.id}")
            factory(arguments)
        }
    }

    private fun container(
        definitions: List<BeanDefinition>,
        instantiator: BeanInstantiator,
        parent: BeanContainerImpl? = null,
        owner: Any = "test",
    ) = BeanContainerImpl(owner, definitions, parent, instantiator)

    // --- resolution -------------------------------------------------------------------------

    @Test
    fun `a bean is resolvable by a type it provides`() = runTest {
        val probe = Probe("only")
        val container = container(
            listOf(definition("Impl", provides = listOf("Impl", Probe::class.java.name))),
            instantiator("Impl" to { probe }),
        )

        container.start()

        assertSame(probe, container.bean(Probe::class))
    }

    @Test
    fun `a constructor parameter receives the bean it asked for`() = runTest {
        val dependency = Probe("dependency")
        val container = container(
            listOf(
                definition("Service", provides = probeTypes("Service"), needs = listOf(needs("Repository"))),
                definition("Repository", provides = probeTypes("Repository")),
            ),
            instantiator(
                "Repository" to { dependency },
                "Service" to { args -> Probe("service", args) },
            ),
        )

        container.start()

        val service = container.beansOfType(Probe::class).single { it.label == "service" }
        assertEquals(listOf(dependency), service.arguments)
    }

    @Test
    fun `a collection parameter receives every provider`() = runTest {
        val first = Probe("first")
        val second = Probe("second")
        val container = container(
            listOf(
                definition("Registrar", provides = probeTypes("Registrar"), needs = listOf(needsAll("Handler"))),
                definition("First", provides = listOf("First", "Handler"), priority = 0),
                definition("Second", provides = listOf("Second", "Handler"), priority = 1),
            ),
            instantiator(
                "First" to { first },
                "Second" to { second },
                "Registrar" to { args -> Probe("registrar", args) },
            ),
        )

        container.start()

        @Suppress("UNCHECKED_CAST")
        val handlers = container.beansOfType(Probe::class)
            .single { it.label == "registrar" }
            .arguments
            .single() as List<Any>

        assertEquals(listOf(first, second), handlers)
    }

    @Test
    fun `an optional parameter nothing provides becomes null`() = runTest {
        val container = container(
            listOf(definition("Service", provides = probeTypes("Service"), needs = listOf(needsOptional("Absent")))),
            instantiator("Service" to { args -> Probe("service", args) }),
        )

        container.start()

        assertNull(container.beansOfType(Probe::class).single().arguments.single())
    }

    @Test
    fun `a required parameter nothing provides fails and names both sides`() = runTest {
        val container = container(
            listOf(definition("Service", needs = listOf(needs("Absent")))),
            instantiator("Service" to { Probe("service") }),
        )

        val failure = assertFailsWith<UnsatisfiedDependencyException> { container.start() }

        assertTrue(failure.message!!.contains("Service"), failure.message)
        assertTrue(failure.message!!.contains("Absent"), failure.message)
    }

    @Test
    fun `a lazy parameter resolves on first access`() = runTest {
        val container = container(
            listOf(
                definition("Early", provides = probeTypes("Early"), needs = listOf(needsLazy("Late"))),
                definition("Late"),
            ),
            instantiator(
                "Late" to { Probe("late") },
                "Early" to { args -> Probe("early", args) },
            ),
        )

        container.start()

        val handle = container.beansOfType(Probe::class)
            .single { it.label == "early" }
            .arguments
            .single() as Lazy<*>

        assertEquals("late", (handle.value as Probe).label)
    }

    @Test
    fun `asking for a type nothing provides fails with the type named`() = runTest {
        val container = container(emptyList(), instantiator())
        container.start()

        val failure = assertFailsWith<IllegalArgumentException> { container.bean(Probe::class) }

        assertTrue(failure.message!!.contains(Probe::class.java.name), failure.message)
    }

    // --- scopes -----------------------------------------------------------------------------

    @Test
    fun `a singleton is one instance per container`() = runTest {
        val definitions = listOf(definition("Impl", provides = listOf("Impl", Probe::class.java.name)))
        var created = 0
        val instantiator = instantiator("Impl" to { Probe("probe-${created++}") })

        val container = container(definitions, instantiator)
        container.start()

        assertSame(container.bean(Probe::class), container.bean(Probe::class))
        assertEquals(1, created)
    }

    @Test
    fun `a prototype is a new instance per resolution`() = runTest {
        var created = 0
        val container = container(
            listOf(
                definition(
                    "Impl",
                    provides = listOf("Impl", Probe::class.java.name),
                    scope = Scope.PROTOTYPE,
                )
            ),
            instantiator("Impl" to { Probe("probe-${created++}") }),
        )

        container.start()

        assertNotSame(container.bean(Probe::class), container.bean(Probe::class))
    }

    @Test
    fun `a shared bean is the same instance in two sibling containers`() = runTest {
        val shared = definition(
            "Connection",
            provides = listOf("Connection", Probe::class.java.name),
            scope = Scope.SHARED,
        )
        var created = 0
        val factory = instantiator("Connection" to { Probe("connection-${created++}") })

        val root = container(emptyList(), factory, owner = "root")
        root.start()

        val first = container(listOf(shared), factory, parent = root, owner = "first")
        val second = container(listOf(shared), factory, parent = root, owner = "second")
        first.start()
        second.start()

        assertSame(first.bean(Probe::class), second.bean(Probe::class))
        assertEquals(1, created)
    }

    // --- hierarchy --------------------------------------------------------------------------

    @Test
    fun `a child resolves a bean from its parent`() = runTest {
        val platform = Probe("platform")
        val root = container(
            listOf(definition("Platform", provides = listOf("Platform", Probe::class.java.name))),
            instantiator("Platform" to { platform }),
            owner = "root",
        )
        root.start()

        val child = container(emptyList(), instantiator(), parent = root, owner = "child")
        child.start()

        assertSame(platform, child.bean(Probe::class))
    }

    @Test
    fun `a parent does not see its child's beans`() = runTest {
        val root = container(emptyList(), instantiator(), owner = "root")
        root.start()

        val child = container(
            listOf(definition("Local", provides = listOf("Local", Probe::class.java.name))),
            instantiator("Local" to { Probe("local") }),
            parent = root,
            owner = "child",
        )
        child.start()

        assertNull(root.beanOrNull(Probe::class))
    }

    @Test
    fun `siblings do not see each other's beans`() = runTest {
        val root = container(emptyList(), instantiator(), owner = "root")
        root.start()

        val first = container(
            listOf(definition("Local", provides = listOf("Local", Probe::class.java.name))),
            instantiator("Local" to { Probe("local") }),
            parent = root,
            owner = "first",
        )
        first.start()

        val second = container(emptyList(), instantiator(), parent = root, owner = "second")
        second.start()

        assertNull(second.beanOrNull(Probe::class))
    }

    // --- lifecycle --------------------------------------------------------------------------

    @Test
    fun `every load runs before any enable, and disable runs in reverse`() = runTest {
        val log = mutableListOf<String>()
        val container = container(
            listOf(
                definition("Top", needs = listOf(needs("Bottom")), lifecycle = true),
                definition("Bottom", lifecycle = true),
            ),
            instantiator(
                "Bottom" to { Phases("bottom", log) },
                "Top" to { Phases("top", log) },
            ),
        )

        container.start()
        container.stop { _, _ -> }

        assertEquals(
            listOf(
                "load:bottom", "load:top",
                "enable:bottom", "enable:top",
                "disable:top", "disable:bottom",
            ),
            log,
        )
    }

    @Test
    fun `a prototype takes no part in the lifecycle`() = runTest {
        val log = mutableListOf<String>()
        val container = container(
            listOf(definition("Transient", scope = Scope.PROTOTYPE, lifecycle = true)),
            instantiator("Transient" to { Phases("transient", log) }),
        )

        container.start()
        container.stop { _, _ -> }

        assertTrue(log.isEmpty(), log.toString())
    }

    @Test
    fun `a failing disable is reported and does not stop the rest`() = runTest {
        val log = mutableListOf<String>()
        val failing = object : SurfComponent {
            override suspend fun disable() = error("boom")
        }
        val reported = mutableListOf<String>()

        val container = container(
            listOf(
                definition("Failing", lifecycle = true, priority = 0),
                definition("Fine", lifecycle = true, priority = 1),
            ),
            instantiator(
                "Failing" to { failing },
                "Fine" to { Phases("fine", log) },
            ),
        )

        container.start()
        container.stop { id, _ -> reported += id }

        assertEquals(1, reported.size)
        assertTrue(log.contains("disable:fine"), log.toString())
    }

    @Test
    fun `starting twice fails`() = runTest {
        val container = container(emptyList(), instantiator())
        container.start()

        assertFailsWith<IllegalStateException> { container.start() }
    }

    // --- post processors --------------------------------------------------------------------

    @Test
    fun `a post processor sees every bean but not itself`() = runTest {
        val log = mutableListOf<String>()
        val container = container(
            listOf(
                definition("Observer", postProcessor = true),
                definition("First"),
                definition("Second"),
            ),
            instantiator(
                "Observer" to { Observer(log) },
                "First" to { Probe("first") },
                "Second" to { Probe("second") },
            ),
        )

        container.start()

        assertEquals(listOf("after:First", "after:Second"), log)
    }

    @Test
    fun `a post processor can replace a bean and the replacement is what is resolved`() = runTest {
        val replacement = Probe("replacement")
        val container = container(
            listOf(
                definition("Replacer", postProcessor = true),
                definition("Target", provides = listOf("Target", Probe::class.java.name)),
            ),
            instantiator(
                "Replacer" to { Replacer(replacement) },
                "Target" to { Probe("replaced") },
            ),
        )

        container.start()

        assertSame(replacement, container.bean(Probe::class))
    }

    @Test
    fun `beforeDestruction runs for every bean in reverse order`() = runTest {
        val log = mutableListOf<String>()
        val container = container(
            listOf(
                definition("Observer", postProcessor = true),
                definition("First", priority = 0),
                definition("Second", priority = 1),
            ),
            instantiator(
                "Observer" to { Observer(log) },
                "First" to { Probe("first") },
                "Second" to { Probe("second") },
            ),
        )

        container.start()
        log.clear()
        container.stop { _, _ -> }

        assertEquals(listOf("before:Second", "before:First"), log)
    }

    // --- factories --------------------------------------------------------------------------

    @Test
    fun `a factory publishes its return type and is created after its declaring bean`() = runTest {
        val published = Probe("published")
        val order = mutableListOf<String>()
        val container = container(
            listOf(
                definition(
                    "Connection",
                    factories = listOf(factory("api", Probe::class.java.name)),
                )
            ),
            BeanInstantiator { provider, _, declaring ->
                order += provider.id
                when (provider) {
                    is ClassProvider -> Probe("connection")
                    is FactoryProvider -> published.also { checkNotNull(declaring) }
                }
            },
        )

        container.start()

        assertSame(published, container.bean(Probe::class))
        assertEquals(listOf("Connection", "Connection#api"), order)
    }
}
