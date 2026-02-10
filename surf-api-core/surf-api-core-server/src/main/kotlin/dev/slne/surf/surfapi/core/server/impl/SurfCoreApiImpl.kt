package dev.slne.surf.surfapi.core.server.impl

import dev.slne.surf.surfapi.core.api.SurfCoreApi

/**
 * The SurfCoreApiImpl class is an implementation of the SurfCoreApi interface. It provides the
 * functionality to access the SurfCoreApi instance.
 */
abstract class SurfCoreApiImpl protected constructor() : SurfCoreApi {

    override fun toString(): String {
        return "SurfCoreApiImpl()"
    }
}
