package dev.slne.surf.api.spring

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.core.type.AnnotationMetadata

/**
 * Registers the annotated bean only if a plugin is installed under one of the given names.
 *
 * The names are alternatives for one plugin whose name differs between platforms, such as
 * `LuckPerms` on Paper and `luckperms` on Velocity, so code shared by several platforms lists each
 * one. Repeat the annotation to require several plugins: every repetition must match.
 *
 * ```kotlin
 * @Service
 * @ConditionalOnPlugin("LuckPerms", "luckperms")
 * @ConditionalOnPlugin("PlaceholderAPI")
 * class PlaceholderPermissions : Permissions
 * ```
 *
 * Spring evaluates the condition from bytecode metadata before it loads the annotated class, so a
 * bean that refers to the API of an optional plugin never triggers a `NoClassDefFoundError` when
 * that plugin is missing. Put such references into an adapter bean carrying this annotation, and
 * let the rest of the plugin inject the adapter through an interface of its own.
 *
 * On a `@Bean` method the referenced types must not appear in the method signature of a
 * configuration class that is always loaded; annotate a dedicated configuration class instead.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@JvmRepeatable(ConditionalOnPlugins::class)
@Conditional(OnPluginCondition::class)
annotation class ConditionalOnPlugin(vararg val value: String)

/**
 * Holds repeated [ConditionalOnPlugin] annotations; the compiler writes it, so plugins never do.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Conditional(OnPluginCondition::class)
annotation class ConditionalOnPlugins(vararg val value: ConditionalOnPlugin)

/**
 * Answers whether a plugin with a given name is installed on the running platform.
 *
 * The contract between [OnPluginCondition] and the platforms: each platform registers one instance
 * in every plugin context it builds, under [BEAN_NAME], before the context refreshes. Plugins use
 * [ConditionalOnPlugin] instead.
 */
@InternalSurfApi
fun interface PluginPresence {

    /**
     * Checks whether a plugin named [name] is installed.
     */
    fun isInstalled(name: String): Boolean

    @InternalSurfApi
    companion object {

        /**
         * The bean name under which the platform registers its [PluginPresence].
         */
        const val BEAN_NAME = "surfPluginPresence"
    }
}

/**
 * Evaluates [ConditionalOnPlugin] against the context's [PluginPresence].
 *
 * An annotation type that carries [ConditionalOnPlugin] as a meta-annotation always matches: the
 * condition applies to the classes it annotates, and a component scan may read the annotation type
 * itself in a context without [PluginPresence].
 *
 * Spring instantiates it reflectively; nothing else should.
 */
class OnPluginCondition internal constructor() : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        if (metadata is AnnotationMetadata && metadata.isAnnotation) return true

        val requirements = metadata.getMergedRepeatableAnnotationAttributes(
            ConditionalOnPlugin::class.java,
            ConditionalOnPlugins::class.java,
            false,
        )
        if (requirements.isEmpty()) return true

        val presence = findPresence(context)

        return requirements.all { requirement ->
            val names = requirement.getStringArray("value")
            require(names.isNotEmpty()) { "@ConditionalOnPlugin names no plugin on $metadata" }

            names.any(presence::isInstalled)
        }
    }

    private fun findPresence(context: ConditionContext): PluginPresence {
        val beanFactory = checkNotNull(context.beanFactory) {
            "@ConditionalOnPlugin needs a bean factory to find the platform's PluginPresence"
        }

        check(beanFactory.containsBean(PluginPresence.BEAN_NAME)) {
            "@ConditionalOnPlugin was evaluated in a context without a PluginPresence; " +
                "the platform registers one before every refresh"
        }

        return beanFactory.getBean<PluginPresence>(PluginPresence.BEAN_NAME)
    }
}
