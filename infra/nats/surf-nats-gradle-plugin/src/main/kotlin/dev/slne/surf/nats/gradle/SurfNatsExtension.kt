package dev.slne.surf.nats.gradle

import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.TypeMapping
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * The `surfNats { }` DSL.
 */
abstract class SurfNatsExtension {

    /**
     * Directories whose `.proto` files are generated and packed into `META-INF/surf-nats/proto`; defaults to
     * `src/main/proto`. `from(...)` adds to the default, `setFrom(...)` replaces it.
     */
    abstract val protoSourceDirs: ConfigurableFileCollection

    /**
     * Directories with `.proto` files that sources may import but that are not generated. Contract jars go into the
     * `surfNatsProtoPath` configuration instead.
     */
    abstract val protoPath: ConfigurableFileCollection

    abstract val typeMappings: ListProperty<TypeMapping>

    abstract val generatedVisibility: Property<GeneratedVisibility>

    /**
     * Dependency notation of the previously published artifact whose `META-INF/surf-nats/proto` is the baseline of
     * `checkSurfNatsCompatibility`. Without a baseline the check is skipped.
     */
    abstract val compatibilityBaseline: Property<String>

    abstract val compatibilityCheck: Property<Boolean>

    val PUBLIC: GeneratedVisibility get() = GeneratedVisibility.PUBLIC

    val INTERNAL: GeneratedVisibility get() = GeneratedVisibility.INTERNAL

    /**
     * Maps the fully qualified proto message or enum [proto] to the existing [kotlinClass], serialized by the
     * `KSerializer` object or class [serializerClass]. Both class names are fully qualified.
     */
    fun mapType(proto: String, kotlinClass: String, serializerClass: String) {
        typeMappings.add(TypeMapping(proto, kotlinClass, serializerClass))
    }

    fun compatibilityBaseline(notation: String) {
        compatibilityBaseline.set(notation)
    }
}
