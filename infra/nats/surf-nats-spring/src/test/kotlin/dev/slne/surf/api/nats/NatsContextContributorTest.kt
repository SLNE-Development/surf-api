package dev.slne.surf.api.nats

import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.NatsServiceInterceptorProvider
import dev.slne.surf.nats.NatsTransactional
import dev.slne.surf.nats.embedded.EmbeddedNats
import dev.slne.surf.nats.embedded.NatsClientControl
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.isNatsTransactional
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor
import dev.slne.surf.nats.test.proto.v1.BroadcastRequest
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.NotifierService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration
import org.springframework.boot.health.registry.HealthContributorRegistry
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.registerBean
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.function.Supplier
import kotlin.concurrent.thread
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A fake connect step stands in for the shared connection, so no NATS server is needed.
 */
@OptIn(InternalNatsApi::class)
class NatsContextContributorTest {

    class NotifierConsumer(val notifier: NotifierService)

    @NatsService(NotifierService::class)
    class NotifierImpl : NotifierService {
        override suspend fun announce(request: BroadcastRequest) = Unit
    }

    @NatsService
    class SecondNotifierImpl : NotifierService {
        override suspend fun announce(request: BroadcastRequest) = Unit
    }

    @NatsService
    class NoServiceImpl

    @NatsTransactional
    class TransactionalWithoutService

    @NatsTransactional
    @NatsService
    class TransactionalNotifierImpl : NotifierService {
        override suspend fun announce(request: BroadcastRequest) = Unit
    }

    @NatsService
    class LazyNotifierImpl : NotifierService {
        override suspend fun announce(request: BroadcastRequest) = Unit
    }

    class ListenerImpl {
        @NatsListener
        fun on(event: Created) = Unit
    }

    /**
     * Answers for every bean, like a metrics provider; it does not handle `@NatsTransactional`.
     */
    class GenericProvider : NatsServiceInterceptorProvider {
        override fun interceptorFor(bean: Any, beanClass: Class<*>, client: NatsClient) =
            NatsPersistentInterceptor { call -> call.proceed() }
    }

    /**
     * Answers for every bean except `@NatsTransactional` ones, like a metrics provider that steps aside.
     */
    class SteppingAsideProvider : NatsServiceInterceptorProvider {
        override fun interceptorFor(bean: Any, beanClass: Class<*>, client: NatsClient) =
            NatsPersistentInterceptor { call -> call.proceed() }.takeUnless { beanClass.isNatsTransactional() }
    }

    class TransactionalProvider(private val asked: MutableList<Class<*>> = CopyOnWriteArrayList()) :
        NatsServiceInterceptorProvider {
        override val handlesTransactional = true

        override fun interceptorFor(bean: Any, beanClass: Class<*>, client: NatsClient): NatsPersistentInterceptor {
            asked += beanClass
            return NatsPersistentInterceptor { call -> call.proceed() }
        }
    }

    class Facade {
        @Autowired
        lateinit var notifier: NotifierService
    }

    @Configuration(proxyBeanMethods = false)
    class FacadeConfiguration {
        @Bean
        fun facade() = Facade()
    }

    private companion object {
        const val RACE_ROUNDS = 500
    }

    private val context = AnnotationConfigApplicationContext()
    private val connected = ArrayList<NatsClientControl>()
    private val plugin = PluginDescriptor(
        name = "Contributor Demo",
        classLoader = javaClass.classLoader,
        source = null,
        basePackages = listOf("dev.slne.surf.api.nats.none"),
    )

    private fun contribute() = NatsContextContributor().contribute(context, plugin) { connected += it }

    @AfterEach
    fun closeContext() = context.close()

    @Test
    fun `a transactional bean that is not a NATS service fails the refresh`() {
        contribute()
        context.registerBean<TransactionalWithoutService>()

        val failure = assertFailsWith<BeanCreationException> { context.refresh() }

        assertContains(failure.mostSpecificCause.message.orEmpty(), "not a @NatsService")
    }

    @Test
    fun `a transactional service without an interceptor provider fails the refresh`() {
        contribute()
        context.registerBean<TransactionalNotifierImpl>()

        val failure = assertFailsWith<Exception> { context.refresh() }

        assertContains(rootCauseOf(failure).message.orEmpty(), "NatsServiceInterceptorProvider")
        assertTrue(connected.isEmpty(), "nothing connects, so no service answers before the failure")
    }

