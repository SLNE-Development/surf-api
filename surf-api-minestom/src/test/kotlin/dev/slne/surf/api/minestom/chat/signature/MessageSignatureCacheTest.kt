package dev.slne.surf.api.minestom.chat.signature

import net.minestom.server.crypto.MessageSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class MessageSignatureCacheTest {

    private fun signature(seed: Int): MessageSignature =
        MessageSignature(ByteArray(SIGNATURE_LENGTH) { index -> (seed + index).toByte() })

    private fun bodyWith(lastSeen: List<MessageSignature>) = SignedMessageBody(
        content = "content",
        timeStamp = Instant.EPOCH,
        salt = 0L,
        lastSeen = LastSeenMessages(lastSeen),
    )

    @Test
    fun `an unknown signature is not found`() {
        val cache = MessageSignatureCache(4)

        assertEquals(MessageSignatureCache.NOT_FOUND, cache.pack(signature(1)))
    }

    @Test
    fun `a pushed signature is packed to the index it was stored at`() {
        val cache = MessageSignatureCache(4)
        val pushed = signature(1)

        cache.push(bodyWith(emptyList()), pushed)

        assertEquals(0, cache.pack(pushed))
        assertEquals(pushed, cache.unpack(0))
    }

    @Test
    fun `an equal signature from a different array packs to the same index`() {
        val cache = MessageSignatureCache(4)

        cache.push(bodyWith(emptyList()), signature(7))

        assertEquals(0, cache.pack(signature(7)))
    }

    @Test
    fun `the newest signature takes the first slot and shifts the previous one down`() {
        val cache = MessageSignatureCache(4)
        val first = signature(1)
        val second = signature(2)

        cache.push(bodyWith(emptyList()), first)
        cache.push(bodyWith(emptyList()), second)

        assertEquals(0, cache.pack(second))
        assertEquals(1, cache.pack(first))
    }

    @Test
    fun `last seen entries are pushed before the message signature`() {
        val cache = MessageSignatureCache(8)
        val seenFirst = signature(1)
        val seenSecond = signature(2)
        val own = signature(3)

        cache.push(bodyWith(listOf(seenFirst, seenSecond)), own)

        // push() drains the queue from the back, so the message's own signature lands first.
        assertEquals(0, cache.pack(own))
        assertEquals(1, cache.pack(seenSecond))
        assertEquals(2, cache.pack(seenFirst))
    }

    @Test
    fun `re-pushing an entry keeps it in the cache exactly once`() {
        val cache = MessageSignatureCache(4)
        val repeated = signature(1)
        val other = signature(2)

        cache.push(bodyWith(emptyList()), repeated)
        cache.push(bodyWith(emptyList()), other)
        cache.push(bodyWith(emptyList()), repeated)

        assertEquals(0, cache.pack(repeated))
        assertEquals(1, cache.pack(other))
        assertNull(cache.unpack(2))
    }

    @Test
    fun `entries beyond the capacity are dropped`() {
        val cache = MessageSignatureCache(2)

        cache.push(bodyWith(emptyList()), signature(1))
        cache.push(bodyWith(emptyList()), signature(2))
        cache.push(bodyWith(emptyList()), signature(3))

        assertEquals(0, cache.pack(signature(3)))
        assertEquals(1, cache.pack(signature(2)))
        assertEquals(MessageSignatureCache.NOT_FOUND, cache.pack(signature(1)))
    }

    private companion object {
        const val SIGNATURE_LENGTH = 256
    }
}
