@file:Suppress("NOTHING_TO_INLINE", "UNUSED")

package dev.slne.surf.surfapi.core.api.util

import it.unimi.dsi.fastutil.booleans.*
import it.unimi.dsi.fastutil.bytes.*
import it.unimi.dsi.fastutil.chars.*
import it.unimi.dsi.fastutil.doubles.*
import it.unimi.dsi.fastutil.floats.*
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.*
import it.unimi.dsi.fastutil.objects.*
import it.unimi.dsi.fastutil.shorts.*
import org.jetbrains.annotations.Unmodifiable
import org.jetbrains.annotations.UnmodifiableView

// region Set
// =====================================================
// ObjectSet Extensions
// =====================================================
inline fun <T> mutableObjectSetOf(vararg elements: T) = ObjectOpenHashSet(elements)
inline fun <T> mutableObjectSetOfNotNull(vararg elements: T?) =
    elements.filterNotNullTo(ObjectOpenHashSet())

inline fun <T> mutableObjectSetOf() = ObjectOpenHashSet<T>()
inline fun <T> mutableObjectSetOf(capacity: Int) = ObjectOpenHashSet<T>(capacity)
inline fun <T> mutableObjectSetOf(iterable: Iterable<T>) = when (iterable) {
    is Collection -> ObjectOpenHashSet(iterable)
    else -> ObjectOpenHashSet<T>(iterable.iterator())
}

inline fun <T> objectSetOf(vararg elements: T): ObjectSet<T> = when (elements.size) {
    0 -> emptyObjectSet<T>()
    1 -> ObjectSets.singleton(elements[0])
    else -> mutableObjectSetOf<T>(*elements).freeze()
}

inline fun <T> objectSetOfNotNull(vararg elements: T?): ObjectSet<T> = when (elements.size) {
    0 -> emptyObjectSet<T>()
    1 -> elements[0]?.let { ObjectSets.singleton(it) } ?: emptyObjectSet()
    else -> mutableObjectSetOfNotNull(*elements).freeze()
}

inline fun <T> objectSetOf(collection: Iterable<T>) = mutableObjectSetOf<T>(collection).freeze()
inline fun <T> objectSetOf() = emptyObjectSet<T>()
inline fun <T> emptyObjectSet(): @Unmodifiable ObjectSet<T> = ObjectSets.emptySet()
inline fun <T> ObjectSet<T>.synchronize(): ObjectSet<T> = ObjectSets.synchronize(this)
inline fun <T> ObjectSet<T>.freeze(): @UnmodifiableView ObjectSet<T> =
    this as? ObjectSets.UnmodifiableSet ?: ObjectSets.unmodifiable(this)

