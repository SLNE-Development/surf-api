package dev.slne.surf.api.core.serializer

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import dev.slne.surf.api.core.nbt.asCollection
import dev.slne.surf.api.core.nbt.isCollectionTag
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.LongArrayList
import net.kyori.adventure.nbt.*
import java.nio.ByteBuffer
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

object NbtOps : DynamicOps<BinaryTag> {

    override fun empty() = EndBinaryTag.endBinaryTag()

    override fun <U : Any> convertTo(
        outOps: DynamicOps<U>,
        input: BinaryTag,
    ): U = when (input) {
        is EndBinaryTag -> outOps.empty()
        is ByteBinaryTag -> outOps.createByte(input.value())
        is ShortBinaryTag -> outOps.createShort(input.value())
        is IntBinaryTag -> outOps.createInt(input.value())
        is LongBinaryTag -> outOps.createLong(input.value())
        is FloatBinaryTag -> outOps.createFloat(input.value())
        is DoubleBinaryTag -> outOps.createDouble(input.value())
        is ByteArrayBinaryTag -> outOps.createByteList(ByteBuffer.wrap(input.value()))
        is StringBinaryTag -> outOps.createString(input.value())
        is ListBinaryTag -> convertList(outOps, input)
        is CompoundBinaryTag -> convertMap(outOps, input)
        is IntArrayBinaryTag -> outOps.createIntList(input.stream())
        is LongArrayBinaryTag -> outOps.createLongList(input.stream())
        else -> throw MatchException("Unknown tag type: ${input::class.java}", null)
    }

    override fun getNumberValue(input: BinaryTag): DataResult<Number> {
        return if (input is NumberBinaryTag) {
            DataResult.success(input.numberValue())
        } else {
            DataResult.error { "Not a number" }
        }
    }

    override fun createNumeric(i: Number) = DoubleBinaryTag.doubleBinaryTag(i.toDouble())
    override fun createByte(value: Byte) = ByteBinaryTag.byteBinaryTag(value)
    override fun createShort(value: Short) = ShortBinaryTag.shortBinaryTag(value)
    override fun createInt(value: Int) = IntBinaryTag.intBinaryTag(value)
    override fun createLong(value: Long) = LongBinaryTag.longBinaryTag(value)
    override fun createFloat(value: Float) = FloatBinaryTag.floatBinaryTag(value)
    override fun createDouble(value: Double) = DoubleBinaryTag.doubleBinaryTag(value)
    override fun createBoolean(value: Boolean) = ByteBinaryTag.byteBinaryTag(if (value) 1 else 0)
    override fun createString(value: String): BinaryTag = StringBinaryTag.stringBinaryTag(value)

    override fun getStringValue(input: BinaryTag): DataResult<String> {
        return if (input is StringBinaryTag) {
            DataResult.success(input.value())
        } else {
            DataResult.error { "Not a string" }
        }
    }

    override fun mergeToList(
        list: BinaryTag,
        value: BinaryTag,
    ): DataResult<BinaryTag> = createCollector(list)
        ?.let { DataResult.success(it.accept(value).result) }
        ?: DataResult.error({ "mergeToList called with not a list: $list" }, list)

    override fun mergeToList(
        list: BinaryTag,
        values: List<BinaryTag>,
    ): DataResult<BinaryTag> = createCollector(list)
        ?.let { DataResult.success(it.acceptAll(values).result) }
        ?: DataResult.error({ "mergeToList called with not a list: $list" }, list)

    override fun mergeToMap(
        map: BinaryTag,
        key: BinaryTag,
        value: BinaryTag,
    ): DataResult<BinaryTag> {
        if (map !is CompoundBinaryTag && map !is EndBinaryTag) {
            return DataResult.error({ "mergeToMap called with not a map: $map" }, map)
        }

        if (key !is StringBinaryTag) {
            return DataResult.error({ "Key is not a string: $key" }, map)
        }

        val builder = CompoundBinaryTag.builder().apply {
            if (map is CompoundBinaryTag) {
                put(map)
            }

            put(key.value(), value)
        }

        return DataResult.success(builder.build())
    }

    override fun mergeToMap(
        map: BinaryTag,
        values: MapLike<BinaryTag>,
    ): DataResult<BinaryTag> {
        if (map !is CompoundBinaryTag && map !is EndBinaryTag) {
            return DataResult.error({ "mergeToMap called with not a map: $map" }, map)
        }

        val nonStrings = mutableListOf<BinaryTag>()
        val builder = CompoundBinaryTag.builder().apply {
            if (map is CompoundBinaryTag) {
                put(map)
            }
        }

        values.entries().forEach { pair ->
            val key = pair.first
            val value = pair.second

            if (key !is StringBinaryTag) {
                nonStrings.add(key)
            } else {
                builder.put(key.value(), value)
            }
        }

        if (nonStrings.isNotEmpty()) {
            return DataResult.error({ "Some keys are not strings: $nonStrings" }, builder.build())
        }

        return DataResult.success(builder.build())
    }

