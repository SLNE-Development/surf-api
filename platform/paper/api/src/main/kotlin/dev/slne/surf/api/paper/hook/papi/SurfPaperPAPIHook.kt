@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.hook.papi

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import dev.slne.surf.api.spring.SurfApiBean

@SurfApiBean
interface SurfPaperPAPIHook {
    fun register(expansion: PapiExpansion)
    fun unregister(expansion: PapiExpansion)

    companion object {
        @InternalSurfApi
        val INSTANCE get() = papiHook
    }
}

private val papiHook by surfBean<SurfPaperPAPIHook>()
