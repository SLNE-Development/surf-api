package dev.slne.surf.surfapi.gradle.platform

import dev.slne.surf.surfapi.gradle.Versions
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

enum class SurfApiPlatform(val dependency: String, val scope: String = COMPILE_ONLY) {
    CORE("dev.slne.surf:surf-api-core-api:${Versions.API_VERSION}"),
    PAPER("dev.slne.surf:surf-api-bukkit-api:${Versions.API_VERSION}"),
    VELOCITY("dev.slne.surf:surf-api-velocity-api:${Versions.API_VERSION}"),
    STANDALONE("dev.slne.surf:surf-api-standalone:${Versions.API_VERSION}", API),
}