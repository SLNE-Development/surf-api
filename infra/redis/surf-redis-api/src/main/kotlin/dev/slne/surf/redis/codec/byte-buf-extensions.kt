package dev.slne.surf.redis.codec

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.time.Instant
import java.util.BitSet
import java.util.EnumSet
import java.util.UUID
import kotlin.enums.enumEntries

fun ByteBuf.writeVarInt(value: Int) {
    RedisVarInt.write(this, value)
}

fun ByteBuf.readVarInt(): Int {
    return RedisVarInt.read(this)
}

fun ByteBuf.writeVarLong(value: Long) {
    RedisVarLong.write(this, value)
}

fun ByteBuf.readVarLong(): Long {
    return RedisVarLong.read(this)
}

fun ByteBuf.writeString(value: CharSequence, maxLength: Int = RedisUtf8String.MAX_STRING_LENGTH) {
    RedisUtf8String.write(this, value, maxLength)
}

fun ByteBuf.readString(maxLength: Int = RedisUtf8String.MAX_STRING_LENGTH): String {
    return RedisUtf8String.read(this, maxLength)
}

fun <T> ByteBuf.writeNullable(value: T?, writer: (ByteBuf, T) -> Unit) {
    if (value == null) {
        writeBoolean(false)
    } else {
        writeBoolean(true)
        writer(this, value)
    }
}

fun <T> ByteBuf.readNullable(reader: (ByteBuf) -> T): T? {
    if (!readBoolean()) return null
    return reader(this)
}

fun <T> ByteBuf.writeCollection(collection: Collection<T>, writer: (ByteBuf, T) -> Unit) {
    writeVarInt(collection.size)
    collection.forEach { writer(this, it) }
}

fun <T, C : MutableCollection<T>> ByteBuf.readCollection(creator: (Int) -> C, reader: (ByteBuf) -> T): C {
    val size = readVarInt()
    checkDecoding(size >= 0) { "Collection size must not be negative: $size" }

    val collection = creator(size)
    repeat(size) {
        collection.add(reader(this))
    }
    return collection
}

fun <T> ByteBuf.readList(reader: (ByteBuf) -> T): ObjectArrayList<T> = readCollection(::ObjectArrayList, reader)

fun <T> ByteBuf.writeArray(array: Array<T>, writer: (ByteBuf, T) -> Unit) {
    writeVarInt(array.size)
    array.forEach { writer(this, it) }
}

fun <T> ByteBuf.readArray(type: Class<T>, reader: (ByteBuf) -> T): Array<T> {
    val length = readVarInt()
    checkDecoding(length >= 0) { "Array length must not be negative: $length" }

    @Suppress("UNCHECKED_CAST")
    val array = java.lang.reflect.Array.newInstance(type, length) as Array<T>

    for (i in 0 until length) {
        array[i] = reader(this)
    }

    return array
}

inline fun <reified T> ByteBuf.readArray(noinline reader: (ByteBuf) -> T): Array<T> {
    return readArray(T::class.java, reader)
}

private fun ByteBuf.readPrimitiveArrayLength(type: String): Int {
    val length = readVarInt()
    checkDecoding(length >= 0) { "$type length must not be negative: $length" }
    return length
}

private fun ByteBuf.checkReadableArrayBytes(
    type: String,
    length: Int,
    bytesPerElement: Int
) {
    val neededBytes = length.toLong() * bytesPerElement

    checkDecoding(neededBytes <= Int.MAX_VALUE) {
        "$type byte size too big: $neededBytes"
    }

    checkDecoding(readableBytes() >= neededBytes) {
        "Not enough readable bytes for $type: need $neededBytes, have ${readableBytes()}"
    }
}

fun ByteBuf.writeByteArray(array: ByteArray) {
    writeVarInt(array.size)
    writeBytes(array)
}

