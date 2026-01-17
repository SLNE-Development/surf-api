package dev.slne.surf.surfapi.hytale.api

import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.util.requiredService

interface SurfHytaleApi : SurfCoreApi {
    companion object : SurfHytaleApi by surfHytaleApi {
        val instance = requiredService<SurfHytaleApi>()
        fun get() = instance
    }
}

val surfHytaleApi get() = SurfHytaleApi.instance