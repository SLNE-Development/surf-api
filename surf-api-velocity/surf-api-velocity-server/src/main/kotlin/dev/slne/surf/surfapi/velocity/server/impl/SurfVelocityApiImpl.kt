package dev.slne.surf.surfapi.velocity.server.impl

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.google.auto.service.AutoService
import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastBuilder
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl
import dev.slne.surf.surfapi.core.server.impl.toast.ToastImpl
import dev.slne.surf.surfapi.velocity.api.SurfVelocityApi
import dev.slne.surf.surfapi.velocity.server.velocityMain
import net.kyori.adventure.text.Component
import java.util.*


@AutoService(SurfCoreApi::class)
class SurfVelocityApiImpl : SurfCoreApiImpl(), SurfVelocityApi {
    init {
        checkInstantiationByServiceLoader()
    }

    override val executorService get() = velocityMain.executorService
    override fun createToast(builder: ToastBuilder.() -> Unit): Toast =
        ToastBuilder().apply(builder).build()

    override fun createToast(
        icon: ItemType,
        text: Component,
        style: ToastStyle
    ) = ToastImpl(
        icon,
        text,
        style
    )

    override fun sendToast(
        player: Player,
        toast: Toast
    ) = toast.send(player.uniqueId)

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {

        val proxy = velocityMain.server
        proxy.getPlayer(playerUuid).ifPresent { player ->
            proxy.getServer(server).ifPresent { server -> player.createConnectionRequest(server) }
        }
    }

    override fun getPlayer(playerUuid: UUID): Player =
        velocityMain.server.getPlayer(playerUuid).orElse(null)
}
