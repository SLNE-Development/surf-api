package dev.slne.surf.microservice.gradle.mainclass

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainClassDetectionTest {

    @Test
    fun `finds the Kotlin top-level main, the parameterless main and the JvmStatic main`() {
        val found = findMainClasses(listOf(testClasses()))

        assertContains(found, "$FIXTURES.args.ArgsMainKt")
        assertContains(found, "$FIXTURES.parameterless.ParameterlessMainKt")
        assertContains(found, "$FIXTURES.objectmain.ObjectMain")
    }

    @Test
    fun `ignores classes without a static main method`() {
        val found = findMainClasses(listOf(testClasses()))

        assertFalse("$FIXTURES.objectmain.NoMain" in found, "$found")
        assertFalse("$FIXTURES.objectmain.ObjectMainKt" in found, "$found")
    }

    @Test
    fun `takes the facade of a multifile class and not its synthetic part class`() {
        val found = findMainClasses(listOf(testClasses()))

        assertContains(found, "$FIXTURES.multifile.MultifileMain")
        assertEquals(
            listOf("$FIXTURES.multifile.MultifileMain"),
            found.filter { "MultifileMain" in it },
            "$found",
        )
    }

    @Test
    fun `returns the binary names sorted and ignores directories that do not exist`() {
        val found = findMainClasses(listOf(File("does-not-exist"), testClasses()))

        assertEquals(found.sorted(), found)
        assertTrue(found.all { '/' !in it }, "$found")
    }

    /**
     * The class directory of the test source set, which holds the fixtures.
     */
    private fun testClasses(): File =
        File(MainClassDetectionTest::class.java.protectionDomain.codeSource.location.toURI())

    private companion object {
        const val FIXTURES = "dev.slne.surf.microservice.gradle.mainclass.fixtures"
    }
}
