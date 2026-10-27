package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.rpc.AllReply
import dev.slne.surf.nats.runtime.rpc.FailureRecorder
import dev.slne.surf.nats.runtime.rpc.classifyAllReply
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.cancellation.CancellationException

/**
 * Collects the replies of an `ALL_INSTANCES` call with a result.
 *
 * The first decodable known answer wins. `Surf-Error` replies and undecodable answers are recorded; when the
 * replies end (timeout or no responders) without a known answer, the first recorded error is thrown with the later
 * ones suppressed, otherwise the answer is unknown.
 */
internal class AllAnswers(
    private val decoder: ReplyDecoder,
    private val subject: String,
) {
    private val failures = FailureRecorder()

    suspend fun collect(replies: ReceiveChannel<Msg>): Answer {
        for (reply in replies) {
            when (classify(reply)) {
                AllReply.NO_RESPONDERS -> return noAnswer()
                AllReply.OTHER_STATUS, AllReply.UNKNOWN -> continue
                AllReply.REMOTE_ERROR -> recordRemoteError(reply)
                AllReply.ANSWER -> decodeOrRecord(reply)?.let { return it }
            }
        }

        return noAnswer()
    }

    fun noAnswer(): Answer {
        failures.throwIfAny()
        return Answer(known = false)
    }

    private fun classify(reply: Msg): AllReply = classifyAllReply(
        NatsHeaders.statusCode(reply),
        { NatsHeaders.get(reply, ProtocolHeaders.ERROR) },
        { NatsHeaders.get(reply, ProtocolHeaders.ANSWER) },
    )

    private fun recordRemoteError(reply: Msg) {
        val error = decoder.remoteErrorOrNull(reply)!!
        failures.record(error)

        val origin = NatsHeaders.get(reply, ProtocolHeaders.ORIGIN)
        console.warn("surf-nats: ALL_INSTANCES responder $origin failed on $subject: ${error.type}")
    }

    private suspend fun decodeOrRecord(reply: Msg): Answer? {
        return try {
            Answer(known = true, value = decoder.decodeBody(reply))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failures.record(e)
            console.warn("surf-nats: undecodable ALL_INSTANCES answer on $subject", e)
            null
        }
    }
}
