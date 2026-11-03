
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
     * Directories containing proto files to generate and package.
     *
     * Defaults to `src/main/proto`. Generated proto files are also packaged
     * under `META-INF/surf-nats/proto` in the resulting JAR.
     *
     * Use `from(...)` to add directories or `setFrom(...)` to replace them.
     */
    abstract val protoSourceDirs: ConfigurableFileCollection

    /**
     * Additional directories containing proto files used for imports.
     *
     * These files are not generated. Dependencies containing packaged proto
     * contracts should instead use the `surfNatsProtoPath` configuration.
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
     * Dependency notation of the previously published artifact used as
     * the compatibility baseline.
     *
     * The `checkSurfNatsCompatibility` task compares the current contracts
     * against the artifact's `META-INF/surf-nats/proto` files.
     * Without a configured baseline, the task is skipped.
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
     * Sets the dependency notation for the compatibility baseline.
     */
    fun compatibilityBaseline(notation: String) {
        compatibilityBaseline.set(notation)
    }
}
