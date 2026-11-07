package dev.slne.surf.nats.rpc

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Retention limits of persistent messages, for sizing deduplication records keyed by
 * [NatsPersistentContext.messageId].
 */
public object NatsPersistentLimits {
    /**
     * How long an unacknowledged persistent message stays in its work queue and can be redelivered.
     */
    public val MESSAGE_MAX_AGE: Duration = 7.days

    /**
     * How long a dead-lettered message stays in the dead-letter stream and can be replayed.
     */
    public val DEAD_LETTER_MAX_AGE: Duration = 7.days

    /**
     * The latest time after its first publish at which a message with the same identity can reach a handler: the
     * work-queue age plus the dead-letter age, covering redeliveries and one dead-letter replay.
     *
     * A replayed message starts a new work-queue age, so each further replay extends this horizon. An offloaded
     * fire payload expires [MESSAGE_MAX_AGE] after its upload, so a replay after that cannot resolve its body.
     */
    public val REDELIVERY_HORIZON: Duration = MESSAGE_MAX_AGE + DEAD_LETTER_MAX_AGE
}
