package dev.slne.surf.nats.gradle.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Type
import dev.slne.surf.nats.gradle.codegen.EventKey
import dev.slne.surf.nats.gradle.codegen.FileConstants
import dev.slne.surf.nats.gradle.codegen.ServiceEmitter
import dev.slne.surf.nats.gradle.codegen.ServiceKey
import dev.slne.surf.nats.gradle.codegen.TypeEmitter
import dev.slne.surf.nats.gradle.model.ContractModel
import dev.slne.surf.nats.gradle.model.KotlinNaming
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.gradle.model.MappedClass
import dev.slne.surf.nats.gradle.schema.containsProtos
import dev.slne.surf.nats.gradle.schema.loadSchema
import dev.slne.surf.nats.gradle.validate.FileFacts
import dev.slne.surf.nats.gradle.validate.FileRules
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import java.io.File

/**
 * Generates Kotlin contracts deterministically from the `.proto` files of [GeneratorInput.sourceDirs].
 */
class SurfNatsGenerator {

    /**
     * Replaces the content of [outputDir], or throws [SurfNatsGeneratorException] without writing when a problem exists.
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

        val loaded = loadSchema(input.sourceDirs, input.protoPathDirs)
        val model = ContractModel(loaded.schema, mappings, problems)
        val rules = FileRules(problems)
        val facts = loaded.sourceFiles.map(rules::check)
        register(facts, model, problems)

        val events = HashMap<EventKey, Location>()
        val services = HashMap<ServiceKey, Location>()
        val typeEmitter = TypeEmitter(model, input.visibility, events)
        val serviceEmitter = ServiceEmitter(model, input.visibility, services)
        val files = facts.mapNotNull { fileSpec(it, typeEmitter, serviceEmitter) }
        problems.throwIfAny()

        outputDir.deleteRecursively()
        val written = files.map { spec ->
            val directory = if (spec.packageName.isEmpty()) outputDir else outputDir.resolve(spec.packageName.replace('.', '/'))
            directory.mkdirs()
            directory.resolve("${spec.name}.kt").also { it.writeText(spec.toString(), Charsets.UTF_8) }
        }
        return GenerationResult(written.sortedBy { it.invariantSeparatorsPath })
    }

    private fun mappings(input: GeneratorInput, problems: Problems): Map<String, MappedClass> {
        val empty = ProtocolFieldRules.EMPTY_TYPE
        if (input.typeMappings.any { it.protoType == empty }) {
            problems.error("mapType($empty)", "$empty is always kotlin.Unit and cannot be remapped")
        }
        val overrides = input.typeMappings.filter { it.protoType != empty }
        val result = LinkedHashMap<String, MappedClass>()
        for ((protoType, mapping) in DefaultTypeMappings.resolve(input.defaultTypeMappings, overrides)) {
            val kotlinClass = className(mapping.kotlinClass, protoType, problems) ?: continue
            val serializer = className(mapping.serializerClass, protoType, problems) ?: continue
            result[protoType] = MappedClass(kotlinClass, serializer)
        }
        return result
    }

    private fun className(name: String, protoType: String, problems: Problems): ClassName? {
        return try {
            ClassName.bestGuess(name)
        } catch (_: IllegalArgumentException) {
            problems.error("mapType($protoType)", "'$name' is not a fully qualified Kotlin class name")
            null
        }
    }

    private fun register(facts: List<FileFacts>, model: ContractModel, problems: Problems) {
        val classes = HashMap<String, Location>()
        val files = HashMap<String, Location>()

        fun claim(className: ClassName, location: Location) {
            classes.put(className.canonicalName, location)?.let { previous ->
                problems.error(location, "Kotlin class ${className.canonicalName} is already generated for $previous")
            }
        }

        fun registerType(type: Type, className: ClassName) {
            if (model.isMapped(type.type)) return
            model.register(type.type, className)
            claim(className, type.location)
            for (nested in type.nestedTypes) registerType(nested, className.nestedClass(nested.type.simpleName))
        }

        for (fact in facts) {
            val fileKey = "${fact.kotlinPackage}/${fileName(fact.file)}".lowercase()
            files.put(fileKey, fact.file.location)?.let { previous ->
                problems.error(
                    fact.file.location,
                    "generates ${fileName(fact.file)}.kt in package '${fact.kotlinPackage}', which $previous already generates",
                )
            }
            for (type in fact.file.types) registerType(type, ClassName(fact.kotlinPackage, type.type.simpleName))
            for (service in fact.file.services) claim(ClassName(fact.kotlinPackage, service.name), service.location)
        }
    }

    private fun fileSpec(facts: FileFacts, typeEmitter: TypeEmitter, serviceEmitter: ServiceEmitter): FileSpec? {
        val file = facts.file
        val types = file.types.mapNotNull { type ->
            typeEmitter.emit(type, ClassName(facts.kotlinPackage, type.type.simpleName), facts.majorVersion)
        }
        val constants = FileConstants()
        val services = file.services.map { service ->
            serviceEmitter.emit(service, ClassName(facts.kotlinPackage, service.name), facts.majorVersion, constants)
        }
        if (types.isEmpty() && services.isEmpty()) return null

        return FileSpec.builder(facts.kotlinPackage, fileName(file))
            .indent("    ")
            .addFileComment("Generated by surf-nats from %L. Do not edit.", file.location.path)
            .addAnnotation(
                // OptIn::class may only be used as an annotation
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%T::class", KotlinTypes.NATS_GENERATED_API)
                    .addMember("%T::class", KotlinTypes.EXPERIMENTAL_SERIALIZATION_API)
                    .build()
            )
            .apply {
                types.forEach(::addType)
                services.forEach(::addType)
                constants.properties.forEach(::addProperty)
            }
            .build()
    }

    private fun fileName(file: ProtoFile): String {
        val name = KotlinNaming.upperCamel(file.name()).map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
        return name.ifEmpty { "Contracts" }
    }
}
