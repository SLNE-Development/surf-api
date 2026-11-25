package dev.slne.surf.database.migration

import dev.slne.surf.database.migration.internal.MigrationClassFinder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MigrationClassFinderTest {

    private val fixtures = "dev.slne.surf.database.migration.fixtures"

    private fun find(vararg packages: String) = MigrationClassFinder.findClassNames(
        packages.toList(),
        javaClass.classLoader,
    )

    @Test
    fun findsObjectsAndClassesInSubpackagesButNoAbstractOrUnrelatedClass() {
        assertEquals(
            listOf(
                "$fixtures.scan.V2_Object",
                "$fixtures.scan.V3_Class",
                "$fixtures.scan.nested.V5_Nested",
            ),
            find("$fixtures.scan"),
        )
    }

    @Test
    fun findsCompanionObjects() {
        assertEquals(
            listOf(
                "$fixtures.companionscan.WithCompanion\$Companion",
                "$fixtures.companionscan.WithNamedCompanion\$Seed",
            ),
            find("$fixtures.companionscan"),
        )
    }

    @Test
    fun findsNamedNestedMigrationsButSkipsAnonymousOnes() {
        assertEquals(
            listOf(
                "$fixtures.nestedscan.Grouped\$V1_NestedObject",
                "$fixtures.nestedscan.Grouped\$V2_NestedClass",
            ),
            find("$fixtures.nestedscan"),
        )
    }

    @Test
    fun skipsAnOpenClassWithoutNoArgConstructorButFindsItsFinalSubclass() {
        assertEquals(
            listOf("$fixtures.openscan.OpenDefault", "$fixtures.openscan.V8_FromBase"),
            find("$fixtures.openscan"),
        )
    }

    @Test
    fun findsAFinalClassWithoutNoArgConstructorWithoutInstantiatingIt() {
        assertEquals(listOf("$fixtures.badscan.V4_NeedsArgs"), find("$fixtures.badscan"))
    }

    @Test
    fun listsAClassOnceWhenPackagesOverlap() {
        assertEquals(
            listOf(
                "$fixtures.scan.V2_Object",
                "$fixtures.scan.V3_Class",
                "$fixtures.scan.nested.V5_Nested",
            ),
            find("$fixtures.scan", "$fixtures.scan.nested", "$fixtures.scan"),
        )
    }

    @Test
    fun unknownPackageFindsNothing() {
        assertEquals(emptyList(), find("$fixtures.doesnotexist"))
    }
}
