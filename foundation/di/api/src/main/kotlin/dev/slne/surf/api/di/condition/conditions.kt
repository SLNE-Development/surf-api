package dev.slne.surf.api.di.condition

import dev.slne.surf.api.di.BeanContainer
import kotlin.reflect.KClass

/**
 * Decides at startup whether a bean is created at all.
 *
 * A bean whose condition does not hold is skipped: it is never constructed, never injected, and an
 * injection point that requires it fails with a message naming the condition. That is deliberate —
 * a missing bean is a configuration error, not something to paper over.
 *
 * Implementations need a constructor the container can call with no arguments; a condition runs
 * before the beans it decides about exist, so it cannot inject them. Use [BeanConditionContext] to
 * reach what it may legitimately look at.
 */
interface BeanCondition {

    /** Returns whether the bean this condition guards should be created. */
    suspend fun evaluate(context: BeanConditionContext): Boolean
}

/**
 * What a [BeanCondition] may look at while deciding.
 *
 * @property owner the application the container belongs to — a plugin instance, a microservice
 * @property container the container being built, for asking what is already known
 * @property beanClassName the bean the condition guards, for messages
 */
class BeanConditionContext(
    val owner: Any,
    val container: BeanContainer,
    val beanClassName: String,
)

/**
 * Creates the bean only if every [condition] holds.
 *
 * Repeatable; the conditions are independent and all have to hold.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOn(val condition: KClass<out BeanCondition>)

/**
 * Creates the bean only if the configuration property at [key] matches.
 *
 * [key] is the path to the value, segment by segment. With [havingValue] set, the value has to equal
 * it, ignoring case; without it, the property only has to be present. [matchIfMissing] decides what
 * an absent property means, which is how a feature defaults to on or off. [file] names the
 * configuration file relative to the application's data directory; empty means the default one.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnProperty(
    val key: Array<String>,
    val havingValue: String = "",
    val matchIfMissing: Boolean = false,
    val file: String = "",
)

/**
 * Creates the bean only if the process runs in one of [environments].
 *
 * Repeatable, and each annotation has to match on its own, so several of them narrow the set.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnEnvironment(val environments: Array<String>)

/**
 * Creates the bean only if no other bean provides [type].
 *
 * This is how a default is written: mark the fallback with it, and any real implementation that
 * appears takes over without the fallback having to know about it. Assignability decides, so a
 * fallback guarded on an interface steps aside for anything implementing it.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnMissingBean(val type: KClass<*>)

/**
 * Creates the bean only if [clazz] is on the runtime classpath.
 *
 * Use it for an integration with an optional library. The annotation itself needs the class at
 * compile time; when even that is not available, use [ConditionalOnClassName].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnClass(val clazz: KClass<*>)

/**
 * Creates the bean only if a class named [className] is on the runtime classpath.
 *
 * The string form of [ConditionalOnClass], for a class that is not on the compile classpath either.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnClassName(val className: String)

/**
 * Creates the bean only if the platform has a plugin with id [pluginId] loaded.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnPlugin(val pluginId: String)

/**
 * Creates the bean only if at least one plugin out of [pluginIds] is loaded.
 *
 * For an integration that works with any of several interchangeable plugins.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnAnyPlugin(val pluginIds: Array<String>)

/**
 * Orders this bean after the bean providing [type], without injecting it.
 *
 * Constructor parameters already express every dependency the bean actually uses, so this is only
 * for the rarer case where the order matters but the value does not — something has to be set up
 * before this bean starts, and the bean never touches it. Assignability decides, so the type may be
 * an interface.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnBean(val type: KClass<*>)
