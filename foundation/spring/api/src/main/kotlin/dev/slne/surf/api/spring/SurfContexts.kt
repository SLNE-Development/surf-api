package dev.slne.surf.api.spring

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils
import org.springframework.context.ApplicationContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Gives surf-api's static entry points access to its root Spring context.
 *
 * The platform installs the root context and uninstalls it when it shuts down. Plugin code injects
 * beans instead of reading them from here.
 */
@InternalSurfApi
object SurfContexts {

    @Volatile
    private var installed: ApplicationContext? = null

    /**
     * surf-api's root context.
     *
     * @throws IllegalStateException if the platform has not installed it yet
     */
    val root: ApplicationContext
        get() = checkNotNull(installed) {
            "surf-api's root context has not been started yet. A bean was asked for while surf-api " +
                "was still loading; read it lazily, where it is used."
        }

    /**
     * The root context, or `null` before it is installed.
     */
    val rootOrNull: ApplicationContext? get() = installed

    /**
     * Installs [root]; called once by the platform, before the refresh when beans that read it are
     * created during the refresh, otherwise after it.
     */
    fun install(root: ApplicationContext) {
        check(installed == null) { "surf-api's root context is already installed" }

        installed = root
    }

    /**
     * Removes the installed root context when the platform shuts down.
     */
    fun uninstall() {
        installed = null
    }
}

/**
 * A property that holds a bean of surf-api's root Spring context, resolved on first read.
 *
 * The form to use in a static entry point of surf-api, as a companion property of the bean's type:
 *
 * ```
 * companion object {
 *     @InternalSurfApi
 *     val instance by surfBean<SurfPaperNmsNbtBridge>()
 * }
 * ```
 *
 * Reading the bean on first access rather than when the declaring class loads keeps the order of
 * unrelated class loads out of the behavior. The property re-resolves the bean whenever the
 * platform installs another root context, so it always reads the bean the current root's own beans
 * inject. While no root context is installed, it keeps returning the bean it resolved last.
 *
 * @param qualifier the bean's `@Qualifier` value or name, or `null` for the only bean of its type
 */
@InternalSurfApi
inline fun <reified T : Any> surfBean(qualifier: String? = null): ReadOnlyProperty<Any?, T> {
    return surfBeanProperty(T::class.java, qualifier)
}

/**
 * Creates the property behind [surfBean]. Creating it reads no root context.
 *
 * @param type the bean's type
 * @param qualifier the bean's `@Qualifier` value or name, or `null` for the only bean of its type
 */
@InternalSurfApi
fun <T : Any> surfBeanProperty(type: Class<T>, qualifier: String?): ReadOnlyProperty<Any?, T> {
    return RootBeanProperty(type, qualifier)
}

/**
 * Caches one bean together with the root context it was read from.
 *
 * The pair is published through a single volatile field, so a reader never combines a bean with
 * the root of another generation. Two threads may resolve the bean at once after a new root is
 * installed; both get a bean of that root and the later write wins.
 */
private class RootBeanProperty<T : Any>(
    private val type: Class<T>,
    private val qualifier: String?,
) : ReadOnlyProperty<Any?, T> {

    private class Resolved<T>(val root: ApplicationContext, val bean: T)

    @Volatile
    private var resolved: Resolved<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val cached = resolved
        val installed = SurfContexts.rootOrNull

        if (cached != null && (installed == null || installed === cached.root)) {
            return cached.bean
        }

        val root = installed ?: SurfContexts.root
        val fresh = Resolved(root, resolve(root))
        resolved = fresh

        return fresh.bean
    }

    private fun resolve(root: ApplicationContext): T {
        if (qualifier == null) {
            return root.getBean(type)
        }

        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(
            root.autowireCapableBeanFactory,
            type,
            qualifier,
        )
    }
}