    override fun mergeToMap(
        map: BinaryTag,
        values: Map<BinaryTag, BinaryTag>,
    ): DataResult<BinaryTag> {
        if (map !is CompoundBinaryTag && map !is EndBinaryTag) {
            return DataResult.error({ "mergeToMap called with not a map: $map" }, map)
        }

        val nonStrings = mutableListOf<BinaryTag>()
        val builder = CompoundBinaryTag.builder().apply {
            if (map is CompoundBinaryTag) {
                put(map)
            }

            values.forEach { (key, value) ->
                if (key !is StringBinaryTag) {
                    nonStrings.add(key)
                } else {
                    put(key.value(), value)
                }
            }
        }

        if (nonStrings.isNotEmpty()) {
            return DataResult.error({ "Some keys are not strings: $nonStrings" }, builder.build())
        }

        return DataResult.success(builder.build())
    }

    override fun getMapValues(input: BinaryTag): DataResult<Stream<Pair<BinaryTag, BinaryTag>>> {
        if (input !is CompoundBinaryTag) {
            return DataResult.error { "Not a map: $input" }
        }

        val resultStream = input.stream()
            .map { (key, value) -> Pair.of(createString(key), value) }

        return DataResult.success(resultStream)
    }

    override fun getMapEntries(input: BinaryTag): DataResult<Consumer<BiConsumer<BinaryTag, BinaryTag>>> {
        if (input !is CompoundBinaryTag) {
            return DataResult.error { "Not a map: $input" }
        }

        return DataResult.success(Consumer { dataProcessor ->
            input.forEach { (key, value) ->
                dataProcessor.accept(createString(key), value)
            }
        })
    }

    override fun getMap(input: BinaryTag): DataResult<MapLike<BinaryTag>> {
        if (input !is CompoundBinaryTag) {
            return DataResult.error { "Not a map: $input" }
        }

        val mapLike = object : MapLike<BinaryTag> {
            override fun get(key: BinaryTag): BinaryTag? {
                if (key !is StringBinaryTag) {
                    throw UnsupportedOperationException("Cannot get map entry with non-string key: $key")
                }

                return input.get(key.value())
            }

            override fun get(key: String): BinaryTag? {
                return input.get(key)
            }

            override fun entries(): Stream<Pair<BinaryTag, BinaryTag>> {
                return input.stream()
                    .map { (key, value) -> Pair.of(createString(key), value) }
            }

            override fun toString(): String {
                return "MapLike[$input])"
            }
        }

        return DataResult.success(mapLike)
    }

    override fun createMap(map: Stream<Pair<BinaryTag, BinaryTag>>): BinaryTag {
        return map.collect(CompoundBinaryTag.toCompoundTag({
            val key = it.first
            if (key !is StringBinaryTag) {
                throw UnsupportedOperationException("Cannot create map with non-string key: $key")
            }

            key.value()
        }, { it.second }))
    }

    override fun getStream(input: BinaryTag): DataResult<Stream<BinaryTag>> {
        if (!input.isCollectionTag()) {
            return DataResult.error { "Not a list: $input" }
        }

        return DataResult.success(input.asCollection().stream())
    }

    override fun getList(input: BinaryTag): DataResult<Consumer<Consumer<BinaryTag>>> {
        if (!input.isCollectionTag()) {
            return DataResult.error { "Not a list: $input" }
        }

        val collectionTag = input.asCollection()
        return DataResult.success(Consumer(collectionTag::forEach))
    }

    override fun getByteBuffer(input: BinaryTag): DataResult<ByteBuffer> {
        if (input is ByteArrayBinaryTag) {
            return DataResult.success(ByteBuffer.wrap(input.value()))
        }

        return super.getByteBuffer(input)
    }

    override fun createByteList(input: ByteBuffer): BinaryTag {
        val buffer = input.duplicate().clear()
        val bytes = ByteArray(input.capacity())
        buffer.get(bytes)
        return ByteArrayBinaryTag.byteArrayBinaryTag(*bytes)
    }

    override fun getIntStream(input: BinaryTag): DataResult<IntStream> {
        if (input is IntArrayBinaryTag) {
            return DataResult.success(input.stream())
        }

        return super.getIntStream(input)
    }

    override fun createIntList(input: IntStream): BinaryTag {
        return IntArrayBinaryTag.intArrayBinaryTag(*input.toArray())
    }

    override fun getLongStream(input: BinaryTag): DataResult<LongStream> {
        if (input is LongArrayBinaryTag) {
            return DataResult.success(input.stream())
        }

        return super.getLongStream(input)
    }

