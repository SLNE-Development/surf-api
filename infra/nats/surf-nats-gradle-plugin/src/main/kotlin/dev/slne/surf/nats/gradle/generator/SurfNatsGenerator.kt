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
 * Generates Kotlin contracts from Protobuf source files.
 *
 * The same sources and settings always produce the same files. Every problem is collected before any file is
 * written, so a failed run leaves the output directory unchanged. Instances hold no state and can be reused.
 */
class SurfNatsGenerator {

    /**
     * Generates the contracts of [input] into [outputDir].
     *
     * Replaces the contents of [outputDir] only when generation reports no problem. Without `.proto` sources,
     * [outputDir] is emptied.
     *
     * @param input The sources, type mappings and visibility.
     * @param outputDir The directory that receives one Kotlin file per `.proto` file with generated declarations.
     * @return The generated files, sorted by path.
     * @throws SurfNatsGeneratorException when a schema cannot be loaded, a type mapping is invalid, or the sources
     *   violate the contract rules. It lists every problem found.
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
     * Loads the schema and builds the Kotlin files in source order without writing them.
     *
     * File rules, type registration and contract resolution report to [problems] in that order. Emission reports
     * nothing.
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
     * Resolves the explicit type mappings over the platform defaults.
     *
     * The last mapping of a Protobuf type wins. Explicit mappings of `google.protobuf.Empty` are reported, because
     * that type is always `kotlin.Unit`. Mappings with invalid class names are reported and left out.
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
     * Returns the class [name] of the mapping of [protoType], or `null` after reporting an invalid name.
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
     * Builds the Kotlin file of [file]: its types, then its services, with a header comment naming the source and
     * a file-level opt-in.
     *
     * @return The file, or `null` when [file] has no generated declaration.
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
     * Returns `@file:OptIn` for `NatsGeneratedApi` and `ExperimentalSerializationApi`, which generated code uses.
     */
    private fun generatedApiOptIn(): AnnotationSpec {
        return AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
            .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
            .addMember("%T::class", KotlinTypes.NATS_GENERATED_API)
            .addMember("%T::class", KotlinTypes.EXPERIMENTAL_SERIALIZATION_API)
            .build()
    }

    /**
     * Replaces [outputDir] with [files] and returns them sorted by path.
     *
     * Call it only after [Problems.throwIfAny], so that a failed run keeps the previous output.
     */
    private fun writeFiles(files: List<FileSpec>, outputDir: File): List<File> {
        outputDir.deleteRecursively()

        return files.map { writeFile(it, outputDir) }
            .sortedBy { it.invariantSeparatorsPath }
    }

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
