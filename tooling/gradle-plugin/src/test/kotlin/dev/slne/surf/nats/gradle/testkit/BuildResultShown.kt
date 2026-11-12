package dev.slne.surf.nats.gradle.testkit

import org.gradle.testkit.runner.BuildResult

/**
 * Returns the `[show] key=value` lines printed by [TestBuild.showTask].
 */
internal fun BuildResult.shown(): Map<String, String> =
    output.lineSequence()
        .filter { it.startsWith("[show] ") }
        .associate { line -> line.removePrefix("[show] ").substringBefore('=') to line.substringAfter('=') }
