package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.header
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.core.NatsPublishOptions
import dev.slne.surf.nats.js.external.nats.core.Subscription
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.js.external.nats.jetstream.ConsumerConfig
import dev.slne.surf.nats.js.external.nats.jetstream.JsMsg
import dev.slne.surf.nats.js.external.nats.jetstream.NextOptions
import dev.slne.surf.nats.js.external.nats.jetstream.StreamConfig
import dev.slne.surf.nats.js.external.nats.jetstream.jetstreamManager
import dev.slne.surf.nats.js.external.nats.jetstream.jetstreamWithConsumers
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreMeta
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreOptions
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreStatus
import dev.slne.surf.nats.js.external.nats.obj.ObjmAdmin
import dev.slne.surf.nats.js.external.nats.obj.Objm
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.external.protobuf.fromBinary
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import js.typedarrays.Uint8Array
import kotlinx.coroutines.await
import node.crypto.randomUUID

/**
 * Raw nats.js helpers that play the JVM side of the protocol in the e2e tests.
 */
internal class RawNats(val connection: NatsConnection) {

    fun subscribe(subject: String, queue: String? = null, handler: (Msg) -> Unit): Subscription {
        val options = SubscriptionOptions(queue = queue, callback = { error, message ->
            if (error == null) handler(message)
        })
        return connection.subscribe(subject, options)
    }

    suspend fun flush() = connection.flush().await()

    fun publish(subject: String, data: Uint8Array<*>, headers: MsgHdrs? = null) {
        connection.publish(subject, data, NatsPublishOptions(headers = headers))
    }

    fun encode(schema: DescMessage, init: ProtoMessageInit): Uint8Array<*> = toBinary(schema, create(schema, init))

    fun decode(schema: DescMessage, data: Uint8Array<*>): ProtoMessage = fromBinary(schema, data)

    /**
     * Headers of a JVM response or event: content type, origin and sender version.
     */
    fun jvmHeaders(origin: String = "jvm-responder@instance"): MsgHdrs {
        return protobufHeaders(ProtocolHeaders.ORIGIN to origin, ProtocolHeaders.SENDER_VERSION to "jvm")
    }

    fun respond(request: Msg, data: Uint8Array<*>, headers: MsgHdrs = jvmHeaders()) {
        request.respond(data, NatsPublishOptions(headers = headers))
    }

    /**
     * Creates the work-queue stream and the DLQ stream of [names] with the JVM `JetStreamProvisioner` settings.
     *
     * With [noAck] the stream stores and deduplicates publishes but never acknowledges them (lost PubAcks).
     */
    suspend fun addPersistentStreams(names: ServiceNames, noAck: Boolean = false) {
        val manager = jetstreamManager(connection).await()
        manager.streams.add(
            StreamConfig(
                name = names.stream,
                subjects = arrayOf(names.streamFilter),
                retention = "workqueue",
                storage = "file",
                allow_msg_ttl = true,
                max_age = ProtocolDefaults.PERSIST_MAX_AGE_SECONDS * NANOS_PER_SECOND,
                duplicate_window = ProtocolDefaults.DEDUPE_WINDOW_SECONDS * NANOS_PER_SECOND,
                no_ack = noAck,
            )
        ).await()
        manager.consumers.add(
            names.stream,
            ConsumerConfig(durable_name = names.responderGroup, ack_policy = "explicit"),
        ).await()
    }

    /**
     * Returns the next message of the durable consumer of [names], or `null` after [expiresMs].
     */
    suspend fun nextPersistent(names: ServiceNames, expiresMs: Int = 5000): JsMsg? {
        val consumer = jetstreamWithConsumers(connection).consumers.get(names.stream, names.responderGroup).await()
        return consumer.next(NextOptions(expires = expiresMs)).await()
    }

    suspend fun streamMessages(stream: String): Int = jetstreamManager(connection).await().streams.info(stream).await().state.messages

    /**
     * Creates [bucket] like the JS `PayloadStore` when it does not exist yet.
     */
    suspend fun ensureBucket(bucket: PayloadBucket) {
        Objm(connection).create(
            bucket.bucketName,
            ObjectStoreOptions(ttl = bucket.ttl.inWholeNanoseconds.toDouble(), storage = "file"),
        ).await()
    }

    suspend fun bucketStatus(bucket: String): ObjectStoreStatus = ObjmAdmin(connection).open(bucket).await().status().await()

    suspend fun getBlob(bucket: String, name: String): Uint8Array<*>? {
        return Objm(connection).open(bucket).await().getBlob(name).await()
    }

    suspend fun putBlob(bucket: String, data: Uint8Array<*>): String {
        val name = randomUUID()
        val store = Objm(connection).open(bucket).await()
        store.putBlob(ObjectStoreMeta(name = name), data).await()
        return name
    }

    suspend fun close() = connection.close().await()

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}

internal fun header(message: Msg, name: String): String? = message.header(name)

internal fun header(message: JsMsg, name: String): String? = message.headers?.get(name)?.takeIf { it.isNotEmpty() }

internal suspend fun rawNats(): RawNats = RawNats(TestNats.rawConnection())
