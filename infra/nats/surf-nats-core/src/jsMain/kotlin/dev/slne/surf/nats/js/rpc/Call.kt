package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.MessageCodec
import dev.slne.surf.nats.js.contract.MethodContract
import dev.slne.surf.nats.js.interop.positiveTimeoutOrNull
import dev.slne.surf.nats.runtime.rpc.CallFlow
import js.typedarrays.Uint8Array
import kotlin.time.Duration

/**
 * One service call with its resolved subject, timeout and encoded request body.
 */
internal class Call private constructor(
    val method: MethodContract,
    val subject: String,
    val stream: String,
    val timeout: Duration,
    val body: Uint8Array<*>,
) {
    val timeoutMs: Int get() = timeout.inWholeMilliseconds.toInt()
    val flow: CallFlow get() = method.flow

    companion object {
        fun prepare(
            method: MethodContract,
            request: ProtoMessageInit?,
            owner: String?,
            timeoutMs: Int?,
        ): Call {
            val timeout = positiveTimeoutOrNull(timeoutMs, "Call timeout") ?: method.defaultTimeout
            val names = method.service.names(owner)
            val subject = method.subject(owner)
            val body = MessageCodec.encode(method.input, request)
            return Call(method, subject, names.stream, timeout, body)
        }
    }
}
