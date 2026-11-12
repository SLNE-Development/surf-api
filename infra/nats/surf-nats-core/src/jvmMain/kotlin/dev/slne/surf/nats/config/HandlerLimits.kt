package dev.slne.surf.nats.config

import dev.slne.surf.nats.InternalNatsApi

/**
 * How many handlers of one service registration may run at the same time: [total] in all, of which at most [long]
 * belong to long-running calls.
 */
@InternalNatsApi
internal data class HandlerLimits(val total: Int, val long: Int = defaultLong(total)) {

    init {
        require(total > 0) { "Handler limit must be positive, was $total" }
        require(long in 1..total) { "Long handler limit must be in 1..$total, was $long" }
    }

    /**
     * Returns these limits with the configured [total] and [long] in place of the own values.
     *
     * A configured [total] without a configured [long] gets the default long share of that total. A [long] above
     * the resolved total is lowered to it.
     */
    fun overriddenBy(total: Int?, long: Int?): HandlerLimits {
        val resolvedTotal = total ?: this.total
        val resolvedLong = long ?: if (total == null) this.long else defaultLong(total)
        return HandlerLimits(resolvedTotal, resolvedLong.coerceAtMost(resolvedTotal))
    }

    companion object {

        /**
         * Returns the default long share of [total]: half of it, at least one.
         */
        fun defaultLong(total: Int): Int = (total / 2).coerceAtLeast(1)
    }
}
