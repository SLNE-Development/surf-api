package dev.slne.surf.surfapi.core.api.util

import com.github.retrooper.packetevents.protocol.world.Location
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Contract
import org.spongepowered.math.GenericMath

interface LocationFactory {
    companion object {
        @JvmStatic
        fun distanceSquared(locationA: Location, locationB: Location): Double {
            return (square(locationA.x - locationB.x) + square(locationA.y - locationB.y) + square(
                locationA.z - locationB.z
            ))
        }

        @JvmStatic
        fun distance(locationA: Location, locationB: Location): Double {
            return GenericMath.sqrt(distanceSquared(locationA, locationB))
        }

        @ApiStatus.Internal
        @Contract(pure = true)
        private fun square(value: Double): Double {
            return value * value
        }
    }
}

infix fun Location.distanceSquared(location: Location): Double {
    return LocationFactory.distanceSquared(this, location)
}

infix fun Location.distance(location: Location): Double {
    return LocationFactory.distance(this, location)
}
