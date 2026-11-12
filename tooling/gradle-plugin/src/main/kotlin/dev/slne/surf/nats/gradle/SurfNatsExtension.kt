
package dev.slne.surf.nats.gradle

import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configures surf-nats code generation and the compatibility check in the `surfNats { }` block.
 *
 * Every setting is optional:
 *
 * ```kotlin
 * surfNats {
 *     protoSourceDirs.from("src/contracts/proto")
 *     protoPath.from("../shared/proto")
 *     mapType("surf.demo.v1.Tag", "dev.slne.demo.Tag", "dev.slne.demo.TagSerializer")
 *     generatedVisibility = INTERNAL
 *     compatibilityBaseline("1.2.0")
 * }
 * ```
 */
abstract class SurfNatsExtension {

    /**
     * The directories whose `.proto` files are generated and published to weaver.
     *
     * Defaults to `src/main/proto`. `from(...)` adds directories to the default and `setFrom(...)` replaces it.
     * Without `.proto` files, generation is skipped and the dependencies are still added.
     */
    abstract val protoSourceDirs: ConfigurableFileCollection

    /**
     * Additional local directories whose `.proto` files the sources may import.
     *
     * Files found here are not generated. Protos of other modules and registry artifacts come from the `weaver`
     * configuration instead. Copies of the protos bundled with the plugin are ignored.
     */
    abstract val protoPath: ConfigurableFileCollection

    /**
     * The custom mappings from proto types to existing Kotlin classes, usually added with [mapType].
     *
     * Mappings here take precedence over the plugin's default mappings for the same proto type.
     */
    abstract val typeMappings: ListProperty<TypeMapping>

    /**
     * The visibility of generated classes, interfaces and enums.
     *
     * Defaults to [PUBLIC]. Members of generated declarations are emitted `public`, which an `internal` declaration
     * limits to the module.
     */
    abstract val generatedVisibility: Property<GeneratedVisibility>

    /**
     * The weaver release of this module's artifact to check compatibility against, such as `1.2.0`.
     *
     * When unset, the baseline is the highest release below `project.version`, without its `-SNAPSHOT` suffix.
     * Setting it also enables the check for modules without a publishing repository. A version that does not exist
     * in the registry fails the `fetchSurfNatsBaseline` task.
     */
    abstract val compatibilityBaseline: Property<String>

    /**
     * Whether `checkSurfNatsCompatibility` runs. Defaults to `true`.
     */
    abstract val compatibilityCheck: Property<Boolean>

    /**
     * Shorthand for [GeneratedVisibility.PUBLIC] in `generatedVisibility = PUBLIC`.
     */
    val PUBLIC: GeneratedVisibility
        get() = GeneratedVisibility.PUBLIC

    /**
     * Shorthand for [GeneratedVisibility.INTERNAL] in `generatedVisibility = INTERNAL`.
     */
    val INTERNAL: GeneratedVisibility
        get() = GeneratedVisibility.INTERNAL

    /**
     * Maps a proto message or enum to an existing Kotlin class instead of generating one.
     *
     * Generated messages use [kotlinClass] for fields of the proto type and encode it with [serializerClass]. A
     * mapped type cannot be an rpc request or response, and generation fails if one is. Mapping
     * `google.protobuf.Empty` is rejected, because it always maps to `kotlin.Unit`.
     *
     * ```kotlin
     * surfNats {
     *     mapType("surf.demo.v1.Tag", "dev.slne.demo.Tag", "dev.slne.demo.TagSerializer")
     * }
     * ```
     *
     * @param proto The fully qualified proto type name, such as `surf.demo.v1.Tag`.
     * @param kotlinClass The fully qualified name of the Kotlin class.
     * @param serializerClass The fully qualified name of the `KSerializer` class or object for [kotlinClass].
     */
    fun mapType(proto: String, kotlinClass: String, serializerClass: String) {
        typeMappings.add(
            TypeMapping(ProtoType.get(proto), kotlinClass, serializerClass)
        )
    }

    /**
     * Sets [compatibilityBaseline] to the weaver release [version], such as `1.2.0`.
     */
    fun compatibilityBaseline(version: String) {
        compatibilityBaseline.set(version)
    }
}
