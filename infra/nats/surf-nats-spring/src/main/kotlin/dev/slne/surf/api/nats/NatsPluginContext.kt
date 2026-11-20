package dev.slne.surf.api.nats

import dev.slne.surf.api.spring.context.withContextClassLoader
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsServiceInterceptorProvider
import dev.slne.surf.nats.embedded.EmbeddedNats
import dev.slne.surf.nats.embedded.NatsClientControl
import dev.slne.surf.nats.isNatsTransactional
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanProvider
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * The NATS state of one plugin context: its clients, the connection and the beans to register once the plugin is
 * enabled.
 *
 * The context's [NatsClient] bean is resolved by type, so a client the plugin declares itself wins over the
 * contributed fallback named after the plugin. The services, listeners and service proxies of the plugin all use
 * that one client. The fallback is created when a bean first asks for it, or when the plugin has `@NatsService` or
 * `@NatsListener` beans and no client of its own.
 *
 * [NatsBeanPostProcessor] reports every client the context creates through [clientCreated]. The clients created by
 * the end of the refresh connect once right after it, on `Dispatchers.IO`, so every plugin that uses NATS is
 * connected before `onLoad`. A client created after that point connects while it is created, which blocks the
 * caller. A plugin that uses nothing of NATS never creates a client and never connects.
 *
 * Recording a client and passing the connect point are transitions guarded by one lock: whichever side observes the
 * other's effect connects the client. The connection itself opens outside the lock, and
 * [NatsClientControl.connect] is idempotent.
 *
 * The interceptors of all recorded services are resolved and validated before the connection opens, so a
 * misconfigured `@NatsTransactional` bean fails the refresh before any service answers. [registerRecorded] closes
 * recording: a service or listener bean that is created afterwards could never be served and fails its creation.
 *
 * @param connect opens the connection of a client; the default connects through the shared connection
 */
