package dev.slne.surf.database.reliability.internal

import java.io.Serial

/**
 * Signals that another execution already committed the claimed key. Carries no stack trace: it only unwinds the
 * claiming transaction so that the stored reply can be read in a new one.
 */
internal class ClaimTakenException : RuntimeException(null, null, false, false) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = 1551993607825604772L
    }
}
