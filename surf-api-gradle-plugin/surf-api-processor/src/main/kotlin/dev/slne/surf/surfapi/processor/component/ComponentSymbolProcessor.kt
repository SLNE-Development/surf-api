package dev.slne.surf.surfapi.processor.component

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import dev.slne.surf.surfapi.processor.util.nameOf
import dev.slne.surf.surfapi.processor.util.toBinaryName
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.Priority
import dev.slne.surf.surfapi.shared.api.component.processor.ComponentPostProcessor
import dev.slne.surf.surfapi.shared.api.component.requirement.*
import dev.slne.surf.surfapi.shared.internal.hook.ComponentsConfig.COMPONENTS_DIRECTORY
import dev.slne.surf.surfapi.shared.internal.hook.ComponentsConfig.json
import dev.slne.surf.surfapi.shared.internal.hook.PluginComponentMeta
import java.io.IOException

class ComponentSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    companion object {
        private val COMPONENT_ANNOTATION = nameOf<ComponentMeta>()
        private val PRIORITY_ANNOTATION = nameOf<Priority>()
        private val DEPENDS_ON_CLASS_ANNOTATION = nameOf<DependsOnClass>()
        private val DEPENDS_ON_CLASS_NAME_ANNOTATION = nameOf<DependsOnClassName>()
        private val DEPENDS_ON_ONE_PLUGIN_ANNOTATION = nameOf<DependsOnOnePlugin>()
        private val DEPENDS_ON_PLUGIN_ANNOTATION = nameOf<DependsOnPlugin>()
        private val DEPENDS_ON_COMPONENT_ANNOTATION = nameOf<DependsOnComponent>()
        private val CONDITIONAL_ON_ANNOTATION = nameOf<ConditionalOn>()
        private val COMPONENT_POST_PROCESSOR_NAME = nameOf<ComponentPostProcessor>()
    }

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val components = mutableMapOf<String, MutableSet<PluginComponentMeta.Component>>()
    private val postProcessors = mutableMapOf<String, MutableSet<PluginComponentMeta.PostProcessor>>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleName = resolver.getModuleName().asString()
        val deferred = mutableListOf<KSAnnotated>()

        // Process components (both direct @ComponentMeta and meta-annotations)
        processComponents(resolver, moduleName, deferred)

        // Process ComponentPostProcessor implementations
        processPostProcessors(resolver, moduleName)

        return deferred
    }


    private fun processComponents(
        resolver: Resolver,
        moduleName: String,
        deferred: MutableList<KSAnnotated>
    ) {
        // Get all classes with direct @ComponentMeta annotation
        val directlyAnnotated = resolver.getSymbolsWithAnnotation(COMPONENT_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        // Get all classes that have annotations which are themselves annotated with @ComponentMeta (meta-annotations)
        val metaAnnotationClasses = directlyAnnotated
            .filter { it.classKind == ClassKind.ANNOTATION_CLASS }

        val metaAnnotated = metaAnnotationClasses.flatMap { metaAnnotation ->
            val qualifiedName = metaAnnotation.qualifiedName?.asString()
            if (qualifiedName == null) {
                logger.warn("Meta-annotation has no qualified name: $metaAnnotation")
                return@flatMap emptyList()
            }

            logger.info("Searching for classes with meta-annotation: $qualifiedName")

            resolver.getSymbolsWithAnnotation(qualifiedName)
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind != ClassKind.ANNOTATION_CLASS } // Exclude annotation classes themselves
                .toList()
                .also { logger.info("Found ${it.size} classes with @$qualifiedName") }
        }

        val allComponentClasses = (directlyAnnotated.filter { it.classKind != ClassKind.ANNOTATION_CLASS } + metaAnnotated)
            .distinctBy { it.qualifiedName?.asString() }

        val componentMetas = allComponentClasses.mapNotNull { componentClass ->
            processComponentClass(componentClass, deferred)
        }

        components.getOrPut(moduleName) { mutableSetOf() }.addAll(componentMetas)
    }

    /**
     * Checks if an annotation is annotated with @ComponentMeta (meta-annotation support)
     */
    private fun hasComponentMetaAnnotation(annotation: KSAnnotation): Boolean {
        val annotationType = annotation.annotationType.resolve()
        val annotationDecl = annotationType.declaration as? KSClassDeclaration ?: return false

        return annotationDecl.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPONENT_ANNOTATION
        }
    }

    /**
     * Checks if a class has @ComponentMeta (directly or via meta-annotation)
     * and returns the priority from @Priority annotation
     */
    private fun findComponentMeta(classDecl: KSClassDeclaration): Pair<KSAnnotation?, Short>? {
        // First, check for direct @ComponentMeta
        val directMeta = classDecl.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPONENT_ANNOTATION
        }

        if (directMeta != null) {
            val priority = findPriority(classDecl)
            return directMeta to priority
        }

        // Check for meta-annotations
        for (annotation in classDecl.annotations) {
            val annotationType = annotation.annotationType.resolve()
            val annotationDecl = annotationType.declaration as? KSClassDeclaration ?: continue

            val componentMetaOnAnnotation = annotationDecl.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPONENT_ANNOTATION
            }

            if (componentMetaOnAnnotation != null) {
                val priority = findPriority(classDecl, annotationDecl)
                return annotation to priority
            }
        }

        return null
    }

    /**
     * Finds the priority for a component class.
     * Direct @Priority on the class takes precedence over meta-annotation @Priority.
     */
    private fun findPriority(classDecl: KSClassDeclaration, metaAnnotationDecl: KSClassDeclaration? = null): Short {
        // First check for direct @Priority annotation on the class
        val directPriority = classDecl.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == PRIORITY_ANNOTATION
        }
        if (directPriority != null) {
            return directPriority.arguments.find { it.name?.asString() == "value" }?.value as? Short ?: 0
        }

        // Then check for @Priority on the meta-annotation
        if (metaAnnotationDecl != null) {
            val metaPriority = metaAnnotationDecl.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PRIORITY_ANNOTATION
            }
            if (metaPriority != null) {
                return metaPriority.arguments.find { it.name?.asString() == "value" }?.value as? Short ?: 0
            }
        }

        // Default priority
        return 0
    }

    private fun processComponentClass(
        componentClass: KSClassDeclaration,
        deferred: MutableList<KSAnnotated>
    ): PluginComponentMeta.Component? {
        var hasUnresolvedClassDependency = false

        val componentMetaInfo = findComponentMeta(componentClass)
        if (componentMetaInfo == null) {
            logger.error("@ComponentMeta annotation not found on element", componentClass)
            return null
        }

        val priority = componentMetaInfo.second

        // Collect all annotations from the class and its meta-annotations
        val allAnnotations = collectAllAnnotations(componentClass)

        val dependsOnClass = allAnnotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DEPENDS_ON_CLASS_ANNOTATION
        }.mapNotNull { annotation ->
            val clazzValue = annotation.arguments.find { it.name?.asString() == "clazz" }?.value as? KSType
            if (clazzValue == null) {
                logger.error("DependsOnClass annotation must have 'clazz' parameter", annotation)
                return@mapNotNull null
            }

            if (clazzValue.isError) {
                deferred += componentClass
                hasUnresolvedClassDependency = true
                return@mapNotNull null
            }

            val closestClass = clazzValue.declaration.closestClassDeclaration()
            if (closestClass == null) {
                deferred += componentClass
                hasUnresolvedClassDependency = true
                return@mapNotNull null
            }
            closestClass.toBinaryName()
        }

        if (hasUnresolvedClassDependency) {
            return null
        }

        val dependsOnClassName = allAnnotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DEPENDS_ON_CLASS_NAME_ANNOTATION
        }.mapNotNull { annotation ->
            val classNameValue = annotation.arguments.find { it.name?.asString() == "className" }?.value as? String
            if (classNameValue == null) {
                logger.error("@DependsOnClassName annotation must have 'className' parameter", annotation)
                return@mapNotNull null
            }
            classNameValue
        }

        val dependsOnOnePlugin = allAnnotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DEPENDS_ON_ONE_PLUGIN_ANNOTATION
        }.mapNotNull { annotation ->
            val argValue = annotation.arguments.find { it.name?.asString() == "pluginIds" }?.value
            val pluginIds = when (argValue) {
                is List<*> -> argValue.filterIsInstance<String>()
                is String -> listOf(argValue)
                else -> emptyList()
            }

            if (pluginIds.isEmpty()) {
                logger.error("@DependsOnOnePlugin annotation must have 'pluginIds' parameter", annotation)
                return@mapNotNull null
            }

            pluginIds
        }

        val dependsOnPlugin = allAnnotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DEPENDS_ON_PLUGIN_ANNOTATION
        }.mapNotNull { annotation ->
            val argValue = annotation.arguments.find { it.name?.asString() == "pluginId" }?.value
            val pluginId = argValue as? String
            if (pluginId == null) {
                logger.error("@DependsOnPlugin annotation must have 'pluginId' parameter", annotation)
                return@mapNotNull null
            }
            pluginId
        }

        val dependsOnComponent = allAnnotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DEPENDS_ON_COMPONENT_ANNOTATION
        }.mapNotNull { annotation ->
            val componentValue = annotation.arguments.find { it.name?.asString() == "component" }?.value as? KSType
            if (componentValue == null) {
                logger.error("@DependsOnComponent annotation must have 'component' parameter", annotation)
                return@mapNotNull null
            }

            if (componentValue.isError) {
                deferred += componentClass
                hasUnresolvedClassDependency = true
                return@mapNotNull null
            }

            componentValue.declaration.closestClassDeclaration()?.toBinaryName()
        }

        if (hasUnresolvedClassDependency) {
            return null
        }

        val customConditions = allAnnotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == CONDITIONAL_ON_ANNOTATION
        }.mapNotNull { annotation ->
            val conditionValue = annotation.arguments.find { it.name?.asString() == "condition" }?.value as? KSType
            conditionValue?.declaration?.closestClassDeclaration()?.toBinaryName()
        }

        return PluginComponentMeta.Component(
            priority = priority,
            className = componentClass.toBinaryName(),
            classDependencies = dependsOnClass.toList() + dependsOnClassName.toList(),
            pluginDependencies = dependsOnPlugin.toList(),
            pluginOneDependencies = dependsOnOnePlugin.toList(),
            componentDependencies = dependsOnComponent.toList(),
            customConditions = customConditions.toList()
        )
    }

    /**
     * Collects all annotations from a class, including annotations from meta-annotations
     */
    private fun collectAllAnnotations(classDecl: KSClassDeclaration): List<KSAnnotation> {
        val result = mutableListOf<KSAnnotation>()

        // Add direct annotations
        result.addAll(classDecl.annotations.toList())

        // Add annotations from meta-annotations (annotations on the class's annotations)
        for (annotation in classDecl.annotations) {
            val annotationType = annotation.annotationType.resolve()
            val annotationDecl = annotationType.declaration as? KSClassDeclaration ?: continue

            // Add annotations from the annotation declaration (excluding @ComponentMeta itself to avoid duplication)
            for (metaAnnotation in annotationDecl.annotations) {
                val metaAnnotationName = metaAnnotation.annotationType.resolve().declaration.qualifiedName?.asString()
                if (metaAnnotationName != COMPONENT_ANNOTATION &&
                    metaAnnotationName != "kotlin.annotation.Target" &&
                    metaAnnotationName != "kotlin.annotation.Retention" &&
                    metaAnnotationName != "kotlin.annotation.Repeatable"
                ) {
                    result.add(metaAnnotation)
                }
            }
        }

        return result
    }

    private fun processPostProcessors(resolver: Resolver, moduleName: String) {
        // Find all classes that implement ComponentPostProcessor
        val postProcessorClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { classDecl ->
                // Check if this class implements ComponentPostProcessor
                classDecl.getAllSuperTypes().any { superType ->
                    superType.declaration.qualifiedName?.asString() == COMPONENT_POST_PROCESSOR_NAME
                }
            }
            .filter { classDecl ->
                // Exclude the interface itself
                classDecl.qualifiedName?.asString() != COMPONENT_POST_PROCESSOR_NAME
            }
            .toList()

        val postProcessorMetas = postProcessorClasses.mapNotNull { postProcessorClass ->
            try {
                // Try to find priority from the class (default to 0)
                // Note: Priority is a property, so we'll use default 0 and let runtime determine actual priority
                PluginComponentMeta.PostProcessor(
                    className = postProcessorClass.toBinaryName(),
                    priority = 0 // Runtime will get actual priority from the instance
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to process post processor ${postProcessorClass.qualifiedName?.asString()}",
                    postProcessorClass
                )
                null
            }
        }

        postProcessors.getOrPut(moduleName) { mutableSetOf() }.addAll(postProcessorMetas)
    }


    override fun finish() {
        generatePluginComponentFile()
        components.clear()
        postProcessors.clear()
    }


    private fun generatePluginComponentFile() {
        val allModules = (components.keys + postProcessors.keys).toSet()

        if (allModules.isEmpty()) {
            return
        }

        for (moduleName in allModules) {
            val moduleComponents = components[moduleName]?.toList() ?: emptyList()
            val modulePostProcessors = postProcessors[moduleName]?.toList() ?: emptyList()

            if (moduleComponents.isEmpty() && modulePostProcessors.isEmpty()) {
                continue
            }

            val componentMeta = PluginComponentMeta(moduleComponents, modulePostProcessors)
            val filePath = "$COMPONENTS_DIRECTORY/$moduleName.json"
            try {
                codeGenerator.createNewFileByPath(Dependencies(aggregating = true), filePath, "")
                    .bufferedWriter()
                    .use { writer ->
                        val jsonString = json.encodeToString(componentMeta)
                        writer.write(jsonString)
                    }

                logger.info("Wrote Components to: $filePath")
            } catch (e: IOException) {
                logger.error("Unable to create $filePath, $e")
            }
        }
    }
}
