package dev.slne.surf.api.paper.sharedbean.consumer

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.event.SurfSyncEvent
import dev.slne.surf.api.luckperms.SurfLuckPerms
import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.plugin.SurfPaperPlugin
import dev.slne.surf.api.paper.sharedbean.provider.ProviderInternals
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreeting
import dev.slne.surf.api.spring.ConditionalOnPlugin
import dev.slne.surf.api.spring.ManualListenerRegistration
import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import dev.slne.surf.api.spring.SurfLifecycle
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.apache.commons.logging.LogFactory
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.Plugin
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
    private val plugin: Plugin,
    private val ownPlugin: SurfPaperPlugin,
    private val context: ApplicationContext,
    private val probes: List<HookProbe>,
    private val configuration: ConsumerConfiguration,
    private val luckPerms: ObjectProvider<LuckPerms>,
    private val surfLuckPerms: SurfLuckPerms,
    private val bootstrapProbe: ConsumerBootstrapProbe,
    private val eventListener: SpikeEventListener,
    private val coroutineProbe: ConsumerCoroutineProbe,
    private val configProbe: ConsumerConfigProbe,
    private val commandMessageProbe: ConsumerCommandMessageProbe,
    private val redisProbe: ConsumerRedisProbe,
    private val server: Server,
    private val pluginLogger: ComponentLogger,
) : SurfLifecycle {

    private val log = logger()
    private val contextClassLoaderIsPlugin = Thread.currentThread()
        .contextClassLoader === javaClass.classLoader

    override suspend fun onLoad(): Unit = phase("load")

    override suspend fun onEnable() {
        phase("enable")
        SpikeEvent().call()
        val instanceLoader = greeting.javaClass.classLoader
        val interfaceLoader = SharedGreeting::class.java.classLoader

        log.atInfo()
            .log(
                "%s greeting=%s sameLoader=%s privateVisible=%s pluginIsSelf=%s ownPluginIsSelf=%s " +
                    "pluginIdentity=%s proxyEquals=%s tccl=%s probes=%s configProxy=%s " +
                    "luckPermsBean=%s surfEvent=%s shutdownHooks=%s logImpl=%s scopeSurvivesLoad=%s surfConfig=%s " +
                    "redis=%s redisson=%s serverBean=%s pluginLogger=%s commandMessage=%s",
                SharedBeanReport.MARKER,
                greeting.greet(),
                instanceLoader === interfaceLoader,
                providerInternals.ifAvailable != null,
                plugin.name == PLUGIN_NAME,
                ownPlugin.name == PLUGIN_NAME,
                plugin === ownPlugin,
                bootstrapProbe.plugin == ownPlugin && ownPlugin == bootstrapProbe.plugin &&
                    bootstrapProbe.plugin.hashCode() == ownPlugin.hashCode(),
                contextClassLoaderIsPlugin,
                probes.map { it.javaClass.simpleName }.sorted(),
                configuration.javaClass.name.contains("CGLIB"),
                luckPerms.ifAvailable.let { it != null && it === surfLuckPerms.luckPerms },
                eventListener.received,
                countShutdownHooks(context),
                LogFactory.getLog(javaClass).javaClass.simpleName,
                coroutineProbe.scopeSurvivesLoad,
                configProbe.surfConfig,
                redisProbe.syncValue,
                redisProbe.redisson,
                server === ownPlugin.server,
                pluginLogger.name == ownPlugin.componentLogger.name,
                commandMessageProbe.commandMessage,
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

    private companion object {
        const val PLUGIN_NAME = "SurfSharedBeanConsumer"
    }
}

/**
 * The consumer's only bootstrap bean: it implements [SurfBootstrapLifecycle], so it is created in
 * the bootstrap phase while every other bean of the consumer waits for the load phase.
 */
@Service
class ConsumerBootstrapProbe(val plugin: Plugin) : SurfBootstrapLifecycle {

    private val log = logger()

    override suspend fun onBootstrap() {
        log.atInfo()
            .log("%s consumer phase=bootstrap", SharedBeanReport.SPIKE_MARKER)
    }
}

/**
 * An adapter to an optional plugin.
 */
interface HookProbe

/**
 * Refers to PlaceholderAPI in its signature; loading this class without PlaceholderAPI fails.
 */
@Service
@ConditionalOnPlugin("PlaceholderAPI")
class PlaceholderProbe : HookProbe {
    fun expansion(): PlaceholderExpansion? = null
}

/**
 * Refers to LuckPerms, which the test server has.
 */
@Service
@ConditionalOnPlugin("LuckPerms")
class LuckPermsProbe : HookProbe {
    fun luckPerms(): LuckPerms = LuckPermsProvider.get()
}

/**
 * Extends a PlaceholderAPI class. Without PlaceholderAPI the superclass cannot be read, so this
 * component must be skipped without its class ever being loaded.
 */
@Service
@ConditionalOnPlugin("PlaceholderAPI")
class ConsumerExpansion : PlaceholderExpansion() {
    override fun getIdentifier(): String = "surfconsumer"
    override fun getAuthor(): String = "SLNE Development Team"
    override fun getVersion(): String = "1"
}

/**
 * A configuration class, which Spring enhances with a CGLIB subclass inside Paper's class loader.
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
 * A Bukkit listener bean, which surf-api registers in the enable phase.
 */
@BukkitListener
class SpikeServerListener : Listener {

    private val log = logger()

    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) {
        log.atInfo()
            .log("%s listener=serverLoad", SharedBeanReport.SPIKE_MARKER)
    }
}

/**
 * A Bukkit listener bean that opted out of the registration; it must never log.
 */
@BukkitListener
@ManualListenerRegistration
class ManualSpikeListener : Listener {

    private val log = logger()

    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) {
        log.atInfo()
            .log("%s listener=manual", SharedBeanReport.SPIKE_MARKER)
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
 * The event the consumer fires on its own event bus listener.
 */
class SpikeEvent : SurfSyncEvent()

/**
 * An event bus listener bean, which surf-api registers in the enable phase.
 */
@Service
class SpikeEventListener {

    @Volatile
    var received = false

    @SurfEventHandler
    fun on(event: SpikeEvent) {
        received = true
    }
}

/**
 * The consumer's main class.
 */
class SharedBeanConsumerPlugin : SurfPaperPlugin()
