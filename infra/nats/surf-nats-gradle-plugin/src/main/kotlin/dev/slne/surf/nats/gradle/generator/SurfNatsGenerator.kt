package dev.slne.surf.nats.gradle.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.codegen.ServiceEmitter
import dev.slne.surf.nats.gradle.codegen.TypeEmitter
import dev.slne.surf.nats.gradle.contract.ContractResolver
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.gradle.model.MappedClass
import dev.slne.surf.nats.gradle.schema.containsProtos
import dev.slne.surf.nats.gradle.schema.loadSchema
import dev.slne.surf.nats.gradle.validate.FileRules
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.gradle.validate.kotlinPackage
import java.io.File

/**
 * Generates Kotlin contracts deterministically from Protobuf source files.
 *
 * Resolves type mappings, validates the loaded schema, and generates Kotlin
 * declarations for messages, enums, and services.
 *
 * All generation problems are collected before writing output files, ensuring
 * that validation failures do not modify existing generated sources.
 */
class SurfNatsGenerator {

    /**
     * Generates Kotlin contracts from the configured Protobuf sources.
     *
     * Replaces the contents of [outputDir] only after all validation and code
     * generation steps complete without reported problems.
     *
     * If no Protobuf sources exist, the output directory is cleared and an
     * empty generation result is returned.
     *
     * @param input the source directories, type mappings, and generation settings
     * @param outputDir the directory receiving the generated Kotlin files
     * @return the generated files, sorted by their normalized paths
     * @throws SurfNatsGeneratorException if generation problems are reported
     */
    fun generate(input: GeneratorInput, outputDir: File): GenerationResult {
        val problems = Problems()
        val mappings = mappings(input, problems)

        if (input.sourceDirs.none { it.containsProtos() }) {
            problems.throwIfAny()
            outputDir.deleteRecursively()
            outputDir.mkdirs()
            return GenerationResult(emptyList())
        }

        val files = generateFileSpecs(input, mappings, problems)
        problems.throwIfAny()

        return GenerationResult(writeFiles(files, outputDir))
    }

    /**
     * Loads and validates the Protobuf schema, then generates Kotlin file specifications.
     *
     * File rules, type registration and contract resolution report to [problems] in that order.
     * Emission reports nothing, and no output file is modified here.
     *
     * @param input the Protobuf sources and generation settings
     * @param mappings the resolved mappings to existing Kotlin types
     * @param problems the collector receiving validation errors
     * @return the generated Kotlin file specifications in source order
     */
    private fun generateFileSpecs(
        input: GeneratorInput,
        mappings: Map<ProtoType, MappedClass>,
        problems: Problems,
    ): List<FileSpec> {
        val loaded = loadSchema(input.sourceDirs, input.protoPathDirs)
        val sourceFiles = loaded.sourceFiles

        val rules = FileRules(problems)
        for (file in sourceFiles) {
            rules.check(file)
        }

        val types = KotlinTypeResolver(loaded.schema, sourceFiles, mappings, problems)
        val contract = ContractResolver(types).resolve(sourceFiles)
        val typeEmitter = TypeEmitter(types, contract, input.visibility)
        val serviceEmitter = ServiceEmitter(types, contract, input.visibility)

        return sourceFiles.mapNotNull {
            fileSpec(it, types, typeEmitter, serviceEmitter)
        }
    }

    /**
     * Resolves explicit type mappings over the platform defaults.
     *
     * Later mappings of the same Protobuf type take precedence. Explicit
     * mappings for `google.protobuf.Empty` are rejected because that type
     * is always represented by Kotlin Unit.
     *
     * Invalid class names are reported and their mappings are omitted.
     *
     * @param input the configured explicit and default type mappings
     * @param problems the collector receiving invalid mapping errors
     * @return the successfully resolved mappings keyed by Protobuf type
     */
    private fun mappings(input: GeneratorInput, problems: Problems): Map<ProtoType, MappedClass> {
        if (input.typeMappings.any { it.protoType == ProtoType.EMPTY }) {
            problems.error(
                "mapType(${ProtoType.EMPTY})",
                "${ProtoType.EMPTY} is always kotlin.Unit and cannot be remapped"
            )
        }

        val effective =
            (input.defaultTypeMappings + input.typeMappings.filter { it.protoType != ProtoType.EMPTY })
                .associateBy { it.protoType }

        val result = LinkedHashMap<ProtoType, MappedClass>()
        for ((protoType, mapping) in effective) {
            val kotlinClass = className(mapping.kotlinClass, protoType, problems) ?: continue
            val serializer = className(mapping.serializerClass, protoType, problems) ?: continue
            result[protoType] = MappedClass(kotlinClass, serializer)
        }

        return result
    }

