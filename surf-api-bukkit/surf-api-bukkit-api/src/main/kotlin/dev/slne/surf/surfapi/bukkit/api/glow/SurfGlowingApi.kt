package dev.slne.surf.surfapi.bukkit.api.glow

import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

interface SurfGlowingApi {
    fun makeGlowing(target: Entity, viewer: Player, color: NamedTextColor? = null)

    fun makeGlowing(
        targetId: Int,
        teamId: String,
        viewer: Player,
        color: NamedTextColor? = null,
        otherFlags: Byte = 0,
    )

    fun makeGlowing(block: Block, viewer: Player, color: NamedTextColor)
    fun makeGlowing(location: Location, viewer: Player, color: NamedTextColor)

    fun removeGlowing(target: Entity, viewer: Player)
    fun removeGlowing(targetId: Int, viewer: Player)

    fun removeGlowing(block: Block, viewer: Player)
    fun removeGlowing(location: Location, viewer: Player)

    companion object : SurfGlowingApi by api {
        val INSTANCE get() = api
    }
}

private val api = requiredService<SurfGlowingApi>()