    @Test
    fun `a provider that does not declare it handles transactional beans fails the refresh`() {
        contribute()
        context.registerBean<TransactionalNotifierImpl>()
        context.registerBean<GenericProvider>()

        val failure = assertFailsWith<Exception> { context.refresh() }

        assertContains(rootCauseOf(failure).message.orEmpty(), "does not declare handlesTransactional")
    }

    @Test
    fun `two providers answering for a transactional bean fail the refresh whatever their order`() {
        contribute()
        context.registerBean<TransactionalNotifierImpl>()
        context.registerBean<GenericProvider>("generic")
        context.registerBean<TransactionalProvider>("transactionalProvider")

        val failure = assertFailsWith<Exception> { context.refresh() }

        assertContains(rootCauseOf(failure).message.orEmpty(), "cannot be chained")
    }

    @Test
    fun `a generic provider that steps aside leaves a transactional bean to the provider that handles it`() {
        contribute()
        context.registerBean<TransactionalNotifierImpl>()
        context.registerBean<SteppingAsideProvider>("steppingAside")
        context.registerBean<TransactionalProvider>("transactionalProvider")

        context.refresh()
    }

    @Test
    fun `a generic provider still serves a bean that is not transactional`() {
        contribute()
        context.registerBean<NotifierImpl>()
        context.registerBean<GenericProvider>()

        context.refresh()
    }

    @Test
    fun `a service is registered with the interceptor of the provider that handles it`() {
        val asked = CopyOnWriteArrayList<Class<*>>()
        contribute()
        context.registerBean<TransactionalNotifierImpl>()
        context.registerBean<NatsServiceInterceptorProvider> { TransactionalProvider(asked) }
        context.refresh()

        // The fake connection never opens, so registering the service itself may fail after the provider answered.
        val enable = runCatching { runBlocking { context.getBean(NatsPhaseListener::class.java).afterEnable() } }

        assertEquals(listOf<Class<*>>(TransactionalNotifierImpl::class.java), asked)
        enable.exceptionOrNull()?.let { assertFalse(it.message.orEmpty().contains("NatsServiceInterceptorProvider")) }
    }

    @Test
    fun `the client bean is a lazy fallback that closes with the context`() {
        contribute()

        val definition = context.getBeanDefinition(NATS_CLIENT_BEAN) as AbstractBeanDefinition
        assertTrue(definition.isLazyInit)
        assertTrue(definition.isFallback)
        assertEquals(NatsClient::class.java.name, definition.beanClassName)
        assertContentEquals(arrayOf("close"), definition.destroyMethodNames)
    }

    @Test
    fun `the health indicator is the component nats in the registry of Spring Boot`() {
        contribute()
        context.register(HealthContributorRegistryAutoConfiguration::class.java)
        context.refresh()

        val registry = context.getBean(HealthContributorRegistry::class.java)

        assertIs<NatsHealthIndicator>(registry.getContributor("nats"))
        assertNull(registry.getContributor(NATS_HEALTH_BEAN))
    }

    @Test
    fun `a context that never uses NATS creates no client and connects nothing`() {
        contribute()
        context.refresh()

        assertFalse(context.beanFactory.containsSingleton(NATS_CLIENT_BEAN))
        assertTrue(connected.isEmpty())
    }

    @Test
    fun `a service proxy is registered for an injected service interface and is a lazy fallback`() {
        contribute()
        context.register(NotifierConsumer::class.java)
        context.refresh()

        val definition = context.getBeanDefinition(NatsServiceProxyRegistrar.proxyBeanName(NotifierService::class.java))
        assertTrue(definition.isLazyInit)
        assertTrue(definition.isFallback)
        assertTrue(definition.dependsOn.isNullOrEmpty(), "the service proxy resolves the client by type, not by name")
        assertNotNull(context.getBean(NotifierConsumer::class.java).notifier)
    }

    @Test
    fun `the client connects once at the end of the refresh and is named after the plugin`() {
        contribute()
        context.register(NotifierConsumer::class.java)
        context.refresh()

        assertEquals(1, connected.size, "only the connector at the end of the refresh connects")
        assertEquals(NatsClientNames.of(plugin.name), context.getBean(NATS_CLIENT_BEAN, NatsClient::class.java).name)
    }

    @Test
    fun `a service proxy requested after the refresh connects the client it creates`() {
        contribute()
        context.registerBean(NotifierConsumer::class.java, BeanDefinitionCustomizer { it.isLazyInit = true })
        context.refresh()
        assertTrue(connected.isEmpty())

        context.getBean(NotifierConsumer::class.java)

        assertEquals(1, connected.size)
    }

