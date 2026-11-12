package dev.slne.surf.nats.gradle.weaver

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WeaverCoordinatesTest {

    private fun problems(
        group: String? = "dev.slne.demo",
        name: String? = "demo",
        version: String? = "1.0.0",
        repository: String? = "proto-releases-public",
        repositories: List<String> = listOf("proto-releases-public"),
    ) = WeaverCoordinates.problems(group, name, version, repository, repositories)

    @Test
    fun `valid coordinates have no problems`() {
        assertEquals(emptyList(), problems())
        assertEquals(emptyList(), problems(group = "dev.slne.surf-demo_2", name = "my.proto-bundle_1"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "1.2", "1.2.3", "1.2.3.4", "1.2.3-SNAPSHOT", "1.4.0-rc.1", "10.20.30.40-beta"])
    fun `accepts versions with one to four numeric parts and an optional qualifier`(version: String) {
        assertEquals(emptyList(), problems(version = version))
    }

    @ParameterizedTest
    @ValueSource(strings = ["v1", "1.2.3.4.5", "1..2", "1.2.x", "1.2.3-", "-1.0.0", "1.0.0 SNAPSHOT", "1.0.0+build"])
    fun `rejects malformed versions`(version: String) {
        val problems = problems(version = version)

        assertEquals(1, problems.size, problems.toString())
        assertContains(problems.single(), "weaver.publishing.version '$version'")
    }

    @Test
    fun `rejects versions longer than 64 characters`() {
        assertEquals(emptyList(), problems(version = "1.0.0-" + "a".repeat(58)))
        assertContains(problems(version = "1.0.0-" + "a".repeat(59)).single(), "at most 64 characters")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Dev.slne", "dev..slne", ".dev", "dev.slne.", "dev slne", "dev.Slne", "dev/slne"])
    fun `rejects groups that are not lowercase reverse-DNS names`(group: String) {
        assertContains(problems(group = group).single(), "weaver.publishing.group '$group'")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Demo", "-demo", "de mo", "demo/x"])
    fun `rejects invalid names`(name: String) {
        assertContains(problems(name = name).single(), "weaver.publishing.name '$name'")
    }

    @Test
    fun `rejects names longer than 100 characters`() {
        assertEquals(emptyList(), problems(name = "a".repeat(100)))
        assertContains(problems(name = "a".repeat(101)).single(), "weaver.publishing.name")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Bad", "-bad", "bad_repo", "a b"])
    fun `rejects invalid repository names for publishing and lookup`(repository: String) {
        assertContains(problems(repository = repository).single(), "weaver.publishing.repository '$repository'")
        assertContains(
            problems(repositories = listOf("proto-releases-public", repository)).single(),
            "weaver.repositories entry '$repository'",
        )
    }

    @Test
    fun `reports missing values and every problem at once`() {
        val problems = problems(group = null, name = " ", version = null, repository = "")

        assertEquals(4, problems.size, problems.toString())
        assertTrue(problems.any { "weaver.publishing.group is not set" in it })
        assertTrue(problems.any { "weaver.publishing.name is not set" in it })
        assertTrue(problems.any { "weaver.publishing.version is not set" in it })
        assertTrue(problems.any { "weaver.publishing.repository is not set" in it })
    }

    @Test
    fun `external dependencies need a group and a version`() {
        assertEquals("dev.slne.demo:demo:1.0.0", WeaverCoordinates.external("dev.slne.demo", "demo", "1.0.0"))
        assertFailsWith<GradleException> { WeaverCoordinates.external(null, "demo", "1.0.0") }
        assertFailsWith<GradleException> { WeaverCoordinates.external("dev.slne.demo", "demo", null) }
        assertFailsWith<GradleException> { WeaverCoordinates.external("", "demo", "1.0.0") }
    }
}
