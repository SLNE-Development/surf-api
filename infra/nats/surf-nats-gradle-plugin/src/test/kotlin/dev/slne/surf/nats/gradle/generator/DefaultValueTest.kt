package dev.slne.surf.nats.gradle.generator

import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import com.squareup.wire.schema.Location
import dev.slne.surf.nats.gradle.model.DefaultValues
import dev.slne.surf.nats.gradle.model.RawDefaults
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * One `[default = ...]` vector per scalar type, parsed through the real wire loader.
 */
class DefaultValueTest {
    @TempDir
    lateinit var temp: File

    private fun generate(type: String, literal: String): String {
        return GeneratorHarness(temp).proto(
            "surf/test/v1/defaults.proto",
            """
            $PROTO2
            package surf.test.v1;
            enum Color { COLOR_UNSPECIFIED = 0; COLOR_RED = 1; }
            message Defaults { optional $type value = 1 [default = $literal]; }
            """
        ).generateFile("Defaults.kt")
    }

    @ParameterizedTest(name = "{0} [default = {1}] -> {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "int32    | 10                   | Int = 10",
            "int32    | -5                   | Int = -5",
            "int32    | 0x1F                 | Int = 31",
            "int32    | 0X1f                 | Int = 31",
            "int32    | -2147483648          | Int = Int.MIN_VALUE",
            "int32    | 2147483647           | Int = 2_147_483_647",
            "uint32   | 4294967295           | Int = -1",
            "uint32   | 0xFFFFFFFF           | Int = -1",
            "uint32   | 2147483648           | Int = Int.MIN_VALUE",
            "fixed32  | 0x10                 | Int = 16",
            "sfixed32 | -7                   | Int = -7",
            "int64    | 9223372036854775807  | Long = 9_223_372_036_854_775_807L",
            "int64    | -9223372036854775808 | Long = Long.MIN_VALUE",
            "uint64   | 18446744073709551615 | Long = -1L",
            "sint64   | -3                   | Long = -3L",
            "fixed64  | 0xFFFFFFFFFFFFFFFF   | Long = -1L",
            "sfixed64 | -0x10                | Long = -16L",
            "float    | 1.5                  | Float = 1.5f",
            "float    | 10                   | Float = 10.0f",
            "float    | 1e3                  | Float = 1000.0f",
            "float    | inf                  | Float = Float.POSITIVE_INFINITY",
            "float    | -inf                 | Float = Float.NEGATIVE_INFINITY",
            "float    | nan                  | Float = Float.NaN",
            "double   | 2.25                 | Double = 2.25",
            "double   | -0.5                 | Double = -0.5",
            "double   | 1e-3                 | Double = 0.001",
            "double   | -inf                 | Double = Double.NEGATIVE_INFINITY",
            "double   | nan                  | Double = Double.NaN",
            "bool     | true                 | Boolean = true",
            "bool     | false                | Boolean = false",
            "Color    | COLOR_UNSPECIFIED    | Color = Color.UNSPECIFIED",
        ],
    )
    fun `scalar defaults become typed Kotlin literals`(type: String, literal: String, expected: String) {
        assertContainsAll(generate(type, literal), "public val `value`: $expected,")
    }

    @Test
    fun `string defaults are escaped for Kotlin`() {
        val text = generate("string", "\"a\\\"b\\n\$x\\\\ \u00e9\"")
        assertContainsAll(text, "public val `value`: String = \"a\\\"b\\n\\\$x\\\\ \\u00e9\",")
    }

    @Test
    fun `octal defaults are rejected by wire-schema`() {
        val errors = GeneratorHarness(temp).proto(
            "surf/test/v1/defaults.proto",
            "$PROTO2\npackage surf.test.v1;\nmessage Defaults { optional int32 value = 1 [default = 010]; }",
        ).errors()
        assertError(errors, "defaults.proto:3:", "invalid default value \"010\" for int32")
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "int32   | 010   | 8",
            "int32   | -010  | -8",
            "uint32  | 037777777777 | -1",
            "int64   | 0777  | 511L",
        ],
    )
    fun `the literal parser understands octal integers`(type: String, literal: String, expected: String) {
        assertEquals(expected, DefaultValues.scalar(type, literal).toString())
    }

    @ParameterizedTest(name = "{0} {1} is invalid")
    @CsvSource(
        delimiter = '|',
        value = ["int32 | 2147483648", "uint32 | -1", "uint64 | 18446744073709551616", "int32 | 0x", "int32 | 09", "float | 1e40", "bool | yes"],
    )
    fun `the literal parser rejects out-of-range and malformed literals`(type: String, literal: String) {
        assertNull(DefaultValues.scalar(type, literal))
    }

    @Test
    fun `bytes defaults become byteArrayOf with one byte per character`() {
        val text = generate("bytes", "\"\\x00\\xff\\001A\"")
        assertContainsAll(text, "public val `value`: ByteArray = byteArrayOf(0, -1, 1, 65),")
    }

    @Test
    fun `bytes and string defaults keep the byte values of the source literal`() {
        assertContainsAll(generate("bytes", "\"\u00e9\""), "public val `value`: ByteArray = byteArrayOf(-61, -87),")
        assertContainsAll(generate("string", "\"\\xC3\\xA9\""), "public val `value`: String = \"\\u00e9\",")
        assertContainsAll(generate("string", "\"\\u00e9\\U0001F600\""), "public val `value`: String = \"\\u00e9\\ud83d\\ude00\",")
        assertContainsAll(generate("string", "\"ab\" 'cd'"), "public val `value`: String = \"abcd\",")
    }

    @Test
    fun `a field named default still finds its default literal`() {
        val text = GeneratorHarness(temp).proto(
            "surf/test/v1/defaults.proto",
            "$PROTO2\npackage surf.test.v1;\nmessage Defaults { optional string default = 1 [json_name = \"d\", default = \"x;y\"]; }",
        ).generateFile("Defaults.kt")
        assertContainsAll(text, "public val default: String = \"x;y\",")
    }

    @Test
    fun `empty bytes default becomes an empty array`() {
        assertContainsAll(generate("bytes", "\"\""), "public val `value`: ByteArray = byteArrayOf(),")
    }

    @Test
    fun `escaped newlines in earlier literals do not shift the defaults of later fields`() {
        // wire-schema counts every quoted character that decodes to a newline as a line break
        val text = GeneratorHarness(temp).proto(
            "surf/test/v1/defaults.proto",
            """
            $PROTO2
            package surf.test.v1;
            message Greeting {
              optional string greeting = 1 [default = "Hello\nWorld"];
              optional string farewell = 2 [default = "Bye"];
              optional string third = 3 [default = "Third"];
              optional bytes blob = 4 [default = "\377\000a"];
              optional string escapes = 5 [default = "a\012b\x0Ac"];
              optional string last = 6 [default = "Last"];
            }
            """
        ).generateFile("Defaults.kt")
        assertContainsAll(
            text,
            "public val greeting: String = \"Hello\\nWorld\",",
            "public val farewell: String = \"Bye\",",
            "public val third: String = \"Third\",",
            "public val blob: ByteArray = byteArrayOf(-1, 0, 97),",
            "public val escapes: String = \"a\\nb\\nc\",",
            "public val last: String = \"Last\",",
        )
    }

    @Test
    fun `a raw literal that does not match the default wire-schema parsed is not used`() {
        val file = temp.resolve("surf/test/v1/raw.proto")
        file.parentFile.mkdirs()
        file.writeText("message A {\n  optional string a = 1 [default = \"a\"];\n  optional string b = 2 [default = \"b\"];\n}\n")
        val second = Location.get(temp.path, "surf/test/v1/raw.proto").at(3, 3)

        val rawDefaults = RawDefaults()
        assertEquals("b", rawDefaults.bytes(second, "b")?.decodeToString())
        assertNull(rawDefaults.bytes(second, "a"))
    }
}
