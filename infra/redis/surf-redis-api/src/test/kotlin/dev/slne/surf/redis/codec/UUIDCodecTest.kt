package dev.slne.surf.redis.codec

import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UUIDCodecTest {

    @Test
    fun `round trips a uuid through its string form`() {
        val uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val encoded = UUIDCodec.INSTANCE.valueEncoder.encode(uuid)

        try {
            assertEquals(uuid.toString(), encoded.toString(Charsets.UTF_8))
            assertEquals(uuid, UUIDCodec.INSTANCE.valueDecoder.decode(encoded, State()))
        } finally {
            encoded.release()
        }
    }

    @Test
    fun `copies itself for another class loader`() {
        val copy = BaseCodec.copy(javaClass.classLoader, UUIDCodec.INSTANCE)

        assertIs<UUIDCodec>(copy)
    }
}
