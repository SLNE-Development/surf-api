package dev.slne.surf.surfapi.hytale.api

import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.requiredService

interface SurfHytaleApi : SurfCoreApi {
    companion object {
        val instance = requiredService<SurfHytaleApi>()
    }
}

val surfHytaleApi get() = SurfHytaleApi.instance