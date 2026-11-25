package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.context.fixture.Journal
import dev.slne.surf.api.spring.context.fixture.boot.*
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.NamedTicker
import dev.slne.surf.api.spring.context.fixture.boot.beans.NeedsGreeting
import dev.slne.surf.api.spring.context.fixture.boot.beans.NeedsRootMarker
import dev.slne.surf.api.spring.context.fixture.boot.beans.NeedsSingleRootMarker
import dev.slne.surf.api.spring.context.fixture.boot.classes.*
import dev.slne.surf.api.spring.context.fixture.boot.environment.*
import dev.slne.surf.api.spring.context.fixture.boot.nounique.FastTicker
import dev.slne.surf.api.spring.context.fixture.boot.nounique.SlowTicker
import dev.slne.surf.api.spring.context.fixture.boot.nounique.TickerUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.SearchStrategy
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import kotlin.test.*

class BootConditionsTest {

    private val rootScope = CoroutineScope(SupervisorJob())
    private val root = AnnotationConfigApplicationContext().apply {
        registerCoroutineScope(this, rootScope, primary = false)
        registerBean<RootMarker>("rootMarker")
        refresh()
    }
    private val manager = SurfContextManager(root) { false }
    private val visibleLoader = HidingClassLoader(emptySet())
    private val hidingLoader = HidingClassLoader(setOf(HIDDEN_TYPE, MARKER_RESOURCE))

    @AfterEach
    fun closeContexts() {
        manager.closeAll()
        root.close()
        rootScope.cancel()
        System.clearProperty(FEATURE_PROPERTY)
        System.clearProperty(NESTED_PROPERTY)
    }

    @Test
    fun `class conditions read the plugin's class loader on scanned classes and bean methods`() {
        val visible = start("visible", "classes", visibleLoader).context
        val hidden = start("hidden", "classes", hidingLoader).context

        assertTrue(visible.hasBean<WithHiddenType>())
        assertFalse(visible.hasBean<WithoutHiddenType>())
        assertEquals(listOf("hidden type present"), visible.names())

        assertFalse(hidden.hasBean<WithHiddenType>())
        assertTrue(hidden.hasBean<WithoutHiddenType>())
        assertEquals(listOf("hidden type missing"), hidden.names())
    }

    @Test
    fun `a SpringBootCondition sees the plugin's class loader`() {
        val context = start("loader", "classes", visibleLoader).context

        assertTrue(context.hasBean<SeesPluginLoader>())
    }

    @Test
    fun `an any-nested condition matches when one of its conditions does`() {
        val visible = start("visible", "classes", visibleLoader).context
        val hidden = start("hidden", "classes", hidingLoader).context

        assertTrue(visible.hasBean<NestedConditionBean>())
        assertFalse(hidden.hasBean<NestedConditionBean>())

        System.setProperty(NESTED_PROPERTY, "true")
        val withProperty = start("property", "classes", hidingLoader).context

        assertTrue(withProperty.hasBean<NestedConditionBean>())
    }

    @Test
    fun `bean conditions see the root context and the exports of dependencies`() {
        start("provider", "provider", visibleLoader, fixturePackage = FIXTURE_PACKAGE)
        val context = start(
            "beans",
            "beans",
            visibleLoader,
            dependencies = listOf("provider"),
        ).context

        assertTrue(context.hasBean<NeedsRootMarker>())
        assertTrue(context.hasBean<NeedsSingleRootMarker>())
        assertTrue(context.hasBean<NeedsGreeting>())
    }

    @Test
    fun `a bean condition misses the exports of a plugin that is no dependency`() {
        start("provider", "provider", visibleLoader, fixturePackage = FIXTURE_PACKAGE)
        val context = start("beans", "beans", visibleLoader).context

        assertFalse(context.hasBean<NeedsGreeting>())
    }

    @Test
    fun `a bean method with ConditionalOnMissingBean registers what nothing else provides`() {
        val context = start("beans", "beans", visibleLoader).context

        assertTrue(context.containsLocalBean("ticker"))
        assertFalse(context.containsLocalBean("rootMarker"))
    }

