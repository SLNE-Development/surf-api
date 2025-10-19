package dev.slne.surf.surfapi.velocity.api

import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.SurfCoreApi
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastBuilder
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.text.Component
import java.util.concurrent.ExecutorService

interface SurfVelocityApi : SurfCoreApi {
    val executorService: ExecutorService?

    /**
     * Creates a Toast using the provided builder function.
     *
     * @param builder A lambda function that configures the ToastBuilder.
     * @return The created Toast instance.
     *
     * @see ToastBuilder
     */
    fun createToast(builder: ToastBuilder.() -> Unit): Toast

    /**
     * Creates a Toast with the specified icon, display text, and style.
     *
     * @param icon The icon ItemType for the toast.
     * @param text The display text of the toast.
     * @param style The style of the toast.
     *
     * @return The created Toast instance.
     * @see Toast
     */
    fun createToast(
        icon: ItemType,
        text: Component,
        style: ToastStyle
    ): Toast

    /**
     * Sends the specified toast to the given player.
     *
     * @param player The Player to whom the toast will be sent.
     * @param toast The Toast instance to be sent.
     *
     * @see Toast
     */
    fun sendToast(player: Player, toast: Toast)

    companion object {
        val instance = requiredService<SurfVelocityApi>()
    }
}

val surfVelocityApi get() = SurfVelocityApi.instance