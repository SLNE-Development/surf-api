package dev.slne.surf.api.minestom.command

import dev.slne.surf.api.shared.api.util.InternalSurfApi

@InternalSurfApi
data class CommandMetadata(
    val shortDescription: String? = null,
    val fullDescription: List<String> = emptyList(),
    val usage: List<String> = emptyList(),
    val help: String? = null,
) {
    companion object {
        @InternalSurfApi
        fun snapshot(
            shortDescription: String?,
            fullDescription: Collection<String>,
            usage: Collection<String>,
            help: String?,
        ): CommandMetadata = CommandMetadata(
            shortDescription = shortDescription,
            fullDescription = fullDescription.toList(),
            usage = usage.toList(),
            help = help,
        )
    }
}
