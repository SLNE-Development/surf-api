package dev.slne.surf.api.spring.context

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Sets the thread context class loader to [classLoader] on every thread a coroutine resumes on, and
 * restores the previous loader when the coroutine suspends.
 *
 * Spring and the libraries it integrates look up classes and resources through the context class
 * loader; on a server thread that loader is the server's, which cannot see a plugin's classes.
 */
@InternalSurfApi
class ContextClassLoaderElement(
    private val classLoader: ClassLoader,
) : ThreadContextElement<ClassLoader?>, AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<ContextClassLoaderElement>

    override fun updateThreadContext(context: CoroutineContext): ClassLoader? {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = classLoader
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: ClassLoader?) {
        Thread.currentThread().contextClassLoader = oldState
    }
}

/**
 * Runs [block] with the thread context class loader set to [classLoader], and restores the previous
 * loader afterwards, also when [block] throws.
 */
@InternalSurfApi
inline fun <T> withContextClassLoader(classLoader: ClassLoader, block: () -> T): T {
    val thread = Thread.currentThread()
    val previous = thread.contextClassLoader
    thread.contextClassLoader = classLoader
    try {
        return block()
    } finally {
        thread.contextClassLoader = previous
    }
}
