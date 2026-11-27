package dev.slne.surf.api.velocity.sharedbean.consumer

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.event.SurfSyncEvent
import dev.slne.surf.api.luckperms.SurfLuckPerms
import dev.slne.surf.api.spring.ConditionalOnPlugin
import dev.slne.surf.api.spring.ManualListenerRegistration
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.velocity.event.VelocityListener
import dev.slne.surf.api.velocity.sharedbean.SharedBeanConsumerPlugin
import dev.slne.surf.api.velocity.sharedbean.provider.ProviderInternals
import dev.slne.surf.api.velocity.sharedbean.provider.SharedGreeting
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.luckperms.api.LuckPerms
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.stereotype.Service

/**
 * Collects every observation of the boot test and logs it in one line when the plugin enables.
 */
@Service
class SharedGreetingConsumer(
    private val greeting: SharedGreeting,
    private val providerInternals: ObjectProvider<ProviderInternals>,
    private val plugin: SharedBeanConsumerPlugin,
    private val container: PluginContainer,
    private val server: ProxyServer,
    private val context: ApplicationContext,
    private val probes: List<HookProbe>,
    private val configuration: ConsumerConfiguration,
    private val velocityListener: SpikeVelocityListener,
    private val manualVelocityListener: ManualSpikeVelocityListener,
    private val surfEventListener: SpikeSurfEventListener,
    private val pluginLogger: ComponentLogger,
) : SurfLifecycle {

    private val log = logger()
    private val contextClassLoaderIsPlugin = Thread.currentThread()
        .contextClassLoader === javaClass.classLoader
    private val pluginConstructed = (plugin.proxy as Any?) === server &&
        (plugin.constructedMarker as String?) != null
    private val subclassFieldInjected = plugin.injectedContainer != null
    private val subclassMethodInjected = plugin.methodInjected

    override suspend fun onLoad(): Unit = phase("load")

    override suspend fun onEnable() {
        phase("enable")
        SpikeSurfEvent().call()
        server.eventManager.fire(SpikeVelocityEvent()).await()

        log.atInfo()
            .log(
                "%s greeting=%s sameLoader=%s privateVisible=%s pluginIsInstance=%s containerIsOwn=%s " +
                    "tccl=%s probes=%s configProxy=%s surfEvent=%s velocityEvent=%s " +
                    "manualVelocityEvent=%s shutdownHooks=%s logImpl=%s pluginConstructed=%s " +
                    "pluginLogger=%s subclassField=%s subclassMethod=%s",
                SharedBeanReport.MARKER,
                greeting.greet(),
                greeting.javaClass.classLoader === SharedGreeting::class.java.classLoader,
                providerInternals.ifAvailable != null,
                container.instance.orElse(null) === plugin,
                container.description.id == SharedBeanReport.PLUGIN_ID,
                contextClassLoaderIsPlugin,
                probes.map { it.javaClass.simpleName }.sorted(),
                configuration.javaClass.name.contains("CGLIB"),
                surfEventListener.received,
                velocityListener.received,
                manualVelocityListener.received,
                countShutdownHooks(context),
                LogFactory.getLog(javaClass).javaClass.simpleName,
                pluginConstructed,
                pluginLogger.name,
                subclassFieldInjected,
                subclassMethodInjected,
            )
    }

    override suspend fun onDisable(): Unit = phase("disable")

    private fun phase(name: String) {
        log.atInfo()
            .log("%s consumer phase=%s", SharedBeanReport.SPIKE_MARKER, name)
    }

    private fun countShutdownHooks(context: ApplicationContext): Int {
        val field = AbstractApplicationContext::class.java.getDeclaredField("shutdownHook")
        field.isAccessible = true
        return generateSequence(context) { it.parent }
            .filterIsInstance<ConfigurableApplicationContext>()
            .count { field.get(it) != null }
    }
}

/**
 * An adapter to an optional plugin.
 */
interface HookProbe

/**
 * Refers to LuckPerms, which the test proxy has, and injects surf-api's [SurfLuckPerms] root bean.
 */
@Service
@ConditionalOnPlugin("luckperms")
class LuckPermsProbe(private val surfLuckPerms: SurfLuckPerms) : HookProbe {
    fun luckPerms(): LuckPerms = surfLuckPerms.luckPerms
}

/**
 * Guarded by a plugin the test proxy does not have, so it must be skipped.
 */
@Service
@ConditionalOnPlugin("surf-shared-bean-absent")
class AbsentProbe : HookProbe

/**
 * A configuration class, which Spring enhances with a CGLIB subclass inside Velocity's class
 * loader.
 */
@Configuration
class ConsumerConfiguration {

    @Bean
    fun consumerAnswer(): ConsumerAnswer = ConsumerAnswer(42)
}

/**
 * The bean [ConsumerConfiguration] declares.
 */
class ConsumerAnswer(val value: Int)

/**
 * The event the consumer fires on Velocity's event manager.
 */
class SpikeVelocityEvent

/**
 * A Velocity listener bean with a suspending handler, which surf-api registers in the enable phase.
 */
@VelocityListener
class SpikeVelocityListener {

    @Volatile
    var received = false

    @Subscribe
    suspend fun on(event: SpikeVelocityEvent) {
        received = true
    }
}

/**
 * A Velocity listener bean that opted out of the registration; it must never receive the event.
 */
@VelocityListener
@ManualListenerRegistration
class ManualSpikeVelocityListener {

    @Volatile
    var received = false

    @Subscribe
    fun on(event: SpikeVelocityEvent) {
        received = true
    }
}

/**
 * A command bean, which surf-api registers in the enable phase. The boot test runs it from the
 * console.
 */
@Service
class SpikeCommand : CommandAPICommand(SharedBeanReport.COMMAND) {

    private val log = logger()

    init {
        executes(
            CommandExecutor { _, _ ->
                log.atInfo()
                    .log(
                        "%s command=executed",
                        SharedBeanReport.SPIKE_MARKER,
                    )
            },
        )
    }
}

/**
 * The event the consumer fires on surf-api's event bus.
 */
class SpikeSurfEvent : SurfSyncEvent()

/**
 * An event bus listener bean, which surf-api registers in the enable phase.
 */
@Service
class SpikeSurfEventListener {

    @Volatile
    var received = false

    @SurfEventHandler
    fun on(event: SpikeSurfEvent) {
        received = true
    }
}
