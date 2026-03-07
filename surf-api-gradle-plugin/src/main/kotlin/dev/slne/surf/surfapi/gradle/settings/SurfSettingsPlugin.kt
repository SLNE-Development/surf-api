package dev.slne.surf.surfapi.gradle.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

internal class SurfSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    }
}