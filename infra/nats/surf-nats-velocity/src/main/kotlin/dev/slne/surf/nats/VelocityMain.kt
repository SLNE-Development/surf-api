package dev.slne.surf.nats

import com.google.inject.Inject
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path

lateinit var instance: VelocityMain

class VelocityMain @Inject constructor(
    val proxy: ProxyServer,
    @DataDirectory val dataPath: Path
) {
    init {
        instance = this
    }
}