    @Test
    fun `a lazy service proxy needs no connection to be created`() {
        val client = NatsClient.create("lazy-proxy-test")
        try {
            val proxy = EmbeddedNats.control(client).lazyService(NotifierService::class)

            assertIs<NotifierService>(proxy)
        } finally {
            client.close()
        }
    }

    @Test
    fun `a local service bean wins over the service proxy and is named after its class`() {
        contribute()
        context.register(NotifierImpl::class.java, NotifierConsumer::class.java)
        context.refresh()

        assertIs<NotifierImpl>(context.getBean(NotifierConsumer::class.java).notifier)
        val beanName = context.getBeanNamesForType(NotifierImpl::class.java).single()
        assertTrue(beanName.endsWith("NotifierImpl"), "the annotation value does not become the bean name: $beanName")
    }

    @Test
    fun `a service bean alone makes the plugin connect before it loads`() {
        contribute()
        context.register(NotifierImpl::class.java)
        context.refresh()

        assertEquals(1, connected.size)
    }

    @Test
    fun `two beans serving the same interface fail the refresh`() {
        contribute()
        context.register(NotifierImpl::class.java, SecondNotifierImpl::class.java)

        val failure = assertFailsWith<BeanCreationException> { context.refresh() }

        assertContains(rootCauseOf(failure).message.orEmpty(), "both serve")
    }

    @Test
    fun `a service bean without a generated interface fails the refresh`() {
        contribute()
        context.register(NoServiceImpl::class.java)

        val failure = assertFailsWith<BeanCreationException> { context.refresh() }

        assertContains(rootCauseOf(failure).message.orEmpty(), "implements no generated NATS service interface")
    }

    @Test
    fun `a plugin's own client wins, connects and serves the service proxies without the fallback being created`() {
        contribute()
        val own = registerOwnClient("own-client-wins")
        context.register(NotifierConsumer::class.java)
        context.refresh()

        assertEquals(listOf(EmbeddedNats.control(own)), connected)
        assertFalse(context.beanFactory.containsSingleton(NATS_CLIENT_BEAN))
        assertSame(own, context.getBean(NatsClient::class.java))
        assertNotNull(context.getBean(NotifierConsumer::class.java).notifier)
    }

    @Test
    fun `an own client named like the plugin does not collide with the fallback`() {
        contribute()
        val own = registerOwnClient(NatsClientNames.of(plugin.name))
        context.register(NotifierConsumer::class.java, NotifierImpl::class.java)

        context.refresh()

        assertEquals(listOf(EmbeddedNats.control(own)), connected)
    }

    @Test
    fun `an own client created after the refresh connects while it is created`() {
        contribute()
        context.refresh()
        assertTrue(connected.isEmpty())

        val own = registerOwnClient("own-client-late")
        context.getBean("own-client-late")

        assertEquals(listOf(EmbeddedNats.control(own)), connected)
    }

    @Test
    fun `a lazy service bean is created while the context starts and connects the client`() {
        contribute()
        context.registerBean(LazyNotifierImpl::class.java, BeanDefinitionCustomizer { it.isLazyInit = true })
        context.refresh()

        assertTrue(isCreated(LazyNotifierImpl::class.java))
        assertEquals(1, connected.size)
    }

    @Test
    fun `global lazy initialization does not defer service and listener beans`() {
        contribute()
        context.register(NotifierImpl::class.java, ListenerImpl::class.java, Facade::class.java)
        context.addBeanFactoryPostProcessor(LazyInitializationBeanFactoryPostProcessor())
        context.refresh()

        assertTrue(isCreated(NotifierImpl::class.java))
        assertTrue(isCreated(ListenerImpl::class.java))
        assertFalse(isCreated(Facade::class.java), "other beans stay lazy")
    }

    @Test
    fun `a service bean created between refresh and enable makes the enable phase create and connect the client`() {
        contribute()
        registerLazy("lazyNotifier") { LazyNotifierImpl() }
        context.refresh()
        assertTrue(connected.isEmpty())
        context.getBean("lazyNotifier")

        // The fake connection never opens, so registering the service itself may fail; the client must exist and
        // have been connected first.
        val enable = runCatching { runBlocking { context.getBean(NatsPhaseListener::class.java).afterEnable() } }
        enable.exceptionOrNull()?.let { assertFalse(it.message.orEmpty().contains("was not created")) }

        assertTrue(context.beanFactory.containsSingleton(NATS_CLIENT_BEAN))
        assertEquals(1, connected.size)
    }

