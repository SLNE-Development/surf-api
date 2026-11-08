package dev.slne.surf.api.di.definition

import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.serialization.Serializable

/**
 * Everything the container needs to know about one bean without loading its class.
 *
 * The KSP processor writes these at compile time, one file per module under
 * [BeanMetadata.DIRECTORY]; the container reads every such file it finds on the classpath and
 * merges them. Keeping the whole decision in metadata is what lets the container build the
 * dependency graph, evaluate conditions and report a cycle or an unsatisfiable parameter before it
 * loads a single bean class.
 *
 * @property className binary name of the bean class
 * @property provides every type the bean can be resolved as — its own class and all supertypes
 * except `Any`, which is what makes resolution over an interface work
 * @property stereotypes binary names of the bean-making annotations on the class, so a post
 * processor can find beans by annotation without reflecting over every class
 * @property scope how many instances exist and who sees them
 * @property qualifier the name from `@Qualifier`, or `null`
 * @property primary whether the bean wins when several provide the same type
 * @property priority order among beans the graph leaves unordered, lower first
 * @property lifecycle whether the class implements `SurfComponent` and takes part in the phases
 * @property postProcessor whether the class implements `BeanPostProcessor`
 * @property ctorParams the primary constructor's parameters, in declaration order; these are the
 * edges of the dependency graph
 * @property factories members annotated with `@Bean`, each publishing its own bean
 * @property conditions what has to hold for the bean to be created at all
 */
@InternalSurfApi
@Serializable
data class BeanDefinition(
    val className: String,
    val provides: List<String> = emptyList(),
    val stereotypes: List<String> = emptyList(),
    val scope: Scope = Scope.SINGLETON,
    val qualifier: String? = null,
    val primary: Boolean = false,
    val priority: Int = 0,
    val lifecycle: Boolean = false,
    val postProcessor: Boolean = false,
    val ctorParams: List<InjectionPoint> = emptyList(),
    val factories: List<FactoryDefinition> = emptyList(),
    val conditions: BeanConditions = BeanConditions(),
)

/**
 * One place the container has to put a value: a constructor parameter, or a parameter of a factory
 * function.
 *
 * The shape of the parameter decides how it is resolved, which is why it is recorded separately
 * from the type.
 *
 * @property type binary name of the declared type — the element type for a collection, the wrapped
 * type for a lazy parameter
 * @property qualifier the name from `@Qualifier` on the parameter, or `null` for any candidate
 * @property optional whether the parameter is nullable, in which case a missing bean becomes `null`
 * instead of a failure
 * @property collection set when the parameter takes every candidate rather than one
 * @property lazy whether the parameter is a `Lazy<T>`, resolved on first access instead of at
 * construction — the deliberate way to break a cycle
 */
@InternalSurfApi
@Serializable
data class InjectionPoint(
    val type: String,
    val qualifier: String? = null,
    val optional: Boolean = false,
    val collection: CollectionKind? = null,
    val lazy: Boolean = false,
)

/** The collection a parameter takes its candidates in. */
@InternalSurfApi
@Serializable
enum class CollectionKind { LIST, SET }

/** Whether a factory is a property or a function. */
@InternalSurfApi
@Serializable
enum class FactoryKind { PROPERTY, FUNCTION }

/**
 * A member annotated with `@Bean`, publishing its return value as a bean of its own.
 *
 * @property memberName the property or function name on the declaring class
 * @property kind whether to read a property or call a function
 * @property returnType binary name of the declared type, which is what the bean is resolved as
 * @property provides the return type and its supertypes
 * @property params the function's parameters; always empty for a property
 */
@InternalSurfApi
@Serializable
data class FactoryDefinition(
    val memberName: String,
    val kind: FactoryKind,
    val returnType: String,
    val provides: List<String> = emptyList(),
    val scope: Scope = Scope.SINGLETON,
    val qualifier: String? = null,
    val primary: Boolean = false,
    val priority: Int = 0,
    val params: List<InjectionPoint> = emptyList(),
)

/**
 * What has to hold before a bean is created.
 *
 * Every list is independent and all of them have to pass. An empty [BeanConditions] imposes nothing.
 *
 * @property classNames classes that have to be on the runtime classpath
 * @property pluginIds plugins that all have to be loaded
 * @property anyPluginIds groups in which at least one plugin has to be loaded
 * @property dependsOnBeans types that have to be provided by some bean, for ordering only
 * @property customConditions binary names of `BeanCondition` implementations to run
 * @property environments groups in which the process has to run in at least one environment
 * @property missingBeans types that must not be provided by any other bean
 * @property properties configuration properties that have to match
 */
@InternalSurfApi
@Serializable
data class BeanConditions(
    val classNames: List<String> = emptyList(),
    val pluginIds: List<String> = emptyList(),
    val anyPluginIds: List<List<String>> = emptyList(),
    val dependsOnBeans: List<String> = emptyList(),
    val customConditions: List<String> = emptyList(),
    val environments: List<List<String>> = emptyList(),
    val missingBeans: List<String> = emptyList(),
    val properties: List<PropertyCondition> = emptyList(),
)

/**
 * One configuration property a bean depends on.
 *
 * @property key path to the value, segment by segment
 * @property havingValue the value it has to equal, ignoring case; empty means "just has to be set"
 * @property matchIfMissing what an absent property counts as
 * @property file configuration file relative to the application's data directory; empty means the
 * default one
 */
@InternalSurfApi
@Serializable
data class PropertyCondition(
    val key: List<String>,
    val havingValue: String = "",
    val matchIfMissing: Boolean = false,
    val file: String = "",
)