    override fun createLongList(input: LongStream): BinaryTag {
        return LongArrayBinaryTag.longArrayBinaryTag(*input.toArray())
    }

    override fun createList(input: Stream<BinaryTag>): BinaryTag =
        ListBinaryTag.heterogeneousListBinaryTag()
            .add(input.toList())
            .build()

    override fun remove(
        input: BinaryTag,
        key: String,
    ): BinaryTag {
        if (input !is CompoundBinaryTag) {
            return input
        }

        return input.remove(key)
    }

    override fun mapBuilder(): RecordBuilder<BinaryTag> = NbtRecordBuilder()

    override fun toString(): String {
        return "NbtOps"
    }

    private fun createCollector(tag: BinaryTag): ListCollector? {
        if (tag is EndBinaryTag) {
            return GenericListCollector()
        }

        if (tag is Iterable<*>) {
            if (!tag.iterator().hasNext()) {
                return GenericListCollector()
            }
        } else {
            return null
        }

        return when (tag) {
            is ListBinaryTag -> GenericListCollector(tag)
            is ByteArrayBinaryTag -> ByteListCollector(tag.value())
            is IntArrayBinaryTag -> IntListCollector(tag.value())
            is LongArrayBinaryTag -> LongListCollector(tag.value())
            else -> throw MatchException("Unknown tag type: ${tag::class.java}", null)
        }
    }


    interface ListCollector {
        val result: BinaryTag
        fun accept(tag: BinaryTag): ListCollector

        fun acceptAll(tags: Iterable<BinaryTag>): ListCollector {
            var listCollector = this
            for (tag in tags) {
                listCollector = listCollector.accept(tag)
            }

            return listCollector
        }
    }

    class ByteListCollector(values: ByteArray) : ListCollector {
        private val values = ByteArrayList(values)

        override val result: BinaryTag
            get() = ByteArrayBinaryTag.byteArrayBinaryTag(*values.toByteArray())

        override fun accept(tag: BinaryTag): ListCollector {
            if (tag is ByteBinaryTag) {
                values.add(tag.value())
                return this
            } else {
                return GenericListCollector(this.values).accept(tag)
            }
        }
    }

    class IntListCollector(values: IntArray) : ListCollector {
        private val values = IntArrayList(values)

        override val result: BinaryTag
            get() = IntArrayBinaryTag.intArrayBinaryTag(*values.toIntArray())

        override fun accept(tag: BinaryTag): ListCollector {
            if (tag is IntBinaryTag) {
                values.add(tag.value())
                return this
            } else {
                return GenericListCollector(this.values).accept(tag)
            }
        }
    }

    class LongListCollector(values: LongArray) : ListCollector {
        private val values = LongArrayList(values)

        override val result: BinaryTag
            get() = LongArrayBinaryTag.longArrayBinaryTag(*values.toLongArray())

        override fun accept(tag: BinaryTag): ListCollector {
            if (tag is LongBinaryTag) {
                values.add(tag.value())
                return this
            } else {
                return GenericListCollector(this.values).accept(tag)
            }
        }
    }

    class GenericListCollector : ListCollector {
        private val resultBuilder = ListBinaryTag.heterogeneousListBinaryTag()
        override val result get() = resultBuilder.build()

        constructor()

        constructor(list: ListBinaryTag) {
            for (tag in list) {
                resultBuilder.add(tag)
            }
        }

        constructor(list: IntArrayList) {
            list.intIterator().forEachRemaining { i ->
                resultBuilder.add(IntBinaryTag.intBinaryTag(i))
            }
        }

        constructor(list: ByteArrayList) {
            list.iterator().forEachRemaining { b: Byte ->
                resultBuilder.add(ByteBinaryTag.byteBinaryTag(b))
            }
        }

        constructor(list: LongArrayList) {
            list.iterator().forEachRemaining { l ->
                resultBuilder.add(LongBinaryTag.longBinaryTag(l))
            }
        }

        override fun accept(tag: BinaryTag): ListCollector = apply {
            resultBuilder.add(tag)
        }
    }

    class NbtRecordBuilder :
        RecordBuilder.AbstractStringBuilder<BinaryTag, CompoundBinaryTag>(this) {
        override fun initBuilder() = CompoundBinaryTag.empty()

        override fun append(
            key: String,
            value: BinaryTag,
            builder: CompoundBinaryTag,
        ) = builder.put(key, value)

        override fun build(
            builder: CompoundBinaryTag,
            prefix: BinaryTag,
        ): DataResult<BinaryTag> {
            if (prefix is EndBinaryTag) {
                return DataResult.success(builder)
            }

            if (prefix !is CompoundBinaryTag) {
                return DataResult.error({ "mergeToMap called with not a map: $prefix" }, prefix)
            }

            val result = CompoundBinaryTag.builder()
                .put(prefix)
                .put(builder)
                .build()

            return DataResult.success(result)
        }
    }
}