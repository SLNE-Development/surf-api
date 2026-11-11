package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.HeaderWriter
import dev.slne.surf.nats.runtime.deadlineHeader
import dev.slne.surf.nats.runtime.headerLineBytes
import dev.slne.surf.nats.runtime.randomId
import kotlin.time.Duration

/**
 * Offloads a body that does not fit into [maxPayload] and returns whether it was offloaded.
 *
 * [headerBytes] is the encoded size of the headers before this call; [extraHeaderBytes] accounts for headers the
 * client library adds after it. With [stampDeadline], `Surf-Deadline` (now + [lifetime]) counts in the size check
 * and is written exactly once: before returning `false`, or after [upload], which used up part of the lifetime.
 *
 * When offloading, [upload] stores the body under the random object name it receives; [writeHeader] then receives
 * that name as `Surf-Payload-Ref` and the caller sends an empty body. Only a body that must be offloaded is checked
 * against the TTL of [bucket]: a [lifetime] above it throws [IllegalArgumentException] before [upload]. A failing
 * [upload] propagates and no header is written. [upload] may suspend when the caller is a suspend function.
 */
@InternalNatsApi
internal inline fun offloadPayload(
    bodyBytes: Int,
    headerBytes: Int,
    extraHeaderBytes: Int,
    maxPayload: Long,
    lifetime: Duration,
    bucket: PayloadBucket,
    stampDeadline: Boolean,
    writeHeader: HeaderWriter,
    upload: (name: String) -> Unit,
): Boolean {
    val deadline = if (stampDeadline) deadlineHeader(lifetime) else null
    val sizedHeaderBytes = headerBytes + headerLineBytes(ProtocolHeaders.DEADLINE, deadline)
    if (!ProtocolDefaults.requiresOffload(bodyBytes, sizedHeaderBytes, extraHeaderBytes, maxPayload)) {
        if (deadline != null) writeHeader(ProtocolHeaders.DEADLINE, deadline)
        return false
    }

    require(lifetime <= bucket.ttl) { bucket.lifetimeExceededMessage(bodyBytes, lifetime) }

    val name = randomId()
    upload(name)
    // The upload used up part of the lifetime
    if (stampDeadline) writeHeader(ProtocolHeaders.DEADLINE, deadlineHeader(lifetime))
    writeHeader(ProtocolHeaders.PAYLOAD_REF, name)
    return true
}