internal class NatsPluginContext(
    private val context: AnnotationConfigApplicationContext,
    private val pluginName: String,
    private val connect: (NatsClientControl) -> Unit = ::connectShared,
) {

    /**
     * Name of the fallback client. Derived on first use, so a plugin that uses nothing of NATS never fails over its
     * name.
     */
    val clientName: String by lazy { NatsClientNames.of(pluginName) }

    private val lock = Any()
    private val clients = ArrayList<NatsClient>()
    private var connectPointPassed = false
    private val servicesByInterface = LinkedHashMap<Class<*>, String>()
    private val serviceBeans = LinkedHashMap<String, List<Class<*>>>()
    private val listenerBeans = LinkedHashMap<String, Class<*>>()
    private val preparedServices = HashMap<String, PreparedService>()
    private var registrationClosed = false

    private class PreparedService(
        val interfaces: List<Class<*>>,
        val bean: Any,
        val interceptor: NatsPersistentInterceptor?,
    )

    /**
     * Creates the fallback client, named after the plugin.
     */
    fun createClient(): NatsClient = NatsClient.create(clientName)

    /**
     * Records [created], a client the context just created, and connects it right away when the context already
     * passed its connect point.
     *
     * A client that fails to connect is closed again, so its name stays available.
     */
    fun clientCreated(created: NatsClient) {
        val connectNow = synchronized(lock) {
            clients += created
            connectPointPassed
        }
        if (!connectNow) return

        try {
            connect(EmbeddedNats.control(created))
        } catch (failure: Throwable) {
            synchronized(lock) { clients -= created }
            closeAfterFailure(created, failure)
            throw failure
        }
    }

    /**
     * Connects the clients the context created if the plugin uses NATS, and passes the connect point either way.
     *
     * A plugin without a client and without NATS beans never connects.
     */
    fun connectIfUsed() {
        if (needsClient()) {
            context.getBean<NatsClient>()
        }

        prepareServices()

        val pending = synchronized(lock) {
            connectPointPassed = true
            clients.toList()
        }

        pending.forEach { connect(EmbeddedNats.control(it)) }
    }

    /**
     * Records that bean [beanName] serves [interfaces].
     *
     * @throws IllegalStateException if another bean of the plugin already serves one of them, or if the recorded beans
     * were registered already
     */
    fun recordService(beanName: String, interfaces: List<Class<*>>) {
        synchronized(lock) {
            requireRecordable(beanName, beanName in serviceBeans)
            for (type in interfaces) {
                val owner = servicesByInterface[type]
                check(owner == null || owner == beanName) {
                    "Beans '$owner' and '$beanName' both serve ${type.name}; a plugin serves a NATS service once"
                }
            }

            interfaces.forEach { servicesByInterface[it] = beanName }
            serviceBeans[beanName] = interfaces
        }
    }

    /**
     * Records that bean [beanName], whose class behind any proxy is [beanClass], has `@NatsListener` methods.
     *
     * @throws IllegalStateException if the recorded beans were registered already
     */
    fun recordListener(beanName: String, beanClass: Class<*>) {
        synchronized(lock) {
            requireRecordable(beanName, beanName in listenerBeans)
            listenerBeans[beanName] = beanClass
        }
    }

    /**
     * Registers the recorded services and listeners with the plugin's client, using the final beans.
     *
     * Resolves the client by type, which creates and connects the fallback when no bean asked for a client before.
     * Each service is registered with the interceptor of the first [NatsServiceInterceptorProvider] that handles it.
     * Every interceptor is resolved before the first service is registered. Closes recording, see [recordService].
     *
     * @throws IllegalStateException if a [dev.slne.surf.nats.NatsTransactional] service bean has no provider that
     * handles it, or more than one
     */
    suspend fun registerRecorded() {
        val hasRecorded = synchronized(lock) {
            registrationClosed = true
            serviceBeans.isNotEmpty() || listenerBeans.isNotEmpty()
        }
        if (!hasRecorded) return

        val resolved = withContext(Dispatchers.IO) { context.getBean<NatsClient>() }
        val control = EmbeddedNats.control(resolved)

        val services = prepareServices().values
        val listeners = synchronized(lock) { listenerBeans.toMap() }

        for (service in services) {
            service.interfaces.forEach {
                registerService(
                    resolved,
                    it,
                    service.bean,
                    service.interceptor
                )
            }
        }

        for ((beanName, beanClass) in listeners) {
            control.registerListener(listenerTarget(beanName, beanClass), beanClass)
        }
    }

    /**
     * Stops the inbound work of the clients the context created: drains their handlers while the plugin's beans are
     * still alive. Publishing and calling stay possible until a client closes with the context.
     */
    suspend fun stopAcceptingIfCreated() {
        val created = synchronized(lock) { clients.toList() }
        if (created.isEmpty()) return
        withContext(Dispatchers.IO) {
            created.forEach { EmbeddedNats.control(it).stopAccepting() }
        }
    }

    /**
     * Whether the plugin has `@NatsService` or `@NatsListener` beans but no client yet, which makes the connect point
     * create one.
     */
    private fun needsClient(): Boolean = synchronized(lock) {
        clients.isEmpty() && (serviceBeans.isNotEmpty() || listenerBeans.isNotEmpty())
    }

    private fun requireRecordable(beanName: String, alreadyRecorded: Boolean) {
        check(alreadyRecorded || !registrationClosed) {
            "Bean '$beanName' uses NATS annotations but was created after the plugin registered its NATS beans, so " +
                    "it would never be served; make it non-lazy so the context creates it while it starts"
        }
    }

    /**
     * Resolves the final bean and the interceptor of every recorded service, once per bean, and validates them.
     */
    private fun prepareServices(): Map<String, PreparedService> {
        val services = synchronized(lock) { serviceBeans.toMap() }
        if (services.isEmpty()) return emptyMap()

        val client = context.getBean<NatsClient>()
        val providers by lazy {
            context.getBeanProvider<NatsServiceInterceptorProvider>().orderedStream().toList()
        }

        return services.mapValues { (beanName, interfaces) ->
            val preparedService = synchronized(lock) { preparedServices[beanName] }
            preparedService ?: run {
                val bean = context.getBean(beanName)

                PreparedService(interfaces, bean, interceptorFor(beanName, bean, providers, client))
                    .also { synchronized(lock) { preparedServices[beanName] = it } }
            }
        }
    }

    private fun interceptorFor(
        beanName: String,
        bean: Any,
        providers: List<NatsServiceInterceptorProvider>,
        client: NatsClient,
    ): NatsPersistentInterceptor? {
        val beanClass = AopProxyUtils.ultimateTargetClass(bean)
        if (!beanClass.isNatsTransactional()) {
            return providers.firstNotNullOfOrNull { it.interceptorFor(bean, beanClass, client) }
        }

        val answers = providers.mapNotNull { provider ->
            provider.interceptorFor(bean, beanClass, client)?.let { provider to it }
        }

        check(answers.isNotEmpty()) {
            "Bean '$beanName' (${beanClass.name}) is @NatsTransactional, but no NatsServiceInterceptorProvider handles " +
                    "it; add the reliability module of surf-microservice"
        }

        check(answers.size == 1) {
            "Bean '$beanName' (${beanClass.name}) is @NatsTransactional, but ${answers.size} " +
                    "NatsServiceInterceptorProviders handle it (${answers.joinToString { it.first.javaClass.name }}); " +
                    "an interceptor cannot be chained, so exactly one provider may answer for it"
        }

        val (provider, interceptor) = answers.single()
        check(provider.handlesTransactional) {
            "Bean '$beanName' (${beanClass.name}) is @NatsTransactional, but its only provider " +
                    "${provider.javaClass.name} does not declare handlesTransactional; add the reliability module of " +
                    "surf-microservice"
        }

        return interceptor
    }

    private suspend fun <S : Any> registerService(
        client: NatsClient,
        type: Class<S>,
        bean: Any,
        interceptor: NatsPersistentInterceptor?,
    ) {
        val implementation = type.cast(bean)
        when (interceptor) {
            null -> client.registerService(type.kotlin, implementation)
            else -> client.registerService(type.kotlin, implementation, interceptor)
        }
    }

    private fun listenerTarget(beanName: String, beanClass: Class<*>): Any {
        val bean = context.getBean(beanName)
        check(beanClass.isInstance(bean)) {
            val interfaces = AopProxyUtils.proxiedUserInterfaces(bean).joinToString { it.name }
            "Bean '$beanName' is proxied as $interfaces, " +
                    "which cannot call the @NatsListener methods of ${beanClass.name}; proxy the class instead"
        }

        return bean
    }

    private fun closeAfterFailure(client: NatsClient, failure: Throwable) {
        try {
            client.close()
        } catch (closeFailure: Exception) {
            failure.addSuppressed(closeFailure)
        }
    }
}

/**
 * Connects the plugin's clients once every singleton of the context exists, which is inside the context's refresh.
 *
 * A failing connection fails the refresh, so the plugin does not load, and closing the context releases what the
 * clients held.
 */
internal class NatsConnector(private val nats: NatsPluginContext) : SmartInitializingSingleton {

    override fun afterSingletonsInstantiated() {
        nats.connectIfUsed()
    }
}

/**
 * Connects [control] with surf-api's class loader as thread context class loader, so the threads the first
 * connection starts do not pin a plugin's loader.
 */
internal fun connectShared(control: NatsClientControl) {
    NatsPlatform.requireInstalled()
    runBlocking(Dispatchers.IO) {
        withContextClassLoader(NatsPluginContext::class.java.classLoader) {
            control.connect()
        }
    }
}
