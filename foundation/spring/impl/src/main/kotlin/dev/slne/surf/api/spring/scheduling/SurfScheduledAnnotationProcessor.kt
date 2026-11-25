package dev.slne.surf.api.spring.scheduling

import dev.slne.surf.api.spring.context.ContextPhaseListener
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.event.ApplicationContextEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.KotlinDetector
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.scheduling.config.TaskSchedulerRouter
import org.springframework.util.ReflectionUtils
import org.springframework.util.StringValueResolver
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.time.Clock
import java.util.*
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Runs the `@Scheduled` methods of one plugin context on the context's coroutine schedulers.
 *
 * Methods that do not suspend go through Spring's own processing, with a router that sends a
 * method without `scheduler` to the context's default scheduler and one with `scheduler` to the
 * named [TaskScheduler] bean. Suspending methods are handled here instead of by Spring, which
 * would need Reactor and run them outside the plugin's scope: each becomes a loop on a
 * [CoroutineTaskScheduler], so a run starts only after the previous one completed. A suspending
 * method must take no parameters, and its `scheduler` must name one of the context's coroutine
 * schedulers.
 *
 * No task starts during the refresh. [start] starts every task once the plugin is enabled,
 * through [afterEnable]; a bean created later starts its tasks right away. [beforeDisable] stops
 * every task before the first disable hook runs, and destroying a bean cancels its tasks.
 */
internal class SurfScheduledAnnotationProcessor :
    ScheduledAnnotationBeanPostProcessor(),
    ContextPhaseListener {

    private val lock = Any()
    private lateinit var beanFactory: BeanFactory
    private var applicationContext: ApplicationContext? = null
    private lateinit var router: DefaultSchedulerRouter
    private var valueResolver: StringValueResolver? = null
    private var isStarting = false
    private var isStarted = false
    private val pendingTasks = ArrayList<SuspendingTask>()
    private val suspendingJobs = IdentityHashMap<Any, MutableList<ScheduledJob>>()

    override fun setBeanFactory(beanFactory: BeanFactory) {
        super.setBeanFactory(beanFactory)
        this.beanFactory = beanFactory

        router = DefaultSchedulerRouter(beanFactory)
        router.setBeanFactory(beanFactory)
        setScheduler(router)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        super.setApplicationContext(applicationContext)
        this.applicationContext = applicationContext
    }

    override fun setEmbeddedValueResolver(resolver: StringValueResolver) {
        super.setEmbeddedValueResolver(resolver)
        valueResolver = resolver
    }

    /**
     * Registers the task of [method], or fails if its schedule or `scheduler` is invalid.
     *
     * Both kinds of method fail here, during the refresh, rather than when the tasks start.
     */
    override fun processScheduled(scheduled: Scheduled, method: Method, bean: Any) {
        if (!KotlinDetector.isSuspendingFunction(method)) {
            validated(method) { checkSync(scheduled) }
            super.processScheduled(scheduled, method, bean)
            return
        }

        val task = validated(method) { suspendingTask(scheduled, method, bean) }
        if (task != null) {
            schedule(task)
        }
    }

    /**
     * Holds back the refresh event, which would start Spring's tasks during the refresh; [start]
     * replays it.
     */
    override fun onApplicationEvent(event: ApplicationContextEvent) {
        val isOwnRefresh = event is ContextRefreshedEvent &&
            event.applicationContext === applicationContext
        if (isOwnRefresh) return

        super.onApplicationEvent(event)
    }

    /**
     * Starts every task found so far; later tasks start as soon as their bean is created. Calls
     * after the first do nothing.
     */
    fun start() {
        val context = checkNotNull(applicationContext) { "The processor has no context" }
        synchronized(lock) {
            if (isStarting) return

            isStarting = true
        }

        super.onApplicationEvent(ContextRefreshedEvent(context))

        val tasks = synchronized(lock) {
            isStarted = true
            pendingTasks.toList().also { pendingTasks.clear() }
        }

        for (task in tasks) {
            launch(task)
        }
    }

    override suspend fun afterEnable() {
        start()
    }

    override suspend fun beforeDisable() {
        beanFactory.getBean<PluginScheduling>(SCHEDULING_BEAN).stop()
    }

    override fun postProcessBeforeDestruction(bean: Any, beanName: String) {
        super.postProcessBeforeDestruction(bean, beanName)
        cancelSuspendingJobs(AopProxyUtils.ultimateSingletonTarget(bean))
    }

    override fun requiresDestruction(bean: Any): Boolean {
        val key = AopProxyUtils.ultimateSingletonTarget(bean)
        val hasSuspendingJobs = synchronized(lock) {
            suspendingJobs.containsKey(key) || pendingTasks.any { task -> task.key === key }
        }

        return hasSuspendingJobs || super.requiresDestruction(bean)
    }

    override fun destroy() {
        super.destroy()

        val jobs = synchronized(lock) {
            suspendingJobs.values.flatten().also { suspendingJobs.clear() }
        }

        for (job in jobs) {
            job.cancel(false)
        }
    }

    /**
     * Checks what Spring only checks once the tasks start: the schedule's intervals and the
     * scheduler that `scheduler` names.
     */
    private fun checkSync(scheduled: Scheduled) {
        scheduled.toTaskSchedule(valueResolver)
        val name = valueResolver.resolveAttribute(scheduled.scheduler) ?: return
        router.qualifiedScheduler(name)
    }

    /**
     * The task of a suspending method, or `null` if its cron expression disables it.
     */
    private fun suspendingTask(scheduled: Scheduled, method: Method, bean: Any): SuspendingTask? {
        check(method.parameterCount == 1) {
            "The suspending @Scheduled method '${method.name}' must not take parameters"
        }

        val schedule = scheduled.toTaskSchedule(valueResolver) ?: return null
        val scheduler = coroutineScheduler(scheduled.scheduler, method)
        val invocable = AopUtils.selectInvocableMethod(method, bean.javaClass)
        ReflectionUtils.makeAccessible(invocable)

        return SuspendingTask(
            key = AopProxyUtils.ultimateSingletonTarget(bean),
            scheduler = scheduler,
            schedule = schedule,
            name = "${method.declaringClass.name}.${method.name}",
        ) { invokeSuspending(invocable, bean) }
    }

    private fun coroutineScheduler(qualifier: String, method: Method): CoroutineTaskScheduler {
        val name = valueResolver.resolveAttribute(qualifier)
        val scheduler = if (name == null) {
            beanFactory.getBean<TaskScheduler>(PLUGIN_SCHEDULER_BEAN)
        } else {
            BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                beanFactory,
                TaskScheduler::class.java,
                name,
            )
        }

        return scheduler as? CoroutineTaskScheduler ?: throw IllegalStateException(
            "The scheduler '$name' of the suspending @Scheduled method '${method.name}' is not " +
                "one of surf-api's coroutine schedulers",
        )
    }

    private fun schedule(task: SuspendingTask) {
        synchronized(lock) {
            if (!isStarted) {
                pendingTasks += task
                return
            }
        }

        launch(task)
    }

    private fun launch(task: SuspendingTask) {
        val job = task.scheduler.launch(task.schedule, task.name, task.action)
        synchronized(lock) {
            suspendingJobs.getOrPut(task.key) { ArrayList() } += job
        }
    }

    private fun cancelSuspendingJobs(key: Any) {
        val jobs = synchronized(lock) {
            pendingTasks.removeIf { task -> task.key === key }
            suspendingJobs.remove(key)
        } ?: return

        for (job in jobs) {
            job.cancel(false)
        }
    }

    private class SuspendingTask(
        val key: Any,
        val scheduler: CoroutineTaskScheduler,
        val schedule: TaskSchedule,
        val name: String,
        val action: suspend () -> Unit,
    )
}

