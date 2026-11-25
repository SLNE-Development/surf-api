package dev.slne.surf.api.paper.serializer

import dev.slne.surf.api.paper.serializer.bukkit.blockdata.BlockDataSerializer
import dev.slne.surf.api.paper.serializer.bukkit.itemstack.ItemStackSerializer
import dev.slne.surf.api.paper.serializer.bukkit.location.LocationSerializer
import dev.slne.surf.api.paper.serializer.bukkit.offlineplayer.OfflinePlayerSerializer
import dev.slne.surf.api.serialization.SurfSerializerModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * Contextual serializers for Bukkit types, the Paper counterpart of [SurfSerializerModule].
 */
object SurfPaperSerializerModule {

    /**
     * Registers [LocationSerializer], [ItemStackSerializer], [OfflinePlayerSerializer] and
     * [BlockDataSerializer].
     */
    val bukkit = SerializersModule {
        contextual(LocationSerializer)
        contextual(ItemStackSerializer)
        contextual(OfflinePlayerSerializer)
        contextual(BlockDataSerializer)
    }

    /**
     * Combines [bukkit] with [SurfSerializerModule.all].
     */
    val all = SerializersModule {
        include(SurfSerializerModule.all)
        include(bukkit)
    }
}
