package dev.slne.surf.nats.gradle.model

import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.gradle.validate.kotlinPackage

/**
 * Defines the integer encoding used by kotlinx-serialization-protobuf.
 */
internal enum class IntEncoding {
    /** Standard Protobuf integer encoding. */
    DEFAULT,

    /** ZigZag encoding for signed integers. */
    SIGNED,

    /** Fixed-width integer encoding. */
    FIXED
}

/**
 * Represents an existing Kotlin class mapped to a Protobuf type.
 *
 * @property kotlinClass the Kotlin class representing the Protobuf type
 * @property serializer the serializer used for the mapped class
 */
internal class MappedClass(
    val kotlinClass: ClassName,
    val serializer: ClassName,
)

/**
 * Describes the Kotlin representation of a Protobuf field element.
 *
 * Contains the resolved Kotlin type, optional serializer, integer encoding,
 * and metadata required for field validation and code generation.
 *
 * @property protoType the original Protobuf type
 * @property kotlinType the resolved Kotlin class
 * @property serializer the serializer for a mapped type, or `null` for generated and scalar types
 * @property encoding the integer encoding, or `null` for types that ignore `@ProtoType`
 * @property packable whether the element supports packed repeated fields
 * @property enumType the generated enum schema, or `null` for other or mapped types
 * @property isMessage whether the element represents a non-enum message type
 */
internal class ElementType(
    val protoType: ProtoType,
    val kotlinType: ClassName,
    val serializer: ClassName?,
    val encoding: IntEncoding?,
    val packable: Boolean,
    val enumType: EnumType?,
    val isMessage: Boolean,
) {
    /**
     * Whether this element uses an explicitly configured type mapping.
     */
    val isMapped: Boolean get() = serializer != null

    /**
     * Whether this element represents a Protobuf bytes field.
     */
    val isBytes: Boolean get() = protoType == ProtoType.BYTES

    /**
     * Returns the Kotlin type as used in a serializable declaration.
     *
     * Mapped types receive a type-use `@Serializable(with = ...)` annotation
     * referencing their configured serializer.
     */
    val usageType: TypeName
        get() = serializer?.let {
            kotlinType.copy(annotations = listOf(KotlinTypes.serializableWith(it)))
        } ?: kotlinType
}

/**
 * Resolves Protobuf declarations to their generated or mapped Kotlin types.
 *
 * Messages, enums, and services declared in source files receive Kotlin
 * class names. Explicitly mapped messages and enums are not generated,
 * and nested declarations of mapped messages are excluded as well.
 *
 * Types originating from the Protobuf import path must be explicitly mapped,
 * since only declarations from source files are generated.
 *
 * Top-level declarations use their source file's Kotlin package, while nested
 * declarations become nested Kotlin classes. Each source file produces one
 * Kotlin file, whose name is determined by [fileName].
 *
 * During initialization, the resolver reports duplicate Protobuf names,
 * Kotlin class name collisions, and generated file name collisions.
 *
 * @property schema the linked Protobuf schema
 * @property problems the collector receiving validation errors
 */
