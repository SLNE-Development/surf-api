import kotlin.test.Test
import kotlin.test.assertEquals

class CheckAbiDumpNestingTest {

    @Test
    fun `reports a nested class whose outer class is left out`() {
        val dump = """
            public final class demo/Public {
            }

            public final class demo/Public${'$'}Companion {
            }

            public final class demo/Internal${'$'}Companion {
            }
        """.trimIndent()

        assertEquals(listOf("demo/Internal\$Companion"), orphanedNestedClasses(dump))
    }

    @Test
    fun `resolves a generated serializer to the class it serializes`() {
        val dump = """
            public final class demo/Data {
            }

            public final synthetic class demo/Data${'$'}${'$'}serializer : kotlinx/serialization/internal/GeneratedSerializer {
            }

            public final synthetic class demo/Hidden${'$'}${'$'}serializer : kotlinx/serialization/internal/GeneratedSerializer {
            }
        """.trimIndent()

        assertEquals(listOf("demo/Hidden\$\$serializer"), orphanedNestedClasses(dump))
    }
}
