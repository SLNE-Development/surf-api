package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ServiceNames

internal class ServiceContract(
    val typeName: String,
    val resolved: ResolvedService,
    val majorVersion: Int,
) {
    val name: String get() = resolved.name
    val global: Boolean get() = resolved.global

    private val names = PerOwner(global, typeName) { owner ->
        ServiceNames(owner, name, majorVersion)
    }

    fun names(owner: String?): ServiceNames = names[owner]
}
