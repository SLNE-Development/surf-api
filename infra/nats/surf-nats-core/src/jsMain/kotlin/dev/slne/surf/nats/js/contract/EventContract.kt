package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.protocol.ProtocolSubjects

internal class EventContract(
    val typeName: String,
    val subject: String,
    val global: Boolean,
    val majorVersion: Int,
) {
    private val subjects = PerOwner(global, typeName) { owner ->
        ProtocolSubjects.event(owner, subject, majorVersion)
    }

    fun subject(owner: String?): String = subjects[owner]
}
