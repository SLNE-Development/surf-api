package dev.slne.surf.api.di.definition

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The bean definitions one module contributes.
 *
 * The KSP processor writes one of these per module into [DIRECTORY]; the container reads every file
 * it finds under that directory across the whole classpath and merges them with [plus]. One file per
 * module is what makes the metadata work across jar boundaries without anything having to be
 * registered: a module's definitions travel in its own jar.
 */
@InternalSurfApi
@Serializable
data class BeanMetadata(val beans: List<BeanDefinition> = emptyList()) {

    /**
     * Merges [other] into this metadata.
     *
     * @throws IllegalStateException if both sides define the same class. A duplicate means the same
     * module reached the classpath twice — typically a module shaded into two jars — and silently
     * keeping one of them would hide the cause of whatever fails later.
     */
    operator fun plus(other: BeanMetadata): BeanMetadata {
        if (beans.isEmpty()) return other
        if (other.beans.isEmpty()) return this

        val known = beans.mapTo(HashSet(beans.size)) { it.className }
        val duplicates = other.beans.map { it.className }.filter { it in known }

        check(duplicates.isEmpty()) {
            "The same bean is defined twice on the classpath: ${duplicates.sorted().joinToString()}. " +
                    "A module most likely reached the classpath through two different jars."
        }

        return BeanMetadata(beans + other.beans)
    }

    companion object {

        /** Classpath directory holding one metadata file per module. */
        const val DIRECTORY = "META-INF/surf-api/beans"

        /**
         * How the metadata is written and read.
         *
         * Unknown keys are ignored so a jar built against a newer schema still loads, and defaults
         * are left out so a file records only what a module actually declared.
         */
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = false
            ignoreUnknownKeys = true
        }

        /** Metadata that contributes nothing. */
        fun empty() = BeanMetadata()
    }
}
