package dev.slne.surf.surfapi.processor.component

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.slne.surf.surfapi.processor.ClassNames
import dev.slne.surf.surfapi.processor.util.*
import dev.slne.surf.surfapi.shared.internal.hook.ComponentsConfig.COMPONENTS_DIRECTORY
import dev.slne.surf.surfapi.shared.internal.hook.ComponentsConfig.json
import dev.slne.surf.surfapi.shared.internal.hook.PluginComponentMeta
import java.io.IOException

class ComponentSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    private val components = mutableMapOf<String, MutableSet<PluginComponentMeta.Component>>()
    private val postProcessors = mutableMapOf<String, MutableSet<PluginComponentMeta.PostProcessor>>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleName = resolver.getModuleName().asString()
        val deferred = mutableListOf<KSAnnotated>()

        processComponents(resolver, moduleName, deferred)
        processPostProcessors(resolver, moduleName)

        return deferred
    }

    override fun finish() {
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
                codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, filePath, "")
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

        components.clear()
        postProcessors.clear()
    }

    private fun processComponents(
        resolver: Resolver,
        moduleName: String,
        deferred: MutableList<KSAnnotated>
    ) {
        val processedComponents = findComponents(resolver)
            .mapNotNull { component ->
                processComponent(component, deferred)
            }
            .toList()

        if (processedComponents.isNotEmpty()) {
            val moduleComponents = components.getOrPut(moduleName) { mutableSetOf() }
            moduleComponents.addAll(processedComponents)
        }
    }

    private fun processComponent(
        component: KSClassDeclaration,
        deferred: MutableList<KSAnnotated>
    ): PluginComponentMeta.Component? {
        val priority = component.findAnnotationRecursive(ClassNames.PRIORITY)
            ?.getArgumentValueAs<Number>("value")
            ?: 0

        val dependsOnClass =
            collectKSTypeAnnotationClassNames(component, ClassNames.DEPENDS_ON_CLASS, "clazz", deferred) ?: return null

        val dependsOnClassName = component.findAllAnnotationsRecursive(ClassNames.DEPENDS_ON_CLASS_NAME)
            .collectArgumentValues<String>("className")

        val dependsOnOnePlugin = component.findAllAnnotationsRecursive(ClassNames.DEPENDS_ON_ONE_PLUGIN)
            .collectArgumentValuesGrouped<String>("pluginIds")

        val dependsOnPlugin = component.findAllAnnotationsRecursive(ClassNames.DEPENDS_ON_PLUGIN)
            .collectArgumentValues<String>("pluginId")

        val dependsOnComponent =
            collectKSTypeAnnotationClassNames(component, ClassNames.DEPENDS_ON_COMPONENT, "component", deferred)
                ?: return null

        val conditionalOn =
            collectKSTypeAnnotationClassNames(component, ClassNames.CONDITIONAL_ON, "condition", deferred)
                ?: return null

        return PluginComponentMeta.Component(
            className = component.toBinaryName(),
            priority = priority.toShort(),
            classDependencies = dependsOnClass + dependsOnClassName,
            pluginOneDependencies = dependsOnOnePlugin,
            pluginDependencies = dependsOnPlugin,
            componentDependencies = dependsOnComponent,
            customConditions = conditionalOn
        )
    }

    private fun processPostProcessors(resolver: Resolver, moduleName: String) {
        val postProcessorClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { declaration ->
                declaration.getAllSuperTypes().any { superType ->
                    superType.declaration.qualifiedName?.asString() == ClassNames.COMPONENT_POST_PROCESSOR
                }
            }
            .filter { classDecl ->
                classDecl.qualifiedName?.asString() != ClassNames.COMPONENT_POST_PROCESSOR
            }
            .toList()

        val postProcessorMetas = postProcessorClasses.map { postProcessorClass ->
            PluginComponentMeta.PostProcessor(
                className = postProcessorClass.toBinaryName(),
            )
        }

        postProcessors.getOrPut(moduleName) { mutableSetOf() }.addAll(postProcessorMetas)
    }

    private fun collectKSTypeAnnotationClassNames(
        component: KSClassDeclaration,
        annotation: String,
        annotationArgumentName: String,
        deferred: MutableList<KSAnnotated>
    ): List<String>? {
        val types = component.findAllAnnotationsRecursive(annotation)
            .collectArgumentValues<KSType>(annotationArgumentName)

        val result = mutableListOf<String>()

        for (type in types) {
            if (type.isError) {
                deferred += component
                return null
            }

            val closestClass = type.declaration.closestClassDeclaration()
            if (closestClass == null) {
                deferred += component
                return null
            }

            result += closestClass.toBinaryName()
        }

        return result
    }

    private fun findComponents(resolver: Resolver): Sequence<KSClassDeclaration> {
        return AnnotationUtils.findAnnotatedClasses(
            resolver = resolver,
            annotationFqName = ClassNames.COMPONENT_META,
            includeMetaAnnotations = true,
            excludeAnnotationClasses = true,
            logger = logger
        )
    }
}