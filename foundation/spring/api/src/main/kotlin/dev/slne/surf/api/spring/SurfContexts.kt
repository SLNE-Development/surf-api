package dev.slne.surf.api.spring

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext

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
 * A property that holds a bean of surf-api's root context, resolved the first time it is read.
 *
 * The form to use in surf-api's static entry points:
 *
 * ```
 * private val bridge by surfBean<SurfPaperNmsNbtBridge>()
 * ```
 *
 * Reading the bean on first access rather than when the declaring class loads keeps the order of
 * unrelated class loads out of the behavior.
 *
 * @param qualifier the bean's `@Qualifier` value or name, or `null` for the only bean of its type
 */
@InternalSurfApi
inline fun <reified T : Any> surfBean(qualifier: String? = null): Lazy<T> = lazy {
    val root = SurfContexts.root
    if (qualifier == null) {
        root.getBean<T>()
    } else {
        BeanFactoryAnnotationUtils.qualifiedBeanOfType(
            root.autowireCapableBeanFactory,
            T::class.java,
            qualifier,
        )
    }
}
