package dev.slne.surf.api.gradle.platform.minestom

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.generated.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

/**
 * Applies the Minestom runtime relocations to an existing Shadow JAR without configuring a
 * complete Surf/Kotlin project. This is intended for final application assemblers that merge
 * multiple Surf Minestom plugins into one executable JAR.
 */
internal class MinestomRelocationsPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.gradleup.shadow") {
            tasks.withType<ShadowJar>().configureEach {
                val prefix = Constants.RELOCATION_PREFIX

                relocate("org.spongepowered.configurate", "$prefix.configurate")
                relocate("com.mojang.serialization", "$prefix.mojang.serialization")
                relocate("com.mojang.datafixers", "$prefix.mojang.datafixers")
                relocate("net.kyori.adventure.nbt", "$prefix.kyori.nbt") {
                    exclude("net.kyori.adventure.nbt.api.**")
                }
                relocate("it.unimi.dsi.fastutil", "$prefix.fastutil")
                relocate("me.devnatan.inventoryframework", "$prefix.devnatan.inventoryframework")
            }
        }
    }
}