    @Test
    fun `a service bean created after the enable phase fails its creation instead of never being served`() {
        contribute()
        registerLazy("lazyNotifier") { LazyNotifierImpl() }
        context.refresh()
        // The fake connection never opens, so registering may fail; recording closes either way.
        enableIgnoringFailure()

        val failure = assertFailsWith<BeanCreationException> { context.getBean("lazyNotifier") }

        assertContains(rootCauseOf(failure).message.orEmpty(), "created after the plugin registered")
    }

    @Test
    fun `a listener bean created after the enable phase fails its creation`() {
        contribute()
        registerLazy("lazyListener") { ListenerImpl() }
        context.refresh()
        enableIgnoringFailure()

        val failure = assertFailsWith<BeanCreationException> { context.getBean("lazyListener") }

        assertContains(rootCauseOf(failure).message.orEmpty(), "created after the plugin registered")
    }

    @Test
    fun `the health indicator does not take the bean name nats`() {
        context.setAllowBeanDefinitionOverriding(false)
        contribute()
        context.registerBean("nats", Any::class.java, Supplier<Any> { Any() })

        context.refresh()

        assertTrue(context.containsBean("nats"))
        assertTrue(context.containsBeanDefinition(NATS_HEALTH_BEAN))
    }

    @Test
    fun `the health indicator is registered although the plugin's loader hides the health module`() {
        context.classLoader = object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> =
                if (name.startsWith("org.springframework.boot.health")) throw ClassNotFoundException(name)
                else super.loadClass(name, resolve)
        }
        contribute()

        assertTrue(context.containsBeanDefinition(NATS_HEALTH_BEAN))
    }

    @Test
    fun `a plugin name without letters or digits only fails once the plugin uses NATS`() {
        val unnamed = PluginDescriptor(
            name = "___",
            classLoader = javaClass.classLoader,
            source = null,
            basePackages = listOf("dev.slne.surf.api.nats.none"),
        )
        NatsContextContributor().contribute(context, unnamed) { connected += it }
        context.refresh()

        assertTrue(connected.isEmpty())
        assertFailsWith<BeanCreationException> { context.getBean(NATS_CLIENT_BEAN) }
    }

    @Test
    fun `a service proxy is registered for a member of a bean method's result`() {
        contribute()
        context.register(FacadeConfiguration::class.java)
        context.refresh()

        assertTrue(context.containsBeanDefinition(NatsServiceProxyRegistrar.proxyBeanName(NotifierService::class.java)))
        assertNotNull(context.getBean(Facade::class.java).notifier)
    }

    @Test
    fun `a client created concurrently with the connect point is connected exactly by one side`() {
        repeat(RACE_ROUNDS) { round ->
            val connectedNow = CopyOnWriteArrayList<NatsClientControl>()
            val nats = NatsPluginContext(AnnotationConfigApplicationContext(), "race", connectedNow::add)
            val client = NatsClient.create("race-client-$round")
            val start = CountDownLatch(1)
            val threads = listOf(
                thread { start.await(); nats.clientCreated(client) },
                thread { start.await(); nats.connectIfUsed() },
            )

            start.countDown()
            threads.forEach(Thread::join)
            client.close()

            assertTrue(connectedNow.isNotEmpty(), "round $round left the client unconnected")
        }
    }

    /**
     * Runs the enable phase. The fake connection never opens, so registering a service may fail afterwards.
     */
    @IgnorableReturnValue
    private fun enableIgnoringFailure() =
        runCatching { runBlocking { context.getBean(NatsPhaseListener::class.java).afterEnable() } }

    private fun isCreated(type: Class<*>) =
        context.beanFactory.containsSingleton(context.getBeanNamesForType(type).single())

    /**
     * Registers a lazy bean whose declared class says nothing about its NATS annotations, like a `@Bean` method that
     * returns a supertype.
     */
    private fun registerLazy(name: String, create: () -> Any) {
        context.registerBean(name, Any::class.java, Supplier<Any> { create() }, BeanDefinitionCustomizer { it.isLazyInit = true })
    }

    private fun registerOwnClient(name: String): NatsClient {
        val own = NatsClient.create(name)
        context.registerBean(name, NatsClient::class.java, Supplier { own })
        return own
    }

    private fun rootCauseOf(failure: Throwable): Throwable =
        generateSequence(failure) { it.cause }.last()
}
