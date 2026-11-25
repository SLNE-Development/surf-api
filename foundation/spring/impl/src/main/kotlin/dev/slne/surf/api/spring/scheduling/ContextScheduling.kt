package dev.slne.surf.api.spring.scheduling

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.Ordered
import org.springframework.scheduling.config.TaskManagementConfigUtils
import java.time.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The name of the scheduler that runs `@Scheduled` methods without `scheduler`, on the dispatcher
 * of the context's coroutine scope.
 */
internal const val PLUGIN_SCHEDULER_BEAN = "surfTaskScheduler"

internal const val SCHEDULING_BEAN = "surfScheduling"

private const val PROCESSOR_REGISTRAR_BEAN = "surfScheduledAnnotationProcessorRegistrar"

/**
 * Enables `@Scheduled` in [context], with every task running inside [scope].
 *
 * Registers the context's [PluginScheduling], the default scheduler, the [SurfSchedulers]
 * schedulers and a registrar that installs [SurfScheduledAnnotationProcessor] as the context's
 * scheduled-annotation processor after the configuration classes are read, replacing the one an
 * `@EnableScheduling` registers. The tasks start with the context's enable phase.
 *
 * @param owner the plugin or application name in the log lines of failing tasks
 * @param clock the clock the schedules read; tests pass one that follows virtual time
 */
internal fun registerScheduling(
    context: AnnotationConfigApplicationContext,
    owner: String,
    scope: CoroutineScope,
    clock: Clock = Clock.systemDefaultZone(),
) {
    val scheduling = PluginScheduling(owner, scope, clock)
    context.registerBeanDefinition(
        SCHEDULING_BEAN,
        RootBeanDefinition(PluginScheduling::class.java).apply {
            setInstanceSupplier { scheduling }
            role = BeanDefinition.ROLE_INFRASTRUCTURE
        },
    )

    registerTaskScheduler(context, PLUGIN_SCHEDULER_BEAN, EmptyCoroutineContext, fallback = false)
    registerTaskScheduler(context, SurfSchedulers.DEFAULT, Dispatchers.Default)
    registerTaskScheduler(context, SurfSchedulers.IO, Dispatchers.IO)

    context.registerBeanDefinition(
        PROCESSOR_REGISTRAR_BEAN,
        RootBeanDefinition(ScheduledAnnotationProcessorRegistrar::class.java).apply {
            setInstanceSupplier { ScheduledAnnotationProcessorRegistrar() }
            role = BeanDefinition.ROLE_INFRASTRUCTURE
        },
    )
}

/**
 * Registers a Spring `TaskScheduler` under [name] in [context] that runs its tasks inside the
 * context's coroutine scope, dispatched with [dispatcher].
 *
 * [context] must have scheduling enabled, which every full plugin context has. The scheduler is a
 * `@Fallback` bean, so an unqualified `TaskScheduler` injection still gets the default scheduler.
 */
@InternalSurfApi
fun registerTaskScheduler(
    context: AnnotationConfigApplicationContext,
    name: String,
    dispatcher: CoroutineContext,
) {
    registerTaskScheduler(context, name, dispatcher, fallback = true)
}

/**
 * Starts the `@Scheduled` tasks of [context] outside an enable phase, for contexts that have none,
 * such as test contexts. Does nothing if they already started.
 */
@InternalSurfApi
fun startScheduledTasks(context: ApplicationContext) {
    context.getBean<SurfScheduledAnnotationProcessor>(
        TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME,
    ).start()
}

private fun registerTaskScheduler(
    context: AnnotationConfigApplicationContext,
    name: String,
    dispatcher: CoroutineContext,
    fallback: Boolean,
) {
    val beanFactory = context.beanFactory
    val definition = RootBeanDefinition(CoroutineTaskScheduler::class.java).apply {
        setInstanceSupplier {
            val scheduling = beanFactory.getBean<PluginScheduling>(SCHEDULING_BEAN)
            CoroutineTaskScheduler(scheduling, dispatcher)
        }
        isFallback = fallback
    }
    context.registerBeanDefinition(name, definition)
}

/**
 * Installs [SurfScheduledAnnotationProcessor] under Spring's processor name.
 *
 * It runs after Spring's `ConfigurationClassPostProcessor`, which is `PriorityOrdered`, so it sees
 * the processor an `@EnableScheduling` declared and replaces it; ours wins in every context,
 * whether bean definition overriding is allowed or not.
 */
private class ScheduledAnnotationProcessorRegistrar : BeanDefinitionRegistryPostProcessor, Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME
        if (registry.containsBeanDefinition(name)) {
            registry.removeBeanDefinition(name)
        }

        val definition = RootBeanDefinition(SurfScheduledAnnotationProcessor::class.java).apply {
            setInstanceSupplier { SurfScheduledAnnotationProcessor() }
            role = BeanDefinition.ROLE_INFRASTRUCTURE
        }
        registry.registerBeanDefinition(name, definition)
    }
}
