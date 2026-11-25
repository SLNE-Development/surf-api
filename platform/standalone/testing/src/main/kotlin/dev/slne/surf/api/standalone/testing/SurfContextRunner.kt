package dev.slne.surf.api.standalone.testing

import dev.slne.surf.api.spring.PluginPresence
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.preparePluginContext
import dev.slne.surf.api.spring.context.surfCoroutineScope
import dev.slne.surf.api.spring.scheduling.startScheduledTasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import java.nio.file.Path

/**
 * Creates an [ApplicationContextRunner] whose contexts resemble a plugin context below surf-api's
 * running standalone root context.
 *
 * Each context's parent is the root context that [SurfApiStandaloneTestSession] starts. Before the
 * refresh, a context receives what surf-api adds to every full plugin context: its own primary
 * `CoroutineScope`, cancelled when the context closes, `@Scheduled` support inside that scope, and
 * the contributions of the root's `ContextContributor` beans, such as a `ConfigHandle` per
 * `@SurfConfig` class in [basePackages]. The contributors see the context's class loader, so a
 * `FilteredClassLoader` set with `withClassLoader` hides classes from them and from the
 * conditions alike.
 *
 * A runner's context has no enable phase, so its `@Scheduled` tasks start when the refresh
 * completes rather than after the plugin's enable hooks.
 *
 * Components are not scanned; register them with `withUserConfiguration` or `withBean`.
 *
 * @param name the plugin name the contributors see
 * @param basePackages the packages the contributors search, for example for `@SurfConfig` classes;
 * by default a package without classes, so they find none
 * @param dataDirectory where config handles read and write their files; `null` for a plugin
 * without one, which fails the context if it declares `@SurfConfig` classes
 * @param installedPlugins the plugin names `@ConditionalOnPlugin` treats as installed
 * @throws IllegalStateException if surf-api's root context is not running
 */
fun surfContextRunner(
    name: String = "test",
    basePackages: List<String> = listOf(UNSCANNED_PACKAGE),
    dataDirectory: Path? = null,
    installedPlugins: Set<String> = emptySet(),
): ApplicationContextRunner {
    val root = SurfContexts.root

    return ApplicationContextRunner()
        .withParent(root)
        .withInitializer { context ->
            val plugin = PluginDescriptor(
                name = name,
                classLoader = checkNotNull(context.classLoader),
                source = null,
                basePackages = basePackages,
                dataDirectory = dataDirectory,
            )
            prepareTestPluginContext(context, root, plugin, installedPlugins)
        }
}

private fun prepareTestPluginContext(
    context: ConfigurableApplicationContext,
    root: ApplicationContext,
    plugin: PluginDescriptor,
    installedPlugins: Set<String>,
) {
    val pluginContext = context as AnnotationConfigApplicationContext
    val scope = surfCoroutineScope(plugin.name, plugin.classLoader, Dispatchers.Default)

    pluginContext.beanFactory.registerSingleton(
        PluginPresence.BEAN_NAME,
        PluginPresence { pluginName -> pluginName in installedPlugins },
    )
    pluginContext.defaultListableBeanFactory.registerDisposableBean(SCOPE_CANCELLATION_BEAN) {
        scope.cancel("The test context of '${plugin.name}' is closed")
    }
    preparePluginContext(pluginContext, root, plugin, scope)
    pluginContext.addApplicationListener(
        ApplicationListener<ContextRefreshedEvent> { event ->
            if (event.applicationContext === pluginContext) {
                startScheduledTasks(pluginContext)
            }
        },
    )
}

private const val SCOPE_CANCELLATION_BEAN = "surfTestScopeCancellation"

/**
 * A package that holds no classes, for a runner whose contributors have nothing to find.
 */
private const val UNSCANNED_PACKAGE = "dev.slne.surf.api.standalone.testing.unscanned"
