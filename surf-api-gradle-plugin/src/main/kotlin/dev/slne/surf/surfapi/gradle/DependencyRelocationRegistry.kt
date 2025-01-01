package dev.slne.surf.surfapi.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

internal object DependencyRelocationRegistry {

    private val coreRelocations = mapOf<String, String>(
        "com.github.retrooper.packetevents" to "packetevents.api",
        "io.github.retrooper.packetevents" to "packetevents.impl"
    )

    private val velocityRelocations = coreRelocations + mapOf<String, String>(
        "it.unimi.dsi.fastutil" to "fastutil",
    )

    private val bukkitRelocations = coreRelocations
    private val standaloneRelocations = coreRelocations

    private fun getRelocations(mode: SurfApiPlatform) = when (mode) {
        SurfApiPlatform.CORE -> coreRelocations
        SurfApiPlatform.PAPER -> bukkitRelocations
        SurfApiPlatform.VELOCITY -> velocityRelocations
        SurfApiPlatform.STANDALONE -> standaloneRelocations
    }

    fun Project.applyRelocations(
        mode: SurfApiPlatform,
    ) {
        val relocations = getRelocations(mode)
        tasks.withType<ShadowJar> {
            relocations.forEach { (from, to) ->
                relocate(from, "${Constants.RELOCATION_PREFIX}.$to")
            }
        }
    }
}