package dev.slne.surf.surfapi.velocity.api.util

import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastBuilder
import dev.slne.surf.surfapi.velocity.api.surfVelocityApi

fun Player.sendToast(toast: Toast) = toast.send(this.uniqueId)
fun Player.sendToast(block: ToastBuilder.() -> Unit) =
    surfVelocityApi.createToast(block).send(this.uniqueId)

fun Toast.send(player: Player) = this.send(player.uniqueId)