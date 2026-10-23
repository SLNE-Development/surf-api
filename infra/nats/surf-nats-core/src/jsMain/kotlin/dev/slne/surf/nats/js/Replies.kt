package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.NoRespondersError
import dev.slne.surf.nats.js.external.nats.core.RequestError
import dev.slne.surf.nats.js.external.nats.core.TimeoutError
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.fromBinary
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.rpc.AllReply
import dev.slne.surf.nats.runtime.rpc.FailureRecorder
import dev.slne.surf.nats.runtime.rpc.classifyAllReply
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.cancellation.CancellationException

// Answer objects of callAnswer: { known: true, value } or { known: false }
internal fun knownAnswer(value: ProtoMessage): Answer = unsafeJso<AnswerInit> {
    known = true
    this.value = value
}

internal fun unknownAnswer(): Answer = unsafeJso<AnswerInit> { known = false }

// Maps a failed nats.js request to the error of the JVM runtime, or returns [error] unchanged.
internal fun mapRequestError(error: Throwable, subject: String, timeoutMs: Int): Throwable {
    if (error is RequestError && error.isNoResponders()) return NatsNoRespondersError(subject)
    if (error is NoRespondersError) return NatsNoRespondersError(subject)
    if (isTimeout(error)) return NatsCallTimeoutError(subject, timeoutMs)
    return error
}

internal fun isTimeout(error: Throwable): Boolean {
    return error is TimeoutError || (error is RequestError && error.cause is TimeoutError)
}

/**
 * Decodes replies by the result kind of a method; the `body` argument resolves the reply body (payload offloading).
 */
internal class ReplyDecoder(
    private val output: DescMessage,
    private val resultKind: NatsResultKind,
) {
    /**
     * Returns `undefined` for UNIT and the decoded message for VALUE; throws [NatsRemoteError] for a `Surf-Error`
     * reply. ANSWER replies are collected by [AllAnswers].
     */
    suspend fun decode(reply: Msg, body: suspend (Msg) -> Uint8Array<*>): ProtoMessage? {
        Wire.header(reply, ProtocolHeaders.ERROR)?.let { type -> throw remoteError(type, reply) }

        return when (resultKind) {
            // undefined, not null: the promise of a UNIT call resolves to undefined (TypeScript void)
            NatsResultKind.UNIT -> undefined
            NatsResultKind.VALUE -> decodeBody(reply, body)
            NatsResultKind.ANSWER -> throw IllegalStateException("ANSWER replies are collected by AllAnswers")
        }
    }

    suspend fun decodeBody(reply: Msg, body: suspend (Msg) -> Uint8Array<*>): ProtoMessage {
        val data = body(reply)
        return decodeMessage(output, data, Wire.header(reply, ProtocolHeaders.CONTENT_TYPE), "reply on ${reply.subject}")
    }

    fun remoteError(type: String, reply: Msg): NatsRemoteError = NatsRemoteError(type, Wire.utf8(reply.data))
}

/**
 * Checks the [contentType], decodes [data] as [schema], normalises enums and checks the required fields like the
 * Kotlin codec.
 */
internal fun decodeMessage(schema: DescMessage, data: Uint8Array<*>, contentType: String?, source: String): ProtoMessage {
    ProtocolHeaders.requireProtobufContentType(contentType)
    val message = fromBinary(schema, data)
    MessageRules.normalizeAndCheck(schema, message)?.let {
        throw IllegalArgumentException("Undecodable ${schema.typeName} ($source): $it")
    }
    return message
}

/**
 * Collects the replies of an `ALL_INSTANCES` call with a result.
 *
 * The first decodable known answer wins. `Surf-Error` replies and undecodable answers are recorded; when the
 * replies end (timeout or no responders) without a known answer, the first recorded error is thrown with the later
 * ones suppressed, otherwise the answer is unknown.
 */
internal class AllAnswers(
    private val decoder: ReplyDecoder,
    private val body: suspend (Msg) -> Uint8Array<*>,
    private val subject: String,
) : FailureRecorder() {

    suspend fun collect(replies: ReceiveChannel<Msg>): Answer {
        for (reply in replies) {
            val kind = classifyAllReply(
                Wire.statusCode(reply),
                { Wire.header(reply, ProtocolHeaders.ERROR) },
                { Wire.header(reply, ProtocolHeaders.ANSWER) },
            )
            when (kind) {
                AllReply.NO_RESPONDERS -> return noAnswer()
                AllReply.OTHER_STATUS, AllReply.UNKNOWN -> Unit
                AllReply.REMOTE_ERROR -> {
                    val remoteType = Wire.header(reply, ProtocolHeaders.ERROR)!!
                    val origin = Wire.header(reply, ProtocolHeaders.ORIGIN)
                    record(decoder.remoteError(remoteType, reply))
                    console.warn("surf-nats: ALL_INSTANCES responder $origin failed on $subject: $remoteType")
                }

                AllReply.ANSWER -> {
                    try {
                        return knownAnswer(decoder.decodeBody(reply, body))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        record(e)
                        console.warn("surf-nats: undecodable ALL_INSTANCES answer on $subject", e)
                    }
                }
            }
        }
        return noAnswer()
    }

    fun noAnswer(): Answer {
        throwIfAny()
        return unknownAnswer()
    }
}
