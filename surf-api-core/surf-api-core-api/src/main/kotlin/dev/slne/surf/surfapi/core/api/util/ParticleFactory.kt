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
interface ParticleFactory {
    companion object {
        @JvmStatic
        fun of(type: ParticleType<*>): Particle<*> {
            return Particle(type)
        }

        fun <D : ParticleData> of(type: ParticleType<D>, data: D): Particle<D> {
            return Particle(type, data)
        }

        fun of(
            type: ParticleType<ParticleBlockStateData>,
            blockState: WrappedBlockState
        ): Particle<ParticleBlockStateData> {
            return of(type, ParticleBlockStateData(blockState))
        }

        fun of(
            type: ParticleType<ParticleDustColorTransitionData>, scale: Float, start: TextColor,
            end: TextColor
        ): Particle<ParticleDustColorTransitionData> {

            return of(
                type,
                ParticleDustColorTransitionData(
                    scale, start.red().toFloat(), start.green().toFloat(), start.blue().toFloat(),
                    end.red().toFloat(), end.green().toFloat(), end.blue().toFloat()
                )
            )
        }

        fun of(
            type: ParticleType<ParticleItemStackData>,
            itemStack: ItemStack
        ): Particle<ParticleItemStackData> {
            return of(type, ParticleItemStackData(itemStack))
        }

        fun of(
            type: ParticleType<ParticleSculkChargeData>,
            roll: Float
        ): Particle<ParticleSculkChargeData> {
            return of(type, ParticleSculkChargeData(roll))
        }

        fun of(type: ParticleType<ParticleShriekData>, delay: Int): Particle<ParticleShriekData> {
            return of(type, ParticleShriekData(delay))
        }

        fun of(
            type: ParticleType<ParticleVibrationData>, startingPosition: Vector3i,
            blockPosition: Vector3i, ticks: Int
        ): Particle<ParticleVibrationData> {
            return of(type, ParticleVibrationData(startingPosition, blockPosition, ticks))
        }

        fun of(
            type: ParticleType<ParticleVibrationData>, startingPosition: Vector3i,
            entityId: Int, ticks: Int
        ): Particle<ParticleVibrationData> {
            return of(type, ParticleVibrationData(startingPosition, entityId, ticks))
        }
    }
}
