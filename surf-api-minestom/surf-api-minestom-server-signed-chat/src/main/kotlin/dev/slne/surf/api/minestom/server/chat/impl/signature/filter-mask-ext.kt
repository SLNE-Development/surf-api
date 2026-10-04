package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.minestom.server.crypto.FilterMask
import java.util.*

internal val FILTER_MASK_PASS_THROUGH: FilterMask = FilterMask(FilterMask.Type.PASS_THROUGH, BitSet(0))

internal val FILTER_MASK_FULLY_FILTERED: FilterMask = FilterMask(FilterMask.Type.FULLY_FILTERED, BitSet(0))

internal fun FilterMask.isPassThrough(): Boolean = type() == FilterMask.Type.PASS_THROUGH

internal fun FilterMask.isFullyFiltered(): Boolean = type() == FilterMask.Type.FULLY_FILTERED
