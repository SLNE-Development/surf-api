package dev.slne.surf.nats.js

import js.objects.unsafeJso
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private external interface ThenInit {
    var then: Any?
}

class ThenableTest {

    @Test
    fun promisesAndObjectsWithACallableThenAreThenables() {
        assertTrue(isThenable(Promise.resolve(1)))
        assertTrue(isThenable(unsafeJso<ThenInit> { then = { _: Any? -> } }))
    }

    @Test
    fun otherValuesAreNoThenables() {
        assertFalse(isThenable(null))
        assertFalse(isThenable(undefined))
        assertFalse(isThenable(1))
        assertFalse(isThenable("then"))
        assertFalse(isThenable(Unit))
        assertFalse(isThenable({ }))
        assertFalse(isThenable(unsafeJso<ThenInit> { then = 1 }))
    }
}
