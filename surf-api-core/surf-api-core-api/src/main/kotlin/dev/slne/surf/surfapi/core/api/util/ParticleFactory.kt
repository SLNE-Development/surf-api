package dev.slne.surf.surfapi.core.api.util

import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.particle.Particle
import com.github.retrooper.packetevents.protocol.particle.data.*
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.util.Vector3i
import net.kyori.adventure.text.format.TextColor
import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
@Deprecated("Not longer maintained.")
interface ParticleFactory {
    companion object {
        @JvmStatic
        @Deprecated(
            "Not longer maintained.",
            ReplaceWith("Particle(type)", "com.github.retrooper.packetevents.protocol.particle.Particle")
        )
        fun of(type: ParticleType<*>): Particle<*> {
            return Particle(type)
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith("Particle(type, data)", "com.github.retrooper.packetevents.protocol.particle.Particle")
        )
        fun <D : ParticleData> of(type: ParticleType<D>, data: D): Particle<D> {
            return Particle(type, data)
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleBlockStateData(blockState))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleBlockStateData.ParticleBlockStateData"
            )
        )
        fun of(
            type: ParticleType<ParticleBlockStateData>,
            blockState: WrappedBlockState
        ): Particle<ParticleBlockStateData> {
            return Particle(type, ParticleBlockStateData(blockState))
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleDustColorTransitionData(scale, start.red().toFloat(), start.green().toFloat(), start.blue().toFloat(), end.red().toFloat(), end.green().toFloat(), end.blue().toFloat()))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleDustColorTransitionData",
            )
        )
        fun of(
            type: ParticleType<ParticleDustColorTransitionData>, scale: Float, start: TextColor,
            end: TextColor
        ): Particle<ParticleDustColorTransitionData> {
            return Particle(
                type,
                ParticleDustColorTransitionData(
                    scale, start.red().toFloat(), start.green().toFloat(), start.blue().toFloat(),
                    end.red().toFloat(), end.green().toFloat(), end.blue().toFloat()
                )
            )
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleItemStackData(itemStack))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleItemStackData"
            )
        )
        fun of(
            type: ParticleType<ParticleItemStackData>,
            itemStack: ItemStack
        ): Particle<ParticleItemStackData> {
            return Particle(type, ParticleItemStackData(itemStack))
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleSculkChargeData(roll))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleSculkChargeData"
            )
        )
        fun of(
            type: ParticleType<ParticleSculkChargeData>,
            roll: Float
        ): Particle<ParticleSculkChargeData> {
            return Particle(type, ParticleSculkChargeData(roll))
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleShriekData(delay))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleShriekData"
            )
        )
        fun of(type: ParticleType<ParticleShriekData>, delay: Int): Particle<ParticleShriekData> {
            return Particle(type, ParticleShriekData(delay))
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleVibrationData(startingPosition, blockPosition, ticks))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleVibrationData"
            )
        )
        fun of(
            type: ParticleType<ParticleVibrationData>, startingPosition: Vector3i,
            blockPosition: Vector3i, ticks: Int
        ): Particle<ParticleVibrationData> {
            return Particle(type, ParticleVibrationData(startingPosition, blockPosition, ticks))
        }

        @Deprecated(
            "Not longer maintained.",
            ReplaceWith(
                "Particle(type, ParticleVibrationData(startingPosition, entityId, ticks))",
                "com.github.retrooper.packetevents.protocol.particle.Particle",
                "com.github.retrooper.packetevents.protocol.particle.data.ParticleVibrationData"
            )
        )
        fun of(
            type: ParticleType<ParticleVibrationData>, startingPosition: Vector3i,
            entityId: Int, ticks: Int
        ): Particle<ParticleVibrationData> {
            return Particle(type, ParticleVibrationData(startingPosition, entityId, ticks))
        }
    }
}
