package dev.slne.surf.api.processor.nms

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.slne.surf.api.processor.ClassNames
import dev.slne.surf.api.processor.util.AnnotationUtils
import dev.slne.surf.api.processor.util.getArgumentValueAs
import dev.slne.surf.api.processor.util.toBinaryName
import dev.slne.surf.api.shared.internal.nms.NmsProviderConfig
import dev.slne.surf.api.shared.internal.nms.NmsProviderMeta
import dev.slne.surf.api.shared.internal.nms.NmsVersion

class NmsProvidersCollector(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val nmsProviders = mutableMapOf<String, MutableList<NmsProviderMeta>>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val metas = AnnotationUtils.findAnnotatedClasses(
            resolver = resolver,
            annotationFqName = ClassNames.NMS_PROVIDER,
            includeMetaAnnotations = false,
            excludeAnnotationClasses = false,
            logger = logger
        ).mapNotNull(fun(nmsProviderClass): NmsProviderMeta? {
            val annotation = nmsProviderClass.findNmsProviderAnnotation() ?: return null
            val rawVersion = annotation.getArgumentValueAs<KSClassDeclaration>("version") ?: return null
            if (rawVersion.classKind != ClassKind.ENUM_ENTRY) return null

            val version = try {
                NmsVersion.valueOf(rawVersion.simpleName.asString())
            } catch (_: Throwable) {
                logger.error("Unknown NmsVersion: ${rawVersion.simpleName.asString()}")
                return null
            }

            return NmsProviderMeta(version, nmsProviderClass.toBinaryName())
        }).toList()

        val module = resolver.getModuleName().asString()
        nmsProviders.computeIfAbsent(module) { mutableListOf() }.addAll(metas)

        return emptyList()
    }

    private fun KSAnnotated.findNmsProviderAnnotation() = annotations.find {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == ClassNames.NMS_PROVIDER
    }

    override fun finish() {
        for ((module, providers) in nmsProviders) {
            if (providers.isEmpty()) continue

            val filePath = "${NmsProviderConfig.NMS_PROVIDERS_DIRECTORY}/$module.json"
            try {
                codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, filePath, "")
                    .bufferedWriter()
                    .use { writer ->
                        val jsonString = NmsProviderConfig.json.encodeToString(providers)
                        writer.write(jsonString)
                    }

                logger.info("Wrote NMS providers to: $filePath")
            } catch (e: Exception) {
                logger.error("Unable to create $filePath, $e")
            }
        }

        nmsProviders.clear()
    }
}