    @Test
    fun `ANCESTORS search fails in a context without a parent`() {
        val context = AnnotationConfigApplicationContext()
        context.register(AncestorsOnly::class.java)

        val failure = assertFailsWith<IllegalStateException> { context.refresh() }

        assertContains(failure.causes().last().message.orEmpty(), "SearchStrategy.ANCESTORS")
    }

    @Test
    fun `property and expression conditions read system properties`() {
        val off = start("off", "environment", visibleLoader).context
        assertFalse(off.hasBean<FeatureBean>())
        assertFalse(off.hasBean<ExpressionBean>())
        assertTrue(off.hasBean<FeatureOffBean>())

        System.setProperty(FEATURE_PROPERTY, "on")
        val on = start("on", "environment", visibleLoader).context

        assertTrue(on.hasBean<FeatureBean>())
        assertTrue(on.hasBean<ExpressionBean>())
        assertFalse(on.hasBean<FeatureOffBean>())
        assertTrue(on.hasBean<FeatureWithResourceBean>())
    }

    @Test
    fun `resource conditions read the plugin's class loader`() {
        System.setProperty(FEATURE_PROPERTY, "on")

        val visible = start("visible", "environment", visibleLoader).context
        val hidden = start("hidden", "environment", hidingLoader).context

        assertTrue(visible.hasBean<ResourceBean>())
        assertFalse(hidden.hasBean<ResourceBean>())
        assertFalse(hidden.hasBean<FeatureWithResourceBean>())
    }

    @Test
    fun `java, threading and cloud platform conditions evaluate`() {
        val context = start("environment", "environment", visibleLoader).context
        val isKubernetes = System.getenv("KUBERNETES_SERVICE_HOST") != null

        assertTrue(context.hasBean<JavaBean>())
        assertFalse(context.hasBean<VirtualThreadsBean>())
        assertEquals(isKubernetes, context.hasBean<KubernetesBean>())
    }

    @Test
    fun `imported auto-configurations run in their declared order whatever the listed order`() {
        for (fixture in listOf("autoconfig.forward", "autoconfig.reverse")) {
            val context = start(fixture, fixture, visibleLoader).context

            assertEquals(listOf("earlier"), context.names(), fixture)
            assertEquals(
                listOf(NamedTicker("first")),
                context.getBeansOfType<Ticker>().values.toList(),
                fixture,
            )
        }
    }

    @Test
    fun `a scanned auto-configuration is not applied`() {
        val context = start("forward", "autoconfig.forward", visibleLoader).context

        assertFalse(context.containsBean("strayNamed"))
    }

    @Test
    fun `a scanned class with ConditionalOnMissingBean fails the plugin's start`() {
        for (fixture in listOf("missingbean", "missingbeanmeta")) {
            val failure = assertFailsWith<PluginContextException> {
                start(fixture, fixture, visibleLoader)
            }

            val cause = failure.causes().filterIsInstance<IllegalStateException>().first()
            assertContains(cause.message.orEmpty(), "@ConditionalOnMissingBean")
            assertContains(cause.message.orEmpty(), "@Fallback")
        }
    }

    @Test
    fun `the root scan rejects a class with ConditionalOnMissingBean`() {
        val context = AnnotationConfigApplicationContext()

        try {
            val failure = assertFailsWith<IllegalStateException> {
                scanComponents(
                    context,
                    visibleLoader,
                    listOf("$FIXTURE_PACKAGE.boot.missingbean"),
                    ScanSelection.ALL,
                    testClasses,
                )
            }

            assertContains(failure.message.orEmpty(), "DefaultTicker")
        } finally {
            context.close()
        }
    }