    /**
     * Resolves a fully qualified Kotlin class name.
     *
     * Reports an invalid mapping when the supplied name cannot be interpreted
     * as a Kotlin class name.
     *
     * @param name the fully qualified Kotlin class name
     * @param protoType the Protobuf type associated with the mapping
     * @param problems the collector receiving invalid class name errors
     * @return the resolved class name, or `null` if the name is invalid
     */
    private fun className(name: String, protoType: ProtoType, problems: Problems): ClassName? {
        return try {
            ClassName.bestGuess(name)
        } catch (_: IllegalArgumentException) {
            problems.error(
                "mapType($protoType)",
                "'$name' is not a fully qualified Kotlin class name"
            )
            null
        }
    }

    /**
     * Generates a Kotlin file specification for a Protobuf source file.
     *
     * Emits supported message, enum, and service declarations in their
     * original order. Generated files include the required opt-in annotations
     * and a comment identifying their Protobuf source.
     *
     * @param file the Protobuf source file to process
     * @param types the resolver for generated and mapped Kotlin types
     * @param typeEmitter the generator for message and enum declarations
     * @param serviceEmitter the generator for service declarations
     * @return the Kotlin file specification, or `null` if no declarations are generated
     */
    private fun fileSpec(
        file: ProtoFile,
        types: KotlinTypeResolver,
        typeEmitter: TypeEmitter,
        serviceEmitter: ServiceEmitter,
    ): FileSpec? {
        val typeSpecs = file.types.mapNotNull(typeEmitter::emit)
        val serviceSpecs = file.services.map(serviceEmitter::emit)
        if (typeSpecs.isEmpty() && serviceSpecs.isEmpty()) return null

        return FileSpec.builder(file.kotlinPackage, types.fileName(file))
            .indent("    ")
            .addFileComment("Generated by surf-nats from %L. Do not edit.", file.location.path)
            .addAnnotation(generatedApiOptIn())
            .apply {
                typeSpecs.forEach(::addType)
                serviceSpecs.forEach(::addType)
            }
            .build()
    }

    /**
     * Creates the file-level opt-in annotation required by generated contracts.
     *
     * Enables access to the NATS generated API and experimental Kotlin
     * serialization APIs used by the generated declarations.
     *
     * @return the file-targeted Kotlin OptIn annotation
     */
    private fun generatedApiOptIn(): AnnotationSpec {
        return AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
            .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
            .addMember("%T::class", KotlinTypes.NATS_GENERATED_API)
            .addMember("%T::class", KotlinTypes.EXPERIMENTAL_SERIALIZATION_API)
            .build()
    }

    /**
     * Replaces the output directory with the generated Kotlin files.
     *
     * Files are written in generation order and returned sorted by their
     * platform-independent paths.
     *
     * This method must only be called after generation problems have been
     * checked to avoid modifying output files when validation fails.
     *
     * @param files the generated Kotlin file specifications
     * @param outputDir the destination directory to replace
     * @return the written files sorted by normalized path
     */
    private fun writeFiles(files: List<FileSpec>, outputDir: File): List<File> {
        outputDir.deleteRecursively()

        return files.map { writeFile(it, outputDir) }
            .sortedBy { it.invariantSeparatorsPath }
    }

    /**
     * Writes a generated Kotlin file to its package directory.
     *
     * Creates the package directory when necessary and writes the rendered
     * Kotlin source using UTF-8 encoding.
     *
     * @param spec the Kotlin file specification to write
     * @param outputDir the root directory for generated sources
     * @return the written Kotlin source file
     */
    private fun writeFile(spec: FileSpec, outputDir: File): File {
        val directory = if (spec.packageName.isEmpty()) {
            outputDir
        } else {
            outputDir.resolve(spec.packageName.replace('.', '/'))
        }

        directory.mkdirs()

        return directory.resolve("${spec.name}.kt").also {
            it.writeText(spec.toString(), Charsets.UTF_8)
        }
    }
}
