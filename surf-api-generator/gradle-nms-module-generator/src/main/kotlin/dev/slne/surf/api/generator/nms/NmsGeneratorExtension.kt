package dev.slne.surf.api.generator.nms

/**
 * Gradle extension for the `nms-module-generator` plugin.
 *
 * ```kotlin
 * nmsGenerator {
 *     referenceVersion = NmsVersion.V26_1
 *     targetVersion    = NmsVersion.V1_21_11
 *
 *     transformations {
 *         renameClass("net.minecraft.resources.Identifier",
 *                     "net.minecraft.resources.ResourceLocation")
 *     }
 * }
 * ```
 *
 * @see GenerateNmsModulePlugin
 */
@NmsGeneratorDsl
open class NmsGeneratorExtension {

    /** The NMS version whose sources serve as the generation template. */
    lateinit var referenceVersion: NmsVersion

    /** The NMS version to generate. */
    lateinit var targetVersion: NmsVersion

    internal var transformationScope: TransformationScope = TransformationScope()
        private set

    /** `true` if both [referenceVersion] and [targetVersion] have been set. */
    internal val isConfigured: Boolean
        get() = ::referenceVersion.isInitialized && ::targetVersion.isInitialized

    /** Configures the source-code transformations applied during generation. */
    fun transformations(block: TransformationScope.() -> Unit) {
        transformationScope = TransformationScope().apply(block)
    }
}
