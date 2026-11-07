package dev.slne.surf.database.query

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.junit.jupiter.api.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class ConditionsTest {

    private val condition: Op<Boolean> = intLiteral(1) eq 1

    @Test
    fun allOfFallsBackToTrue() {
        assertSame(Op.TRUE, allOf())
        assertSame(Op.TRUE, allOf(null, null))
        assertNull(allOfOrNull(null))
    }

    @Test
    fun anyOfFallsBackToFalse() {
        assertSame(Op.FALSE, anyOf())
        assertSame(Op.FALSE, anyOf(null, null))
        assertNull(anyOfOrNull(null))
    }

    @Test
    fun singleConditionIsReturnedUnchanged() {
        assertSame(condition, allOf(null, condition, null))
        assertSame(condition, anyOfOrNull(condition, null))
    }
}
