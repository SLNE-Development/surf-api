package dev.slne.surf.nats.gradle.validate

import com.squareup.wire.schema.Location
import dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException

/**
 * Collects generator errors in report order; [throwIfAny] raises them together.
 */
internal class Problems {
    private val errors = mutableListOf<String>()

    fun error(location: Location, message: String) {
        errors += "$location: $message"
    }

    fun error(source: String, message: String) {
        errors += "$source: $message"
    }

    fun throwIfAny() {
        if (errors.isNotEmpty()) throw SurfNatsGeneratorException(errors.toList())
    }
}
