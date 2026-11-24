package dev.slne.surf.nats.js.codec.rules

/**
 * A rule violation. [under] prefixes the path with the enclosing field while the walk returns.
 */
internal class Violation private constructor(
    private var path: String,
    private val problem: String?,
) {

    fun under(segment: String): Violation {
        return apply {
            path = "$segment.$path"
        }
    }

    override fun toString(): String {
        if (problem != null) return "field '$path' $problem"

        return "required field '$path' is not set"
    }

    companion object {
        fun missing(field: String): Violation = Violation(field, problem = null)

        fun invalid(field: String, problem: String): Violation = Violation(field, problem)
    }
}
