package dev.slne.surf.api.core.environment

import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class EnvironmentVariablesTest {
    @Test
    fun `required string exists`() {
        val environment = EnvironmentVariables.from(mapOf("HOST" to "localhost"))

        val host: String = environment.required("HOST")

        assertEquals("localhost", host)
    }

    @Test
    fun `required string may be blank`() {
        val environment = EnvironmentVariables.from(mapOf("VALUE" to ""))

        assertEquals("", environment.required("VALUE"))
    }

    @Test
    fun `missing required string fails with its name`() {
        val environment = EnvironmentVariables.from(emptyMap())

        val exception = assertFailsWith<MissingEnvironmentVariableException> {
            environment.required("HOST")
        }

        assertEquals("HOST", exception.variableName)
        assertContains(exception.message.orEmpty(), "missing")
    }

    @Test
    fun `optional strings distinguish existing and missing values`() {
        val environment = EnvironmentVariables.from(mapOf("PRESENT" to "value"))

        val present: String? = environment.optional("PRESENT")
        val missing: String? = environment.optional("MISSING")

        assertEquals("value", present)
        assertNull(missing)
    }

    @Test
    fun `default is used only for a missing value`() {
        val environment = EnvironmentVariables.from(
            mapOf(
                "PRESENT" to "9000",
                "INVALID" to ""
            )
        )

        assertEquals(9000, environment.int("PRESENT", default = 8080))
        assertEquals(8080, environment.int("MISSING", default = 8080))
        assertFailsWith<InvalidEnvironmentVariableException> {
            environment.int("INVALID", default = 8080)
        }
    }

    @Test
    fun `integer parsing is type safe and descriptive`() {
        val environment = EnvironmentVariables.from(
            mapOf(
                "PORT" to "8080",
                "INVALID_PORT" to "eight"
            )
        )

        val port: Int = environment.int("PORT")
        val optionalPort: Int? = environment.optionalInt("OPTIONAL_PORT")
        val exception = assertFailsWith<InvalidEnvironmentVariableException> {
            environment.int("INVALID_PORT")
        }

        assertEquals(8080, port)
        assertNull(optionalPort)
        assertEquals("Int", exception.expectedType)
        assertContains(exception.message.orEmpty(), "INVALID_PORT")
        assertContains(exception.message.orEmpty(), "eight")
        assertIs<NumberFormatException>(exception.cause)
    }

    @Test
    fun `boolean parsing accepts only true and false ignoring case`() {
        val environment = EnvironmentVariables.from(
            mapOf(
                "TRUE" to "TrUe",
                "FALSE" to "FALSE",
                "INVALID" to "yes-maybe"
            )
        )

        assertTrue(environment.boolean("TRUE"))
        assertFalse(environment.boolean("FALSE"))

        val exception = assertFailsWith<InvalidEnvironmentVariableException> {
            environment.boolean("INVALID")
        }
        assertEquals("Boolean", exception.expectedType)
        assertContains(exception.message.orEmpty(), "yes-maybe")
    }

    @Test
    fun `enum parsing accepts exact constant names and rejects others`() {
        val environment = EnvironmentVariables.from(
            mapOf(
                "MODE" to "PRODUCTION",
                "INVALID_MODE" to "production"
            )
        )

        assertEquals(Mode.PRODUCTION, environment.enum<Mode>("MODE"))

        val exception = assertFailsWith<InvalidEnvironmentVariableException> {
            environment.enum<Mode>("INVALID_MODE")
        }
        assertEquals("Mode", exception.expectedType)
    }

    @Test
    fun `generic converters preserve static types and parser failures`() {
        val id = UUID.randomUUID()
        val environment = EnvironmentVariables.from(
            mapOf(
                "ID" to id.toString(),
                "INVALID_ID" to "not-a-uuid",
                "TIMEOUT" to "15s"
            )
        )

        val parsedId: UUID = environment.required("ID", UUID::fromString)
        val optionalId: UUID? = environment.optional("OPTIONAL_ID", UUID::fromString)

        assertEquals(id, parsedId)
        assertNull(optionalId)
        assertEquals(15.seconds, environment.duration("TIMEOUT"))

        val exception = assertFailsWith<InvalidEnvironmentVariableException> {
            environment.required("INVALID_ID", UUID::fromString)
        }
        assertEquals("UUID", exception.expectedType)
        assertIs<IllegalArgumentException>(exception.cause)
    }

    @Test
    fun `validation runs for parsed and default values`() {
        val environment = EnvironmentVariables.from(
            mapOf(
                "PORT" to "443",
                "INVALID_PORT" to "70000"
            )
        )

        assertEquals(443, environment.int("PORT") { requireIn(1..65535) })
        assertEquals(4, environment.int("THREADS", default = 4) {
            require("expected a positive thread count") { it > 0 }
        })

        val rangeException = assertFailsWith<EnvironmentVariableValidationException> {
            environment.int("INVALID_PORT") { requireIn(1..65535) }
        }
        assertContains(rangeException.message.orEmpty(), "INVALID_PORT")
        assertContains(rangeException.message.orEmpty(), "1..65535")

        assertFailsWith<EnvironmentVariableValidationException> {
            environment.int("THREADS", default = 0) {
                require("expected a positive thread count") { it > 0 }
            }
        }
    }

    @Test
    fun `required non-blank strings reject blank values`() {
        val environment = EnvironmentVariables.from(mapOf("TOKEN" to "   "))

        val exception = assertFailsWith<EnvironmentVariableValidationException> {
            environment.requiredNonBlank("TOKEN")
        }

        assertContains(exception.message.orEmpty(), "non-blank")
    }

    @Test
    fun `sensitive failures do not retain or display raw values`() {
        val secret = "top-secret-value"
        val environment = EnvironmentVariables.from(mapOf("PASSWORD" to secret))

        val exception = assertFailsWith<InvalidEnvironmentVariableException> {
            environment.int("PASSWORD", sensitive = true)
        }

        assertFalse(exception.message.orEmpty().contains(secret))
        assertNull(exception.rawValue)
        assertNull(exception.cause)
    }

    @Test
    fun `delegates infer names and preserve nullability`() {
        val environment = EnvironmentVariables.from(
            mapOf(
                "HOST" to "localhost",
                "PORT" to "8080",
                "ENABLED" to "true"
            )
        )

        val HOST: String by environment.required()
        val PORT: Int by environment.int()
        val OPTIONAL_PORT: Int? by environment.optionalInt()
        val ENABLED: Boolean by environment.boolean(default = false)

        assertEquals("localhost", HOST)
        assertEquals(8080, PORT)
        assertNull(OPTIONAL_PORT)
        assertTrue(ENABLED)
    }

    @Test
    fun `named delegates use the explicit variable name`() {
        val environment = EnvironmentVariables.from(mapOf("DATABASE_HOST" to "database"))

        val databaseHost: String by environment.required().named("DATABASE_HOST")

        assertEquals("database", databaseHost)
    }

    @Test
    fun `delegates resolve lazily and cache null and non-null results`() {
        val resolutions = AtomicInteger()
        val environment = EnvironmentVariables.from(EnvironmentSource { name ->
            resolutions.incrementAndGet()
            if (name == "VALUE") "value" else null
        })

        val VALUE: String by environment.required()
        val MISSING: String? by environment.optional()

        assertEquals(0, resolutions.get())
        assertEquals("value", VALUE)
        assertEquals("value", VALUE)
        assertNull(MISSING)
        assertNull(MISSING)
        assertEquals(2, resolutions.get())
    }

    @Test
    fun `concurrent delegate access resolves exactly once`() {
        val resolutions = AtomicInteger()
        val environment = EnvironmentVariables.from(EnvironmentSource {
            resolutions.incrementAndGet()
            Thread.sleep(20)
            "8080"
        })
        val PORT: Int by environment.int()
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)

        try {
            val futures = List(32) {
                executor.submit<Int> {
                    start.await()
                    PORT
                }
            }

            start.countDown()
            assertTrue(futures.all { it.get(5, TimeUnit.SECONDS) == 8080 })
            assertEquals(1, resolutions.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `map-backed sources are copied on creation`() {
        val values = mutableMapOf("PORT" to "8080")
        val environment = EnvironmentVariables.from(values)
        values["PORT"] = "9000"

        assertEquals(8080, environment.int("PORT"))
    }

    private enum class Mode {
        DEVELOPMENT,
        PRODUCTION
    }
}
