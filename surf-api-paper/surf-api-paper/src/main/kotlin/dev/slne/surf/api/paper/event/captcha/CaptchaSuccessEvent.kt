package dev.slne.surf.api.paper.event.captcha

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

data class CaptchaSuccessEvent(
    val player: Player,
    val size: Int,
    val timePerPage: Long
) : SurfSyncEvent()