fun ByteBuf.readByteArray(): ByteArray {
    val length = readPrimitiveArrayLength("ByteArray")
    checkReadableArrayBytes("ByteArray", length, Byte.SIZE_BYTES)

    val array = ByteArray(length)
    readBytes(array)
    return array
}

fun ByteBuf.writeBooleanArray(array: BooleanArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeBoolean(value)
    }
}

fun ByteBuf.readBooleanArray(): BooleanArray {
    val length = readPrimitiveArrayLength("BooleanArray")
    checkReadableArrayBytes("BooleanArray", length, Byte.SIZE_BYTES)

    return BooleanArray(length) {
        readBoolean()
    }
}

fun ByteBuf.writeShortArray(array: ShortArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeShort(value.toInt())
    }
}

fun ByteBuf.readShortArray(): ShortArray {
    val length = readPrimitiveArrayLength("ShortArray")
    checkReadableArrayBytes("ShortArray", length, Short.SIZE_BYTES)

    return ShortArray(length) {
        readShort()
    }
}

fun ByteBuf.writeCharArray(array: CharArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeChar(value.code)
    }
}

fun ByteBuf.readCharArray(): CharArray {
    val length = readPrimitiveArrayLength("CharArray")
    checkReadableArrayBytes("CharArray", length, Char.SIZE_BYTES)

    return CharArray(length) {
        readChar()
    }
}

fun ByteBuf.writeIntArray(array: IntArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeInt(value)
    }
}

fun ByteBuf.readIntArray(): IntArray {
    val length = readPrimitiveArrayLength("IntArray")
    checkReadableArrayBytes("IntArray", length, Int.SIZE_BYTES)

    return IntArray(length) {
        readInt()
    }
}

fun ByteBuf.writeLongArray(array: LongArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeLong(value)
    }
}

fun ByteBuf.readLongArray(): LongArray {
    val length = readPrimitiveArrayLength("LongArray")
    checkReadableArrayBytes("LongArray", length, Long.SIZE_BYTES)

    return LongArray(length) {
        readLong()
    }
}

fun ByteBuf.writeFloatArray(array: FloatArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeFloat(value)
    }
}

fun ByteBuf.readFloatArray(): FloatArray {
    val length = readPrimitiveArrayLength("FloatArray")
    checkReadableArrayBytes("FloatArray", length, Float.SIZE_BYTES)

    return FloatArray(length) {
        readFloat()
    }
}

fun ByteBuf.writeDoubleArray(array: DoubleArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeDouble(value)
    }
}

fun ByteBuf.readDoubleArray(): DoubleArray {
    val length = readPrimitiveArrayLength("DoubleArray")
    checkReadableArrayBytes("DoubleArray", length, Double.SIZE_BYTES)

    return DoubleArray(length) {
        readDouble()
    }
}

fun ByteBuf.writeVarIntArray(array: IntArray) {
    writeVarInt(array.size)

    for (value in array) {
        writeVarInt(value)
    }
}

fun ByteBuf.readVarIntArray(): IntArray {
    val length = readPrimitiveArrayLength("VarIntArray")

    return IntArray(length) {
        readVarInt()
    }
}

fun ByteBuf.writeInstant(instant: Instant) {
    writeLong(instant.toEpochMilli())
}

fun ByteBuf.readInstant(): Instant {
    return Instant.ofEpochMilli(readLong())
}

fun <K, V> ByteBuf.writeMap(
    map: Map<K, V>,
    keyWriter: (ByteBuf, K) -> Unit,
    valueWriter: (ByteBuf, V) -> Unit
) {
    writeVarInt(map.size)

    for ((key, value) in map) {
        keyWriter(this, key)
        valueWriter(this, value)
    }
}

fun <K, V, M : MutableMap<K, V>> ByteBuf.readMap(
    creator: (Int) -> M,
    keyReader: (ByteBuf) -> K,
    valueReader: (ByteBuf) -> V
): M {
    val size = readVarInt()
    checkDecoding(size >= 0) { "Map size must not be negative: $size" }

    val map = creator(size)

    repeat(size) {
        val key = keyReader(this)
        val value = valueReader(this)
        map[key] = value
    }

    return map
}

