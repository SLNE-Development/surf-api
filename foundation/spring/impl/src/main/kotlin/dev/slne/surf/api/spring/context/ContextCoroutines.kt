package dev.slne.surf.api.spring.context

import kotlinx.coroutines.*
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The name of the [CoroutineScope] bean of every plugin context and every root context.
 */
const val COROUTINE_SCOPE_BEAN = "surfCoroutineScope"

/**
 * Creates the coroutine scope of a plugin or of surf-api's root context.
 *
 * The scope carries a [SupervisorJob], so one failing coroutine does not cancel the others, a
 * handler that logs uncaught failures under [owner], [dispatcher] as its default dispatcher,
 * [classLoader] as thread context class loader of every coroutine, and [elements]. The context
 * that owns the scope cancels it once its disable hooks have run.
 */
fun surfCoroutineScope(
    owner: String,
    classLoader: ClassLoader,
    dispatcher: CoroutineDispatcher,
    elements: CoroutineContext = EmptyCoroutineContext,
): CoroutineScope {
    val log = LogFactory.getLog("surf-coroutines")
    val handler = CoroutineExceptionHandler { _, failure ->
        log.error("Unhandled exception in a coroutine of '$owner'", failure)
    }

    return CoroutineScope(
        SupervisorJob() + dispatcher + CoroutineName("surf-$owner") + handler +
            ContextClassLoaderElement(classLoader) + elements,
    )
}

/**
 * The elements of [scope] that lifecycle hooks run with: everything except its job, its
 * dispatcher and its exception handler, so a hook stays part of the caller's coroutine.
 */
internal fun hookElements(scope: CoroutineScope): CoroutineContext {
    return scope.coroutineContext
        .minusKey(Job)
        .minusKey(ContinuationInterceptor)
        .minusKey(CoroutineExceptionHandler)
}

/**
 * Registers [scope] as the context's [CoroutineScope] bean.
 *
 * A [primary] scope wins over the root context's scope, which every plugin context sees as an
 * ancestor. The context never cancels the scope on its own; its owner does.
 */
internal fun registerCoroutineScope(
    context: AnnotationConfigApplicationContext,
    scope: CoroutineScope,
    primary: Boolean,
) {
    val definition = RootBeanDefinition(CoroutineScope::class.java).apply {
        setInstanceSupplier { scope }
        isPrimary = primary
    }
    context.registerBeanDefinition(COROUTINE_SCOPE_BEAN, definition)
}