internal class KotlinTypeResolver(
    val schema: Schema,
    sourceFiles: List<ProtoFile>,
    private val mappings: Map<ProtoType, MappedClass>,
    val problems: Problems,
) {
    private val generated: Map<ProtoType, ClassName> = generatedClasses(sourceFiles)

    /**
     * Checks whether a Protobuf type has an explicit Kotlin mapping.
     *
     * @param protoType the Protobuf type to inspect
     * @return `true` if the type is mapped
     */
    fun isMapped(protoType: ProtoType): Boolean = protoType in mappings

    /**
     * Returns the configured Kotlin mapping for a Protobuf type.
     *
     * @param protoType the Protobuf type to resolve
     * @return its mapping, or `null` if no mapping exists
     */
    fun mapping(protoType: ProtoType): MappedClass? = mappings[protoType]

    /**
     * Returns the Kotlin class generated for a Protobuf declaration.
     *
     * Supports messages, enums, and services registered from source files.
     * Explicitly mapped declarations are excluded from the generated classes.
     *
     * @param protoType the Protobuf declaration to resolve
     * @return the generated Kotlin class, or `null` if it is not generated
     */
    fun generatedClass(protoType: ProtoType): ClassName? = generated[protoType]

    /**
     * Determines the Kotlin file name for a Protobuf source file.
     *
     * Converts the source file name to UpperCamelCase and replaces characters
     * that are neither letters nor digits with underscores.
     *
     * Uses `Contracts` if the resulting name is empty.
     *
     * @param file the Protobuf source file
     * @return the generated Kotlin file name without the `.kt` extension
     */
    fun fileName(file: ProtoFile): String {
        val name = KotlinNaming.upperCamel(file.name())
            .map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")

        return name.ifEmpty { "Contracts" }
    }

    /**
     * Resolves a non-map Protobuf field element to its Kotlin representation.
     *
     * Scalar types use their built-in mappings, while message and enum types
     * are resolved through generated classes or explicit mappings.
     *
     * `google.protobuf.Empty` is rejected because it is only supported
     * as an RPC request or response.
     *
     * @param protoType the Protobuf element type
     * @param location the source location used when reporting errors
     * @return the resolved element, or `null` if the type is unsupported or unknown
     */
    fun element(protoType: ProtoType, location: Location): ElementType? {
        if (protoType == ProtoType.EMPTY) {
            problems.error(
                location,
                "${ProtoType.EMPTY} can only be an rpc request or response, not a field type"
            )
            return null
        }

        return if (protoType.isScalar) {
            scalar(protoType, location)
        } else {
            declaredElement(protoType, location)
        }
    }

    /**
     * Resolves a Protobuf message or enum to a generated or mapped Kotlin type.
     *
     * Mapped enums do not expose their schema through [ElementType.enumType],
     * since they do not have a generated enum implementation.
     *
     * Unknown types are reported through [reportUnknown].
     *
     * @param protoType the message or enum type to resolve
     * @param location the source location used when reporting errors
     * @return the resolved element, or `null` if no Kotlin representation exists
     */
    private fun declaredElement(protoType: ProtoType, location: Location): ElementType? {
        val enumType = schema.getType(protoType) as? EnumType
        val mapped = mappings[protoType]
        val kotlinType = mapped?.kotlinClass ?: generated[protoType]

        if (kotlinType == null) {
            reportUnknown(protoType, location)
            return null
        }

        return ElementType(
            protoType = protoType,
            kotlinType = kotlinType,
            serializer = mapped?.serializer,
            encoding = if (enumType != null) IntEncoding.DEFAULT else null,
            packable = enumType != null,
            // Mapped enums have no generated enum class.
            enumType = if (mapped == null) enumType else null,
            isMessage = enumType == null,
        )
    }

    /**
     * Reports a Protobuf type that is neither generated nor explicitly mapped.
     *
     * Includes the type's originating Protobuf file when available and
     * suggests configuring a mapping or moving the declaration into
     * the source directories.
     *
     * @param protoType the unresolved Protobuf type
     * @param location the source location referencing the type
     */
    fun reportUnknown(protoType: ProtoType, location: Location) {
        val file = schema.protoFile(protoType)?.location?.path ?: "an unknown file"

        problems.error(
            location,
            "$protoType from $file is neither generated nor mapped; add mapType(\"$protoType\", ...) or move the .proto into protoSourceDirs",
        )
    }

    /**
     * Collects generated Kotlin classes and validates declaration name collisions.
     *
     * Processes source files in their original order, including nested types
     * and services. Duplicate declarations are reported during registration.
     *
     * @param sourceFiles the Protobuf files being generated
     * @return the generated Kotlin classes indexed by fully qualified Protobuf name
     */
    private fun generatedClasses(sourceFiles: List<ProtoFile>): Map<ProtoType, ClassName> {
        val registry = GeneratedClassRegistry()

        for (file in sourceFiles) {
            registry.registerFile(file)
        }

        return registry.generated
    }

    /**
     * Tracks generated declarations and their naming conflicts for one resolution run.
     *
     * Maintains separate registries for fully qualified Protobuf names, Kotlin
     * class names, and generated file paths. These registries allow collisions
     * to be reported without interrupting declaration processing.
     */
    private inner class GeneratedClassRegistry {
        val generated = HashMap<ProtoType, ClassName>()

        private val fullNames = HashMap<ProtoType, Location>()
        private val classes = HashMap<String, Location>()
        private val files = HashMap<String, Location>()

        /**
         * Registers all declarations and the generated file name of a source file.
         *
         * File name collisions are checked before processing its types and
         * services. Nested declarations are registered recursively.
         *
         * @param file the Protobuf source file to register
         */
        fun registerFile(file: ProtoFile) {
            registerFileName(file)

            for (type in file.types) {
                registerType(type, ClassName(file.kotlinPackage, type.type.simpleName))
            }

            for (service in file.services) {
                registerService(service, file.kotlinPackage)
            }
        }

        /**
         * Checks whether a generated Kotlin file path has already been registered.
         *
         * File paths are compared case-insensitively to detect collisions on
         * file systems that do not distinguish letter case.
         *
         * @param file the Protobuf source file to register
         */
        private fun registerFileName(file: ProtoFile) {
            // File names may differ only in case on case-insensitive file systems.
            val fileKey = "${file.kotlinPackage}/${fileName(file)}".lowercase()

            files.put(fileKey, file.location)?.let { previous ->
                problems.error(
                    file.location,
                    "generates ${fileName(file)}.kt in package '${file.kotlinPackage}', which $previous already generates",
                )
            }
        }

        /**
         * Registers a message or enum and its nested declarations.
         *
         * Every declaration is checked for duplicate Protobuf names,
         * including declarations with explicit type mappings.
         *
         * Mapped types are not generated, and their nested declarations
         * are not traversed.
         *
         * @param type the Protobuf message or enum to register
         * @param className the Kotlin class name assigned to the declaration
         */
        private fun registerType(type: Type, className: ClassName) {
            declare(
                if (type is EnumType) "enum" else "message",
                type.type,
                type.location,
            )

            if (isMapped(type.type)) return

            registerGeneratedClass(type.type, className, type.location)

            for (nested in type.nestedTypes) {
                registerType(nested, className.nestedClass(nested.type.simpleName))
            }
        }

        /**
         * Registers a Protobuf service and its generated Kotlin interface.
         *
         * @param service the service declaration to register
         * @param kotlinPackage the Kotlin package of its source file
         */
        private fun registerService(service: Service, kotlinPackage: String) {
            declare("service", service.type, service.location)

            registerGeneratedClass(
                service.type,
                ClassName(kotlinPackage, service.name),
                service.location,
            )
        }

        /**
         * Records a fully qualified Protobuf declaration name.
         *
         * Reports a collision if the name was previously declared, including
         * the source location of the previous declaration.
         *
         * @param kind the declaration kind used in error messages
         * @param fullName the fully qualified Protobuf name
         * @param location the declaration's source location
         */
        private fun declare(kind: String, fullName: ProtoType, location: Location) {
            fullNames.put(fullName, location)?.let { previous ->
                problems.error(
                    location,
                    "$kind $fullName: the full name is already declared at $previous",
                )
            }
        }

        /**
         * Registers a generated Kotlin class and checks for name collisions.
         *
         * Associates the Protobuf type with its Kotlin class and reports
         * when another declaration already uses the same Kotlin class name.
         *
         * @param protoType the Protobuf declaration being generated
         * @param className the assigned Kotlin class name
         * @param location the declaration's source location
         */
        private fun registerGeneratedClass(
            protoType: ProtoType,
            className: ClassName,
            location: Location,
        ) {
            generated[protoType] = className

            classes.put(className.canonicalName, location)?.let { previous ->
                problems.error(
                    location,
                    "Kotlin class ${className.canonicalName} is already generated for $previous",
                )
            }
        }
    }

    /**
     * Resolves a built-in Protobuf scalar type to its Kotlin representation.
     *
     * Determines the Kotlin primitive type, integer encoding, and whether
     * the scalar supports packed repeated fields.
     *
     * `sint32` is explicitly unsupported because kotlinx-serialization-protobuf
     * cannot correctly decode values greater than or equal to 2^30.
     *
     * @param protoType the Protobuf scalar type to resolve
     * @param location the source location used when reporting errors
     * @return the resolved scalar element, or `null` if unsupported
     */
    private fun scalar(protoType: ProtoType, location: Location): ElementType? {
        val (kotlinType, encoding, packable) = when (protoType) {
            ProtoType.DOUBLE -> Triple(DOUBLE, null, true)
            ProtoType.FLOAT -> Triple(FLOAT, null, true)
            ProtoType.INT32, ProtoType.UINT32 -> Triple(INT, IntEncoding.DEFAULT, true)
            ProtoType.INT64, ProtoType.UINT64 -> Triple(LONG, IntEncoding.DEFAULT, true)
            ProtoType.SINT64 -> Triple(LONG, IntEncoding.SIGNED, false)
            ProtoType.FIXED32, ProtoType.SFIXED32 -> Triple(INT, IntEncoding.FIXED, false)
            ProtoType.FIXED64, ProtoType.SFIXED64 -> Triple(LONG, IntEncoding.FIXED, false)
            ProtoType.BOOL -> Triple(BOOLEAN, IntEncoding.DEFAULT, true)
            ProtoType.STRING -> Triple(STRING, null, false)
            ProtoType.BYTES -> Triple(BYTE_ARRAY, null, false)

            ProtoType.SINT32 -> {
                problems.error(
                    location,
                    "sint32 is not supported (kotlinx-serialization-protobuf cannot decode values >= 2^30); use sint64 or int32",
                )
                return null
            }

            else -> {
                problems.error(location, "unsupported scalar type $protoType")
                return null
            }
        }

        return ElementType(protoType, kotlinType, null, encoding, packable, null, false)
    }
}
