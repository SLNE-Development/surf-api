package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import dev.slne.surf.api.spring.SurfLifecycle
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.core.annotation.AnnotationAwareOrderComparator

/**
 * Runs the [SurfLifecycle] and [SurfBootstrapLifecycle] hooks of one context.
 *
 * The driver is installed as a bean post-processor before the context refreshes. It records the
 * name of every singleton hook bean when that bean finishes initializing, which is after all of its
 * dependencies did, so the recorded order is a valid dependency order. Hooks are invoked on the
 * bean the context hands out, so proxies created by later post-processors are honored.
 */
internal class LifecycleHookRunner(private val owner: String) : BeanPostProcessor {

    private val log = LogFactory.getLog(LifecycleHookRunner::class.java)
    private val initializationOrder = ArrayList<String>()
    private lateinit var beanFactory: ConfigurableListableBeanFactory

    /**
     * Attaches the driver to [beanFactory]; call it before the context refreshes.
     */
    fun install(beanFactory: ConfigurableListableBeanFactory) {
        this.beanFactory = beanFactory
        beanFactory.addBeanPostProcessor(this)
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        val isHookBean = bean is SurfLifecycle || bean is SurfBootstrapLifecycle

        if (isHookBean && beanFactory.containsBean(beanName) && beanFactory.isSingleton(beanName)) {
            synchronized(initializationOrder) {
                initializationOrder += beanName
            }
        }

        return bean
    }

    /**
     * Runs [SurfBootstrapLifecycle.onBootstrap] on every participant.
     */
    suspend fun bootstrap() {
        for (lifecycle in participants<SurfBootstrapLifecycle>()) {
            lifecycle.onBootstrap()
        }
    }

    /**
     * Runs [SurfLifecycle.onLoad] on every participant.
     */
    suspend fun load() {
        for (lifecycle in participants<SurfLifecycle>()) {
            lifecycle.onLoad()
        }
    }

    /**
     * Runs [SurfLifecycle.onEnable] on every participant.
     */
    suspend fun enable() {
        for (lifecycle in participants<SurfLifecycle>()) {
            lifecycle.onEnable()
        }
    }

    /**
     * Runs [SurfLifecycle.onDisable] on every participant in reverse order.
     *
     * A failing hook is logged and does not stop the remaining hooks, so every bean gets the chance
     * to release what it holds.
     */
    suspend fun disable() {
        participants<SurfLifecycle>().asReversed()
            .forEachLoggingFailures(::logFailure) { it.onDisable() }
    }

    private fun logFailure(participant: SurfLifecycle, exception: Throwable) {
        log.error("onDisable of ${participant.javaClass.name} in plugin '$owner' failed", exception)
    }

    private inline fun <reified T : Any> participants(): List<T> {
        val names = synchronized(initializationOrder) { initializationOrder.toList() }

        return names.asSequence()
            .map(beanFactory::getBean)
            .filterIsInstance<T>()
            .sortedWith(AnnotationAwareOrderComparator.INSTANCE)
            .toList()
    }
}
