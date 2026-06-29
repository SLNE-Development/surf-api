package dev.slne.surf.api.core.server.impl

import dev.slne.surf.api.core.SurfApiCore
import net.kyori.adventure.audience.Audience
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * The SurfCoreApiImpl class is an implementation of the SurfCoreApi interface. It provides the
 * functionality to access the SurfCoreApi instance.
 */
abstract class SurfApiCoreImpl protected constructor() : SurfApiCore {

    abstract fun isPlayer(audience: Audience): Boolean

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    companion object {
        fun get() = SurfApiCore.instance as SurfApiCoreImpl
    }
}
