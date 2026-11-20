package dev.slne.surf.api.gradle.pluginclass

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault

/**
 * Base of the checks that only pass or fail.
 *
 * A passing check writes [passMarker], which gives Gradle an output to decide up-to-date and
 * build cache hits by. A failing check writes nothing and so runs again.
 */
@DisableCachingByDefault(because = "A base class; each check declares its own caching")
abstract class VerificationTask : DefaultTask() {

    @get:OutputFile
    abstract val passMarker: RegularFileProperty

    /**
     * Records that the check passed.
     */
    protected fun markPassed() {
        passMarker.get().asFile.apply {
            parentFile.mkdirs()
            writeText("passed")
        }
    }
}
