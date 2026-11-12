@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.processor.bean

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.definition.*
import dev.slne.surf.api.processor.util.*
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.io.IOException

/** How many `Lazy`/collection wrappers an injection point may be nested in before it is rejected. */
private const val MAX_WRAPPER_DEPTH = 4

/** How many type aliases may stand between a written type and the class behind it. */
private const val MAX_ALIAS_DEPTH = 16

/**
 * Writes the container's bean metadata.
 *
 * Every class annotated with `@BeanMeta`, directly or through an annotation that carries it,
 * becomes one [BeanDefinition]; the definitions of a module are written as a single JSON file under
 * [BeanMetadata.DIRECTORY] so the container can build its graph without loading a class. Whatever
 * can be decided from the source is decided here: a class the container could never create, a
 * factory it could never reach and a second `@Primary` for a type are compile errors rather than
 * startup failures.
 *
 * A type that does not resolve yet — one another processor is still generating — defers its whole
 * bean to a later round instead of being written as a broken definition, because a definition that
 * silently lost a condition or a parameter is a bean that silently never appears.
 */
class BeanSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    /**
     * The definitions collected so far, per module and keyed by binary name.
     *
     * Keying by binary name is what makes deferral safe: a symbol described again in a later round
     * replaces its earlier attempt instead of adding a second entry for the same class.
     */
    private val definitions = mutableMapOf<String, MutableMap<String, BeanDefinition>>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleName = resolver.getModuleName().asString().split(":").last()
        val deferred = mutableListOf<KSAnnotated>()
        val module = definitions.getOrPut(moduleName) { linkedMapOf() }

        val beans = findBeans(resolver).toList()
        reportFactoriesOutsideBeans(resolver, beans)

        val describedThisRound = linkedMapOf<String, KSClassDeclaration>()
        for (bean in beans) {
            val definition = describe(bean, deferred) ?: continue
            module[definition.className] = definition
            describedThisRound[definition.className] = bean
        }

        reportPrimaryConflicts(module, describedThisRound)

        return deferred
    }

    override fun finish() {
        for ((moduleName, module) in definitions) {
            if (module.isEmpty()) continue

            // Sorted so the file is byte-identical whenever the sources are, which keeps the jar
            // reproducible and a diff of the metadata readable.
            val metadata = BeanMetadata(module.values.sortedBy { it.className })
            val path = "${BeanMetadata.DIRECTORY}/$moduleName.json"

            try {
                codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, path, "")
                    .bufferedWriter()
                    .use { writer ->
                        writer.write(
                            BeanMetadata.json.encodeToString(BeanMetadata.serializer(), metadata)
                        )
                    }

                logger.info("Wrote ${metadata.beans.size} bean definitions to $path")
            } catch (e: IOException) {
                logger.error("Unable to create $path, $e")
            }
        }

        definitions.clear()
    }

    // region discovery

    /**
     * Every class in this round that carries `@BeanMeta`, directly or through a meta-annotation.
     *
     * Nested classes are included, so a bean declared inside another class is found rather than
     * quietly left out of the metadata. Annotation classes are skipped: an annotation that carries
     * `@BeanMeta` makes other classes beans and is not one itself.
     */
    private fun findBeans(resolver: Resolver): Sequence<KSClassDeclaration> =
        resolver.getAllFiles()
            .flatMap { file -> file.declarations }
            .flatMap { declaration -> classesIn(declaration) }
            .filter { declaration -> declaration.classKind != ClassKind.ANNOTATION_CLASS }
            .filter { declaration ->
                AnnotationUtils.findAnnotationRecursive(declaration, BeanClassNames.BEAN_META) != null
            }

    /** [declaration] and every class declared inside it, if it is a class at all. */
    private fun classesIn(declaration: KSDeclaration): Sequence<KSClassDeclaration> {
        if (declaration !is KSClassDeclaration || declaration.isLocal()) return emptySequence()

        return sequenceOf(declaration) + declaration.declarations.flatMap { classesIn(it) }
    }

    // endregion

    // region bean

    /** The definition for [bean], or `null` when it was deferred or rejected. */
    private fun describe(
        bean: KSClassDeclaration,
        deferred: MutableList<KSAnnotated>
    ): BeanDefinition? {
        val provides = collectProvides(bean, bean, deferred) ?: return null
        val stereotypes = stereotypesOf(bean, deferred) ?: return null
        val scope = scopeOf(bean)
        val conditions = conditionsOf(bean, bean, deferred) ?: return null

        val parameters = constructorParameters(bean) ?: return null
        val label = bean.qualifiedName?.asString() ?: bean.simpleName.asString()
        val ctorParams = parameters.map { parameter ->
            val name = parameter.name?.asString()
            injectionPoint(parameter, "Parameter '$name' of $label", bean, deferred) ?: return null
        }

        val factories = factories(bean, scope, deferred) ?: return null

        return BeanDefinition(
            className = bean.toBinaryName(),
            provides = provides,
            stereotypes = stereotypes,
            scope = scope,
            qualifier = qualifierOf(bean),
            primary = bean.findAnnotationRecursive(BeanClassNames.PRIMARY) != null,
            priority = priorityOf(bean),
            lifecycle = BeanClassNames.SURF_COMPONENT in provides,
            postProcessor = BeanClassNames.BEAN_POST_PROCESSOR in provides,
            ctorParams = ctorParams,
            factories = factories,
            conditions = conditions,
        )
    }

    /**
     * [declaration] and every type it can stand in for, as binary names.
     *
     * The hierarchy is walked breadth-first from the declaration itself over the declared
     * supertypes of each class reached, so an interface a supertype implements is included as well
     * and the nearest types come first. A type is visited once, which is what keeps a diamond —
     * two interfaces extending a third — from listing that third one twice. `Any` and
     * `java.lang.Object` are left out: every class provides them and no bean is resolved as one.
     *
     * Returns `null` when a supertype does not resolve yet, after deferring [owner].
     */
    private fun collectProvides(
        declaration: KSClassDeclaration,
        owner: KSAnnotated,
        deferred: MutableList<KSAnnotated>
    ): List<String>? {
        val names = linkedSetOf<String>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<KSClassDeclaration>()
        queue += declaration

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            // Nothing without a qualified name can be written into the metadata.
            val qualifiedName = current.qualifiedName?.asString() ?: continue

            if (!visited.add(qualifiedName)) continue
            if (qualifiedName in BeanClassNames.ROOT_TYPES) continue
            if (current.isLocal()) continue

            names += current.toBinaryName()

            for (reference in current.superTypes) {
                val superType = reference.resolve()
                if (superType.isError) {
                    deferred += owner
                    return null
                }

                val superDeclaration = classBehind(superType.declaration)
                if (superDeclaration == null) {
                    deferred += owner
                    return null
                }

                queue += superDeclaration
            }
        }

        return names.toList()
    }

    /**
     * The annotations on [bean] that make it a bean, as binary names.
     *
     * An annotation counts when it is `@BeanMeta` itself or carries it, so a project's own
     * stereotype is recorded under its own name and a post processor can find the beans it marked.
     */
    private fun stereotypesOf(
        bean: KSClassDeclaration,
        deferred: MutableList<KSAnnotated>
    ): List<String>? {
        val names = linkedSetOf<String>()

        for (annotation in bean.annotations) {
            val type = annotation.annotationType.resolve()
            if (type.isError) {
                deferred += bean
                return null
            }

            val declaration = classBehind(type.declaration) ?: continue
            val qualifiedName = declaration.qualifiedName?.asString() ?: continue

            val makesBeans = qualifiedName == BeanClassNames.BEAN_META ||
                    AnnotationUtils.findAnnotationRecursive(
                        declaration,
                        BeanClassNames.BEAN_META
                    ) != null

            if (makesBeans) names += declaration.toBinaryName()
        }

        return names.toList()
    }

    /**
     * The parameters the container passes to [bean], or `null` when it could not create it at all.
     *
     * An `object` is its own instance, so it takes nothing; everything else needs a primary
     * constructor, the one member the container reads parameters from.
     */
    private fun constructorParameters(bean: KSClassDeclaration): List<KSValueParameter>? {
        val name = bean.qualifiedName?.asString() ?: bean.simpleName.asString()

        if (bean.classKind == ClassKind.OBJECT) return emptyList()

        val reason = when {
            bean.classKind == ClassKind.INTERFACE -> "an interface"
            bean.classKind == ClassKind.ENUM_CLASS -> "an enum class"
            bean.classKind == ClassKind.ENUM_ENTRY -> "an enum entry"
            Modifier.SEALED in bean.modifiers -> "sealed"
            Modifier.ABSTRACT in bean.modifiers -> "abstract"
            Modifier.INNER in bean.modifiers -> "an inner class, which needs an outer instance"
            else -> null
        }

        if (reason != null) {
            logger.error(
                "$name is $reason, so the container cannot create it. Move the annotation to the " +
                        "class that can be created, or publish the value with @Bean from a bean " +
                        "that can.",
                bean
            )
            return null
        }

        val constructor = bean.primaryConstructor
        if (constructor == null) {
            logger.error(
                "$name has no primary constructor, so the container has nothing to call. Give it " +
                        "one, or publish the value with @Bean from a bean that can be created.",
                bean
            )
            return null
        }

        return constructor.parameters
    }

    // endregion

    // region injection points

    /** What the shape of a written type says about how it is resolved. */
    private data class Shape(
        val type: String,
        val optional: Boolean,
        val collection: CollectionKind?,
        val lazy: Boolean,
    )

    /** The injection point for [parameter], or `null` when it was deferred or rejected. */
    private fun injectionPoint(
        parameter: KSValueParameter,
        label: String,
        owner: KSAnnotated,
        deferred: MutableList<KSAnnotated>
    ): InjectionPoint? {
        val shape = shapeOf(parameter.type, label, parameter, owner, deferred) ?: return null

        return InjectionPoint(
            type = shape.type,
            qualifier = qualifierOf(parameter),
            optional = shape.optional,
            collection = shape.collection,
            lazy = shape.lazy,
        )
    }

    /**
     * Reads a written type as the shape of one injection point.
     *
     * The wrappers are peeled off one at a time: a `Lazy<T>` resolves on first access and records
     * `T`, a `List<T>` or `Set<T>` takes every candidate and records the element type, and a
     * nullable type anywhere in the chain makes the point optional. What is left is the type the
     * container looks a bean up by.
     *
     * Two shapes are rejected rather than recorded. A point that is both lazy and a collection —
     * `Lazy<List<T>>` or `List<Lazy<T>>` — cannot be expressed: the container decides between a
     * collection and a lazy value, so one of the two flags would be dropped and the parameter would
     * be handed an object of the wrong type at construction. A type nested inside the same wrapper
     * twice has no single bean behind it either.
     */
    private fun shapeOf(
        reference: KSTypeReference,
        label: String,
        node: KSNode,
        owner: KSAnnotated,
        deferred: MutableList<KSAnnotated>
    ): Shape? {
        var current = reference.resolve()
        var optional = false
        var lazy = false
        var collection: CollectionKind? = null
        var depth = 0

        while (true) {
            if (current.isError) {
                deferred += owner
                return null
            }

            if (current.isMarkedNullable) optional = true

            if (current.declaration is KSTypeParameter) {
                logger.error(
                    "$label is a generic type parameter, which the container cannot resolve: a " +
                            "bean is looked up by the type written at the injection point.",
                    node
                )
                return null
            }

            val declaration = classBehind(current.declaration)
            if (declaration == null) {
                deferred += owner
                return null
            }

            val qualifiedName = declaration.qualifiedName?.asString() ?: ""
            val kind = when (qualifiedName) {
                in BeanClassNames.LIST_TYPES -> CollectionKind.LIST
                in BeanClassNames.SET_TYPES -> CollectionKind.SET
                else -> null
            }

            when {
                qualifiedName == BeanClassNames.LAZY -> {
                    if (lazy) {
                        logger.error(
                            "$label wraps Lazy in Lazy, which has no single bean behind it.",
                            node
                        )
                        return null
                    }
                    lazy = true
                }

                kind != null -> {
                    if (collection != null) {
                        logger.error(
                            "$label nests one collection in another; a collection takes the " +
                                    "candidates for one type, so only the outer one could be " +
                                    "filled.",
                            node
                        )
                        return null
                    }
                    collection = kind
                }

                else -> {
                    if (qualifiedName in BeanClassNames.AMBIGUOUS_COLLECTIONS) {
                        logger.warn(
                            "$label takes $qualifiedName, so it is resolved as a single bean of " +
                                    "that type. Declare it as List or Set to take every candidate.",
                            node
                        )
                    }

                    return Shape(declaration.toBinaryName(), optional, collection, lazy)
                }
            }

            if (lazy && collection != null) {
                logger.error(
                    "$label is both lazy and a collection. The container resolves a point as one " +
                            "or the other, so pick a Lazy of one bean or a collection of every " +
                            "candidate.",
                    node
                )
                return null
            }

            val argument = current.arguments.firstOrNull()?.type
            if (argument == null) {
                logger.error(
                    "$label uses a star projection, so there is no type to resolve a bean by.",
                    node
                )
                return null
            }

            if (++depth > MAX_WRAPPER_DEPTH) {
                logger.error("$label is nested too deeply to read as an injection point.", node)
                return null
            }

            current = argument.resolve()
        }
    }

    // endregion

    // region factories

    /** What a `@Bean` member publishes. */
    private data class Published(val type: String, val provides: List<String>)

    /**
     * The `@Bean` members [bean] declares, or `null` when one of them was deferred or rejected.
     *
     * Only declared members count: an inherited `@Bean` belongs to the bean that declares it, and
     * recording it on both would publish the same value twice.
     */
    private fun factories(
        bean: KSClassDeclaration,
        beanScope: Scope,
        deferred: MutableList<KSAnnotated>
    ): List<FactoryDefinition>? {
        val factories = mutableListOf<FactoryDefinition>()

        for (declaration in bean.declarations) {
            val annotation = declaration.findAnnotationRecursive(BeanClassNames.BEAN) ?: continue

            val factory = when (declaration) {
                is KSPropertyDeclaration ->
                    propertyFactory(declaration, annotation, bean, beanScope, deferred)

                is KSFunctionDeclaration ->
                    functionFactory(declaration, annotation, bean, beanScope, deferred)

                else -> {
                    logger.error(
                        "@Bean sits on ${declaration.simpleName.asString()}, which is neither a " +
                                "property nor a function.",
                        declaration
                    )
                    null
                }
            } ?: return null

            factories += factory
        }

        reportFactoryCollisions(bean, factories)

        return factories
    }

    /** The definition for a `@Bean` property, or `null` when it was deferred or rejected. */
    private fun propertyFactory(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
        bean: KSClassDeclaration,
        beanScope: Scope,
        deferred: MutableList<KSAnnotated>
    ): FactoryDefinition? {
        val label = "@Bean property ${bean.simpleName.asString()}.${property.simpleName.asString()}"

        if (!isReachable(property, label, property.extensionReceiver)) return null
        reportConditionsOnFactory(property, label)
        reportInferredType(property.type, property, label, "Write the type the bean is resolved as")

        val published = publishedType(property.type, label, property, bean, deferred) ?: return null

        return FactoryDefinition(
            memberName = property.simpleName.asString(),
            kind = FactoryKind.PROPERTY,
            returnType = published.type,
            provides = published.provides,
            scope = factoryScope(annotation, beanScope),
            qualifier = qualifierOf(property),
            primary = property.findAnnotationRecursive(BeanClassNames.PRIMARY) != null,
            priority = priorityOf(property),
        )
    }

    /** The definition for a `@Bean` function, or `null` when it was deferred or rejected. */
    private fun functionFactory(
        function: KSFunctionDeclaration,
        annotation: KSAnnotation,
        bean: KSClassDeclaration,
        beanScope: Scope,
        deferred: MutableList<KSAnnotated>
    ): FactoryDefinition? {
        val label = "@Bean function ${bean.simpleName.asString()}.${function.simpleName.asString()}"

        if (!isReachable(function, label, function.extensionReceiver)) return null
        reportConditionsOnFactory(function, label)

        if (Modifier.SUSPEND in function.modifiers) {
            logger.warn(
                "$label is suspending, and the container reads a factory outside a coroutine. " +
                        "Publish the value from a plain function or a property.",
                function
            )
        }

        val returnReference = function.returnType
        if (returnReference == null) {
            deferred += bean
            return null
        }

        reportInferredType(
            returnReference,
            function,
            label,
            "Write the return type the bean is resolved as"
        )

        val published = publishedType(returnReference, label, function, bean, deferred)
            ?: return null

        val params = function.parameters.map { parameter ->
            val name = parameter.name?.asString()
            injectionPoint(parameter, "Parameter '$name' of $label", bean, deferred) ?: return null
        }

        return FactoryDefinition(
            memberName = function.simpleName.asString(),
            kind = FactoryKind.FUNCTION,
            returnType = published.type,
            provides = published.provides,
            scope = factoryScope(annotation, beanScope),
            qualifier = qualifierOf(function),
            primary = function.findAnnotationRecursive(BeanClassNames.PRIMARY) != null,
            priority = priorityOf(function),
            params = params,
        )
    }

    /**
     * The type a `@Bean` member publishes, taken from the written type rather than the value behind
     * it, because that is the type consumers inject.
     */
    private fun publishedType(
        reference: KSTypeReference,
        label: String,
        node: KSNode,
        owner: KSAnnotated,
        deferred: MutableList<KSAnnotated>
    ): Published? {
        val type = reference.resolve()
        if (type.isError) {
            deferred += owner
            return null
        }

        if (type.declaration is KSTypeParameter) {
            logger.error(
                "$label publishes a generic type parameter, so there is no type consumers could " +
                        "inject it as.",
                node
            )
            return null
        }

        val declaration = classBehind(type.declaration)
        if (declaration == null) {
            deferred += owner
            return null
        }

        val qualifiedName = declaration.qualifiedName?.asString() ?: ""
        if (qualifiedName in BeanClassNames.EMPTY_TYPES) {
            logger.error("$label publishes $qualifiedName, which is not a value anything can inject.", node)
            return null
        }

        if (type.isMarkedNullable) {
            logger.warn(
                "$label is nullable, and a factory that returns null fails when the bean is " +
                        "created. Publish a value, or guard the bean with a condition.",
                node
            )
        }

        val provides = collectProvides(declaration, owner, deferred) ?: return null

        return Published(declaration.toBinaryName(), provides)
    }

    /**
     * The scope of a `@Bean` member: the one it states, or the declaring bean's.
     *
     * Falling back to the declaring bean's scope is what lets a factory on a shared bean publish
     * into the root container without repeating the scope at every member.
     */
    private fun factoryScope(annotation: KSAnnotation, beanScope: Scope): Scope {
        val written = annotation.arguments
            .firstOrNull { it.name?.asString() == "scope" }
            ?: return beanScope

        return scopeFrom(written.value) ?: beanScope
    }

    // endregion

    // region conditions

    /** The conditions on [annotated], or `null` when a type in one of them did not resolve yet. */
    private fun conditionsOf(
        annotated: KSAnnotated,
        owner: KSAnnotated,
        deferred: MutableList<KSAnnotated>
    ): BeanConditions? {
        val classes = conditionTypes(
            annotated, BeanClassNames.CONDITIONAL_ON_CLASS, "clazz", owner, deferred
        ) ?: return null

        val classNames = annotated
            .findAllAnnotationsRecursive(BeanClassNames.CONDITIONAL_ON_CLASS_NAME)
            .collectArgumentValues<String>("className")

        val plugins = annotated
            .findAllAnnotationsRecursive(BeanClassNames.CONDITIONAL_ON_PLUGIN)
            .collectArgumentValues<String>("pluginId")

        val anyPlugins = annotated
            .findAllAnnotationsRecursive(BeanClassNames.CONDITIONAL_ON_ANY_PLUGIN)
            .collectArgumentValuesGrouped<String>("pluginIds")

        val dependsOnBeans = conditionTypes(
            annotated, BeanClassNames.DEPENDS_ON_BEAN, "type", owner, deferred
        ) ?: return null

        val customConditions = conditionTypes(
            annotated, BeanClassNames.CONDITIONAL_ON, "condition", owner, deferred
        ) ?: return null

        val environments = annotated
            .findAllAnnotationsRecursive(BeanClassNames.CONDITIONAL_ON_ENVIRONMENT)
            .collectArgumentValuesGrouped<String>("environments")

        val missingBeans = conditionTypes(
            annotated, BeanClassNames.CONDITIONAL_ON_MISSING_BEAN, "type", owner, deferred
        ) ?: return null

        val properties = annotated
            .findAllAnnotationsRecursive(BeanClassNames.CONDITIONAL_ON_PROPERTY)
            .map { annotation ->
                PropertyCondition(
                    key = annotation.collectArgumentValues<String>("key"),
                    havingValue = annotation.getArgumentValueAs<String>("havingValue") ?: "",
                    matchIfMissing = annotation.getArgumentValueAs<Boolean>("matchIfMissing") ?: false,
                    file = annotation.getArgumentValueAs<String>("file") ?: "",
                )
            }

        return BeanConditions(
            classNames = classes + classNames,
            pluginIds = plugins,
            anyPluginIds = anyPlugins,
            dependsOnBeans = dependsOnBeans,
            customConditions = customConditions,
            environments = environments,
            missingBeans = missingBeans,
            properties = properties,
        )
    }

    /**
     * The classes named by [argumentName] on every [annotationFqName] on [annotated].
     *
     * Returns `null` after deferring [owner] when one of them does not resolve yet: a condition
     * read as absent would let a bean through that the author guarded, so the bean waits for the
     * round in which its types exist.
     */
    private fun conditionTypes(
        annotated: KSAnnotated,
        annotationFqName: String,
        argumentName: String,
        owner: KSAnnotated,
        deferred: MutableList<KSAnnotated>
    ): List<String>? {
        val types = annotated
            .findAllAnnotationsRecursive(annotationFqName)
            .collectArgumentValues<KSType>(argumentName)

        val names = mutableListOf<String>()

        for (type in types) {
            if (type.isError) {
                deferred += owner
                return null
            }

            val declaration = classBehind(type.declaration)
            if (declaration == null) {
                deferred += owner
                return null
            }

            names += declaration.toBinaryName()
        }

        return names
    }

    // endregion

    // region validation

    /** Reports a `@Bean` member the container could not reach. */
    private fun isReachable(
        member: KSDeclaration,
        label: String,
        extensionReceiver: KSTypeReference?
    ): Boolean {
        if (Modifier.PRIVATE in member.modifiers) {
            logger.error(
                "$label is private. A published bean is part of the module's surface, so make the " +
                        "member at least internal.",
                member
            )
            return false
        }

        if (extensionReceiver != null) {
            val nullable = extensionReceiver.resolve().isMarkedNullable
            val shape = if (nullable) "a nullable receiver" else "an extension receiver"
            logger.error(
                "$label has $shape, and the container reads a factory on the declaring bean only. " +
                        "Declare it as an ordinary member.",
                member
            )
            return false
        }

        if (member.typeParameters.isNotEmpty()) {
            logger.error(
                "$label has type parameters, so there is no single type the bean is published as.",
                member
            )
            return false
        }

        return true
    }

    /** Reports two `@Bean` members on [bean] the container could not tell apart. */
    private fun reportFactoryCollisions(
        bean: KSClassDeclaration,
        factories: List<FactoryDefinition>
    ) {
        val collisions = factories
            .groupBy { Triple(it.kind, it.memberName, it.params.size) }
            .filterValues { it.size > 1 }
            .keys

        for ((_, memberName, parameterCount) in collisions) {
            logger.error(
                "${bean.qualifiedName?.asString()} declares $memberName as a @Bean twice with " +
                        "$parameterCount parameters, and the container reads a factory by name. " +
                        "Rename one of them.",
                bean
            )
        }
    }

    /**
     * Reports a `@Bean` member outside a bean.
     *
     * The declaring class has to be a bean, because the container reads a factory from the instance
     * it belongs to. A member anywhere else would publish nothing and never say so.
     */
    private fun reportFactoriesOutsideBeans(resolver: Resolver, beans: List<KSClassDeclaration>) {
        val beanNames = beans.mapNotNullTo(mutableSetOf()) { it.qualifiedName?.asString() }

        for (file in resolver.getAllFiles()) {
            for (declaration in file.declarations) {
                reportFactoriesOutsideBeans(declaration, null, beanNames)
            }
        }
    }

    private fun reportFactoriesOutsideBeans(
        declaration: KSDeclaration,
        owner: KSClassDeclaration?,
        beanNames: Set<String>
    ) {
        if (declaration is KSClassDeclaration) {
            for (member in declaration.declarations) {
                reportFactoriesOutsideBeans(member, declaration, beanNames)
            }
            return
        }

        if (declaration !is KSPropertyDeclaration && declaration !is KSFunctionDeclaration) return
        if (declaration.findAnnotationRecursive(BeanClassNames.BEAN) == null) return

        val ownerName = owner?.qualifiedName?.asString()
        if (ownerName != null && ownerName in beanNames) return

        val where = ownerName?.let { "$it, which is not a bean" } ?: "no class at all"
        logger.error(
            "@Bean sits on ${declaration.simpleName.asString()}, declared in $where. The " +
                    "container reads a factory from the bean declaring it, so move the member " +
                    "into a bean or annotate its class with @Service, @Repository or @BeanMeta.",
            declaration
        )
    }

    /**
     * Reports a second `@Primary` for a type within this module.
     *
     * Only one bean per provided type may win when an injection point names none, and two of them
     * leave the container with nothing to decide on. Beans in other modules are not on this
     * compilation's source path, so a clash across module boundaries can only be found when the
     * container merges the metadata.
     */
    private fun reportPrimaryConflicts(
        module: Map<String, BeanDefinition>,
        describedThisRound: Map<String, KSClassDeclaration>
    ) {
        val claimedBy = mutableMapOf<String, String>()

        for ((className, definition) in module) {
            val claims = buildList {
                if (definition.primary) add(className to definition.provides)
                for (factory in definition.factories) {
                    if (factory.primary) add("$className.${factory.memberName}" to factory.provides)
                }
            }

            for ((claimant, provides) in claims) {
                for (type in provides) {
                    // The lifecycle markers say nothing about what a bean provides, and two
                    // primaries that both take part in the phases are not in conflict.
                    if (type == BeanClassNames.SURF_COMPONENT) continue
                    if (type == BeanClassNames.BEAN_POST_PROCESSOR) continue

                    val existing = claimedBy[type]
                    if (existing == null) {
                        claimedBy[type] = claimant
                        continue
                    }
                    if (existing == claimant) continue

                    // Reported on the bean described in this round, so the error has a symbol to
                    // point at and a clash found again in a later round is not reported twice.
                    val symbol = describedThisRound[className] ?: continue
                    logger.error(
                        "$claimant and $existing are both @Primary for $type, so neither can win " +
                                "when an injection point names no qualifier. Keep one, or give " +
                                "the other a @Qualifier.",
                        symbol
                    )
                }
            }
        }
    }

    /** Reports a condition on a `@Bean` member, which the metadata cannot carry. */
    private fun reportConditionsOnFactory(member: KSDeclaration, label: String) {
        for (condition in BeanClassNames.CONDITIONS) {
            if (member.findAnnotationRecursive(condition) == null) continue

            logger.warn(
                "$label carries $condition, and a factory's conditions are not part of the bean " +
                        "metadata, so it is not evaluated. Guard the declaring bean instead.",
                member
            )
        }
    }

    /** Reports a `@Bean` member whose type is inferred rather than written. */
    private fun reportInferredType(
        reference: KSTypeReference,
        member: KSDeclaration,
        label: String,
        advice: String
    ) {
        if (reference.isWrittenInSource()) return

        logger.warn(
            "$label has no written type, so the bean is published as whatever the value happens " +
                    "to be — an implementation type rather than the contract. $advice.",
            member
        )
    }

    // endregion

    // region annotations

    /** The name from `@Qualifier`, or `null` when there is none to match on. */
    private fun qualifierOf(annotated: KSAnnotated): String? = annotated
        .findAnnotationRecursive(BeanClassNames.QUALIFIER)
        ?.stringArg("name")
        ?.takeIf { it.isNotBlank() }

    /** The value from `@Priority`, or `0` when the graph may order the bean freely. */
    private fun priorityOf(annotated: KSAnnotated): Int = annotated
        .findAnnotationRecursive(BeanClassNames.PRIORITY)
        ?.intArg("value", 0)
        ?: 0

    /**
     * The scope [bean] is created in.
     *
     * The nearest `@Service` or `@Repository` decides, which is what lets a project's own
     * stereotype carry a scope for every class it marks. A class with neither — a plain `@BeanMeta`
     * — is a singleton.
     */
    private fun scopeOf(bean: KSClassDeclaration): Scope {
        val nearest = (
                AnnotationUtils.findAllByDistance(bean, BeanClassNames.SERVICE) +
                        AnnotationUtils.findAllByDistance(bean, BeanClassNames.REPOSITORY)
                ).minByOrNull { it.distance } ?: return Scope.SINGLETON

        val value = nearest.annotation.getArgumentValue("scope") ?: return Scope.SINGLETON

        return scopeFrom(value) ?: run {
            logger.warn(
                "The scope on ${bean.qualifiedName?.asString()} could not be read as one of " +
                        "${Scope.entries.joinToString { it.name }}; it is treated as " +
                        "${Scope.SINGLETON.name}.",
                bean
            )
            Scope.SINGLETON
        }
    }

    /**
     * Reads an annotation argument as a [Scope].
     *
     * An enum argument reaches a processor in more than one shape depending on where the annotation
     * was read from, so every shape it can take is tried before the value is given up on.
     */
    private fun scopeFrom(value: Any?): Scope? {
        if (value == null) return null

        val candidates = buildList {
            when (value) {
                is KSType -> add(value.declaration.simpleName.asString())
                is KSClassDeclaration -> add(value.simpleName.asString())
                is KSName -> add(value.getShortName())
            }
            add(value.toString().substringAfterLast('.'))
        }

        return candidates.firstNotNullOfOrNull { candidate ->
            Scope.entries.firstOrNull { it.name == candidate }
        }
    }

    // endregion

    // region types

    /**
     * The class behind [declaration], following type aliases.
     *
     * Returns `null` when there is no class behind it — a type parameter, or a type that does not
     * resolve in this round.
     */
    private fun classBehind(declaration: KSDeclaration, depth: Int = 0): KSClassDeclaration? =
        when {
            declaration is KSClassDeclaration -> declaration
            declaration is KSTypeParameter -> null
            declaration is KSTypeAlias && depth < MAX_ALIAS_DEPTH ->
                classBehind(declaration.type.resolve().declaration, depth + 1)

            else -> declaration.closestClassDeclaration()
        }

    /**
     * Whether this node stands for something written in the source rather than filled in for it.
     *
     * An omitted type and an omitted annotation argument both reach a processor as the value the
     * compiler inferred, with no place in the file behind it, which is how a written `SINGLETON`
     * is told apart from one that was never typed.
     */
    private fun KSNode.isWrittenInSource(): Boolean =
        origin != Origin.SYNTHETIC && location is FileLocation

    // endregion
}
