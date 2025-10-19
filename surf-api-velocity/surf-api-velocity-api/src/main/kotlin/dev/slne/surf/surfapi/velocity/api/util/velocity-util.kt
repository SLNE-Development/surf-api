package dev.slne.surf.surfapi.velocity.api.util

import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.velocity.api.surfVelocityApi
import dev.slne.surf.surfapi.velocity.api.toast.ToastVelocityBuilder

fun Player.sendToast(toast: Toast) = toast.send(this.uniqueId)
fun Player.sendToast(block: ToastVelocityBuilder.() -> Unit) =
    surfVelocityApi.createToast(block).send(this.uniqueId)

fun Toast.send(player: Player) = this.send(player.uniqueId)