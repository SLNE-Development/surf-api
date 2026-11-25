package dev.slne.surf.api.gradle.util

import dev.slne.surf.microservice.gradle.testkit.TestBuild
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * Generates plugin files through TestKit, offline, each in a Git repository of its own so the
 * `origin` remote is under the test's control.
 */
class PluginUrlBuildTest {

    private fun paperProject(info: TestInfo, origin: String?, description: String = ""): TestBuild {
        return project(info, origin).apply {
            script(
                """
                plugins { id 'dev.slne.surf.api.gradle.paper-plugin' }
                paper { $description }
                """,
            )
        }
    }

    private fun velocityProject(info: TestInfo, origin: String?): TestBuild {
        return project(info, origin).apply {
            script(
                """
                plugins { id 'dev.slne.surf.api.gradle.velocity' }
                velocityPluginFile { main = 'com.example.plugin.Main' }
                """,
            )
        }
    }

    private fun project(info: TestInfo, origin: String?): TestBuild {
        return TestBuild.create(info.displayName, withSurfApi = true).apply {
            file("gradle.properties", "kotlin.stdlib.default.dependency=false")
            file(
                "src/main/kotlin/com/example/plugin/Main.kt",
                "package com.example.plugin\nclass Main",
            )
            git("init", "--quiet")
            origin?.let { git("remote", "add", "origin", it) }
        }
    }

    private fun TestBuild.git(vararg arguments: String) {
        val process = ProcessBuilder("git", *arguments)
            .directory(dir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "git ${arguments.joinToString(" ")} failed: $output" }
    }

    private fun TestBuild.withUrlProperty() {
        file("gradle.properties", "kotlin.stdlib.default.dependency=false\nurl=https://example.com")
    }

    private fun TestBuild.paperPluginYml(): String {
        run("generatePaperPluginDescription")

        return dir.resolve("build/generated/plugin-yml/Paper/paper-plugin.yml").readText()
    }

    private fun TestBuild.velocityPluginJson(): String {
        run("generateVelocityPluginFile")

        return dir.resolve("build/generated/surf-api/velocity/velocity-plugin.json").readText()
    }

    @Test
    fun `paper website defaults to the origin remote`(info: TestInfo) {
        val build = paperProject(info, origin = "git@github.com:example/demo.git")

        assertContains(build.paperPluginYml(), "website: https://github.com/example/demo")
    }

    @Test
    fun `url property wins over the origin remote`(info: TestInfo) {
        val build = paperProject(info, origin = "git@github.com:example/demo.git")
        build.withUrlProperty()

        assertContains(build.paperPluginYml(), "website: https://example.com")
    }

    @Test
    fun `explicit website wins over the url property`(info: TestInfo) {
        val build = paperProject(
            info,
            origin = null,
            description = "website = 'https://explicit.example'",
        )
        build.withUrlProperty()

        assertContains(build.paperPluginYml(), "website: https://explicit.example")
    }

    @Test
    fun `paper description has no website without an origin remote`(info: TestInfo) {
        val build = paperProject(info, origin = null)

        assertFalse("website:" in build.paperPluginYml())
    }

    @Test
    fun `velocity url defaults to the origin remote`(info: TestInfo) {
        val build = velocityProject(info, origin = "https://token@git.example.com/group/demo.git")

        val pluginJson = build.velocityPluginJson()

        assertContains(pluginJson, "\"url\": \"https://git.example.com/group/demo\"")
    }
}
