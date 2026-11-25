package dev.slne.surf.api.gradle.internalapi

import dev.slne.surf.api.gradle.internalapi.InternalApiFixtures.INTERNAL_LIBRARY_API
import dev.slne.surf.api.gradle.internalapi.InternalApiFixtures.INTERNAL_SURF_API
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InternalApiUsageScannerTest {

    @TempDir
    lateinit var consumerClasses: File

    private fun scan(vararg allowedMarkers: String): List<InternalApiViolation> {
        val roots = listOf(InternalApiFixtures.copyConsumerClasses(consumerClasses))

        return ClassIndex(roots + InternalApiFixtures.testClasses).use { index ->
            InternalApiUsageScanner(index, allowedMarkers.toSet()).scan(roots)
        }
    }

    private fun List<InternalApiViolation>.from(
        simpleClassName: String,
    ): List<InternalApiViolation> {
        val className = "$FIXTURE.consumer.$simpleClassName"

        return filter { it.source == className || it.source.startsWith("$className.") }
    }

    @Test
    fun `reports a call to an internal function`() {
        val violation = scan().from("CallsInternalApi").single()

        assertEquals(LIBRARY + ".internalApi", violation.target)
        assertEquals(listOf(INTERNAL_SURF_API), violation.markers)
    }

    @Test
    fun `reports a library's own marker retained only in bytecode`() {
        val violation = scan().from("CallsLibraryInternalApi").single()

        assertEquals(listOf(INTERNAL_LIBRARY_API), violation.markers)
    }

    @Test
    fun `reports a read of an internal property`() {
        val targets = scan().from("ReadsInternalProperty").map { it.target }

        assertEquals(listOf("$LIBRARY.getInternalProperty"), targets)
    }

    @Test
    fun `reports a class extending an internal class`() {
        val targets = scan().from("ExtendsInternalBase")
            .map { it.target }
            .toSet()

        assertTrue("$FIXTURE.library.InternalBase" in targets, targets.toString())
    }

    @Test
    fun `reports a class nested in an internal class`() {
        val targets = scan().from("CallsNestedOfInternal")
            .map { it.target }
            .toSet()

        assertTrue("$FIXTURE.library.InternalOuter\$Nested.call" in targets, targets.toString())
    }

    @Test
    fun `reports an inherited internal member called through the project's own subclass`() {
        val targets = scan().from("CallsInheritedInternalApi").map { it.target }

        assertEquals(listOf("$FIXTURE.library.PublicBase.internalInherited"), targets)
    }

    @Test
    fun `reports an internal interface default called through the project's own class`() {
        val targets = scan().from("CallsInheritedInterfaceDefault").map { it.target }

        assertEquals(listOf("$FIXTURE.library.PublicContract.internalDefault"), targets)
    }

    @Test
    fun `reports a call to an internal function that uses its default arguments`() {
        val targets = scan().from("CallsInternalApiWithDefaults").map { it.target }

        assertEquals(
            listOf("$FIXTURE.library.DefaultArguments.internalWithDefault\$default"),
            targets,
        )
    }

    @Test
    fun `reports a call to an internal constructor that uses its default arguments`() {
        val targets = scan().from("CallsInternalConstructorWithDefaults").map { it.target }

        assertEquals(listOf("$FIXTURE.library.InternalConstructor.<init>"), targets)
    }

    @Test
    fun `reports public api called through an internal subclass of a dependency`() {
        val violations = scan().from("CallsPublicApiThroughInternalSubclass")
        val publicInheritedCall = violations.single { it.target.endsWith(".publicInherited") }

        assertEquals("$FIXTURE.library.InternalSub.publicInherited", publicInheritedCall.target)
        assertEquals(listOf(INTERNAL_SURF_API), publicInheritedCall.markers)
    }

    @Test
    fun `ignores the project's own markers on a class that inherits public api`() {
        assertEquals(emptyList(), scan().from("InheritsPublicApiInOwnInternalClass"))
    }

    @Test
    fun `ignores public api, published api and annotations that are no marker`() {
        val violations = scan()

        assertEquals(emptyList(), violations.from("CallsPublicApi"))
        assertEquals(emptyList(), violations.from("CallsPublishedApi"))
    }

    @Test
    fun `ignores references between the project's own classes`() {
        assertEquals(emptyList(), scan().from("CallsOwnInternalApi"))
    }

    @Test
    fun `ignores allowed markers`() {
        val violations = scan(INTERNAL_SURF_API, INTERNAL_LIBRARY_API)

        assertEquals(emptyList(), violations)
    }

    private companion object {
        const val FIXTURE = "dev.slne.surf.api.gradle.internalapi.fixture"
        const val LIBRARY = "$FIXTURE.library.Library"
    }
}
