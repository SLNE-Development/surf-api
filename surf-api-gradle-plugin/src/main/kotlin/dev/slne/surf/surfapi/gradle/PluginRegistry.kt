package dev.slne.surf.surfapi.gradle

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import org.gradle.api.Project

internal object PluginRegistry {
    private val commonPlugins = listOf(
        "org.gradle.java-gradle-plugin",
        "org.gradle.java-library",
        "org.gradle.maven-publish",
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.kapt",
        "org.jetbrains.kotlin.plugin.spring",
        "org.jetbrains.kotlin.plugin.jpa",
        "org.hibernate.build.maven-repo-auth",
        "com.gradleup.shadow"
    )

    private val platformPlugins = mapOf(
        SurfApiPlatform.PAPER to listOf(
            "xyz.jpenilla.run-paper",
            "net.minecrell.plugin-yml.paper"
        )
    )

    fun Project.applyCommonPlugins() {
        commonPlugins.forEach { pluginId ->
            try {
                pluginManager.apply(pluginId)
                logger.lifecycle("Applied plugin 0: $pluginId")
            } catch (e: Exception) {
                logger.error("Failed to apply plugin: $pluginId")
                e.printStackTrace()
            }
        }
    }

    fun Project.applyPlatformDependentPlugins(platform: SurfApiPlatform) {
        platformPlugins[platform]?.forEach { pluginId ->
            try {
                pluginManager.apply(pluginId)
                logger.lifecycle("Applied platform dependent plugin: $pluginId")
            } catch (e: Exception) {
                logger.error("Failed to apply platform dependent plugin: $pluginId")
                e.printStackTrace()
            }
        }
    }
}