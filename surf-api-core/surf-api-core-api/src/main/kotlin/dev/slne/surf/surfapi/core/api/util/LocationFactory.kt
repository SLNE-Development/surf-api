package dev.slne.surf.surfapi.core.api.util

import com.github.retrooper.packetevents.protocol.world.Location
import org.spongepowered.math.GenericMath

@Deprecated("Not longer maintained.")
interface LocationFactory {
    companion object {
        @JvmStatic
        @Deprecated("Not longer maintained.", ReplaceWith("locationA.position.distanceSquared(locationB.position)"))
        fun distanceSquared(locationA: Location, locationB: Location): Double {
            return locationA.position.distanceSquared(locationB.position)
        }

        @JvmStatic
        @Deprecated("Not longer maintained.", ReplaceWith("locationA.position.distance(locationB.position)"))
        fun distance(locationA: Location, locationB: Location): Double {
            return GenericMath.sqrt(locationA.position.distanceSquared(locationB.position))
        }
    }
}

@Deprecated("Not longer maintained.", ReplaceWith("this.position.distanceSquared(location.position)"))
infix fun Location.distanceSquared(location: Location): Double {
    return this.position.distanceSquared(location.position)
}

@Deprecated("Not longer maintained.", ReplaceWith("this.position.distance(location.position)"))
infix fun Location.distance(location: Location): Double {
    return this.position.distance(location.position)
}
