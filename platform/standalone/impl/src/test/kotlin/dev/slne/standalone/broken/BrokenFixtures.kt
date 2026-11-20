package dev.slne.standalone.broken

import org.springframework.stereotype.Component

/**
 * A bean whose creation fails, so that the refresh of its context fails before any lifecycle bean starts.
 */
@Component
class FailingOnCreate {

    init {
        error("creation failed")
    }
}
