package dev.slne.surf.api.generator.nms

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * Gradle plugin that registers the `nmsGenerator` extension and the
 * `generateNmsModule` task.
 *
 * Apply the plugin and configure it in `build.gradle.kts`:
 *
 * ```kotlin
 * plugins {
 *     id("dev.slne.surf.api.generator.nms-module-generator")
 * }
 *
 * nmsGenerator {
 *     referenceVersion = NmsVersion.V26_1
 *     targetVersion    = NmsVersion.V1_21_11
 *
 *     transformations {
 *         renameClass("net.minecraft.resources.Identifier",
 *                     "net.minecraft.resources.ResourceLocation")
 *         removeImport("net.minecraft.world.item.component.TypedEntityData")
 *         replaceCode("TypedEntityData.decodeEntity(nbt)", "CustomData.of(nbt)")
 *     }
 * }
 * ```
 *
 * Then run:
 * ```
 * ./gradlew generateNmsModule            # interactive — asks before overwriting
 * ./gradlew generateNmsModule -PforceOverwrite  # non-interactive
 * ```
 */
abstract class GenerateNmsModulePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create<NmsGeneratorExtension>("nmsGenerator")
        target.tasks.register<GenerateNmsModuleTask>("generateNmsModule")
    }
}

