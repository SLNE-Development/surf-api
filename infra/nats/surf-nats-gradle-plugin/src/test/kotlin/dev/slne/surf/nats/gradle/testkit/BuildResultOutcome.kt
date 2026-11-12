package dev.slne.surf.nats.gradle.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

internal fun BuildResult.outcome(path: String): TaskOutcome? = task(path)?.outcome
