package dev.slne.surf.surfapi.processor.hook

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.slne.surf.surfapi.processor.util.nameOf
import dev.slne.surf.surfapi.processor.util.toBinaryName
import dev.slne.surf.surfapi.shared.api.hook.HookMeta
import dev.slne.surf.surfapi.shared.api.hook.requirement.DependsOnClass
import dev.slne.surf.surfapi.shared.api.hook.requirement.DependsOnClassName
import dev.slne.surf.surfapi.shared.api.hook.requirement.DependsOnOnePlugin
import dev.slne.surf.surfapi.shared.api.hook.requirement.DependsOnPlugin
import dev.slne.surf.surfapi.shared.internal.hook.HooksConfig.HOOKS_FILE_NAME
import dev.slne.surf.surfapi.shared.internal.hook.HooksConfig.json
import dev.slne.surf.surfapi.shared.internal.hook.PluginHookMeta
import java.io.IOException

class HookSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    companion object {
        private val HOOK_ANNOTATION = nameOf<HookMeta>()
        private val DEPENDS_ON_CLASS_ANNOTATION = nameOf<DependsOnClass>()
        private val DEPENDS_ON_CLASS_NAME_ANNOTATION = nameOf<DependsOnClassName>()
        private val DEPENDS_ON_ONE_PLUGIN_ANNOTATION = nameOf<DependsOnOnePlugin>()
        private val DEPENDS_ON_PLUGIN_ANNOTATION = nameOf<DependsOnPlugin>()
    }

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val hooks = mutableSetOf<PluginHookMeta.Hook>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        val hooksMetas = resolver.getSymbolsWithAnnotation(HOOK_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { hookClass ->
                var hasUnresolvedClassDependency = false
                val hookMeta = hookClass.annotations.findAnnotation(HOOK_ANNOTATION) ?: run {
                    logger.error("@HookMeta annotation not found on element", hookClass)
                    return@mapNotNull null
                }

                val priority = hookMeta.arguments.find { it.name?.asString() == "priority" }?.value as? Short ?: 0
                val dependsOnClass = hookClass.annotations.findAnnotations(DEPENDS_ON_CLASS_ANNOTATION)
                    .mapNotNull { annotation ->
                        val clazzValue = annotation.arguments.find { it.name?.asString() == "clazz" }?.value as? KSType
                        if (clazzValue == null) {
                            logger.error("DependsOnClass annotation must have 'clazz' parameter", annotation)
                            return@mapNotNull null
                        }

                        if (clazzValue.isError) {
                            deferred += hookClass
                            hasUnresolvedClassDependency = true
                            return@mapNotNull null
                        }

                        val closestClass = clazzValue.declaration.closestClassDeclaration()
                        if (closestClass == null) {
                            deferred += hookClass
                            hasUnresolvedClassDependency = true
                            return@mapNotNull null
                        }
                        closestClass.toBinaryName()
                    }

                if (hasUnresolvedClassDependency) {
                    return@mapNotNull null
                }

                val dependsOnClassName = hookClass.annotations.findAnnotations(DEPENDS_ON_CLASS_NAME_ANNOTATION)
                    .mapNotNull { annotation ->
                        val classNameValue =
                            annotation.arguments.find { it.name?.asString() == "className" }?.value as? String
                        if (classNameValue == null) {
                            logger.error("@DependsOnClassName annotation must have 'className' parameter", annotation)
                            return@mapNotNull null
                        }
                        classNameValue
                    }

                val dependsOnOnePlugin = hookClass.annotations.findAnnotations(DEPENDS_ON_ONE_PLUGIN_ANNOTATION)
                    .mapNotNull { annotation ->
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

                val dependsOnPlugin = hookClass.annotations.findAnnotations(DEPENDS_ON_PLUGIN_ANNOTATION)
                    .mapNotNull { annotation ->
                        val argValue = annotation.arguments.find { it.name?.asString() == "pluginId" }?.value
                        val pluginId = argValue as? String
                        if (pluginId == null) {
                            logger.error("@DependsOnPlugin annotation must have 'pluginId' parameter", annotation)
                            return@mapNotNull null
                        }
                        pluginId
                    }

                PluginHookMeta.Hook(
                    priority = priority,
                    className = hookClass.toBinaryName(),
                    classDependencies = dependsOnClass.toList() + dependsOnClassName.toList(),
                    pluginDependencies = dependsOnPlugin.toList(),
                    pluginOneDependencies = dependsOnOnePlugin.toList()
                )
            }.toList()

        hooks.addAll(hooksMetas)
        return deferred
    }

    override fun finish() {
        generatePluginHookFile()
    }


    private fun generatePluginHookFile() {
        if (hooks.isEmpty()) {
            return
        }

        val hookMeta = PluginHookMeta(hooks.toList())
        try {
            codeGenerator.createNewFileByPath(Dependencies(aggregating = true), HOOKS_FILE_NAME, "").bufferedWriter()
                .use { writer ->
                    val jsonString = json.encodeToString(hookMeta)
                    writer.write(jsonString)
                }

            logger.info("Wrote Hooks to: $HOOKS_FILE_NAME")
        } catch (e: IOException) {
            logger.error("Unable to create $HOOKS_FILE_NAME, $e")
        }
    }

    private fun Sequence<KSAnnotation>.findAnnotation(annotationClassName: String): KSAnnotation? {
        return this.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationClassName
        }
    }

    private fun Sequence<KSAnnotation>.findAnnotations(annotationClassName: String): Sequence<KSAnnotation> {
        return this.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationClassName
        }
    }
}
