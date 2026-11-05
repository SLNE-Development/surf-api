package dev.slne.surf.microservice.runtime.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommandLineTokenizerTest {

    @Test
    fun `splits on any whitespace`() {
        assertEquals(listOf("spark", "profiler", "stop"), tokenize("  spark\tprofiler   stop  "))
    }

    @Test
    fun `returns no tokens for a blank line`() {
        assertEquals(emptyList(), tokenize("   "))
    }

    @Test
    fun `groups double-quoted text`() {
        assertEquals(listOf("say", "hello world"), tokenize("say \"hello world\""))
    }

    @Test
    fun `keeps single-quoted text literally`() {
        assertEquals(listOf("say", "a \\\"b\" c"), tokenize("say 'a \\\"b\" c'"))
    }

    @Test
    fun `escapes the next character with a backslash`() {
        assertEquals(listOf("a b", "\"", "'", "\\"), tokenize("a\\ b \\\" \\' \\\\"))
    }

    @Test
    fun `escapes quotes inside double quotes`() {
        assertEquals(listOf("say \"hi\""), tokenize("\"say \\\"hi\\\"\""))
    }

    @Test
    fun `joins adjacent quoted and unquoted parts`() {
        assertEquals(listOf("abc d", "x'y"), tokenize("a\"bc d\" x\"'\"y"))
    }

    @Test
    fun `keeps empty quoted tokens`() {
        assertEquals(listOf("set", "", ""), tokenize("set \"\" ''"))
    }

    @Test
    fun `rejects an unterminated double quote`() {
        val exception = assertFailsWith<CommandLineSyntaxException> { tokenize("say \"hello") }

        assertTrue("unterminated" in exception.message.orEmpty())
    }

    @Test
    fun `rejects an unterminated single quote`() {
        assertFailsWith<CommandLineSyntaxException> { tokenize("say 'hello") }
    }

    @Test
    fun `rejects a trailing backslash`() {
        assertFailsWith<CommandLineSyntaxException> { tokenize("say hello\\") }
    }

    private fun tokenize(line: String) = CommandLineTokenizer.tokenize(line)
}