    @Test
    fun `a failed start carries Spring Boot's failure analysis`() {
        val failure = assertFailsWith<PluginContextException> {
            start("nounique", "nounique", visibleLoader)
        }

        val analyzed = failure.causes().filterIsInstance<ContextFailureAnalyzedException>().single()
        val message = analyzed.message.orEmpty()

        assertContains(message, "surf plugin context of 'nounique' failed to start")
        assertContains(message, "Description:")
        assertContains(message, "required a single bean, but 2 were found")
        assertContains(message, "Action:")
        assertTrue(analyzed.causes().any { it is NoUniqueBeanDefinitionException })
    }

    @Test
    fun `the failure analysis reads the condition outcomes of the failed context`() {
        val failure = assertFailsWith<PluginContextException> {
            start("unmet", "unmet", hidingLoader)
        }

        val analyzed = failure.causes().filterIsInstance<ContextFailureAnalyzedException>().single()
        val message = analyzed.message.orEmpty()

        assertContains(message, "hiddenTicker")
        assertContains(message, "@ConditionalOnClass did not find required class")
    }

    @Test
    fun `a failed root context carries Spring Boot's failure analysis`() {
        val loader = URLClassLoader("empty-root-loader", emptyArray(), null)
        val scope = CoroutineScope(SupervisorJob())

        val failure = loader.use {
            assertFailsWith<ContextFailureAnalyzedException> {
                SurfRootContext.create(loader, scope) { context ->
                    context.register(
                        FastTicker::class.java,
                        SlowTicker::class.java,
                        TickerUser::class.java,
                    )
                }
            }
        }

        assertContains(failure.message.orEmpty(), "surf root context failed to start")
        assertContains(failure.message.orEmpty(), "required a single bean, but 2 were found")
        assertIs<NoUniqueBeanDefinitionException>(failure.causes().last())
    }

    @Test
    fun `a failure no analyzer recognizes is rethrown as it is`() {
        val failure = assertFailsWith<PluginContextException> {
            start("missingbean", "missingbean", visibleLoader)
        }

        assertIs<IllegalStateException>(failure.cause)
    }

    private fun start(
        name: String,
        fixture: String,
        classLoader: ClassLoader,
        dependencies: List<String> = emptyList(),
        fixturePackage: String = "$FIXTURE_PACKAGE.boot",
    ): PluginContext {
        val descriptor = PluginDescriptor(
            name = name,
            classLoader = classLoader,
            source = testClasses,
            basePackages = listOf("$fixturePackage.$fixture"),
            dependencies = dependencies,
        )
        val scope = surfCoroutineScope(name, classLoader, Dispatchers.Unconfined)

        return manager.start(descriptor, scope)
    }

    private inline fun <reified T : Any> AnnotationConfigApplicationContext.hasBean(): Boolean {
        return getBeanNamesForType(T::class.java).isNotEmpty()
    }

    private fun AnnotationConfigApplicationContext.names(): List<String> {
        return getBeansOfType<Named>().values.map(Named::name)
    }

    private fun Throwable.causes(): Sequence<Throwable> = generateSequence(this) { it.cause }

    /**
     * Delegates to the test's class loader, but hides some classes and resources, as a plugin's
     * class loader would that cannot see them.
     */
    private class HidingClassLoader(
        private val hidden: Set<String>,
    ) : ClassLoader(
        PluginLoaderCondition.PLUGIN_LOADER,
        BootConditionsTest::class.java.classLoader,
    ) {

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name in hidden) throw ClassNotFoundException(name)

            return super.loadClass(name, resolve)
        }

        override fun getResource(name: String): URL? {
            return if (name in hidden) null else super.getResource(name)
        }

        override fun getResources(name: String): Enumeration<URL> {
            return if (name in hidden) Collections.emptyEnumeration() else super.getResources(name)
        }
    }

    @Service
    @ConditionalOnBean(RootMarker::class, search = SearchStrategy.ANCESTORS)
    class AncestorsOnly

    private companion object {
        const val FIXTURE_PACKAGE = "dev.slne.surf.api.spring.context.fixture"
        const val NESTED_PROPERTY = "surf.test.boot.nested"

        val testClasses: Path = Path.of(
            Journal::class.java.protectionDomain.codeSource.location.toURI(),
        )
    }
}
