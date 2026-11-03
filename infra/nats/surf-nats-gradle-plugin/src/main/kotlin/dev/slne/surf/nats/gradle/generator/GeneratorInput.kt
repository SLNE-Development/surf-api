
package dev.slne.surf.nats.gradle.generator

import com.squareup.wire.schema.ProtoType
import java.io.File
import java.io.Serial
import java.io.Serializable

/**
 * Maps a Protobuf message or enum type to an existing Kotlin class and its serializer.
 *
 * Type mappings allow the generator to reuse existing Kotlin implementations
 * instead of generating new types for the corresponding Protobuf declarations.
 *
 * @property protoType the fully qualified Protobuf type to map
 * @property kotlinClass the fully qualified name of the existing Kotlin class
 * @property serializerClass the fully qualified name of its KSerializer implementation
 */
data class TypeMapping(
    val protoType: ProtoType,
    val kotlinClass: String,
    val serializerClass: String
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 3952024840026078072L
    }
}

/**
 * Controls the visibility of generated Kotlin declarations.
 */
enum class GeneratedVisibility {
    /** Generates public declarations. */
    PUBLIC,

    /** Generates internal declarations. */
    INTERNAL
}

/**
 * Contains the configuration and source locations for a single code generation run.
 *
 * Explicit [typeMappings] configured through `mapType` take precedence over
 * [defaultTypeMappings] provided by the plugin for the target platform.
 *
 * @property sourceDirs directories containing the Protobuf source files to generate
 * @property protoPathDirs additional directories used to resolve imported Protobuf definitions
 * @property typeMappings explicitly configured mappings from Protobuf types to existing Kotlin classes
 * @property visibility the visibility applied to generated Kotlin declarations
 * @property defaultTypeMappings platform-specific mappings used unless overridden by explicit mappings
 */
data class GeneratorInput(
    val sourceDirs: List<File>,
    val protoPathDirs: List<File>,
    val typeMappings: List<TypeMapping>,
    val visibility: GeneratedVisibility,
    val defaultTypeMappings: List<TypeMapping>,
)

/**
 * Contains the files produced by a successful code generation run.
 *
 * @property files the generated output files
 */
class GenerationResult(val files: List<File>)

/**
 * Indicates that code generation failed because one or more problems were detected.
 *
 * All collected problems are included in the exception message, prefixed by
 * `surf-nats code generation failed:`. Each problem is formatted as
 * `file:line:col: message`.
 *
 * @property errors the individual error messages collected during generation
 */
class SurfNatsGeneratorException(val errors: List<String>) : RuntimeException(
    "surf-nats code generation failed:\n" + errors.joinToString("\n")
) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -7044215391234861039L
    }
}
