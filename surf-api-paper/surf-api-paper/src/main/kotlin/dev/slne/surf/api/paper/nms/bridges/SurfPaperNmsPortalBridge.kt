package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import org.bukkit.Location
import org.bukkit.World

@NmsUseWithCaution
interface SurfPaperNmsPortalBridge {

    /**
     * Finds the nether portal block closest to the given position using the vanilla exit portal
     * search (the server's point-of-interest lookup, as run for every regular portal travel).
     *
     * The result is additionally required to lie within the half-open XZ box
     * ([minX]..&lt;[maxXExclusive], [minZ]..&lt;[maxZExclusive]); a closest portal outside the box
     * yields `null`. Pass [Int.MIN_VALUE]/[Int.MAX_VALUE] to accept any position.
     *
     * Must be called from the thread owning the region around the position.
     *
     * @param searchRadius the horizontal search radius in blocks
     * @return the center of the closest portal block, or `null` when none exists or the closest
     * one lies outside the box
     */
    fun findNearestNetherPortal(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        searchRadius: Int,
        minX: Int,
        minZ: Int,
        maxXExclusive: Int,
        maxZExclusive: Int,
    ): Location?

    /**
     * Creates a nether portal near the given position using the vanilla placement algorithm.
     *
     * A suitable spot is searched within [creationRadius] blocks; when none exists the portal is
     * force-created at the position with a small obsidian platform. The
     * server fires [org.bukkit.event.world.PortalCreateEvent] with reason `NETHER_PAIR` as part
     * of the creation.
     *
     * Must be called from the thread owning the region around the position.
     *
     * @param axisX whether the portal is oriented along the X axis
     * @return a spawn location centered inside the created portal, or `null` when the creation
     * failed or its event was cancelled
     */
    fun createNetherPortal(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        axisX: Boolean,
        creationRadius: Int,
    ): Location?

    companion object : SurfPaperNmsPortalBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPortalBridge>()
