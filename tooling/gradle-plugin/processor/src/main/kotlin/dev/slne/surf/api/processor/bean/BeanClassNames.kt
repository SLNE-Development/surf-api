package dev.slne.surf.api.processor.bean

import dev.slne.surf.api.di.Bean
import dev.slne.surf.api.di.BeanMeta
import dev.slne.surf.api.di.BeanPostProcessor
import dev.slne.surf.api.di.Primary
import dev.slne.surf.api.di.Priority
import dev.slne.surf.api.di.Qualifier
import dev.slne.surf.api.di.Repository
import dev.slne.surf.api.di.Service
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.condition.ConditionalOn
import dev.slne.surf.api.di.condition.ConditionalOnAnyPlugin
import dev.slne.surf.api.di.condition.ConditionalOnClass
import dev.slne.surf.api.di.condition.ConditionalOnClassName
import dev.slne.surf.api.di.condition.ConditionalOnEnvironment
import dev.slne.surf.api.di.condition.ConditionalOnMissingBean
import dev.slne.surf.api.di.condition.ConditionalOnPlugin
import dev.slne.surf.api.di.condition.ConditionalOnProperty
import dev.slne.surf.api.di.condition.DependsOnBean
import dev.slne.surf.api.processor.util.nameOf

/**
 * The names [BeanSymbolProcessor] looks for.
 *
 * Every annotation and interface is named through its own class rather than as a literal, so a
 * rename in the container's contracts breaks this file at compile time instead of silently
 * producing metadata with no beans in it.
 */
object BeanClassNames {
    val BEAN_META = nameOf<BeanMeta>()
    val SERVICE = nameOf<Service>()
    val REPOSITORY = nameOf<Repository>()
    val BEAN = nameOf<Bean>()
    val QUALIFIER = nameOf<Qualifier>()
    val PRIMARY = nameOf<Primary>()
    val PRIORITY = nameOf<Priority>()
    val SURF_COMPONENT = nameOf<SurfComponent>()
    val BEAN_POST_PROCESSOR = nameOf<BeanPostProcessor>()

    val CONDITIONAL_ON = nameOf<ConditionalOn>()
    val CONDITIONAL_ON_PROPERTY = nameOf<ConditionalOnProperty>()
    val CONDITIONAL_ON_ENVIRONMENT = nameOf<ConditionalOnEnvironment>()
    val CONDITIONAL_ON_MISSING_BEAN = nameOf<ConditionalOnMissingBean>()
    val CONDITIONAL_ON_CLASS = nameOf<ConditionalOnClass>()
    val CONDITIONAL_ON_CLASS_NAME = nameOf<ConditionalOnClassName>()
    val CONDITIONAL_ON_PLUGIN = nameOf<ConditionalOnPlugin>()
    val CONDITIONAL_ON_ANY_PLUGIN = nameOf<ConditionalOnAnyPlugin>()
    val DEPENDS_ON_BEAN = nameOf<DependsOnBean>()

    /** Every condition annotation, for reporting one that sits where the schema cannot carry it. */
    val CONDITIONS = listOf(
        CONDITIONAL_ON,
        CONDITIONAL_ON_PROPERTY,
        CONDITIONAL_ON_ENVIRONMENT,
        CONDITIONAL_ON_MISSING_BEAN,
        CONDITIONAL_ON_CLASS,
        CONDITIONAL_ON_CLASS_NAME,
        CONDITIONAL_ON_PLUGIN,
        CONDITIONAL_ON_ANY_PLUGIN,
        DEPENDS_ON_BEAN,
    )

    /** The wrapper that turns an injection point into one resolved on first access. */
    const val LAZY = "kotlin.Lazy"

    /** Types a parameter takes every candidate in, as a list. */
    val LIST_TYPES = setOf(
        "kotlin.collections.List",
        "kotlin.collections.MutableList",
        "kotlin.collections.ArrayList",
        "java.util.List",
    )

    /** Types a parameter takes every candidate in, as a set. */
    val SET_TYPES = setOf(
        "kotlin.collections.Set",
        "kotlin.collections.MutableSet",
        "kotlin.collections.HashSet",
        "kotlin.collections.LinkedHashSet",
        "java.util.Set",
    )

    /**
     * Types that hold many values but say nothing about which collection to build.
     *
     * A parameter of one of these is resolved as a single bean of that type, which is almost never
     * what the declaration meant, so it is worth a word at compile time.
     */
    val AMBIGUOUS_COLLECTIONS = setOf(
        "kotlin.Array",
        "kotlin.collections.Collection",
        "kotlin.collections.MutableCollection",
        "kotlin.collections.Iterable",
        "kotlin.collections.MutableIterable",
        "kotlin.sequences.Sequence",
    )

    /** The root of every hierarchy, which no bean is resolved as. */
    val ROOT_TYPES = setOf("kotlin.Any", "java.lang.Object")

    /** Return types that publish no value. */
    val EMPTY_TYPES = setOf("kotlin.Unit", "kotlin.Nothing", "void")
}
