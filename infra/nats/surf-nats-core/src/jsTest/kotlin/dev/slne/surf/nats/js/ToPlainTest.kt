package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.fixtures.PlainNestedSchema
import dev.slne.surf.nats.js.fixtures.PlainSampleData
import dev.slne.surf.nats.js.fixtures.PlainSampleSchema
import dev.slne.surf.nats.js.fixtures.PlainWellKnownData
import dev.slne.surf.nats.js.fixtures.PlainWellKnownSchema
import dev.slne.surf.nats.js.fixtures.TimestampSchema
import dev.slne.surf.nats.js.fixtures.ValueSchema
import dev.slne.surf.nats.js.fixtures.fromJsonString
import js.date.Date
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.slne.surf.nats.js.fixtures.facade.toPlain as facadeToPlain

class ToPlainTest {

    private fun sample(json: String) = fromJsonString(PlainSampleSchema, json)

    private fun plainJson(json: String): String = JSON.stringify(toPlain(PlainSampleSchema, sample(json)))

    @Test
    fun setFieldsBecomePlainValues() {
        val json = """
            {"name":"Steve","nickname":"","level":0,"balance":"9007199254740991","total":"42",
             "delta":"-9007199254740991","history":["1","-2"],"labels":{"7":"seven"},"scores":{"a":"3"},
             "active":false,"createdAt":"2026-11-06T12:00:00.123456789Z","id":{"value":"$UUID_BASE64"},
             "visits":["1970-01-01T00:00:01Z"],"owners":{"x":{"value":"$UUID_BASE64"}},
             "nested":{"note":"n","parent":{}},"children":[{}],"color":"PLAIN_COLOR_RED",
             "colors":["PLAIN_COLOR_UNSPECIFIED"],"group":{"note":"g"}}
        """.trimIndent()

        val expected = """
            {"name":"Steve","nickname":"","level":0,"balance":9007199254740991,"total":42,
            "delta":-9007199254740991,"history":[1,-2],"labels":{"7":"seven"},"scores":{"a":3},"data":null,
            "active":false,"createdAt":"2026-11-06T12:00:00.123Z","id":"$UUID",
            "visits":["1970-01-01T00:00:01.000Z"],"owners":{"x":"$UUID"},
            "nested":{"note":"n","parent":{"note":null,"parent":null}},"children":[{"note":null,"parent":null}],
            "color":1,"colors":[0],"target":{"case":"group","value":{"note":"g","parent":null}}}
        """.trimIndent().replace("\n", "")
        assertEquals(expected, plainJson(json))
    }

    @Test
    fun unsetFieldsAreNullInsteadOfTheirDefaults() {
        val expected = """
            {"name":null,"nickname":null,"level":null,"balance":null,"total":null,"delta":null,"history":[],
            "labels":{},"scores":{},"data":null,"active":null,"createdAt":null,"id":null,"visits":[],"owners":{},
            "nested":null,"children":[],"color":null,"colors":[],"target":{"case":null,"value":null}}
        """.trimIndent().replace("\n", "")

        assertEquals(expected, JSON.stringify(toPlain(PlainSampleSchema, create(PlainSampleSchema))))
    }

    @Test
    fun aScalarOneofCarriesItsConvertedValue() {
        val plain = plainJson("""{"name":"x","ticket":"-5"}""")
        assertTrue(plain.endsWith(""""target":{"case":"ticket","value":-5}}"""), plain)
    }

    @Test
    fun bytesStayUint8Arrays() {
        val plain = toPlain(PlainSampleSchema, sample("""{"name":"x","data":"AQI="}""")).unsafeCast<PlainSampleData>()
        assertContentEquals(byteArrayOf(1, 2), assertIs<Uint8Array<*>>(plain.data).toByteArray())
    }

    @Test
    fun a64BitIntegerOutsideTheSafeRangeIsRejected() {
        for (json in listOf(
            """{"name":"x","balance":"9007199254740992"}""",
            """{"name":"x","delta":"-9007199254740992"}""",
            """{"name":"x","total":"18446744073709551615"}""",
            """{"name":"x","history":["1","9007199254740992"]}""",
        )) {
            val error = assertFails { plainJson(json) }
            assertTrue(error.message!!.contains("does not fit into a number"), error.message)
        }
    }

