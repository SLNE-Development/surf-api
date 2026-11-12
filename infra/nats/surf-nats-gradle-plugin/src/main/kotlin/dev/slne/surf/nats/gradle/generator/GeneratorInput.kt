
package dev.slne.surf.nats.gradle.generator

import com.squareup.wire.schema.ProtoType
import java.io.File
import java.io.Serial
import java.io.Serializable

/**
 * Maps a Protobuf message or enum type to an existing Kotlin class and its serializer.
 *
 * The generator uses [kotlinClass] wherever [protoType] appears and does not generate a class for it. Build scripts
 * create mappings with `surfNats { mapType(...) }`.
 *
 * Instances are Java-serializable, so they can serve as Gradle task inputs.
 *
 * @property protoType The fully qualified Protobuf type to map.
 * @property kotlinClass The fully qualified name of the existing Kotlin class.
 * @property serializerClass The fully qualified name of its `KSerializer` class or object.
 */
data class TypeMapping(
    val protoType: ProtoType,
    val kotlinClass: String,
    val serializerClass: String
) : Serializable {

    /**
     * Serializes [protoType] by name, because [ProtoType] is not [Serializable] and Gradle fingerprints task inputs
     * with Java serialization.
     */
    @Serial
    private fun writeReplace(): Any = SerializedTypeMapping(protoType.toString(), kotlinClass, serializerClass)

    private class SerializedTypeMapping(
        private val protoType: String,
        private val kotlinClass: String,
        private val serializerClass: String,
    ) : Serializable {

        @Serial
        private fun readResolve(): Any = TypeMapping(ProtoType.get(protoType), kotlinClass, serializerClass)

        companion object {
            @Serial
            private const val serialVersionUID: Long = 1L
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 3952024840026078072L
    }
}

/**
 * Selects the visibility modifier of generated classes, interfaces and enums.
 *
 * Members of generated declarations are emitted `public`, which an `internal` declaration limits to the module.
 */
enum class GeneratedVisibility {
    /**
     * Generates `public` declarations.
     */
    PUBLIC,

    /**
     * Generates `internal` declarations.
     */
    INTERNAL
}

/**
 * Holds the sources and settings of one code generation run.
 *
 * [typeMappings] take precedence over [defaultTypeMappings] for the same Protobuf type.
 *
 * @property sourceDirs The directories whose `.proto` files are generated.
 * @property protoPathDirs The additional directories that resolve imports. Their files are not generated.
 * @property typeMappings The mappings configured with `mapType`.
 * @property visibility The visibility of generated classes, interfaces and enums.
 * @property defaultTypeMappings The mappings the plugin provides for the target platform.
 */
data class GeneratorInput(
    val sourceDirs: List<File>,
    val protoPathDirs: List<File>,
    val typeMappings: List<TypeMapping>,
    val visibility: GeneratedVisibility,
    val defaultTypeMappings: List<TypeMapping>,
)

/**
 * Holds the files written by a successful code generation run.
 *
 * @property files The generated Kotlin files, sorted by path. Empty when there were no `.proto` sources.
 */
class GenerationResult(val files: List<File>)

/**
 * Signals that code generation or the compatibility check found problems in the Protobuf sources.
 *
 * The message starts with `surf-nats code generation failed:` and lists one problem per line. A problem has the
 * form `location: message`. The location is usually `file:line:column` in a `.proto` file, and
 * `mapType(<proto type>)` for an invalid type mapping.
 *
 * @property errors The individual problems in report order.
 */
class SurfNatsGeneratorException(val errors: List<String>) : RuntimeException(
    "surf-nats code generation failed:\n" + errors.joinToString("\n")
) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -7044215391234861039L
    }
}
