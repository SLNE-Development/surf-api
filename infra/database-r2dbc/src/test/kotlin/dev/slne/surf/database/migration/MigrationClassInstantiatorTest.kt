package dev.slne.surf.database.migration

import dev.slne.surf.database.migration.fixtures.companionscan.WithCompanion
import dev.slne.surf.database.migration.fixtures.nestedscan.Grouped
import dev.slne.surf.database.migration.fixtures.scan.V2_Object
import dev.slne.surf.database.migration.internal.MigrationClassInstantiator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MigrationClassInstantiatorTest {

    private val scanPackage = "dev.slne.surf.database.migration.fixtures.scan"
    private val companionPackage = "dev.slne.surf.database.migration.fixtures.companionscan"

    @Test
    fun instantiatesANamedCompanionObject() {
        val result = MigrationClassInstantiator.instantiate(
            javaClass.classLoader,
            listOf(
                "$companionPackage.WithCompanion\$Companion",
                "$companionPackage.WithNamedCompanion\$Seed",
            ),
        )

        assertEquals(listOf("6", "7"), result.map { it.version })
        assertSame(WithCompanion.Companion, result.first())
    }

    @Test
    fun instantiatesExactlyTheNamedClasses() {
        val result = MigrationClassInstantiator.instantiate(
            javaClass.classLoader,
            listOf(
                "$scanPackage.V3_Class",
                V2_Object::class.java.name,
                Grouped.V2_NestedClass::class.java.name,
            ),
        )

        assertEquals(listOf("3", "2", "2"), result.map { it.version })
        assertSame(V2_Object, result[1])
        assertIs<Grouped.V2_NestedClass>(result.last())
    }

    @Test
    fun loadsNoClassBesidesTheNamedOnes() {
        val broken = "$scanPackage.V3_Class"
        val loader = object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == broken) throw NoClassDefFoundError("missing/Dependency")

                return super.loadClass(name, resolve)
            }
        }

        val result = MigrationClassInstantiator.instantiate(
            loader,
            listOf(V2_Object::class.java.name),
        )

        assertSame(V2_Object, result.single())
    }

    @Test
    fun wrapsLinkageErrorsWhileLoadingAClass() {
        val broken = "$scanPackage.V3_Class"
        val loader = object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == broken) throw NoClassDefFoundError("missing/Dependency")

                return super.loadClass(name, resolve)
            }
        }

        val e = assertThrows<IllegalStateException> {
            MigrationClassInstantiator.instantiate(
                loader,
                listOf(broken),
            )
        }

        assertTrue(e.message!!.contains(broken), e.message)
        assertTrue(e.cause is NoClassDefFoundError)
    }

    @Test
    fun namedClassThatCannotBeLoadedFails() {
        val e = assertThrows<IllegalStateException> {
            MigrationClassInstantiator.instantiate(
                javaClass.classLoader,
                listOf("$scanPackage.DoesNotExist"),
            )
        }

        assertTrue(e.message!!.contains("DoesNotExist"), e.message)
    }

    @Test
    fun namedClassThatIsNoConcreteMigrationFails() {
        for (simpleName in listOf("NotAMigration", "Base")) {
            val e = assertThrows<IllegalStateException>(simpleName) {
                MigrationClassInstantiator.instantiate(
                    javaClass.classLoader,
                    listOf("$scanPackage.$simpleName"),
                )
            }
            assertTrue(e.message!!.contains(simpleName), e.message)
        }
    }

    @Test
    fun namedClassWithoutNoArgConstructorFails() {
        val e = assertThrows<IllegalStateException> {
            MigrationClassInstantiator.instantiate(
                javaClass.classLoader,
                listOf("dev.slne.surf.database.migration.fixtures.badscan.V4_NeedsArgs"),
            )
        }

        assertTrue(e.message!!.contains("V4_NeedsArgs"), e.message)
    }
}
