import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckInlineInternalApiTest {

    private val classesDir = File(
        CheckInlineInternalApiTest::class.java.protectionDomain.codeSource.location.toURI(),
    )

    private fun violatingFunctions(): Set<String> = ClassIndex(listOf(classesDir)).use { index ->
        InlineInternalApi.findViolations(listOf(classesDir), index)
            .map { it.inlineFunction }
            .filter { it.startsWith("inlinecheck.fixture.") }
            .map { it.substringAfterLast('.') }
            .toSet()
    }

    @Test
    fun `reports public inline functions that reach an internal-only declaration`() {
        assertEquals(
            setOf(
                "usesInternal",
                "usesInternalClass",
                "usesInternalProperty",
                "withDefault",
                "crossinlineUse",
            ),
            violatingFunctions(),
        )
    }
}
