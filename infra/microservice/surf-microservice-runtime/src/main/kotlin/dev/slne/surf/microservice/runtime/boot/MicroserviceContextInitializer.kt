package dev.slne.surf.microservice.runtime.boot

import dev.slne.surf.api.spring.SurfProperties
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceNames
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceShutdown
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.ExitCodeEvent
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.core.Ordered
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.get
import kotlin.io.path.Path

/**
 * Makes the application context of a microservice surf-api's root context, before anything else
 * initializes it.
 *
 * Resolves the [MicroserviceInfo] and registers it, together with [shutdown], as a bean. Publishes
 * the name as `spring.application.name` and the data folder as [SurfProperties.CONFIG_DIRECTORY],
 * ahead of every other property source, so that surf modules read the same values. Registers the
 * listeners that turn the close of the context into a shutdown request, with the exit code of
 * `SpringApplication.exit`, so the process ends however the context closes, and registers the bean
 * that the context destroys last, which tells [shutdown] that the close is over. Then hands the
 * context to [SurfApiStandaloneBootstrap.adopt], with the application's package as the package of
 * the hosted application.
 *
 * Also listens for the failure of the application run. An initializer that runs after this one may
 * throw, which leaves surf-api adopted on a context that never refreshes and so never destroys its
 * beans; the listener then releases surf-api. A context that did refresh stops surf-api through its
 * own lifecycle.
 */
internal class MicroserviceContextInitializer(
    private val application: Class<*>,
    private val shutdown: MicroserviceShutdown,
) : ApplicationContextInitializer<ConfigurableApplicationContext>,
    ApplicationListener<ApplicationFailedEvent>,
    Ordered {

    @Volatile
    private var adoptedContext: ConfigurableApplicationContext? = null

    override fun initialize(context: ConfigurableApplicationContext) {
        val surfContext = checkNotNull(context as? AnnotationConfigApplicationContext) {
            "A microservice needs an AnnotationConfigApplicationContext, was ${context.javaClass.name}"
        }
        surfContext.defaultListableBeanFactory.registerDisposableBean(
            CLOSE_SIGNAL_BEAN,
            DisposableBean(shutdown::markContextClosed),
        )
        val info = resolveInfo(context.environment)

        context.environment.propertySources.addFirst(
            MapPropertySource(
                PROPERTY_SOURCE,
                mapOf(
                    SurfProperties.APPLICATION_NAME to info.name,
                    SurfProperties.CONFIG_DIRECTORY to info.dataPath.toString(),
                ),
            ),
        )
        context.beanFactory.registerSingleton(INFO_BEAN, info)
        context.beanFactory.registerSingleton(SHUTDOWN_BEAN, shutdown)
        context.addApplicationListener(
            ApplicationListener<ExitCodeEvent> { shutdown.exitCode = it.exitCode },
        )
        context.addApplicationListener(
            ApplicationListener<ContextClosedEvent> { event ->
                if (event.applicationContext === context) shutdown.request()
            },
        )
        SurfApiStandaloneBootstrap.adopt(surfContext, descriptor(info))
        adoptedContext = surfContext
    }

    override fun onApplicationEvent(event: ApplicationFailedEvent) {
        val context = adoptedContext ?: return
        if (context.isActive) return

        runBlocking { SurfApiStandaloneBootstrap.shutdown() }
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    private fun resolveInfo(environment: Environment): MicroserviceInfo {
        val name = environment[MicroserviceInfo.NAME_ENVIRONMENT_VARIABLE]
            ?: environment[SurfProperties.APPLICATION_NAME]
            ?: defaultName()
        val dataPath = Path(
            environment[MicroserviceInfo.CONFIG_DIR_ENVIRONMENT_VARIABLE]
                ?: SurfProperties.DEFAULT_CONFIG_DIRECTORY,
        )

        return MicroserviceInfo(MicroserviceNames.requireValid(name), dataPath)
    }

    /**
     * The class name without an `Application` or `Microservice` suffix, in kebab case:
     * `PunishApplication` becomes `punish`.
     */
    private fun defaultName(): String {
        val simpleName = NAME_SUFFIXES.fold(application.simpleName) { name, suffix ->
            name.removeSuffix(
                suffix,
            )
        }
            .ifEmpty { application.simpleName }

        return simpleName.replace(WORD_BOUNDARY, "-").lowercase()
    }

    private fun descriptor(info: MicroserviceInfo): PluginDescriptor {
        return PluginDescriptor(
            name = info.name,
            classLoader = application.classLoader,
            source = null,
            basePackages = listOf(application.packageName),
            dataDirectory = info.dataPath,
        )
    }

    private companion object {

        private const val PROPERTY_SOURCE = "surfMicroservice"
        private const val INFO_BEAN = "microserviceInfo"
        private const val SHUTDOWN_BEAN = "microserviceShutdown"
        private const val CLOSE_SIGNAL_BEAN = "microserviceCloseSignal"
        private val NAME_SUFFIXES = listOf("Application", "Microservice")
        private val WORD_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")
    }
}
