package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.compoundOr

/**
 * Combines the non-null [conditions] with `OR`, or returns `FALSE` if all of them are `null`.
 *
 * Unlike Exposed's `compoundOr`, it accepts an empty or all-`null` list; unlike `orIfNotNull`, it needs no non-null
 * first condition. A single remaining condition is returned unchanged.
 */
fun anyOf(vararg conditions: Op<Boolean>?): Op<Boolean> = anyOfOrNull(*conditions) ?: Op.FALSE

/**
 * Combines the non-null [conditions] with `OR`, or returns `null` if all of them are `null`.
 *
 * A single remaining condition is returned unchanged.
 */
fun anyOfOrNull(vararg conditions: Op<Boolean>?): Op<Boolean>? =
    conditions.filterNotNull().takeIf { it.isNotEmpty() }?.compoundOr()
