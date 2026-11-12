package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minestom.server.particle.Particle

/**
 * Parses a namespaced particle key into a [Particle.Simple].
 *
 * Particles that take options, such as `dust`, `block` or `vibration`, are rejected as unsupported
 * because the syntax has no way to supply the options. Unknown keys are rejected as well. Both
 * failures are reported at the position where the key starts.
 */
internal object ParticleParser : ArgumentType<Particle> {
    private val UNKNOWN = DynamicCommandExceptionType { value ->
        LiteralMessage("Unknown particle '$value'")
    }

    private val UNSUPPORTED = DynamicCommandExceptionType { value ->
        LiteralMessage("Unsupported particle '$value'; it requires options this argument cannot express")
    }

    override fun parse(reader: StringReader): Particle {
        val start = reader.cursor
        val key = ResourceLocationParser.readKey(reader)
        val particle = Particle.fromKey(key) ?: run {
            reader.cursor = start
            throw UNKNOWN.createWithContext(reader, key.asString())
        }

        if (particle !is Particle.Simple) {
            reader.cursor = start
            throw UNSUPPORTED.createWithContext(reader, key.asString())
        }

        return particle
    }
}
