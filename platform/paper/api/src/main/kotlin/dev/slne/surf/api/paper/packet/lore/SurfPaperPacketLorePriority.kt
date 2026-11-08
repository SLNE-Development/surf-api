package dev.slne.surf.api.paper.packet.lore

import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLorePriority.FIRST
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLorePriority.LAST
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLorePriority.NORMAL


/**
 * Predefined priority constants for [SurfPaperPacketLoreHandler]s.
 *
 * Handlers are invoked in **ascending** order of their priority — the smaller the value,
 * the earlier the handler is called; the larger the value, the later it is called.
 *
 * This means a handler registered with [FIRST] runs before one registered with [LAST],
 * giving the latter the chance to override or post-process whatever earlier handlers wrote
 * to the lore list.
 *
 * Priorities are exposed as [Short] to keep memory overhead low while still leaving plenty
 * of room between the predefined values for fine-grained custom ordering.
 *
 * Example:
 * ```
 * SurfPaperPacketApi.registerPacketLoreListener(
 *     plugin,
 *     key,
 *     handler,
 *     priority = SurfPaperPacketLorePriority.LATE,
 * )
 * ```
 */
object SurfPaperPacketLorePriority {
    /**
     * Runs first, before every other handler. Use this when you want to *prepare* lore
     * that subsequent handlers will inspect or extend.
     */
    const val FIRST: Short = Short.MIN_VALUE

    /**
     * Runs early, but allows handlers using [FIRST] to go before this one.
     */
    const val EARLY: Short = -16384

    /**
     * The default priority used by handlers that don't override it.
     */
    const val NORMAL: Short = 0

    /**
     * Runs late, after [NORMAL] but before [LAST].
     */
    const val LATE: Short = 16384

    /**
     * Runs last, after every other handler. Use this when you want to have the final say
     * on the lore list — e.g. for stripping or post-processing.
     */
    const val LAST: Short = Short.MAX_VALUE
}

