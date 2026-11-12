package dev.slne.surf.nats.gradle.contract

import dev.slne.surf.nats.gradle.compat.CompatibilityChecker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * [CompatibilityChecker] reads field semantics from Wire alone, without the contract. These tests pin that it still
 * reports every change of the constructor property that [ContractResolver] decides for a field.
 *
 * `packed` is outside the claim: it changes neither the type nor the default of the property.
 */
class ContractCompatAgreementTest {
    @TempDir
    lateinit var temp: File

    private val respelledDefault = listOf(Change(DECIMAL_DEFAULT, HEX_DEFAULT), Change(HEX_DEFAULT, DECIMAL_DEFAULT))

    @Test
    fun `every change of a field's constructor property is a compatibility violation`() {
        val changes = DECLARATIONS.flatMap { before -> (DECLARATIONS - before).map { after -> Change(before, after) } }
        val comparison = Comparison(changes)

        val unchanged = changes.filterNot(comparison::changesProperty)
        val unreported = changes.filter { comparison.changesProperty(it) && !comparison.isReported(it) }

        assertEquals(respelledDefault.toSet(), unchanged.toSet(), "changes that keep the constructor property")
        assertEquals(emptyList(), unreported)
    }

    @Test
    fun `a default written as another literal of the same value is no violation`() {
        val comparison = Comparison(respelledDefault)

        assertEquals(emptyList(), respelledDefault.filter(comparison::isReported))
    }

    /**
     * A field `f` declared as [before] in the baseline and as [after] in the current sources, in a message of its
     * own.
     */
    private data class Change(val before: String, val after: String) {
        val message = "surf.test.v1.P_${DECLARATIONS.indexOf(before)}_${DECLARATIONS.indexOf(after)}"

        override fun toString(): String = "$before -> $after"
    }

    /**
     * The resolved contracts of the baseline and the current sources of [changes], and the compatibility report
     * between them.
     */
    private inner class Comparison(changes: List<Change>) {
        private val baseline = sources("baseline", changes) { it.before }
        private val current = sources("current", changes) { it.after }
        private val report =
            CompatibilityChecker.check(listOf(baseline.sourceDir), listOf(current.sourceDir), emptyList())

        fun changesProperty(change: Change): Boolean {
            val before = baseline.members(change.message).single()
            val after = current.members(change.message).single()
            return before.type != after.type || before.default != after.default
        }

        fun isReported(change: Change): Boolean = report.violations.any { it.element == "${change.message}.f" }

        private fun sources(
            directory: String,
            changes: List<Change>,
            declaration: (Change) -> String,
        ): ContractHarness {
            val messages = changes.joinToString("\n") {
                "message ${it.message.substringAfterLast('.')} { ${declaration(it)} }"
            }
            return ContractHarness(temp.resolve(directory)).proto(
                "surf/test/v1/pairs.proto",
                """
                |syntax = "proto2";
                |package surf.test.v1;
                |enum Color { COLOR_UNSPECIFIED = 0; }
                |message Msg {}
                |$messages
                """.trimMargin(),
            )
        }
    }

    private companion object {
        const val DECIMAL_DEFAULT = "optional int32 f = 1 [default = 16];"
        const val HEX_DEFAULT = "optional int32 f = 1 [default = 0x10];"

        val DECLARATIONS = listOf(
            "required int32 f = 1;",
            "optional int32 f = 1;",
            DECIMAL_DEFAULT,
            HEX_DEFAULT,
            "optional int64 f = 1;",
            "optional string f = 1;",
            "optional string f = 1 [default = \"a\"];",
            "optional Color f = 1;",
            "optional Msg f = 1;",
            "required Msg f = 1;",
            "repeated int32 f = 1;",
            "map<string, int32> f = 1;",
        )
    }
}
