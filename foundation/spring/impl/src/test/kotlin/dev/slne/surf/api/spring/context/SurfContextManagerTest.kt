package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.PluginPresence
import dev.slne.surf.api.spring.context.fixture.Greeting
import dev.slne.surf.api.spring.context.fixture.Journal
import dev.slne.surf.api.spring.context.fixture.ambiguousoptional.OptionalGreetingUser
import dev.slne.surf.api.spring.context.fixture.conditional.OptionalUser
import dev.slne.surf.api.spring.context.fixture.consumer.GreetingConsumer
import dev.slne.surf.api.spring.context.fixture.contributed.Contributed
import dev.slne.surf.api.spring.context.fixture.contributed.ContributedSettings
import dev.slne.surf.api.spring.context.fixture.lazyconsumer.LazyGreetingUser
import dev.slne.surf.api.spring.context.fixture.lazyprovider.LazyGreeting
import dev.slne.surf.api.spring.context.fixture.provider.ProviderGreeting
import dev.slne.surf.api.spring.context.fixture.qualified.QualifiedConsumer
import dev.slne.surf.api.spring.context.fixture.scope.ScopeUser
import dev.slne.surf.api.spring.context.fixture.transitive.TransitiveConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactoryUtils
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SurfContextManagerTest {

    private val rootScope = CoroutineScope(SupervisorJob())
    private val contributions = mutableListOf<Pair<String, List<Class<*>>>>()
    private val root = AnnotationConfigApplicationContext().apply {
        registerCoroutineScope(this, rootScope, primary = false)
        beanFactory.registerSingleton("testContributor", ContextContributor { context, plugin ->
            contributions += plugin.name to findAnnotatedClasses(context, plugin, Contributed::class.java)
        })
        refresh()
    }
    private val manager = SurfContextManager(root) { name -> name == "Present" }
    private val pluginLoader = URLClassLoader(PLUGIN_LOADER_NAME, emptyArray(), javaClass.classLoader)

    @BeforeEach
    fun clearJournal() = Journal.clear()

    @AfterEach
    fun closeContexts() {
        manager.closeAll()
        root.close()
        rootScope.cancel()
        pluginLoader.close()
    }

    @Test
    fun `a consumer injects its dependency's exports by interface and generic type`() {
        val provider = start("provider", "provider")
        val consumer = start("consumer", "consumer", dependencies = listOf("provider"))

        val bean = consumer.context.getBean(GreetingConsumer::class.java)

        assertSame(provider.context.getBean(ProviderGreeting::class.java), bean.greeting)
        assertEquals("apples", bean.apples.describe())
        assertEquals("pears", bean.pears.describe())
    }

    @Test
    fun `a bean without an exported interface stays private to its plugin`() {
        start("provider", "provider")
        val consumer = start("consumer", "consumer", dependencies = listOf("provider"))

        assertNull(consumer.context.getBean(GreetingConsumer::class.java).helper.ifAvailable)
    }

    @Test
    fun `an export is initialized and destroyed once, by its provider`() {
        start("provider", "provider")
        start("consumer", "consumer", dependencies = listOf("provider"))

        manager.close("consumer")
        assertEquals(listOf("provider.init"), Journal.entries)

        manager.close("provider")
        assertEquals(listOf("provider.init", "provider.destroy"), Journal.entries)
    }

    @Test
    fun `exports of a dependency's dependency are not visible`() {
        start("provider", "provider")
        start("consumer", "consumer", dependencies = listOf("provider"))
        val transitive = start("transitive", "transitive", dependencies = listOf("consumer"))

        assertNull(transitive.context.getBean(TransitiveConsumer::class.java).greeting.ifAvailable)
    }

    @Test
    fun `two providers of one type are ambiguous even if one is primary and a name matches`() {
        start("provider", "provider")
        start("second", "second")

        val failure = assertFailsWith<PluginContextException> {
            start("ambiguous", "ambiguous", dependencies = listOf("provider", "second"))
        }

        assertTrue(failure.hasCause<NoUniqueBeanDefinitionException>(), failure.stackTraceToString())
        assertNull(manager.find("ambiguous"))
    }

    @Test
    fun `a qualifier picks one of two providers`() {
        start("provider", "provider")
        start("second", "second")
        val consumer = start("qualified", "qualified", dependencies = listOf("provider", "second"))

        assertEquals("hello from second", consumer.context.getBean(QualifiedConsumer::class.java).greeting.greet())
    }

    @Test
    fun `an export implementing Lifecycle fails its provider`() {
        assertFailsWith<PluginContextException> { start("badexport", "badexport") }
        assertNull(manager.find("badexport"))
    }

    @Test
    fun `lifecycle hooks follow initialization order, Order wins, disable runs in reverse`() = runTest {
        val plugin = start("lifecycle", "lifecycle")

        plugin.load()
        plugin.enable()
        plugin.disable()

        assertEquals(
            listOf(
                "load:early", "load:base:tccl=$PLUGIN_LOADER_NAME", "load:dependent",
                "enable:early", "enable:base", "enable:dependent",
                "disable:dependent", "disable:early",
            ),
            Journal.entries,
        )
    }

    @Test
    fun `a bootstrapped plugin creates only bootstrap beans until its full context starts`() = runTest {
        val bootstrap = manager.bootstrap(descriptor("deferred", "deferred"))
        bootstrap.bootstrap()

        assertEquals(listOf("created:bootstrap", "hook:bootstrap"), Journal.entries)

        Journal.clear()
        val plugin = manager.start(descriptor("deferred", "deferred"), pluginScope("deferred"))
        plugin.load()

        assertEquals(
            listOf("created:late", "initialized:late", "hook:load:bootstrap", "hook:load:late"),
            Journal.entries,
        )
    }

    @Test
    fun `a plugin without a bootstrap context runs its bootstrap hooks before its load hooks`() = runTest {
        val plugin = manager.start(descriptor("deferred", "deferred"), pluginScope("deferred"))
        plugin.load()

        assertEquals(
            listOf("hook:bootstrap", "hook:load:bootstrap", "hook:load:late"),
            Journal.entries.filter { it.startsWith("hook:") },
        )
    }

    @Test
    fun `getIfUnique of two providers' type finds nothing, a programmatic lookup fails`() {
        start("provider", "provider")
        start("second", "second")
        val consumer = start("ambiguousoptional", "ambiguousoptional", dependencies = listOf("provider", "second"))

        val user = consumer.context.getBean(OptionalGreetingUser::class.java)
        assertNull(user.greeting.ifUnique)
        assertFailsWith<NoUniqueBeanDefinitionException> { consumer.context.getBean(Greeting::class.java) }
    }

    @Test
    fun `a provider closes only after the plugins importing from it`() {
        start("provider", "provider")
        val consumer = start("consumer", "consumer", dependencies = listOf("provider"))

        manager.close("provider")
        assertEquals(listOf("provider.init"), Journal.entries)
        assertEquals("hello from provider", consumer.context.getBean(GreetingConsumer::class.java).greeting.greet())

        manager.close("consumer")
        assertEquals(listOf("provider.init", "provider.destroy"), Journal.entries)
    }

    @Test
    fun `a dependency declared under an alias resolves to its plugin`() {
        manager.start(descriptor("provider", "provider", aliases = listOf("ProviderAlias")), pluginScope("provider"))
        val consumer = start("consumer", "consumer", dependencies = listOf("ProviderAlias"))

        assertEquals("hello from provider", consumer.context.getBean(GreetingConsumer::class.java).greeting.greet())
    }

    @Test
    fun `ConditionalOnPlugin keeps beans of missing plugins out`() {
        val plugin = start("conditional", "conditional")

        assertEquals("present", plugin.context.getBean(OptionalUser::class.java).optional?.answer())
    }

    @Test
    fun `ConditionalOnPlugin accepts any of its names and requires every repetition`() {
        val plugin = start("conditionalnames", "conditionalnames")

        val registered = plugin.context.beanDefinitionNames.filter { it.endsWith("Bean") }.toSet()
        assertEquals(setOf("alternativeNameBean", "everyRepetitionBean"), registered)
    }

    @Test
    fun `root contributors see each full context and find the plugin's annotated classes`() {
        val plugin = start("contributed", "contributed")

        assertEquals(listOf("contributed" to listOf<Class<*>>(ContributedSettings::class.java)), contributions)
        assertTrue(plugin.context.getBeansOfType(ContributedSettings::class.java).isEmpty())
    }

    @Test
    fun `phase listeners run after the enable hooks and before the disable hooks`() = runTest {
        val plugin = start("phase", "phase")

        plugin.enable()
        plugin.disable()

        assertEquals(listOf("enable:hook", "afterEnable", "beforeDisable", "disable:hook"), Journal.entries)
    }

    @Test
    fun `a plugin bean injects its plugin's coroutine scope rather than the root's`() {
        val scope = pluginScope("scope")
        val plugin = start("scope", "scope", scope = scope)

        assertSame(scope, plugin.context.getBean(ScopeUser::class.java).scope)
        assertSame(scope, plugin.coroutineScope)
    }

    @Test
    fun `lifecycle hooks run with the elements of the plugin's coroutine scope`() = runTest {
        val plugin = start("scope", "scope")

        plugin.enable()

        assertEquals("surf-scope", plugin.context.getBean(ScopeUser::class.java).enableCoroutineName)
    }

    @Test
    fun `the coroutine scope is cancelled after the disable hooks ran`() = runTest {
        val scope = pluginScope("scope")
        val plugin = start("scope", "scope", scope = scope)

        plugin.disable()

        assertEquals(true, plugin.context.getBean(ScopeUser::class.java).scopeActiveDuringDisable)
        assertTrue(scope.coroutineContext.job.isCancelled)
    }

    @Test
    fun `closing a plugin without disabling it cancels its coroutine scope`() {
        val scope = pluginScope("scope")
        start("scope", "scope", scope = scope)

        manager.close("scope")

        assertTrue(scope.coroutineContext.job.isCancelled)
    }

    @Test
    fun `a context that cannot be built cancels its coroutine scope`() {
        val scope = pluginScope("badexport")

        assertFailsWith<PluginContextException> { start("badexport", "badexport", scope = scope) }
        assertTrue(scope.coroutineContext.job.isCancelled)
    }

    @Test
    fun `a plugin on surf-api's class loader inside surf-api's package is rejected`() {
        val descriptor = rootLoaderDescriptor("$FIXTURE_PACKAGE.provider")

        assertFailsWith<IllegalArgumentException> { manager.start(descriptor, pluginScope("shaded")) }
        assertFailsWith<IllegalArgumentException> { manager.bootstrap(descriptor) }
    }

    @Test
    fun `a plugin on surf-api's class loader scanning a parent of surf-api's package is rejected`() {
        val descriptor = rootLoaderDescriptor("dev.slne.surf")

        assertFailsWith<IllegalArgumentException> { manager.start(descriptor, pluginScope("shaded")) }
    }

    @Test
    fun `a plugin on surf-api's class loader outside surf-api's package starts`() {
        manager.start(rootLoaderDescriptor("dev.slne.surfapi"), pluginScope("shaded"))
    }

    @Test
    fun `an export is fetched from its provider only by a real lookup`() {
        val provider = start("lazyprovider", "lazyprovider")
        val consumer = start("lazyconsumer", "lazyconsumer", dependencies = listOf("lazyprovider"))
        val importName = "${PluginExport.IMPORT_PREFIX}lazyprovider/lazyGreeting"

        assertContains(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(consumer.context, Greeting::class.java), importName)
        assertTrue(consumer.context.isTypeMatch(importName, Greeting::class.java))
        assertEquals(LazyGreeting::class.java, consumer.context.getType(importName))
        assertTrue("lazy.created" !in Journal.entries, "type checks must not create the export")

        val greeting = consumer.context.getBean(Greeting::class.java)

        assertSame(provider.context.getBean("lazyGreeting"), greeting)
        assertSame(greeting, consumer.context.getBean<LazyGreetingUser>().greeting.getObject())
        assertEquals(listOf("lazy.created"), Journal.entries)
    }

    @Test
    fun `concurrent first lookups of an export share one instance`() {
        start("lazyprovider", "lazyprovider")
        val consumer = start("lazyconsumer", "lazyconsumer", dependencies = listOf("lazyprovider"))
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(CONCURRENT_LOOKUPS)

        try {
            val lookups = List(CONCURRENT_LOOKUPS) {
                executor.submit<Greeting> {
                    start.await()
                    consumer.context.getBean(Greeting::class.java)
                }
            }
            start.countDown()
            val instances = lookups.map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(1, instances.toSet().size)
            assertEquals(listOf("lazy.created"), Journal.entries)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `an export declared with a type argument never satisfies another one`() {
        start("lazyprovider", "lazyprovider")
        val consumer = start("lazyconsumer", "lazyconsumer", dependencies = listOf("lazyprovider"))

        val user = consumer.context.getBean<LazyGreetingUser>()

        assertEquals("plums", user.plums.describe())
        assertNull(user.apples)
    }

    @Test
    fun `an export of a closed provider fails every uncached lookup`() {
        val provider = start("lazyprovider", "lazyprovider")
        val consumer = start("lazyconsumer", "lazyconsumer", dependencies = listOf("lazyprovider"))
        provider.context.close()

        repeat(2) {
            val failure = assertFailsWith<IllegalStateException> { consumer.context.getBean(Greeting::class.java) }
            assertContains(failure.message.orEmpty(), "is closed")
        }
    }

    @Test
    fun `a dependency declared under its name and an alias is imported once`() {
        manager.start(descriptor("provider", "provider", aliases = listOf("ProviderAlias")), pluginScope("provider"))
        val consumer = start("consumer", "consumer", dependencies = listOf("provider", "ProviderAlias"))

        assertEquals("hello from provider", consumer.context.getBean(GreetingConsumer::class.java).greeting.greet())
    }

    @Test
    fun `two exports under one import name fail the import context`() {
        val first = providerOfGreeting()
        val second = providerOfGreeting()
        try {
            val exports = collectExports("p", first, pluginLoader) + collectExports("p", second, pluginLoader)

            val failure = assertFailsWith<IllegalStateException> { createImportContext("consumer", root, exports) }

            assertContains(failure.message.orEmpty(), "${PluginExport.IMPORT_PREFIX}p/greeting")
        } finally {
            first.close()
            second.close()
        }
    }

    private fun providerOfGreeting() = AnnotationConfigApplicationContext().apply {
        registerBean<Greeting>("greeting") {
            object : Greeting {
                override fun greet() = "hello"
            }
        }
        refresh()
    }

    private fun start(
        name: String,
        fixture: String,
        dependencies: List<String> = emptyList(),
        scope: CoroutineScope = pluginScope(name),
    ) = manager.start(descriptor(name, fixture, dependencies), scope)

    private fun pluginScope(name: String) = surfCoroutineScope(name, pluginLoader, Dispatchers.Unconfined)

    private fun descriptor(
        name: String,
        fixture: String,
        dependencies: List<String> = emptyList(),
        aliases: List<String> = emptyList(),
    ) = PluginDescriptor(
        name = name,
        classLoader = pluginLoader,
        source = testClasses,
        basePackages = listOf("$FIXTURE_PACKAGE.$fixture"),
        dependencies = dependencies,
        aliases = aliases,
    )

    private fun rootLoaderDescriptor(basePackage: String) = PluginDescriptor(
        name = "shaded",
        classLoader = checkNotNull(root.classLoader),
        source = testClasses,
        basePackages = listOf(basePackage),
    )

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
        generateSequence(this) { it.cause }.any { it is T }

    private companion object {
        const val PLUGIN_LOADER_NAME = "test-plugin-loader"
        const val FIXTURE_PACKAGE = "dev.slne.surf.api.spring.context.fixture"
        const val CONCURRENT_LOOKUPS = 8

        val testClasses: Path = Path.of(Journal::class.java.protectionDomain.codeSource.location.toURI())
    }
}