inline fun <T> Sequence<T>.toMutableObjectSet(): ObjectSet<T> = ObjectOpenHashSet(iterator())
inline fun <T> Sequence<T>.toObjectSet(): ObjectSet<T> {
    val it = iterator()
    if (!it.hasNext()) return emptyObjectSet()
    val first = it.next()
    if (!it.hasNext()) return objectSetOf(first)
    val set = mutableObjectSetOf<T>()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun <T> Array<out T>.toObjectSet() = objectSetOf(*this)
inline fun <T> Array<out T>.toMutableObjectSet() = mutableObjectSetOf(*this)
inline fun <T> Iterable<T>.toObjectSet() = (this as? ObjectSet<T>)?.freeze() ?: objectSetOf(this)
inline fun <T> Iterable<T>.toMutableObjectSet() =
    (this as? ObjectSet<T>)?.toMutableObjectSet() ?: mutableObjectSetOf(this)

inline fun <T> Collection<T>.toObjectSet() = (this as? ObjectSet<T>)?.freeze() ?: objectSetOf(this)
inline fun <T> ObjectSet<T>.toMutableObjectSet() =
    this as? ObjectOpenHashSet<T> ?: ObjectOpenHashSet(this)


// =====================================================
// BooleanSet Extensions
// =====================================================

inline fun mutableBooleanSetOf(vararg elements: Boolean): BooleanSet = BooleanOpenHashSet(elements)
inline fun mutableBooleanSetOfNotNull(vararg elements: Boolean?) =
    elements.filterNotNullTo(BooleanOpenHashSet())

inline fun mutableBooleanSetOf(): BooleanSet = BooleanOpenHashSet()
inline fun mutableBooleanSetOf(capacity: Int): BooleanSet = BooleanOpenHashSet(capacity)
inline fun mutableBooleanSetOf(iterable: Iterable<Boolean>): BooleanSet = when (iterable) {
    is Collection -> BooleanOpenHashSet(iterable)
    else -> BooleanOpenHashSet(iterable.iterator())
}

inline fun booleanSetOf(vararg elements: Boolean): BooleanSet = when (elements.size) {
    0 -> emptyBooleanSet()
    1 -> BooleanSets.singleton(elements[0])
    else -> mutableBooleanSetOf(*elements).freeze()
}

inline fun booleanSetOfNotNull(vararg elements: Boolean?): BooleanSet = when (elements.size) {
    0 -> emptyBooleanSet()
    1 -> elements[0]?.let { BooleanSets.singleton(it) } ?: emptyBooleanSet()
    else -> mutableBooleanSetOfNotNull(*elements).freeze()
}

inline fun booleanSetOf(iterable: Iterable<Boolean>): BooleanSet =
    mutableBooleanSetOf(iterable).freeze()

inline fun booleanSetOf(): BooleanSet = emptyBooleanSet()
inline fun emptyBooleanSet(): @Unmodifiable BooleanSet = BooleanSets.emptySet()
inline fun BooleanSet.synchronize(): BooleanSet = BooleanSets.synchronize(this)
inline fun BooleanSet.freeze(): @UnmodifiableView BooleanSet =
    this as? BooleanSets.UnmodifiableSet ?: BooleanSets.unmodifiable(this)

inline fun Sequence<Boolean>.toMutableBooleanSet(): BooleanSet = BooleanOpenHashSet(iterator())
inline fun Sequence<Boolean>.toBooleanSet(): BooleanSet {
    val it = iterator()
    if (!it.hasNext()) return emptyBooleanSet()
    val first = it.next()
    if (!it.hasNext()) return booleanSetOf(first)
    val set = mutableBooleanSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Boolean>.toBooleanSet() = booleanSetOf(*toBooleanArray())
inline fun Array<Boolean>.toMutableBooleanSet() = mutableBooleanSetOf(*toBooleanArray())
inline fun Iterable<Boolean>.toBooleanSet() = (this as? BooleanSet)?.freeze() ?: booleanSetOf(this)
inline fun Iterable<Boolean>.toMutableBooleanSet() =
    (this as? BooleanSet)?.toMutableBooleanSet() ?: mutableBooleanSetOf(this)

inline fun Collection<Boolean>.toBooleanSet() =
    (this as? BooleanSet)?.freeze() ?: booleanSetOf(this)

inline fun BooleanSet.toMutableBooleanSet() =
    this as? BooleanOpenHashSet ?: BooleanOpenHashSet(this)


// =====================================================
// ByteSet Extensions
// =====================================================

inline fun mutableByteSetOf(vararg elements: Byte): ByteSet = ByteOpenHashSet(elements)
inline fun mutableByteSetOfNotNull(vararg elements: Byte?) =
    elements.filterNotNullTo(ByteOpenHashSet())

inline fun mutableByteSetOf(): ByteSet = ByteOpenHashSet()
inline fun mutableByteSetOf(capacity: Int): ByteSet = ByteOpenHashSet(capacity)
inline fun mutableByteSetOf(iterable: Iterable<Byte>): ByteSet = when (iterable) {
    is Collection -> ByteOpenHashSet(iterable)
    else -> ByteOpenHashSet(iterable.iterator())
}

inline fun byteSetOf(vararg elements: Byte): ByteSet = when (elements.size) {
    0 -> emptyByteSet()
    1 -> ByteSets.singleton(elements[0])
    else -> mutableByteSetOf(*elements).freeze()
}

inline fun byteSetOfNotNull(vararg elements: Byte?): ByteSet = when (elements.size) {
    0 -> emptyByteSet()
    1 -> elements[0]?.let { ByteSets.singleton(it) } ?: emptyByteSet()
    else -> mutableByteSetOfNotNull(*elements).freeze()
}

inline fun byteSetOf(iterable: Iterable<Byte>): ByteSet = mutableByteSetOf(iterable).freeze()
inline fun byteSetOf(): ByteSet = emptyByteSet()
inline fun emptyByteSet(): @Unmodifiable ByteSet = ByteSets.emptySet()
inline fun ByteSet.synchronize(): ByteSet = ByteSets.synchronize(this)
inline fun ByteSet.freeze(): @UnmodifiableView ByteSet =
    this as? ByteSets.UnmodifiableSet ?: ByteSets.unmodifiable(this)

inline fun Sequence<Byte>.toMutableByteSet(): ByteSet = ByteOpenHashSet(iterator())
inline fun Sequence<Byte>.toByteSet(): ByteSet {
    val it = iterator()
    if (!it.hasNext()) return emptyByteSet()
    val first = it.next()
    if (!it.hasNext()) return byteSetOf(first)
    val set = mutableByteSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Byte>.toByteSet() = byteSetOf(*toByteArray())
inline fun Array<Byte>.toMutableByteSet() = mutableByteSetOf(*toByteArray())
inline fun Iterable<Byte>.toByteSet() = (this as? ByteSet)?.freeze() ?: byteSetOf(this)
inline fun Iterable<Byte>.toMutableByteSet() =
    (this as? ByteSet)?.toMutableByteSet() ?: mutableByteSetOf(this)

inline fun Collection<Byte>.toByteSet() = (this as? ByteSet)?.freeze() ?: byteSetOf(this)
inline fun ByteSet.toMutableByteSet() = this as? ByteOpenHashSet ?: ByteOpenHashSet(this)


// =====================================================
// CharSet Extensions
// =====================================================

inline fun mutableCharSetOf(vararg elements: Char): CharSet = CharOpenHashSet(elements)
inline fun mutableCharSetOfNotNull(vararg elements: Char?) =
    elements.filterNotNullTo(CharOpenHashSet())

inline fun mutableCharSetOf(): CharSet = CharOpenHashSet()
inline fun mutableCharSetOf(capacity: Int): CharSet = CharOpenHashSet(capacity)
inline fun mutableCharSetOf(iterable: Iterable<Char>): CharSet = when (iterable) {
    is Collection -> CharOpenHashSet(iterable)
    else -> CharOpenHashSet(iterable.iterator())
}

inline fun charSetOf(vararg elements: Char): CharSet = when (elements.size) {
    0 -> emptyCharSet()
    1 -> CharSets.singleton(elements[0])
    else -> mutableCharSetOf(*elements).freeze()
}

inline fun charSetOfNotNull(vararg elements: Char?): CharSet = when (elements.size) {
    0 -> emptyCharSet()
    1 -> elements[0]?.let { CharSets.singleton(it) } ?: emptyCharSet()
    else -> mutableCharSetOfNotNull(*elements).freeze()
}

inline fun charSetOf(iterable: Iterable<Char>): CharSet = mutableCharSetOf(iterable).freeze()
inline fun charSetOf(): CharSet = emptyCharSet()
inline fun emptyCharSet(): @Unmodifiable CharSet = CharSets.emptySet()
inline fun CharSet.synchronize(): CharSet = CharSets.synchronize(this)
inline fun CharSet.freeze(): @UnmodifiableView CharSet =
    this as? CharSets.UnmodifiableSet ?: CharSets.unmodifiable(this)

inline fun Sequence<Char>.toMutableCharSet(): CharSet = CharOpenHashSet(iterator())
inline fun Sequence<Char>.toCharSet(): CharSet {
    val it = iterator()
    if (!it.hasNext()) return emptyCharSet()
    val first = it.next()
    if (!it.hasNext()) return charSetOf(first)
    val set = mutableCharSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Char>.toCharSet() = charSetOf(*toCharArray())
inline fun Array<Char>.toMutableCharSet() = mutableCharSetOf(*toCharArray())
inline fun Iterable<Char>.toCharSet() = (this as? CharSet)?.freeze() ?: charSetOf(this)
inline fun Iterable<Char>.toMutableCharSet() =
    (this as? CharSet)?.toMutableCharSet() ?: mutableCharSetOf(this)

inline fun Collection<Char>.toCharSet() = (this as? CharSet)?.freeze() ?: charSetOf(this)
inline fun CharSet.toMutableCharSet() = this as? CharOpenHashSet ?: CharOpenHashSet(this)


// =====================================================
// ShortSet Extensions
// =====================================================

inline fun mutableShortSetOf(vararg elements: Short): ShortSet = ShortOpenHashSet(elements)
inline fun mutableShortSetOfNotNull(vararg elements: Short?) =
    elements.filterNotNullTo(ShortOpenHashSet())

inline fun mutableShortSetOf(): ShortSet = ShortOpenHashSet()
inline fun mutableShortSetOf(capacity: Int): ShortSet = ShortOpenHashSet(capacity)
inline fun mutableShortSetOf(iterable: Iterable<Short>): ShortSet = when (iterable) {
    is Collection -> ShortOpenHashSet(iterable)
    else -> ShortOpenHashSet(iterable.iterator())
}

inline fun shortSetOf(vararg elements: Short): ShortSet = when (elements.size) {
    0 -> emptyShortSet()
    1 -> ShortSets.singleton(elements[0])
    else -> mutableShortSetOf(*elements).freeze()
}

inline fun shortSetOfNotNull(vararg elements: Short?): ShortSet = when (elements.size) {
    0 -> emptyShortSet()
    1 -> elements[0]?.let { ShortSets.singleton(it) } ?: emptyShortSet()
    else -> mutableShortSetOfNotNull(*elements).freeze()
}

inline fun shortSetOf(iterable: Iterable<Short>): ShortSet = mutableShortSetOf(iterable).freeze()
inline fun shortSetOf(): ShortSet = emptyShortSet()
inline fun emptyShortSet(): @Unmodifiable ShortSet = ShortSets.emptySet()
inline fun ShortSet.synchronize(): ShortSet = ShortSets.synchronize(this)
inline fun ShortSet.freeze(): @UnmodifiableView ShortSet =
    this as? ShortSets.UnmodifiableSet ?: ShortSets.unmodifiable(this)

inline fun Sequence<Short>.toMutableShortSet(): ShortSet = ShortOpenHashSet(iterator())
inline fun Sequence<Short>.toShortSet(): ShortSet {
    val it = iterator()
    if (!it.hasNext()) return emptyShortSet()
    val first = it.next()
    if (!it.hasNext()) return shortSetOf(first)
    val set = mutableShortSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Short>.toShortSet() = shortSetOf(*toShortArray())
inline fun Array<Short>.toMutableShortSet() = mutableShortSetOf(*toShortArray())
inline fun Iterable<Short>.toShortSet() = (this as? ShortSet)?.freeze() ?: shortSetOf(this)
inline fun Iterable<Short>.toMutableShortSet() =
    (this as? ShortSet)?.toMutableShortSet() ?: mutableShortSetOf(this)

inline fun Collection<Short>.toShortSet() = (this as? ShortSet)?.freeze() ?: shortSetOf(this)
inline fun ShortSet.toMutableShortSet() = this as? ShortOpenHashSet ?: ShortOpenHashSet(this)


// =====================================================
// IntSet Extensions
// =====================================================

inline fun mutableIntSetOf(vararg elements: Int): IntSet = IntOpenHashSet(elements)
inline fun mutableIntSetOfNotNull(vararg elements: Int?) =
    elements.filterNotNullTo(IntOpenHashSet())

inline fun mutableIntSetOf(): IntSet = IntOpenHashSet()
inline fun mutableIntSetOf(capacity: Int): IntSet = IntOpenHashSet(capacity)
inline fun mutableIntSetOf(iterable: Iterable<Int>): IntSet = when (iterable) {
    is Collection -> IntOpenHashSet(iterable)
    else -> IntOpenHashSet(iterable.iterator())
}

inline fun intSetOf(vararg elements: Int): IntSet = when (elements.size) {
    0 -> emptyIntSet()
    1 -> IntSets.singleton(elements[0])
    else -> mutableIntSetOf(*elements).freeze()
}

inline fun intSetOfNotNull(vararg elements: Int?): IntSet = when (elements.size) {
    0 -> emptyIntSet()
    1 -> elements[0]?.let { IntSets.singleton(it) } ?: emptyIntSet()
    else -> mutableIntSetOfNotNull(*elements).freeze()
}

inline fun intSetOf(iterable: Iterable<Int>): IntSet = mutableIntSetOf(iterable).freeze()
inline fun intSetOf(): IntSet = emptyIntSet()
inline fun emptyIntSet(): @Unmodifiable IntSet = IntSets.emptySet()
inline fun IntSet.synchronize(): IntSet = IntSets.synchronize(this)
inline fun IntSet.freeze(): @UnmodifiableView IntSet =
    this as? IntSets.UnmodifiableSet ?: IntSets.unmodifiable(this)

inline fun Sequence<Int>.toMutableIntSet(): IntSet = IntOpenHashSet(iterator())
inline fun Sequence<Int>.toIntSet(): IntSet {
    val it = iterator()
    if (!it.hasNext()) return emptyIntSet()
    val first = it.next()
    if (!it.hasNext()) return intSetOf(first)
    val set = mutableIntSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Int>.toIntSet() = intSetOf(*toIntArray())
inline fun Array<Int>.toMutableIntSet() = mutableIntSetOf(*toIntArray())
inline fun Iterable<Int>.toIntSet() = (this as? IntSet)?.freeze() ?: intSetOf(this)
inline fun Iterable<Int>.toMutableIntSet() =
    (this as? IntSet)?.toMutableIntSet() ?: mutableIntSetOf(this)

inline fun Collection<Int>.toIntSet() = (this as? IntSet)?.freeze() ?: intSetOf(this)
inline fun IntSet.toMutableIntSet() = this as? IntOpenHashSet ?: IntOpenHashSet(this)


// =====================================================
// LongSet Extensions
// =====================================================

inline fun mutableLongSetOf(vararg elements: Long): LongSet = LongOpenHashSet(elements)
inline fun mutableLongSetOfNotNull(vararg elements: Long?) =
    elements.filterNotNullTo(LongOpenHashSet())

inline fun mutableLongSetOf(): LongSet = LongOpenHashSet()
inline fun mutableLongSetOf(capacity: Int): LongSet = LongOpenHashSet(capacity)
inline fun mutableLongSetOf(iterable: Iterable<Long>): LongSet = when (iterable) {
    is Collection -> LongOpenHashSet(iterable)
    else -> LongOpenHashSet(iterable.iterator())
}

inline fun longSetOf(vararg elements: Long): LongSet = when (elements.size) {
    0 -> emptyLongSet()
    1 -> LongSets.singleton(elements[0])
    else -> mutableLongSetOf(*elements).freeze()
}

inline fun longSetOfNotNull(vararg elements: Long?): LongSet = when (elements.size) {
    0 -> emptyLongSet()
    1 -> elements[0]?.let { LongSets.singleton(it) } ?: emptyLongSet()
    else -> mutableLongSetOfNotNull(*elements).freeze()
}

inline fun longSetOf(iterable: Iterable<Long>): LongSet = mutableLongSetOf(iterable).freeze()
inline fun longSetOf(): LongSet = emptyLongSet()
inline fun emptyLongSet(): @Unmodifiable LongSet = LongSets.emptySet()
inline fun LongSet.synchronize(): LongSet = LongSets.synchronize(this)
inline fun LongSet.freeze(): @UnmodifiableView LongSet =
    this as? LongSets.UnmodifiableSet ?: LongSets.unmodifiable(this)

inline fun Sequence<Long>.toMutableLongSet(): LongSet = LongOpenHashSet(iterator())
inline fun Sequence<Long>.toLongSet(): LongSet {
    val it = iterator()
    if (!it.hasNext()) return emptyLongSet()
    val first = it.next()
    if (!it.hasNext()) return longSetOf(first)
    val set = mutableLongSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Long>.toLongSet() = longSetOf(*toLongArray())
inline fun Array<Long>.toMutableLongSet() = mutableLongSetOf(*toLongArray())
inline fun Iterable<Long>.toLongSet() = (this as? LongSet)?.freeze() ?: longSetOf(this)
inline fun Iterable<Long>.toMutableLongSet() =
    (this as? LongSet)?.toMutableLongSet() ?: mutableLongSetOf(this)

inline fun Collection<Long>.toLongSet() = (this as? LongSet)?.freeze() ?: longSetOf(this)
inline fun LongSet.toMutableLongSet() = this as? LongOpenHashSet ?: LongOpenHashSet(this)


// =====================================================
// FloatSet Extensions
// =====================================================

inline fun mutableFloatSetOf(vararg elements: Float): FloatSet = FloatOpenHashSet(elements)
inline fun mutableFloatSetOfNotNull(vararg elements: Float?) =
    elements.filterNotNullTo(FloatOpenHashSet())

inline fun mutableFloatSetOf(): FloatSet = FloatOpenHashSet()
inline fun mutableFloatSetOf(capacity: Int): FloatSet = FloatOpenHashSet(capacity)
inline fun mutableFloatSetOf(iterable: Iterable<Float>): FloatSet = when (iterable) {
    is Collection -> FloatOpenHashSet(iterable)
    else -> FloatOpenHashSet(iterable.iterator())
}

inline fun floatSetOf(vararg elements: Float): FloatSet = when (elements.size) {
    0 -> emptyFloatSet()
    1 -> FloatSets.singleton(elements[0])
    else -> mutableFloatSetOf(*elements).freeze()
}

inline fun floatSetOfNotNull(vararg elements: Float?): FloatSet = when (elements.size) {
    0 -> emptyFloatSet()
    1 -> elements[0]?.let { FloatSets.singleton(it) } ?: emptyFloatSet()
    else -> mutableFloatSetOfNotNull(*elements).freeze()
}

inline fun floatSetOf(iterable: Iterable<Float>): FloatSet = mutableFloatSetOf(iterable).freeze()
inline fun floatSetOf(): FloatSet = emptyFloatSet()
inline fun emptyFloatSet(): @Unmodifiable FloatSet = FloatSets.emptySet()
inline fun FloatSet.synchronize(): FloatSet = FloatSets.synchronize(this)
inline fun FloatSet.freeze(): @UnmodifiableView FloatSet =
    this as? FloatSets.UnmodifiableSet ?: FloatSets.unmodifiable(this)

inline fun Sequence<Float>.toMutableFloatSet(): FloatSet = FloatOpenHashSet(iterator())
inline fun Sequence<Float>.toFloatSet(): FloatSet {
    val it = iterator()
    if (!it.hasNext()) return emptyFloatSet()
    val first = it.next()
    if (!it.hasNext()) return floatSetOf(first)
    val set = mutableFloatSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Float>.toFloatSet() = floatSetOf(*toFloatArray())
inline fun Array<Float>.toMutableFloatSet() = mutableFloatSetOf(*toFloatArray())
inline fun Iterable<Float>.toFloatSet() = (this as? FloatSet)?.freeze() ?: floatSetOf(this)
inline fun Iterable<Float>.toMutableFloatSet() =
    (this as? FloatSet)?.toMutableFloatSet() ?: mutableFloatSetOf(this)

inline fun Collection<Float>.toFloatSet() = (this as? FloatSet)?.freeze() ?: floatSetOf(this)
inline fun FloatSet.toMutableFloatSet() = this as? FloatOpenHashSet ?: FloatOpenHashSet(this)


// =====================================================
// DoubleSet Extensions
// =====================================================

inline fun mutableDoubleSetOf(vararg elements: Double): DoubleSet = DoubleOpenHashSet(elements)
inline fun mutableDoubleSetOfNotNull(vararg elements: Double?) =
    elements.filterNotNullTo(DoubleOpenHashSet())

inline fun mutableDoubleSetOf(): DoubleSet = DoubleOpenHashSet()
inline fun mutableDoubleSetOf(capacity: Int): DoubleSet = DoubleOpenHashSet(capacity)
inline fun mutableDoubleSetOf(iterable: Iterable<Double>): DoubleSet = when (iterable) {
    is Collection -> DoubleOpenHashSet(iterable)
    else -> DoubleOpenHashSet(iterable.iterator())
}

inline fun doubleSetOf(vararg elements: Double): DoubleSet = when (elements.size) {
    0 -> emptyDoubleSet()
    1 -> DoubleSets.singleton(elements[0])
    else -> mutableDoubleSetOf(*elements).freeze()
}

inline fun doubleSetOfNotNull(vararg elements: Double?): DoubleSet = when (elements.size) {
    0 -> emptyDoubleSet()
    1 -> elements[0]?.let { DoubleSets.singleton(it) } ?: emptyDoubleSet()
    else -> mutableDoubleSetOfNotNull(*elements).freeze()
}

inline fun doubleSetOf(iterable: Iterable<Double>): DoubleSet =
    mutableDoubleSetOf(iterable).freeze()

inline fun doubleSetOf(): DoubleSet = emptyDoubleSet()
inline fun emptyDoubleSet(): @Unmodifiable DoubleSet = DoubleSets.emptySet()
inline fun DoubleSet.synchronize(): DoubleSet = DoubleSets.synchronize(this)
inline fun DoubleSet.freeze(): @UnmodifiableView DoubleSet =
    this as? DoubleSets.UnmodifiableSet ?: DoubleSets.unmodifiable(this)

inline fun Sequence<Double>.toMutableDoubleSet(): DoubleSet = DoubleOpenHashSet(iterator())
inline fun Sequence<Double>.toDoubleSet(): DoubleSet {
    val it = iterator()
    if (!it.hasNext()) return emptyDoubleSet()
    val first = it.next()
    if (!it.hasNext()) return doubleSetOf(first)
    val set = mutableDoubleSetOf()
    set.add(first)
    while (it.hasNext()) set.add(it.next())
    return set.freeze()
}

inline fun Array<Double>.toDoubleSet() = doubleSetOf(*toDoubleArray())
inline fun Array<Double>.toMutableDoubleSet() = mutableDoubleSetOf(*toDoubleArray())
inline fun Iterable<Double>.toDoubleSet() = (this as? DoubleSet)?.freeze() ?: doubleSetOf(this)
inline fun Iterable<Double>.toMutableDoubleSet() =
    (this as? DoubleSet)?.toMutableDoubleSet() ?: mutableDoubleSetOf(this)

inline fun Collection<Double>.toDoubleSet() = (this as? DoubleSet)?.freeze() ?: doubleSetOf(this)
inline fun DoubleSet.toMutableDoubleSet() = this as? DoubleOpenHashSet ?: DoubleOpenHashSet(this)
// endregion

// region List
// =====================================================
// ObjectList Extensions
// =====================================================

inline fun <T> mutableObjectListOf(vararg elements: T) = ObjectArrayList<T>(elements)
inline fun <T> mutableObjectListOf() = ObjectArrayList<T>()
inline fun <T> mutableObjectListOf(capacity: Int) = ObjectArrayList<T>(capacity)
inline fun <T> mutableObjectListOf(iterable: Iterable<T>) = when (iterable) {
    is Collection -> ObjectArrayList(iterable)
    else -> ObjectArrayList<T>(iterable.iterator())
}

inline fun <T> mutableObjectListOfNotNull(vararg elements: T?) =
    elements.filterNotNullTo(ObjectArrayList())

inline fun <T> mutableObjectListOfNotNull(iterable: Iterable<T?>) =
    iterable.filterNotNullTo(ObjectArrayList())

inline fun <T> objectListOf(vararg elements: T): @Unmodifiable ObjectList<T> =
    ObjectList.of(*elements)

inline fun <T> objectListOf() = emptyObjectList<T>()
inline fun <T> objectListOfNotNull(element: T?): @Unmodifiable ObjectList<T> =
    if (element != null) ObjectList.of(element) else emptyObjectList()

inline fun <T> objectListOfNotNull(vararg elements: T?): @Unmodifiable ObjectList<T> =
    elements.filterNotNullTo(ObjectArrayList()).freeze()

inline fun <T> objectListOfNotNull(iterable: Iterable<T?>): @Unmodifiable ObjectList<T> =
    iterable.filterNotNullTo(ObjectArrayList()).freeze()

inline fun <T> emptyObjectList(): @Unmodifiable ObjectList<T> = ObjectList.of()
inline fun <T> ObjectList<T>.synchronize(): ObjectList<T> = ObjectLists.synchronize(this)
inline fun <T> ObjectList<T>.freeze(): @UnmodifiableView ObjectList<T> =
    ObjectLists.unmodifiable(this)

inline fun <T> Sequence<T>.toMutableObjectList() = ObjectArrayList<T>(iterator())
inline fun <T> Sequence<T>.toObjectList() = toMutableObjectList().freeze()
inline fun <T> Iterable<T>.toObjectList() =
    this as? ObjectList<T> ?: ObjectArrayList<T>(iterator()).freeze()
inline fun <T> Iterable<T>.toMutableObjectList() =
    this as? ObjectArrayList<T> ?: ObjectArrayList<T>(iterator())

inline fun <T> Array<out T>.toObjectList(): @Unmodifiable ObjectList<T> = ObjectList.of(*this)
inline fun <T> Array<out T>.toMutableObjectList() = ObjectArrayList(this)


// =====================================================
// BooleanList Extensions
// =====================================================

inline fun mutableBooleanListOf(vararg elements: Boolean) = BooleanArrayList(elements)
inline fun mutableBooleanListOf() = BooleanArrayList()
inline fun mutableBooleanListOf(capacity: Int) = BooleanArrayList(capacity)
inline fun mutableBooleanListOf(iterable: Iterable<Boolean>) = when (iterable) {
    is Collection -> BooleanArrayList(iterable)
    else -> BooleanArrayList(iterable.iterator())
}

inline fun mutableBooleanListOfNotNull(vararg elements: Boolean?) =
    elements.filterNotNullTo(BooleanArrayList())

inline fun mutableBooleanListOfNotNull(iterable: Iterable<Boolean?>) =
    iterable.filterNotNullTo(BooleanArrayList())

inline fun booleanListOf(vararg elements: Boolean): @Unmodifiable BooleanList =
    BooleanList.of(*elements)

inline fun booleanListOf() = emptyBooleanList()
inline fun booleanListOfNotNull(element: Boolean?): @Unmodifiable BooleanList =
    if (element != null) BooleanList.of(element) else emptyBooleanList()

inline fun booleanListOfNotNull(vararg elements: Boolean?): @Unmodifiable BooleanList =
    elements.filterNotNullTo(BooleanArrayList()).freeze()

inline fun booleanListOfNotNull(iterable: Iterable<Boolean?>): @Unmodifiable BooleanList =
    iterable.filterNotNullTo(BooleanArrayList()).freeze()

inline fun emptyBooleanList(): @Unmodifiable BooleanList = BooleanList.of()
inline fun BooleanList.synchronize(): BooleanList = BooleanLists.synchronize(this)
inline fun BooleanList.freeze(): @UnmodifiableView BooleanList = BooleanLists.unmodifiable(this)
inline fun Sequence<Boolean>.toMutableBooleanList() = BooleanArrayList(iterator())
inline fun Sequence<Boolean>.toBooleanList() = toMutableBooleanList().freeze()
inline fun Iterable<Boolean>.toBooleanList() =
    this as? BooleanList ?: BooleanArrayList(iterator()).freeze()

inline fun Array<Boolean>.toBooleanList(): @Unmodifiable BooleanList =
    BooleanList.of(*toBooleanArray())

inline fun Array<Boolean>.toMutableBooleanList() = BooleanArrayList(toBooleanArray())
inline fun BooleanArray.toBooleanList(): @Unmodifiable BooleanList = BooleanList.of(*this)
inline fun BooleanArray.toMutableBooleanList() = BooleanArrayList(this)


// =====================================================
// ByteList Extensions
// =====================================================

inline fun mutableByteListOf(vararg elements: Byte) = ByteArrayList(elements)
inline fun mutableByteListOf() = ByteArrayList()
inline fun mutableByteListOf(capacity: Int) = ByteArrayList(capacity)
inline fun mutableByteListOf(iterable: Iterable<Byte>) = when (iterable) {
    is Collection -> ByteArrayList(iterable)
    else -> ByteArrayList(iterable.iterator())
}

inline fun mutableByteListOfNotNull(vararg elements: Byte?) =
    elements.filterNotNullTo(ByteArrayList())

inline fun mutableByteListOfNotNull(iterable: Iterable<Byte?>) =
    iterable.filterNotNullTo(ByteArrayList())

inline fun byteListOf(vararg elements: Byte): @Unmodifiable ByteList = ByteList.of(*elements)
inline fun byteListOf() = emptyByteList()
inline fun byteListOfNotNull(element: Byte?): @Unmodifiable ByteList =
    if (element != null) ByteList.of(element) else emptyByteList()

inline fun byteListOfNotNull(vararg elements: Byte?): @Unmodifiable ByteList =
    elements.filterNotNullTo(ByteArrayList()).freeze()

inline fun byteListOfNotNull(iterable: Iterable<Byte?>): @Unmodifiable ByteList =
    iterable.filterNotNullTo(ByteArrayList()).freeze()

inline fun emptyByteList(): @Unmodifiable ByteList = ByteList.of()
inline fun ByteList.synchronize(): ByteList = ByteLists.synchronize(this)
inline fun ByteList.freeze(): @UnmodifiableView ByteList = ByteLists.unmodifiable(this)
inline fun Sequence<Byte>.toMutableByteList() = ByteArrayList(iterator())
inline fun Sequence<Byte>.toByteList() = toMutableByteList().freeze()
inline fun Iterable<Byte>.toByteList() =
    this as? ByteList ?: ByteArrayList(iterator()).freeze()

inline fun Array<Byte>.toByteList(): @Unmodifiable ByteList = ByteList.of(*toByteArray())
inline fun Array<Byte>.toMutableByteList() = ByteArrayList(toByteArray())
inline fun ByteArray.toByteList(): @Unmodifiable ByteList = ByteList.of(*this)
inline fun ByteArray.toMutableByteList() = ByteArrayList(this)


// =====================================================
// CharList Extensions
// =====================================================

inline fun mutableCharListOf(vararg elements: Char) = CharArrayList(elements)
inline fun mutableCharListOf() = CharArrayList()
inline fun mutableCharListOf(capacity: Int) = CharArrayList(capacity)
inline fun mutableCharListOf(iterable: Iterable<Char>) = when (iterable) {
    is Collection -> CharArrayList(iterable)
    else -> CharArrayList(iterable.iterator())
}

inline fun mutableCharListOfNotNull(vararg elements: Char?) =
    elements.filterNotNullTo(CharArrayList())

inline fun mutableCharListOfNotNull(iterable: Iterable<Char?>) =
    iterable.filterNotNullTo(CharArrayList())

inline fun charListOf(vararg elements: Char): @Unmodifiable CharList = CharList.of(*elements)
inline fun charListOf() = emptyCharList()
inline fun charListOfNotNull(element: Char?): @Unmodifiable CharList =
    if (element != null) CharList.of(element) else emptyCharList()

inline fun charListOfNotNull(vararg elements: Char?): @Unmodifiable CharList =
    elements.filterNotNullTo(CharArrayList()).freeze()

inline fun charListOfNotNull(iterable: Iterable<Char?>): @Unmodifiable CharList =
    iterable.filterNotNullTo(CharArrayList()).freeze()

inline fun emptyCharList(): @Unmodifiable CharList = CharList.of()
inline fun CharList.synchronize(): CharList = CharLists.synchronize(this)
inline fun CharList.freeze(): @UnmodifiableView CharList = CharLists.unmodifiable(this)
inline fun Sequence<Char>.toMutableCharList() = CharArrayList(iterator())
inline fun Sequence<Char>.toCharList() = toMutableCharList().freeze()
inline fun Iterable<Char>.toCharList() =
    this as? CharList ?: CharArrayList(iterator()).freeze()

inline fun Array<Char>.toCharList(): @Unmodifiable CharList = CharList.of(*toCharArray())
inline fun Array<Char>.toMutableCharList() = CharArrayList(toCharArray())
inline fun CharArray.toCharList(): @Unmodifiable CharList = CharList.of(*this)
inline fun CharArray.toMutableCharList() = CharArrayList(this)


// =====================================================
// ShortList Extensions
// =====================================================

inline fun mutableShortListOf(vararg elements: Short) = ShortArrayList(elements)
inline fun mutableShortListOf() = ShortArrayList()
inline fun mutableShortListOf(capacity: Int) = ShortArrayList(capacity)
inline fun mutableShortListOf(iterable: Iterable<Short>) = when (iterable) {
    is Collection -> ShortArrayList(iterable)
    else -> ShortArrayList(iterable.iterator())
}

inline fun mutableShortListOfNotNull(vararg elements: Short?) =
    elements.filterNotNullTo(ShortArrayList())

inline fun mutableShortListOfNotNull(iterable: Iterable<Short?>) =
    iterable.filterNotNullTo(ShortArrayList())

inline fun shortListOf(vararg elements: Short): @Unmodifiable ShortList = ShortList.of(*elements)
inline fun shortListOf() = emptyShortList()
inline fun shortListOfNotNull(element: Short?): @Unmodifiable ShortList =
    if (element != null) ShortList.of(element) else emptyShortList()

inline fun shortListOfNotNull(vararg elements: Short?): @Unmodifiable ShortList =
    elements.filterNotNullTo(ShortArrayList()).freeze()

inline fun shortListOfNotNull(iterable: Iterable<Short?>): @Unmodifiable ShortList =
    iterable.filterNotNullTo(ShortArrayList()).freeze()

inline fun emptyShortList(): @Unmodifiable ShortList = ShortList.of()
inline fun ShortList.synchronize(): ShortList = ShortLists.synchronize(this)
inline fun ShortList.freeze(): @UnmodifiableView ShortList = ShortLists.unmodifiable(this)
inline fun Sequence<Short>.toMutableShortList() = ShortArrayList(iterator())
inline fun Sequence<Short>.toShortList() = toMutableShortList().freeze()
inline fun Iterable<Short>.toShortList() =
    this as? ShortList ?: ShortArrayList(iterator()).freeze()

inline fun Array<Short>.toShortList(): @Unmodifiable ShortList = ShortList.of(*toShortArray())
inline fun Array<Short>.toMutableShortList() = ShortArrayList(toShortArray())
inline fun ShortArray.toShortList(): @Unmodifiable ShortList = ShortList.of(*this)
inline fun ShortArray.toMutableShortList() = ShortArrayList(this)


// =====================================================
// IntList Extensions
// =====================================================

inline fun mutableIntListOf(vararg elements: Int) = IntArrayList(elements)
inline fun mutableIntListOf() = IntArrayList()
inline fun mutableIntListOf(capacity: Int) = IntArrayList(capacity)
inline fun mutableIntListOf(iterable: Iterable<Int>) = when (iterable) {
    is Collection -> IntArrayList(iterable)
    else -> IntArrayList(iterable.iterator())
}

inline fun mutableIntListOfNotNull(vararg elements: Int?) = elements.filterNotNullTo(IntArrayList())
inline fun mutableIntListOfNotNull(iterable: Iterable<Int?>) =
    iterable.filterNotNullTo(IntArrayList())

inline fun intListOf(vararg elements: Int): @Unmodifiable IntList = IntList.of(*elements)
inline fun intListOf() = emptyIntList()
inline fun intListOfNotNull(element: Int?): @Unmodifiable IntList =
    if (element != null) IntList.of(element) else emptyIntList()

inline fun intListOfNotNull(vararg elements: Int?): @Unmodifiable IntList =
    elements.filterNotNullTo(IntArrayList()).freeze()

inline fun intListOfNotNull(iterable: Iterable<Int?>): @Unmodifiable IntList =
    iterable.filterNotNullTo(IntArrayList()).freeze()

inline fun emptyIntList(): @Unmodifiable IntList = IntList.of()
inline fun IntList.synchronize(): IntList = IntLists.synchronize(this)
inline fun IntList.freeze(): @UnmodifiableView IntList = IntLists.unmodifiable(this)
inline fun Sequence<Int>.toMutableIntList() = IntArrayList(iterator())
inline fun Sequence<Int>.toIntList() = toMutableIntList().freeze()
inline fun Iterable<Int>.toIntList() =
    this as? IntList ?: IntArrayList(iterator()).freeze()

inline fun Array<Int>.toIntList(): @Unmodifiable IntList = IntList.of(*toIntArray())
inline fun Array<Int>.toMutableIntList() = IntArrayList(toIntArray())
inline fun IntArray.toIntList(): @Unmodifiable IntList = IntList.of(*this)
inline fun IntArray.toMutableIntList() = IntArrayList(this)


// =====================================================
// LongList Extensions
// =====================================================

inline fun mutableLongListOf(vararg elements: Long) = LongArrayList(elements)
inline fun mutableLongListOf() = LongArrayList()
inline fun mutableLongListOf(capacity: Int) = LongArrayList(capacity)
inline fun mutableLongListOf(iterable: Iterable<Long>) = when (iterable) {
    is Collection -> LongArrayList(iterable)
    else -> LongArrayList(iterable.iterator())
}

inline fun mutableLongListOfNotNull(vararg elements: Long?) =
    elements.filterNotNullTo(LongArrayList())

inline fun mutableLongListOfNotNull(iterable: Iterable<Long?>) =
    iterable.filterNotNullTo(LongArrayList())

inline fun longListOf(vararg elements: Long): @Unmodifiable LongList = LongList.of(*elements)
inline fun longListOf() = emptyLongList()
inline fun longListOfNotNull(element: Long?): @Unmodifiable LongList =
    if (element != null) LongList.of(element) else emptyLongList()

inline fun longListOfNotNull(vararg elements: Long?): @Unmodifiable LongList =
    elements.filterNotNullTo(LongArrayList()).freeze()

inline fun longListOfNotNull(iterable: Iterable<Long?>): @Unmodifiable LongList =
    iterable.filterNotNullTo(LongArrayList()).freeze()

inline fun emptyLongList(): @Unmodifiable LongList = LongList.of()
inline fun LongList.synchronize(): LongList = LongLists.synchronize(this)
inline fun LongList.freeze(): @UnmodifiableView LongList = LongLists.unmodifiable(this)
inline fun Sequence<Long>.toMutableLongList() = LongArrayList(iterator())
inline fun Sequence<Long>.toLongList() = toMutableLongList().freeze()
inline fun Iterable<Long>.toLongList() =
    this as? LongList ?: LongArrayList(iterator()).freeze()

inline fun Array<Long>.toLongList(): @Unmodifiable LongList = LongList.of(*toLongArray())
inline fun Array<Long>.toMutableLongList() = LongArrayList(toLongArray())
inline fun LongArray.toLongList(): @Unmodifiable LongList = LongList.of(*this)
inline fun LongArray.toMutableLongList() = LongArrayList(this)


// =====================================================
// FloatList Extensions
// =====================================================

inline fun mutableFloatListOf(vararg elements: Float) = FloatArrayList(elements)
inline fun mutableFloatListOf() = FloatArrayList()
inline fun mutableFloatListOf(capacity: Int) = FloatArrayList(capacity)
inline fun mutableFloatListOf(iterable: Iterable<Float>) = when (iterable) {
    is Collection -> FloatArrayList(iterable)
    else -> FloatArrayList(iterable.iterator())
}

inline fun mutableFloatListOfNotNull(vararg elements: Float?) =
    elements.filterNotNullTo(FloatArrayList())

inline fun mutableFloatListOfNotNull(iterable: Iterable<Float?>) =
    iterable.filterNotNullTo(FloatArrayList())

inline fun floatListOf(vararg elements: Float): @Unmodifiable FloatList = FloatList.of(*elements)
inline fun floatListOf() = emptyFloatList()
inline fun floatListOfNotNull(element: Float?): @Unmodifiable FloatList =
    if (element != null) FloatList.of(element) else emptyFloatList()

inline fun floatListOfNotNull(vararg elements: Float?): @Unmodifiable FloatList =
    elements.filterNotNullTo(FloatArrayList()).freeze()

inline fun floatListOfNotNull(iterable: Iterable<Float?>): @Unmodifiable FloatList =
    iterable.filterNotNullTo(FloatArrayList()).freeze()

inline fun emptyFloatList(): @Unmodifiable FloatList = FloatList.of()
inline fun FloatList.synchronize(): FloatList = FloatLists.synchronize(this)
inline fun FloatList.freeze(): @UnmodifiableView FloatList = FloatLists.unmodifiable(this)
inline fun Sequence<Float>.toMutableFloatList() = FloatArrayList(iterator())
inline fun Sequence<Float>.toFloatList() = toMutableFloatList().freeze()
inline fun Iterable<Float>.toFloatList() =
    this as? FloatList ?: FloatArrayList(iterator()).freeze()

inline fun Array<Float>.toFloatList(): @Unmodifiable FloatList = FloatList.of(*toFloatArray())
inline fun Array<Float>.toMutableFloatList() = FloatArrayList(toFloatArray())
inline fun FloatArray.toFloatList(): @Unmodifiable FloatList = FloatList.of(*this)
inline fun FloatArray.toMutableFloatList() = FloatArrayList(this)


// =====================================================
// DoubleList Extensions
// =====================================================

inline fun mutableDoubleListOf(vararg elements: Double) = DoubleArrayList(elements)
inline fun mutableDoubleListOf() = DoubleArrayList()
inline fun mutableDoubleListOf(capacity: Int) = DoubleArrayList(capacity)
inline fun mutableDoubleListOf(iterable: Iterable<Double>) = when (iterable) {
    is Collection -> DoubleArrayList(iterable)
    else -> DoubleArrayList(iterable.iterator())
}

inline fun mutableDoubleListOfNotNull(vararg elements: Double?) =
    elements.filterNotNullTo(DoubleArrayList())

inline fun mutableDoubleListOfNotNull(iterable: Iterable<Double?>) =
    iterable.filterNotNullTo(DoubleArrayList())

inline fun doubleListOf(vararg elements: Double): @Unmodifiable DoubleList =
    DoubleList.of(*elements)

inline fun doubleListOf() = emptyDoubleList()
inline fun doubleListOfNotNull(element: Double?): @Unmodifiable DoubleList =
    if (element != null) DoubleList.of(element) else emptyDoubleList()

inline fun doubleListOfNotNull(vararg elements: Double?): @Unmodifiable DoubleList =
    elements.filterNotNullTo(DoubleArrayList()).freeze()

inline fun doubleListOfNotNull(iterable: Iterable<Double?>): @Unmodifiable DoubleList =
    iterable.filterNotNullTo(DoubleArrayList()).freeze()

inline fun emptyDoubleList(): @Unmodifiable DoubleList = DoubleList.of()
inline fun DoubleList.synchronize(): DoubleList = DoubleLists.synchronize(this)
inline fun DoubleList.freeze(): @UnmodifiableView DoubleList = DoubleLists.unmodifiable(this)
inline fun Sequence<Double>.toMutableDoubleList() = DoubleArrayList(iterator())
inline fun Sequence<Double>.toDoubleList() = toMutableDoubleList().freeze()
inline fun Iterable<Double>.toDoubleList() =
    this as? DoubleList ?: DoubleArrayList(iterator()).freeze()

inline fun Array<Double>.toDoubleList(): @Unmodifiable DoubleList = DoubleList.of(*toDoubleArray())
inline fun Array<Double>.toMutableDoubleList() = DoubleArrayList(toDoubleArray())
// endregion

// region Map
// region ObjectMap
inline fun <K, V> mutableObject2ObjectMapOf(vararg pairs: Pair<K, V>) =
    Object2ObjectOpenHashMap<K, V>(pairs.size).apply { putAll(pairs) }

inline fun <K, V> mutableObject2ObjectMapOf() = Object2ObjectOpenHashMap<K, V>()
inline fun <K, V> mutableObject2ObjectMapOf(capacity: Int) =
    Object2ObjectOpenHashMap<K, V>(capacity)

inline fun <K, V> object2ObjectMapOf(vararg pairs: Pair<K, V>) =
    mutableObject2ObjectMapOf(*pairs).freeze()

inline fun <K, V> object2ObjectMapOf() = emptyObject2ObjectMap<K, V>()
inline fun <K, V> emptyObject2ObjectMap(): @Unmodifiable Object2ObjectMap<K, V> =
    Object2ObjectMaps.emptyMap()

inline fun <K, V> Object2ObjectMap<K, V>.synchronize(): Object2ObjectMap<K, V> =
    Object2ObjectMaps.synchronize(this)

inline fun <K, V> Object2ObjectMap<K, V>.freeze(): @UnmodifiableView Object2ObjectMap<K, V> =
    Object2ObjectMaps.unmodifiable(this)

inline fun <K, V> mutableObject2MultiObjectsMapOf(vararg pairs: Pair<K, ObjectSet<V>>) =
    Object2ObjectOpenHashMap<K, ObjectSet<V>>(pairs.size).apply { putAll(pairs) }

inline fun <K, V> mutableObject2MultiObjectsMapOf() = Object2ObjectOpenHashMap<K, ObjectSet<V>>()
inline fun <K, V> object2MultiObjectsMapOf(vararg pairs: Pair<K, ObjectSet<V>>) =
    mutableObject2MultiObjectsMapOf(*pairs).freeze()

inline fun <K, V> object2MultiObjectsMapOf() = emptyObject2MultiObjectsMap<K, V>()
inline fun <K, V> emptyObject2MultiObjectsMap(): @Unmodifiable Object2MultiObjectsMap<K, V> =
    Object2ObjectMaps.emptyMap()
typealias Object2MultiObjectsMap<K, V> = Object2ObjectMap<K, ObjectSet<V>>

inline fun <K, V> Object2MultiObjectsMap<K, V>.add(key: K, value: V) {
    val set = get(key) ?: mutableObjectSetOf<V>().also { put(key, it) }
    set.add(value)
}

inline fun <K, V> Object2MultiObjectsMap<K, V>.addAll(key: K, values: Iterable<V>) {
    val set = get(key) ?: mutableObjectSetOf<V>().also { put(key, it) }
    set.addAll(values)
}

inline fun <K, V> Object2MultiObjectsMap<K, V>.remove(key: K, value: V) {
    val set = get(key) ?: return
    set.remove(value)
    if (set.isEmpty()) remove(key)
}

inline fun <K, V> Object2MultiObjectsMap<K, V>.removeAll(key: K, values: Iterable<V>) {
    val set = get(key) ?: return
    set.removeAll(values)
    if (set.isEmpty()) remove(key)
}

inline fun <K, V> Object2MultiObjectsMap<K, V>.removeAll(key: K) {
    remove(key)
}

inline fun <K, V> Object2ObjectMap<out K, V>.toMutableObjectList(): ObjectList<Pair<K, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K, V> Object2ObjectMap<out K, V>.toObjectList(): ObjectList<Pair<K, V>> {
    return toMutableObjectList().freeze()
}


// =====================================================
// Object2BooleanMap Extensions
// =====================================================

inline fun <K> mutableObject2BooleanMapOf(vararg pairs: Pair<K, Boolean>) =
    Object2BooleanOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2BooleanMapOf() = Object2BooleanOpenHashMap<K>()
inline fun <K> mutableObject2BooleanMapOf(capacity: Int) = Object2BooleanOpenHashMap<K>(capacity)
inline fun <K> object2BooleanMapOf(vararg pairs: Pair<K, Boolean>) =
    mutableObject2BooleanMapOf(*pairs).freeze()

inline fun <K> object2BooleanMapOf() = emptyObject2BooleanMap<K>()
inline fun <K> emptyObject2BooleanMap(): @Unmodifiable Object2BooleanMap<K> =
    Object2BooleanMaps.emptyMap()

inline fun <K> Object2BooleanMap<K>.synchronize(): Object2BooleanMap<K> =
    Object2BooleanMaps.synchronize(this)

inline fun <K> Object2BooleanMap<K>.freeze(): @UnmodifiableView Object2BooleanMap<K> =
    Object2BooleanMaps.unmodifiable(this)

inline fun <K> Object2BooleanMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2BooleanMap<out K>.toObjectList(): ObjectList<Pair<K, Boolean>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2ByteMap Extensions
// =====================================================

inline fun <K> mutableObject2ByteMapOf(vararg pairs: Pair<K, Byte>) =
    Object2ByteOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2ByteMapOf() = Object2ByteOpenHashMap<K>()
inline fun <K> mutableObject2ByteMapOf(capacity: Int) = Object2ByteOpenHashMap<K>(capacity)
inline fun <K> object2ByteMapOf(vararg pairs: Pair<K, Byte>) =
    mutableObject2ByteMapOf(*pairs).freeze()

inline fun <K> object2ByteMapOf() = emptyObject2ByteMap<K>()
inline fun <K> emptyObject2ByteMap(): @Unmodifiable Object2ByteMap<K> =
    Object2ByteMaps.emptyMap()

inline fun <K> Object2ByteMap<K>.synchronize(): Object2ByteMap<K> =
    Object2ByteMaps.synchronize(this)

inline fun <K> Object2ByteMap<K>.freeze(): @UnmodifiableView Object2ByteMap<K> =
    Object2ByteMaps.unmodifiable(this)

inline fun <K> Object2ByteMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2ByteMap<out K>.toObjectList(): ObjectList<Pair<K, Byte>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2CharMap Extensions
// =====================================================
inline fun <K> mutableObject2CharMapOf(vararg pairs: Pair<K, Char>) =
    Object2CharOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2CharMapOf() = Object2CharOpenHashMap<K>()
inline fun <K> mutableObject2CharMapOf(capacity: Int) = Object2CharOpenHashMap<K>(capacity)
inline fun <K> object2CharMapOf(vararg pairs: Pair<K, Char>) =
    mutableObject2CharMapOf(*pairs).freeze()

inline fun <K> object2CharMapOf() = emptyObject2CharMap<K>()
inline fun <K> emptyObject2CharMap(): @Unmodifiable Object2CharMap<K> =
    Object2CharMaps.emptyMap()

inline fun <K> Object2CharMap<K>.synchronize(): Object2CharMap<K> =
    Object2CharMaps.synchronize(this)

inline fun <K> Object2CharMap<K>.freeze(): @UnmodifiableView Object2CharMap<K> =
    Object2CharMaps.unmodifiable(this)

inline fun <K> Object2CharMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2CharMap<out K>.toObjectList(): ObjectList<Pair<K, Char>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2ShortMap Extensions
// =====================================================

inline fun <K> mutableObject2ShortMapOf(vararg pairs: Pair<K, Short>) =
    Object2ShortOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2ShortMapOf() = Object2ShortOpenHashMap<K>()
inline fun <K> mutableObject2ShortMapOf(capacity: Int) = Object2ShortOpenHashMap<K>(capacity)
inline fun <K> object2ShortMapOf(vararg pairs: Pair<K, Short>) =
    mutableObject2ShortMapOf(*pairs).freeze()

inline fun <K> object2ShortMapOf() = emptyObject2ShortMap<K>()
inline fun <K> emptyObject2ShortMap(): @Unmodifiable Object2ShortMap<K> =
    Object2ShortMaps.emptyMap()

inline fun <K> Object2ShortMap<K>.synchronize(): Object2ShortMap<K> =
    Object2ShortMaps.synchronize(this)

inline fun <K> Object2ShortMap<K>.freeze(): @UnmodifiableView Object2ShortMap<K> =
    Object2ShortMaps.unmodifiable(this)

inline fun <K> Object2ShortMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2ShortMap<out K>.toObjectList(): ObjectList<Pair<K, Short>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2IntMap Extensions
// =====================================================
inline fun <K> mutableObject2IntMapOf(vararg pairs: Pair<K, Int>) =
    Object2IntOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2IntMapOf() = Object2IntOpenHashMap<K>()
inline fun <K> mutableObject2IntMapOf(capacity: Int) = Object2IntOpenHashMap<K>(capacity)
inline fun <K> object2IntMapOf(vararg pairs: Pair<K, Int>) = mutableObject2IntMapOf(*pairs).freeze()
inline fun <K> object2IntMapOf() = emptyObject2IntMap<K>()
inline fun <K> emptyObject2IntMap(): @Unmodifiable Object2IntMap<K> =
    Object2IntMaps.emptyMap()

inline fun <K> Object2IntMap<K>.synchronize(): Object2IntMap<K> =
    Object2IntMaps.synchronize(this)

inline fun <K> Object2IntMap<K>.freeze(): @UnmodifiableView Object2IntMap<K> =
    Object2IntMaps.unmodifiable(this)

inline fun <K> Object2IntMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2IntMap<out K>.toObjectList(): ObjectList<Pair<K, Int>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2LongMap Extensions
// =====================================================

inline fun <K> mutableObject2LongMapOf(vararg pairs: Pair<K, Long>) =
    Object2LongOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2LongMapOf() = Object2LongOpenHashMap<K>()
inline fun <K> mutableObject2LongMapOf(capacity: Int) = Object2LongOpenHashMap<K>(capacity)
inline fun <K> object2LongMapOf(vararg pairs: Pair<K, Long>) =
    mutableObject2LongMapOf(*pairs).freeze()

inline fun <K> object2LongMapOf() = emptyObject2LongMap<K>()
inline fun <K> emptyObject2LongMap(): @Unmodifiable Object2LongMap<K> =
    Object2LongMaps.emptyMap()

inline fun <K> Object2LongMap<K>.synchronize(): Object2LongMap<K> =
    Object2LongMaps.synchronize(this)

inline fun <K> Object2LongMap<K>.freeze(): @UnmodifiableView Object2LongMap<K> =
    Object2LongMaps.unmodifiable(this)

inline fun <K> Object2LongMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2LongMap<out K>.toObjectList(): ObjectList<Pair<K, Long>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2FloatMap Extensions
// =====================================================

inline fun <K> mutableObject2FloatMapOf(vararg pairs: Pair<K, Float>) =
    Object2FloatOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2FloatMapOf() = Object2FloatOpenHashMap<K>()
inline fun <K> mutableObject2FloatMapOf(capacity: Int) = Object2FloatOpenHashMap<K>(capacity)
inline fun <K> object2FloatMapOf(vararg pairs: Pair<K, Float>) =
    mutableObject2FloatMapOf(*pairs).freeze()

inline fun <K> object2FloatMapOf() = emptyObject2FloatMap<K>()
inline fun <K> emptyObject2FloatMap(): @Unmodifiable Object2FloatMap<K> =
    Object2FloatMaps.emptyMap()

inline fun <K> Object2FloatMap<K>.synchronize(): Object2FloatMap<K> =
    Object2FloatMaps.synchronize(this)

inline fun <K> Object2FloatMap<K>.freeze(): @UnmodifiableView Object2FloatMap<K> =
    Object2FloatMaps.unmodifiable(this)

inline fun <K> Object2FloatMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2FloatMap<out K>.toObjectList(): ObjectList<Pair<K, Float>> =
    toMutableObjectList().freeze()


// =====================================================
// Object2DoubleMap Extensions
// =====================================================

inline fun <K> mutableObject2DoubleMapOf(vararg pairs: Pair<K, Double>) =
    Object2DoubleOpenHashMap<K>(pairs.size).apply { putAll(pairs) }

inline fun <K> mutableObject2DoubleMapOf() = Object2DoubleOpenHashMap<K>()
inline fun <K> mutableObject2DoubleMapOf(capacity: Int) = Object2DoubleOpenHashMap<K>(capacity)
inline fun <K> object2DoubleMapOf(vararg pairs: Pair<K, Double>) =
    mutableObject2DoubleMapOf(*pairs).freeze()

inline fun <K> object2DoubleMapOf() = emptyObject2DoubleMap<K>()
inline fun <K> emptyObject2DoubleMap(): @Unmodifiable Object2DoubleMap<K> =
    Object2DoubleMaps.emptyMap()

inline fun <K> Object2DoubleMap<K>.synchronize(): Object2DoubleMap<K> =
    Object2DoubleMaps.synchronize(this)

inline fun <K> Object2DoubleMap<K>.freeze(): @UnmodifiableView Object2DoubleMap<K> =
    Object2DoubleMaps.unmodifiable(this)

inline fun <K> Object2DoubleMap<out K>.toMutableObjectList(): ObjectList<Pair<K, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<K, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}

inline fun <K> Object2DoubleMap<out K>.toObjectList(): ObjectList<Pair<K, Double>> =
    toMutableObjectList().freeze()
// endregion

// =====================================================
// ========== INT2* MAP EXTENSIONS ==========
// =====================================================

// ------------------- Int2BooleanMap -------------------
inline fun mutableInt2BooleanMapOf(vararg pairs: Pair<Int, Boolean>) =
    Int2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2BooleanMapOf() = Int2BooleanOpenHashMap()
inline fun mutableInt2BooleanMapOf(capacity: Int) = Int2BooleanOpenHashMap(capacity)
inline fun int2BooleanMapOf(vararg pairs: Pair<Int, Boolean>) = mutableInt2BooleanMapOf(*pairs).freeze()
inline fun int2BooleanMapOf() = emptyInt2BooleanMap()
inline fun emptyInt2BooleanMap(): @Unmodifiable Int2BooleanMap = Int2BooleanMaps.EMPTY_MAP
inline fun Int2BooleanMap.synchronize(): Int2BooleanMap = Int2BooleanMaps.synchronize(this)
inline fun Int2BooleanMap.freeze(): @UnmodifiableView Int2BooleanMap = Int2BooleanMaps.unmodifiable(this)
inline fun Int2BooleanMap.toMutableObjectList(): ObjectList<Pair<Int, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2BooleanMap.toObjectList(): ObjectList<Pair<Int, Boolean>> = toMutableObjectList().freeze()

// ------------------- Int2ByteMap -------------------
inline fun mutableInt2ByteMapOf(vararg pairs: Pair<Int, Byte>) =
    Int2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2ByteMapOf() = Int2ByteOpenHashMap()
inline fun mutableInt2ByteMapOf(capacity: Int) = Int2ByteOpenHashMap(capacity)
inline fun int2ByteMapOf(vararg pairs: Pair<Int, Byte>) = mutableInt2ByteMapOf(*pairs).freeze()
inline fun int2ByteMapOf() = emptyInt2ByteMap()
inline fun emptyInt2ByteMap(): @Unmodifiable Int2ByteMap = Int2ByteMaps.EMPTY_MAP
inline fun Int2ByteMap.synchronize(): Int2ByteMap = Int2ByteMaps.synchronize(this)
inline fun Int2ByteMap.freeze(): @UnmodifiableView Int2ByteMap = Int2ByteMaps.unmodifiable(this)
inline fun Int2ByteMap.toMutableObjectList(): ObjectList<Pair<Int, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2ByteMap.toObjectList(): ObjectList<Pair<Int, Byte>> = toMutableObjectList().freeze()

// ------------------- Int2CharMap -------------------
inline fun mutableInt2CharMapOf(vararg pairs: Pair<Int, Char>) =
    Int2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2CharMapOf() = Int2CharOpenHashMap()
inline fun mutableInt2CharMapOf(capacity: Int) = Int2CharOpenHashMap(capacity)
inline fun int2CharMapOf(vararg pairs: Pair<Int, Char>) = mutableInt2CharMapOf(*pairs).freeze()
inline fun int2CharMapOf() = emptyInt2CharMap()
inline fun emptyInt2CharMap(): @Unmodifiable Int2CharMap = Int2CharMaps.EMPTY_MAP
inline fun Int2CharMap.synchronize(): Int2CharMap = Int2CharMaps.synchronize(this)
inline fun Int2CharMap.freeze(): @UnmodifiableView Int2CharMap = Int2CharMaps.unmodifiable(this)
inline fun Int2CharMap.toMutableObjectList(): ObjectList<Pair<Int, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2CharMap.toObjectList(): ObjectList<Pair<Int, Char>> = toMutableObjectList().freeze()

// ------------------- Int2ShortMap -------------------
inline fun mutableInt2ShortMapOf(vararg pairs: Pair<Int, Short>) =
    Int2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2ShortMapOf() = Int2ShortOpenHashMap()
inline fun mutableInt2ShortMapOf(capacity: Int) = Int2ShortOpenHashMap(capacity)
inline fun int2ShortMapOf(vararg pairs: Pair<Int, Short>) = mutableInt2ShortMapOf(*pairs).freeze()
inline fun int2ShortMapOf() = emptyInt2ShortMap()
inline fun emptyInt2ShortMap(): @Unmodifiable Int2ShortMap = Int2ShortMaps.EMPTY_MAP
inline fun Int2ShortMap.synchronize(): Int2ShortMap = Int2ShortMaps.synchronize(this)
inline fun Int2ShortMap.freeze(): @UnmodifiableView Int2ShortMap = Int2ShortMaps.unmodifiable(this)
inline fun Int2ShortMap.toMutableObjectList(): ObjectList<Pair<Int, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2ShortMap.toObjectList(): ObjectList<Pair<Int, Short>> = toMutableObjectList().freeze()

// ------------------- Int2IntMap -------------------
inline fun mutableInt2IntMapOf(vararg pairs: Pair<Int, Int>) =
    Int2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2IntMapOf() = Int2IntOpenHashMap()
inline fun mutableInt2IntMapOf(capacity: Int) = Int2IntOpenHashMap(capacity)
inline fun int2IntMapOf(vararg pairs: Pair<Int, Int>) = mutableInt2IntMapOf(*pairs).freeze()
inline fun int2IntMapOf() = emptyInt2IntMap()
inline fun emptyInt2IntMap(): @Unmodifiable Int2IntMap = Int2IntMaps.EMPTY_MAP
inline fun Int2IntMap.synchronize(): Int2IntMap = Int2IntMaps.synchronize(this)
inline fun Int2IntMap.freeze(): @UnmodifiableView Int2IntMap = Int2IntMaps.unmodifiable(this)
inline fun Int2IntMap.toMutableObjectList(): ObjectList<Pair<Int, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2IntMap.toObjectList(): ObjectList<Pair<Int, Int>> = toMutableObjectList().freeze()

// ------------------- Int2LongMap -------------------
inline fun mutableInt2LongMapOf(vararg pairs: Pair<Int, Long>) =
    Int2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2LongMapOf() = Int2LongOpenHashMap()
inline fun mutableInt2LongMapOf(capacity: Int) = Int2LongOpenHashMap(capacity)
inline fun int2LongMapOf(vararg pairs: Pair<Int, Long>) = mutableInt2LongMapOf(*pairs).freeze()
inline fun int2LongMapOf() = emptyInt2LongMap()
inline fun emptyInt2LongMap(): @Unmodifiable Int2LongMap = Int2LongMaps.EMPTY_MAP
inline fun Int2LongMap.synchronize(): Int2LongMap = Int2LongMaps.synchronize(this)
inline fun Int2LongMap.freeze(): @UnmodifiableView Int2LongMap = Int2LongMaps.unmodifiable(this)
inline fun Int2LongMap.toMutableObjectList(): ObjectList<Pair<Int, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2LongMap.toObjectList(): ObjectList<Pair<Int, Long>> = toMutableObjectList().freeze()

// ------------------- Int2FloatMap -------------------
inline fun mutableInt2FloatMapOf(vararg pairs: Pair<Int, Float>) =
    Int2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2FloatMapOf() = Int2FloatOpenHashMap()
inline fun mutableInt2FloatMapOf(capacity: Int) = Int2FloatOpenHashMap(capacity)
inline fun int2FloatMapOf(vararg pairs: Pair<Int, Float>) = mutableInt2FloatMapOf(*pairs).freeze()
inline fun int2FloatMapOf() = emptyInt2FloatMap()
inline fun emptyInt2FloatMap(): @Unmodifiable Int2FloatMap = Int2FloatMaps.EMPTY_MAP
inline fun Int2FloatMap.synchronize(): Int2FloatMap = Int2FloatMaps.synchronize(this)
inline fun Int2FloatMap.freeze(): @UnmodifiableView Int2FloatMap = Int2FloatMaps.unmodifiable(this)
inline fun Int2FloatMap.toMutableObjectList(): ObjectList<Pair<Int, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2FloatMap.toObjectList(): ObjectList<Pair<Int, Float>> = toMutableObjectList().freeze()

// ------------------- Int2DoubleMap -------------------
inline fun mutableInt2DoubleMapOf(vararg pairs: Pair<Int, Double>) =
    Int2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableInt2DoubleMapOf() = Int2DoubleOpenHashMap()
inline fun mutableInt2DoubleMapOf(capacity: Int) = Int2DoubleOpenHashMap(capacity)
inline fun int2DoubleMapOf(vararg pairs: Pair<Int, Double>) = mutableInt2DoubleMapOf(*pairs).freeze()
inline fun int2DoubleMapOf() = emptyInt2DoubleMap()
inline fun emptyInt2DoubleMap(): @Unmodifiable Int2DoubleMap = Int2DoubleMaps.EMPTY_MAP
inline fun Int2DoubleMap.synchronize(): Int2DoubleMap = Int2DoubleMaps.synchronize(this)
inline fun Int2DoubleMap.freeze(): @UnmodifiableView Int2DoubleMap = Int2DoubleMaps.unmodifiable(this)
inline fun Int2DoubleMap.toMutableObjectList(): ObjectList<Pair<Int, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Int2DoubleMap.toObjectList(): ObjectList<Pair<Int, Double>> = toMutableObjectList().freeze()

// ------------------- Int2ObjectMap -------------------
inline fun <V> mutableInt2ObjectMapOf(vararg pairs: Pair<Int, V>) =
    Int2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableInt2ObjectMapOf() = Int2ObjectOpenHashMap<V>()
inline fun <V> mutableInt2ObjectMapOf(capacity: Int) = Int2ObjectOpenHashMap<V>(capacity)
inline fun <V> int2ObjectMapOf(vararg pairs: Pair<Int, V>) = mutableInt2ObjectMapOf(*pairs).freeze()
inline fun <V> int2ObjectMapOf() = emptyInt2ObjectMap<V>()
inline fun <V> emptyInt2ObjectMap(): @Unmodifiable Int2ObjectMap<V> = Int2ObjectMaps.emptyMap()
inline fun <V> Int2ObjectMap<V>.synchronize(): Int2ObjectMap<V> = Int2ObjectMaps.synchronize(this)
inline fun <V> Int2ObjectMap<V>.freeze(): @UnmodifiableView Int2ObjectMap<V> = Int2ObjectMaps.unmodifiable(this)
inline fun <V> Int2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Int, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Int, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Int2ObjectMap<V>.toObjectList(): ObjectList<Pair<Int, V>> = toMutableObjectList().freeze()


// =====================================================
// ========== DOUBLE2* MAP EXTENSIONS ==========
// =====================================================

// ------------------- Double2BooleanMap -------------------
inline fun mutableDouble2BooleanMapOf(vararg pairs: Pair<Double, Boolean>) =
    Double2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2BooleanMapOf() = Double2BooleanOpenHashMap()
inline fun mutableDouble2BooleanMapOf(capacity: Int) = Double2BooleanOpenHashMap(capacity)
inline fun double2BooleanMapOf(vararg pairs: Pair<Double, Boolean>) = mutableDouble2BooleanMapOf(*pairs).freeze()
inline fun double2BooleanMapOf() = emptyDouble2BooleanMap()
inline fun emptyDouble2BooleanMap(): @Unmodifiable Double2BooleanMap = Double2BooleanMaps.EMPTY_MAP
inline fun Double2BooleanMap.synchronize(): Double2BooleanMap = Double2BooleanMaps.synchronize(this)
inline fun Double2BooleanMap.freeze(): @UnmodifiableView Double2BooleanMap = Double2BooleanMaps.unmodifiable(this)
inline fun Double2BooleanMap.toMutableObjectList(): ObjectList<Pair<Double, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2BooleanMap.toObjectList(): ObjectList<Pair<Double, Boolean>> = toMutableObjectList().freeze()

// ------------------- Double2ByteMap -------------------
inline fun mutableDouble2ByteMapOf(vararg pairs: Pair<Double, Byte>) =
    Double2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2ByteMapOf() = Double2ByteOpenHashMap()
inline fun mutableDouble2ByteMapOf(capacity: Int) = Double2ByteOpenHashMap(capacity)
inline fun double2ByteMapOf(vararg pairs: Pair<Double, Byte>) = mutableDouble2ByteMapOf(*pairs).freeze()
inline fun double2ByteMapOf() = emptyDouble2ByteMap()
inline fun emptyDouble2ByteMap(): @Unmodifiable Double2ByteMap = Double2ByteMaps.EMPTY_MAP
inline fun Double2ByteMap.synchronize(): Double2ByteMap = Double2ByteMaps.synchronize(this)
inline fun Double2ByteMap.freeze(): @UnmodifiableView Double2ByteMap = Double2ByteMaps.unmodifiable(this)
inline fun Double2ByteMap.toMutableObjectList(): ObjectList<Pair<Double, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2ByteMap.toObjectList(): ObjectList<Pair<Double, Byte>> = toMutableObjectList().freeze()

// ------------------- Double2CharMap -------------------
inline fun mutableDouble2CharMapOf(vararg pairs: Pair<Double, Char>) =
    Double2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2CharMapOf() = Double2CharOpenHashMap()
inline fun mutableDouble2CharMapOf(capacity: Int) = Double2CharOpenHashMap(capacity)
inline fun double2CharMapOf(vararg pairs: Pair<Double, Char>) = mutableDouble2CharMapOf(*pairs).freeze()
inline fun double2CharMapOf() = emptyDouble2CharMap()
inline fun emptyDouble2CharMap(): @Unmodifiable Double2CharMap = Double2CharMaps.EMPTY_MAP
inline fun Double2CharMap.synchronize(): Double2CharMap = Double2CharMaps.synchronize(this)
inline fun Double2CharMap.freeze(): @UnmodifiableView Double2CharMap = Double2CharMaps.unmodifiable(this)
inline fun Double2CharMap.toMutableObjectList(): ObjectList<Pair<Double, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2CharMap.toObjectList(): ObjectList<Pair<Double, Char>> = toMutableObjectList().freeze()

// ------------------- Double2ShortMap -------------------
inline fun mutableDouble2ShortMapOf(vararg pairs: Pair<Double, Short>) =
    Double2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2ShortMapOf() = Double2ShortOpenHashMap()
inline fun mutableDouble2ShortMapOf(capacity: Int) = Double2ShortOpenHashMap(capacity)
inline fun double2ShortMapOf(vararg pairs: Pair<Double, Short>) = mutableDouble2ShortMapOf(*pairs).freeze()
inline fun double2ShortMapOf() = emptyDouble2ShortMap()
inline fun emptyDouble2ShortMap(): @Unmodifiable Double2ShortMap = Double2ShortMaps.EMPTY_MAP
inline fun Double2ShortMap.synchronize(): Double2ShortMap = Double2ShortMaps.synchronize(this)
inline fun Double2ShortMap.freeze(): @UnmodifiableView Double2ShortMap = Double2ShortMaps.unmodifiable(this)
inline fun Double2ShortMap.toMutableObjectList(): ObjectList<Pair<Double, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2ShortMap.toObjectList(): ObjectList<Pair<Double, Short>> = toMutableObjectList().freeze()

// ------------------- Double2IntMap -------------------
inline fun mutableDouble2IntMapOf(vararg pairs: Pair<Double, Int>) =
    Double2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2IntMapOf() = Double2IntOpenHashMap()
inline fun mutableDouble2IntMapOf(capacity: Int) = Double2IntOpenHashMap(capacity)
inline fun double2IntMapOf(vararg pairs: Pair<Double, Int>) = mutableDouble2IntMapOf(*pairs).freeze()
inline fun double2IntMapOf() = emptyDouble2IntMap()
inline fun emptyDouble2IntMap(): @Unmodifiable Double2IntMap = Double2IntMaps.EMPTY_MAP
inline fun Double2IntMap.synchronize(): Double2IntMap = Double2IntMaps.synchronize(this)
inline fun Double2IntMap.freeze(): @UnmodifiableView Double2IntMap = Double2IntMaps.unmodifiable(this)
inline fun Double2IntMap.toMutableObjectList(): ObjectList<Pair<Double, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2IntMap.toObjectList(): ObjectList<Pair<Double, Int>> = toMutableObjectList().freeze()

// ------------------- Double2LongMap -------------------
inline fun mutableDouble2LongMapOf(vararg pairs: Pair<Double, Long>) =
    Double2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2LongMapOf() = Double2LongOpenHashMap()
inline fun mutableDouble2LongMapOf(capacity: Int) = Double2LongOpenHashMap(capacity)
inline fun double2LongMapOf(vararg pairs: Pair<Double, Long>) = mutableDouble2LongMapOf(*pairs).freeze()
inline fun double2LongMapOf() = emptyDouble2LongMap()
inline fun emptyDouble2LongMap(): @Unmodifiable Double2LongMap = Double2LongMaps.EMPTY_MAP
inline fun Double2LongMap.synchronize(): Double2LongMap = Double2LongMaps.synchronize(this)
inline fun Double2LongMap.freeze(): @UnmodifiableView Double2LongMap = Double2LongMaps.unmodifiable(this)
inline fun Double2LongMap.toMutableObjectList(): ObjectList<Pair<Double, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2LongMap.toObjectList(): ObjectList<Pair<Double, Long>> = toMutableObjectList().freeze()

// ------------------- Double2FloatMap -------------------
inline fun mutableDouble2FloatMapOf(vararg pairs: Pair<Double, Float>) =
    Double2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2FloatMapOf() = Double2FloatOpenHashMap()
inline fun mutableDouble2FloatMapOf(capacity: Int) = Double2FloatOpenHashMap(capacity)
inline fun double2FloatMapOf(vararg pairs: Pair<Double, Float>) = mutableDouble2FloatMapOf(*pairs).freeze()
inline fun double2FloatMapOf() = emptyDouble2FloatMap()
inline fun emptyDouble2FloatMap(): @Unmodifiable Double2FloatMap = Double2FloatMaps.EMPTY_MAP
inline fun Double2FloatMap.synchronize(): Double2FloatMap = Double2FloatMaps.synchronize(this)
inline fun Double2FloatMap.freeze(): @UnmodifiableView Double2FloatMap = Double2FloatMaps.unmodifiable(this)
inline fun Double2FloatMap.toMutableObjectList(): ObjectList<Pair<Double, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2FloatMap.toObjectList(): ObjectList<Pair<Double, Float>> = toMutableObjectList().freeze()

// ------------------- Double2DoubleMap -------------------
inline fun mutableDouble2DoubleMapOf(vararg pairs: Pair<Double, Double>) =
    Double2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableDouble2DoubleMapOf() = Double2DoubleOpenHashMap()
inline fun mutableDouble2DoubleMapOf(capacity: Int) = Double2DoubleOpenHashMap(capacity)
inline fun double2DoubleMapOf(vararg pairs: Pair<Double, Double>) = mutableDouble2DoubleMapOf(*pairs).freeze()
inline fun double2DoubleMapOf() = emptyDouble2DoubleMap()
inline fun emptyDouble2DoubleMap(): @Unmodifiable Double2DoubleMap = Double2DoubleMaps.EMPTY_MAP
inline fun Double2DoubleMap.synchronize(): Double2DoubleMap = Double2DoubleMaps.synchronize(this)
inline fun Double2DoubleMap.freeze(): @UnmodifiableView Double2DoubleMap = Double2DoubleMaps.unmodifiable(this)
inline fun Double2DoubleMap.toMutableObjectList(): ObjectList<Pair<Double, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Double2DoubleMap.toObjectList(): ObjectList<Pair<Double, Double>> = toMutableObjectList().freeze()

// ------------------- Double2ObjectMap -------------------
inline fun <V> mutableDouble2ObjectMapOf(vararg pairs: Pair<Double, V>) =
    Double2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableDouble2ObjectMapOf() = Double2ObjectOpenHashMap<V>()
inline fun <V> mutableDouble2ObjectMapOf(capacity: Int) = Double2ObjectOpenHashMap<V>(capacity)
inline fun <V> double2ObjectMapOf(vararg pairs: Pair<Double, V>) = mutableDouble2ObjectMapOf(*pairs).freeze()
inline fun <V> double2ObjectMapOf() = emptyDouble2ObjectMap<V>()
inline fun <V> emptyDouble2ObjectMap(): @Unmodifiable Double2ObjectMap<V> = Double2ObjectMaps.emptyMap()
inline fun <V> Double2ObjectMap<V>.synchronize(): Double2ObjectMap<V> = Double2ObjectMaps.synchronize(this)
inline fun <V> Double2ObjectMap<V>.freeze(): @UnmodifiableView Double2ObjectMap<V> = Double2ObjectMaps.unmodifiable(this)
inline fun <V> Double2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Double, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Double, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Double2ObjectMap<V>.toObjectList(): ObjectList<Pair<Double, V>> = toMutableObjectList().freeze()


// =====================================================
// 2. BYTE2* MAP EXTENSIONS
// (Using package it.unimi.dsi.fastutil.bytes.*)
// =====================================================

/** Byte2BooleanMap **/
inline fun mutableByte2BooleanMapOf(vararg pairs: Pair<Byte, Boolean>) =
    Byte2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2BooleanMapOf() = Byte2BooleanOpenHashMap()
inline fun mutableByte2BooleanMapOf(capacity: Int) = Byte2BooleanOpenHashMap(capacity)
inline fun byte2BooleanMapOf(vararg pairs: Pair<Byte, Boolean>) = mutableByte2BooleanMapOf(*pairs).freeze()
inline fun byte2BooleanMapOf() = emptyByte2BooleanMap()
inline fun emptyByte2BooleanMap(): @Unmodifiable Byte2BooleanMap = Byte2BooleanMaps.EMPTY_MAP
inline fun Byte2BooleanMap.synchronize(): Byte2BooleanMap = Byte2BooleanMaps.synchronize(this)
inline fun Byte2BooleanMap.freeze(): @UnmodifiableView Byte2BooleanMap = Byte2BooleanMaps.unmodifiable(this)
inline fun Byte2BooleanMap.toMutableObjectList(): ObjectList<Pair<Byte, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2BooleanMap.toObjectList(): ObjectList<Pair<Byte, Boolean>> = toMutableObjectList().freeze()

/** Byte2ByteMap **/
inline fun mutableByte2ByteMapOf(vararg pairs: Pair<Byte, Byte>) =
    Byte2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2ByteMapOf() = Byte2ByteOpenHashMap()
inline fun mutableByte2ByteMapOf(capacity: Int) = Byte2ByteOpenHashMap(capacity)
inline fun byte2ByteMapOf(vararg pairs: Pair<Byte, Byte>) = mutableByte2ByteMapOf(*pairs).freeze()
inline fun byte2ByteMapOf() = emptyByte2ByteMap()
inline fun emptyByte2ByteMap(): @Unmodifiable Byte2ByteMap = Byte2ByteMaps.EMPTY_MAP
inline fun Byte2ByteMap.synchronize(): Byte2ByteMap = Byte2ByteMaps.synchronize(this)
inline fun Byte2ByteMap.freeze(): @UnmodifiableView Byte2ByteMap = Byte2ByteMaps.unmodifiable(this)
inline fun Byte2ByteMap.toMutableObjectList(): ObjectList<Pair<Byte, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2ByteMap.toObjectList(): ObjectList<Pair<Byte, Byte>> = toMutableObjectList().freeze()

/** Byte2CharMap **/
inline fun mutableByte2CharMapOf(vararg pairs: Pair<Byte, Char>) =
    Byte2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2CharMapOf() = Byte2CharOpenHashMap()
inline fun mutableByte2CharMapOf(capacity: Int) = Byte2CharOpenHashMap(capacity)
inline fun byte2CharMapOf(vararg pairs: Pair<Byte, Char>) = mutableByte2CharMapOf(*pairs).freeze()
inline fun byte2CharMapOf() = emptyByte2CharMap()
inline fun emptyByte2CharMap(): @Unmodifiable Byte2CharMap = Byte2CharMaps.EMPTY_MAP
inline fun Byte2CharMap.synchronize(): Byte2CharMap = Byte2CharMaps.synchronize(this)
inline fun Byte2CharMap.freeze(): @UnmodifiableView Byte2CharMap = Byte2CharMaps.unmodifiable(this)
inline fun Byte2CharMap.toMutableObjectList(): ObjectList<Pair<Byte, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2CharMap.toObjectList(): ObjectList<Pair<Byte, Char>> = toMutableObjectList().freeze()

/** Byte2ShortMap **/
inline fun mutableByte2ShortMapOf(vararg pairs: Pair<Byte, Short>) =
    Byte2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2ShortMapOf() = Byte2ShortOpenHashMap()
inline fun mutableByte2ShortMapOf(capacity: Int) = Byte2ShortOpenHashMap(capacity)
inline fun byte2ShortMapOf(vararg pairs: Pair<Byte, Short>) = mutableByte2ShortMapOf(*pairs).freeze()
inline fun byte2ShortMapOf() = emptyByte2ShortMap()
inline fun emptyByte2ShortMap(): @Unmodifiable Byte2ShortMap = Byte2ShortMaps.EMPTY_MAP
inline fun Byte2ShortMap.synchronize(): Byte2ShortMap = Byte2ShortMaps.synchronize(this)
inline fun Byte2ShortMap.freeze(): @UnmodifiableView Byte2ShortMap = Byte2ShortMaps.unmodifiable(this)
inline fun Byte2ShortMap.toMutableObjectList(): ObjectList<Pair<Byte, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2ShortMap.toObjectList(): ObjectList<Pair<Byte, Short>> = toMutableObjectList().freeze()

/** Byte2IntMap **/
inline fun mutableByte2IntMapOf(vararg pairs: Pair<Byte, Int>) =
    Byte2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2IntMapOf() = Byte2IntOpenHashMap()
inline fun mutableByte2IntMapOf(capacity: Int) = Byte2IntOpenHashMap(capacity)
inline fun byte2IntMapOf(vararg pairs: Pair<Byte, Int>) = mutableByte2IntMapOf(*pairs).freeze()
inline fun byte2IntMapOf() = emptyByte2IntMap()
inline fun emptyByte2IntMap(): @Unmodifiable Byte2IntMap = Byte2IntMaps.EMPTY_MAP
inline fun Byte2IntMap.synchronize(): Byte2IntMap = Byte2IntMaps.synchronize(this)
inline fun Byte2IntMap.freeze(): @UnmodifiableView Byte2IntMap = Byte2IntMaps.unmodifiable(this)
inline fun Byte2IntMap.toMutableObjectList(): ObjectList<Pair<Byte, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2IntMap.toObjectList(): ObjectList<Pair<Byte, Int>> = toMutableObjectList().freeze()

/** Byte2LongMap **/
inline fun mutableByte2LongMapOf(vararg pairs: Pair<Byte, Long>) =
    Byte2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2LongMapOf() = Byte2LongOpenHashMap()
inline fun mutableByte2LongMapOf(capacity: Int) = Byte2LongOpenHashMap(capacity)
inline fun byte2LongMapOf(vararg pairs: Pair<Byte, Long>) = mutableByte2LongMapOf(*pairs).freeze()
inline fun byte2LongMapOf() = emptyByte2LongMap()
inline fun emptyByte2LongMap(): @Unmodifiable Byte2LongMap = Byte2LongMaps.EMPTY_MAP
inline fun Byte2LongMap.synchronize(): Byte2LongMap = Byte2LongMaps.synchronize(this)
inline fun Byte2LongMap.freeze(): @UnmodifiableView Byte2LongMap = Byte2LongMaps.unmodifiable(this)
inline fun Byte2LongMap.toMutableObjectList(): ObjectList<Pair<Byte, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2LongMap.toObjectList(): ObjectList<Pair<Byte, Long>> = toMutableObjectList().freeze()

/** Byte2FloatMap **/
inline fun mutableByte2FloatMapOf(vararg pairs: Pair<Byte, Float>) =
    Byte2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2FloatMapOf() = Byte2FloatOpenHashMap()
inline fun mutableByte2FloatMapOf(capacity: Int) = Byte2FloatOpenHashMap(capacity)
inline fun byte2FloatMapOf(vararg pairs: Pair<Byte, Float>) = mutableByte2FloatMapOf(*pairs).freeze()
inline fun byte2FloatMapOf() = emptyByte2FloatMap()
inline fun emptyByte2FloatMap(): @Unmodifiable Byte2FloatMap = Byte2FloatMaps.EMPTY_MAP
inline fun Byte2FloatMap.synchronize(): Byte2FloatMap = Byte2FloatMaps.synchronize(this)
inline fun Byte2FloatMap.freeze(): @UnmodifiableView Byte2FloatMap = Byte2FloatMaps.unmodifiable(this)
inline fun Byte2FloatMap.toMutableObjectList(): ObjectList<Pair<Byte, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2FloatMap.toObjectList(): ObjectList<Pair<Byte, Float>> = toMutableObjectList().freeze()

/** Byte2DoubleMap **/
inline fun mutableByte2DoubleMapOf(vararg pairs: Pair<Byte, Double>) =
    Byte2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableByte2DoubleMapOf() = Byte2DoubleOpenHashMap()
inline fun mutableByte2DoubleMapOf(capacity: Int) = Byte2DoubleOpenHashMap(capacity)
inline fun byte2DoubleMapOf(vararg pairs: Pair<Byte, Double>) = mutableByte2DoubleMapOf(*pairs).freeze()
inline fun byte2DoubleMapOf() = emptyByte2DoubleMap()
inline fun emptyByte2DoubleMap(): @Unmodifiable Byte2DoubleMap = Byte2DoubleMaps.EMPTY_MAP
inline fun Byte2DoubleMap.synchronize(): Byte2DoubleMap = Byte2DoubleMaps.synchronize(this)
inline fun Byte2DoubleMap.freeze(): @UnmodifiableView Byte2DoubleMap = Byte2DoubleMaps.unmodifiable(this)
inline fun Byte2DoubleMap.toMutableObjectList(): ObjectList<Pair<Byte, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Byte2DoubleMap.toObjectList(): ObjectList<Pair<Byte, Double>> = toMutableObjectList().freeze()

/** Byte2ObjectMap **/
inline fun <V> mutableByte2ObjectMapOf(vararg pairs: Pair<Byte, V>) =
    Byte2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableByte2ObjectMapOf() = Byte2ObjectOpenHashMap<V>()
inline fun <V> mutableByte2ObjectMapOf(capacity: Int) = Byte2ObjectOpenHashMap<V>(capacity)
inline fun <V> byte2ObjectMapOf(vararg pairs: Pair<Byte, V>) = mutableByte2ObjectMapOf(*pairs).freeze()
inline fun <V> byte2ObjectMapOf() = emptyByte2ObjectMap<V>()
inline fun <V> emptyByte2ObjectMap(): @Unmodifiable Byte2ObjectMap<V> = Byte2ObjectMaps.emptyMap()
inline fun <V> Byte2ObjectMap<V>.synchronize(): Byte2ObjectMap<V> = Byte2ObjectMaps.synchronize(this)
inline fun <V> Byte2ObjectMap<V>.freeze(): @UnmodifiableView Byte2ObjectMap<V> = Byte2ObjectMaps.unmodifiable(this)
inline fun <V> Byte2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Byte, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Byte, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Byte2ObjectMap<V>.toObjectList(): ObjectList<Pair<Byte, V>> = toMutableObjectList().freeze()

// =====================================================
// 3. CHAR2* MAP EXTENSIONS
// (Similar pattern – using it.unimi.dsi.fastutil.chars.*)
// =====================================================

/** Char2BooleanMap **/
inline fun mutableChar2BooleanMapOf(vararg pairs: Pair<Char, Boolean>) =
    Char2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2BooleanMapOf() = Char2BooleanOpenHashMap()
inline fun mutableChar2BooleanMapOf(capacity: Int) = Char2BooleanOpenHashMap(capacity)
inline fun char2BooleanMapOf(vararg pairs: Pair<Char, Boolean>) = mutableChar2BooleanMapOf(*pairs).freeze()
inline fun char2BooleanMapOf() = emptyChar2BooleanMap()
inline fun emptyChar2BooleanMap(): @Unmodifiable Char2BooleanMap = Char2BooleanMaps.EMPTY_MAP
inline fun Char2BooleanMap.synchronize(): Char2BooleanMap = Char2BooleanMaps.synchronize(this)
inline fun Char2BooleanMap.freeze(): @UnmodifiableView Char2BooleanMap = Char2BooleanMaps.unmodifiable(this)
inline fun Char2BooleanMap.toMutableObjectList(): ObjectList<Pair<Char, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2BooleanMap.toObjectList(): ObjectList<Pair<Char, Boolean>> = toMutableObjectList().freeze()

/** Char2ByteMap **/
inline fun mutableChar2ByteMapOf(vararg pairs: Pair<Char, Byte>) =
    Char2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2ByteMapOf() = Char2ByteOpenHashMap()
inline fun mutableChar2ByteMapOf(capacity: Int) = Char2ByteOpenHashMap(capacity)
inline fun char2ByteMapOf(vararg pairs: Pair<Char, Byte>) = mutableChar2ByteMapOf(*pairs).freeze()
inline fun char2ByteMapOf() = emptyChar2ByteMap()
inline fun emptyChar2ByteMap(): @Unmodifiable Char2ByteMap = Char2ByteMaps.EMPTY_MAP
inline fun Char2ByteMap.synchronize(): Char2ByteMap = Char2ByteMaps.synchronize(this)
inline fun Char2ByteMap.freeze(): @UnmodifiableView Char2ByteMap = Char2ByteMaps.unmodifiable(this)
inline fun Char2ByteMap.toMutableObjectList(): ObjectList<Pair<Char, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2ByteMap.toObjectList(): ObjectList<Pair<Char, Byte>> = toMutableObjectList().freeze()

/** Char2CharMap **/
inline fun mutableChar2CharMapOf(vararg pairs: Pair<Char, Char>) =
    Char2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2CharMapOf() = Char2CharOpenHashMap()
inline fun mutableChar2CharMapOf(capacity: Int) = Char2CharOpenHashMap(capacity)
inline fun char2CharMapOf(vararg pairs: Pair<Char, Char>) = mutableChar2CharMapOf(*pairs).freeze()
inline fun char2CharMapOf() = emptyChar2CharMap()
inline fun emptyChar2CharMap(): @Unmodifiable Char2CharMap = Char2CharMaps.EMPTY_MAP
inline fun Char2CharMap.synchronize(): Char2CharMap = Char2CharMaps.synchronize(this)
inline fun Char2CharMap.freeze(): @UnmodifiableView Char2CharMap = Char2CharMaps.unmodifiable(this)
inline fun Char2CharMap.toMutableObjectList(): ObjectList<Pair<Char, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2CharMap.toObjectList(): ObjectList<Pair<Char, Char>> = toMutableObjectList().freeze()

/** Char2ShortMap **/
inline fun mutableChar2ShortMapOf(vararg pairs: Pair<Char, Short>) =
    Char2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2ShortMapOf() = Char2ShortOpenHashMap()
inline fun mutableChar2ShortMapOf(capacity: Int) = Char2ShortOpenHashMap(capacity)
inline fun char2ShortMapOf(vararg pairs: Pair<Char, Short>) = mutableChar2ShortMapOf(*pairs).freeze()
inline fun char2ShortMapOf() = emptyChar2ShortMap()
inline fun emptyChar2ShortMap(): @Unmodifiable Char2ShortMap = Char2ShortMaps.EMPTY_MAP
inline fun Char2ShortMap.synchronize(): Char2ShortMap = Char2ShortMaps.synchronize(this)
inline fun Char2ShortMap.freeze(): @UnmodifiableView Char2ShortMap = Char2ShortMaps.unmodifiable(this)
inline fun Char2ShortMap.toMutableObjectList(): ObjectList<Pair<Char, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2ShortMap.toObjectList(): ObjectList<Pair<Char, Short>> = toMutableObjectList().freeze()

/** Char2IntMap **/
inline fun mutableChar2IntMapOf(vararg pairs: Pair<Char, Int>) =
    Char2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2IntMapOf() = Char2IntOpenHashMap()
inline fun mutableChar2IntMapOf(capacity: Int) = Char2IntOpenHashMap(capacity)
inline fun char2IntMapOf(vararg pairs: Pair<Char, Int>) = mutableChar2IntMapOf(*pairs).freeze()
inline fun char2IntMapOf() = emptyChar2IntMap()
inline fun emptyChar2IntMap(): @Unmodifiable Char2IntMap = Char2IntMaps.EMPTY_MAP
inline fun Char2IntMap.synchronize(): Char2IntMap = Char2IntMaps.synchronize(this)
inline fun Char2IntMap.freeze(): @UnmodifiableView Char2IntMap = Char2IntMaps.unmodifiable(this)
inline fun Char2IntMap.toMutableObjectList(): ObjectList<Pair<Char, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2IntMap.toObjectList(): ObjectList<Pair<Char, Int>> = toMutableObjectList().freeze()

/** Char2LongMap **/
inline fun mutableChar2LongMapOf(vararg pairs: Pair<Char, Long>) =
    Char2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2LongMapOf() = Char2LongOpenHashMap()
inline fun mutableChar2LongMapOf(capacity: Int) = Char2LongOpenHashMap(capacity)
inline fun char2LongMapOf(vararg pairs: Pair<Char, Long>) = mutableChar2LongMapOf(*pairs).freeze()
inline fun char2LongMapOf() = emptyChar2LongMap()
inline fun emptyChar2LongMap(): @Unmodifiable Char2LongMap = Char2LongMaps.EMPTY_MAP
inline fun Char2LongMap.synchronize(): Char2LongMap = Char2LongMaps.synchronize(this)
inline fun Char2LongMap.freeze(): @UnmodifiableView Char2LongMap = Char2LongMaps.unmodifiable(this)
inline fun Char2LongMap.toMutableObjectList(): ObjectList<Pair<Char, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2LongMap.toObjectList(): ObjectList<Pair<Char, Long>> = toMutableObjectList().freeze()

/** Char2FloatMap **/
inline fun mutableChar2FloatMapOf(vararg pairs: Pair<Char, Float>) =
    Char2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2FloatMapOf() = Char2FloatOpenHashMap()
inline fun mutableChar2FloatMapOf(capacity: Int) = Char2FloatOpenHashMap(capacity)
inline fun char2FloatMapOf(vararg pairs: Pair<Char, Float>) = mutableChar2FloatMapOf(*pairs).freeze()
inline fun char2FloatMapOf() = emptyChar2FloatMap()
inline fun emptyChar2FloatMap(): @Unmodifiable Char2FloatMap = Char2FloatMaps.EMPTY_MAP
inline fun Char2FloatMap.synchronize(): Char2FloatMap = Char2FloatMaps.synchronize(this)
inline fun Char2FloatMap.freeze(): @UnmodifiableView Char2FloatMap = Char2FloatMaps.unmodifiable(this)
inline fun Char2FloatMap.toMutableObjectList(): ObjectList<Pair<Char, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2FloatMap.toObjectList(): ObjectList<Pair<Char, Float>> = toMutableObjectList().freeze()

/** Char2DoubleMap **/
inline fun mutableChar2DoubleMapOf(vararg pairs: Pair<Char, Double>) =
    Char2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableChar2DoubleMapOf() = Char2DoubleOpenHashMap()
inline fun mutableChar2DoubleMapOf(capacity: Int) = Char2DoubleOpenHashMap(capacity)
inline fun char2DoubleMapOf(vararg pairs: Pair<Char, Double>) = mutableChar2DoubleMapOf(*pairs).freeze()
inline fun char2DoubleMapOf() = emptyChar2DoubleMap()
inline fun emptyChar2DoubleMap(): @Unmodifiable Char2DoubleMap = Char2DoubleMaps.EMPTY_MAP
inline fun Char2DoubleMap.synchronize(): Char2DoubleMap = Char2DoubleMaps.synchronize(this)
inline fun Char2DoubleMap.freeze(): @UnmodifiableView Char2DoubleMap = Char2DoubleMaps.unmodifiable(this)
inline fun Char2DoubleMap.toMutableObjectList(): ObjectList<Pair<Char, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Char2DoubleMap.toObjectList(): ObjectList<Pair<Char, Double>> = toMutableObjectList().freeze()

/** Char2ObjectMap **/
inline fun <V> mutableChar2ObjectMapOf(vararg pairs: Pair<Char, V>) =
    Char2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableChar2ObjectMapOf() = Char2ObjectOpenHashMap<V>()
inline fun <V> mutableChar2ObjectMapOf(capacity: Int) = Char2ObjectOpenHashMap<V>(capacity)
inline fun <V> char2ObjectMapOf(vararg pairs: Pair<Char, V>) = mutableChar2ObjectMapOf(*pairs).freeze()
inline fun <V> char2ObjectMapOf() = emptyChar2ObjectMap<V>()
inline fun <V> emptyChar2ObjectMap(): @Unmodifiable Char2ObjectMap<V> = Char2ObjectMaps.emptyMap()
inline fun <V> Char2ObjectMap<V>.synchronize(): Char2ObjectMap<V> = Char2ObjectMaps.synchronize(this)
inline fun <V> Char2ObjectMap<V>.freeze(): @UnmodifiableView Char2ObjectMap<V> = Char2ObjectMaps.unmodifiable(this)
inline fun <V> Char2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Char, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Char, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Char2ObjectMap<V>.toObjectList(): ObjectList<Pair<Char, V>> = toMutableObjectList().freeze()

// =====================================================
// 4. SHORT2* MAP EXTENSIONS
// (Using it.unimi.dsi.fastutil.shorts.*)
// =====================================================

/** Short2BooleanMap **/
inline fun mutableShort2BooleanMapOf(vararg pairs: Pair<Short, Boolean>) =
    Short2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2BooleanMapOf() = Short2BooleanOpenHashMap()
inline fun mutableShort2BooleanMapOf(capacity: Int) = Short2BooleanOpenHashMap(capacity)
inline fun short2BooleanMapOf(vararg pairs: Pair<Short, Boolean>) = mutableShort2BooleanMapOf(*pairs).freeze()
inline fun short2BooleanMapOf() = emptyShort2BooleanMap()
inline fun emptyShort2BooleanMap(): @Unmodifiable Short2BooleanMap = Short2BooleanMaps.EMPTY_MAP
inline fun Short2BooleanMap.synchronize(): Short2BooleanMap = Short2BooleanMaps.synchronize(this)
inline fun Short2BooleanMap.freeze(): @UnmodifiableView Short2BooleanMap = Short2BooleanMaps.unmodifiable(this)
inline fun Short2BooleanMap.toMutableObjectList(): ObjectList<Pair<Short, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2BooleanMap.toObjectList(): ObjectList<Pair<Short, Boolean>> = toMutableObjectList().freeze()

/** Short2ByteMap **/
inline fun mutableShort2ByteMapOf(vararg pairs: Pair<Short, Byte>) =
    Short2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2ByteMapOf() = Short2ByteOpenHashMap()
inline fun mutableShort2ByteMapOf(capacity: Int) = Short2ByteOpenHashMap(capacity)
inline fun short2ByteMapOf(vararg pairs: Pair<Short, Byte>) = mutableShort2ByteMapOf(*pairs).freeze()
inline fun short2ByteMapOf() = emptyShort2ByteMap()
inline fun emptyShort2ByteMap(): @Unmodifiable Short2ByteMap = Short2ByteMaps.EMPTY_MAP
inline fun Short2ByteMap.synchronize(): Short2ByteMap = Short2ByteMaps.synchronize(this)
inline fun Short2ByteMap.freeze(): @UnmodifiableView Short2ByteMap = Short2ByteMaps.unmodifiable(this)
inline fun Short2ByteMap.toMutableObjectList(): ObjectList<Pair<Short, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2ByteMap.toObjectList(): ObjectList<Pair<Short, Byte>> = toMutableObjectList().freeze()

/** Short2CharMap **/
inline fun mutableShort2CharMapOf(vararg pairs: Pair<Short, Char>) =
    Short2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2CharMapOf() = Short2CharOpenHashMap()
inline fun mutableShort2CharMapOf(capacity: Int) = Short2CharOpenHashMap(capacity)
inline fun short2CharMapOf(vararg pairs: Pair<Short, Char>) = mutableShort2CharMapOf(*pairs).freeze()
inline fun short2CharMapOf() = emptyShort2CharMap()
inline fun emptyShort2CharMap(): @Unmodifiable Short2CharMap = Short2CharMaps.EMPTY_MAP
inline fun Short2CharMap.synchronize(): Short2CharMap = Short2CharMaps.synchronize(this)
inline fun Short2CharMap.freeze(): @UnmodifiableView Short2CharMap = Short2CharMaps.unmodifiable(this)
inline fun Short2CharMap.toMutableObjectList(): ObjectList<Pair<Short, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2CharMap.toObjectList(): ObjectList<Pair<Short, Char>> = toMutableObjectList().freeze()

/** Short2ShortMap **/
inline fun mutableShort2ShortMapOf(vararg pairs: Pair<Short, Short>) =
    Short2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2ShortMapOf() = Short2ShortOpenHashMap()
inline fun mutableShort2ShortMapOf(capacity: Int) = Short2ShortOpenHashMap(capacity)
inline fun short2ShortMapOf(vararg pairs: Pair<Short, Short>) = mutableShort2ShortMapOf(*pairs).freeze()
inline fun short2ShortMapOf() = emptyShort2ShortMap()
inline fun emptyShort2ShortMap(): @Unmodifiable Short2ShortMap = Short2ShortMaps.EMPTY_MAP
inline fun Short2ShortMap.synchronize(): Short2ShortMap = Short2ShortMaps.synchronize(this)
inline fun Short2ShortMap.freeze(): @UnmodifiableView Short2ShortMap = Short2ShortMaps.unmodifiable(this)
inline fun Short2ShortMap.toMutableObjectList(): ObjectList<Pair<Short, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2ShortMap.toObjectList(): ObjectList<Pair<Short, Short>> = toMutableObjectList().freeze()

/** Short2IntMap **/
inline fun mutableShort2IntMapOf(vararg pairs: Pair<Short, Int>) =
    Short2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2IntMapOf() = Short2IntOpenHashMap()
inline fun mutableShort2IntMapOf(capacity: Int) = Short2IntOpenHashMap(capacity)
inline fun short2IntMapOf(vararg pairs: Pair<Short, Int>) = mutableShort2IntMapOf(*pairs).freeze()
inline fun short2IntMapOf() = emptyShort2IntMap()
inline fun emptyShort2IntMap(): @Unmodifiable Short2IntMap = Short2IntMaps.EMPTY_MAP
inline fun Short2IntMap.synchronize(): Short2IntMap = Short2IntMaps.synchronize(this)
inline fun Short2IntMap.freeze(): @UnmodifiableView Short2IntMap = Short2IntMaps.unmodifiable(this)
inline fun Short2IntMap.toMutableObjectList(): ObjectList<Pair<Short, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2IntMap.toObjectList(): ObjectList<Pair<Short, Int>> = toMutableObjectList().freeze()

/** Short2LongMap **/
inline fun mutableShort2LongMapOf(vararg pairs: Pair<Short, Long>) =
    Short2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2LongMapOf() = Short2LongOpenHashMap()
inline fun mutableShort2LongMapOf(capacity: Int) = Short2LongOpenHashMap(capacity)
inline fun short2LongMapOf(vararg pairs: Pair<Short, Long>) = mutableShort2LongMapOf(*pairs).freeze()
inline fun short2LongMapOf() = emptyShort2LongMap()
inline fun emptyShort2LongMap(): @Unmodifiable Short2LongMap = Short2LongMaps.EMPTY_MAP
inline fun Short2LongMap.synchronize(): Short2LongMap = Short2LongMaps.synchronize(this)
inline fun Short2LongMap.freeze(): @UnmodifiableView Short2LongMap = Short2LongMaps.unmodifiable(this)
inline fun Short2LongMap.toMutableObjectList(): ObjectList<Pair<Short, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2LongMap.toObjectList(): ObjectList<Pair<Short, Long>> = toMutableObjectList().freeze()

/** Short2FloatMap **/
inline fun mutableShort2FloatMapOf(vararg pairs: Pair<Short, Float>) =
    Short2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2FloatMapOf() = Short2FloatOpenHashMap()
inline fun mutableShort2FloatMapOf(capacity: Int) = Short2FloatOpenHashMap(capacity)
inline fun short2FloatMapOf(vararg pairs: Pair<Short, Float>) = mutableShort2FloatMapOf(*pairs).freeze()
inline fun short2FloatMapOf() = emptyShort2FloatMap()
inline fun emptyShort2FloatMap(): @Unmodifiable Short2FloatMap = Short2FloatMaps.EMPTY_MAP
inline fun Short2FloatMap.synchronize(): Short2FloatMap = Short2FloatMaps.synchronize(this)
inline fun Short2FloatMap.freeze(): @UnmodifiableView Short2FloatMap = Short2FloatMaps.unmodifiable(this)
inline fun Short2FloatMap.toMutableObjectList(): ObjectList<Pair<Short, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2FloatMap.toObjectList(): ObjectList<Pair<Short, Float>> = toMutableObjectList().freeze()

/** Short2DoubleMap **/
inline fun mutableShort2DoubleMapOf(vararg pairs: Pair<Short, Double>) =
    Short2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableShort2DoubleMapOf() = Short2DoubleOpenHashMap()
inline fun mutableShort2DoubleMapOf(capacity: Int) = Short2DoubleOpenHashMap(capacity)
inline fun short2DoubleMapOf(vararg pairs: Pair<Short, Double>) = mutableShort2DoubleMapOf(*pairs).freeze()
inline fun short2DoubleMapOf() = emptyShort2DoubleMap()
inline fun emptyShort2DoubleMap(): @Unmodifiable Short2DoubleMap = Short2DoubleMaps.EMPTY_MAP
inline fun Short2DoubleMap.synchronize(): Short2DoubleMap = Short2DoubleMaps.synchronize(this)
inline fun Short2DoubleMap.freeze(): @UnmodifiableView Short2DoubleMap = Short2DoubleMaps.unmodifiable(this)
inline fun Short2DoubleMap.toMutableObjectList(): ObjectList<Pair<Short, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Short2DoubleMap.toObjectList(): ObjectList<Pair<Short, Double>> = toMutableObjectList().freeze()

/** Short2ObjectMap **/
inline fun <V> mutableShort2ObjectMapOf(vararg pairs: Pair<Short, V>) =
    Short2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableShort2ObjectMapOf() = Short2ObjectOpenHashMap<V>()
inline fun <V> mutableShort2ObjectMapOf(capacity: Int) = Short2ObjectOpenHashMap<V>(capacity)
inline fun <V> short2ObjectMapOf(vararg pairs: Pair<Short, V>) = mutableShort2ObjectMapOf(*pairs).freeze()
inline fun <V> short2ObjectMapOf() = emptyShort2ObjectMap<V>()
inline fun <V> emptyShort2ObjectMap(): @Unmodifiable Short2ObjectMap<V> = Short2ObjectMaps.emptyMap()
inline fun <V> Short2ObjectMap<V>.synchronize(): Short2ObjectMap<V> = Short2ObjectMaps.synchronize(this)
inline fun <V> Short2ObjectMap<V>.freeze(): @UnmodifiableView Short2ObjectMap<V> = Short2ObjectMaps.unmodifiable(this)
inline fun <V> Short2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Short, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Short, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Short2ObjectMap<V>.toObjectList(): ObjectList<Pair<Short, V>> = toMutableObjectList().freeze()

// =====================================================
// 5. LONG2* MAP EXTENSIONS
// (Using it.unimi.dsi.fastutil.longs.*)
// =====================================================

/** Long2BooleanMap **/
inline fun mutableLong2BooleanMapOf(vararg pairs: Pair<Long, Boolean>) =
    Long2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2BooleanMapOf() = Long2BooleanOpenHashMap()
inline fun mutableLong2BooleanMapOf(capacity: Int) = Long2BooleanOpenHashMap(capacity)
inline fun long2BooleanMapOf(vararg pairs: Pair<Long, Boolean>) = mutableLong2BooleanMapOf(*pairs).freeze()
inline fun long2BooleanMapOf() = emptyLong2BooleanMap()
inline fun emptyLong2BooleanMap(): @Unmodifiable Long2BooleanMap = Long2BooleanMaps.EMPTY_MAP
inline fun Long2BooleanMap.synchronize(): Long2BooleanMap = Long2BooleanMaps.synchronize(this)
inline fun Long2BooleanMap.freeze(): @UnmodifiableView Long2BooleanMap = Long2BooleanMaps.unmodifiable(this)
inline fun Long2BooleanMap.toMutableObjectList(): ObjectList<Pair<Long, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2BooleanMap.toObjectList(): ObjectList<Pair<Long, Boolean>> = toMutableObjectList().freeze()

/** Long2ByteMap **/
inline fun mutableLong2ByteMapOf(vararg pairs: Pair<Long, Byte>) =
    Long2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2ByteMapOf() = Long2ByteOpenHashMap()
inline fun mutableLong2ByteMapOf(capacity: Int) = Long2ByteOpenHashMap(capacity)
inline fun long2ByteMapOf(vararg pairs: Pair<Long, Byte>) = mutableLong2ByteMapOf(*pairs).freeze()
inline fun long2ByteMapOf() = emptyLong2ByteMap()
inline fun emptyLong2ByteMap(): @Unmodifiable Long2ByteMap = Long2ByteMaps.EMPTY_MAP
inline fun Long2ByteMap.synchronize(): Long2ByteMap = Long2ByteMaps.synchronize(this)
inline fun Long2ByteMap.freeze(): @UnmodifiableView Long2ByteMap = Long2ByteMaps.unmodifiable(this)
inline fun Long2ByteMap.toMutableObjectList(): ObjectList<Pair<Long, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2ByteMap.toObjectList(): ObjectList<Pair<Long, Byte>> = toMutableObjectList().freeze()

/** Long2CharMap **/
inline fun mutableLong2CharMapOf(vararg pairs: Pair<Long, Char>) =
    Long2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2CharMapOf() = Long2CharOpenHashMap()
inline fun mutableLong2CharMapOf(capacity: Int) = Long2CharOpenHashMap(capacity)
inline fun long2CharMapOf(vararg pairs: Pair<Long, Char>) = mutableLong2CharMapOf(*pairs).freeze()
inline fun long2CharMapOf() = emptyLong2CharMap()
inline fun emptyLong2CharMap(): @Unmodifiable Long2CharMap = Long2CharMaps.EMPTY_MAP
inline fun Long2CharMap.synchronize(): Long2CharMap = Long2CharMaps.synchronize(this)
inline fun Long2CharMap.freeze(): @UnmodifiableView Long2CharMap = Long2CharMaps.unmodifiable(this)
inline fun Long2CharMap.toMutableObjectList(): ObjectList<Pair<Long, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2CharMap.toObjectList(): ObjectList<Pair<Long, Char>> = toMutableObjectList().freeze()

/** Long2ShortMap **/
inline fun mutableLong2ShortMapOf(vararg pairs: Pair<Long, Short>) =
    Long2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2ShortMapOf() = Long2ShortOpenHashMap()
inline fun mutableLong2ShortMapOf(capacity: Int) = Long2ShortOpenHashMap(capacity)
inline fun long2ShortMapOf(vararg pairs: Pair<Long, Short>) = mutableLong2ShortMapOf(*pairs).freeze()
inline fun long2ShortMapOf() = emptyLong2ShortMap()
inline fun emptyLong2ShortMap(): @Unmodifiable Long2ShortMap = Long2ShortMaps.EMPTY_MAP
inline fun Long2ShortMap.synchronize(): Long2ShortMap = Long2ShortMaps.synchronize(this)
inline fun Long2ShortMap.freeze(): @UnmodifiableView Long2ShortMap = Long2ShortMaps.unmodifiable(this)
inline fun Long2ShortMap.toMutableObjectList(): ObjectList<Pair<Long, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2ShortMap.toObjectList(): ObjectList<Pair<Long, Short>> = toMutableObjectList().freeze()

/** Long2IntMap **/
inline fun mutableLong2IntMapOf(vararg pairs: Pair<Long, Int>) =
    Long2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2IntMapOf() = Long2IntOpenHashMap()
inline fun mutableLong2IntMapOf(capacity: Int) = Long2IntOpenHashMap(capacity)
inline fun long2IntMapOf(vararg pairs: Pair<Long, Int>) = mutableLong2IntMapOf(*pairs).freeze()
inline fun long2IntMapOf() = emptyLong2IntMap()
inline fun emptyLong2IntMap(): @Unmodifiable Long2IntMap = Long2IntMaps.EMPTY_MAP
inline fun Long2IntMap.synchronize(): Long2IntMap = Long2IntMaps.synchronize(this)
inline fun Long2IntMap.freeze(): @UnmodifiableView Long2IntMap = Long2IntMaps.unmodifiable(this)
inline fun Long2IntMap.toMutableObjectList(): ObjectList<Pair<Long, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2IntMap.toObjectList(): ObjectList<Pair<Long, Int>> = toMutableObjectList().freeze()

/** Long2LongMap **/
inline fun mutableLong2LongMapOf(vararg pairs: Pair<Long, Long>) =
    Long2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2LongMapOf() = Long2LongOpenHashMap()
inline fun mutableLong2LongMapOf(capacity: Int) = Long2LongOpenHashMap(capacity)
inline fun long2LongMapOf(vararg pairs: Pair<Long, Long>) = mutableLong2LongMapOf(*pairs).freeze()
inline fun long2LongMapOf() = emptyLong2LongMap()
inline fun emptyLong2LongMap(): @Unmodifiable Long2LongMap = Long2LongMaps.EMPTY_MAP
inline fun Long2LongMap.synchronize(): Long2LongMap = Long2LongMaps.synchronize(this)
inline fun Long2LongMap.freeze(): @UnmodifiableView Long2LongMap = Long2LongMaps.unmodifiable(this)
inline fun Long2LongMap.toMutableObjectList(): ObjectList<Pair<Long, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2LongMap.toObjectList(): ObjectList<Pair<Long, Long>> = toMutableObjectList().freeze()

/** Long2FloatMap **/
inline fun mutableLong2FloatMapOf(vararg pairs: Pair<Long, Float>) =
    Long2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2FloatMapOf() = Long2FloatOpenHashMap()
inline fun mutableLong2FloatMapOf(capacity: Int) = Long2FloatOpenHashMap(capacity)
inline fun long2FloatMapOf(vararg pairs: Pair<Long, Float>) = mutableLong2FloatMapOf(*pairs).freeze()
inline fun long2FloatMapOf() = emptyLong2FloatMap()
inline fun emptyLong2FloatMap(): @Unmodifiable Long2FloatMap = Long2FloatMaps.EMPTY_MAP
inline fun Long2FloatMap.synchronize(): Long2FloatMap = Long2FloatMaps.synchronize(this)
inline fun Long2FloatMap.freeze(): @UnmodifiableView Long2FloatMap = Long2FloatMaps.unmodifiable(this)
inline fun Long2FloatMap.toMutableObjectList(): ObjectList<Pair<Long, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2FloatMap.toObjectList(): ObjectList<Pair<Long, Float>> = toMutableObjectList().freeze()

/** Long2DoubleMap **/
inline fun mutableLong2DoubleMapOf(vararg pairs: Pair<Long, Double>) =
    Long2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableLong2DoubleMapOf() = Long2DoubleOpenHashMap()
inline fun mutableLong2DoubleMapOf(capacity: Int) = Long2DoubleOpenHashMap(capacity)
inline fun long2DoubleMapOf(vararg pairs: Pair<Long, Double>) = mutableLong2DoubleMapOf(*pairs).freeze()
inline fun long2DoubleMapOf() = emptyLong2DoubleMap()
inline fun emptyLong2DoubleMap(): @Unmodifiable Long2DoubleMap = Long2DoubleMaps.EMPTY_MAP
inline fun Long2DoubleMap.synchronize(): Long2DoubleMap = Long2DoubleMaps.synchronize(this)
inline fun Long2DoubleMap.freeze(): @UnmodifiableView Long2DoubleMap = Long2DoubleMaps.unmodifiable(this)
inline fun Long2DoubleMap.toMutableObjectList(): ObjectList<Pair<Long, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Long2DoubleMap.toObjectList(): ObjectList<Pair<Long, Double>> = toMutableObjectList().freeze()

/** Long2ObjectMap **/
inline fun <V> mutableLong2ObjectMapOf(vararg pairs: Pair<Long, V>) =
    Long2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableLong2ObjectMapOf() = Long2ObjectOpenHashMap<V>()
inline fun <V> mutableLong2ObjectMapOf(capacity: Int) = Long2ObjectOpenHashMap<V>(capacity)
inline fun <V> long2ObjectMapOf(vararg pairs: Pair<Long, V>) = mutableLong2ObjectMapOf(*pairs).freeze()
inline fun <V> long2ObjectMapOf() = emptyLong2ObjectMap<V>()
inline fun <V> emptyLong2ObjectMap(): @Unmodifiable Long2ObjectMap<V> = Long2ObjectMaps.emptyMap()
inline fun <V> Long2ObjectMap<V>.synchronize(): Long2ObjectMap<V> = Long2ObjectMaps.synchronize(this)
inline fun <V> Long2ObjectMap<V>.freeze(): @UnmodifiableView Long2ObjectMap<V> = Long2ObjectMaps.unmodifiable(this)
inline fun <V> Long2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Long, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Long, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Long2ObjectMap<V>.toObjectList(): ObjectList<Pair<Long, V>> = toMutableObjectList().freeze()

// =====================================================
// 6. FLOAT2* MAP EXTENSIONS
// (Using it.unimi.dsi.fastutil.floats.*)
// =====================================================

/** Float2BooleanMap **/
inline fun mutableFloat2BooleanMapOf(vararg pairs: Pair<Float, Boolean>) =
    Float2BooleanOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2BooleanMapOf() = Float2BooleanOpenHashMap()
inline fun mutableFloat2BooleanMapOf(capacity: Int) = Float2BooleanOpenHashMap(capacity)
inline fun float2BooleanMapOf(vararg pairs: Pair<Float, Boolean>) = mutableFloat2BooleanMapOf(*pairs).freeze()
inline fun float2BooleanMapOf() = emptyFloat2BooleanMap()
inline fun emptyFloat2BooleanMap(): @Unmodifiable Float2BooleanMap = Float2BooleanMaps.EMPTY_MAP
inline fun Float2BooleanMap.synchronize(): Float2BooleanMap = Float2BooleanMaps.synchronize(this)
inline fun Float2BooleanMap.freeze(): @UnmodifiableView Float2BooleanMap = Float2BooleanMaps.unmodifiable(this)
inline fun Float2BooleanMap.toMutableObjectList(): ObjectList<Pair<Float, Boolean>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Boolean>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2BooleanMap.toObjectList(): ObjectList<Pair<Float, Boolean>> = toMutableObjectList().freeze()

/** Float2ByteMap **/
inline fun mutableFloat2ByteMapOf(vararg pairs: Pair<Float, Byte>) =
    Float2ByteOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2ByteMapOf() = Float2ByteOpenHashMap()
inline fun mutableFloat2ByteMapOf(capacity: Int) = Float2ByteOpenHashMap(capacity)
inline fun float2ByteMapOf(vararg pairs: Pair<Float, Byte>) = mutableFloat2ByteMapOf(*pairs).freeze()
inline fun float2ByteMapOf() = emptyFloat2ByteMap()
inline fun emptyFloat2ByteMap(): @Unmodifiable Float2ByteMap = Float2ByteMaps.EMPTY_MAP
inline fun Float2ByteMap.synchronize(): Float2ByteMap = Float2ByteMaps.synchronize(this)
inline fun Float2ByteMap.freeze(): @UnmodifiableView Float2ByteMap = Float2ByteMaps.unmodifiable(this)
inline fun Float2ByteMap.toMutableObjectList(): ObjectList<Pair<Float, Byte>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Byte>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2ByteMap.toObjectList(): ObjectList<Pair<Float, Byte>> = toMutableObjectList().freeze()

/** Float2CharMap **/
inline fun mutableFloat2CharMapOf(vararg pairs: Pair<Float, Char>) =
    Float2CharOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2CharMapOf() = Float2CharOpenHashMap()
inline fun mutableFloat2CharMapOf(capacity: Int) = Float2CharOpenHashMap(capacity)
inline fun float2CharMapOf(vararg pairs: Pair<Float, Char>) = mutableFloat2CharMapOf(*pairs).freeze()
inline fun float2CharMapOf() = emptyFloat2CharMap()
inline fun emptyFloat2CharMap(): @Unmodifiable Float2CharMap = Float2CharMaps.EMPTY_MAP
inline fun Float2CharMap.synchronize(): Float2CharMap = Float2CharMaps.synchronize(this)
inline fun Float2CharMap.freeze(): @UnmodifiableView Float2CharMap = Float2CharMaps.unmodifiable(this)
inline fun Float2CharMap.toMutableObjectList(): ObjectList<Pair<Float, Char>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Char>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2CharMap.toObjectList(): ObjectList<Pair<Float, Char>> = toMutableObjectList().freeze()

/** Float2ShortMap **/
inline fun mutableFloat2ShortMapOf(vararg pairs: Pair<Float, Short>) =
    Float2ShortOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2ShortMapOf() = Float2ShortOpenHashMap()
inline fun mutableFloat2ShortMapOf(capacity: Int) = Float2ShortOpenHashMap(capacity)
inline fun float2ShortMapOf(vararg pairs: Pair<Float, Short>) = mutableFloat2ShortMapOf(*pairs).freeze()
inline fun float2ShortMapOf() = emptyFloat2ShortMap()
inline fun emptyFloat2ShortMap(): @Unmodifiable Float2ShortMap = Float2ShortMaps.EMPTY_MAP
inline fun Float2ShortMap.synchronize(): Float2ShortMap = Float2ShortMaps.synchronize(this)
inline fun Float2ShortMap.freeze(): @UnmodifiableView Float2ShortMap = Float2ShortMaps.unmodifiable(this)
inline fun Float2ShortMap.toMutableObjectList(): ObjectList<Pair<Float, Short>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Short>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2ShortMap.toObjectList(): ObjectList<Pair<Float, Short>> = toMutableObjectList().freeze()

/** Float2IntMap **/
inline fun mutableFloat2IntMapOf(vararg pairs: Pair<Float, Int>) =
    Float2IntOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2IntMapOf() = Float2IntOpenHashMap()
inline fun mutableFloat2IntMapOf(capacity: Int) = Float2IntOpenHashMap(capacity)
inline fun float2IntMapOf(vararg pairs: Pair<Float, Int>) = mutableFloat2IntMapOf(*pairs).freeze()
inline fun float2IntMapOf() = emptyFloat2IntMap()
inline fun emptyFloat2IntMap(): @Unmodifiable Float2IntMap = Float2IntMaps.EMPTY_MAP
inline fun Float2IntMap.synchronize(): Float2IntMap = Float2IntMaps.synchronize(this)
inline fun Float2IntMap.freeze(): @UnmodifiableView Float2IntMap = Float2IntMaps.unmodifiable(this)
inline fun Float2IntMap.toMutableObjectList(): ObjectList<Pair<Float, Int>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Int>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2IntMap.toObjectList(): ObjectList<Pair<Float, Int>> = toMutableObjectList().freeze()

/** Float2LongMap **/
inline fun mutableFloat2LongMapOf(vararg pairs: Pair<Float, Long>) =
    Float2LongOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2LongMapOf() = Float2LongOpenHashMap()
inline fun mutableFloat2LongMapOf(capacity: Int) = Float2LongOpenHashMap(capacity)
inline fun float2LongMapOf(vararg pairs: Pair<Float, Long>) = mutableFloat2LongMapOf(*pairs).freeze()
inline fun float2LongMapOf() = emptyFloat2LongMap()
inline fun emptyFloat2LongMap(): @Unmodifiable Float2LongMap = Float2LongMaps.EMPTY_MAP
inline fun Float2LongMap.synchronize(): Float2LongMap = Float2LongMaps.synchronize(this)
inline fun Float2LongMap.freeze(): @UnmodifiableView Float2LongMap = Float2LongMaps.unmodifiable(this)
inline fun Float2LongMap.toMutableObjectList(): ObjectList<Pair<Float, Long>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Long>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2LongMap.toObjectList(): ObjectList<Pair<Float, Long>> = toMutableObjectList().freeze()

/** Float2FloatMap **/
inline fun mutableFloat2FloatMapOf(vararg pairs: Pair<Float, Float>) =
    Float2FloatOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2FloatMapOf() = Float2FloatOpenHashMap()
inline fun mutableFloat2FloatMapOf(capacity: Int) = Float2FloatOpenHashMap(capacity)
inline fun float2FloatMapOf(vararg pairs: Pair<Float, Float>) = mutableFloat2FloatMapOf(*pairs).freeze()
inline fun float2FloatMapOf() = emptyFloat2FloatMap()
inline fun emptyFloat2FloatMap(): @Unmodifiable Float2FloatMap = Float2FloatMaps.EMPTY_MAP
inline fun Float2FloatMap.synchronize(): Float2FloatMap = Float2FloatMaps.synchronize(this)
inline fun Float2FloatMap.freeze(): @UnmodifiableView Float2FloatMap = Float2FloatMaps.unmodifiable(this)
inline fun Float2FloatMap.toMutableObjectList(): ObjectList<Pair<Float, Float>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Float>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2FloatMap.toObjectList(): ObjectList<Pair<Float, Float>> = toMutableObjectList().freeze()

/** Float2DoubleMap **/
inline fun mutableFloat2DoubleMapOf(vararg pairs: Pair<Float, Double>) =
    Float2DoubleOpenHashMap(pairs.size).apply { putAll(pairs) }
inline fun mutableFloat2DoubleMapOf() = Float2DoubleOpenHashMap()
inline fun mutableFloat2DoubleMapOf(capacity: Int) = Float2DoubleOpenHashMap(capacity)
inline fun float2DoubleMapOf(vararg pairs: Pair<Float, Double>) = mutableFloat2DoubleMapOf(*pairs).freeze()
inline fun float2DoubleMapOf() = emptyFloat2DoubleMap()
inline fun emptyFloat2DoubleMap(): @Unmodifiable Float2DoubleMap = Float2DoubleMaps.EMPTY_MAP
inline fun Float2DoubleMap.synchronize(): Float2DoubleMap = Float2DoubleMaps.synchronize(this)
inline fun Float2DoubleMap.freeze(): @UnmodifiableView Float2DoubleMap = Float2DoubleMaps.unmodifiable(this)
inline fun Float2DoubleMap.toMutableObjectList(): ObjectList<Pair<Float, Double>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, Double>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun Float2DoubleMap.toObjectList(): ObjectList<Pair<Float, Double>> = toMutableObjectList().freeze()

/** Float2ObjectMap **/
inline fun <V> mutableFloat2ObjectMapOf(vararg pairs: Pair<Float, V>) =
    Float2ObjectOpenHashMap<V>(pairs.size).apply { putAll(pairs) }
inline fun <V> mutableFloat2ObjectMapOf() = Float2ObjectOpenHashMap<V>()
inline fun <V> mutableFloat2ObjectMapOf(capacity: Int) = Float2ObjectOpenHashMap<V>(capacity)
inline fun <V> float2ObjectMapOf(vararg pairs: Pair<Float, V>) = mutableFloat2ObjectMapOf(*pairs).freeze()
inline fun <V> float2ObjectMapOf() = emptyFloat2ObjectMap<V>()
inline fun <V> emptyFloat2ObjectMap(): @Unmodifiable Float2ObjectMap<V> = Float2ObjectMaps.emptyMap()
inline fun <V> Float2ObjectMap<V>.synchronize(): Float2ObjectMap<V> = Float2ObjectMaps.synchronize(this)
inline fun <V> Float2ObjectMap<V>.freeze(): @UnmodifiableView Float2ObjectMap<V> = Float2ObjectMaps.unmodifiable(this)
inline fun <V> Float2ObjectMap<V>.toMutableObjectList(): ObjectList<Pair<Float, V>> {
    if (isEmpty()) return emptyObjectList()
    val list = ObjectArrayList<Pair<Float, V>>(size)
    forEach { k, v -> list.add(k to v) }
    return list
}
inline fun <V> Float2ObjectMap<V>.toObjectList(): ObjectList<Pair<Float, V>> = toMutableObjectList().freeze()
// endregion