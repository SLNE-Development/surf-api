package dev.slne.surf.api.core.serializer

import net.kyori.adventure.nbt.ByteArrayBinaryTag
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

class NbtOpsTest {
    @Test
    fun `byte list respects buffer position and limit`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4))
        buffer.position(1)
        buffer.limit(3)

        val result = NbtOps.createByteList(buffer) as ByteArrayBinaryTag

        assertContentEquals(byteArrayOf(2, 3), result.value())
    }
}
