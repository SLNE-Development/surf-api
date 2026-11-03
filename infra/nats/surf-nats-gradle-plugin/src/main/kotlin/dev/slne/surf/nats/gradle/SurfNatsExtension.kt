
package dev.slne.surf.nats.gradle

import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configures the `surfNats { }` Gradle DSL.
 *
 * Defines proto source directories, import paths, type mappings,
 * generated code visibility, and compatibility checking.
 */
abstract class SurfNatsExtension {

    /**
     * Directories containing proto files to generate and publish to weaver.
     *
     * Defaults to `src/main/proto`.
     *
     * Use `from(...)` to add directories or `setFrom(...)` to replace them.
     */
    abstract val protoSourceDirs: ConfigurableFileCollection

    /**
     * Additional directories containing proto files used for imports.
     *
     * These files are not generated. Protos of other modules and registry
     * artifacts come from the `weaver` configuration instead.
     */
    abstract val protoPath: ConfigurableFileCollection

    /**
     * Custom mappings from proto types to existing Kotlin classes.
     */
    abstract val typeMappings: ListProperty<TypeMapping>

    /**
     * Visibility of generated Kotlin declarations.
     */
    abstract val generatedVisibility: Property<GeneratedVisibility>

    /**
     * Weaver version of this module's artifact used as the compatibility baseline, such as `1.2.0`.
     *
     * Defaults to the highest release below `project.version`. Setting it also enables the check for
     * modules without a publishing repository.
     */
    abstract val compatibilityBaseline: Property<String>

    /**
     * Whether compatibility checking is enabled.
     */
    abstract val compatibilityCheck: Property<Boolean>

    val PUBLIC: GeneratedVisibility
        get() = GeneratedVisibility.PUBLIC

    val INTERNAL: GeneratedVisibility
        get() = GeneratedVisibility.INTERNAL

    /**
     * Maps a proto message or enum to an existing Kotlin class.
     *
     * @param proto Fully qualified proto type name.
     * @param kotlinClass Fully qualified Kotlin class name.
     * @param serializerClass Fully qualified name of the `KSerializer` class or object.
     */
    fun mapType(proto: String, kotlinClass: String, serializerClass: String) {
        typeMappings.add(
            TypeMapping(ProtoType.get(proto), kotlinClass, serializerClass)
        )
    }

    /**
     * Sets the weaver version used as the compatibility baseline.
     */
    fun compatibilityBaseline(version: String) {
        compatibilityBaseline.set(version)
    }
}
