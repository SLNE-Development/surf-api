
package dev.slne.surf.nats.gradle.tasks

import dev.slne.surf.nats.gradle.generator.*
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Generates Kotlin contracts from the `.proto` files in [sourceDirectories].
 *
 * The plugin registers it as `generateSurfNatsKotlin`. Each `.proto` file with at least one generated declaration
 * becomes one Kotlin file in [outputDir]. The task replaces the whole directory, and leaves it untouched when
 * generation fails. The task is skipped (`NO-SOURCE`) when [sourceDirectories] contain no `.proto` files.
 *
 * Generated declarations:
 *
 * - A message becomes a `@Serializable` data class named after the proto message and annotated with
 *   `@SerialName("<proto full name>")`. Every field property carries `@ProtoNumber`. A message without fields becomes a
 *   plain class whose instances are all equal.
 * - A oneof becomes a nested sealed interface with one data class per case, held in a nullable property.
 * - A message with the surf event option implements `NatsEvent` and exposes its descriptor as `NATS_DESCRIPTOR`.
 * - An enum becomes a Kotlin enum without the value prefix. Each entry has its proto `number`, the companion offers
 *   `forNumber(number)`, and decoding an unknown number yields the zero-valued entry.
 * - A service becomes an interface with one abstract `suspend` function per rpc and a `NATS_DESCRIPTOR` companion
 *   property. An rpc whose request is `google.protobuf.Empty` has no parameter.
 *
 * Types mapped with `surfNats { mapType(...) }` are not generated.
 */
@CacheableTask
abstract class GenerateSurfNatsKotlinTask : ProtoInputsTask() {

    /**
     * The `.proto` files of [sourceDirectories]. The task is skipped when there are none.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoSources: FileTree
        get() = protoFiles(sourceDirectories)

    /**
     * The type mappings the plugin provides for the platform, such as `google.protobuf.Timestamp` to
     * `java.time.Instant`.
     *
     * [typeMappings] take precedence over these defaults for the same proto type.
     */
    @get:Input
    abstract val defaultTypeMappings: ListProperty<TypeMapping>

    /**
     * The mappings configured with `surfNats { mapType(...) }`.
     */
    @get:Input
    abstract val typeMappings: ListProperty<TypeMapping>

    /**
     * The visibility of generated classes, interfaces and enums, from `surfNats.generatedVisibility`.
     */
    @get:Input
    abstract val visibility: Property<GeneratedVisibility>

    /**
     * The directory that receives the generated Kotlin sources, `build/generated/surf-nats/kotlin` by default.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * Generates the contracts into [outputDir].
     *
     * @throws GradleException when the sources cannot be loaded or violate the surf-nats contract rules, for
     *   example a persistent rpc without a result whose call timeout exceeds the 120-second duplicate window. The
     *   message lists every problem, prefixed with its `file:line:column` location where it has one.
     */
    @TaskAction
    fun generate() {
        val input = createGeneratorInput()

        try {
            SurfNatsGenerator().generate(input, outputDir.get().asFile)
        } catch (e: SurfNatsGeneratorException) {
            throw GradleException(e.message.orEmpty(), e)
        }
    }

    private fun createGeneratorInput(): GeneratorInput = GeneratorInput(
        sourceDirs = sourceDirectories.files.toList(),
        protoPathDirs = protoPath(),
        typeMappings = typeMappings.get(),
        visibility = visibility.get(),
        defaultTypeMappings = defaultTypeMappings.get(),
    )
}
