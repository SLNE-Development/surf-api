package dev.slne.surf.api.paper.nms.common

import java.net.URLClassLoader

interface LibLoaderBridge {
    fun getActiveCompatibilities(): MutableSet<String>

    fun convertWithCommodore(
        b: ByteArray,
        pluginName: String,
        pluginVersion: String,
        activeCompatibilities: MutableSet<String>
    ): ByteArray

    fun overwriteLibraryLoader(pluginClassLoader: URLClassLoader, newLoader: URLClassLoader)

    fun getLibraryLoader(pluginClassLoader: URLClassLoader): URLClassLoader
}