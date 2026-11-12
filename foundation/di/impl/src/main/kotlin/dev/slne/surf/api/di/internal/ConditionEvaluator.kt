package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.condition.BeanCondition
import dev.slne.surf.api.di.condition.BeanConditionContext
import dev.slne.surf.api.di.definition.BeanConditions
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.PropertyCondition
import java.lang.reflect.Modifier
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads one configuration value.
 *
 * The evaluator never opens a file itself: configuration formats and their caching belong to the
 * platform side, and keeping the access behind this one function is what lets the evaluator be
 * tested without a data directory on disk.
 *
 * @see ConditionEvaluator
 */
internal fun interface PropertyReader {

    /**
     * Returns the value at [key] in [file] under [dataPath], or `null` when it is not set.
     *
     * @param key path to the value, segment by segment
     * @param file configuration file relative to [dataPath]; empty means the default one
     */
    fun read(dataPath: Path, key: List<String>, file: String): String?
}

/**
 * The answer to "may this bean be created".
 *
 * A rejection carries a reason instead of being logged, because the evaluator cannot know what a
 * rejection means: a bean nobody needs is skipped with a debug line, while the same rejection
 * becomes a failure the moment an injection point requires that bean. The caller decides, and it
 * can only write a readable failure if the reason travelled with the decision.
 */
internal sealed interface Decision {

    /** Whether the bean may be created. */
    val accepted: Boolean

    /** Why the bean may not be created, or `null` when it may. */
    val reason: String?

    /** Every condition held. */
    data object Accepted : Decision {
        override val accepted get() = true
        override val reason: String? get() = null
    }

    /**
     * One condition did not hold.
     *
     * [reason] names what failed and reads as the continuation of a sentence about the bean, so a
     * caller can write `"$beanId skipped: ${decision.reason}"` without reformatting it.
     */
    data class Rejected(override val reason: String) : Decision {
        override val accepted get() = false
    }
}

/**
 * Decides whether one bean's conditions hold.
 *
 * Every family in [BeanConditions] is independent and all of them have to pass, so the first one
 * that fails ends the evaluation: nothing after it can change the answer, and a condition that is
 * never reached costs nothing — which matters because loading a class or running a user-written
 * condition is the expensive part.
 *
 * The evaluator decides about one bean at a time and looks at nothing else in the container, which
 * is why the set of types other beans provide arrives as a predicate rather than being read from a
 * registry here.
 *
 * @param platform where classes, plugins, the data directory and the environments come from
 * @param owner the application the container belongs to
 * @param container the container being built, handed to custom conditions
 * @param propertyReader how `@ConditionalOnProperty` reads a configuration value
 */
