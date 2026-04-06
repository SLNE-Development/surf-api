package dev.slne.surf.api.processor.autoservice

import com.google.auto.service.AutoService
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import dev.slne.surf.api.processor.util.toBinaryName
import java.io.IOException

class AutoServiceSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    companion object {
        private val SERVICE_ANNOTATION = AutoService::class.java.name
    }

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    // Map: serviceFqName -> set of (impl binary name, source file)
    private val providers = mutableMapOf<String, MutableSet<Pair<String, KSFile?>>>()

    private val verify = environment.options["autoserviceKsp.verify"]?.toBoolean() == true
    private val verbose = environment.options["autoserviceKsp.verbose"]?.toBoolean() == true


    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()

        resolver.getSymbolsWithAnnotation(SERVICE_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { implementer ->
                val annotation = implementer.annotations.firstOrNull {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == SERVICE_ANNOTATION
                } ?: run {
                    logger.error("@AutoService annotation not found on element", implementer)
                    return@forEach
                }

                val argValue =
                    annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value
                val providerTypes: List<KSType> = when (argValue) {
                    is List<*> -> argValue.filterIsInstance<KSType>()
                    is KSType -> listOf(argValue)
                    null -> emptyList()
                    else -> emptyList()
                }

                if (providerTypes.isEmpty()) {
                    logger.error(
                        "No service interfaces specified in @AutoService. " +
                                "Use @AutoService(YourService::class).",
                        annotation
                    )
                }

                for (providerType in providerTypes) {
                    if (providerType.isError) {
                        deferred += implementer
                        continue
                    }

                    val providerDecl = providerType.declaration.closestClassDeclaration()
                    if (providerDecl == null) {
                        deferred += implementer
                        continue
                    }

                    when (checkImplementer(implementer, providerType)) {
                        ValidationResult.VALID -> {
                            val key = providerDecl.toBinaryName()
                            val impl = implementer.toBinaryName()
                            providers.getOrPut(key) { mutableSetOf() }
                                .add(impl to implementer.containingFile)
                        }

                        ValidationResult.INVALID -> {
                            logger.error(
                                "Service providers must implement their service interface. " +
                                        "${implementer.qualifiedName?.asString()} does not implement " +
                                        providerDecl.qualifiedName?.asString(),
                                implementer
                            )
                        }

                        ValidationResult.DEFERRED -> {
                            deferred += implementer
                        }
                    }
                }
            }

        return deferred
    }

    override fun finish() {
        generateAndClearConfigFiles()
    }

    private fun generateAndClearConfigFiles() {
        for ((serviceFqName, impls) in providers) {
            val resourcePath = "META-INF/services/$serviceFqName"
            log("Working on resource file: $resourcePath")

            try {
                val allServices = impls.asSequence().map { it.first }.toSortedSet()
                log("New service file contents: $allServices")

                val ksFiles = impls.mapNotNull { it.second }
                log("Originating files: ${ksFiles.map(KSFile::fileName)}")

                val deps = if (ksFiles.isEmpty()) {
                    Dependencies(aggregating = true)
                } else {
                    Dependencies(aggregating = true, sources = ksFiles.toTypedArray())
                }

                codeGenerator.createNewFileByPath(deps, resourcePath, "").bufferedWriter()
                    .use { writer ->
                        for (service in allServices) {
                            writer.write(service)
                            writer.newLine()
                        }
                    }

                log("Wrote to: $resourcePath")
            } catch (e: IOException) {
                logger.error("Unable to create $resourcePath, $e")
            }
        }
    }

    private fun log(message: String) {
        if (verbose) {
            logger.info(message)
        }
    }

    private fun checkImplementer(
        implementer: KSClassDeclaration,
        providerType: KSType,
    ): ValidationResult {
        if (!verify) return ValidationResult.VALID

        for (superType in implementer.getAllSuperTypes()) {
            if (superType.isError) return ValidationResult.DEFERRED
            // Accept equal types or assignable in either direction
            if (superType == providerType ||
                superType.isAssignableFrom(providerType) ||
                providerType.isAssignableFrom(superType)
            ) {
                return ValidationResult.VALID
            }
        }

        return ValidationResult.INVALID
    }

    private enum class ValidationResult { VALID, INVALID, DEFERRED }
}