/**
 * Routes Spring's tasks by their `scheduler` attribute, and those without one to the context's
 * default scheduler instead of Spring's search for a unique or `taskScheduler` bean.
 *
 * Its clock is the default scheduler's, because Spring computes start times from it.
 */
private class DefaultSchedulerRouter(private val beanFactory: BeanFactory) : TaskSchedulerRouter() {

    override fun determineDefaultScheduler(): TaskScheduler {
        return beanFactory.getBean<TaskScheduler>(PLUGIN_SCHEDULER_BEAN)
    }

    override fun getClock(): Clock = determineDefaultScheduler().clock

    /**
     * The scheduler that the `scheduler` attribute [qualifier] selects.
     *
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no scheduler
     * matches [qualifier]
     */
    fun qualifiedScheduler(qualifier: String): TaskScheduler {
        return determineQualifiedScheduler(qualifier)
    }
}

/**
 * The result of [block], with an [IllegalArgumentException] it throws reported as an invalid
 * `@Scheduled` [method].
 */
private inline fun <T> validated(method: Method, block: () -> T): T {
    try {
        return block()
    } catch (exception: IllegalArgumentException) {
        throw IllegalStateException(
            "Encountered invalid @Scheduled method '${method.name}': ${exception.message}",
            exception,
        )
    }
}

private suspend fun invokeSuspending(method: Method, target: Any) {
    try {
        suspendCoroutineUninterceptedOrReturn<Any?> { continuation ->
            method.invoke(target, continuation)
        }
    } catch (exception: InvocationTargetException) {
        throw exception.targetException
    }
}
