package dev.slne.surf.surfapi.gradle.platform

import dev.slne.surf.surfapi.gradle.generated.Constants
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

enum class SurfApiPlatform(val dependency: String, val scope: String = COMPILE_ONLY) {
    CORE("dev.slne.surf:surf-api-core-api:${Constants.SURF_API_VERSION}"),
    PAPER("dev.slne.surf:surf-api-bukkit-api:${Constants.SURF_API_VERSION}"),
    VELOCITY("dev.slne.surf:surf-api-velocity-api:${Constants.SURF_API_VERSION}"),
    STANDALONE("dev.slne.surf:surf-api-standalone:${Constants.SURF_API_VERSION}", API),
}