package dev.slne.surf.nats.gradle.testkit

import org.gradle.testkit.runner.BuildResult

/**
 * Returns the task paths listed by `--dry-run`, in execution order.
 */
internal fun BuildResult.plan(): List<String> =
    output.lineSequence()
        .mapNotNull { Regex("""^(:\S+) SKIPPED$""").find(it)?.groupValues?.get(1) }
        .toList()