fun <K, V> ByteBuf.readMap(
    keyReader: (ByteBuf) -> K,
    valueReader: (ByteBuf) -> V
): Object2ObjectOpenHashMap<K, V> {
    return readMap(::Object2ObjectOpenHashMap, keyReader, valueReader)
}

fun ByteBuf.writeUuid(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun ByteBuf.readUuid(): UUID {
    return UUID(readLong(), readLong())
}

fun ByteBuf.writeEnum(value: Enum<*>) {
    writeVarInt(value.ordinal)
}

inline fun <reified E : Enum<E>> ByteBuf.readEnum(): E {
    val ordinal = readVarInt()
    val values = enumEntries<E>()

    checkDecoding(ordinal in values.indices) {
        "Invalid enum ordinal for ${E::class.simpleName}: $ordinal"
    }

    return values[ordinal]
}

inline fun <reified E : Enum<E>> ByteBuf.writeEnumSet(set: Set<E>) {
    val values = enumEntries<E>()
    val bitSet = BitSet(values.size)

    for (i in values.indices) {
        bitSet.set(i, values[i] in set)
    }

    writeFixedBitSet(bitSet, values.size)
}

inline fun <reified E : Enum<E>> ByteBuf.readEnumSet(): EnumSet<E> {
    val values = enumEntries<E>()
    val bitSet = readFixedBitSet(values.size)
    val result = EnumSet.noneOf(E::class.java)

    for (i in values.indices) {
        if (bitSet.get(i)) {
            result.add(values[i])
        }
    }

    return result
}

fun ByteBuf.writeBitSet(bitSet: BitSet) {
    writeLongArray(bitSet.toLongArray())
}

fun ByteBuf.readBitSet(): BitSet {
    return BitSet.valueOf(readLongArray())
}

fun ByteBuf.writeFixedBitSet(bitSet: BitSet, size: Int) {
    check(bitSet.length() <= size) {
        "BitSet is larger than expected size (${bitSet.length()} > $size)"
    }

    val byteSize = (size + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS
    val bytes = bitSet.toByteArray().copyOf(byteSize)
    writeBytes(bytes)
}

fun ByteBuf.readFixedBitSet(size: Int): BitSet {
    val byteSize = (size + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS
    val bytes = ByteArray(byteSize)
    readBytes(bytes)
    return BitSet.valueOf(bytes)
}

fun ByteBuf.readWithCount(reader: (ByteBuf) -> Unit) {
    val count = readVarInt()
    checkDecoding(count >= 0) { "Count must not be negative: $count" }

    repeat(count) {
        reader(this)
    }
}

fun <T> ByteBuf.writeById(value: T, idGetter: (T) -> Int) {
    writeVarInt(idGetter(value))
}

fun <T> ByteBuf.readById(resolver: (Int) -> T): T {
    return resolver(readVarInt())
}

fun ByteBuf.writeVarIntList(list: IntList) {
    writeVarInt(list.size)

    list.iterator().forEachRemaining { value ->
        writeVarInt(value)
    }
}

fun ByteBuf.readVarIntList(): IntArrayList {
    val size = readVarInt()
    checkDecoding(size >= 0) { "IntList size must not be negative: $size" }

    val list = IntArrayList(size)

    repeat(size) {
        list.add(readVarInt())
    }

    return list
}

fun ByteBuf.writeKey(key: Key) {
    writeString(key.asMinimalString())
}

fun ByteBuf.readKey(): Key {
    return Key.key(readString())
}

fun ByteBuf.writeComponent(component: Component) {
    val string = GsonComponentSerializer.gson().serialize(component)
    writeString(string, Int.MAX_VALUE)
}

fun ByteBuf.readComponent(): Component {
    return GsonComponentSerializer.gson().deserialize(readString(Int.MAX_VALUE))
}