package dev.slne.surf.api.paper.event.captcha

import dev.slne.surf.api.core.event.SurfSyncEvent
import org.bukkit.entity.Player

/**
 * Fired when a player successfully completes a CAPTCHA verification.
 *
 * This synchronous event is triggered when a player successfully completes and verifies a CAPTCHA challenge.
 *
 * @property player The player who passed the CAPTCHA
 * @property size The size/difficulty rating of the CAPTCHA
 * @property timePerPage The time allowed per CAPTCHA page in milliseconds
 */
data class CaptchaSuccessEvent(
    val player: Player,
    val size: Int,
    val timePerPage: Long
) : SurfSyncEvent()