    @Test
    fun aTimestampBecomesADate() {
        val timestamp = fromJsonString(TimestampSchema, "\"1969-12-31T23:59:59.500Z\"")
        val date = assertIs<Date>(toPlain(TimestampSchema, timestamp))
        assertEquals(-500.0, date.getTime())
    }

    @Test
    fun aMessageOfAnotherSchemaIsRejected() {
        assertFailsWith<IllegalArgumentException> { toPlain(PlainNestedSchema, sample("""{"name":"x"}""")) }
    }

    @Test
    fun theFacadeExportsToPlain() {
        val plain = JSON.stringify(facadeToPlain(PlainNestedSchema, create(PlainNestedSchema)))
        assertEquals("""{"note":null,"parent":null}""", plain)
    }

    @Test
    fun wellKnownTypesBecomePlainValues() {
        val json = """
            {"label":"","count":"-7","total":"9007199254740991","level":0,"rank":2,"ratio":0.5,"scale":1.5,
             "flag":false,"tags":["a",""],"limits":{"k":"3"},"attributes":{"a":1,"b":[true,null,"s"],"c":{"d":null}},
             "setting":"text","entries":[1,{"x":[]}],"documents":[{},{"n":2}],"options":{"o":null,"p":[1]},
             "bigId":"-9223372036854775808","bigIds":["18446744073709551615","0"],"wrapped":"w"}
        """.trimIndent()

        val expected = """
            {"label":"","count":-7,"total":9007199254740991,"level":0,"rank":2,"ratio":0.5,"scale":1.5,"flag":false,
            "blob":null,"tags":["a",""],"limits":{"k":3},"attributes":{"a":1,"b":[true,null,"s"],"c":{"d":null}},
            "setting":"text","entries":[1,{"x":[]}],"documents":[{},{"n":2}],"options":{"o":null,"p":[1]},
            "bigId":"-9223372036854775808","bigIds":["18446744073709551615","0"],
            "choice":{"case":"wrapped","value":"w"}}
        """.trimIndent().replace("\n", "")
        assertEquals(expected, wellKnownJson(json))
    }

    @Test
    fun unsetWellKnownTypesAreNull() {
        val expected = """
            {"label":null,"count":null,"total":null,"level":null,"rank":null,"ratio":null,"scale":null,"flag":null,
            "blob":null,"tags":[],"limits":{},"attributes":null,"setting":null,"entries":null,"documents":[],
            "options":{},"bigId":null,"bigIds":[],"choice":{"case":null,"value":null}}
        """.trimIndent().replace("\n", "")

        assertEquals(expected, JSON.stringify(toPlain(PlainWellKnownSchema, create(PlainWellKnownSchema))))
    }

    @Test
    fun aValueOfKindNullIsNull() {
        val plain = wellKnownJson("""{"setting":null,"options":{"o":null}}""")
        assertTrue(plain.contains(""""setting":null,""") && plain.contains(""""options":{"o":null}"""), plain)
        assertNull(toPlain(ValueSchema, fromJsonString(ValueSchema, "null")))
        assertNull(toPlain(ValueSchema, create(ValueSchema)))
    }

    @Test
    fun aStructInAOneofBecomesAPlainObject() {
        val plain = wellKnownJson("""{"document":{"k":"v","n":[]}}""")
        assertTrue(plain.endsWith(""""choice":{"case":"document","value":{"k":"v","n":[]}}}"""), plain)
    }

    @Test
    fun aBytesWrapperBecomesAUint8Array() {
        val plain = toPlain(PlainWellKnownSchema, fromJsonString(PlainWellKnownSchema, """{"blob":"AQI="}"""))
        val blob = plain.unsafeCast<PlainWellKnownData>().blob
        assertContentEquals(byteArrayOf(1, 2), assertIs<Uint8Array<*>>(blob).toByteArray())
    }

    private fun wellKnownJson(json: String): String =
        JSON.stringify(toPlain(PlainWellKnownSchema, fromJsonString(PlainWellKnownSchema, json)))

    private companion object {
        const val UUID = "00112233-4455-6677-8899-aabbccddeeff"
        const val UUID_BASE64 = "ABEiM0RVZneImaq7zN3u/w=="
    }
}
