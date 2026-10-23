package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.external.protobuf.fromBinary
import dev.slne.surf.nats.js.external.protobuf.isMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.js.fixtures.Inner
import dev.slne.surf.nats.js.fixtures.InnerSchema
import dev.slne.surf.nats.js.fixtures.Location
import dev.slne.surf.nats.js.fixtures.OneofCase
import dev.slne.surf.nats.js.fixtures.Rules
import dev.slne.surf.nats.js.fixtures.RulesSchema
import dev.slne.surf.nats.js.fixtures.Uuid
import dev.slne.surf.nats.js.fixtures.fields
import dev.slne.surf.nats.protocol.ProtocolHeaders
import js.buffer.ArrayBuffer
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlin.js.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageRulesTest {

    // Smallest valid Rules: the non-null fields without default.
    private fun validInit(): Rules = unsafeJso {
        requiredText = "x"
        color = 1
    }

    private fun inner(init: Inner.() -> Unit): Inner = unsafeJso(init)

    private fun field(name: String) = RulesSchema.fields.first { it.localName == name }

    private fun isSet(message: ProtoMessage, name: String): Boolean = reflect(RulesSchema, message).isSet(field(name))

    private fun unknownFields(message: ProtoMessage) = reflect(RulesSchema, message).getUnknown()

    private fun violationOf(init: Rules): String? = MessageRules.violation(RulesSchema, create(RulesSchema, init))

    private fun roundTrip(init: Rules): ProtoMessage = fromBinary(RulesSchema, toBinary(RulesSchema, create(RulesSchema, init)))

    @Test
    fun prepareCreatesAMessageFromAnInitObject() {
        val message = MessageRules.prepare(RulesSchema, validInit())
        assertTrue(isMessage(message, RulesSchema))
        assertEquals("x", message.fields<Rules>().requiredText)
    }

    @Test
    fun prepareRejectsAMessageOfAnotherSchema() {
        val other = create(InnerSchema, inner { label = "l" })
        assertFailsWith<IllegalArgumentException> { MessageRules.prepare(RulesSchema, other) }
    }

    @Test
    fun missingRequiredFieldNamesThePath() {
        val error = assertFailsWith<IllegalArgumentException> {
            MessageRules.prepare(RulesSchema, unsafeJso<Rules> { color = 1 })
        }
        assertTrue(error.message!!.contains("'requiredText'"), error.message)
    }

    @Test
    fun nullableDefaultAndOneofFieldsAreOptional() {
        assertNull(violationOf(validInit()))
        assertTrue(isSet(create(RulesSchema, validInit()), "requiredText"))
    }

    @Test
    fun enumWithoutDefaultIsRequired() {
        val violation = violationOf(unsafeJso { requiredText = "x" })
        assertTrue(violation!!.contains("'color'"), violation)
    }

    @Test
    fun nestedMessagesAreChecked() {
        val init = validInit()
        init.inner = inner { count = 1 }
        assertTrue(violationOf(init)!!.contains("'inner.label'"))

        val list = validInit()
        list.inners = arrayOf(inner { label = "a" }, inner { })
        assertTrue(violationOf(list)!!.contains("'inners[1].label'"))

        val map = validInit()
        map.innerMap = json("k" to inner { })
        assertTrue(violationOf(map)!!.contains("'innerMap[k].label'"))

        val oneof = validInit()
        oneof.choice = unsafeJso<OneofCase> {
            case = "innerChoice"
            value = inner { }
        }
        assertTrue(violationOf(oneof)!!.contains("'innerChoice.label'"))
    }

    @Test
    fun wellKnownTypesAreNotChecked() {
        val init = validInit()
        init.at = unsafeJso { }
        assertNull(violationOf(init))
    }

    @Test
    fun uuidMustHaveSixteenBytes() {
        val missing = validInit()
        missing.uuid = unsafeJso { }
        assertTrue(violationOf(missing)!!.contains("'uuid.value'"))

        val short = validInit()
        short.uuid = unsafeJso<Uuid> { value = bytesOfSize(15) }
        assertTrue(violationOf(short)!!.contains("16 bytes"))

        val valid = validInit()
        valid.uuid = unsafeJso<Uuid> { value = bytesOfSize(16) }
        assertNull(violationOf(valid))
    }

    @Test
    fun locationWorldIsNullableAndCoordinatesAreRequired() {
        val valid = validInit()
        valid.location = unsafeJso<Location> {
            x = 1.0
            y = 2.0
            z = 3.0
            yaw = 0.0
            pitch = 0.0
        }
        assertNull(violationOf(valid))

        val missing = validInit()
        missing.location = unsafeJso<Location> {
            y = 2.0
            z = 3.0
            yaw = 0.0
            pitch = 0.0
        }
        assertTrue(violationOf(missing)!!.contains("'location.x'"))
    }

    @Test
    fun unknownEnumNumbersBecomeTheZeroEntry() {
        val init = validInit()
        init.color = 77
        init.nullableColor = 78
        init.colors = arrayOf(1, 77)
        init.colorMap = json("a" to 79, "b" to 2)
        init.choice = unsafeJso<OneofCase> {
            case = "colorChoice"
            value = 80
        }

        val decoded = roundTrip(init)
        val rules = decoded.fields<Rules>()
        assertEquals(0, rules.color)
        assertTrue(unknownFields(decoded) != null)

        assertNull(MessageRules.normalizeAndCheck(RulesSchema, decoded))
        assertEquals(0, rules.color)
        assertTrue(isSet(decoded, "color"))
        assertEquals(0, rules.nullableColor)
        assertTrue(isSet(decoded, "nullableColor"))
        assertEquals(listOf(1, 0), rules.colors!!.toList())
        assertEquals(0, rules.colorMap!!["a"])
        assertEquals(2, rules.colorMap!!["b"])
        assertEquals("colorChoice", rules.choice!!.case)
        assertEquals(0, rules.choice!!.value)
        assertEquals("undefined", jsTypeOf(unknownFields(decoded)))
        assertNull(MessageRules.violation(RulesSchema, decoded))
    }

    @Test
    fun knownEnumsAreKept() {
        val init = validInit()
        init.colors = arrayOf(1, 2)
        val decoded = roundTrip(init)
        assertNull(MessageRules.normalizeAndCheck(RulesSchema, decoded))
        val rules = decoded.fields<Rules>()
        assertEquals(1, rules.color)
        assertEquals(listOf(1, 2), rules.colors!!.toList())
        assertTrue(rules.nullableColor == 0 && !isSet(decoded, "nullableColor"))
    }

    @Test
    fun decodeRejectsAMissingRequiredField() {
        val bytes = toBinary(RulesSchema, create(RulesSchema, unsafeJso<Rules> { color = 1 }))
        val error = assertFailsWith<IllegalArgumentException> { decodeMessage(RulesSchema, bytes, ProtocolHeaders.PROTOBUF_CONTENT_TYPE, "test") }
        assertTrue(error.message!!.contains("'requiredText'"), error.message)
    }

    @Test
    fun defaultsAreAppliedWhenAbsent() {
        val decoded = decodeMessage(RulesSchema, toBinary(RulesSchema, create(RulesSchema, validInit())), ProtocolHeaders.PROTOBUF_CONTENT_TYPE, "test")
        assertEquals(7, decoded.fields<Rules>().defaulted)
        assertEquals("", decoded.fields<Rules>().nullableText)
        assertTrue(!isSet(decoded, "nullableText"))
    }

    @Test
    fun uint32AboveIntMaxRoundTrips() {
        val init = validInit()
        init.big = 3_000_000_000.0
        val decoded = decodeMessage(RulesSchema, toBinary(RulesSchema, create(RulesSchema, init)), ProtocolHeaders.PROTOBUF_CONTENT_TYPE, "test")
        assertEquals(3_000_000_000.0, decoded.fields<Rules>().big)
    }

    @Test
    fun unknownEnumOfAnUnsetOneofMemberOnlyClearsUnknown() {
        val init = validInit()
        init.choice = unsafeJso<OneofCase> {
            case = "textChoice"
            value = "t"
        }
        // Appends color_choice (field 9) = 81, an unknown number of the closed enum, after text_choice.
        val bytes = toBinary(RulesSchema, create(RulesSchema, init))
        val withUnknown = Uint8Array<ArrayBuffer>(bytes.length + 2)
        withUnknown.set(bytes)
        withUnknown.set(bytes(0x48, 81), bytes.length)
        val decoded = fromBinary(RulesSchema, withUnknown)
        assertNull(MessageRules.normalizeAndCheck(RulesSchema, decoded))
        assertEquals("textChoice", decoded.fields<Rules>().choice!!.case)
        assertEquals("undefined", jsTypeOf(unknownFields(decoded)))
    }
}
