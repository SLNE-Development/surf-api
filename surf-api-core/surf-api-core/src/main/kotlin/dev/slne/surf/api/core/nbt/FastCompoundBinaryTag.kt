package dev.slne.surf.api.core.nbt

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.jetbrains.annotations.UnmodifiableView

/**
 * A mutable, high-performance implementation of CompoundBinaryTag backed by fastutil collections.
 *
 * This interface extends CompoundBinaryTag with additional mutation capabilities and optimized
 * iteration through fastutil's specialized collection types.
 *
 * **Important:** This implementation violates the immutability principle of CompoundBinaryTag.
 * All operations (put, remove, etc.) mutate the tag directly, unlike the standard CompoundBinaryTag
 * which is immutable and returns a new tag for each operation.
 */
@Suppress("NonExtendableApiUsage")
interface FastCompoundBinaryTag : CompoundBinaryTag {

    /**
     * Removes all key-value mappings from this compound tag.
     */
    fun clear()


    /**
     * Returns an optimized set view of the keys contained in this compound tag.
     *
     * @return An ObjectSet providing efficient key iteration
     */
    override fun keySet(): @UnmodifiableView ObjectSet<String>

    /**
     * Returns an optimized iterator over the entries in this compound tag.
     *
     * @return An ObjectIterator for efficient entry traversal
     */
    override fun iterator(): ObjectIterator<Object2ObjectMap.Entry<String, BinaryTag>>
}

/**
 * Wraps this CompoundBinaryTag in a mutable FastCompoundBinaryTag for improved performance.
 *
 * The returned tag is mutable and all operations modify the tag directly, unlike the immutable
 * CompoundBinaryTag interface.
 *
 * @param synchronize If true, wraps the underlying map with synchronization for thread-safe access
 * @return A mutable FastCompoundBinaryTag backed by fastutil collections
 */
fun CompoundBinaryTag.fast(synchronize: Boolean = false) =
    InternalNbtBridge.wrapCompoundBinaryTag(this, synchronize)

/**
 * Builds and wraps the result in a mutable FastCompoundBinaryTag.
 *
 * @param synchronize If true, wraps the underlying map with synchronization for thread-safe access
 * @return A mutable FastCompoundBinaryTag backed by fastutil collections
 */
fun CompoundBinaryTag.Builder.buildFast(synchronize: Boolean = false) = build().fast(synchronize)