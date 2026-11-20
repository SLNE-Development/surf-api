package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.PluginApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.AutowireCandidateQualifier
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.Lifecycle
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.util.ClassUtils

/**
 * One bean that a plugin exports to the plugins depending on it.
 *
 * It carries everything a consumer's import context needs to treat the bean like one of its own
 * candidates — its full generic type and the provider's qualifier, primary and fallback metadata —
 * without ever owning it. The instance is fetched from the provider's context on first use.
 *
 * @property provider the name of the exporting plugin
 * @property beanName the bean's name in the provider's context
 * @property type the bean's type including generics, used for generic autowiring in consumers
 */
class PluginExport internal constructor(
    val provider: String,
    val beanName: String,
    val type: ResolvableType,
    private val primary: Boolean,
    private val fallback: Boolean,
    private val defaultCandidate: Boolean,
    private val qualifier: String?,
    private val owner: ConfigurableApplicationContext,
    private val ownerClassLoader: ClassLoader,
    internal val ownReference: Boolean = false,
) {

    /**
     * The name under which consumers see this bean.
     *
     * A `/` cannot occur in a JVM parameter name, so Spring's fallback of matching a constructor
     * parameter's name against bean names can never pick an export silently.
     */
    val importName: String get() = "$IMPORT_PREFIX$provider/$beanName"

    /**
     * The bean as the provider's context hands it out, including any proxy.
     *
     * The provider creates it on first call, with its own class loader as thread context class
     * loader.
     *
     * @throws IllegalStateException if the provider's context is closed
     */
    fun instance(): Any {
        check(owner.isActive) {
            "Plugin '$provider' is closed; its export '$beanName' is no longer available"
        }
        return withContextClassLoader(ownerClassLoader) { owner.beanFactory.getBean(beanName) }
    }

    internal fun toBeanDefinition(): RootBeanDefinition = RootBeanDefinition().apply {
        setTargetType(type)
        isPrimary = primary
        isFallback = fallback
        isDefaultCandidate = defaultCandidate
        isLazyInit = true
        isSynthetic = true
        qualifier?.let { addQualifier(AutowireCandidateQualifier(Qualifier::class.java, it)) }
    }

    override fun toString() = "PluginExport($importName: $type)"

    companion object {

        /**
         * The prefix of every [importName].
         */
        const val IMPORT_PREFIX = "import/"

        /**
         * The exporting plugin encoded in a bean name, or `null` for a bean that is not an export.
         */
        fun providerOf(beanName: String): String? {
            return if (beanName.startsWith(IMPORT_PREFIX)) {
                beanName.removePrefix(IMPORT_PREFIX).substringBefore('/')
            } else {
                null
            }
        }

        /**
         * Checks whether [beanNames] contain exports of more than one plugin.
         */
        fun hasMultipleProviders(beanNames: Collection<String>): Boolean {
            return beanNames.mapNotNullTo(HashSet(), ::providerOf).size > 1
        }
    }
}

/**
 * Collects the beans a plugin exports from one of its refreshed contexts.
 *
 * Only beans defined in [context] itself are considered, so the exports of the plugin's own
 * dependencies are never passed on. Beans that are no autowire candidates are not exported.
 * Only a plain `@Qualifier` value travels with an export; custom qualifier annotations do not.
 *
 * @throws IllegalStateException if an exported bean implements [Lifecycle] or [ApplicationListener]
 */
internal fun collectExports(
    provider: String,
    context: ConfigurableApplicationContext,
    classLoader: ClassLoader,
): List<PluginExport> {
    val beanFactory = context.beanFactory
    return beanFactory.beanDefinitionNames.mapNotNull { name ->
        exportedBeanType(beanFactory, name)?.let { type ->
            createExport(provider, context, classLoader, name, type)
        }
    }
}

/**
 * Collects references to every application bean of a plugin's bootstrap context, so that the
 * plugin's full context, which sits below a different parent, can inject them through its import
 * context.
 *
 * The references point at the same instances; the bootstrap context keeps owning them.
 */
internal fun collectReferences(
    owner: String,
    context: ConfigurableApplicationContext,
    classLoader: ClassLoader,
): List<PluginExport> {
    val beanFactory = context.beanFactory
    return beanFactory.beanDefinitionNames
        .filter { name -> isCandidate(beanFactory.getMergedBeanDefinition(name)) }
        .mapNotNull { name ->
            beanFactory.getType(name, false)?.let { type ->
                createExport(owner, context, classLoader, name, type, ownReference = true)
            }
        }
}

private fun isCandidate(definition: BeanDefinition): Boolean {
    return !definition.isAbstract &&
            definition.isSingleton &&
            definition.isAutowireCandidate &&
            definition.role == BeanDefinition.ROLE_APPLICATION
}

private fun exportedBeanType(
    beanFactory: ConfigurableListableBeanFactory,
    name: String
): Class<*>? {
    if (!isCandidate(beanFactory.getMergedBeanDefinition(name))) return null

    val type = beanFactory.getType(name, false) ?: return null
    return type.takeIf { type ->
        ClassUtils.getAllInterfacesForClassAsSet(type).any { api -> api.isPluginApi() }
    }
}

private fun Class<*>.isPluginApi(): Boolean {
    return MergedAnnotations.from(this, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
        .isPresent(PluginApi::class.java)
}

private fun createExport(
    provider: String,
    context: ConfigurableApplicationContext,
    classLoader: ClassLoader,
    name: String,
    type: Class<*>,
    ownReference: Boolean = false,
): PluginExport {
    if (!ownReference) checkNotSelfManaged(provider, name, type)
    val beanFactory = context.beanFactory
    val definition = beanFactory.getMergedBeanDefinition(name)

    return PluginExport(
        provider = provider,
        beanName = name,
        type = exportedType(definition, type),
        primary = definition.isPrimary,
        fallback = definition.isFallback,
        defaultCandidate = (definition as? AbstractBeanDefinition)?.isDefaultCandidate ?: true,
        qualifier = beanFactory.findAnnotationOnBean(name, Qualifier::class.java, false)
            ?.value
            ?.takeIf(String::isNotEmpty),
        owner = context,
        ownerClassLoader = classLoader,
        ownReference = ownReference,
    )
}

/**
 * Rejects an export that a consumer context would start, stop or register a second time.
 */
internal fun checkNotSelfManaged(provider: String, beanName: String, type: Class<*>) {
    check(
        !Lifecycle::class.java.isAssignableFrom(type) &&
                !ApplicationListener::class.java.isAssignableFrom(type)
    ) {
        "Plugin '$provider' exports bean '$beanName', which implements Lifecycle or ApplicationListener. " +
                "A consumer context would start, stop or register it a second time; move that part " +
                "into a bean that implements no @PluginApi interface."
    }
}

private fun exportedType(definition: BeanDefinition, type: Class<*>): ResolvableType {
    val declared = (definition as? RootBeanDefinition)?.resolvableType
    if (declared != null && declared != ResolvableType.NONE) return declared

    return ResolvableType.forClass(type)
}
