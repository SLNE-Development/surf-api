package dev.slne.surf.microservice.gradle.plugin.settings

import dev.slne.surf.microservice.gradle.generated.Constants
import dev.slne.surf.microservice.gradle.plugin.SurfApiPlugin
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

internal class SettingsMicroservicePlugin : Plugin<Settings> {
    override fun apply(target: Settings) = with(target) {
        pluginManagement {
            plugins {
                id(SurfApiPlugin.SETTINGS.pluginName).version("${Constants.MINECRAFT_VERSION}+")
            }
        }
    }
}