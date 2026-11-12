package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.fixtures.FindPlayerResponse
import dev.slne.surf.nats.js.fixtures.FindPlayerResponseSchema
import js.objects.unsafeJso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AnswerTest {

    @Test
    fun answerObjectsHaveTheTsShape() {
        val message = create(FindPlayerResponseSchema, unsafeJso<FindPlayerResponse> { name = "v" })
        val known = Answer(known = true, value = message)
        assertEquals(true, known.known)
        assertSame(message, known.value)
        assertEquals(listOf("known", "value"), JsObject.keys(known).toList())

        val unknown = Answer(known = false)
        assertEquals(false, unknown.known)
        assertEquals(listOf("known"), JsObject.keys(unknown).toList())
    }
}
