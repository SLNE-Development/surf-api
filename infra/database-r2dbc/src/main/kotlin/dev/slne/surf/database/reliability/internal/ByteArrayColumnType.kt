package dev.slne.surf.database.reliability.internal

import org.jetbrains.exposed.v1.core.BasicBinaryColumnType
import java.nio.ByteBuffer

/**
 * Unbounded binary column (`bytea` / `BLOB`). Unlike [BasicBinaryColumnType], reads only the remaining bytes of a
 * [ByteBuffer], which drivers may return as a view of a larger array.
 */
internal class ByteArrayColumnType : BasicBinaryColumnType() {
    override fun valueFromDB(value: Any): ByteArray = when (value) {
        is ByteBuffer -> ByteArray(value.remaining()).also { value.duplicate().get(it) }
        else -> super.valueFromDB(value)
    }
}
