package dev.slne.surf.nats.gradle.generator

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Writes `.proto` sources into [root] and runs [SurfNatsGenerator] on them.
 */
class GeneratorHarness(private val root: File) {
    val sourceDir: File = root.resolve("src")
    val outputDir: File = root.resolve("out")

    fun proto(path: String, content: String): GeneratorHarness {
        val file = sourceDir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent() + "\n")
        return this
    }

    fun input(
        visibility: GeneratedVisibility = GeneratedVisibility.PUBLIC,
        paperTypes: Boolean = false,
        mappings: List<TypeMapping> = emptyList(),
        sourceDirs: List<File> = listOf(sourceDir),
        protoPathDirs: List<File> = emptyList(),
    ) = GeneratorInput(
        sourceDirs,
        protoPathDirs,
        mappings,
        visibility,
        if (paperTypes) DefaultTypeMappings.common + DefaultTypeMappings.paper else DefaultTypeMappings.common,
    )

    /**
     * Returns the generated files keyed by their path relative to [outputDir].
     */
    fun generate(input: GeneratorInput = input(), outputDir: File = this.outputDir): Map<String, String> {
        val result = SurfNatsGenerator().generate(input, outputDir)
        return result.files.associate { it.relativeTo(outputDir).invariantSeparatorsPath to it.readText() }
    }

    fun errors(input: GeneratorInput = input()): List<String> {
        try {
            SurfNatsGenerator().generate(input, outputDir)
        } catch (e: SurfNatsGeneratorException) {
            return e.errors
        }
        fail("expected a SurfNatsGeneratorException")
    }

    /**
     * Returns the generated text of the single file whose path ends with [suffix].
     */
    fun generateFile(suffix: String, input: GeneratorInput = input()): String {
        val files = generate(input)
        return files.entries.singleOrNull { it.key.endsWith(suffix) }?.value
            ?: fail("no generated file ends with $suffix; generated: ${files.keys}")
    }

    companion object {
        const val PROTO2 = "syntax = \"proto2\";"
        const val OPTIONS_IMPORT = "import \"surf/nats/v1/options.proto\";"
    }
}

fun assertContainsAll(text: String, vararg fragments: String) {
    for (fragment in fragments) {
        assertTrue(fragment in text, "expected fragment not found:\n$fragment\n--- in ---\n$text")
    }
}

fun assertInOrder(text: String, vararg fragments: String) {
    var from = 0
    for (fragment in fragments) {
        val index = text.indexOf(fragment, from)
        assertTrue(index >= 0, "expected fragment not found after index $from:\n$fragment\n--- in ---\n$text")
        from = index + fragment.length
    }
}

fun assertNotContains(text: String, vararg fragments: String) {
    for (fragment in fragments) {
        assertTrue(fragment !in text, "unexpected fragment found:\n$fragment\n--- in ---\n$text")
    }
}

fun assertError(errors: List<String>, vararg fragments: String) {
    assertTrue(
        errors.any { error -> fragments.all { it in error } },
        "no error contains ${fragments.toList()}; errors:\n${errors.joinToString("\n")}",
    )
}
