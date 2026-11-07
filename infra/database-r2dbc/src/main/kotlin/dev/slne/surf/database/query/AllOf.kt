package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.compoundAnd

/**
 * Combines the non-null [conditions] with `AND`, or returns `TRUE` if all of them are `null`.
 *
 * Unlike Exposed's `compoundAnd`, it accepts an empty or all-`null` list; unlike `andIfNotNull`, it needs no non-null
 * first condition. A single remaining condition is returned unchanged.
 */
fun allOf(vararg conditions: Op<Boolean>?): Op<Boolean> = allOfOrNull(*conditions) ?: Op.TRUE

/**
 * Combines the non-null [conditions] with `AND`, or returns `null` if all of them are `null`.
 *
 * A single remaining condition is returned unchanged.
 */
fun allOfOrNull(vararg conditions: Op<Boolean>?): Op<Boolean>? =
    conditions.filterNotNull().takeIf { it.isNotEmpty() }?.compoundAnd()
