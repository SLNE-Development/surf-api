package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.microservice.api.MicroserviceInfo

/**
 * Validates the service name, which also names the database pool and the migration history table.
 */
internal object MicroserviceNames {
    private val PATTERN = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    /**
     * Returns [name] if it is a valid [MicroserviceInfo.name].
     *
     * @throws IllegalStateException if [name] is not lowercase letters and digits in hyphen-separated groups or is
     * longer than [MicroserviceInfo.MAX_NAME_LENGTH]
     */
    fun requireValid(name: String): String {
        check(name.length <= MicroserviceInfo.MAX_NAME_LENGTH) {
            "Microservice name '$name' is longer than ${MicroserviceInfo.MAX_NAME_LENGTH} characters."
        }

        check(PATTERN.matches(name)) {
            "Microservice name '$name' must consist of lowercase letters and digits, separated by single hyphens. " +
                "Set spring.application.name or ${MicroserviceInfo.NAME_ENVIRONMENT_VARIABLE}."
        }

        return name
    }
}