internal class ConditionEvaluator(
    private val platform: ContainerPlatform,
    private val owner: Any,
    private val container: BeanContainer,
    private val propertyReader: PropertyReader,
) {

    /**
     * Returns whether [definition] may be created.
     *
     * [isProvided] answers whether some *other* bean provides a type, for
     * `@ConditionalOnMissingBean`. The caller supplies it because only it knows the full set of
     * providers, and it has to exclude [definition]'s own providers — a bean guarded on a type it
     * provides itself would otherwise always reject.
     *
     * Note that [BeanConditions.dependsOnBeans] is deliberately not evaluated here. It expresses
     * creation order, not a precondition: the bean is created either way, only later than the bean
     * it names. Ordering is the graph's job, and treating it as a condition here would silently skip
     * beans whose ordering hint names a type nothing provides.
     */
    suspend fun evaluate(definition: BeanDefinition, isProvided: (String) -> Boolean): Decision {
        val conditions = definition.conditions

        val reason = missingClasses(conditions)
            ?: missingPlugins(conditions)
            ?: unsatisfiedPluginGroup(conditions)
            ?: unmatchedEnvironment(conditions)
            ?: occupiedType(conditions, isProvided)
            ?: unmatchedProperty(conditions)
            ?: unsatisfiedCondition(conditions, definition.className)

        return if (reason == null) Decision.Accepted else Decision.Rejected(reason)
    }

    /**
     * Reports the classes from [BeanConditions.classNames] that the application cannot load.
     *
     * The class is resolved with `initialize = false`: the condition asks whether an optional
     * library is present, and running a static initialiser to find that out would be a side effect
     * of merely asking. All missing classes are collected rather than just the first, because a bean
     * guarded on several of them is usually missing the whole library and naming one class sends the
     * reader after the wrong dependency.
     */
    private fun missingClasses(conditions: BeanConditions): String? {
        if (conditions.classNames.isEmpty()) return null

        val classLoader = platform.classLoader(owner)
        val missing = conditions.classNames.filterNot { className ->
            try {
                Class.forName(className, false, classLoader)
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: LinkageError) {
                // The class exists but cannot be linked, usually because its own supertypes are
                // absent. It is unusable either way, so it counts as missing rather than as an error.
                false
            }
        }

        if (missing.isEmpty()) return null

        return "${if (missing.size == 1) "class" else "classes"} ${missing.joinToString()} " +
                "${if (missing.size == 1) "is" else "are"} not on the classpath"
    }

    /** Reports the plugins from [BeanConditions.pluginIds] that are not loaded; all have to be. */
    private fun missingPlugins(conditions: BeanConditions): String? {
        val missing = conditions.pluginIds.filterNot(platform::isPluginLoaded)

        if (missing.isEmpty()) return null

        return "${if (missing.size == 1) "plugin" else "plugins"} " +
                "${missing.joinToString { "'$it'" }} ${if (missing.size == 1) "is" else "are"} not loaded"
    }

    /**
     * Reports the first group of [BeanConditions.anyPluginIds] in which no plugin is loaded.
     *
     * An empty group imposes nothing. It cannot be written down in source — `@ConditionalOnAnyPlugin`
     * always names at least one plugin — so an empty list in metadata means "nothing was asked for"
     * and must not be read as "nothing can satisfy this".
     */
    private fun unsatisfiedPluginGroup(conditions: BeanConditions): String? {
        val unsatisfied = conditions.anyPluginIds
            .firstOrNull { group -> group.isNotEmpty() && group.none(platform::isPluginLoaded) }
            ?: return null

        return "none of the plugins ${unsatisfied.joinToString { "'$it'" }} is loaded"
    }

    /**
     * Reports the first group of [BeanConditions.environments] the process does not run in.
     *
     * Each group narrows further: one of its environments has to match, and every group has to
     * match on its own, which is what makes the repeatable annotation an intersection of unions.
     * Matching ignores case so `PRODUCTION` and `production` name the same environment, and an
     * empty group imposes nothing for the same reason as in [unsatisfiedPluginGroup].
     */
    private fun unmatchedEnvironment(conditions: BeanConditions): String? {
        if (conditions.environments.isEmpty()) return null

        val current = platform.environments()
        val unsatisfied = conditions.environments
            .firstOrNull { group ->
                group.isNotEmpty() && group.none { wanted ->
                    current.any { it.equals(wanted, ignoreCase = true) }
                }
            }
            ?: return null

        return "the process runs in [${current.joinToString()}], " +
                "not in any of [${unsatisfied.joinToString()}]"
    }

    /**
     * Reports the first type of [BeanConditions.missingBeans] that another bean already provides.
     *
     * This is how a fallback steps aside for a real implementation. Assignability is already baked
     * into the predicate's input: a provider is indexed under every type it provides, supertypes
     * included, so an exact match on the guarded type answers the assignable question too.
     */
    private fun occupiedType(conditions: BeanConditions, isProvided: (String) -> Boolean): String? {
        val occupied = conditions.missingBeans.firstOrNull(isProvided) ?: return null

        return "another bean already provides $occupied"
    }

    /**
     * Reports the first property condition that does not match.
     *
     * An absent value counts as whatever [PropertyCondition.matchIfMissing] says, which is how a
     * feature defaults to on or off without the configuration file having to mention it. A present
     * value matches [PropertyCondition.havingValue] ignoring case, or, when that is empty, matches
     * by being set at all.
     *
     * A reader that fails on a malformed file is left to propagate: that is not this bean's
     * condition failing but the configuration being unreadable, and silently skipping every bean
     * guarded on that file would hide it.
     */
    private fun unmatchedProperty(conditions: BeanConditions): String? {
        if (conditions.properties.isEmpty()) return null

        val dataPath = platform.dataPath(owner)

        conditions.properties.forEach { condition ->
            val value = propertyReader.read(dataPath, condition.key, condition.file)
            val matches = when {
                value == null -> condition.matchIfMissing
                condition.havingValue.isEmpty() -> true
                else -> value.equals(condition.havingValue, ignoreCase = true)
            }

            if (!matches) return describeMismatch(condition, value)
        }

        return null
    }

    /** Spells out which property was expected to hold what, and what it actually held. */
    private fun describeMismatch(condition: PropertyCondition, value: String?): String {
        val key = condition.key.joinToString(".")
        val where = if (condition.file.isEmpty()) {
            "property '$key'"
        } else {
            "property '$key' in '${condition.file}'"
        }

        // A missing value only reaches here when matchIfMissing is false, and a present one only
        // when havingValue is set and differs, so those two cases describe every rejection.
        return if (value == null) {
            "$where is not set"
        } else {
            "$where is '$value', not '${condition.havingValue}'"
        }
    }

    /**
     * Reports the first custom condition that does not hold, or cannot be run at all.
     *
     * A condition that throws rejects the bean, with the throwable named in the reason, rather than
     * letting the exception escape: one broken condition would otherwise take the whole container
     * down, and the bean it guards is the one piece that is definitely not safe to create — nothing
     * said it may be. The reason carries the throwable so the broken condition is still findable.
     *
     * Cancellation is not a failing condition and is rethrown, so a container build that is being
     * cancelled does not turn into a pile of rejected beans.
     */
    private suspend fun unsatisfiedCondition(
        conditions: BeanConditions,
        beanClassName: String,
    ): String? {
        if (conditions.customConditions.isEmpty()) return null

        val classLoader = platform.classLoader(owner)
        val context = BeanConditionContext(
            owner = owner,
            container = container,
            beanClassName = beanClassName,
        )

        conditions.customConditions.forEach { conditionClassName ->
            rejectionFrom(conditionClassName, classLoader, context)?.let { return it }
        }

        return null
    }

    /** Runs one condition, returning why it rejected the bean, or `null` when it held. */
    private suspend fun rejectionFrom(
        conditionClassName: String,
        classLoader: ClassLoader,
        context: BeanConditionContext,
    ): String? {
        val holds = try {
            val condition = instantiate(conditionClassName, classLoader)
                ?: return "condition $conditionClassName does not implement BeanCondition"

            condition.evaluate(context)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return "condition $conditionClassName failed to evaluate: $failure"
        } catch (failure: LinkageError) {
            return "condition $conditionClassName failed to evaluate: $failure"
        }

        return if (holds) null else "condition $conditionClassName does not hold"
    }

    /**
     * Creates the condition named by [className], or returns `null` when the class is not one.
     *
     * A condition declared as an `object` is used as the object itself; anything else is created
     * through its no-argument constructor, which is made accessible first so that a condition
     * written with a private or internal constructor still works. Conditions are asked a question
     * and hold no state, so creating one per bean is cheaper than caching them would be worth.
     */
    private fun instantiate(className: String, classLoader: ClassLoader): BeanCondition? {
        val conditionClass = Class.forName(className, false, classLoader)

        // A condition is usually an `object`, and often a private one declared next to the bean it
        // guards — which is the readable way to write one. `KClass.objectInstance` refuses a private
        // object, so the INSTANCE field is read directly instead.
        conditionClass.declaredFields
            .find { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
            ?.let { field ->
                field.trySetAccessible()
                return field.get(null) as? BeanCondition
            }

        val constructor = conditionClass.getDeclaredConstructor()
        constructor.trySetAccessible()

        return constructor.newInstance() as? BeanCondition
    }
}
