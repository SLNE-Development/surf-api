package dev.slne.surf.api.minestom.command.commandapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class CommandMetadata(
    val shortDescription: String? = null,
    val fullDescription: List<String> = emptyList(),
    val usage: List<String> = emptyList(),
    val help: String? = null,
) {
    companion object {
        @ApiStatus.Internal
        fun snapshot(
            shortDescription: String?,
            fullDescription: Collection<String>,
            usage: Collection<String>,
            help: String?,
        ): CommandMetadata = CommandMetadata(
            shortDescription = shortDescription,
            fullDescription = ObjectLists.unmodifiable(ObjectArrayList(fullDescription)),
            usage = ObjectLists.unmodifiable(ObjectArrayList(usage)),
            help = help,
        )
    }
}
