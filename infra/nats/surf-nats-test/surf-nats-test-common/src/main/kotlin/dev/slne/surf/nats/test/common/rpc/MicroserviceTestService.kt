package dev.slne.surf.nats.test.common.rpc

import dev.slne.surf.nats.rpc.NatsService
import dev.slne.surf.nats.rpc.NatsPersistent

@NatsService("microservice")
interface MicroserviceTestService {

    /**
     * Echoes the provided text with a prefix.
     */
    suspend fun echo(text: String): String

    /**
     * Call fails on the implementation.
     */
    suspend fun fail(message: String): String

    /**
     * Returns the size of the provided byte array.
     */
    suspend fun size(data: ByteArray): Int

    /**
     * Returns the stored entries.
     */
    suspend fun stored(): List<String>

    /**
     * Stores the provided entry. Uses JetStream.
     */
    @NatsPersistent
    suspend fun store(entry: String)

    /**
     * Returns the number of stored entries. Uses JetStream.
     */
    @NatsPersistent
    suspend fun storedCount(): Int
}